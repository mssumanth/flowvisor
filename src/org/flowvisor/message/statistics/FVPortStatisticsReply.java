package org.flowvisor.message.statistics;

import java.util.Iterator;

import org.flowvisor.classifier.FVClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;

public class FVPortStatisticsReply extends OFPortStatisticsReply implements
		SlicableStatistic, ClassifiableStatistic {

	final static Logger logger = LoggerFactory.getLogger(FVPortStatisticsReply.class);
	@Override
	public void sliceFromController(OFMessage msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		logger.warn("{} dropping unexpected msg: {}", fvSlicer.getName(), msg);
	}

	@Override
	public void classifyFromSwitch(OFMessage msg, FVClassifier fvClassifier) {
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(msg, fvClassifier);
		if (fvSlicer == null) {
			logger.warn("fvSlicer - dropping unclassifiable port stats reply: {}", this.getClass());
			return;
		}
		boolean changed = false;
		OFStatisticsReply statsReply = (OFStatisticsReply) msg;
		for (Iterator<OFStatistics> it = statsReply.getStatistics().iterator(); it
				.hasNext();) {
			OFStatistics stat = it.next();
			if (stat instanceof OFPortStatisticsReply) {
				OFPortStatisticsReply portStat = (OFPortStatisticsReply) stat;
				if (!fvSlicer.portInSlice(portStat.getPortNumber())) {
					it.remove();
					changed = true;
				}
			}
		}
		if (changed) { // removed a stat; rebuild packet
			int statsLen = 0;
			for (OFStatistics stat : statsReply.getStatistics()) {
				statsLen += stat.getLength();
			}
			statsReply.setLengthU(statsLen + OFStatisticsReply.MINIMUM_LENGTH);
		}
		fvSlicer.sendMsg(msg, fvClassifier);
	}
}
