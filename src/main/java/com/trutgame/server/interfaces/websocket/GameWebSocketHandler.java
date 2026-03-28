package com.trutgame.server.interfaces.websocket;

import com.trutgame.server.application.dto.ActionCommand;
import com.trutgame.server.application.port.in.ApplyActionUseCase;
import com.trutgame.server.domain.exception.InvalidActionException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class GameWebSocketHandler {

    private final ApplyActionUseCase applyActionUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    public GameWebSocketHandler(ApplyActionUseCase applyActionUseCase,
                               SimpMessagingTemplate messagingTemplate) {
        this.applyActionUseCase = applyActionUseCase;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/games/{gameId}/action")
    public void handleAction(
            @DestinationVariable String gameId,
            @Payload ActionMessage message,
            SimpMessageHeaderAccessor headerAccessor) {
        try {
            ActionCommand command = new ActionCommand(
                    gameId,
                    message.playerId(),
                    message.type(),
                    message.cardId()
            );
            applyActionUseCase.applyAction(command);
        } catch (InvalidActionException | IllegalArgumentException e) {
            // Send error only to the player who sent the invalid action
            String errorDest = "/topic/games/" + gameId + "/player/" + message.playerId() + "/errors";
            messagingTemplate.convertAndSend(errorDest, Map.of("error", e.getMessage()));
        }
    }

    public record ActionMessage(String playerId, String type, String cardId) {}
}
