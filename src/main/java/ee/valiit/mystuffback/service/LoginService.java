package ee.valiit.mystuffback.service;


import ee.valiit.mystuffback.controller.login.dto.LoginResponse;
import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import ee.valiit.mystuffback.persistence.role.Role;
import ee.valiit.mystuffback.persistence.role.RoleRepository;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserMapper;
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
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public LoginResponse login(String email, String password) {
        User user = getValidActiveUser(email.trim(), password);
        return userMapper.toLoginResponse(user);
    }

    @Transactional
    public LoginResponse googleLogin(GoogleAuthService.GoogleUserInfo userInfo) {
        // 1. Look up by googleId
        User user = userRepository.findByGoogleId(userInfo.googleId()).orElse(null);

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
            user.setUsername(deriveUsername(userInfo.name()));
            user.setPassword(null);
            user.setStatus(ACTIVE.getCode());
            user.setRole(role);
            userRepository.save(user);
        }

        return userMapper.toLoginResponse(user);
    }

    private String deriveUsername(String name) {
        if (name == null || name.isBlank()) return "user";
        String firstName = name.trim().split("\\s+")[0];
        return firstName.length() > 50 ? firstName.substring(0, 50) : firstName;
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
