package ee.valiit.mystuffback.controller.login.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "LoginRequest", description = "Login credentials for authenticating a user.")
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 254, message = "Email must be at most 254 characters")
    @Schema(description = "User's email", example = "katharina@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(max = 255, message = "Password must be at most 255 characters")
    @Schema(description = "User's password", example = "Secret123!")
    private String password;
}
