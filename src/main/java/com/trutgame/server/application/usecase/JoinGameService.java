package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.application.dto.JoinGameCommand;
import com.trutgame.server.application.dto.JoinGameResult;
import com.trutgame.server.application.port.in.JoinGameUseCase;
import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.application.port.out.GameViewPublisher;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.Hand;
import com.trutgame.server.domain.model.Player;
import com.trutgame.server.domain.model.PlayerId;
import com.trutgame.server.domain.model.Team;
import com.trutgame.server.domain.model.Trick;
import com.trutgame.server.domain.phase.GamePhase;
import com.trutgame.server.domain.service.TrutGameEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoinGameService implements JoinGameUseCase {

    private final GameSessionRepository repository;
    private final GameViewPublisher publisher;
    private final TrutGameEngine engine;
    private final GameViewBuilder viewBuilder;

    public JoinGameService(GameSessionRepository repository, GameViewPublisher publisher,
                           TrutGameEngine engine, GameViewBuilder viewBuilder) {
        this.repository = repository;
        this.publisher = publisher;
        this.engine = engine;
        this.viewBuilder = viewBuilder;
    }

    @Override
    public JoinGameResult joinGame(JoinGameCommand command) {
        GameState state = repository.findById(command.gameId())
            .orElseThrow(() -> new IllegalArgumentException("Game not found: " + command.gameId()));

        if (state.phase() != GamePhase.WAITING_FOR_PLAYERS) {
            throw new IllegalStateException("Game already started");
        }
        if (state.players().size() >= 4) {
            throw new IllegalStateException("Game is full");
        }

        PlayerId playerId = PlayerId.generate();
        int seatIndex = state.players().size();
        Team team = (seatIndex % 2 == 0) ? Team.TEAM_A : Team.TEAM_B;
        Player newPlayer = new Player(playerId, command.pseudo(), team, seatIndex, false);

        List<Player> updatedPlayers = new ArrayList<>(state.players());
        updatedPlayers.add(newPlayer);

        Map<PlayerId, Hand> updatedHands = new HashMap<>(state.hands());
        updatedHands.put(playerId, Hand.empty());

        GameState updatedState = new GameState(
            state.gameId(), state.phase(), updatedPlayers, state.currentDealerId(),
            state.currentPlayerId(), updatedHands, state.talon(), state.completedTricks(),
            state.currentTrick(), state.trutChallenge(), state.score(), state.roundNumber(),
            state.fortialActive(), state.winner(), state.rematchVotes()
        );

        repository.save(updatedState);
        publishViewsToAll(updatedState);

        List<JoinGameResult.PlayerInfo> playerInfos = updatedPlayers.stream()
            .map(p -> new JoinGameResult.PlayerInfo(p.id().value(), p.pseudo(), p.team().name()))
            .toList();

        return new JoinGameResult(playerId.value(), playerInfos);
    }

    private void publishViewsToAll(GameState state) {
        for (Player player : state.players()) {
            GameView view = viewBuilder.buildView(state, player.id());
            publisher.publishGameView(state.gameId(), player.id(), view);
        }
    }
}
