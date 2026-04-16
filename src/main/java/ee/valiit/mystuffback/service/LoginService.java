package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.infrastructure.exception.BadRequestException;
import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import ee.valiit.mystuffback.persistence.role.Role;
import ee.valiit.mystuffback.persistence.role.RoleRepository;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static ee.valiit.mystuffback.infrastructure.error.Error.INCORRECT_CREDENTIALS;
import static ee.valiit.mystuffback.infrastructure.error.Error.INCORRECT_CURRENT_PASSWORD;
import static ee.valiit.mystuffback.service.UserService.CUSTOMER_ROLE_NAME;
import static ee.valiit.mystuffback.infrastructure.status.Status.ACTIVE;


@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Transactional
    public User login(String email, String password) {
        User user = getValidActiveUser(email.trim(), password);
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);
        user.getRole().getName(); // force lazy-load role before entity detaches from session
        return user;
    }

    @Transactional
    public User googleLogin(GoogleAuthService.GoogleUserInfo userInfo) {
        // 1. Look up by googleId
        User user = userRepository.findActiveUserByGoogleId(userInfo.googleId()).orElse(null);

        if (user == null) {
            // 2. Look up by email — link googleId if found
            user = userRepository.findActiveUserByEmail(userInfo.email()).orElse(null);
            if (user != null) {
                user.setGoogleId(userInfo.googleId());
                userRepository.save(user);
            }
        }

        if (user == null) {
            // 3. Auto-create user
            Role role = roleRepository.getRoleBy(CUSTOMER_ROLE_NAME)
                    .orElseThrow(() -> new IllegalStateException("Role not found: " + CUSTOMER_ROLE_NAME));
            user = new User();
            user.setGoogleId(userInfo.googleId());
            user.setEmail(userInfo.email());
            user.setUsername(deriveDisplayName(userInfo.name(), userInfo.email()));
            user.setPassword(passwordEncoder.encode("GOOGLE_AUTH_ONLY_" + userInfo.googleId()));
            user.setStatus(ACTIVE.getCode());
            user.setRole(role);
            user.setCreatedAt(OffsetDateTime.now());
            user.setAuthProvider("GOOGLE");
            userRepository.save(user);
        }
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);
        user.getRole().getName(); // force lazy-load role before entity detaches from session
        return user;
    }

    private String deriveDisplayName(String name, String email) {
        String displayName;

        if (name != null && !name.isBlank()) {
            displayName = name.trim().split("\\s+")[0];
        } else if (email != null && !email.isBlank()) {
            displayName = email.split("@")[0].trim();
        } else {
            displayName = "user";
        }

        if (displayName.isBlank()) {
            displayName = "user";
        }

        if (displayName.length() > 50) {
            displayName = displayName.substring(0, 50);
        }
        return displayName;
    }

    private User getValidActiveUser(String email, String password) {
        User user = userRepository.findActiveUserByEmail(email)
                .orElseThrow(() -> new ForbiddenException(
                        INCORRECT_CREDENTIALS.getMessage(),
                        INCORRECT_CREDENTIALS.getErrorCode()
                ));

        String storedPassword = user.getPassword();
        if (storedPassword == null) {
            throw new ForbiddenException(
                    INCORRECT_CREDENTIALS.getMessage(),
                    INCORRECT_CREDENTIALS.getErrorCode()
            );
        }

        boolean ok;

        // If stored password already looks like bcrypt => verify with bcrypt
        if (looksLikeBcrypt(storedPassword)) {
            ok = passwordEncoder.matches(password, storedPassword);
        } else {
            // legacy plaintext support
            ok = password.equals(storedPassword);

            // if legacy login succeeds => upgrade to bcrypt automatically
            if (ok) {
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
            }
        }

        if (!ok) {
            throw new ForbiddenException(
                    INCORRECT_CREDENTIALS.getMessage(),
                    INCORRECT_CREDENTIALS.getErrorCode()
            );
        }

        return user;
    }

    private boolean looksLikeBcrypt(String s) {
        return s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$");
    }

    @Transactional
    public void changePassword(Integer userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("User not found", 403));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException(
                    INCORRECT_CURRENT_PASSWORD.getMessage(),
                    INCORRECT_CURRENT_PASSWORD.getErrorCode());
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user id={}", userId);
    }
}
