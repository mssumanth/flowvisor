package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFTableStatistics;

public class FVTableStatistics extends OFTableStatistics implements
		SlicableStatistic, ClassifiableStatistic {

	final static Logger logger = LoggerFactory.getLogger(FVTableStatistics.class);
	@Override
	public void classifyFromSwitch(FVStatisticsReply msg,
			FVClassifier fvClassifier) {
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(msg, fvClassifier);
		if (fvSlicer ==  null) {
			logger.warn("{} Dropping unclassifiable message: {}", fvClassifier.getName(), msg.getClass().getName());
			return;
		}
		int currentMax = fvClassifier.getMaxAllowedFlowMods(fvSlicer.getSliceName());
		int currentFMs = fvClassifier.getCurrentFlowModCounter(fvSlicer.getSliceName());
		if (currentMax != -1)
			this.setMaximumEntries(currentMax);
		this.setActiveCount(currentFMs);
		fvSlicer.sendMsg(msg, fvClassifier);
		
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg,
			FVClassifier fvClassifier, FVSlicer fvSlicer) {
		logger.warn("{} dropping unexpected msg: {}", fvClassifier.getName(), msg.getClass().getName());
	}

}
