package org.flowvisor.message;

import org.flowvisor.classifier.FVClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFGetConfigReply;

public class FVGetConfigReply extends OFGetConfigReply implements Classifiable,
		Slicable {
	
	final static Logger logger = LoggerFactory.getLogger(FVGetConfigReply.class);

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		FVSlicer fvSlicer = FVMessageUtil.untranslateXid(this, fvClassifier);
		if (fvSlicer == null) {
			logger.warn(" {} dropping unclassifiable xid in GetConfigReply: {}" , fvClassifier.getName(),  this.getClass());
			return;
		}
		this.setMissSendLength(fvSlicer.getMissSendLength());
		fvSlicer.sendMsg(this, fvClassifier);
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}
}
