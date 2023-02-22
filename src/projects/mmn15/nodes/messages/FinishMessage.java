package projects.mmn15.nodes.messages;

import sinalgo.nodes.messages.Message;

public class FinishMessage extends Message {
    @Override
    public Message clone() {
        return new FinishMessage();
    }
}
