package ee.valiit.mystuffback.controller.login;

import ee.valiit.mystuffback.controller.login.dto.GoogleLoginRequest;
import ee.valiit.mystuffback.controller.login.dto.LoginRequest;
import ee.valiit.mystuffback.controller.login.dto.LoginResponse;
import ee.valiit.mystuffback.infrastructure.error.ApiError;
import ee.valiit.mystuffback.service.GoogleAuthService;
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
    private final GoogleAuthService googleAuthService;

    private String getClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    @PostMapping("/login")
    @Operation(
            summary = "Log in",
            description = """
                    Authenticates user with email and password.
                    Only active users can log in.
                    Returns user id and role name.
                    If credentials are invalid, returns errorCode 111.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "403", description = "Email or password incorrect",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        rateLimitService.checkRateLimitOrThrow("login", getClientIp(httpRequest), 10, 60);
        return loginService.login(request.getEmail(), request.getPassword());
    }

    @PostMapping("/google")
    @Operation(summary = "Google Sign-In", description = "Authenticates or auto-creates a user via Google ID token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "403", description = "Invalid Google token",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public LoginResponse googleLogin(@Valid @RequestBody GoogleLoginRequest request, HttpServletRequest httpRequest) {
        rateLimitService.checkRateLimitOrThrow("google-login", getClientIp(httpRequest), 10, 60);
        GoogleAuthService.GoogleUserInfo userInfo = googleAuthService.verify(request.getIdToken());
        return loginService.googleLogin(userInfo);
    }
}
