package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.infrastructure.exception.BadRequestException;
import ee.valiit.mystuffback.persistence.passwordreset.PasswordResetToken;
import ee.valiit.mystuffback.persistence.passwordreset.PasswordResetTokenRepository;
import ee.valiit.mystuffback.persistence.user.User;
import ee.valiit.mystuffback.persistence.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static ee.valiit.mystuffback.infrastructure.error.Error.INVALID_RESET_TOKEN;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${mystuff.reset.token-ttl-minutes:60}")
    private int tokenTtlMinutes;

    @Value("${mystuff.reset.link-base}")
    private String linkBase;

    /**
     * Generates a reset token and sends an email.
     * Always returns silently — never reveals whether the email exists.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userRepository.findActiveUserByEmail(email.trim().toLowerCase());

        if (userOpt.isEmpty()) {
            log.debug("Password reset requested for unknown email: {}", email);
            // Simulate the time an email send would take to prevent timing-based email enumeration
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(200, 500));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiresAt(Instant.now().plus(tokenTtlMinutes, ChronoUnit.MINUTES));
        tokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), linkBase + "?token=" + token);
    }

    /**
     * Validates the token and updates the password.
     * Returns 400 with errorCode 551 for any invalid token state.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException(
                        INVALID_RESET_TOKEN.getMessage(), INVALID_RESET_TOKEN.getErrorCode()));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new BadRequestException(
                    INVALID_RESET_TOKEN.getMessage(), INVALID_RESET_TOKEN.getErrorCode());
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password successfully reset for user id={}", user.getId());
    }
}
