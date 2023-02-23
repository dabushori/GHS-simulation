package projects.mmn15.nodes.messages;

import projects.mmn15.nodes.nodeImplementations.GHSNode;
import sinalgo.nodes.messages.Message;

/**
 * A message that is broadcasted in the MWOE_BROADCASTING state of the GHS algorithm.
 * This message is used to broadcast the MWOE that was chosen by the root of the fragment to be added to the MST.
 */
public class ChosenMWOEMessage extends Message {
    GHSNode from;
    GHSNode to;
    Integer weight;

    public GHSNode getFrom() {
        return from;
    }

    public GHSNode getTo() {
        return to;
    }

    public Integer getWeight() {
        return weight;
    }

    public ChosenMWOEMessage(MWOESuggestionMessage msg) {
        this.from = msg.getFrom();
        this.to = msg.getTo();
        this.weight = msg.getWeight();
    }

    public ChosenMWOEMessage(GHSNode from, GHSNode to, Integer weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    @Override
    public Message clone() {
        return new ChosenMWOEMessage(from, to, weight);
    }
}

