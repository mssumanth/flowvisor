/**
 *
 */
package org.flowvisor.events;

import org.flowvisor.events.FVEvent;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

import java.nio.channels.*;

/**
 * Event: underlying socket has pending I/O
 *
 * @author capveg
 *
 */
public class FVIOEvent extends FVEvent {
	SelectionKey sk;

	public FVIOEvent(SelectionKey sk, FVEventHandler src, FVEventHandler dst) {
		super(src, dst);
		FVLog.log(LogLevel.TRACE, null, "FVIOEvent constructor");
		this.sk = sk;
	}

	public SelectionKey getSelectionKey() {
		return sk;
	}
}
