package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;

public final class FVFlowStatisticsRequest extends OFFlowStatisticsRequest
		implements SlicableStatistic, ClassifiableStatistic {
	
	final static Logger logger = LoggerFactory.getLogger(FVFlowStatisticsRequest.class);

	@Override
	public void classifyFromSwitch(FVStatisticsReply msg, FVClassifier fvClassifier) {
		logger.warn("{} dropping unexpected msg: {}", fvClassifier.getName(), this.getClass().getName());
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVMessageUtil.translateXidMsg(msg,fvClassifier, fvSlicer);
		if (!fvClassifier.pollFlowTableStats(msg))
			fvClassifier.sendFlowStatsResp(fvSlicer, msg);
		
	}
	
	
	
	
}
