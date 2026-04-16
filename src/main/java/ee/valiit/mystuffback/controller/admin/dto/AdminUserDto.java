package ee.valiit.mystuffback.controller.admin.dto;

import java.time.OffsetDateTime;

public record AdminUserDto(
        Integer id,
        String username,
        String email,
        String status,
        String role,
        String authProvider,
        OffsetDateTime createdAt,
        OffsetDateTime lastLoginAt,
        Long itemCount
) {}
