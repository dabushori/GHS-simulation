package projects.mmn15.nodes.messages;

import sinalgo.nodes.messages.Message;

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
