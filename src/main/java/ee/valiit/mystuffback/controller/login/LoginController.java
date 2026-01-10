package ee.valiit.mystuffback.controller.login;

import ee.valiit.mystuffback.controller.login.dto.LoginRequest;
import ee.valiit.mystuffback.controller.login.dto.LoginResponse;
import ee.valiit.mystuffback.infrastructure.error.ApiError;
import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import ee.valiit.mystuffback.service.LoginService;
import ee.valiit.mystuffback.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class LoginController {

    private final LoginService loginService;
    private final RateLimitService rateLimitService;

    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in",
            description = """
                    Authenticates a user with username and password.
                    Only active users can log in.
                    Returns user id and role name.
                    If credentials are invalid, returns errorCode 111.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "403", description = "Username or password incorrect",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        rateLimitService.checkRateLimitOrThrow("login", getClientIp(httpRequest), 10, 60);

        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            throw new ForbiddenException("Access denied", 403);
        }

        return loginService.login(request.getUsername(), request.getPassword());
    }
}
