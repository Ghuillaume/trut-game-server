package com.trutgame.server.infrastructure.websocket;

import com.trutgame.server.application.port.out.GameViewPublisher;
import com.trutgame.server.application.port.out.PlayerConnectionPort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Tracks player WebSocket connections per game.
 * Handles disconnect timers and reconnection.
 */
@Component
public class PlayerConnectionTracker implements PlayerConnectionPort {

    private static final long DISCONNECT_TIMEOUT_SECONDS = 60;

    private final Map<String, Set<String>> connectedPlayers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> disconnectedPlayers = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> disconnectTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final GameViewPublisher publisher;

    public PlayerConnectionTracker(GameViewPublisher publisher) {
        this.publisher = publisher;
    }

    public void playerConnected(String gameId, String playerId) {
        connectedPlayers.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(playerId);
        disconnectedPlayers.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).remove(playerId);

        // Cancel disconnect timer if exists
        String timerKey = gameId + ":" + playerId;
        ScheduledFuture<?> timer = disconnectTimers.remove(timerKey);
        if (timer != null) {
            timer.cancel(false);
        }
    }

    public void playerDisconnected(String gameId, String playerId) {
        connectedPlayers.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).remove(playerId);
        disconnectedPlayers.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet()).add(playerId);

        String timerKey = gameId + ":" + playerId;
        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            disconnectTimers.remove(timerKey);
            publisher.publishEvent(gameId, "Partie abandonnée — " + playerId + " ne s'est pas reconnecté");
        }, DISCONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        disconnectTimers.put(timerKey, timer);
    }

    public boolean wasDisconnected(String gameId, String playerId) {
        return disconnectedPlayers.getOrDefault(gameId, Set.of()).contains(playerId);
    }

    public Set<String> getDisconnectedPlayers(String gameId) {
        return Set.copyOf(disconnectedPlayers.getOrDefault(gameId, Set.of()));
    }

    public boolean isConnected(String gameId, String playerId) {
        return connectedPlayers.getOrDefault(gameId, Set.of()).contains(playerId);
    }
}
