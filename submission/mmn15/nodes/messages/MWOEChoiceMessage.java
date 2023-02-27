package projects.mmn15.nodes.messages;

import sinalgo.nodes.messages.Message;

/**
 * A message that is sent in the MWOE_SEND state of the GHS algorithm.
 * This message indicates that the sender is connecting its fragment to the receiver's fragment using the edge between them.
 */
public class MWOEChoiceMessage extends Message {
    int weight;

    public int getWeight() {
        return weight;
    }

    public MWOEChoiceMessage(int weight) {
        this.weight = weight;
    }

    @Override
    public Message clone() {
        return new MWOEChoiceMessage(weight);
    }
}
