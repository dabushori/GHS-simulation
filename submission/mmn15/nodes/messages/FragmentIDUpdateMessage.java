package projects.mmn15.nodes.messages;

import sinalgo.nodes.messages.Message;

/**
 * A message that is broadcasted in the FRAGMENT_ID_DISCOVERY state of the GHS algorithm.
 * This message is used by the root to broadcast its ID as the new fragment ID.
 */
public class FragmentIDUpdateMessage extends Message {
    int id;

    public int getId() {
        return id;
    }

    public FragmentIDUpdateMessage(int id) {
        this.id = id;
    }

    @Override
    public Message clone() {
        return new FragmentIDUpdateMessage(id);
    }
}
