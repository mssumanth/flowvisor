package org.flowvisor.message.statistics;

import org.flowvisor.classifier.FVClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.statistics.OFFlowStatisticsReply;

public class FVFlowStatisticsReply extends OFFlowStatisticsReply implements
		SlicableStatistic, ClassifiableStatistic {
	
	final static Logger logger = LoggerFactory.getLogger(FVFlowStatisticsReply.class);

	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		logger.warn(fvSlicer.getName(), "dropping unexpected msg: " + msg);
	}

	/**
	 * Rewrite flowstats replies to include only the stats that are in this
	 * slice's flowspace
	 */

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		// TODO : implement slicing here; remove irrelevant flows
		// TODO: serve this from cache?
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(msg, fvClassifier);
		if (fvSlicer == null)
			logger.warn(fvClassifier.getName(),
					"dropping unclassifiable msg: " + msg);
		else {
			fvSlicer.sendMsg(msg, fvClassifier);
		}
	}

}
