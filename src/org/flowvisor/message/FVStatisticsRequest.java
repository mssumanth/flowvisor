package org.flowvisor.message;

import java.util.List;

import org.flowvisor.classifier.FVClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.message.statistics.SlicableStatistic;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFStatisticsMessageBase;
import org.openflow.protocol.OFStatisticsRequest;
import org.openflow.protocol.statistics.OFStatistics;

public class FVStatisticsRequest extends OFStatisticsRequest implements
		Classifiable, Slicable, SanityCheckable {
	
	final static Logger logger = LoggerFactory.getLogger(FVFeaturesReply.class);

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		logger.warn( "{} dropping unexpected msg: {}", fvClassifier.getName(), this.getClass());
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		// TODO: come back and retool FV stats handling to make this less fugly
		List<OFStatistics> statsList = this.getStatistics();
		if (statsList.size() > 0) { // if there is a body, do body specific
			// parsing
			OFStatistics stat = statsList.get(0);
			assert (stat instanceof SlicableStatistic);
			((SlicableStatistic) stat).sliceFromController(this, fvClassifier,
					fvSlicer);
		} else {
			// else just slice by xid and hope for the best
			FVMessageUtil.translateXidAndSend(this, fvClassifier, fvSlicer);
		}
	}

	@Override
	public String toString() {
		return super.toString() + ";st=" + this.getStatisticType();
		// ";mfr=" + this.getManufacturerDescription() +
	}

	/**
	 * Check to make sure the packet really has all of the statistics is claims
	 * to have. This is really to make sure the framing is correct.
	 */
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
			logger.warn("msg failed sanity check: {}", this.getClass());
			return false;
		}
	}

}
