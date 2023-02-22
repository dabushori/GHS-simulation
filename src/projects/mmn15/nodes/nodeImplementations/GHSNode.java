package projects.mmn15.nodes.nodeImplementations;

import projects.mmn15.CustomGlobal;
import projects.mmn15.nodes.edges.WeightedEdge;
import projects.mmn15.nodes.messages.*;
import sinalgo.configuration.WrongConfigurationException;
import sinalgo.nodes.Node;
import sinalgo.nodes.edges.Edge;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;

import java.util.HashMap;
import java.util.Vector;

/**
 * A node that implements the GHS algorithm.
 */
public class GHSNode extends Node {
    // MST (and fragment) management members
    public int fragmentID;
    public HashMap<GHSNode, Integer> nodesToFragmentID = new HashMap<>();
    public HashMap<GHSNode, WeightedEdge> neighborsToEdges = new HashMap<>();
    public Vector<GHSNode> children = new Vector<>();
    public GHSNode parent;
    MWOESuggestionMessage mwoeToAdd;

    public Vector<MWOESuggestionMessage> mwoeQueue = new Vector<>();
    public GHSNode parentCandidate;

    boolean isRoot;
    boolean isServer;

    public Integer getWeightOfEdgeTo(GHSNode neighbor) {
        if (neighbor == null) return null;
        return neighborsToEdges.get(neighbor).getWeight();
    }

    public GHSNode getMinimumWeightEdge() {
        int minWeight = WeightedEdge.MAX_WEIGHT + 1; // max value
        GHSNode minimumWeightNeighbor = null;
        for (Edge e: outgoingConnections) {

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

    enum GHSStates {
        NOT_STARTED, // MWOE == Minimum Weight Outgoing Edge
        MWOE_SEND,
        LEADER_DISCOVERY,
        FRAGMENT_ID_DISCOVERY,
        MWOE_SEARCHING,
        MWOE_BROADCASTING,
        NEW_ROOT_BROADCASTING,
        FINISHED,
    }

    public boolean hasFinished() {
        return currentState == GHSStates.FINISHED;
    }

    int iterationCounter = 0;
    int counter = 0;
    public GHSStates currentState = GHSStates.NOT_STARTED;
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
                counter = 0;
                break;
            case FRAGMENT_ID_DISCOVERY:
                if (++counter == CustomGlobal.getNumOfNodes()) {
                    currentState = GHSStates.MWOE_SEARCHING;
                    counter = 0;
                }
                break;
            case MWOE_SEARCHING:
                if (++counter == CustomGlobal.getNumOfNodes()) {
                    currentState = GHSStates.MWOE_BROADCASTING;
                    counter = 0;
                }
                break;
            case MWOE_BROADCASTING:
                if (++counter == CustomGlobal.getNumOfNodes()) {
                    currentState = GHSStates.NEW_ROOT_BROADCASTING;
                    counter = 0;
                }
                break;
            case NEW_ROOT_BROADCASTING:
                if (++counter == CustomGlobal.getNumOfNodes()) {
                    // Stupid java doesn't have a Math.log2 function
                    if (++iterationCounter == Math.ceil(Math.log(CustomGlobal.getNumOfNodes()) / Math.log(2))) {
                        iterationCounter = 0;
                        currentState = GHSStates.FINISHED;
                    } else {
                        counter = 0;
                        currentState = GHSStates.MWOE_SEND;
                    }
                }
                break;
            case FINISHED:
                break;
        }
    }

    @Override
    public void handleMessages(Inbox inbox) {
        if (!initialized) {
            startGHS();
        }
        GHSNode mwoeNode;
        switch (currentState) {
            case MWOE_SEND:
                // If the current node is the node with the lightest edge that goes out of the fragment, add this edge to the MST and connect the fragments.
                if (isRoot) {
                    send(new MWOEChoiceMessage(getWeightOfEdgeTo(parentCandidate)), parentCandidate);
                }
                break;
            case LEADER_DISCOVERY:
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
                                // If the current node has the maximal ID mark it as a root
                            } else {
                                children.add(node);
                                parentCandidate = null;
                                isRoot = true;
                            }
                            // If a node which is not the parentCandidate chooses you as the other side of its MWOE (you are its parentCandidate) add it as your child
                        } else {
                            children.add(node);
                        }
                    } else if (msg instanceof FinishMessage) {
                        for (GHSNode child : children) {
                            send(msg, child);
                        }
                        currentState = GHSStates.FINISHED;
                    }
                }
                // If the parentCandidate didn't choose you (very sad) add the chosen MWOE to the fragment and mark it as a parent
                if (parentCandidate != null) {
                    parent = parentCandidate;
                    parentCandidate = null;
                    isRoot = false;
                }
                break;
            case FRAGMENT_ID_DISCOVERY:
                // If the current node is the root of the fragment, send the node's id to all of its children only if it is the first iteration of this state
                if (isRoot) {
                    // If it is the first iteration of this state send the node's id (which is the new fragment id) to its children
                    if (counter == 0) {
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
                        } else if (msg instanceof FinishMessage) {
                            for (GHSNode child : children) {
                                send(msg, child);
                            }
                            currentState = GHSStates.FINISHED;
                        }
                    }
                    // If the current node is not the root of the fragment, update the fragment id according to the received id and pass it to the node's children
                } else {
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
                            // Listen for updates of the neighbors about their fragment ids
                        } else if (msg instanceof FragmentIDMessage) {
                            nodesToFragmentID.put((GHSNode) inbox.getSender(), ((FragmentIDMessage) msg).getId());
                        } else if (msg instanceof FinishMessage) {
                            for (GHSNode child : children) {
                                send(msg, child);
                            }
                            currentState = GHSStates.FINISHED;
                        }
                    }
                }
                break;
            case MWOE_SEARCHING:
                if (children.isEmpty()) {
                    if (counter == 0) {
                        mwoeNode = getMinimumWeightEdge();
                        // If mwoeNode == null, getWeightOfEdgeTo(mwoeNode) == null and it is fine
                        // We must do it because the parent must receive one message from every child
                        send(new MWOESuggestionMessage(this, mwoeNode, getWeightOfEdgeTo(mwoeNode), 1), parent);
                    }
                } else {
                    while (inbox.hasNext()) {
                        Message msg = inbox.next();
                        if (msg instanceof MWOESuggestionMessage) {
                            mwoeQueue.add((MWOESuggestionMessage) msg);
                        } else if (msg instanceof FinishMessage) {
                            for (GHSNode child : children) {
                                send(msg, child);
                            }
                            currentState = GHSStates.FINISHED;
                        }
                    }
                    if (mwoeQueue.size() == children.size()) {
                        mwoeNode = getMinimumWeightEdge();
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
                                for (GHSNode child : children) {
                                    send(new FinishMessage(), child);
                                }
                                currentState = GHSStates.FINISHED;
                            }
                            mwoeToAdd = mwoeSuggestionToSend;
                        } else {
                            send(mwoeSuggestionToSend, parent);
                        }
                        mwoeQueue.clear();
                    }
                }
                break;
            case MWOE_BROADCASTING:
                if (isRoot) {
                    if (counter == 0) {
                        if (mwoeToAdd == null) break;
                        if (mwoeToAdd.getFrom().equals(this)) {
                            parentCandidate = mwoeToAdd.getTo();
                        } else if (mwoeToAdd.getTo().equals(this)) {
                            parentCandidate = mwoeToAdd.getFrom();
                        } else {
                            for (GHSNode child : children) {
                                ChosenMWOEMessage msgToSend = new ChosenMWOEMessage(mwoeToAdd);
                                send(msgToSend, child);
//                                send(new ChosenMWOEMessage(mwoeToAdd.getFrom(), mwoeToAdd.getTo(), mwoeToAdd.getWeight()), child);
                            }
                        }
                        mwoeToAdd = null;
                    }
                } else {
                    while (inbox.hasNext()) {
                        Message msg = inbox.next();
                        if (msg instanceof ChosenMWOEMessage) {
                            ChosenMWOEMessage chosenMWOEMsg = (ChosenMWOEMessage) msg;
                            if (chosenMWOEMsg.getFrom().equals(this)) {
                                parentCandidate = chosenMWOEMsg.getTo();
                            } else if (chosenMWOEMsg.getTo().equals(this)) {
                                parentCandidate = chosenMWOEMsg.getFrom();
                            } else {
                                for (GHSNode child : children) {
                                    send(chosenMWOEMsg, child);
                                }
                            }
                        } else if (msg instanceof FinishMessage) {
                            for (GHSNode child : children) {
                                send(msg, child);
                            }
                            currentState = GHSStates.FINISHED;
                        }
                    }
                }
                break;
            case NEW_ROOT_BROADCASTING:
                if (counter == 0 && parentCandidate != null) {
                    if (!isRoot) {
                        children.add(parent);
                        send(new FlipEdgeDirectionMessage(), parent);
                    }
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
                    } else if (msg instanceof FinishMessage) {
                        for (GHSNode child : children) {
                            send(msg, child);
                        }
                        currentState = GHSStates.FINISHED;
                    }
                }
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

    public boolean initialized = false;
    public void startGHS() {
        // Reset all the records
        nodesToFragmentID.clear();
        neighborsToEdges.clear();

        mwoeQueue.clear();
        children.clear();
        parent = null;
        mwoeToAdd = null;

        counter = 0;
        iterationCounter = 0;

        // Each node is its fragment root in the first iteration, so each node will connect its fragment to another fragment using the MWOE he finds
        isRoot = true;
        // isServer = false;

        fragmentID = ID;
        for (Edge e : outgoingConnections) {
            neighborsToEdges.put((GHSNode) e.endNode, (WeightedEdge) e);
            nodesToFragmentID.put((GHSNode) e.endNode, e.endNode.ID);
        }

        parentCandidate = getMinimumWeightEdge();
        currentState = GHSStates.MWOE_SEND;

        initialized = true;
    }

    @NodePopupMethod(menuText = "Print Neighbors")
    public void printNeighbors() {
        System.out.println("Neighbors of " + ID + ":");
        for (Edge e : outgoingConnections) {
            System.out.println("Edge from " + e.startNode.ID + " to " + e.endNode.ID);
        }
    }

    @NodePopupMethod(menuText = "Print Children")
    public void printChildren() {
        System.out.println("Children of " + ID + ":");
        for (GHSNode child : children) {
            System.out.println(child.ID);
        }
    }

    @NodePopupMethod(menuText = "Print Parent")
    public void printParent() {
        if (parent == null) {
            System.out.println("Node " + ID + " doesn't have a parent (it is the root). isRoot = " + isRoot);
        } else {
            System.out.println("Parent of " + ID + " is " + parent.ID);
        }
    }
}
