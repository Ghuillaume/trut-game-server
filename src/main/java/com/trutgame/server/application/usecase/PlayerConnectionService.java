package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.application.port.out.GameViewPublisher;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.Player;

public class PlayerConnectionService {

    private final GameSessionRepository repository;
    private final GameViewPublisher publisher;
    private final GameViewBuilder viewBuilder;

    public PlayerConnectionService(GameSessionRepository repository,
                                   GameViewPublisher publisher,
                                   GameViewBuilder viewBuilder) {
        this.repository = repository;
        this.publisher = publisher;
        this.viewBuilder = viewBuilder;
    }

    public void onPlayerConnected(String gameId, String playerId, boolean wasDisconnected) {
        repository.findById(gameId).ifPresent(state -> {
            if (wasDisconnected) {
                String pseudo = findPseudo(state, playerId);
                publisher.publishEvent(gameId, pseudo + " s'est reconnecté !");
            }
            publishViewsToAll(state);
        });
    }

    public void onPlayerDisconnected(String gameId, String playerId) {
        repository.findById(gameId).ifPresent(state -> {
            String pseudo = findPseudo(state, playerId);
            publisher.publishEvent(gameId, pseudo + " s'est déconnecté");
            publishViewsToAll(state);
        });
    }

    private String findPseudo(GameState state, String playerId) {
        return state.players().stream()
            .filter(p -> p.id().value().equals(playerId))
            .map(Player::pseudo)
            .findFirst()
            .orElse("Joueur inconnu");
    }

    private void publishViewsToAll(GameState state) {
        for (Player player : state.players()) {
            GameView view = viewBuilder.buildView(state, player.id());
            publisher.publishGameView(state.gameId(), player.id(), view);
        }
    }
}
