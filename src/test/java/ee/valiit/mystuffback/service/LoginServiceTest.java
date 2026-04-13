package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.persistence.role.Role;
import ee.valiit.mystuffback.persistence.role.RoleRepository;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserRepository;
import ee.valiit.mystuffback.service.GoogleAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LoginService.deriveDisplayName (private method tested through googleLogin).
 *
 * deriveDisplayName replaced the old deriveUniqueUsername: it no longer checks for collisions
 * or appends numeric suffixes — it just derives a display name from the provided name or email.
 */
@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RoleRepository roleRepository;

    @InjectMocks LoginService loginService;

    @BeforeEach
    void mockNewUserFlow() {
        // All tests create a brand-new Google user (no existing user in DB)
        when(userRepository.findActiveUserByGoogleId(any())).thenReturn(Optional.empty());
        when(userRepository.findActiveUserByEmail(any())).thenReturn(Optional.empty());

        Role customerRole = new Role();
        customerRole.setId(2);
        customerRole.setName("customer");
        when(roleRepository.getRoleBy("customer")).thenReturn(Optional.of(customerRole));

        when(passwordEncoder.encode(any())).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private User googleLoginWith(String googleId, String email, String name) {
        return loginService.googleLogin(
                new GoogleAuthService.GoogleUserInfo(googleId, email, name));
    }

    @Test
    void usesFirstWordOfNameWhenNameIsProvided() {
        User result = googleLoginWith("gid1", "anna@example.com", "Anna Karenina");
        assertThat(result.getUsername()).isEqualTo("Anna");
    }

    @Test
    void usesEmailPrefixWhenNameIsNull() {
        User result = googleLoginWith("gid2", "john.doe@example.com", null);
        assertThat(result.getUsername()).isEqualTo("john.doe");
    }

    @Test
    void usesEmailPrefixWhenNameIsBlank() {
        User result = googleLoginWith("gid3", "jane@example.com", "   ");
        assertThat(result.getUsername()).isEqualTo("jane");
    }

    @Test
    void fallsBackToLiteralUser_whenBothNameAndEmailAreNull() {
        User result = googleLoginWith("gid4", null, null);
        assertThat(result.getUsername()).isEqualTo("user");
    }

    @Test
    void truncatesDisplayNameTo50Characters() {
        String longName = "A".repeat(60);
        User result = googleLoginWith("gid5", "x@example.com", longName);
        assertThat(result.getUsername()).hasSize(50);
    }

    @Test
    void doesNotAppendNumericSuffix_whenUsernameWouldConflict() {
        // Before this refactor, deriveUniqueUsername would check the DB and append "-2", "-3" etc.
        // Now username is just a display name — no collision handling, no suffix.
        // @BeforeEach already stubs these with any() — no overrides needed here

        User first  = googleLoginWith("gidA", "a@example.com", "Maria");
        User second = googleLoginWith("gidB", "b@example.com", "Maria");

        // Both get exactly "Maria" — no "-2" suffix on the second
        assertThat(first.getUsername()).isEqualTo("Maria");
        assertThat(second.getUsername()).isEqualTo("Maria");
    }
}
