package ee.valiit.mystuffback.controller.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO for {@link ee.valiit.mystuffback.persistence.user.User}
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto implements Serializable {

    @NotBlank(message = "Username is required")
    @Size(message = "Username must be 3-20 charactes", min = 3, max = 20)
    private String username;

    @NotBlank
    @Size(message = "Password must be 8-50 characters", min = 8, max = 50)
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Password must contain at least one letter and one number"
    )
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Size(message = "Email must be at most 254 characters", max = 254)
    private String email;

    // Honeypot: must stay empty
    private String website;

    private String captchaToken;
}