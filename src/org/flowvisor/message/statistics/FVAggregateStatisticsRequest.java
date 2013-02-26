package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;

public class FVAggregateStatisticsRequest extends
		org.openflow.protocol.statistics.OFAggregateStatisticsRequest implements
		SlicableStatistic, ClassifiableStatistic {
	
	final static Logger logger = LoggerFactory.getLogger(FVAggregateStatisticsRequest.class);

	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVMessageUtil.translateXidAndSend(msg, fvClassifier, fvSlicer);
	}

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		logger.warn("{} dropping unexpected msg: {}", fvClassifier.getName(), msg);
	}

}
