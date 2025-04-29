package me.artificial.autoserver.velocity;

import com.velocitypowered.api.proxy.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RateLimiter {
    private final long maxRequestsPerMinute;
    private final long windowMillis = TimeUnit.MINUTES.toMillis(1);
    private final Map<UUID, ArrayList<Long>> requests = new HashMap<>();

    RateLimiter(long maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public boolean canRequest(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        requests.putIfAbsent(playerId, new ArrayList<>());
        ArrayList<Long> timestamps = requests.get(playerId);

        // clean out timestamps
        timestamps.removeIf(stamp -> currentTime - stamp > windowMillis);

        if (timestamps.size() < maxRequestsPerMinute) {
            timestamps.add(currentTime);
            return true;
        }
        return false;
    }

    public long getRemainingCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        ArrayList<Long> timestamps = requests.get(playerId);
        if (timestamps == null || timestamps.isEmpty()) {
            return 0;
        }

        timestamps.removeIf(stamp -> currentTime - stamp > windowMillis);
        if (timestamps.size() < maxRequestsPerMinute) {
            return 0;
        }
        long oldest = timestamps.get(0);
        long timeUntilReset = windowMillis - (currentTime - oldest);

        return TimeUnit.MILLISECONDS.toSeconds(Math.max(timeUntilReset, 0));
    }
}
