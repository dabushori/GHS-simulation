package projects.mmn15.nodes.messages;

import sinalgo.nodes.messages.Message;

/**
 * A message that is broadcasted when the algorithm is finished, i.e. when the root finds out all the nodes in the graph are in its fragment.
 * This message starts the server rerouting processes which makes the server the root of the MST.
 */
public class StartServerReroutingMessage extends Message {
    int startTime;

    public int getStartTime() {
        return startTime;
    }

    public StartServerReroutingMessage(int startTime) {
        this.startTime = startTime;
    }

    @Override
    public Message clone() {
        return new StartServerReroutingMessage(startTime);
    }
}
