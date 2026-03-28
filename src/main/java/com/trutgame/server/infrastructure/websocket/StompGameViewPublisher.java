package com.trutgame.server.infrastructure.websocket;

import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.application.port.out.GameViewPublisher;
import com.trutgame.server.domain.model.PlayerId;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompGameViewPublisher implements GameViewPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public StompGameViewPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publishGameView(String gameId, PlayerId playerId, GameView view) {
        String destination = "/topic/games/" + gameId + "/player/" + playerId.value();
        messagingTemplate.convertAndSend(destination, view);
    }

    @Override
    public void publishEvent(String gameId, String eventMessage) {
        String destination = "/topic/games/" + gameId + "/events";
        messagingTemplate.convertAndSend(destination, eventMessage);
    }
}
