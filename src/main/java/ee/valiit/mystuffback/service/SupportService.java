package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.controller.support.dto.SupportRequestCreateRequest;
import ee.valiit.mystuffback.controller.support.dto.SupportVerifyRequest;
import ee.valiit.mystuffback.persistence.support.SupportRequest;
import ee.valiit.mystuffback.persistence.support.SupportRequestRepository;
import ee.valiit.mystuffback.persistence.item.Item;
import ee.valiit.mystuffback.persistence.item.ItemRepository;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserRepository;
import ee.valiit.mystuffback.infrastructure.exception.ForbiddenException;
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

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final SupportRequestRepository supportRequestRepository;


    // in-memory token store (supportToken -> expiresAtEpochSeconds)
    private record SupportTokenData(String username, long expiresAtEpochSeconds) {}
    private final Map<String, SupportTokenData> supportTokens = new ConcurrentHashMap<>();


    public String verifyOwnershipAndIssueToken(SupportVerifyRequest request) {
        // 1) Honeypot check
        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            // silent deny: look like success but don't issue usable token
            throw new ForbiddenException("Access denied", 403);
        }

        String username = request.getUsername().trim();
        String qrToken = request.getQrToken().trim();

        // 2) Find active user (case-sensitive username)
        User user = userRepository.findActiveUserByUsername(username)
                .orElseThrow(() -> new ForbiddenException("Access denied", 403));

        // 3) Find active item by qrToken
        Item item = itemRepository.findActiveItemByQrToken(qrToken)
                .orElseThrow(() -> new ForbiddenException("Access denied", 403));

        // 4) Ownership check
        if (!item.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied", 403);
        }

        // 5) Issue short-lived support token
        String supportToken = UUID.randomUUID().toString().replace("-", "");
        long expiresAt = Instant.now().getEpochSecond() + SUPPORT_TOKEN_TTL_SECONDS;
        supportTokens.put(supportToken, new SupportTokenData(user.getUsername(), expiresAt));

        return supportToken;
    }

    public void validateSupportTokenOrThrow(String supportToken) {
        if (supportToken == null || supportToken.isBlank()) {
            throw new ForbiddenException("Access denied", 403);
        }

        SupportTokenData data = supportTokens.get(supportToken);
        long now = Instant.now().getEpochSecond();

        if (data == null || data.expiresAtEpochSeconds() < now) {
            supportTokens.remove(supportToken);
            throw new ForbiddenException("Access denied", 403);
        }
    }

    public void createSupportRequest(SupportRequestCreateRequest request) {

        if (request.getWebsite() != null && !request.getWebsite().isBlank()) {
            throw new ForbiddenException("Access denied", 403);
        }

        String supportToken = request.getSupportToken().trim();
        validateSupportTokenOrThrow(supportToken);

        String username = getUsernameFromSupportTokenOrThrow(supportToken);

        User user = userRepository.findActiveUserByUsername(username)
                .orElseThrow(() -> new ForbiddenException("Access denied", 403));

        SupportRequest supportRequest = new SupportRequest();
        supportRequest.setUsername(user.getUsername());
        supportRequest.setEmail(user.getEmail()); // force match to account email
        supportRequest.setMessage(request.getMessage().trim());
        supportRequest.setStatus("NEW");

        supportRequestRepository.save(supportRequest);
        supportTokens.remove(supportToken);
    }

    private String getUsernameFromSupportTokenOrThrow(String supportToken) {
        SupportTokenData data = supportTokens.get(supportToken);
        long now = Instant.now().getEpochSecond();

        if (data == null || data.expiresAtEpochSeconds() < now) {
            supportTokens.remove(supportToken);
            throw new ForbiddenException("Access denied", 403);
        }

        return data.username();
    }
}
