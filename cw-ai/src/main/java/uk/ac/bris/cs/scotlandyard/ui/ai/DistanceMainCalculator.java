package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.GameSetup;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard;
import java.util.Arrays;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;


public class DistanceMainCalculator implements DistanceCalculator {
    //creates 2d matrix 200x200
    //[node][destinations]
    private final int[][] matrix = new int[200][200];//199 nodes shown in pos.txt of cw-model
    private final int[][] prev = new int[200][200];
    private final int[][] nextNode = new int[200][200];
    public Set<Integer> adjNodesMrx;
    public ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graphAcesss;

    public DistanceMainCalculator(GameSetup setup){
        this.graphAcesss = setup.graph;

        //[node] = big value
        for (int i = 0; i < 200; i++) {
            Arrays.fill(matrix[i], 100000); // if Integer.Max_VALUE when + with wieght causes overflow
            Arrays.fill(prev[i], -1);
        }
        // finds shortest distance from each node to every other node
        for (Integer node : setup.graph.nodes()) {
            Dijkstras(node, setup.graph); // considers all nodes as source and finds shortest path
        }
        for (Integer source : setup.graph.nodes()) {
            for (Integer destination : setup.graph.nodes()) {
                nextNode[source][destination] = computeNextNode(source, destination);
            }
        }
    }
    private int computeNextNode(int source, int destination) {
        if (source == destination) return source;
        if (prev[source][destination] == -1) return -1; // unreachable

        // walk prev chain backwards until we find the node
        // whose predecessor is the source
        int current = destination;
        while (prev[source][current] != source) {
            current = prev[source][current];
        }
        return current; // this is the first step from source toward destination
    }


    @Override
    public void Dijkstras(Integer source, ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph){
       //Priority queue to help choose next best node to explore,
        //implements queue which extends collection
        PriorityQueue<Integer[]> pqueue = new PriorityQueue<>((a, b) -> (a[0] - b[0]));// order based on distance, entry a[value,node] and b[value,node]
        matrix[source][source] = 0; // sets source to have distance of 0
        pqueue.add(new Integer[]{0,source}); // add to queue
        Set<Integer> processed = new HashSet<>();

        while (!pqueue.isEmpty()){
            Integer[] first = pqueue.poll();//get top item from queue
            // priority queue stores [value,node] extract them to individual variables
            Integer value = first[0];
            Integer node = first[1];

            if (value > matrix[source][node] || processed.contains(node)) {
                continue;
            }

            processed.add(node);

            for(Integer adj:graph.adjacentNodes(node)){ // else we look at each of its adjacent nodes
                int weight = 1; // moving from one node to another has the same weight of one, as only 1 ticket being used
                // in score will wight it more based on type

                int newWeight = matrix[source][node] + weight; // store what moving from current node to adjacent wieght be
                if (newWeight < matrix[source][adj]){ // if less than the one stored in the matrix
                    matrix[source][adj] = newWeight;
                    prev[source][adj] = node;
                    pqueue.add(new Integer[]{newWeight,adj}); // update matrix and add to queue
                }

            }

        }

    }


    @Override
    public Integer getdistance(Integer source, Integer destination){
        this.adjNodesMrx = graphAcesss.adjacentNodes(source);
        return matrix[source][destination]; // gets shortest distance from specific source to destination
    }
    public Integer getNextBestMove(Integer source, Integer destination){

        return nextNode[source][destination]; // gets shortest distance from specific source to destination
    }


}
