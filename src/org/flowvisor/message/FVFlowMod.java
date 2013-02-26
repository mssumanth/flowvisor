package org.flowvisor.message;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.exceptions.ActionDisallowedException;
import org.flowvisor.flows.FlowIntersect;
import org.flowvisor.flows.SliceAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFError.OFBadRequestCode;
import org.openflow.protocol.OFError.OFFlowModFailedCode;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.action.OFAction;

public class FVFlowMod extends org.openflow.protocol.OFFlowMod implements
		Classifiable, Slicable, Cloneable {
	
	final static Logger logger = LoggerFactory.getLogger(FVFlowMod.class);

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	/**
	 * FlowMod slicing
	 *
	 * 1) make sure all actions are ok
	 *
	 * 2) expand this FlowMod to the intersection of things in the given match
	 * and the slice's flowspace
	 */

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		logger.debug("{} recv from controller: {}",fvSlicer.getName(), this.getClass());
		FVMessageUtil.translateXid(this, fvClassifier, fvSlicer);

		// make sure that this slice can access this bufferID
		if ((this.command != OFFlowMod.OFPFC_DELETE 
				&& this.command != OFFlowMod.OFPFC_DELETE_STRICT) 
				&& !fvSlicer.isBufferIDAllowed(this.getBufferId())) {
			logger.warn("{} EPERM - This buffer_id is disallowed: {}", fvSlicer.getName(), this.getClass());
			fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(
						OFBadRequestCode.OFPBRC_BUFFER_UNKNOWN, this), fvSlicer);
			return;
		}
		
		// make sure the list of actions is kosher
		List<OFAction> actionsList = this.getActions();
		try {
			actionsList = FVMessageUtil.approveActions(actionsList, this.match,
					fvClassifier, fvSlicer);
		} catch (ActionDisallowedException e) {
			
			logger.warn("{} EPERM bad actions: {}", fvSlicer.getName(), this.getClass());
			fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(
					e.getError(), this), fvSlicer);
			return;
		}
		// expand this match to everything that intersects the flowspace
		List<FlowIntersect> intersections = fvSlicer.getFlowSpace().intersects(
				fvClassifier.getDPID(), new FVMatch(getMatch()));

		int expansions = 0;
		OFFlowMod original = null;
		try {
			original = this.clone(); // keep an unmodified copy
		} catch (CloneNotSupportedException e) {
			// will never happen because clone in FVFlowMod
			// doesn't throw a CloneNotSupportedException
			// but clone()'s spec does.
			e.printStackTrace();
		}
		
		int oldALen = FVMessageUtil.countActionsLen(this.getActions());
		this.setActions(actionsList);
		// set new length as a function of old length and old actions length
		this.setLength((short) (getLength() - oldALen + FVMessageUtil
				.countActionsLen(actionsList)));

		for (FlowIntersect intersect : intersections) {
			try {
				if (intersect.getFlowEntry().hasPermissions(
						fvSlicer.getSliceName(), SliceAction.WRITE)) {
					expansions++;
					FVFlowMod newFlowMod = (FVFlowMod) this.clone();
					// replace match with the intersection
					newFlowMod.setMatch(intersect.getMatch());
					// update flowDBs
					fvSlicer.getFlowRewriteDB().processFlowMods(original,
							newFlowMod);
					fvClassifier.getFlowDB().processFlowMod(newFlowMod,
							fvClassifier.getDPID(), fvSlicer.getSliceName());
					// actually send msg
					/*if (fvClassifier.isFlowTracking() && !((this.flags & 1) != 0))
						this.flags = (short) (this.flags & 1);*/
					fvClassifier.sendMsg(newFlowMod, fvSlicer);
				}
			} catch (CloneNotSupportedException e) {
				// will never happen because clone in FVFlowMod
				// doesn't throw a CloneNotSupportedException
				// but clone()'s spec does.
				e.printStackTrace();
			}
		}
		

		if (expansions == 0) {
			logger.warn("{} dropping illegal fm: {}",fvSlicer.getName(), this.getClass());
			fvSlicer.sendMsg(FVMessageUtil.makeErrorMsg(
					OFFlowModFailedCode.OFPFMFC_EPERM, this), fvSlicer);
		} else
			logger.debug("{} expanded fm {} times: {}",fvSlicer.getName(), expansions, this.getClass());
	}
	

	public FVFlowMod setMatch(FVMatch match) {
		this.match = match;
		return this;
	}
	
	@Override
	public OFMatch getMatch() {
		return this.match;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return super.toString() + ";actions="
				+ FVMessageUtil.actionsToString(this.getActions());
	}
}
