package ee.valiit.mystuffback.controller.login.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
