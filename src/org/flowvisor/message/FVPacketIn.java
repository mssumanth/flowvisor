package org.flowvisor.message;

import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.FlowVisor;
import org.flowvisor.api.LinkAdvertisement;
import org.flowvisor.classifier.FVClassifier;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.flowvisor.flows.FlowEntry;
import org.flowvisor.flows.FlowSpaceUtil;
import org.flowvisor.flows.SliceAction;
import org.flowvisor.message.lldp.LLDPUtil;
import org.flowvisor.ofswitch.DPIDandPort;
import org.flowvisor.ofswitch.TopologyConnection;
import org.flowvisor.openflow.protocol.FVMatch;
import org.flowvisor.slicer.FVSlicer;
import org.openflow.protocol.OFFeaturesReply;
import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FVPacketIn extends OFPacketIn implements Classifiable, Slicable,
		TopologyControllable {
	
	final static Logger logger = LoggerFactory.getLogger(FVPacketIn.class);
	/**
	 * route and rewrite packet_in messages from switch to controller
	 *
	 * if it's lldp, do the lldp decode stuff else, look up the embedded
	 * packet's controller(s) by flowspace and send to them
	 */

	@Override
	public void classifyFromSwitch(FVClassifier fvClassifier) {
		// handle LLDP as a special (hackish) case
		if (LLDPUtil.handleLLDPFromSwitch(this, fvClassifier))
			return;
		// TODO add ARP special case
		this.lookupByFlowSpace(fvClassifier);

	}

	private void lookupByFlowSpace(FVClassifier fvClassifier) {
		SliceAction sliceAction;
		int perms;
		// grab single matching rule: only one because it's a point in flowspace
		FlowEntry flowEntry = fvClassifier.getSwitchFlowMap().matches(
				fvClassifier.getSwitchInfo().getDatapathId(), this.getInPort(),
				this.getPacketData());
		if (flowEntry == null) {
			logger.debug("{} dropping unclassifiable msg: {}", fvClassifier.getName(), this.toVerboseString());
			return;
		}
		
		boolean foundHome = false;
		// foreach slice in that rule
		for (OFAction ofAction : flowEntry.getActionsList()) {
			sliceAction = (SliceAction) ofAction;
			perms = sliceAction.getSlicePerms();
			if ((perms & (SliceAction.READ | SliceAction.WRITE)) != 0) {
				// lookup slice and send msg to them
				// TODO record buffer id for later validation
				FVSlicer fvSlicer = fvClassifier.getSlicerByName(sliceAction
						.getSliceName());
				if (fvSlicer == null) {
					logger.warn("{} tried to send msg to non-existant slice: {} corrupted flowspace?:: {}", 
								fvClassifier.getName(), sliceAction.getSliceName(), this.toVerboseString());
					continue;
				}
				if (fvSlicer.isConnected()) {
					if ((perms & SliceAction.WRITE) != 0)
						fvSlicer.setBufferIDAllowed(this.getBufferId());
					fvSlicer.sendMsg(this, fvClassifier);
					/**
					 * TODO : come back and decide if we should uncomment this
					 * i.e., should a rule get squashed if it's only recipient
					 * is read only
					 *
					 * if yes, then tests-readonly.py needs to be changed
					 *
					 */

					// if ((perms & SliceAction.WRITE) != 0)
					foundHome = true;
				} else {
					sendDropRule(fvClassifier, flowEntry, fvSlicer.getSliceName(), (short) 0, (short) 1);
				}
				foundHome = true;
			}
			/*
			 * ash: not sure if this is the intended behavior, but I am guessing
			 * that if this packet hasn't gotten handled anywhere we should at least
			 * default to adding a drop rule for unless we want to be flooded.
			 */
			if (!foundHome)
				sendDropRule(fvClassifier, flowEntry, "exact", (short) 0, (short) 1);
		}
	}

	/**
	 * Tell the classifier to drop packets that look like this
	 *
	 * @param fvClassifier
	 * @param flowEntry
	 * @param hardTimeout
	 * @param idleTimeout
	 */

	private void sendDropRule(FVClassifier fvClassifier, FlowEntry flowEntry,
			String sliceName, short hardTimeout, short idleTimeout) {
		FVFlowMod flowMod = (FVFlowMod) FlowVisor.getInstance().getFactory()
				.getMessage(OFType.FLOW_MOD);
		// block this exact flow
		OFMatch match = new OFMatch();
		match.loadFromPacket(this.packetData, this.inPort);
		// different from previous policty of block by rule
		// flowMod.setMatch(flowEntry.getRuleMatch());
		
		String drop_policy = null;
		try {
			drop_policy = FVConfig.getDropPolicy(sliceName);
		} catch (ConfigError e) {
			logger.error("{} Failed to retrieve drop policy from config. \nDefauting to exact drop_policy", fvClassifier.getName());
			drop_policy = "exact";
		}
		if (drop_policy.equals("exact"))
			flowMod.setMatch(match);
		else if (drop_policy.equals("rule"))
			flowMod.setMatch(flowEntry.getRuleMatch());
		else
			// Should never happen
			logger.error("{} Error in configuration!", fvClassifier.getName());
		flowMod.setCommand(FVFlowMod.OFPFC_ADD);
		flowMod.setActions(new LinkedList<OFAction>()); // send to zero-length
		// list, i.e., DROP
		flowMod.setLengthU(OFFlowMod.MINIMUM_LENGTH);
		flowMod.setHardTimeout(hardTimeout);
		flowMod.setIdleTimeout(idleTimeout);
		flowMod.setPriority((short) 0); // set to lowest priority
		flowMod.setFlags((short) 1);
		// send removed msg (1), not the check overlap (2), or
		// emergency flow cache (4)

		logger.warn("{} inserting drop (hard= {}, idle= {}) rule for {}", fvClassifier.getName(), 
				hardTimeout , idleTimeout , flowEntry);
		fvClassifier.sendMsg(flowMod, fvClassifier);
	}

	private String toVerboseString() {
		String pkt;
		if (this.packetData != null)
			pkt = new OFMatch().loadFromPacket(this.packetData, this.inPort)
					.toString();
		else
			pkt = "empty";
		return this.toString() + ";pkt=" + pkt;
	}

	@Override
	public void sliceFromController(FVClassifier fvClassifier, FVSlicer fvSlicer) {
		FVMessageUtil.dropUnexpectedMesg(this, fvSlicer);
	}

	@Override
	public FVPacketIn setPacketData(byte[] packetData) {
		if (packetData == null)
			this.length = (short) (MINIMUM_LENGTH);
		else
			this.length = (short) (MINIMUM_LENGTH + packetData.length);
		this.packetData = packetData;
		return this;
	}

	/**
	 * The topologyController handles LLDP messages and ignores everything else
	 */
	@Override
	public void topologyController(TopologyConnection topologyConnection) {
		synchronized (topologyConnection) {
			DPIDandPort dpidandport = TopologyConnection.parseLLDP(this
					.getPacketData());
			if (dpidandport == null) {
				logger.debug("{} ignoring non-lldp packetin: {}", topologyConnection.getName(), this.toVerboseString());
				return;
			}
			OFFeaturesReply featuresReply = topologyConnection
					.getFeaturesReply();
			if (featuresReply == null) {
				logger.warn("{} ignoring packet_in: no features_reply yet", topologyConnection.getName());
				return;
			}
			LinkAdvertisement linkAdvertisement = new LinkAdvertisement(
					dpidandport.getDpid(), dpidandport.getPort(), featuresReply
							.getDatapathId(), this.inPort);
			topologyConnection.getTopologyController().reportProbe(
					linkAdvertisement);
			topologyConnection.signalFastPort(this.inPort);
		}
	}

	public static void main(String args[]) throws FileNotFoundException, ConfigError {
		if (args.length < 3) {
			System.err.println("Usage: <config.xml> <dpid> <ofmatch>");
			System.exit(1);
		}

		FVConfig.readFromFile(args[0]);
		long dpid = FlowSpaceUtil.parseDPID(args[1]);
		FVMatch packet = new FVMatch();
		packet.fromString(args[2]);

		System.err.println("Looking up packet '" + packet + "' on dpid="
				+ FlowSpaceUtil.dpidToString(dpid));
		List<FlowEntry> entries = FVConfig.getFlowSpaceFlowMap().matches(dpid,
				packet);

		System.err.println("Matches found: " + entries.size());
		if (entries.size() > 1)
			System.err.println("WARN: only sending to the first match");
		for (FlowEntry flowEntry : entries) {
			System.out.println(flowEntry);
		}

	}
}
