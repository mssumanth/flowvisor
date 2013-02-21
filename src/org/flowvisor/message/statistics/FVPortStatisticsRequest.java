package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFPortStatisticsRequest;

public class FVPortStatisticsRequest extends OFPortStatisticsRequest implements
		ClassifiableStatistic, SlicableStatistic {
	
	final static Logger logger = LoggerFactory.getLogger(FVPortStatisticsRequest.class);

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		logger.warn(fvClassifier.getName(), "dropping unexpected msg: "
				+ msg);
	}

	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		FVMessageUtil.translateXidAndSend(msg, fvClassifier, fvSlicer);
	}

}
