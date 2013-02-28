package org.flowvisor.message;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFQueueConfigReply;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.queue.OFPacketQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FVQueueConfigReply extends OFQueueConfigReply implements
		Classifiable, Slicable  {
	final static Logger logger = LoggerFactory.getLogger(FVQueueConfigReply.class);

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvClassifier);
	}

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(this, fvClassifier);
		if (fvSlicer == null) {
			logger.warn("{} dropping unclassifiable xid in QueueConfigReply: {}", fvClassifier.getName(), this.getClass().getName());
			return;
		}
		FVMatch match = new FVMatch();
		match.setInputPort(this.port);
		logger.info("{} matching FS", fvSlicer.getName());
		List<FlowEntry> entries = fvSlicer.getFlowSpace().matches(fvClassifier.getDPID(), match);
		Iterator<FlowEntry> it = entries.iterator();
		while (it.hasNext()) {
			logger.info("{} pruning FS", fvSlicer.getName());
			FlowEntry fe = it.next();
			for (OFAction act : fe.getActionsList()) {
				SliceAction sa = (SliceAction) act;
				if (!sa.getSliceName().equals(fvSlicer.getSliceName()))
					it.remove();
			}
		}
		boolean found = false;
		List<Integer> queuelog = new LinkedList<Integer>();
		Iterator<OFPacketQueue> qit = this.queues.iterator();
		while (qit.hasNext()) {
			logger.info("{} matching queue reply", fvSlicer.getName());
			OFPacketQueue queue = qit.next();
			queuelog.add(queue.getQueueId());
			for (FlowEntry fe : entries) {
				if (fe.getQueueId().contains(queue.getQueueId())) {
					found = true;
					break;
				} 
			}
			if (!found) {
				logger.info("{} Pruning queue {}  because it is not in slice {}", fvClassifier.getName(), queue.getQueueId() 
						, fvSlicer.getSliceName());
				qit.remove();
				this.setLengthU(this.getLengthU() - queue.computeLength());
			}
			
		}
		if (!found) {
			logger.warn("{} dropping QueueConfigReply because queues {}  are not in slice: {} : {}", fvClassifier.getName(), queuelog ,
					fvSlicer.getSliceName(), this.getClass().getName());
			return;
		}
		if (fvSlicer.portInSlice(this.port))
			fvSlicer.sendMsg(this, fvClassifier);
		else 
			logger.warn("{} dropping QueueConfigReply because port is not in slice: {} : {}", fvClassifier.getName(), fvSlicer.getSliceName(), 
					this.getClass().getName());
	}

}