package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.infrastructure.exception.PrimaryKeyNotFoundException;
import ee.valiit.mystuffback.persistence.item.Item;
import ee.valiit.mystuffback.persistence.item.ItemRepository;
import ee.valiit.mystuffback.persistence.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class QrCodeService {

    private final ItemRepository itemRepository;
    private final UserService userService;

    @Value("${mystuff.server.address}")
    private String serverAddress;

    @Value("${mystuff.item.path}")
    private String itemPath;

    public String getQrCode(Integer itemId) {
        User user = userService.getAuthenticatedUser();
        Item item = itemRepository.findActiveItemByIdAndUserId(itemId, user.getId())
                .orElseThrow(() -> new PrimaryKeyNotFoundException("itemId", itemId));
        return constructImageQrLink(item);
    }

    private String constructImageQrLink(Item item) {
        String token = item.getQrToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("QR token missing for itemId=" + item.getId());
        }
        return serverAddress + itemPath + item.getId() + "&t=" + token;
    }

}
