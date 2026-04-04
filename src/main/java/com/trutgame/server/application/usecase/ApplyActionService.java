package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.ActionCommand;
import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.application.port.in.ApplyActionUseCase;
import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.application.port.out.GameViewPublisher;
import com.trutgame.server.domain.action.BrellanAction;
import com.trutgame.server.domain.action.CallAction;
import com.trutgame.server.domain.action.DeuxPareillesAction;
import com.trutgame.server.domain.action.FoldAction;
import com.trutgame.server.domain.action.GameAction;
import com.trutgame.server.domain.action.PlayCardAction;
import com.trutgame.server.domain.action.TrutAction;
import com.trutgame.server.domain.model.Card;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.Hand;
import com.trutgame.server.domain.model.Player;
import com.trutgame.server.domain.model.PlayerId;
import com.trutgame.server.domain.model.Team;
import com.trutgame.server.domain.model.TokenCount;
import com.trutgame.server.domain.model.Trick;
import com.trutgame.server.domain.phase.GamePhase;
import com.trutgame.server.domain.service.TrutGameEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApplyActionService implements ApplyActionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApplyActionService.class);

    private final GameSessionRepository repository;
    private final GameViewPublisher publisher;
    private final TrutGameEngine engine;
    private final GameViewBuilder viewBuilder;
    private final AiTurnScheduler aiTurnScheduler;

    public ApplyActionService(GameSessionRepository repository, GameViewPublisher publisher,
                              TrutGameEngine engine, GameViewBuilder viewBuilder,
                              AiTurnScheduler aiTurnScheduler) {
        this.repository = repository;
        this.publisher = publisher;
        this.engine = engine;
        this.viewBuilder = viewBuilder;
        this.aiTurnScheduler = aiTurnScheduler;
    }

    @Override
    public void applyAction(ActionCommand command) {
        GameState state = repository.findById(command.gameId())
            .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        PlayerId playerId = new PlayerId(command.playerId());

        GameState newState;
        String event;

        if ("START_NEW_ROUND".equals(command.actionType())) {
            newState = engine.startNewRound(state);
            event = "Nouvelle manche !";
        } else if ("REMATCH".equals(command.actionType())) {
            newState = handleRematch(state, playerId);
            event = state.getPlayer(playerId).pseudo() + " veut une revanche !";
            if (newState.phase() == GamePhase.PLAYING_TRICK || newState.phase() == GamePhase.FORTIAL_DECISION) {
                event = "Revanche acceptée ! Nouvelle partie !";
            }
        } else {
            GameAction action = mapAction(command, playerId);
            newState = engine.apply(state, action);
            event = describeAction(action, state);
        }

        repository.save(newState);
        publishViewsToAll(newState);
        publisher.publishEvent(newState.gameId(), event);

        // Schedule AI turns asynchronously with delay
        aiTurnScheduler.scheduleNextAiTurn(newState.gameId());
    }

    private GameState handleRematch(GameState state, PlayerId playerId) {
        if (state.phase() != GamePhase.GAME_OVER) {
            throw new IllegalStateException("Rematch only available when game is over");
        }

        Set<PlayerId> votes = new HashSet<>(state.rematchVotes());
        votes.add(playerId);

        // AI players auto-vote
        for (Player p : state.players()) {
            if (p.isAi()) {
                votes.add(p.id());
            }
        }

        // Check if all human players have voted
        long humanCount = state.players().stream().filter(p -> !p.isAi()).count();
        long humanVotes = votes.stream()
            .filter(vid -> state.players().stream()
                .anyMatch(p -> p.id().equals(vid) && !p.isAi()))
            .count();

        if (humanVotes >= humanCount) {
            // All humans voted, restart
            return createRematchState(state);
        }

        // Not all voted yet, just record vote
        return new GameState(
            state.gameId(), state.phase(), state.players(), state.currentDealerId(),
            state.currentPlayerId(), state.hands(), state.talon(), state.completedTricks(),
            state.currentTrick(), state.trutChallenge(), state.score(), state.roundNumber(),
            state.fortialActive(), state.winner(), votes
        );
    }

    private GameState createRematchState(GameState state) {
        Map<PlayerId, Hand> emptyHands = new HashMap<>();
        state.players().forEach(p -> emptyHands.put(p.id(), Hand.empty()));

        GameState freshState = new GameState(
            state.gameId(), GamePhase.WAITING_FOR_PLAYERS, state.players(),
            null, null, emptyHands, List.of(), List.of(), Trick.empty(), null,
            Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()),
            0, false, null, Set.of()
        );

        return engine.startNewRound(freshState);
    }

    private void publishViewsToAll(GameState state) {
        for (Player player : state.players()) {
            GameView view = viewBuilder.buildView(state, player.id());
            publisher.publishGameView(state.gameId(), player.id(), view);
        }
    }

    private GameAction mapAction(ActionCommand command, PlayerId playerId) {
        return switch (command.actionType()) {
            case "PLAY_CARD" -> new PlayCardAction(playerId, Card.fromId(command.cardId()));
            case "TRUT" -> new TrutAction(playerId);
            case "CALL" -> new CallAction(playerId);
            case "FOLD" -> new FoldAction(playerId);
            case "BRELLAN" -> new BrellanAction(playerId);
            case "DEUX_PAREILLES" -> new DeuxPareillesAction(playerId);
            default -> throw new IllegalArgumentException("Unknown action: " + command.actionType());
        };
    }

    private String describeAction(GameAction action, GameState state) {
        String pseudo;
        try {
            pseudo = state.getPlayer(action.playerId()).pseudo();
        } catch (IllegalArgumentException e) {
            pseudo = action.playerId().value();
        }
        return switch (action) {
            case PlayCardAction a -> pseudo + " a joué le " + cardLabel(a.card().id());
            case TrutAction ignored -> pseudo + " a truté !";
            case CallAction ignored -> pseudo + " va voir";
            case FoldAction ignored -> pseudo + " se couche";
            case BrellanAction ignored -> pseudo + " annonce Brelan !";
            case DeuxPareillesAction ignored -> pseudo + " annonce Deux pareilles une fausse !";
        };
    }

    private String cardLabel(String cardId) {
        Map<String, String> values = Map.of(
            "SEVEN", "7", "EIGHT", "8", "ACE", "As", "KING", "Roi",
            "QUEEN", "Dame", "JACK", "Valet", "TEN", "10", "NINE", "9");
        Map<String, String> suits = Map.of(
            "HEARTS", "♥", "DIAMONDS", "♦", "CLUBS", "♣", "SPADES", "♠");
        String[] parts = cardId.split("_");
        return values.getOrDefault(parts[0], parts[0]) + " de " + suits.getOrDefault(parts[1], parts[1]);
    }
}
