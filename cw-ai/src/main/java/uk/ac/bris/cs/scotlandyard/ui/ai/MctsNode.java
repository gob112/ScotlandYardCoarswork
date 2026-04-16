package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;

import java.util.ArrayList;
import java.util.List;

public class MctsNode {
    public NewGameState state;
    public Move move;
    public MctsNode parent;
    public List<MctsNode> children;

    //for each node
    public double totalScore; //the total score for this node
    public int visitCount; //how many times this node has been visited

    public MctsNode(NewGameState state, Move move, MctsNode parent) {
        this.state = state;
        this.move = move;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.totalScore = 0;
        this.visitCount = 0;
    }

    //to see if all the nodes all already been fully expanded
    public boolean fullyExpanded(DistanceMainCalculator distancecalc) {
        return children.size() == state.filteredAvailableMoves(state.played,distancecalc).size();
    }
}
