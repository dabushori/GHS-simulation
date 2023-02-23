package projects.mmn15.nodes.messages;

import sinalgo.nodes.messages.Message;

/**
 * A message that is broadcasted when the algorithm is finished, i.e. when the root finds out all the nodes in the graph are in its fragment.
 */
public class FinishMessage extends Message {
    @Override
    public Message clone() {
        return new FinishMessage();
    }
}
