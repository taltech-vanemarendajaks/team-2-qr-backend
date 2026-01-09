package ee.valiit.mystuffback.service;


import ee.valiit.mystuffback.controller.login.dto.LoginResponse;
import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserMapper;
import ee.valiit.mystuffback.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static ee.valiit.mystuffback.infrastructure.error.Error.INCORRECT_CREDENTIALS;


@Service
@RequiredArgsConstructor
public class LoginService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(String username, String password) {
        User user = getValidActiveUser(username.trim(), password);
        return userMapper.toLoginResponse(user);
    }

    private User getValidActiveUser(String username, String password) {
        User user = userRepository.findActiveUserByUsername(username)
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
