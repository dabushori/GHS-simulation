package projects.mmn15.nodes.edges;

import projects.mmn15.CustomGlobal;
import projects.mmn15.nodes.nodeImplementations.GHSNode;
import sinalgo.nodes.edges.BidirectionalEdge;
import sinalgo.tools.statistics.UniformDistribution;

import java.awt.*;

/**
 * A weighted edge which its weight is uniformly selected from [1, 1000000000].
 */
public class WeightedEdge extends BidirectionalEdge {
    public static int MIN_WEIGHT = 1, MAX_WEIGHT = 1000000000;

    int weight;
    public static UniformDistribution weightDist = new UniformDistribution(MIN_WEIGHT, MAX_WEIGHT);

    /**
     * Generate a random weight using the uniform distribution.
     *
     * @return A random weight from [1, 1000000000]
     */
    public static int generateRandomWeight() {
        return (int) weightDist.nextSample();
    }

    /**
     * Initialize the weight using the weight hashmap if the edge exists, otherwise initialize it using a random weight.
     */
    @Override
    public void initializeEdge() {
        super.initializeEdge();
        Integer weight = CustomGlobal.getWeight(startNode.ID, endNode.ID);
        if (weight == null) {
            weight = generateRandomWeight();
        }
        this.weight = weight;
    }

    /**
     * Get the edge's weight.
     *
     * @return The edge's weight
     */
    public int getWeight() {
        return weight;
    }

    /**
     * Override the toString method to view the weight in the edge info menu.
     *
     * @return "Weight: <edge weight>"
     */
    @Override
    public String toString() {
        return "Weight: " + getWeight();
    }

    /**
     * Override the getColor method to color the edges that are in the MST in green.
     *
     * @return The color of the edge
     */
    @Override
    public Color getColor() {
        GHSNode start = (GHSNode) startNode, end = (GHSNode) endNode;
        // If the edge is in the MST, which is determined by whether one of the nodes is the parent of the other
        if ((start.getParent() != null && start.getParent().equals(end)) || (end.getParent() != null && end.getParent().equals(start))) {
            return Color.green;
        }
        return super.getColor();
    }
}
