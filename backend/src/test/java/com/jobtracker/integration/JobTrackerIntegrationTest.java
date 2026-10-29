package com.jobtracker.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jobtracker.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full-stack test: real Spring context + real PostgreSQL (in Docker) + real Flyway migration +
 * real JWT security. Skipped automatically when Docker isn't available on the machine.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class JobTrackerIntegrationTest {

    @Container
    @ServiceConnection // Spring Boot wires the datasource to this container automatically
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;

    // ---------- helpers ----------

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private String register(String role) throws Exception {
        String email = role.toLowerCase() + "-" + UUID.randomUUID() + "@test.com";
        MvcResult res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Test " + role, "email", email,
                                "password", "password123", "role", role))))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.token");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Map<String, Object> jobBody(String title) {
        return Map.of("title", title, "company", "Acme", "location", "Remote",
                "description", "Build great things", "salaryRange", "10-20 LPA");
    }

    private int createJob(String recruiterToken, String title) throws Exception {
        MvcResult res = mvc.perform(post("/api/jobs").header("Authorization", bearer(recruiterToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(jobBody(title))))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.id");
    }

    private int apply(String candidateToken, int jobId) throws Exception {
        MvcResult res = mvc.perform(post("/api/jobs/" + jobId + "/applications")
                        .header("Authorization", bearer(candidateToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"coverLetter\":\"Pick me\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.id");
    }

    // ---------- tests ----------

    @Test
    void happyPath_recruiterPosts_candidateApplies_recruiterReviews_candidateWithdraws() throws Exception {
        String recruiter = register("RECRUITER");
        String candidate = register("CANDIDATE");
        int jobId = createJob(recruiter, "Spring Engineer " + UUID.randomUUID());

        mvc.perform(get("/api/jobs").param("keyword", "spring engineer")
                        .header("Authorization", bearer(candidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id==" + jobId + ")]").isNotEmpty());

        int appId = apply(candidate, jobId);

        mvc.perform(get("/api/jobs/" + jobId + "/applications").header("Authorization", bearer(recruiter)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("APPLIED"));

        mvc.perform(patch("/api/applications/" + appId + "/status").header("Authorization", bearer(recruiter))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INTERVIEW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INTERVIEW"));

        mvc.perform(get("/api/applications/mine").header("Authorization", bearer(candidate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("INTERVIEW"));

        mvc.perform(delete("/api/applications/" + appId).header("Authorization", bearer(candidate)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/applications/mine").header("Authorization", bearer(candidate)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void applyingTwice_isRejectedWith409() throws Exception {
        String recruiter = register("RECRUITER");
        String candidate = register("CANDIDATE");
        int jobId = createJob(recruiter, "Duplicate Test");
        apply(candidate, jobId);

        mvc.perform(post("/api/jobs/" + jobId + "/applications").header("Authorization", bearer(candidate))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You have already applied to this job"));
    }

    @Test
    void applyingToClosedJob_isRejectedWith422() throws Exception {
        String recruiter = register("RECRUITER");
        String candidate = register("CANDIDATE");
        int jobId = createJob(recruiter, "Soon closed");
        Map<String, Object> closed = new java.util.HashMap<>(jobBody("Soon closed"));
        closed.put("status", "CLOSED");
        mvc.perform(put("/api/jobs/" + jobId).header("Authorization", bearer(recruiter))
                        .contentType(MediaType.APPLICATION_JSON).content(json(closed)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/jobs/" + jobId + "/applications").header("Authorization", bearer(candidate))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void ownershipChecks_otherUsersCannotTouchMyData() throws Exception {
        String recruiterA = register("RECRUITER");
        String recruiterB = register("RECRUITER");
        String candidate1 = register("CANDIDATE");
        String candidate2 = register("CANDIDATE");
        int jobId = createJob(recruiterA, "A's job");
        int appId = apply(candidate1, jobId);

        // Recruiter B cannot edit / delete / view applicants / change status for A's job
        mvc.perform(put("/api/jobs/" + jobId).header("Authorization", bearer(recruiterB))
                        .contentType(MediaType.APPLICATION_JSON).content(json(jobBody("hijacked"))))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/jobs/" + jobId).header("Authorization", bearer(recruiterB)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/jobs/" + jobId + "/applications").header("Authorization", bearer(recruiterB)))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/applications/" + appId + "/status").header("Authorization", bearer(recruiterB))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isForbidden());

        // Candidate 2 cannot withdraw candidate 1's application
        mvc.perform(delete("/api/applications/" + appId).header("Authorization", bearer(candidate2)))
                .andExpect(status().isForbidden());
    }

    @Test
    void roleBasedAccess_andMissingOrBadTokens() throws Exception {
        String recruiter = register("RECRUITER");
        String candidate = register("CANDIDATE");
        int jobId = createJob(recruiter, "RBAC");

        mvc.perform(post("/api/jobs").header("Authorization", bearer(candidate))
                        .contentType(MediaType.APPLICATION_JSON).content(json(jobBody("nope"))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/jobs/" + jobId + "/applications").header("Authorization", bearer(recruiter))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/jobs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/jobs").header("Authorization", "Bearer garbage.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registration_validationDuplicatesAndPasswordHashing() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@test.com";
        String body = json(Map.of("name", "Dup", "email", email, "password", "password123", "role", "CANDIDATE"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"email\":\"bad\",\"password\":\"1\",\"role\":\"CANDIDATE\"}"))
                .andExpect(status().isBadRequest());

        String stored = userRepository.findByEmail(email).orElseThrow().getPassword();
        assertThat(stored).startsWith("$2").isNotEqualTo("password123"); // BCrypt hash
    }

    @Test
    void login_rightAndWrongPassword() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@test.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "L", "email", email, "password", "password123",
                                "role", "RECRUITER"))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("RECRUITER"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }
}
