package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.GameSetup;
import uk.ac.bris.cs.scotlandyard.model.LogEntry;
import uk.ac.bris.cs.scotlandyard.model.Player;

import javax.annotation.Nonnull;
import java.util.List;

public interface MiniMaxInterface {
    long Minimax(NewGameState currentState, Integer depth, long alpha, long beta, boolean isMrx);
}
