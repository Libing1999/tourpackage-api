package com.tourpackage.api.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Authentication and authorisation, end to end. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    /** Seeded by V11__seed_bootstrap_admin.sql. */
    private static final String ADMIN_EMAIL = "admin@tourpackage.com";
    private static final String ADMIN_PASSWORD = "ChangeMe123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("an admin endpoint rejects an anonymous caller")
    void adminEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/admin/bookings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an admin endpoint rejects a malformed token rather than trusting it")
    void garbageTokenIsRejected() throws Exception {
        mockMvc.perform(get("/admin/bookings").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a token signed with the wrong key is rejected")
    void forgedTokenIsRejected() throws Exception {
        // Structurally a JWT, but signed with something else. Accepting this
        // would mean anyone could mint an admin session.
        String forged = "eyJhbGciOiJIUzI1NiJ9."
                + "eyJzdWIiOiJhZG1pbkB0b3VycGFja2FnZS5jb20iLCJyb2xlIjoiU1VQRVJfQURNSU4ifQ."
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

        mockMvc.perform(get("/admin/bookings").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("correct credentials return a usable token")
    void loginSucceeds() throws Exception {
        String token = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/admin/bookings").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("a wrong password is 401 and does not reveal whether the account exists")
    void wrongPasswordIsUnauthorized() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"definitely-wrong\"}".formatted(ADMIN_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String body = result.getResponse().getContentAsString().toLowerCase();
        // "no such user" versus "wrong password" is an account-enumeration oracle.
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain("not found");
    }

    @Test
    @DisplayName("an unknown account is 401, the same as a wrong password")
    void unknownAccountIsIndistinguishable() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\",\"password\":\"whatever\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a password is never echoed back in any response")
    void passwordIsNeverReflected() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andReturn();

        org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                .doesNotContain(ADMIN_PASSWORD);
    }

    @Test
    @DisplayName("forgot-password answers the same for a known and unknown address")
    void forgotPasswordDoesNotEnumerate() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(ADMIN_EMAIL)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("an error body never carries a stack trace")
    void errorsDoNotLeakInternals() throws Exception {
        mockMvc.perform(get("/admin/bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

}
