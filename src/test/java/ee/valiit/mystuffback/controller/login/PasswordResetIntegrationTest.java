package ee.valiit.mystuffback.controller.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.valiit.mystuffback.AbstractIntegrationTest;
import ee.valiit.mystuffback.controller.login.dto.ForgotPasswordRequest;
import ee.valiit.mystuffback.controller.login.dto.ResetPasswordRequest;
import ee.valiit.mystuffback.persistence.passwordreset.PasswordResetToken;
import ee.valiit.mystuffback.persistence.passwordreset.PasswordResetTokenRepository;
import ee.valiit.mystuffback.persistence.role.RoleRepository;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserRepository;
import io.mailtrap.client.MailtrapClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the password reset flow.
 *
 * MailtrapClient is mocked via @MockBean so no real emails are sent.
 * Tests cover the full token lifecycle: request, use, expiry, and reuse prevention.
 */
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class PasswordResetIntegrationTest extends AbstractIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordResetTokenRepository tokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @MockitoBean
    MailtrapClient mailtrapClient;

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    User testUser;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        var customerRole = roleRepository.getRoleBy("customer").orElseThrow();

        testUser = new User();
        testUser.setUsername("resetuser");
        testUser.setEmail("reset@example.com");
        testUser.setPassword(passwordEncoder.encode("OldPassword1"));
        testUser.setStatus("A");
        testUser.setRole(customerRole);
        userRepository.save(testUser);
    }

    // ── forgot-password endpoint ────────────────────────────────────────────────

    @Test
    void forgotPassword_withRegisteredEmail_returns200AndSendsEmail() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest("reset@example.com"))))
                .andExpect(status().isOk());

        verify(mailtrapClient, times(1)).send(any());
    }

    @Test
    void forgotPassword_withUnknownEmail_returns200AndDoesNotSendEmail() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest("nobody@example.com"))))
                .andExpect(status().isOk());

        verify(mailtrapClient, never()).send(any());
    }

    @Test
    void forgotPassword_withInvalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    // ── reset-password endpoint ─────────────────────────────────────────────────

    @Test
    void resetPassword_withValidToken_returns200AndChangesPassword() throws Exception {
        String token = saveToken(testUser, Instant.now().plus(1, ChronoUnit.HOURS), false);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest(token, "NewPassword1"))))
                .andExpect(status().isOk());

        User updated = userRepository.findActiveUserByEmail("reset@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("NewPassword1", updated.getPassword())).isTrue();

        PasswordResetToken usedToken = tokenRepository.findByToken(token).orElseThrow();
        assertThat(usedToken.isUsed()).isTrue();
    }

    @Test
    void resetPassword_withExpiredToken_returns400WithCode551() throws Exception {
        String token = saveToken(testUser, Instant.now().minus(1, ChronoUnit.HOURS), false);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest(token, "NewPassword1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(551));
    }

    @Test
    void resetPassword_withUsedToken_returns400WithCode551() throws Exception {
        String token = saveToken(testUser, Instant.now().plus(1, ChronoUnit.HOURS), true);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest(token, "NewPassword1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(551));
    }

    @Test
    void resetPassword_withNonExistentToken_returns400WithCode551() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest(UUID.randomUUID().toString(), "NewPassword1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(551));
    }

    @Test
    void resetPassword_tokenCannotBeReused() throws Exception {
        String token = saveToken(testUser, Instant.now().plus(1, ChronoUnit.HOURS), false);

        // First use succeeds
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest(token, "NewPassword1"))))
                .andExpect(status().isOk());

        // Second use fails
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest(token, "AnotherPassword1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(551));
    }

    @Test
    void resetPassword_withBlankToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\": \"\", \"newPassword\": \"ValidPass1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPassword_withShortPassword_returns400() throws Exception {
        String token = saveToken(testUser, Instant.now().plus(1, ChronoUnit.HOURS), false);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest(token, "short"))))
                .andExpect(status().isBadRequest());
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private String saveToken(User user, Instant expiresAt, boolean used) {
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiresAt(expiresAt);
        resetToken.setUsed(used);
        tokenRepository.save(resetToken);
        return token;
    }

    private ForgotPasswordRequest forgotRequest(String email) {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail(email);
        return req;
    }

    private ResetPasswordRequest resetRequest(String token, String newPassword) {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken(token);
        req.setNewPassword(newPassword);
        return req;
    }
}
