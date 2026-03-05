package ee.valiit.mystuffback.controller.support.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SupportVerifyRequest {

    // user enters this (case-sensitive username)
    @NotBlank(message = "Username is required")
    private String username;

    // user enters email (email to be checked to match the one in DB)
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    // token from QR URL: ...&t=XXXX
    @NotBlank(message = "QR token is required")
    private String qrToken;

}
