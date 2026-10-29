package com.jobtracker.controller;

import com.jobtracker.entity.Role;
import com.jobtracker.security.AuthUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Helpers to "log in" a fake user inside MockMvc tests (no real JWT needed). */
final class ControllerTestSupport {

    private ControllerTestSupport() {
    }

    static RequestPostProcessor recruiter() {
        return SecurityMockMvcRequestPostProcessors.user(new AuthUser(1L, "rec@test.com", "x", Role.RECRUITER));
    }

    static RequestPostProcessor candidate() {
        return SecurityMockMvcRequestPostProcessors.user(new AuthUser(2L, "cand@test.com", "x", Role.CANDIDATE));
    }
}
