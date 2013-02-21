package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFAggregateStatisticsReply;

public class FVAggregateStatisticsReply extends OFAggregateStatisticsReply
		implements SlicableStatistic, ClassifiableStatistic {
	
	final static Logger logger = LoggerFactory.getLogger(FVAggregateStatisticsReply.class);

	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		logger.warn(fvClassifier.getName(), "dropping unexpected msg: "
				+ msg);
	}

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		FVMessageUtil.untranslateXidAndSend(msg, fvClassifier);
	}
}
