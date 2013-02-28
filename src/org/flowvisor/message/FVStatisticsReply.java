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
		if (this.getStatistics().size() < 1) {
			logger.warn("{} dropping unclassifiable msg: {}", fvClassifier.getName(), this.getClass().getName());
			return;
		}
		
		OFStatistics stat = this.getStatistics().get(0);
        assert (stat instanceof ClassifiableStatistic);
        ((ClassifiableStatistic) stat).classifyFromSwitch(this,
                        fvClassifier);
		
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
				logger.debug("{} got descriptions stats: {}",topologyConnection.getName(), stat);
				topologyConnection
						.setDescriptionStatistics((FVDescriptionStatistics) stat);
			} else {
				logger.debug("{} ignoring unrequested stat: {}", topologyConnection.getName(), stat);
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
			logger.warn("msg failed sanity check: {}", this.getClass().getName());
			return false;
		}
	}
}
