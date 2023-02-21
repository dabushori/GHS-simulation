package projects.mmn15.nodes.nodeImplementations;

import sinalgo.configuration.WrongConfigurationException;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;

/**
 * A node that implements the GHS algorithm.
 */
public class GHSNode extends Node {

	@Override
	public void handleMessages(Inbox inbox) {
		while(inbox.hasNext()) {
			Message msg = inbox.next();
			// Do stuff
		}
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
	public void checkRequirements() throws WrongConfigurationException {}
}
