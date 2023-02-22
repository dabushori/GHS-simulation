package projects.mmn15.nodes.messages;

import sinalgo.nodes.messages.Message;

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
