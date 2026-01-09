package ee.valiit.mystuffback.controller.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "LoginRequest", description = "Login credentials for authenticating a user.")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username must be at most 50 characters")
    @Schema(description = "User's username", example = "katharina")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 50, message = "Password must be at most 50 characters")
    @Schema(description = "User's password", example = "Secret123!")
    private String password;
}
