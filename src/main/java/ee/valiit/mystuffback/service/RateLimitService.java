package ee.valiit.mystuffback.service;

import ee.valiit.mystuffback.infrastructure.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private record WindowCounter(long windowStartEpochSeconds, int count) {}

    // key: endpoint + ":" + identifier (IP or userId)
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public void resetAll() {
        counters.clear();
    }

    public void checkRateLimitOrThrow(String endpointKey, String identifier, int maxRequests, int windowSeconds) {
        if(identifier == null || identifier.isBlank()) {
            identifier = "unknown";
        }

        String key = endpointKey + ":" + identifier;
        long now = Instant.now().getEpochSecond();

        counters.compute(key, (k, existing) -> {
            // new window
            if (existing == null || (now - existing.windowStartEpochSeconds()) >= windowSeconds) {
                return new WindowCounter(now, 1);
            }

            int nextCount = existing.count() + 1;
            if (nextCount > maxRequests) {
                throw new TooManyRequestsException("Too many requests");
            }

            return new WindowCounter(existing.windowStartEpochSeconds(), nextCount);
        });
    }
}
