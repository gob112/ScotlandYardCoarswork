package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Move;
import java.util.Comparator;
import java.util.NoSuchElementException;

//mctsnode function(fully expanded)
//expand mcts search
//minimax as well
public class MctsSearch {
    private final MiniMax miniMax; //here we use minimax for simulation
    private final ScoreMain scoreMain;
    private final DistanceMainCalculator distanceCalculator;

    public MctsSearch (MiniMax miniMax, ScoreMain scoreMain, DistanceMainCalculator distanceCalculator) {
        this.miniMax = miniMax;
        this.scoreMain = scoreMain;
        this.distanceCalculator = distanceCalculator;
    }

    //pick the best move based on the current state
    public Move getBestMove(NewGameState rootState, int iterations) {
        MctsNode rootNode = new MctsNode(rootState, null, null);

        //MCTS tree
        for (int i = 0; i < iterations; i++) {

            MctsNode selectedNode = select(rootNode);
            MctsNode expandedNode = expand(selectedNode);
            long simulationResult = rollout(expandedNode.state);
            backpropagate(expandedNode, simulationResult);
        }
        return getBestChild(rootNode).move;
    }

    //selection
    private MctsNode select(MctsNode node) {
        if (!node.fullyExpanded(distanceCalculator) || node.state.statelessAvailableMoves(node.state.played).isEmpty()) {
            return node;
        }

        // choose the node based on UCB
        return node.children.stream()
                .max(Comparator.comparingDouble(this::calculateUCB))
                .map(this::select)
                .orElse(node);
    }

    //expansion
    private MctsNode expand(MctsNode node) {
        if (node.visitCount==0){
            return node;
        }
        var availableMoves = node.state.filteredAvailableMoves(node.state.played,distanceCalculator);

        // choose the first move we haven't tried
        for (Move move : availableMoves) {
            boolean exists = node.children.stream()
                    .anyMatch(child -> child.move.equals(move));
            if (!exists) {

                NewGameState nextState = node.state.advanceState(move);
                MctsNode newNode = new MctsNode(nextState, move, node);
                node.children.add(newNode);
                node.state.moves=null;
                return newNode;

            }

            node.state.moves=null;
        }
        return node.children.isEmpty()?node:node.children.get(0);
    }

    //rollout(simulation with our minimax)
    private long rollout(NewGameState state) {
        int fixedDepth = 3;
        return miniMax.Minimax(
                state,
                fixedDepth,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                state.isMrxTurn()
        );
    }

    //backpropagation
    private void backpropagate(MctsNode node,long result) {
        MctsNode current = node;

        while (current != null) {
            current.visitCount++;
            // flip score perspective based on whose turn it is
            current.totalScore += current.state.isMrxTurn() ? result : -result;
            current = current.parent;
        }
    }

    //our UCB
    private double calculateUCB(MctsNode node) {
        if (node.visitCount == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double exploitation = node.totalScore / node.visitCount;
        double exploration = Math.sqrt(Math.log(node.parent.visitCount) / node.visitCount);
        return exploitation + 1.0 * exploration;
    }

    //choose the move with the highest score
    private MctsNode getBestChild(MctsNode rootNode) {

        return rootNode.children.stream()
                .max(Comparator.comparingDouble(child -> {
                    double value = (double) child.totalScore/child.visitCount;

                    return value;
                }))
                .map(child -> {
                    double maxValue = (double) child.totalScore;

                    return child;
                })
                .orElseThrow(() -> new NoSuchElementException("No available moves"));
    }
}
