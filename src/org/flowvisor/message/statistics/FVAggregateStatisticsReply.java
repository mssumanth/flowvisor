package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFAggregateStatisticsReply;

public class FVAggregateStatisticsReply extends OFAggregateStatisticsReply
		implements SlicableStatistic, ClassifiableStatistic {
	
	final static Logger logger = LoggerFactory.getLogger(FVAggregateStatisticsReply.class);



	@Override
	public void classifyFromSwitch(FVStatisticsReply msg, FVClassifier fvClassifier) {
		logger.warn("{} dropping unexpected msg: {}",fvClassifier.getName(), msg);
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		logger.warn("{} dropping unexpected msg: {}", fvClassifier.getName(), msg);
	}
}
