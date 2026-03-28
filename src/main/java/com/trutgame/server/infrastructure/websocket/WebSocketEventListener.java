package com.trutgame.server.infrastructure.websocket;

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

    private final PlayerConnectionTracker connectionTracker;
    private final Map<String, String[]> sessionToGamePlayer = new ConcurrentHashMap<>();

    public WebSocketEventListener(PlayerConnectionTracker connectionTracker) {
        this.connectionTracker = connectionTracker;
    }

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String gameId = getFirstHeader(accessor, "gameId");
        String playerId = getFirstHeader(accessor, "playerId");

        if (gameId != null && playerId != null && sessionId != null) {
            sessionToGamePlayer.put(sessionId, new String[]{gameId, playerId});
            connectionTracker.playerConnected(gameId, playerId);
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
            }
        }
    }

    private String getFirstHeader(StompHeaderAccessor accessor, String headerName) {
        var values = accessor.getNativeHeader(headerName);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }
}
