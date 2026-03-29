package com.trutgame.server.infrastructure.websocket;

import com.trutgame.server.application.usecase.PlayerConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for WebSocket STOMP connect/disconnect events and updates the
 * {@link PlayerConnectionTracker}.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final PlayerConnectionTracker connectionTracker;
    private final PlayerConnectionService connectionService;
    private final Map<String, String[]> sessionToGamePlayer = new ConcurrentHashMap<>();

    public WebSocketEventListener(PlayerConnectionTracker connectionTracker,
                                  PlayerConnectionService connectionService) {
        this.connectionTracker = connectionTracker;
        this.connectionService = connectionService;
    }

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String gameId = getFirstHeader(accessor, "gameId");
        String playerId = getFirstHeader(accessor, "playerId");

        if (gameId != null && playerId != null && sessionId != null) {
            boolean wasDisconnected = connectionTracker.wasDisconnected(gameId, playerId);
            sessionToGamePlayer.put(sessionId, new String[]{gameId, playerId});
            connectionTracker.playerConnected(gameId, playerId);
            try {
                connectionService.onPlayerConnected(gameId, playerId, wasDisconnected);
            } catch (Exception e) {
                log.error("🌧 Error handling player connect for game {} player {}", gameId, playerId, e);
            }
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (sessionId != null) {
            String[] gamePlayer = sessionToGamePlayer.remove(sessionId);
            if (gamePlayer != null) {
                connectionTracker.playerDisconnected(gamePlayer[0], gamePlayer[1]);
                try {
                    connectionService.onPlayerDisconnected(gamePlayer[0], gamePlayer[1]);
                } catch (Exception e) {
                    log.error("🌧 Error handling player disconnect for game {} player {}", gamePlayer[0], gamePlayer[1], e);
                }
            }
        }
    }

    private String getFirstHeader(StompHeaderAccessor accessor, String headerName) {
        var values = accessor.getNativeHeader(headerName);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }
}
