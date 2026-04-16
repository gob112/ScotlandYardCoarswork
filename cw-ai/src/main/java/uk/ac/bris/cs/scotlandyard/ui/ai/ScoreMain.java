package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public class ScoreMain implements ScoreInterface {
    
    public long score(Player MrX, List<Player> detectives,ImmutableList<LogEntry> travelog,DistanceMainCalculator distancematrix){

        final int MrxLocation = MrX.location();
        final List<Integer> detectiveLocations = detectives.stream().map(p->p.location()).toList();
        long MrxNextPosRoutes = distancematrix.graphAcesss.adjacentNodes(MrxLocation).size();
        //TODO: sum of distance to detectives, see if he is surrounded


        if (detectiveLocations.isEmpty()){return 0;} // check if no locations found due to some other error

        int sumOfDistance = 0;
        for (int location:detectiveLocations){ // calcs distance of each detective with respect to mrx's current location
            sumOfDistance += distancematrix.getdistance(MrxLocation,location);
        }
        int avarageDistance = sumOfDistance/detectiveLocations.size();

        ImmutableList<LogEntry> log = travelog;
        int currentMove = log.size() + 1;
        boolean isRvealRound = ScotlandYard.REVEAL_MOVES.contains(currentMove);
        long totalScore = 0;

        if (avarageDistance < 2){
            totalScore -= Integer.MAX_VALUE / 6; // he is likely to be surrounded as the avarage suggest that many are around 2 nodes away.
            //high penalty
        } else {
            if (isRvealRound) {
                totalScore += avarageDistance * 120;
            } else {
                totalScore += avarageDistance * 100;
            }
        }





        long count = detectiveLocations.stream()
                .filter(x -> distancematrix.getdistance(MrxLocation, x) < 2)
                .count();
        if(count == MrxNextPosRoutes){
            totalScore -= Integer.MAX_VALUE / 2;
        } else if (count < MrxNextPosRoutes && isRvealRound) {
            totalScore -= (MrxNextPosRoutes * 250);
        } else if (count < MrxNextPosRoutes && !isRvealRound) {
            totalScore -= (MrxNextPosRoutes * 200);
        }


        int ticketBonus = 0;
        ticketBonus += MrX.tickets().getOrDefault(ScotlandYard.Ticket.SECRET, 0) * 50;
        ticketBonus += MrX.tickets().getOrDefault(ScotlandYard.Ticket.DOUBLE, 0) * 40;
        ticketBonus += MrX.tickets().getOrDefault(ScotlandYard.Ticket.UNDERGROUND, 0) * 40;
        ticketBonus += MrX.tickets().getOrDefault(ScotlandYard.Ticket.BUS, 0) * 10;
        ticketBonus += MrX.tickets().getOrDefault(ScotlandYard.Ticket.TAXI, 0) * 5;
        totalScore += ticketBonus;


        int minDistance = detectiveLocations.stream()
                .mapToInt(x -> distancematrix.getdistance(MrxLocation, x))
                .min()
                .getAsInt();

        if (minDistance < 2) {
            totalScore -= Integer.MAX_VALUE / 2;
        } else if (minDistance <= 3){
            totalScore += minDistance * minDistance * 100;
        } else {
            totalScore += minDistance * minDistance * 200; //encourage mrx to go further
        }




        // encourage mrx to go further
        if(!travelog.isEmpty()){
            int avgOldDist = travelog.stream()
                    .mapToInt(e -> e.location().orElse(-1))
                    .filter(pos -> pos != -1)
                    .map(oldPos -> distancematrix.getdistance(oldPos,MrxLocation))
                    .sum();
            totalScore += avgOldDist * 18; //the further to the positions he has been the higher score he will get
        }

        //if its equal-distances moves, force one-way movement to avoid back-and-forth
        totalScore += (MrxLocation / 10) * 8;


        return totalScore;
    };
}
