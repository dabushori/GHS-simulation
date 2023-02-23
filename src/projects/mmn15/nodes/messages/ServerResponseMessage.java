package projects.mmn15.nodes.messages;

import projects.mmn15.nodes.nodeImplementations.GHSNode;
import sinalgo.nodes.messages.Message;

import java.util.Vector;

/**
 * A message that is sent all the way down from the server.
 * This message represents a response from the server.
 */
public class ServerResponseMessage extends Message {
    String message;
    Vector<GHSNode> route;

    public String getMessage() {
        return message;
    }

    public Vector<GHSNode> getRoute() {
        return new Vector<>(route);
    }

    /**
     * A regular constructor.
     *
     * @param message The message to send to the server
     * @param route   The route to the current node in the MST
     */
    public ServerResponseMessage(String message, Vector<GHSNode> route) {
        this.message = message;
        this.route = new Vector<>(route);
    }

    /**
     * Generate the server response from a given message.
     *
     * @param message The given message
     * @return The server's response
     */
    public static String generateServerResponse(String message) {
        return "Your message: [" + message + "] is received successfully, and can now be processed";
    }

    /**
     * A constructor that copies the route from a request message and generates the response from the message in that request.
     * @param req The request message
     */
    public ServerResponseMessage(ServerRequestMessage req) {
        this.message = generateServerResponse(req.getMessage());
        this.route = new Vector<>(req.getRoute());
    }

    /**
     * A copy constructor for the clone method.
     * @param other The other ServerResponseMessage object
     */
    public ServerResponseMessage(ServerResponseMessage other) {
        this.message = other.message;
        this.route = new Vector<>(other.route);
    }

    @Override
    public Message clone() {
        return new ServerResponseMessage(this);
    }
}
