package projects.mmn15.nodes.messages;

import sinalgo.nodes.messages.Message;

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
