package ee.valiit.mystuffback.controller.support;

import ee.valiit.mystuffback.controller.support.dto.SupportRequestCreateRequest;
import ee.valiit.mystuffback.controller.support.dto.SupportVerifyRequest;
import ee.valiit.mystuffback.controller.support.dto.SupportVerifyResponse;
import ee.valiit.mystuffback.infrastructure.error.ApiError;
import ee.valiit.mystuffback.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/support")
public class SupportController {

    private final SupportService supportService;

    @PostMapping("/verify-qr")
    @Operation(
            summary = "Verify ownership using a QR token and issue a short-lived support token",
            description = "Used to unlock the 'contact admin' flow. Requires a valid QR token belonging to the user's item."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public SupportVerifyResponse verifyQr(@RequestBody @Valid SupportVerifyRequest request) {
        String supportToken = supportService.verifyOwnershipAndIssueToken(request);
        return new SupportVerifyResponse(supportToken);
    }
    @PostMapping("/request")
    @Operation(
            summary = "Create a support request (contact admin) using a short-lived support token",
            description = "Requires a valid support token issued by /api/support/verify-qr."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public void createSupportRequest(@RequestBody @Valid SupportRequestCreateRequest request) {
        supportService.createSupportRequest(request);
    }

}
