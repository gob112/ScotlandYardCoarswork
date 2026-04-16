package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.atlassian.fugue.Option;
import jakarta.annotation.Nonnull;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class MyAi implements Ai {

	private final ScoreMain score = new ScoreMain();

	@Nonnull @Override public String name() { return "MyAi"; }

	@Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
//---------------------------------------------------------------------------------------------------------------------------------


		ImmutableMap<Piece, ImmutableMap<ScotlandYard.Ticket, Integer>> tickets = Objects.requireNonNull(
				board.getPlayers().stream().collect(ImmutableMap.toImmutableMap(
						Function.identity(), x -> {
							Board.TicketBoard b = board.getPlayerTickets(x).orElseThrow();
							return Stream.of(ScotlandYard.Ticket.values()).collect(ImmutableMap.toImmutableMap(
									Function.identity(), b::getCount));
						})));

		ImmutableMap<Piece.Detective, Integer> detectiveLocations = Objects.requireNonNull(board.getPlayers().stream()
				.filter(Piece::isDetective)
				.map(Piece.Detective.class::cast)
				.collect(ImmutableMap.toImmutableMap(Function.identity(),
						x1 -> board.getDetectiveLocation(x1).orElseThrow())));

		List<Player> DetectivePlayer = detectiveLocations.keySet().stream().map(d->new Player(d,tickets.get(d),detectiveLocations.get(d))).toList();
		int MrXLocation;
		if(board.getMrXTravelLog().isEmpty()){
			MrXLocation=board.getAvailableMoves().stream().findFirst().get().source();

		}else{
			Optional<Integer> MrXLocationtoUpdate =
					NewGameState.revealedtravelLog.get(NewGameState.revealedtravelLog.size() - 1).location();
			MrXLocation = MrXLocationtoUpdate.get();

		}


		Player mrX = board.getPlayers().stream()
				.filter(Piece::isMrX)
				.map(p -> new Player(p, tickets.get(p), MrXLocation))
				.findAny()
				.orElseThrow();


		ImmutableList<LogEntry> travalLog = board.getMrXTravelLog();
		GameSetup setup = board.getSetup();
;//---------------------------------------------------------------------------------------------------------------------------------
		final DistanceMainCalculator calc = new DistanceMainCalculator(board.getSetup()); //need to have the real class
		final MiniMax finalmove = new MiniMax(score, calc);
		MctsSearch mcts = new MctsSearch(finalmove, score, calc);
		/// update current move




		Integer maxscore = Integer.MIN_VALUE;

		NewGameState rootState = new NewGameState(setup,DetectivePlayer,mrX,travalLog,board.getAvailableMoves(),true,ImmutableSet.of(),NewGameState.roundNum,ImmutableSet.of());
		Move bestmove = mcts.getBestMove(rootState, 500);

		//need to check if this null check is still valid and only happens if all moves wehre equally horrible or is there some other edge case that causes it as well
		if (bestmove == null) {
			bestmove = board.getAvailableMoves().iterator().next();

		}

		NewGameState.UpdateRevealedTravelLog(bestmove);
		if (bestmove instanceof Move.DoubleMove){
			NewGameState.roundNum+=2;
		}else{
			NewGameState.roundNum+=1;
		}
	 return bestmove;
	}
}
