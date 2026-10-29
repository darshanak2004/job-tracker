package com.jobtracker.controller;

import static com.jobtracker.controller.ControllerTestSupport.candidate;
import static com.jobtracker.controller.ControllerTestSupport.recruiter;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobtracker.config.SecurityConfig;
import com.jobtracker.dto.JobRequest;
import com.jobtracker.dto.JobResponse;
import com.jobtracker.dto.PageResponse;
import com.jobtracker.entity.JobStatus;
import com.jobtracker.exception.ForbiddenException;
import com.jobtracker.exception.ResourceNotFoundException;
import com.jobtracker.security.CustomUserDetailsService;
import com.jobtracker.security.JwtService;
import com.jobtracker.service.JobPostingService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobPostingController.class)
@Import(SecurityConfig.class)
class JobPostingControllerTest {

    @Autowired MockMvc mvc;
    @MockBean JobPostingService jobService;
    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService userDetailsService;

    private static final String VALID_JOB = """
            {"title":"Dev","company":"Acme","location":"Remote","description":"Build","salaryRange":"10 LPA"}""";

    private JobResponse sampleJob() {
        return new JobResponse(5L, "Dev", "Acme", "Remote", "Build", "10 LPA", JobStatus.OPEN, 1L, "Rec",
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void list_requiresAuthentication() throws Exception {
        mvc.perform(get("/api/jobs")).andExpect(status().isUnauthorized());
        verifyNoInteractions(jobService);
    }

    @Test
    void list_candidateCanBrowse_andParamsArePassedThrough() throws Exception {
        when(jobService.searchOpenJobs("java", 1, 5))
                .thenReturn(new PageResponse<>(List.of(sampleJob()), 1, 5, 6, 2));

        mvc.perform(get("/api/jobs").param("keyword", "java").param("page", "1").param("size", "5")
                        .with(candidate()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Dev"))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getOne_notFound_returns404() throws Exception {
        when(jobService.getById(99L)).thenThrow(new ResourceNotFoundException("Job posting 99 not found"));

        mvc.perform(get("/api/jobs/99").with(candidate()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Job posting 99 not found"));
    }

    @Test
    void create_asRecruiter_returns201() throws Exception {
        when(jobService.create(eq(1L), any(JobRequest.class))).thenReturn(sampleJob());

        mvc.perform(post("/api/jobs").with(recruiter()).contentType(MediaType.APPLICATION_JSON).content(VALID_JOB))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void create_asCandidate_isForbidden() throws Exception {
        mvc.perform(post("/api/jobs").with(candidate()).contentType(MediaType.APPLICATION_JSON).content(VALID_JOB))
                .andExpect(status().isForbidden());
        verifyNoInteractions(jobService);
    }

    @Test
    void create_invalidBody_returns400() throws Exception {
        mvc.perform(post("/api/jobs").with(recruiter()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"company\":\"\",\"location\":\"\",\"description\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
        verifyNoInteractions(jobService);
    }

    @Test
    void update_notOwner_returns403() throws Exception {
        when(jobService.update(eq(1L), eq(5L), any(JobRequest.class)))
                .thenThrow(new ForbiddenException("You can only modify your own job postings"));

        mvc.perform(put("/api/jobs/5").with(recruiter()).contentType(MediaType.APPLICATION_JSON).content(VALID_JOB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only modify your own job postings"));
    }

    @Test
    void delete_asRecruiter_returns204() throws Exception {
        mvc.perform(delete("/api/jobs/5").with(recruiter())).andExpect(status().isNoContent());
        verify(jobService).delete(1L, 5L);
    }

    @Test
    void mine_asCandidate_isForbidden() throws Exception {
        mvc.perform(get("/api/jobs/mine").with(candidate())).andExpect(status().isForbidden());
    }

    @Test
    void mine_asRecruiter_listsOwnJobs() throws Exception {
        when(jobService.listByRecruiter(1L)).thenReturn(List.of(sampleJob()));

        mvc.perform(get("/api/jobs/mine").with(recruiter()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recruiterName").value("Rec"));
    }
}
