package org.flowvisor.message.actions;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.ActionDisallowedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionTransportLayerDestination;

public class FVActionTransportLayerDestination extends
		OFActionTransportLayerDestination implements SlicableAction {

	final static Logger logger = LoggerFactory.getLogger(FVActionTransportLayerDestination.class);
	
	@Override
	public void slice(List<OFAction> approvedActions, OFMatch match,
			FVClassifier fvClassifier, FVSlicer fvSlicer)
			throws ActionDisallowedException {
		logger.error(fvSlicer.getName(),
				"action slicing unimplemented for type: " + this);
		approvedActions.add(this);
	}

}
