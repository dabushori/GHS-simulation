package projects.mmn15.nodes.edges;

import projects.mmn15.CustomGlobal;
import projects.mmn15.nodes.nodeImplementations.GHSNode;
import sinalgo.nodes.edges.BidirectionalEdge;
import sinalgo.tools.statistics.UniformDistribution;

/**
 * A weighted edge which its weight is uniformly selected from [1, 1000000000]
 */
public class WeightedEdge extends BidirectionalEdge {

	int weight;
	public static UniformDistribution weightDist = new UniformDistribution(1, 1000000000);

	/**
	 * Generate a random weight using the uniform distribution.
	 * @return A random weight from [1, 1000000000]
	 */
	public static int generateRandomWeight() {
		return (int) weightDist.nextSample();
	}

	/**
	 * Initialize the weight using the weight hashmap if the edge exists, otherwise initialize it using a random weight.
	 */
	public WeightedEdge() {
		Integer weight = CustomGlobal.getWeight((GHSNode) startNode, (GHSNode) endNode);
		if (weight == null) {
			weight = generateRandomWeight();
		}
		this.weight = weight;
	}

	/**
	 * Get the edge's weight.
	 * @return The edge's weight
	 */
	public int getWeight() {
		return weight;
	}

	/**
	 * Override the toString method to view the weight in the edge info menu.
	 * @return "Weight: <edge weight>"
	 */
	@Override
	public String toString() {
		return "Weight: " + getWeight();
	}
}
