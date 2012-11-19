/**
 *
 */
package org.flowvisor.classifier;

import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;


/**
 * @author capveg
 *
 */
public class XidPair {
	int xid;
	String sliceName;

	public XidPair(int xid, String sliceName) {
		this.xid = xid;
		this.sliceName = sliceName;
	}

	public int getXid() {
		FVLog.log(LogLevel.TRACE, null, "XIDPair: xid is:" + xid);
		return xid;
	}

	public void setXid(int xid) {
		this.xid = xid;
	}

	/**
	 * @return the sliceName
	 */
	public String getSliceName() {
		FVLog.log(LogLevel.TRACE, null, "XIDPair: sliceName is:" + sliceName);
		return sliceName;
	}

	/**
	 * @param sliceName the sliceName to set
	 */
	public void setSliceName(String sliceName) {
		this.sliceName = sliceName;
	}

}
