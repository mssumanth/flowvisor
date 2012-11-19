package org.flowvisor.counters;

import java.util.HashMap;
import java.util.Map;

import org.flowvisor.classifier.FVSendMsg;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;
import org.openflow.protocol.OFMessage;

public class SendRecvDropStats {

	public static final String NO_STATS_AVAILABLE_MSG = "No stats exist for this slice";

	public enum FVStatsType {
		SEND, RECV, DROP
	}

	Map<FVStatsType, FVStatsGroup> stats;

	public SendRecvDropStats() {
		this(new FVStatsGroup(), new FVStatsGroup(), new FVStatsGroup());
	}

	public SendRecvDropStats(FVStatsGroup send, FVStatsGroup recv,
			FVStatsGroup drop) {
		this.stats = new HashMap<FVStatsType, FVStatsGroup>();
		this.stats.put(FVStatsType.SEND, send);
		this.stats.put(FVStatsType.RECV, recv);
		this.stats.put(FVStatsType.DROP, drop);
	}

	public String combinedString() {
		StringBuffer ret = new StringBuffer();
		ret.append("---Sent---\n");
		ret.append(this.stats.get(FVStatsType.SEND).toString());
		ret.append("---Recv---\n");
		ret.append(this.stats.get(FVStatsType.RECV).toString());
		ret.append("---Drop---\n");
		ret.append(this.stats.get(FVStatsType.DROP).toString());
		return ret.toString();
	}

	public void increment(FVStatsType stat, FVSendMsg from, OFMessage ofm) {
		FVLog.log(LogLevel.TRACE, null, "SendRecvDropStats: increment");
		this.stats.get(stat).increment(from, ofm);
	}

	public long get(FVStatsType stat, FVSendMsg from, OFMessage ofm) {
		return this.stats.get(stat).get(from, ofm);
	}

	public FVStats getTotal(FVStatsType stat) {
		return this.stats.get(stat).total;
	}

	public long getTotal(FVStatsType stat, OFMessage ofm) {
		return this.stats.get(stat).getTotal(ofm);
	}

	public static SendRecvDropStats createSharedStats(String owner) {
		return new SendRecvDropStats(FVStatsGroup.createSharedStats(owner
				+ FVStatsType.SEND), FVStatsGroup.createSharedStats(owner
				+ FVStatsType.RECV), FVStatsGroup.createSharedStats(owner
				+ FVStatsType.DROP));
	}
}
