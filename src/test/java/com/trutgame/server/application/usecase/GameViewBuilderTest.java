package com.trutgame.server.application.usecase;

import com.trutgame.server.application.dto.GameView;
import com.trutgame.server.domain.action.PlayCardAction;
import com.trutgame.server.domain.action.TrutAction;
import com.trutgame.server.domain.model.Card;
import com.trutgame.server.domain.model.CardValue;
import com.trutgame.server.domain.model.GameState;
import com.trutgame.server.domain.model.Hand;
import com.trutgame.server.domain.model.Player;
import com.trutgame.server.domain.model.PlayerId;
import com.trutgame.server.domain.model.Suit;
import com.trutgame.server.domain.model.Team;
import com.trutgame.server.domain.model.TokenCount;
import com.trutgame.server.domain.model.Trick;
import com.trutgame.server.domain.model.TrutChallenge;
import com.trutgame.server.domain.phase.GamePhase;
import com.trutgame.server.application.port.out.PlayerConnectionPort;
import com.trutgame.server.domain.service.TrutGameEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GameViewBuilder — unit tests")
class GameViewBuilderTest {

    private static final PlayerId PLAYER_1_ID = new PlayerId("p1");
    private static final PlayerId PLAYER_2_ID = new PlayerId("p2");
    private static final PlayerId PLAYER_3_ID = new PlayerId("p3");
    private static final PlayerId PLAYER_4_ID = new PlayerId("p4");

    private static final Card ACE_HEARTS = new Card(CardValue.ACE, Suit.HEARTS);
    private static final Card KING_SPADES = new Card(CardValue.KING, Suit.SPADES);
    private static final Card SEVEN_CLUBS = new Card(CardValue.SEVEN, Suit.CLUBS);

    @Mock
    private TrutGameEngine engine;

    @Mock
    private PlayerConnectionPort connectionPort;

    @InjectMocks
    private GameViewBuilder viewBuilder;

    @Test
    @DisplayName("should build view containing the player's hand cards")
    void shouldBuildViewWithPlayerHand() {
        // Given
        GameState state = createState(
            Map.of(
                PLAYER_1_ID, Hand.of(ACE_HEARTS, KING_SPADES),
                PLAYER_2_ID, Hand.of(SEVEN_CLUBS)
            )
        );
        given(engine.availableActions(any(), any())).willReturn(List.of());

        // When
        GameView view = viewBuilder.buildView(state, PLAYER_1_ID);

        // Then
        then(view.myHand()).containsExactlyInAnyOrder(ACE_HEARTS.id(), KING_SPADES.id());
        then(view.myTeam()).isEqualTo("TEAM_A");
    }

    @Test
    @DisplayName("should build view with score for both teams")
    void shouldBuildViewWithScore() {
        // Given
        var scoreMap = Map.of(
            Team.TEAM_A, new TokenCount(2, 1),
            Team.TEAM_B, new TokenCount(1, 0)
        );
        GameState state = createStateWithScore(scoreMap);
        given(engine.availableActions(any(), any())).willReturn(List.of());

        // When
        GameView view = viewBuilder.buildView(state, PLAYER_1_ID);

        // Then
        then(view.score()).containsKey("TEAM_A");
        then(view.score()).containsKey("TEAM_B");
        then(view.score().get("TEAM_A").grands()).isEqualTo(2);
        then(view.score().get("TEAM_A").petits()).isEqualTo(1);
        then(view.score().get("TEAM_B").grands()).isEqualTo(1);
        then(view.score().get("TEAM_B").petits()).isZero();
    }

    @Test
    @DisplayName("should build view with available actions from engine")
    void shouldBuildViewWithAvailableActions() {
        // Given
        GameState state = createState(Map.of(
            PLAYER_1_ID, Hand.of(ACE_HEARTS),
            PLAYER_2_ID, Hand.empty()
        ));
        given(engine.availableActions(state, PLAYER_1_ID)).willReturn(List.of(
            new PlayCardAction(PLAYER_1_ID, ACE_HEARTS),
            new TrutAction(PLAYER_1_ID)
        ));

        // When
        GameView view = viewBuilder.buildView(state, PLAYER_1_ID);

        // Then
        then(view.availableActions()).containsExactlyInAnyOrder("PLAY_CARD", "TRUT");
    }

    @Test
    @DisplayName("should build view with trut challenge info when present")
    void shouldBuildViewWithTrutChallenge() {
        // Given
        TrutChallenge challenge = TrutChallenge.create(PLAYER_1_ID, Team.TEAM_A, TrutChallenge.ChallengeType.TRUT);
        GameState state = createStateWithChallenge(challenge);
        given(engine.availableActions(any(), any())).willReturn(List.of());

        // When
        GameView view = viewBuilder.buildView(state, PLAYER_2_ID);

        // Then
        then(view.trutChallenge()).isNotNull();
        then(view.trutChallenge().challengerId()).isEqualTo(PLAYER_1_ID.value());
        then(view.trutChallenge().status()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("should show other players' card count without revealing their cards")
    void shouldShowOtherPlayersCardCount() {
        // Given
        GameState state = createState(Map.of(
            PLAYER_1_ID, Hand.of(ACE_HEARTS, KING_SPADES),
            PLAYER_2_ID, Hand.of(SEVEN_CLUBS)
        ));
        given(engine.availableActions(any(), any())).willReturn(List.of());

        // When
        GameView view = viewBuilder.buildView(state, PLAYER_1_ID);

        // Then — player 1 sees own hand cards directly in myHand
        then(view.myHand()).hasSize(2);

        // Other player shows card count, not actual cards
        GameView.PlayerView otherPlayer = view.players().stream()
            .filter(p -> p.id().equals(PLAYER_2_ID.value()))
            .findFirst()
            .orElseThrow();
        then(otherPlayer.cardCount()).isEqualTo(1);

        // Self shows card count too
        GameView.PlayerView self = view.players().stream()
            .filter(p -> p.id().equals(PLAYER_1_ID.value()))
            .findFirst()
            .orElseThrow();
        then(self.cardCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("should include disconnected players from connection port")
    void shouldIncludeDisconnectedPlayersFromPort() {
        // Given
        GameState state = createState(Map.of(
            PLAYER_1_ID, Hand.of(ACE_HEARTS),
            PLAYER_2_ID, Hand.empty()
        ));
        given(engine.availableActions(any(), any())).willReturn(List.of());
        given(connectionPort.getDisconnectedPlayers("game-1")).willReturn(Set.of(PLAYER_2_ID.value()));

        // When
        GameView view = viewBuilder.buildView(state, PLAYER_1_ID);

        // Then
        then(view.disconnectedPlayers()).containsExactly(PLAYER_2_ID.value());
    }

    // --- helpers ---

    private GameState createState(Map<PlayerId, Hand> hands) {
        List<Player> players = List.of(
            new Player(PLAYER_1_ID, "Alice", Team.TEAM_A, 0, false),
            new Player(PLAYER_2_ID, "Bob", Team.TEAM_B, 1, false)
        );
        return new GameState(
            "game-1", GamePhase.PLAYING_TRICK, players,
            PLAYER_1_ID, PLAYER_1_ID, hands, List.of(), List.of(), Trick.empty(), null,
            Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()),
            1, false, null, Set.of()
        );
    }

    private GameState createStateWithScore(Map<Team, TokenCount> scoreMap) {
        List<Player> players = List.of(
            new Player(PLAYER_1_ID, "Alice", Team.TEAM_A, 0, false),
            new Player(PLAYER_2_ID, "Bob", Team.TEAM_B, 1, false)
        );
        return new GameState(
            "game-1", GamePhase.PLAYING_TRICK, players,
            PLAYER_1_ID, PLAYER_1_ID,
            Map.of(PLAYER_1_ID, Hand.empty(), PLAYER_2_ID, Hand.empty()),
            List.of(), List.of(), Trick.empty(), null,
            scoreMap, 1, false, null, Set.of()
        );
    }

    private GameState createStateWithChallenge(TrutChallenge challenge) {
        List<Player> players = List.of(
            new Player(PLAYER_1_ID, "Alice", Team.TEAM_A, 0, false),
            new Player(PLAYER_2_ID, "Bob", Team.TEAM_B, 1, false)
        );
        return new GameState(
            "game-1", GamePhase.TRUT_CHALLENGE, players,
            PLAYER_1_ID, PLAYER_2_ID,
            Map.of(PLAYER_1_ID, Hand.empty(), PLAYER_2_ID, Hand.empty()),
            List.of(), List.of(), Trick.empty(), challenge,
            Map.of(Team.TEAM_A, TokenCount.zero(), Team.TEAM_B, TokenCount.zero()),
            1, false, null, Set.of()
        );
    }
}
