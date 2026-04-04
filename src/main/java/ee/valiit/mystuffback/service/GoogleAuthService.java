package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GoogleAuthService {

    @Value("${google.client-id}")
    private String googleClientId;

    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleUserInfo verify(String idToken) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        TokenInfoResponse response;
        try {
            response = restTemplate.getForObject(url, TokenInfoResponse.class);
        } catch (Exception e) {
            throw new ForbiddenException("Invalid Google token", 403);
        }

        if (response == null || response.getSub() == null) {
            throw new ForbiddenException("Invalid Google token", 403);
        }

        if (!googleClientId.equals(response.getAud())) {
            throw new ForbiddenException("Google token audience mismatch", 403);
        }

        return new GoogleUserInfo(response.getSub(), response.getEmail(), response.getName());
    }

    public record GoogleUserInfo(String googleId, String email, String name) {}

    @Data
    public static class TokenInfoResponse {
        private String sub;
        private String email;
        private String name;
        private String aud;
    }

}
