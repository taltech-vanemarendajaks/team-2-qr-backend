package ee.valiit.mystuffback.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ee.valiit.mystuffback.AbstractIntegrationTest;
import ee.valiit.mystuffback.controller.user.dto.UserDto;
import ee.valiit.mystuffback.persistence.role.RoleRepository;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for user registration constraints.
 *
 * Covers:
 * - Duplicate email is rejected with errorCode 223
 * - Duplicate username is now allowed (username is a display name, not a unique key)
 */
@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class UserRegistrationTest extends AbstractIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        var customerRole = roleRepository.getRoleBy("customer").orElseThrow();

        var existingUser = new User();
        existingUser.setUsername("existinguser");
        existingUser.setEmail("taken@example.com");
        existingUser.setPassword("somePassword1");
        existingUser.setStatus("A");
        existingUser.setRole(customerRole);
        userRepository.save(existingUser);
    }

    private String signupJson(String username, String password, String email) throws Exception {
        return objectMapper.writeValueAsString(new UserDto(username, password, email));
    }

    @Test
    void signup_withDuplicateEmail_isForbidden_withErrorCode223() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("newuser", "Password1", "taken@example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value(223));
    }

    @Test
    void signup_withDuplicateUsername_succeeds() throws Exception {
        // username is no longer a unique constraint — same display name is allowed
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("existinguser", "Password1", "other@example.com")))
                .andExpect(status().isCreated());
    }

    @Test
    void signup_withUniqueEmail_succeeds() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("brandnew", "Password1", "brandnew@example.com")))
                .andExpect(status().isCreated());
    }
}
