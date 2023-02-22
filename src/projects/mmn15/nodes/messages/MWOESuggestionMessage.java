package projects.mmn15.nodes.messages;

import projects.mmn15.nodes.nodeImplementations.GHSNode;
import sinalgo.nodes.messages.Message;

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

    public int getNumOfNodesInSubtree() { return numOfNodesInSubtree; }
    public void setNumOfNodesInSubtree(int numOfNodesInSubtree) { this.numOfNodesInSubtree = numOfNodesInSubtree; }

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
