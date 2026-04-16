package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NewGameStateInterface {

    NewGameState advanceState(Move move);
    Optional<Board.TicketBoard> playerTickets(Piece piece);
    ImmutableSet<Move> availableMoves();
    ImmutableSet<Piece> winner();

}
