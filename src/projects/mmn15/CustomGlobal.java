/*
 Copyright (c) 2007, Distributed Computing Group (DCG)
                    ETH Zurich
                    Switzerland
                    dcg.ethz.ch

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the
   distribution.

 - Neither the name 'Sinalgo' nor the names of its contributors may be
   used to endorse or promote products derived from this software
   without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package projects.mmn15;


import projects.mmn15.nodes.edges.WeightedEdge;
import projects.mmn15.nodes.nodeImplementations.GHSNode;
import sinalgo.configuration.Configuration;
import sinalgo.runtime.AbstractCustomGlobal;
import sinalgo.runtime.Runtime;
import sinalgo.tools.Tools;
import sinalgo.tools.Tuple;
import sinalgo.tools.statistics.UniformDistribution;

import java.util.HashMap;
import java.util.Vector;

/**
 * This class holds customized global state and methods for the framework. 
 * The only mandatory method to overwrite is 
 * <code>hasTerminated</code>
 * <br>
 * Optional methods to override are
 * <ul>
 * <li><code>customPaint</code></li>
 * <li><code>handleEmptyEventQueue</code></li>
 * <li><code>onExit</code></li>
 * <li><code>preRun</code></li>
 * <li><code>preRound</code></li>
 * <li><code>postRound</code></li>
 * <li><code>checkProjectRequirements</code></li>
 * </ul>
 * @see AbstractCustomGlobal for more details.
 * <br>
 * In addition, this class also provides the possibility to extend the framework with
 * custom methods that can be called either through the menu or via a button that is
 * added to the GUI. 
 */
public class CustomGlobal extends AbstractCustomGlobal {

	/* (non-Javadoc)
	 * @see runtime.AbstractCustomGlobal#hasTerminated()
	 */
	public boolean hasTerminated() {
		return false;
	}

	// A vector of all the nodes
	Vector<GHSNode> nodes = new Vector<GHSNode>();
	// A uniform distribution between 0 and 1
	UniformDistribution dist = new UniformDistribution(0, 1);
	// The hashmap containing the weights of the edges in the graph
	static HashMap<Tuple<GHSNode, GHSNode>, Integer> weights = new HashMap<>();

	public static Integer getWeight(GHSNode start, GHSNode end) {
		return weights.get(new Tuple<>(start, end));
	}

	/**
	 * Build an undirected weighted graph. Each node chooses 7 other nodes and adds a weighted edge to between it and them.
	 * @param numNodes The number of nodes in the graph.
	 */
	public void buildGraph(int numNodes) {
		if(numNodes <= 0) {
			Tools.showMessageDialog("The number of nodes needs to be at least 1.\nCreation of graph aborted.");
			return;
		}

		// Clear all nodes (if any)
		Runtime.clearAllNodes();
		weights.clear();
		nodes.clear();

		// Create the nodes in a random position uniformly selected from [0, dimX] x [0, dimY]
		for(int i = 0; i < numNodes; ++i) {
			GHSNode node = new GHSNode();
			double x = dist.nextSample() * Configuration.dimX;
			double y = dist.nextSample() * Configuration.dimY;
			node.setPosition(x, y, 0);
			node.finishInitializationWithDefaultModels(true);
			nodes.add(node);
		}

		// Choose 7 random nodes to connect to
		for (GHSNode currNode: nodes) {
			for (int i = 0; i < 7; ++i) {
				GHSNode neighbor = null;
				while (neighbor == null || currNode == neighbor || currNode.outgoingConnections.contains(currNode, neighbor)) {
					neighbor = nodes.get((int) (dist.nextSample() * nodes.size()));
				}
				int weight = WeightedEdge.generateRandomWeight();
				weights.put(new Tuple<>(currNode, neighbor), weight);
				weights.put(new Tuple<>(neighbor, currNode), weight);
				currNode.addConnectionTo(neighbor);
			}
		}

		// Repaint the GUI as we have added some nodes
		Tools.repaintGUI();
	}

	/**
	 * The function that will be executed when clicking on the 'Build Graph' button.
	 */
	@CustomButton(buttonText="Build Graph", toolTipText="Builds a graph")
	public void buildGraph() {
		int numNodes = Integer.parseInt(Tools.showQueryDialog("Number of nodes (n):"));
		buildGraph(numNodes);
	}
}
