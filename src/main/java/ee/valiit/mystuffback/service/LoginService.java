package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import ee.valiit.mystuffback.persistence.role.Role;
import ee.valiit.mystuffback.persistence.role.RoleRepository;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static ee.valiit.mystuffback.infrastructure.error.Error.INCORRECT_CREDENTIALS;
import static ee.valiit.mystuffback.service.UserService.CUSTOMER_ROLE_NAME;
import static ee.valiit.mystuffback.infrastructure.status.Status.ACTIVE;


@Service
@RequiredArgsConstructor
public class LoginService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public User login(String email, String password) {
        User user = getValidActiveUser(email.trim(), password);
        user.getRole().getName();
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
            Role role = roleRepository.getRoleBy(CUSTOMER_ROLE_NAME);
            user = new User();
            user.setGoogleId(userInfo.googleId());
            user.setEmail(userInfo.email());
            user.setUsername(deriveUniqueUsername(userInfo.name(), userInfo.email()));
            user.setPassword(passwordEncoder.encode("GOOGLE_AUTH_ONLY_" + userInfo.googleId()));
            user.setStatus(ACTIVE.getCode());
            user.setRole(role);
            userRepository.save(user);
        }
        user.getRole().getName();
        return user;
    }

    private String deriveUniqueUsername(String name, String email) {
        String base;

        if (name != null && !name.isBlank()) {
            base = name.trim().split("\\s+")[0];
        } else if (email != null && !email.isBlank()) {
            base = email.split("@")[0];
        } else {
            base = "user";
        }

        base = base.trim();
        if (base.isBlank()) {
            base = "user";
        }

        if (base.length() > 50) {
            base = base.substring(0, 50);
        }

        String candidate = base;
        int counter = 1;

        while (userRepository.usernameExistsBy(candidate)) {
            String suffix = String.valueOf(counter);
            int maxBaseLength = 50 - suffix.length();
            String shortenedBase = base.length() > maxBaseLength
                    ? base.substring(0, maxBaseLength)
                    : base;

            candidate = shortenedBase + suffix;
            counter++;
        }

        return candidate;
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
}
