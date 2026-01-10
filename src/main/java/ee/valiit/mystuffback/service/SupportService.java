package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.controller.support.dto.SupportRequestCreateRequest;
import ee.valiit.mystuffback.controller.support.dto.SupportVerifyRequest;
import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
import ee.valiit.mystuffback.persistence.item.Item;
import ee.valiit.mystuffback.persistence.item.ItemRepository;
import ee.valiit.mystuffback.persistence.support.SupportRequest;
import ee.valiit.mystuffback.persistence.support.SupportRequestRepository;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SupportService {

    private static final int SUPPORT_TOKEN_TTL_SECONDS = 10 * 60; // 10 minutes
    private static final String ACCESS_DENIED = "Access denied";

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final SupportRequestRepository supportRequestRepository;

    // In-memory token store: supportToken -> (username, expiresAtEpochSeconds)
    private record SupportTokenData(String username, long expiresAtEpochSeconds) {}
    private final Map<String, SupportTokenData> supportTokens = new ConcurrentHashMap<>();

    public String verifyOwnershipAndIssueToken(SupportVerifyRequest request) {
        denyIfHoneypotFilled(request.getWebsite());

        String username = requireTrimmed(request.getUsername());
        String email = requireTrimmed(request.getEmail());
        String qrToken = requireTrimmed(request.getQrToken());

        // 1) Find active user by username (case-sensitive username)
        User user = userRepository.findActiveUserByUsername(username)
                .orElseThrow(() -> forbidden());

        // 2) Email must match the user's email in DB (case-insensitive)
        if (user.getEmail() == null || !user.getEmail().trim().equalsIgnoreCase(email)) {
            throw forbidden();
        }

        // 3) Find active item by qrToken
        Item item = itemRepository.findActiveItemByQrToken(qrToken)
                .orElseThrow(() -> forbidden());

        // 4) Ownership check: item must belong to the user
        if (item.getUser() == null || item.getUser().getId() == null || !item.getUser().getId().equals(user.getId())) {
            throw forbidden();
        }

        // 5) Issue short-lived support token
        String supportToken = UUID.randomUUID().toString().replace("-", "");
        long expiresAt = Instant.now().getEpochSecond() + SUPPORT_TOKEN_TTL_SECONDS;
        supportTokens.put(supportToken, new SupportTokenData(user.getUsername(), expiresAt));

        return supportToken;
    }

    public void createSupportRequest(SupportRequestCreateRequest request) {
        denyIfHoneypotFilled(request.getWebsite());

        String supportToken = requireTrimmed(request.getSupportToken());
        String message = requireTrimmed(request.getMessage());

        String username = getValidUsernameFromTokenOrThrow(supportToken);

        User user = userRepository.findActiveUserByUsername(username)
                .orElseThrow(() -> forbidden());

        SupportRequest supportRequest = new SupportRequest();
        supportRequest.setUsername(user.getUsername());
        supportRequest.setEmail(user.getEmail()); // locked to account email
        supportRequest.setMessage(message);
        supportRequest.setStatus("NEW");

        supportRequestRepository.save(supportRequest);

        // One-time use
        supportTokens.remove(supportToken);
    }

    private String getValidUsernameFromTokenOrThrow(String supportToken) {
        SupportTokenData data = supportTokens.get(supportToken);
        long now = Instant.now().getEpochSecond();

        if (data == null || data.expiresAtEpochSeconds() < now) {
            supportTokens.remove(supportToken);
            throw forbidden();
        }
        return data.username();
    }

    private void denyIfHoneypotFilled(String website) {
        if (website != null && !website.isBlank()) {
            throw forbidden();
        }
    }

    private String requireTrimmed(String value) {
        if (value == null || value.isBlank()) {
            throw forbidden();
        }
        return value.trim();
    }

    private ForbiddenException forbidden() {
        return new ForbiddenException(ACCESS_DENIED, 403);
    }
}
