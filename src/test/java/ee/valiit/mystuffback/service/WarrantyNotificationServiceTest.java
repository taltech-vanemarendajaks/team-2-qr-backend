package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.AbstractIntegrationTest;
import ee.valiit.mystuffback.infrastructure.exception.DataNotFoundException;
import ee.valiit.mystuffback.persistence.item.Item;
import ee.valiit.mystuffback.persistence.item.ItemRepository;
import ee.valiit.mystuffback.persistence.role.RoleRepository;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserRepository;
import io.mailtrap.client.MailtrapClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Sql(scripts = "classpath:cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class WarrantyNotificationServiceTest extends AbstractIntegrationTest {

    @Autowired WarrantyNotificationService warrantyNotificationService;
    @Autowired ItemRepository itemRepository;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;

    @MockitoBean
    MailtrapClient mailtrapClient;

    User testUser;

    @BeforeEach
    void setUp() {
        var customerRole = roleRepository.getRoleBy("customer").orElseThrow();

        testUser = new User();
        testUser.setUsername("warrantyuser");
        testUser.setEmail("warranty@test.com");
        testUser.setPassword("irrelevant");
        testUser.setStatus("A");
        testUser.setRole(customerRole);
        userRepository.save(testUser);
    }

    private Item createItem(String name, Instant warrantyNotifyAt) {
        Item item = new Item();
        item.setName(name);
        item.setDate(LocalDate.now());
        item.setStatus("A");
        item.setUser(testUser);
        item.setQrToken(UUID.randomUUID().toString().replace("-", ""));
        item.setWarrantyEndDate(LocalDate.now().plusDays(7));
        item.setWarrantyNotifyAt(warrantyNotifyAt);
        return itemRepository.save(item);
    }

    @Test
    void triggerWarrantyNotifications_pastNotifyAt_sendsEmailAndClearsTimestamp() throws Exception {
        Item item = createItem("Fridge", Instant.now().minus(1, ChronoUnit.HOURS));

        warrantyNotificationService.triggerWarrantyNotifications();

        verify(mailtrapClient, times(1)).send(any());
        Item updated = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(updated.getWarrantyNotifyAt()).isNull();
    }

    @Test
    void triggerWarrantyNotifications_futureNotifyAt_skipsItem() throws Exception {
        createItem("Oven", Instant.now().plus(1, ChronoUnit.DAYS));

        warrantyNotificationService.triggerWarrantyNotifications();

        verify(mailtrapClient, never()).send(any());
    }

    @Test
    void triggerWarrantyNotifications_nullNotifyAt_skipsItem() throws Exception {
        createItem("Microwave", null);

        warrantyNotificationService.triggerWarrantyNotifications();

        verify(mailtrapClient, never()).send(any());
    }

    @Test
    void triggerItemNotification_unknownId_throwsDataNotFoundException() {
        assertThatThrownBy(() -> warrantyNotificationService.triggerItemNotification(999999))
                .isInstanceOf(DataNotFoundException.class);
    }
}
