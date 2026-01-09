package ee.valiit.mystuffback.controller.support.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SupportVerifyResponse {
    private String supportToken;
}
