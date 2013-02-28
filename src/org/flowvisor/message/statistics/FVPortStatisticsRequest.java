package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFPortStatisticsRequest;

public class FVPortStatisticsRequest extends OFPortStatisticsRequest implements
		ClassifiableStatistic, SlicableStatistic {
	
	final static Logger logger = LoggerFactory.getLogger(FVPortStatisticsRequest.class);


	@Override
	public void sliceFromController(FVStatisticsRequest msg,
			FVClassifier fvClassifier, FVSlicer fvSlicer) {
		//TODO: implement port stats
		FVMessageUtil.translateXidAndSend(msg, fvClassifier, fvSlicer);
	}

	@Override
	public void classifyFromSwitch(FVStatisticsReply msg,
			FVClassifier fvClassifier) {
		logger.warn("{} dropping unexpected msg: {}", fvClassifier.getName(), this.getClass().getName());
	}

}
