package projects.mmn15.nodes.messages;

import projects.mmn15.nodes.nodeImplementations.GHSNode;
import sinalgo.nodes.messages.Message;

/**
 * A message that is sent in the MWOE_SEARCHING state of the GHS algorithm.
 * This message is used to convergecast the MWOEs to the root of the fragment.
 * Each node sends this message to its parent, after he received this message from all of its children and took the minimal MWOE from them.
 * This message also contains the number of nodes in the subtree of the current node, which is updated in every node according to its children and used to let the root know the number of nodes in its fragment.
 */
public class MWOESuggestionMessage extends Message {
    GHSNode from;
    GHSNode to;
    Integer weight;

    int numOfNodesInSubtree;

    public GHSNode getFrom() {
        return from;
    }

    public GHSNode getTo() {
        return to;
    }

    public Integer getWeight() {
        return weight;
    }

    public int getNumOfNodesInSubtree() {
        return numOfNodesInSubtree;
    }

    public void setNumOfNodesInSubtree(int numOfNodesInSubtree) {
        this.numOfNodesInSubtree = numOfNodesInSubtree;
    }

    public MWOESuggestionMessage(GHSNode from, GHSNode to, Integer weight, int numOfNodesInSubtree) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.numOfNodesInSubtree = numOfNodesInSubtree;
    }

    public MWOESuggestionMessage(GHSNode from, GHSNode to, Integer weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.numOfNodesInSubtree = 0;
    }

    @Override
    public Message clone() {
        return new MWOESuggestionMessage(from, to, weight, numOfNodesInSubtree);
    }
}
