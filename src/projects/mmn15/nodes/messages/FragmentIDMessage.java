package projects.mmn15.nodes.messages;

import sinalgo.nodes.messages.Message;

/**
 * A message that is sent in the FRAGMENT_ID_DISCOVERY state of the GHS algorithm.
 * This message indicates that the neighbor has changed its fragment ID.
 */
public class FragmentIDMessage extends Message {
    int id;

    public int getId() {
        return id;
    }

    public FragmentIDMessage(int id) {
        this.id = id;
    }

    @Override
    public Message clone() {
        return new FragmentIDMessage(id);
    }
}
