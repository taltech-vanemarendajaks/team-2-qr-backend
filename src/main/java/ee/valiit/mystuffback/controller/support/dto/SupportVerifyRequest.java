package ee.valiit.mystuffback.controller.support.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupportVerifyRequest {

    // user enters this (case-sensitive username)
    @NotBlank(message = "Username is required")
    private String username;

    // token from QR URL: ...&t=XXXX
    @NotBlank(message = "QR token is required")
    private String qrToken;

    // Honeypot: must stay empty
    private String website;
}
