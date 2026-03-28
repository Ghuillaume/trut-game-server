package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.application.dto.SwapTeamCommand;
import com.trutgame.server.application.port.in.SwapTeamUseCase;
import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.application.port.out.GameViewPublisher;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.Player;
import com.trutgame.server.domain.model.PlayerId;
import com.trutgame.server.domain.model.Team;
import com.trutgame.server.domain.phase.GamePhase;

import java.util.List;

public class SwapTeamService implements SwapTeamUseCase {

    private final GameSessionRepository repository;
    private final GameViewPublisher publisher;
    private final GameViewBuilder viewBuilder;

    public SwapTeamService(GameSessionRepository repository, GameViewPublisher publisher,
                           GameViewBuilder viewBuilder) {
        this.repository = repository;
        this.publisher = publisher;
        this.viewBuilder = viewBuilder;
    }

    @Override
    public void swapTeam(SwapTeamCommand command) {
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
            throw new IllegalStateException("Only the game creator can swap teams");
        }

        PlayerId targetId = new PlayerId(command.targetPlayerId());
        Player target = state.getPlayer(targetId);

        Team newTeam = target.team() == Team.TEAM_A ? Team.TEAM_B : Team.TEAM_A;
        Player swappedPlayer = new Player(target.id(), target.pseudo(), newTeam, target.seatIndex(), target.isAi());

        List<Player> updatedPlayers = state.players().stream()
            .map(p -> p.id().equals(targetId) ? swappedPlayer : p)
            .toList();

        GameState updatedState = new GameState(
            state.gameId(), state.phase(), updatedPlayers, state.currentDealerId(),
            state.currentPlayerId(), state.hands(), state.talon(), state.completedTricks(),
            state.currentTrick(), state.trutChallenge(), state.score(), state.roundNumber(),
            state.fortialActive(), state.winner(), state.rematchVotes()
        );

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
