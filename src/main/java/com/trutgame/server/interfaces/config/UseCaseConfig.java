package com.trutgame.server.interfaces.config;

import com.trutgame.server.application.port.in.AddAiPlayerUseCase;
import com.trutgame.server.application.port.in.ApplyActionUseCase;
import com.trutgame.server.application.port.in.CreateGameUseCase;
import com.trutgame.server.application.port.in.GetGameViewUseCase;
import com.trutgame.server.application.port.in.JoinGameUseCase;
import com.trutgame.server.application.port.in.StartGameUseCase;
import com.trutgame.server.application.port.in.SwapTeamUseCase;
import com.trutgame.server.application.port.out.GameSessionRepository;
import com.trutgame.server.application.port.out.GameViewPublisher;
import com.trutgame.server.application.port.out.PlayerConnectionPort;
import com.trutgame.server.application.usecase.*;
import com.trutgame.server.application.usecase.PlayerConnectionService;
import com.trutgame.server.domain.service.AiPlayerStrategy;
import com.trutgame.server.domain.service.TrutGameEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class UseCaseConfig {

    @Bean
    public TrutGameEngine trutGameEngine() {
        return new TrutGameEngine();
    }

    @Bean
    public AiPlayerStrategy aiPlayerStrategy() {
        return new AiPlayerStrategy();
    }

    @Bean
    public GameViewBuilder gameViewBuilder(TrutGameEngine engine, PlayerConnectionPort connectionPort) {
        return new GameViewBuilder(engine, connectionPort);
    }

    @Bean
    public CreateGameUseCase createGameUseCase(GameSessionRepository repository) {
        return new CreateGameService(repository);
    }

    @Bean
    public JoinGameUseCase joinGameUseCase(GameSessionRepository repository,
                                           GameViewPublisher publisher,
                                           TrutGameEngine engine,
                                           GameViewBuilder viewBuilder) {
        return new JoinGameService(repository, publisher, engine, viewBuilder);
    }

    @Bean
    public ScheduledExecutorService aiScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ai-player-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    @Bean
    public ApplyActionUseCase applyActionUseCase(GameSessionRepository repository,
                                                  GameViewPublisher publisher,
                                                  TrutGameEngine engine,
                                                  GameViewBuilder viewBuilder,
                                                  AiPlayerStrategy aiStrategy,
                                                  ScheduledExecutorService aiScheduler) {
        return new ApplyActionService(repository, publisher, engine, viewBuilder, aiStrategy, aiScheduler);
    }

    @Bean
    public GetGameViewUseCase getGameViewUseCase(GameSessionRepository repository,
                                                  GameViewBuilder viewBuilder) {
        return new GetGameViewService(repository, viewBuilder);
    }

    @Bean
    public AddAiPlayerUseCase addAiPlayerUseCase(GameSessionRepository repository,
                                                  GameViewPublisher publisher,
                                                  TrutGameEngine engine,
                                                  GameViewBuilder viewBuilder) {
        return new AddAiPlayerService(repository, publisher, engine, viewBuilder);
    }

    @Bean
    public SwapTeamUseCase swapTeamUseCase(GameSessionRepository repository,
                                           GameViewPublisher publisher,
                                           GameViewBuilder viewBuilder) {
        return new SwapTeamService(repository, publisher, viewBuilder);
    }

    @Bean
    public StartGameUseCase startGameUseCase(GameSessionRepository repository,
                                             GameViewPublisher publisher,
                                             TrutGameEngine engine,
                                             GameViewBuilder viewBuilder) {
        return new StartGameService(repository, publisher, engine, viewBuilder);
    }

    @Bean
    public PlayerConnectionService playerConnectionService(GameSessionRepository repository,
                                                            GameViewPublisher publisher,
                                                            GameViewBuilder viewBuilder) {
        return new PlayerConnectionService(repository, publisher, viewBuilder);
    }
}
