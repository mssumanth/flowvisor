package org.flowvisor.message.statistics;

import java.util.Iterator;

import org.flowvisor.classifier.FVClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.message.FVMessageUtil;
import org.flowvisor.message.FVStatisticsReply;
import org.flowvisor.message.FVStatisticsRequest;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.statistics.OFPortStatisticsReply;
import org.openflow.protocol.statistics.OFStatistics;

public class FVPortStatisticsReply extends OFPortStatisticsReply implements
		SlicableStatistic, ClassifiableStatistic {
	final static Logger logger = LoggerFactory.getLogger(FVPortStatisticsReply.class);
	@Override
	public void classifyFromSwitch(FVStatisticsReply msg,
			FVClassifier fvClassifier) {
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(msg, fvClassifier);
        if (fvSlicer == null) {
                logger.warn("{} - dropping unclassifiable port stats reply: {}", fvClassifier.getName(), this.getClass().getName());
                return;
        }
        for (Iterator<OFStatistics> it = msg.getStatistics().iterator(); it
                        .hasNext();) {
                OFStatistics stat = it.next();
                if (stat instanceof OFPortStatisticsReply) {
                        OFPortStatisticsReply portStat = (OFPortStatisticsReply) stat;
                        if (!fvSlicer.portInSlice(portStat.getPortNumber())) {
                        		logger.debug("{} Dropping port {} because it is not in slice {}", fvClassifier.getName(), portStat.getPortNumber(), 
                        				fvSlicer.getSliceName());
                                it.remove();
                                msg.setLengthU(msg.getLengthU() - portStat.computeLength());
                        }
                }
        }
        if (msg.getStatistics().size() == 0) {
        	logger.debug("{} Dropping emptied port stats reply", fvClassifier.getName());
        	return;
        }
        	
       
        fvSlicer.sendMsg(msg, fvClassifier);
		
	}

	@Override
	public void sliceFromController(FVStatisticsRequest msg,
			FVClassifier fvClassifier, FVSlicer fvSlicer) {
		logger.warn("{} dropping unexpected msg: {}", fvSlicer.getName(), this.getClass().getName());
	}
}
