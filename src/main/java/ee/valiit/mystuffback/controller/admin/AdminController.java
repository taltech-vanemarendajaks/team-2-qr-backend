package ee.valiit.mystuffback.controller.admin;

import ee.valiit.mystuffback.controller.admin.dto.AdminUserDto;
import ee.valiit.mystuffback.service.AdminService;
import ee.valiit.mystuffback.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    @GetMapping("/users")
    public List<AdminUserDto> getAllUsers() {
        return adminService.getAllUsersWithStats(userService.getAuthenticatedUser());
    }
}
