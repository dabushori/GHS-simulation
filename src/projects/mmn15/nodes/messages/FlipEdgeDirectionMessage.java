package projects.mmn15.nodes.messages;

import sinalgo.nodes.messages.Message;

/**
 * A message that is sent in the NEW_ROOT_BROADCASTING state of the GHS algorithm.
 * This message indicates that there's a new root to the fragment, and is sent on the route from the new root to the old one to flip the edges' direction.
 */
public class FlipEdgeDirectionMessage extends Message {
    @Override
    public Message clone() {
        return new FlipEdgeDirectionMessage();
    }
}
