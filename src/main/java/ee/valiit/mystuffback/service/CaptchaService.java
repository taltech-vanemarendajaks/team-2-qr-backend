package ee.valiit.mystuffback.service;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class CaptchaService {

    @Value("${hcaptcha.secret}")
    private String hcaptchaSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verify(String token) {
        if (token == null || token.isBlank()) return false;

        String url = "https://hcaptcha.com/siteverify";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", hcaptchaSecret);
        form.add("response", token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<HcaptchaResponse> response =
                restTemplate.postForEntity(url, request, HcaptchaResponse.class);

        return response.getBody() != null && Boolean.TRUE.equals(response.getBody().getSuccess());
    }

    @Data
    public static class HcaptchaResponse {
        private Boolean success;
    }
}
