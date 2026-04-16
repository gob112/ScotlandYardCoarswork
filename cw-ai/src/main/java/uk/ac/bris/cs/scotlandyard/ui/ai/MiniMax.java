package uk.ac.bris.cs.scotlandyard.ui.ai;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.List;
import java.util.Set;


public class MiniMax implements MiniMaxInterface {

    private final ScoreMain score;
    private final DistanceMainCalculator calc;

    public MiniMax(ScoreMain score, DistanceMainCalculator calc) {
        this.score = score;
        this.calc = calc;
    }

    @Override
    public long Minimax( NewGameState currentState, Integer depth, long alpha, long beta, boolean ismrx) {

        List<Player> detectives =currentState.detectives;

        Player mrX = currentState.mrX;
        GameSetup setup = currentState.setup;
        ImmutableList<LogEntry> travelLog = currentState.travelLog;


        //logic to consider who is playing
        //mrx max
        //detectives min


        if (depth == 0 || !currentState.winner().isEmpty()) {
            ImmutableSet<Piece> winners = currentState.winner();
            if (!winners.isEmpty()) {

                if (winners.contains(currentState.mrX.piece())) {

                    return Integer.MAX_VALUE;
                } else {
                    return Integer.MIN_VALUE;
                }
            }
            return score.score(mrX, detectives, currentState.travelLog, calc);
        }



        Set<Move> availableMoves = currentState.filteredAvailableMoves(currentState.played,calc);



        if (ismrx) {
            long maxScore = Long.MIN_VALUE;
            for (Move move : availableMoves) {

                NewGameState nextstate = currentState.advanceState(move);

                long childScore = Minimax(nextstate,depth - 1, alpha, beta, false);


                for (Integer i:nextstate.detectiveLocations){
                    if(calc.getdistance(nextstate.mrX.location(),i)<=1){
                        childScore+=Integer.MIN_VALUE/4;
                        break;
                    }
                }

                maxScore = Math.max(maxScore, childScore);

                alpha = Math.max(alpha, childScore);
                if (beta <= alpha) {
                    break;
                }
                currentState.moves=null;
            }
            return maxScore;
        } else {

            long minScore = Long.MAX_VALUE;
            for (Move move : availableMoves) {
                NewGameState nextstate = currentState.advanceState(move);
                long childScore;
                if (nextstate.played.isEmpty()){
                     childScore = Minimax(nextstate, depth - 1, alpha, beta, true);

                }else{
                     childScore = Minimax(nextstate, depth - 1, alpha, beta, false);
                }

                currentState.moves=null;
                minScore = Math.min(minScore, childScore);
                beta = Math.min(beta, childScore);
                if (beta <= alpha) {
                    break;
                }
            }
            return minScore;
        }
    }
}
