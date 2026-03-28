package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.AddAiPlayerCommand;
import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.application.port.in.AddAiPlayerUseCase;
import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.application.port.out.GameViewPublisher;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.Hand;
import com.trutgame.server.domain.model.Player;
import com.trutgame.server.domain.model.PlayerId;
import com.trutgame.server.domain.model.Team;
import com.trutgame.server.domain.phase.GamePhase;
import com.trutgame.server.domain.service.TrutGameEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AddAiPlayerService implements AddAiPlayerUseCase {

    private static final List<String> AI_NAMES = List.of(
        "🤖 Bot Alpha", "🤖 Bot Bravo", "🤖 Bot Charlie", "🤖 Bot Delta"
    );
    private static final Random RANDOM = new Random();

    private final GameSessionRepository repository;
    private final GameViewPublisher publisher;
    private final TrutGameEngine engine;
    private final GameViewBuilder viewBuilder;

    public AddAiPlayerService(GameSessionRepository repository, GameViewPublisher publisher,
                              TrutGameEngine engine, GameViewBuilder viewBuilder) {
        this.repository = repository;
        this.publisher = publisher;
        this.engine = engine;
        this.viewBuilder = viewBuilder;
    }

    @Override
    public void addAiPlayer(AddAiPlayerCommand command) {
        GameState state = repository.findById(command.gameId())
            .orElseThrow(() -> new IllegalArgumentException("Game not found: " + command.gameId()));

        if (state.phase() != GamePhase.WAITING_FOR_PLAYERS) {
            throw new IllegalStateException("Game already started");
        }

        Player requester = state.players().stream()
            .filter(p -> p.id().value().equals(command.requestingPlayerId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Requesting player not found"));

        if (requester.seatIndex() != 0) {
            throw new IllegalStateException("Only the game creator can add AI players");
        }

        if (state.players().size() >= 4) {
            throw new IllegalStateException("Game is full");
        }

        PlayerId aiPlayerId = PlayerId.generate();
        int seatIndex = state.players().size();
        Team team = (seatIndex % 2 == 0) ? Team.TEAM_A : Team.TEAM_B;
        String aiName = AI_NAMES.get(RANDOM.nextInt(AI_NAMES.size()));
        Player aiPlayer = new Player(aiPlayerId, aiName, team, seatIndex, true);

        List<Player> updatedPlayers = new ArrayList<>(state.players());
        updatedPlayers.add(aiPlayer);

        Map<PlayerId, Hand> updatedHands = new HashMap<>(state.hands());
        updatedHands.put(aiPlayerId, Hand.empty());

        GameState updatedState = new GameState(
            state.gameId(), state.phase(), updatedPlayers, state.currentDealerId(),
            state.currentPlayerId(), updatedHands, state.talon(), state.completedTricks(),
            state.currentTrick(), state.trutChallenge(), state.score(), state.roundNumber(),
            state.fortialActive(), state.winner(), state.rematchVotes()
        );

        if (updatedPlayers.size() == 4) {
            updatedState = engine.startNewRound(updatedState);
        }

        repository.save(updatedState);
        publishViewsToAll(updatedState);
    }

    private void publishViewsToAll(GameState state) {
        for (Player player : state.players()) {
            GameView view = viewBuilder.buildView(state, player.id());
            publisher.publishGameView(state.gameId(), player.id(), view);
        }
    }
}
