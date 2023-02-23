package projects.mmn15.nodes.nodeImplementations;

import projects.mmn15.CustomGlobal;
import projects.mmn15.nodes.edges.WeightedEdge;
import projects.mmn15.nodes.messages.*;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.nodes.Node;
import sinalgo.nodes.edges.Edge;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import sinalgo.tools.Tools;

import java.util.HashMap;
import java.util.Vector;

/**
 * A node that implements the GHS algorithm.
 */
public class GHSNode extends Node {
    // The current fragment ID of the current node
    int fragmentID;
    // The fragment ID of each neighbor during the algorithm
    HashMap<GHSNode, Integer> nodesToFragmentID = new HashMap<>();
    // The edges to each neighbor (it is used to access its weight in O(1) instead of iterating over all the outgoingConnections linked list
    HashMap<GHSNode, WeightedEdge> neighborsToEdges = new HashMap<>();
    // The children of the node in the MST (empty if it is a leaf)
    Vector<GHSNode> children = new Vector<>();
    // The parent of the node in the MST (null if it is the root)
    GHSNode parent;
    // A temporary member where the root saves the new MWOE to add in every iteration
    MWOESuggestionMessage mwoeToAdd;
    // The queue of the MWOE suggestions of the children. Here each node saves all the MWOE suggestion of its children, and uses it to find the minimal edge and forward it to its parent.
    public Vector<MWOESuggestionMessage> mwoeQueue = new Vector<>();
    // The new candidate for a parent (the other edge of the MWOE)
    public GHSNode parentCandidate;
    // A variable that indicates whether the current node is the root of its fragment
    boolean isRoot;
    boolean isServer;

    /**
     * Get the children vector of the node.
     *
     * @return The children vector of the node
     */
    public Vector<GHSNode> getChildren() {
        return children;
    }

    /**
     * Get the parent of the node.
     *
     * @return The parent of the node
     */
    public GHSNode getParent() {
        return parent;
    }

    /**
     * Change the value of the isServer variable, which indicates whether the current node is the server or not.
     *
     * @param isServer the new value of isServer
     */
    public void setIsServer(boolean isServer) {
        this.isServer = isServer;
    }

    /**
     * Get the weight of an edge to a given neighbor (and null if it is not a neighbor).
     *
     * @param neighbor The given neighbor
     * @return The weight of the edge between `this` and `neighbor`
     */
    private Integer getWeightOfEdgeTo(GHSNode neighbor) {
        if (neighbor == null) return null;
        return neighborsToEdges.get(neighbor).getWeight();
    }

    /**
     * Get the neighbor of the other side of the edge with the minimum weight which connects the node to a node from another fragment.
     *
     * @return The spoken neighbor, and null if there isn't one
     */
    public GHSNode getMinimumWeightEdge() {
        int minWeight = WeightedEdge.MAX_WEIGHT + 1; // max value
        GHSNode minimumWeightNeighbor = null;
        for (Edge e : outgoingConnections) {

            GHSNode neighbor = (GHSNode) e.endNode;
            if (nodesToFragmentID.get(neighbor) == fragmentID) continue;
            int weight = getWeightOfEdgeTo(neighbor);
            if (weight < minWeight) {
                minWeight = weight;
                minimumWeightNeighbor = neighbor;
            }
        }
        return minimumWeightNeighbor;
    }

    /**
     * Reset the parameters and restart the GHS algorithm.
     */
    public void startGHS() {
        // Reset all the records (i.e. restart the algorithm)
        nodesToFragmentID.clear();
        neighborsToEdges.clear();
        mwoeQueue.clear();
        children.clear();
        parent = null;
        mwoeToAdd = null;

        // Initialize iteration and round counters
        roundCounter = 0;

        // Each node is its fragment root in the first iteration, so each node will connect its fragment to another fragment using the MWOE he finds
        isRoot = true;

        fragmentID = ID;
        for (Edge e : outgoingConnections) {
            neighborsToEdges.put((GHSNode) e.endNode, (WeightedEdge) e);
            nodesToFragmentID.put((GHSNode) e.endNode, e.endNode.ID);
        }

        // Find the first MWOE and change the state to MWOE_SEND to start the algorithm
        parentCandidate = getMinimumWeightEdge();
        currentState = GHSStates.MWOE_SEND;
    }

    /**
     * The possible states of the algorithm.
     */
    enum GHSStates {
        NOT_STARTED, // not initialized yet
        MWOE_SEND, // connect fragments using the chosen MWOE (MWOE == Minimum Weight Outgoing Edge)
        LEADER_DISCOVERY, // find the new root of each fragment (which will be the maximum ID node which chose a node that chose it too)
        FRAGMENT_ID_DISCOVERY, // broadcast the new fragment ID
        MWOE_SEARCHING, // convergecast the MWOE to the root of the fragment
        MWOE_BROADCASTING, // broadcast the MWOE of the fragment which will be added to the MST
        NEW_ROOT_BROADCASTING, // broadcast the new root of the fragment and flip the edges on the route from it to the old root of the fragment
        SERVER_REROUTING, // redirect the edges to make the server the root of the MST
        FINISHED, // The algorithm has finished
    }

    /**
     * Return whether the algorithm has finished or not.
     *
     * @return true if the algorithm has finished, false otherwise
     */
    public boolean hasFinished() {
        return currentState == GHSStates.FINISHED;
    }

    // A round counter to insure each phase (which needs to) takes n rounds
    int roundCounter = 0;
    // The current state of the algorithm at the current node
    public GHSStates currentState = GHSStates.NOT_STARTED;

    /**
     * Switch the current state to the next state, based on the iteration and round counters.
     * These states require 1 round:
     * - MWOE_SEND
     * - LEADER_DISCOVERY
     * These states require n rounds:
     * - FRAGMENT_ID_DISCOVERY
     * - MWOE_SEARCHING
     * - MWOE_BROADCASTING
     * - NEW_ROOT_BROADCASTING
     * The lists are sorted by the order of the states.
     * Once the root notices that all the nodes in the network are in his fragment, the server rerouting starts.
     * The server rerouting takes 2n rounds from the moment the root noticed that all the nodes are in his fragments.
     */
    public void switchState() {
        switch (currentState) {
            case NOT_STARTED:
                currentState = GHSStates.MWOE_SEND;
                break;
            case MWOE_SEND:
                currentState = GHSStates.LEADER_DISCOVERY;
                break;
            case LEADER_DISCOVERY:
                currentState = GHSStates.FRAGMENT_ID_DISCOVERY;
                roundCounter = 0;
                break;
            case FRAGMENT_ID_DISCOVERY:
                if (++roundCounter == CustomGlobal.getNumOfNodes()) {
                    currentState = GHSStates.MWOE_SEARCHING;
                    roundCounter = 0;
                }
                break;
            case MWOE_SEARCHING:
                if (++roundCounter == CustomGlobal.getNumOfNodes()) {
                    currentState = GHSStates.MWOE_BROADCASTING;
                    roundCounter = 0;
                }
                break;
            case MWOE_BROADCASTING:
                if (++roundCounter == CustomGlobal.getNumOfNodes()) {
                    currentState = GHSStates.NEW_ROOT_BROADCASTING;
                    roundCounter = 0;
                }
                break;
            case NEW_ROOT_BROADCASTING:
                if (++roundCounter == CustomGlobal.getNumOfNodes()) {
                    roundCounter = 0;
                    currentState = GHSStates.MWOE_SEND;
                }
                break;
            case SERVER_REROUTING:
                if (++roundCounter == 2 * CustomGlobal.getNumOfNodes()) {
                    currentState = GHSStates.FINISHED;
                    roundCounter = 0;
                }
                break;
            case FINISHED:
                break;
        }
    }

    public void startServerRerouting(StartServerReroutingMessage msg) {
        currentState = GHSStates.SERVER_REROUTING;
        roundCounter = ((int) Tools.getGlobalTime()) - msg.getStartTime();
    }

    /**
     * An iteration of the MWOE_SEND state.
     * In this state, if the node is a root (which means its MWOE has been chosen), he connects to the fragment of the node on the other side of the MWOE by sending a MWOEChoiceMessage.
     *
     * @param inbox The inbox of the node
     */
    public void mwoeSendIter(Inbox inbox) {
        // If the current node is the node with the lightest edge that goes out of the fragment, add this edge to the MST and connect the fragments.
        if (isRoot) {
            send(new MWOEChoiceMessage(getWeightOfEdgeTo(parentCandidate)), parentCandidate);
        }
    }

    /**
     * An iteration of the LEADER_DISCOVERY state.
     * In this state, the new root of each fragment is chosen. The new root will be the maximum ID node which the node in the other side of the MWOE chose it too.
     * The children and parent members are also updated according to every node that chose an MWOE which is connected to the current node.
     *
     * @param inbox The inbox of the node
     */
    public void leaderDiscoveryIter(Inbox inbox) {
        while (inbox.hasNext()) {
            Message msg = inbox.next();
            if (msg instanceof MWOEChoiceMessage) {
                GHSNode node = (GHSNode) inbox.getSender();
                // Case of the parentCandidate (the node in the other side of the MWOE) choosing you too
                if (node.equals(parentCandidate)) {
                    // If the other node has the maximal ID mark it as this node's parent
                    if (parentCandidate.ID > ID) {
                        parent = parentCandidate;
                        parentCandidate = null;
                        isRoot = false;
                    }
                    // If the current node has the maximal ID mark it as a root
                    else {
                        children.add(node);
                        parentCandidate = null;
                        isRoot = true;
                    }
                }
                // If a node which is not the parentCandidate chooses you as the other side of its MWOE (you are its parentCandidate) add it as your child
                else {
                    children.add(node);
                }
            }
            // Listen for a start server rerouting message
            else if (msg instanceof StartServerReroutingMessage) {
                for (GHSNode child : children) {
                    send(msg, child);
                }
                startServerRerouting((StartServerReroutingMessage) msg);
            }
        }
        // If the parentCandidate didn't choose you (very sad) add the chosen MWOE to the fragment and mark it as a parent
        if (parentCandidate != null) {
            parent = parentCandidate;
            parentCandidate = null;
            isRoot = false;
        }
    }

    /**
     * An iteration of the FRAGMENT_ID_DISCOVERY state.
     * In this state, the new root of each fragment broadcasts its ID as the new fragment ID.
     * In addition, each node that receives a new fragment ID updates all its neighbors with the new fragment ID.
     *
     * @param inbox The inbox of the node
     */
    public void fragmentIDDiscoveryIter(Inbox inbox) {
        // If the current node is the root of the fragment, send the node's id to all of its children only if it is the first iteration of this state
        if (isRoot) {
            // If it is the first iteration of this state send the node's id (which is the new fragment id) to its children
            if (roundCounter == 0) {
                fragmentID = ID;
                for (GHSNode child : children) {
                    send(new FragmentIDUpdateMessage(fragmentID), child);
                }
                // Update neighbors about my new fragment id
                for (Edge e : outgoingConnections) {
                    send(new FragmentIDMessage(fragmentID), e.endNode);
                }
            }
            // Listen for updates of the neighbors about their fragment id
            while (inbox.hasNext()) {
                Message msg = inbox.next();
                if (msg instanceof FragmentIDMessage) {
                    nodesToFragmentID.put((GHSNode) inbox.getSender(), ((FragmentIDMessage) msg).getId());
                }
                // Listen for a start server rerouting message
                else if (msg instanceof StartServerReroutingMessage) {
                    for (GHSNode child : children) {
                        send(msg, child);
                    }
                    startServerRerouting((StartServerReroutingMessage) msg);
                }
            }
        }
        // If the current node is not the root of the fragment, update the fragment id according to the received id and pass it to the node's children
        else {
            while (inbox.hasNext()) {
                Message msg = inbox.next();
                if (msg instanceof FragmentIDUpdateMessage) {
                    fragmentID = ((FragmentIDUpdateMessage) msg).getId();
                    for (GHSNode child : children) {
                        send(msg, child);
                    }
                    // Update neighbors about my new fragment id
                    for (Edge e : outgoingConnections) {
                        send(new FragmentIDMessage(fragmentID), e.endNode);
                    }
                }
                // Listen for updates of the neighbors about their fragment ids
                else if (msg instanceof FragmentIDMessage) {
                    nodesToFragmentID.put((GHSNode) inbox.getSender(), ((FragmentIDMessage) msg).getId());
                }
                // Listen for a start server rerouting message
                else if (msg instanceof StartServerReroutingMessage) {
                    for (GHSNode child : children) {
                        send(msg, child);
                    }
                    startServerRerouting((StartServerReroutingMessage) msg);
                }
            }
        }
    }

    /**
     * An iteration of the MWOE_SEARCHING state.
     * In this state, each node convergecasts the MWOE which connects the node to a node from another fragment.
     * Each node waits for the MWOE suggestions from all of its children to be received, and then chooses the minimal from those and from its MWOE and forwards it to its parent.
     * In addition, each node sends the number of nodes in the subtree which it is the root of. Each node sums the values of its children and adds 1, and this is the value it forwards its parent.
     * The root of every fragment will eventually get the number of nodes in its fragment, and if it is n the algorithm will be finished by broadcasting start server rerouting messages.
     *
     * @param inbox The inbox of the node
     */
    public void mwoeSearchingIter(Inbox inbox) {
        if (children.isEmpty()) {
            if (roundCounter == 0) {
                GHSNode mwoeNode = getMinimumWeightEdge();
                // If mwoeNode == null, getWeightOfEdgeTo(mwoeNode) == null and it is fine
                // We must do it because the parent must receive one message from every child
                send(new MWOESuggestionMessage(this, mwoeNode, getWeightOfEdgeTo(mwoeNode), 1), parent);
            }
            while (inbox.hasNext()) {
                Message msg = inbox.next();
                if (msg instanceof StartServerReroutingMessage) {
                    startServerRerouting((StartServerReroutingMessage) msg);
                }
            }
        } else {
            while (inbox.hasNext()) {
                Message msg = inbox.next();
                if (msg instanceof MWOESuggestionMessage) {
                    mwoeQueue.add((MWOESuggestionMessage) msg);
                }
                // Listen for a start server rerouting message
                else if (msg instanceof StartServerReroutingMessage) {
                    for (GHSNode child : children) {
                        send(msg, child);
                    }
                    startServerRerouting((StartServerReroutingMessage) msg);
                }
            }
            if (mwoeQueue.size() == children.size()) {
                GHSNode mwoeNode = getMinimumWeightEdge();
                Integer min_weight = getWeightOfEdgeTo(mwoeNode);
                MWOESuggestionMessage mwoeSuggestionToSend = new MWOESuggestionMessage(this, mwoeNode, min_weight);
                // Initialized to 1 to include the current node in the count
                int nodesInSubtreeCounter = 1;
                if (min_weight == null) min_weight = WeightedEdge.MAX_WEIGHT;
                for (MWOESuggestionMessage mwoeMsg : mwoeQueue) {
                    nodesInSubtreeCounter += mwoeMsg.getNumOfNodesInSubtree();
                    Integer currWeight = mwoeMsg.getWeight();
                    if (currWeight == null) continue;
                    if (currWeight < min_weight) {
                        min_weight = currWeight;
                        mwoeSuggestionToSend = mwoeMsg;
                    }
                }
                mwoeSuggestionToSend.setNumOfNodesInSubtree(nodesInSubtreeCounter);
                if (isRoot) {
                    // If all the nodes are in the subtree of the current root, the algorithm is finished
                    if (nodesInSubtreeCounter == CustomGlobal.getNumOfNodes()) {
                        StartServerReroutingMessage msg = new StartServerReroutingMessage((int) Tools.getGlobalTime());
                        for (GHSNode child : children) {
                            send(msg, child);
                        }
                        startServerRerouting(msg);
                    }
                    mwoeToAdd = mwoeSuggestionToSend;
                } else {
                    send(mwoeSuggestionToSend, parent);
                }
                mwoeQueue.clear();
            }
        }
    }

    /**
     * An iteration of the MWOE_BROADCASTING state.
     * In this state, the root broadcasts the chosen MWOE that goes out of the current fragment.
     * Each node will receive the message, and if it is its MWOE it will update its parentCandidate member.
     *
     * @param inbox The inbox of the node
     */
    public void mwoeBroadcastingIter(Inbox inbox) {
        if (isRoot) {
            if (roundCounter == 0) {
                if (mwoeToAdd == null) return;
                // If this is the current node's MWOE, update parentCandidate
                if (mwoeToAdd.getFrom().equals(this)) {
                    parentCandidate = mwoeToAdd.getTo();
                } else if (mwoeToAdd.getTo().equals(this)) {
                    parentCandidate = mwoeToAdd.getFrom();
                }
                // Otherwise, update the children about the MWOE
                else {
                    for (GHSNode child : children) {
                        send(new ChosenMWOEMessage(mwoeToAdd), child);
                    }
                }
                mwoeToAdd = null;
            }
        } else {
            while (inbox.hasNext()) {
                Message msg = inbox.next();
                if (msg instanceof ChosenMWOEMessage) {
                    ChosenMWOEMessage chosenMWOEMsg = (ChosenMWOEMessage) msg;
                    // If this is the current node's MWOE, update parentCandidate
                    if (chosenMWOEMsg.getFrom().equals(this)) {
                        parentCandidate = chosenMWOEMsg.getTo();
                    } else if (chosenMWOEMsg.getTo().equals(this)) {
                        parentCandidate = chosenMWOEMsg.getFrom();
                    }
                    // Otherwise, update the children about the MWOE
                    else {
                        for (GHSNode child : children) {
                            send(chosenMWOEMsg, child);
                        }
                    }
                }
                // Listen for a start server rerouting message
                else if (msg instanceof StartServerReroutingMessage) {
                    for (GHSNode child : children) {
                        send(msg, child);
                    }
                    startServerRerouting((StartServerReroutingMessage) msg);
                }
            }
        }
    }

    /**
     * An iteration of the NEW_ROOT_BROADCASTING state.
     * In this state, the node which the fragment's MWOE is connected to will become the new root of the fragment.
     * It will send a FlipEdgeDirectionMessage to its parent, and each node that receives this message will change its parent to be the sender and forward it to its old parent.
     * This will continue all the way up to the old root of the fragment.
     *
     * @param inbox The inbox of the node
     */
    public void newRootBroadcastingIter(Inbox inbox) {
        if (roundCounter == 0 && parentCandidate != null) {
            if (!isRoot) {
                children.add(parent);
                send(new FlipEdgeDirectionMessage(), parent);
                parent = null;
                isRoot = true;
            }
        }
        while (inbox.hasNext()) {
            Message msg = inbox.next();
            if (msg instanceof FlipEdgeDirectionMessage) {
                if (!isRoot) {
                    children.add(parent);
                    send(msg, parent);
                } else {
                    isRoot = false;
                }
                parent = (GHSNode) inbox.getSender();
                children.remove(parent);
            }
            // Listen for a start server rerouting message
            else if (msg instanceof StartServerReroutingMessage) {
                for (GHSNode child : children) {
                    send(msg, child);
                }
                startServerRerouting((StartServerReroutingMessage) msg);
            }
        }
    }

    /**
     * An iteration of the SERVER_REROUTING state.
     * In this state, the edges on the route from the root to the server will be flipped in order to make the server the new root of the MST.
     *
     * @param inbox The inbox of the node
     */
    public void serverReroutingIter(Inbox inbox) {
        if (isServer && !isRoot) {
            children.add(parent);
            send(new FlipEdgeDirectionMessage(), parent);
            parent = null;
            isRoot = true;
        }
        while (inbox.hasNext()) {
            Message msg = inbox.next();
            if (msg instanceof FlipEdgeDirectionMessage) {
                if (!isRoot) {
                    children.add(parent);
                    send(msg, parent);
                } else {
                    isRoot = false;
                }
                parent = (GHSNode) inbox.getSender();
                children.remove(parent);
            }
            // Listen for a start server rerouting message
            else if (msg instanceof StartServerReroutingMessage) {
                for (GHSNode child : children) {
                    send(msg, child);
                }
                startServerRerouting((StartServerReroutingMessage) msg);
            }
        }
    }

    /* Sinalgo hooks */

    /**
     * The logic of the node which will be executed in every round.
     * This function initializes the node if it is not initialized, calls the function of the relevant state and switches the state.
     *
     * @param inbox The inbox of the node
     */
    @Override
    public void handleMessages(Inbox inbox) {
        if (currentState == GHSStates.NOT_STARTED) {
            startGHS();
        }
        switch (currentState) {
            case MWOE_SEND:
                mwoeSendIter(inbox);
                break;
            case LEADER_DISCOVERY:
                leaderDiscoveryIter(inbox);
                break;
            case FRAGMENT_ID_DISCOVERY:
                fragmentIDDiscoveryIter(inbox);
                break;
            case MWOE_SEARCHING:
                mwoeSearchingIter(inbox);
                break;
            case MWOE_BROADCASTING:
                mwoeBroadcastingIter(inbox);
                break;
            case NEW_ROOT_BROADCASTING:
                newRootBroadcastingIter(inbox);
                break;
            case SERVER_REROUTING:
                serverReroutingIter(inbox);
                break;
        }
        switchState();
    }

    @Override
    public void init() {
    }

    @Override
    public void neighborhoodChange() {
    }

    @Override
    public void preStep() {
    }

    @Override
    public void postStep() {
    }

    @Override
    public void checkRequirements() throws WrongConfigurationException {
    }

    /* End of Sinalgo hooks */

    /* Sinalgo menu buttons */

    /**
     * Print all the neighbors of the current node.
     */
    @NodePopupMethod(menuText = "Print Neighbors")
    public void printNeighbors() {
        System.out.println("Neighbors of " + ID + ":");
        for (Edge e : outgoingConnections) {
            System.out.println("Edge from " + e.startNode.ID + " to " + e.endNode.ID);
        }
    }

    /**
     * Print all the children of the current node.
     */
    @NodePopupMethod(menuText = "Print Children")
    public void printChildren() {
        System.out.println("Children of " + ID + ":");
        for (GHSNode child : children) {
            System.out.println(child.ID);
        }
    }

    /**
     * Print the parent of the current node.
     */
    @NodePopupMethod(menuText = "Print Parent")
    public void printParent() {
        if (parent == null) {
            System.out.println("Node " + ID + " doesn't have a parent (it is the root).");
        } else {
            System.out.println("Parent of " + ID + " is " + parent.ID);
        }
    }

    /* End of Sinalgo menu buttons */
}
