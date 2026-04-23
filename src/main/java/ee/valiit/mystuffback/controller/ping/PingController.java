package ee.valiit.mystuffback.controller.ping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    @Operation(summary = "CSRF seed", description = "Public endpoint that triggers the XSRF-TOKEN cookie to be set. Call this once on app load before any mutating requests.")
    @ApiResponse(responseCode = "204", description = "No content — cookie set")
    public ResponseEntity<Void> ping(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        return ResponseEntity.noContent().build();
    }
}
