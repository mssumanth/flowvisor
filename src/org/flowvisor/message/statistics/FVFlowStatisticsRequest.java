package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFFlowStatisticsRequest;

public final class FVFlowStatisticsRequest extends OFFlowStatisticsRequest
		implements SlicableStatistic, ClassifiableStatistic {
	
	final static Logger logger = LoggerFactory.getLogger(FVFlowStatisticsRequest.class);

	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		// TODO: rewrite/sanity check this request against the flowspace
		logger.warn("{} need to implement flowstats request slicing", fvSlicer.getName());
		FVMessageUtil.translateXidAndSend(msg, fvClassifier, fvSlicer);
	}

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		logger.warn("{} dropping unexpected msg: {}", fvClassifier.getName(), msg);
	}

}
