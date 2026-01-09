package ee.valiit.mystuffback.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ee.valiit.mystuffback.infrastructure.exception.PrimaryKeyNotFoundException;
import ee.valiit.mystuffback.persistence.item.Item;
import ee.valiit.mystuffback.persistence.item.ItemRepository;


@Service
@RequiredArgsConstructor
public class QrCodeService {

    private final ItemRepository itemRepository;
    @Value("${mystuff.server.address}")
    private String serverAddress;

    @Value("${mystuff.item.path}")
    private String itemPath;

    public String getQrCode(Integer itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(()-> new PrimaryKeyNotFoundException("itemId",itemId));
        return constructImageQrLink(item);
    }

    private String constructImageQrLink(Item item) {

        return serverAddress + itemPath + item.getId() + "&t=" + item.getQrToken();
    }
}
