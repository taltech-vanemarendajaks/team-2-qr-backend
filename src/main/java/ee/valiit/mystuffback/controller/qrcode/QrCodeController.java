package ee.valiit.mystuffback.controller.qrcode;

import ee.valiit.mystuffback.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QrCodeController {

    private final QrCodeService qrCodeService;

    @GetMapping("/qr-code")
    public String getQrCode(@RequestParam Integer itemId) {
        return qrCodeService.getQrCode(itemId);
    }

}
