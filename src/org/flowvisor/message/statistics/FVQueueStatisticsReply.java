package org.flowvisor.message.statistics;

import java.util.Iterator;
import java.util.List;

import org.flowvisor.classifier.FVClassifier;

import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.SliceAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.statistics.OFQueueStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;

public class FVQueueStatisticsReply extends OFQueueStatisticsReply implements
		ClassifiableStatistic, SlicableStatistic {
	final static Logger logger = LoggerFactory.getLogger(FVQueueStatisticsReply.class);
		
    @Override
    public void sliceFromController(FVStatisticsRequest msg, FVClassifier fvClassifier,
                    FVSlicer fvSlicer) {
            logger.warn("{} dropping unexpected msg: {}", fvSlicer.getName(), msg.getClass().getName());
    }

    /**
     * No need to rewrite response
     */

    @Override
    public void classifyFromSwitch(FVStatisticsReply msg, FVClassifier fvClassifier) {
    	FVSlicer fvSlicer = FVMessageUtil.untranslateXid(msg, fvClassifier);
    	if (fvSlicer == null) {
    		logger.warn("{} dropping unclassifiable port stats reply: ",fvClassifier.getName(), this.getClass().getName());
    		return;
    	}

    	Iterator<OFStatistics> it = msg.getStatistics().iterator();
    	while (it.hasNext()) {
    		FVQueueStatisticsReply reply = (FVQueueStatisticsReply) it.next();
    		if (!fvSlicer.portInSlice(reply.portNumber)) {
    			logger.warn("{} Port {} is not in slice {}",fvClassifier.getName(), reply.portNumber, fvSlicer.getSliceName());
    			it.remove();
    			msg.setLengthU(msg.getLengthU() - reply.computeLength());
    			continue;
    		}
    		FVMatch testMatch = new FVMatch();
    		testMatch.setInputPort(reply.portNumber);
    		testMatch.setWildcards(testMatch.getWildcards() & ~FVMatch.OFPFW_IN_PORT);
    		List<FlowEntry> matches = 
    				fvSlicer.getFlowSpace().matches(fvClassifier.getDPID(), testMatch);
    		logger.debug("matches {}",matches);
    		
    		boolean found = false;
    		for (FlowEntry fe : matches) {
    		
    			if (fe.getQueueId().contains(reply.queueId)) {
    				for (OFAction act : fe.getActionsList()) {
    					assert(act instanceof SliceAction);
    					SliceAction sa = (SliceAction) act;
    					if (sa.getSliceName().equals(fvSlicer.getSliceName())) {
    						found = true;
    						break;
    					}
    				}
    				if (found)
    					break;
    			} 
    		}
    		if (!found) {
    			it.remove();
    			msg.setLengthU(msg.getLengthU() - reply.computeLength());
    			logger.warn("{} QueueId {} is not associated to port {} in slice {}",fvClassifier.getName(), reply.queueId,
    					reply.getPortNumber(), fvSlicer.getSliceName());
    		}
    	}
    	if (msg.getStatistics().size() > 0) {
    		fvSlicer.sendMsg(msg, fvClassifier);
    	} else {
    		logger.warn("{}, Dropping emptied Queue stats reply: {}", fvClassifier.getName(), msg.getClass().getName());
    	}

    }


}
