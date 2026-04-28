package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.infrastructure.exception.DataNotFoundException;
import ee.valiit.mystuffback.persistence.item.Item;
import ee.valiit.mystuffback.persistence.item.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarrantyNotificationService {

    private final ItemRepository itemRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void triggerWarrantyNotifications() {
        List<Item> due = itemRepository.findItemsDueForWarrantyNotification(Instant.now());
        log.info("Warranty notification check: {} item(s) due", due.size());
        for (Item item : due) {
            emailService.sendWarrantyExpirationEmail(
                    item.getUser().getEmail(),
                    item.getName(),
                    item.getWarrantyEndDate(),
                    item.getId()
            );
            item.setWarrantyNotifyAt(null);
        }
    }

    @Transactional
    public void triggerItemNotification(Integer itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new DataNotFoundException("Item not found: " + itemId, 404));
        emailService.sendWarrantyExpirationEmail(
                item.getUser().getEmail(),
                item.getName(),
                item.getWarrantyEndDate(),
                item.getId()
        );
        item.setWarrantyNotifyAt(null);
    }
}
