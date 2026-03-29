package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.domain.action.GameAction;
import com.trutgame.server.domain.model.Card;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.Hand;
import com.trutgame.server.domain.model.Player;
import com.trutgame.server.domain.model.PlayerId;
import com.trutgame.server.domain.model.Team;
import com.trutgame.server.domain.model.TokenCount;
import com.trutgame.server.domain.model.Trick;
import com.trutgame.server.domain.model.TrickEntry;
import com.trutgame.server.domain.service.TrutGameEngine;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameViewBuilder {

    private final TrutGameEngine engine;

    public GameViewBuilder(TrutGameEngine engine) {
        this.engine = engine;
    }

    public GameView buildView(GameState state, PlayerId playerId) {
        Hand myHand = state.hands().getOrDefault(playerId, Hand.empty());
        Player me = state.players().stream()
            .filter(p -> p.id().equals(playerId))
            .findFirst()
            .orElse(null);

        List<String> handCards = myHand.cards().stream()
            .map(Card::id)
            .toList();

        List<GameView.PlayerView> playerViews = state.players().stream()
            .map(p -> new GameView.PlayerView(
                p.id().value(),
                p.pseudo(),
                p.team().name(),
                state.hands().getOrDefault(p.id(), Hand.empty()).size(),
                p.isAi()
            ))
            .toList();

        List<GameView.TrickEntryView> trickEntries = state.currentTrick().entries().stream()
            .map(e -> new GameView.TrickEntryView(e.playerId().value(), e.card().id()))
            .toList();

        List<GameView.CompletedTrickView> completedTrickViews = state.completedTricks().stream()
            .map(trick -> {
                List<GameView.TrickEntryView> entries = trick.entries().stream()
                    .map(e -> new GameView.TrickEntryView(e.playerId().value(), e.card().id()))
                    .toList();
                String winnerTeam = trick.winner(state.players())
                    .map(Team::name)
                    .orElse(null);
                return new GameView.CompletedTrickView(entries, winnerTeam);
            })
            .toList();

        GameView.TrutChallengeView challengeView = null;
        if (state.trutChallenge() != null) {
            String status = state.trutChallenge().resolved()
                ? (state.trutChallenge().accepted() ? "ACCEPTED" : "DECLINED")
                : "PENDING";
            challengeView = new GameView.TrutChallengeView(
                state.trutChallenge().challengerId().value(),
                status,
                state.trutChallenge().challengeType().name());
        }

        Map<String, GameView.ScoreView> scoreView = state.score().entrySet().stream()
            .collect(Collectors.toMap(
                e -> e.getKey().name(),
                e -> new GameView.ScoreView(e.getValue().grands(), e.getValue().petits())
            ));

        List<String> availableActions = engine.availableActions(state, playerId).stream()
            .map(GameAction::type)
            .distinct()
            .toList();

        List<String> rematchVoteIds = state.rematchVotes().stream()
            .map(PlayerId::value)
            .toList();

        // Creator is the player at seat 0
        String creatorId = state.players().stream()
            .filter(p -> p.seatIndex() == 0)
            .map(p -> p.id().value())
            .findFirst()
            .orElse(null);

        return new GameView(
            state.gameId(),
            state.phase().name(),
            handCards,
            me != null ? me.team().name() : null,
            state.currentPlayerId() != null ? state.currentPlayerId().value() : null,
            playerViews,
            trickEntries,
            completedTrickViews,
            challengeView,
            scoreView,
            availableActions,
            state.roundNumber(),
            state.fortialActive(),
            state.winner() != null ? state.winner().name() : null,
            rematchVoteIds,
            List.of(),
            creatorId
        );
    }
}
