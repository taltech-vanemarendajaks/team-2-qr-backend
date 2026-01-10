package ee.valiit.mystuffback.controller.user;

import ee.valiit.mystuffback.controller.user.dto.UserDto;
import ee.valiit.mystuffback.infrastructure.error.ApiError;
import ee.valiit.mystuffback.service.UserService;
import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/api/auth/signup")
    @Operation(summary = "New user account creation", description = "all fields are mandatory")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "403", description = "Account with this name already exists (errorCode: 222)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public void addUser(@RequestBody @Valid UserDto userDto) {
        if (userDto.getWebsite() != null && !userDto.getWebsite().isBlank()) {
            throw new ForbiddenException("Access denied", 403);
        }
        userService.addUser(userDto);
    }

}