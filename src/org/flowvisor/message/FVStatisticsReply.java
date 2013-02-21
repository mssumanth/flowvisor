package org.flowvisor.message;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.message.statistics.ClassifiableStatistic;
import org.flowvisor.message.statistics.FVDescriptionStatistics;
import org.flowvisor.ofswitch.TopologyConnection;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFStatisticsMessageBase;
import org.openflow.protocol.OFStatisticsReply;
import org.openflow.protocol.statistics.OFDescriptionStatistics;
import org.openflow.protocol.statistics.OFStatistics;

public class FVStatisticsReply extends OFStatisticsReply implements
		Classifiable, Slicable, TopologyControllable, SanityCheckable {
	
	final static Logger logger = LoggerFactory.getLogger(FVStatisticsReply.class);

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		// TODO: come back and retool FV stats handling to make this less fugly
		List<OFStatistics> statsList = this.getStatistics();
		if (statsList.size() > 0) { // if there is a body, do body specific
			// parsing
			OFStatistics stat = statsList.get(0);
			assert (stat instanceof ClassifiableStatistic);
			((ClassifiableStatistic) stat).classifyFromSwitch(this,
					fvClassifier);
		} else {
			// else just classify by xid and hope for the best
			FVSlicer fvSlicer = FVMessageUtil
					.untranslateXid(this, fvClassifier);
			if (fvSlicer == null)
				logger.warn(fvClassifier.getName(),
						"dropping unclassifiable msg: " + this);
			else
				fvSlicer.sendMsg(this, fvClassifier);
		}
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// should never get stats replies from controller
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}

	@Override
	public String toString() {
		return super.toString() + ";st=" + this.getStatisticType();
		// ";mfr=" + this.getManufacturerDescription() +
	}

	@Override
	public void topologyController(TopologyConnection topologyConnection) {
		List<OFStatistics> statList = this.getStatistics();
		for (OFStatistics stat : statList) {
			if (stat instanceof OFDescriptionStatistics) {
				logger.debug(topologyConnection.getName(),
						" got descriptions stats: " + stat);
				topologyConnection
						.setDescriptionStatistics((FVDescriptionStatistics) stat);
			} else {
				logger.debug(topologyConnection.getName(),
						"ignoring unrequested stat: " + stat);
			}
		}
	}

	@Override
	public boolean isSane() {
		int msgLen = this.getLengthU();
		int count;
		count = OFStatisticsMessageBase.MINIMUM_LENGTH;
		for (OFStatistics stat : this.getStatistics()) {
			count += stat.getLength();
		}
		if (count == msgLen)
			return true;
		else {
			logger.warn("msg failed sanity check: " + this);
			return false;
		}
	}
}
