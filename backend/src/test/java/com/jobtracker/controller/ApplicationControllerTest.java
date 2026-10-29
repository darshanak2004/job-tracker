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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jobtracker.config.SecurityConfig;
import com.jobtracker.dto.ApplicationResponse;
import com.jobtracker.dto.ApplyRequest;
import com.jobtracker.entity.ApplicationStatus;
import com.jobtracker.exception.DuplicateResourceException;
import com.jobtracker.security.CustomUserDetailsService;
import com.jobtracker.security.JwtService;
import com.jobtracker.service.ApplicationService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApplicationController.class)
@Import(SecurityConfig.class)
class ApplicationControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ApplicationService applicationService;
    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService userDetailsService;

    private ApplicationResponse sample(ApplicationStatus status) {
        return new ApplicationResponse(50L, 10L, "Dev", "Acme", 2L, "Cand", "cand@test.com", "Hi", status,
                Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void apply_asCandidate_returns201() throws Exception {
        when(applicationService.apply(eq(2L), eq(10L),
                any(ApplyRequest.class))).thenReturn(sample(ApplicationStatus.APPLIED));

        mvc.perform(post("/api/jobs/10/applications").with(candidate())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"coverLetter\":\"Hi\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPLIED"));
    }

    @Test
    void apply_asRecruiter_isForbidden() throws Exception {
        mvc.perform(post("/api/jobs/10/applications").with(recruiter())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(applicationService);
    }

    @Test
    void apply_duplicate_returns409() throws Exception {
        when(applicationService.apply(eq(2L), eq(10L),
                any(ApplyRequest.class))).thenThrow(new DuplicateResourceException("You have already applied to this job"));

        mvc.perform(post("/api/jobs/10/applications").with(candidate())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You have already applied to this job"));
    }

    @Test
    void apply_withoutToken_returns401() throws Exception {
        mvc.perform(post("/api/jobs/10/applications").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mine_asCandidate_listsApplications() throws Exception {
        when(applicationService.listForCandidate(2L)).thenReturn(List.of(sample(ApplicationStatus.APPLIED)));

        mvc.perform(get("/api/applications/mine").with(candidate()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].jobTitle").value("Dev"));
    }

    @Test
    void mine_asRecruiter_isForbidden() throws Exception {
        mvc.perform(get("/api/applications/mine").with(recruiter())).andExpect(status().isForbidden());
    }

    @Test
    void forJob_asRecruiter_listsApplicants() throws Exception {
        when(applicationService.listForJob(1L, 10L)).thenReturn(List.of(sample(ApplicationStatus.APPLIED)));

        mvc.perform(get("/api/jobs/10/applications").with(recruiter()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].candidateEmail").value("cand@test.com"));
    }

    @Test
    void forJob_asCandidate_isForbidden() throws Exception {
        mvc.perform(get("/api/jobs/10/applications").with(candidate())).andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_asRecruiter_returnsUpdated() throws Exception {
        when(applicationService.updateStatus(1L, 50L, ApplicationStatus.INTERVIEW))
                .thenReturn(sample(ApplicationStatus.INTERVIEW));

        mvc.perform(patch("/api/applications/50/status").with(recruiter())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INTERVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"));
    }

    @Test
    void updateStatus_bogusValue_returns400() throws Exception {
        mvc.perform(patch("/api/applications/50/status").with(recruiter())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(applicationService);
    }

    @Test
    void updateStatus_missingStatus_returns400() throws Exception {
        mvc.perform(patch("/api/applications/50/status").with(recruiter())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    void withdraw_asCandidate_returns204() throws Exception {
        mvc.perform(delete("/api/applications/50").with(candidate())).andExpect(status().isNoContent());
        verify(applicationService).withdraw(2L, 50L);
    }

    @Test
    void withdraw_asRecruiter_isForbidden() throws Exception {
        mvc.perform(delete("/api/applications/50").with(recruiter())).andExpect(status().isForbidden());
    }
}
