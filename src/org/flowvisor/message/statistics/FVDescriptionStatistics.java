package org.flowvisor.message.statistics;

import java.net.InetSocketAddress;

import org.flowvisor.FlowVisor;
import org.flowvisor.classifier.FVClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFDescriptionStatistics;

public class FVDescriptionStatistics extends OFDescriptionStatistics implements
		SlicableStatistic, ClassifiableStatistic {

	final static Logger logger = LoggerFactory.getLogger(FlowVisor.class);
	/**
	 * NOTE: we no long do any DescriptionStatistics rewriting, now that 1.0
	 * support dp_desc field.
	 *
	 * NOTE: now that no 1.0 vendors use dp_desc the way I wanted them to, we're
	 * going to do rewriting again... *sigh*
	 */

	@Override
	public void classifyFromSwitch(FVStatisticsReply msg, FVClassifier fvClassifier) {
		
		if (fvClassifier.wantStatsDescHack()) {
			InetSocketAddress remote = (InetSocketAddress) fvClassifier
					.getSocketChannel().socket().getRemoteSocketAddress();

			this.datapathDescription += " (" + remote.getAddress().getHostAddress() + ":"
					+ remote.getPort() + ")";

			if (this.datapathDescription.length() > FVDescriptionStatistics.DESCRIPTION_STRING_LENGTH)
				this.datapathDescription = this.datapathDescription.substring(
						0,
						FVDescriptionStatistics.DESCRIPTION_STRING_LENGTH - 1);
		}
		FVMessageUtil.untranslateXidAndSend(msg, fvClassifier);
		
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg, FVClassifier fvClassifier,
			FVSlicer fvSlicer) {
		logger.info("{} FVDescriptions requests have no body; message is illegal. Dropping: {}",fvSlicer.getName(), this.getClass().getName());		
	}
}
