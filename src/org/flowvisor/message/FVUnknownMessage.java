package org.flowvisor.message;

import java.nio.ByteBuffer;

import org.flowvisor.classifier.FVClassifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFMessage;
import org.openflow.util.U32;
import org.openflow.util.U8;

public class FVUnknownMessage extends OFMessage implements Classifiable,
		Slicable {
	byte[] data;
	byte unknownType;
	
	final static Logger logger = LoggerFactory.getLogger(FVUnknownMessage.class);

	public FVUnknownMessage() {
		super();
		this.unknownType = -1;
	}

	@Override
	public void readFrom(ByteBuffer bb) {
		int pos = bb.position(); // intentionally ignoring mark() in case it's
		// in use
		this.unknownType = bb.get(pos + 1);
		bb.position(pos);
		logger.warn("read unhandled OFMessage type "
				+ this.type);
		super.readFrom(bb);
		int left = this.length - OFMessage.MINIMUM_LENGTH;
		if (left > 0) {
			this.data = new byte[left];
			bb.get(this.data);
		}
	}

	@Override
	public void writeTo(ByteBuffer bb) {
		super.writeTo(bb);
		if (data != null && data.length > 0)
			bb.put(data);
	}

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		logger.warn(fvClassifier.getName(),
				"tried to classify UNKNOWN OF message: giving up");
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		logger.warn(fvSlicer.getName(),
				"tried to slice UNKNOWN OF message: giving up");
	}

	@Override
	public String toString() {
		return "ofmsg" + ":v=" + U8.f(this.getVersion()) + ";t=" + "UNKNOWN-"
				+ this.unknownType + ";l=" + this.getLengthU() + ";x="
				+ U32.f(this.getXid());
	}

}
