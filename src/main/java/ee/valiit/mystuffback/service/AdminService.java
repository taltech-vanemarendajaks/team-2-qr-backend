package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.controller.admin.dto.AdminUserDto;
import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    public List<AdminUserDto> getAllUsersWithStats(User requestingUser) {
        if (!"admin".equals(requestingUser.getRole().getName())) {
            throw new ForbiddenException("Access denied", 403);
        }
        return userRepository.findAllUsersWithStats();
    }
}
