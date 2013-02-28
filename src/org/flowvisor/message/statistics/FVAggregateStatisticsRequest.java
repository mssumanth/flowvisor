package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;

public class FVAggregateStatisticsRequest extends
		org.openflow.protocol.statistics.OFAggregateStatisticsRequest implements
		SlicableStatistic, ClassifiableStatistic {
	
	final static Logger logger = LoggerFactory.getLogger(FVAggregateStatisticsRequest.class);

	@Override
	public void classifyFromSwitch(FVStatisticsReply msg, FVClassifier fvClassifier) {
		logger.warn("{} dropping unexpected msg: {}", fvClassifier.getName(), msg);
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVMessageUtil.translateXidMsg(msg,fvClassifier, fvSlicer);
		if (!fvClassifier.pollFlowTableStats(msg))
			fvClassifier.sendAggStatsResp(fvSlicer, msg);
	}
}
