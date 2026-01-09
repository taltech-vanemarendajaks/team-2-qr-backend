package ee.valiit.mystuffback.service;


import ee.valiit.mystuffback.controller.login.dto.LoginResponse;
import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserMapper;
import ee.valiit.mystuffback.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static ee.valiit.mystuffback.infrastructure.error.Error.INCORRECT_CREDENTIALS;


@Service
@RequiredArgsConstructor
public class LoginService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

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

        if (!password.equals(user.getPassword())) {
            throw new ForbiddenException(
                    INCORRECT_CREDENTIALS.getMessage(),
                    INCORRECT_CREDENTIALS.getErrorCode()
            );
        }

        return user;
    }

}
