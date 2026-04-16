package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import uk.ac.bris.cs.scotlandyard.model.*;

import java.util.List;

public interface ScoreInterface {
   long score(Player MrX, List<Player> detectives , ImmutableList<LogEntry> travelog, DistanceMainCalculator distancematrix);
}
