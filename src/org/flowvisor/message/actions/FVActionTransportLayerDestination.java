package org.flowvisor.message.actions;

import java.util.Iterator;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.SliceAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFError.OFBadActionCode;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionTransportLayerDestination;

public class FVActionTransportLayerDestination extends
		OFActionTransportLayerDestination implements SlicableAction {

	final static Logger logger = LoggerFactory.getLogger(FVActionTransportLayerDestination.class);
	
	@Override
	public void slice(List<OFAction> approvedActions, OFMatch match,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws ActionDisallowedException {
		FVMatch neoMatch = new FVMatch(match);
		neoMatch.setTransportDestination(this.transportPort);
		List<FlowEntry> flowEntries = fvClassifier.getSwitchFlowMap().matches(fvClassifier.getDPID(), neoMatch);
		for (FlowEntry fe : flowEntries) {
			Iterator<OFAction> it = fe.getActionsList().iterator();
			while (it.hasNext()) {
				OFAction act = it.next();
				if (act instanceof SliceAction) {
					SliceAction action = (SliceAction) act;
					if (action.getSliceName().equals(fvSlicer.getSliceName())) {
						logger.debug("{} Approving {} for {}",fvSlicer.getName(), this.getClass().getName(), match.getClass().getName());
						approvedActions.add(this);
						return;
					}
				}
			}
		}
		throw new ActionDisallowedException(
				"Slice " + fvSlicer.getSliceName() + " may not rewrite transport " +
				"dst port to " + this.transportPort, 
				OFBadActionCode.OFPBAC_BAD_ARGUMENT);
	}

}
