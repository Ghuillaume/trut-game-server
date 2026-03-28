package com.trutgame.server.interfaces.rest;

import com.trutgame.server.application.dto.*;
import com.trutgame.server.application.port.in.AddAiPlayerUseCase;
import com.trutgame.server.application.port.in.CreateGameUseCase;
import com.trutgame.server.application.port.in.GetGameViewUseCase;
import com.trutgame.server.application.port.in.JoinGameUseCase;
import com.trutgame.server.application.port.in.SwapTeamUseCase;
import com.trutgame.server.domain.model.PlayerId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final CreateGameUseCase createGameUseCase;
    private final JoinGameUseCase joinGameUseCase;
    private final GetGameViewUseCase getGameViewUseCase;
    private final AddAiPlayerUseCase addAiPlayerUseCase;
    private final SwapTeamUseCase swapTeamUseCase;

    public GameController(CreateGameUseCase createGameUseCase,
                         JoinGameUseCase joinGameUseCase,
                         GetGameViewUseCase getGameViewUseCase,
                         AddAiPlayerUseCase addAiPlayerUseCase,
                         SwapTeamUseCase swapTeamUseCase) {
        this.createGameUseCase = createGameUseCase;
        this.joinGameUseCase = joinGameUseCase;
        this.getGameViewUseCase = getGameViewUseCase;
        this.addAiPlayerUseCase = addAiPlayerUseCase;
        this.swapTeamUseCase = swapTeamUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateGameResult> createGame(@Valid @RequestBody CreateGameRequest request) {
        CreateGameResult result = createGameUseCase.createGame(
                new CreateGameCommand(request.pseudo().trim()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<JoinGameResult> joinGame(
            @PathVariable String gameId,
            @Valid @RequestBody JoinGameRequest request) {
        JoinGameResult result = joinGameUseCase.joinGame(
                new JoinGameCommand(gameId, request.pseudo().trim()));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameView> getGameView(
            @PathVariable String gameId,
            @RequestParam @NotBlank String playerId) {
        GameView view = getGameViewUseCase.getGameView(gameId, new PlayerId(playerId));
        return ResponseEntity.ok(view);
    }

    @PostMapping("/{gameId}/add-ai")
    public ResponseEntity<Map<String, String>> addAiPlayer(
            @PathVariable String gameId,
            @Valid @RequestBody AddAiRequest request) {
        addAiPlayerUseCase.addAiPlayer(
            new AddAiPlayerCommand(gameId, request.requestingPlayerId()));
        return ResponseEntity.ok(Map.of("status", "AI player added"));
    }

    @PostMapping("/{gameId}/swap-team")
    public ResponseEntity<Map<String, String>> swapTeam(
            @PathVariable String gameId,
            @Valid @RequestBody SwapTeamRequest request) {
        swapTeamUseCase.swapTeam(
            new SwapTeamCommand(gameId, request.requestingPlayerId(), request.targetPlayerId()));
        return ResponseEntity.ok(Map.of("status", "Team swapped"));
    }

    public record CreateGameRequest(
        @NotBlank(message = "Le pseudo est obligatoire")
        @Size(min = 1, max = 20, message = "Le pseudo doit faire entre 1 et 20 caractères")
        String pseudo
    ) {}

    public record JoinGameRequest(
        @NotBlank(message = "Le pseudo est obligatoire")
        @Size(min = 1, max = 20, message = "Le pseudo doit faire entre 1 et 20 caractères")
        String pseudo
    ) {}

    public record AddAiRequest(
        @NotBlank(message = "Le requestingPlayerId est obligatoire")
        String requestingPlayerId
    ) {}

    public record SwapTeamRequest(
        @NotBlank(message = "Le requestingPlayerId est obligatoire")
        String requestingPlayerId,
        @NotBlank(message = "Le targetPlayerId est obligatoire")
        String targetPlayerId
    ) {}
}
