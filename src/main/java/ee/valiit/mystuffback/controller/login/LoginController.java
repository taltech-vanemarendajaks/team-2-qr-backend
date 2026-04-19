package ee.valiit.mystuffback.controller.login;

import ee.valiit.mystuffback.controller.login.dto.ChangePasswordRequest;
import ee.valiit.mystuffback.controller.login.dto.ForgotPasswordRequest;
import ee.valiit.mystuffback.controller.login.dto.GoogleLoginRequest;
import ee.valiit.mystuffback.controller.login.dto.LoginRequest;
import ee.valiit.mystuffback.controller.login.dto.LoginResponse;
import ee.valiit.mystuffback.controller.login.dto.ResetPasswordRequest;
import ee.valiit.mystuffback.infrastructure.error.ApiError;
import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserMapper;
import ee.valiit.mystuffback.service.GoogleAuthService;
import ee.valiit.mystuffback.service.LoginService;
import ee.valiit.mystuffback.service.PasswordResetService;
import ee.valiit.mystuffback.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class LoginController {

    private final LoginService loginService;
    private final RateLimitService rateLimitService;
    private final GoogleAuthService googleAuthService;
    private final UserMapper userMapper;
    private final PasswordResetService passwordResetService;

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
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "Too many login attempts",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })

    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String clientIp = getClientIp(httpRequest);

        rateLimitService.checkRateLimitOrThrow("login:" + normalizedEmail, clientIp, 3, 30);

        User user = loginService.login(request.getEmail(), request.getPassword());
        establishSession(user, httpRequest);
        return userMapper.toLoginResponse(user);
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
        User user = loginService.googleLogin(userInfo);
        establishSession(user, httpRequest);
        return userMapper.toLoginResponse(user);
    }

    private void establishSession(User user, HttpServletRequest request) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );
    }
    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request password reset",
            description = """
                    Accepts an email address. If an active account exists, sends a password reset link.
                    Always returns 200 — never reveals whether the email is registered.
                    Rate limited to 3 requests per 5 minutes per email address.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Request processed"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                               HttpServletRequest httpRequest) {
        rateLimitService.checkRateLimitOrThrow(
                "forgot-password:" + request.getEmail().trim().toLowerCase(),
                getClientIp(httpRequest), 3, 300);
        passwordResetService.requestPasswordReset(request.getEmail());
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "Reset password",
            description = "Accepts a reset token and a new password. Token must be valid, unexpired, and unused."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token (errorCode 551)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                              HttpServletRequest httpRequest) {
        rateLimitService.checkRateLimitOrThrow(
                "reset-password", getClientIp(httpRequest), 10, 60);
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
    }

    @PostMapping("/change-password")
    @Operation(
            summary = "Change password",
            description = "Changes password for the currently authenticated user. Requires correct current password."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Incorrect current password (errorCode 552)",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
                               HttpServletRequest httpRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth.getPrincipal() instanceof User user)) {
            throw new ForbiddenException("Access denied", 403);
        }
        rateLimitService.checkRateLimitOrThrow(
                "change-password:" + user.getId(), getClientIp(httpRequest), 5, 60);
        loginService.changePassword(user.getId(), request.getCurrentPassword(), request.getNewPassword());
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out", description = "Ends the current session.")
    @ApiResponse(responseCode = "200", description = "Logout successful")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, null);
    }

    @GetMapping("/me")
    @Operation(summary = "Current logged-in user", description = "Returns the currently authenticated user from session, or 204 if not authenticated.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authenticated user returned"),
            @ApiResponse(responseCode = "204", description = "Not authenticated")
    })
    public ResponseEntity<LoginResponse> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(userMapper.toLoginResponse(user));
    }

}
