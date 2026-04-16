package ee.valiit.mystuffback.controller.admin;

import ee.valiit.mystuffback.controller.admin.dto.AdminUserDto;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.service.AdminService;
import ee.valiit.mystuffback.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    @GetMapping("/users")
    @Operation(summary = "Get all users with stats", description = "Admin only. Returns all users with item count, last login, registration source and created date.")
    public List<AdminUserDto> getAllUsers() {
        User requestingUser = userService.getAuthenticatedUser();
        return adminService.getAllUsersWithStats(requestingUser);
    }
}
