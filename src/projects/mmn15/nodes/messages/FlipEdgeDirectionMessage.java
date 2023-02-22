package projects.mmn15.nodes.messages;

import sinalgo.nodes.messages.Message;

public class FlipEdgeDirectionMessage extends Message {
    @Override
    public Message clone() {
        return new FlipEdgeDirectionMessage();
    }
}
