package org.flowvisor.flows;

import java.io.Serializable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.flowvisor.events.FVEventHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.flowvisor.openflow.protocol.FVMatch;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFFlowRemoved;

/**
 * Internal DB for tracking the switch's state
 *
 * NOT internally thread-safe
 *
 * @author capveg
 *
 */

public class LinearFlowDB implements FlowDB, Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	SortedSet<FlowDBEntry> db;
	long dpid;
	transient FVEventHandler fvEventHandler;
	transient int flowID;
	
	final static Logger logger = LoggerFactory.getLogger(LinearFlowDB.class);

	public LinearFlowDB(FVEventHandler fvEventHandler) {
		this.db = new TreeSet<FlowDBEntry>();
		this.fvEventHandler = fvEventHandler;
		this.flowID = 0;
	}

	@Override
	public void processFlowMod(OFFlowMod flowMod, long dpid, String sliceName) {
		String op = "unknown";
		switch (flowMod.getCommand()) {
		case OFFlowMod.OFPFC_ADD:
			op = "ADD";
			processFlowModAdd(flowMod, sliceName, dpid);
			break;
		case OFFlowMod.OFPFC_MODIFY:
		case OFFlowMod.OFPFC_MODIFY_STRICT:
			op = "MOD";
			processFlowModModify(flowMod, sliceName, dpid);
			break;
		case OFFlowMod.OFPFC_DELETE_STRICT:
			op = "DEL";
			processFlowModDeleteStrict(flowMod, sliceName, dpid);
			break;
		case OFFlowMod.OFPFC_DELETE:
			op = "DEL";
			processFlowModDelete(flowMod, sliceName, dpid);
			break;
		default:
			logger.warn("{} flowDB: ignore fm with unknown flow_mod command:: {}", fvEventHandler.getName(),
					flowMod.getCommand());
		}
		logger.debug("flowdb: {}: new size {} ", op, size());
	}

	private void processFlowModDeleteStrict(OFFlowMod flowMod,
			String sliceName, long dpid) {
		boolean found = false;
		for (Iterator<FlowDBEntry> it = this.db.iterator(); it.hasNext();) {
			FlowDBEntry flowDBEntry = it.next();
			if (flowDBEntry.matches(dpid, new FVMatch(flowMod.getMatch()),
					flowMod.getCookie(), flowMod.getPriority()).getMatchType() == MatchType.EQUAL) {
				logger.debug("{} flowDB: del by strict: {}", fvEventHandler.getName(), flowDBEntry);
				it.remove();
				found = true;
			}
		}
		if (!found)
			logger.debug("{} flowDB: delete strict - no match found", fvEventHandler.getName());
	}

	/**
	 * Remove one or more flowdb entries
	 *
	 * fail silently if there is nothing deleted
	 *
	 * @param flowMod
	 * @param sliceName
	 */

	private void processFlowModDelete(OFFlowMod flowMod, String sliceName,
			long dpid) {
		boolean found = false;
		for (Iterator<FlowDBEntry> it = this.db.iterator(); it.hasNext();) {
			FlowDBEntry flowDBEntry = it.next();
			MatchType matchType = flowDBEntry.matches(dpid, new FVMatch(flowMod.getMatch()),
					flowMod.getCookie(), flowMod.getPriority()).getMatchType();
			logger.debug("flowdb {} == {}", flowDBEntry.getCookie(), flowMod.getCookie());
			if (matchType == MatchType.EQUAL || matchType == MatchType.SUPERSET) {
				logger.debug("{} flowDB: del by non-strict: {}", fvEventHandler.getName(), flowDBEntry);
				it.remove();
				found = true;
			}
		}
		if (!found)
			logger.debug("{} flowDB: delete - no match found", fvEventHandler.getName());
	}

	/**
	 * Change one or more flowdb entries
	 *
	 * fail silently if nothing matches
	 *
	 * @param flowMod
	 * @param sliceName
	 */
	private void processFlowModModify(OFFlowMod flowMod, String sliceName,
			long dpid) {
		logger.warn("{} flowdb: ignoring unimplemented flowMod.modify", fvEventHandler.getName());
	}

	/**
	 * Add a new flowdb entry
	 *
	 * @param flowMod
	 * @param sliceName
	 * @param dpid
	 */
	private void processFlowModAdd(OFFlowMod flowMod, String sliceName,
			long dpid) {
		FlowDBEntry flowDBEntry = new FlowDBEntry(dpid, new FVMatch(flowMod.getMatch()),
				this.flowID++, flowMod.getPriority(), flowMod.getActions(),
				sliceName, flowMod.getCookie());
		logger.debug("{} , flowDB: {} , adding new entry: {}", this.fvEventHandler.getName(), flowDBEntry, flowMod);
		this.db.add(flowDBEntry);
	}

	@Override
	public String processFlowRemoved(OFFlowRemoved flowRemoved, long dpid) {
		String sliceName = null;
		for (Iterator<FlowDBEntry> it = this.db.iterator(); it.hasNext();) {
			FlowDBEntry flowDBEntry = it.next();
			logger.debug("{}",flowDBEntry.toString());
			logger.debug("FV {} == {}",flowDBEntry.getCookie(), flowRemoved.getCookie());
			
			if (flowDBEntry.getRuleMatch().equals(flowRemoved.getMatch())
					&& flowDBEntry.getPriority() == flowRemoved.getPriority()
					&& flowDBEntry.getCookie() == flowRemoved.getCookie()
					&& flowDBEntry.getDpid() == dpid) {
				it.remove();
				
				sliceName = flowDBEntry.getSliceName();
				logger.debug("{} flowDB: removing flow '{}' matching flowRemoved: {}", this.fvEventHandler.getName(), flowDBEntry, flowRemoved);
				break;
			}
		}
		if (sliceName == null)
			logger.info("{} flowDB: ignoring unmatched flowRemoved: {}", this.fvEventHandler.getName(), flowRemoved);
		return sliceName;
	}

	@Override
	public Iterator<FlowDBEntry> iterator() {
		return this.db.iterator();
	}

	@Override
	public int size() {
		return this.db.size();
	}

}
