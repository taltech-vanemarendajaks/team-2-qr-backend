package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.controller.user.dto.UserDto;
import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import ee.valiit.mystuffback.infrastructure.exception.PrimaryKeyNotFoundException;
import ee.valiit.mystuffback.persistence.role.Role;
import ee.valiit.mystuffback.persistence.role.RoleRepository;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserMapper;
import ee.valiit.mystuffback.persistence.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static ee.valiit.mystuffback.infrastructure.error.Error.EMAIL_UNAVAILABLE;
import static ee.valiit.mystuffback.infrastructure.error.Error.USERNAME_UNAVAILABLE;
import static ee.valiit.mystuffback.infrastructure.status.Status.ACTIVE;

@Service
@RequiredArgsConstructor
public class UserService {
    public static final int CUSTOMER_ROLE_ID = 2;
    public static final String CUSTOMER_ROLE_NAME = "customer";
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;


    @Transactional
    public void addUser(@Valid UserDto userDto) {
        String username = userDto.getUsername().trim();
        String email = userDto.getEmail().trim();

        validateUserNameIsAvailable(username);
        validateEmailIsAvailable(email);

        Role role = roleRepository.getRoleBy(CUSTOMER_ROLE_NAME);
        User user = userMapper.toUser(userDto);

        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRole(role);
        user.setStatus(ACTIVE.getCode());

        userRepository.save(user);
    }

    public User getValidUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new PrimaryKeyNotFoundException("userId", userId));
    }

    private void validateUserNameIsAvailable(String username) {
        boolean usernameExists = userRepository.usernameExistsBy(username);
        if (usernameExists) {
            throw new ForbiddenException(USERNAME_UNAVAILABLE.getMessage(), USERNAME_UNAVAILABLE.getErrorCode());
        }
    }

    private void validateEmailIsAvailable(String email) {
        boolean emailExists = userRepository.emailExistsBy(email);
        if (emailExists) {
            throw new ForbiddenException(EMAIL_UNAVAILABLE.getMessage(), EMAIL_UNAVAILABLE.getErrorCode());
        }
    }


}