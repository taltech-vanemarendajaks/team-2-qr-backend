package ee.valiit.mystuffback.controller.user;

import ee.valiit.mystuffback.controller.user.dto.UserDto;
import ee.valiit.mystuffback.infrastructure.error.ApiError;
import ee.valiit.mystuffback.service.RateLimitService;
import ee.valiit.mystuffback.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;
    private final RateLimitService rateLimitService;

    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    @PostMapping("/signup")
    @Operation(summary = "New user account creation", description = "all fields are mandatory")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "403", description = "This email already exists(errorCode: 223)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> addUser(@RequestBody @Valid UserDto userDto, HttpServletRequest httpRequest) {
        rateLimitService.checkRateLimitOrThrow("signup", getClientIp(httpRequest), 5, 60);
        userService.addUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
