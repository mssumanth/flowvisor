/**
 *
 */
package org.flowvisor.events;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.LinkedList;
import java.util.List;

import org.flowvisor.FlowVisor;
import org.flowvisor.api.APIUserCred;
import org.flowvisor.exceptions.UnhandledEvent;
import org.flowvisor.fvtimer.FVTimer;
import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;

/**
 * @author capveg
 *
 */
public class FVEventLoop {
	long thread_id;
	boolean shouldStop;
	Selector selector;
	FVTimer fvtimer;
	List<FVEvent> eventQueue;

	public FVEventLoop() throws IOException {
		this.shouldStop = false;
		this.selector = Selector.open();
		this.fvtimer = new FVTimer();
		this.eventQueue = new LinkedList<FVEvent>();
	}

	/**
	 * Register a new event listener with socket
	 *
	 * @param ch
	 * @param ops
	 * @param handler
	 */

	public void register(SelectableChannel ch, int ops, FVEventHandler handler) {
		FVLog.log(LogLevel.TRACE,null,"FVEventLoop: Registering a new event listener with socket");
		FlowVisor.getInstance().addHandler(handler);
		try {
			ch.register(selector, ops, handler);
		} catch (ClosedChannelException e) {
			FVLog.log(LogLevel.WARN, null, "Tried to register channel ", ch,
					" but got :", e);
		}
	}

	/**
	 * Clean up after a dying event handler
	 *
	 * @param ch
	 *            the socket that was previous registered
	 * @param handler
	 *            the handler that was previously registered
	 */
	public void unregister(SelectableChannel ch, FVEventHandler handler) {
		// note, apparently you don't have to manually cancel a selectable
		// channel; it's handled by socket.close()
		FVLog.log(LogLevel.TRACE,null,"FVEventLoop: unregister");
		FlowVisor.getInstance().removeHandler(handler);
	}

	public void queueEvent(FVEvent e) {
		FVLog.log(LogLevel.DEBUG, null, "FVEventLoop: Queing Events");
		synchronized (eventQueue) {
			eventQueue.add(e);
		}
		selector.wakeup(); // tell the selector to come out of sleep: awesome
		// call!
	}

	public long getThreadContext() {
		return thread_id;
	}

	/****
	 * Pass a timer event on to the Timer class
	 *
	 * @param e
	 */
	public void addTimer(FVTimerEvent e) {
		fvtimer.addTimer(e);
	}

	/**
	 * Tell this EventLoop to stop
	 */
	public void shouldStop() {
		shouldStop = true;
	}

	/****
	 * Main top-level IO loop this dispatches all IO events and timer events
	 * together I believe this is fairly efficient for processing IO events and
	 * events queued should cause the select to wake up
	 *
	 */
	public void doEventLoop() throws IOException, UnhandledEvent {
		FVLog.log(LogLevel.DEBUG,null, "Main top level I/O loop");
		this.thread_id = Thread.currentThread().getId();
		while (!shouldStop) {
			long nextTimerEvent;
			int nEvents;
			List<FVEvent> tmpQueue = null;
			long startCounter;

			// copy queued events out of the way and clear queue
			synchronized (eventQueue) {
				if (!eventQueue.isEmpty()) {
					FVLog.log(LogLevel.DEBUG,null,"EventQueue is not empty!");
					tmpQueue = eventQueue;
					eventQueue = new LinkedList<FVEvent>();
				}
			}

			// process all queued events, if any
			if (tmpQueue != null)
				for (FVEvent e : tmpQueue) {
					startCounter = System.currentTimeMillis();
					e.getDst().handleEvent(e);
					FVEventUtils.starvationTest(startCounter, e.getDst(), e);
				}

			// calc time until next timer event
			nextTimerEvent = this.fvtimer.processEvent(); // this fires off a
			// timer event if
			// it's ready

			// update the interested ops for each event handler
			int ops;
			FVEventHandler handler;
			for (SelectionKey sk : selector.keys()) {
				ops = 0;
				if (!sk.isValid())
					continue;
				handler = (FVEventHandler) sk.attachment();
				if (handler.needsRead())
					ops |= SelectionKey.OP_READ;
				if (handler.needsWrite())
					ops |= SelectionKey.OP_WRITE;
				if (handler.needsConnect())
					ops |= SelectionKey.OP_CONNECT;
				if (handler.needsAccept())
					ops |= SelectionKey.OP_ACCEPT;
				sk.interestOps(ops);
			}

			// wait until next IO event or timer event
			FVLog.log(LogLevel.DEBUG, null, "calling select with timeout=",
					nextTimerEvent);
			nEvents = selector.select(nextTimerEvent);
			if (nEvents > 0) {
				for (SelectionKey sk : selector.selectedKeys()) {
					if (sk.isValid()) { // skip any keys that have been canceled
						handler = (FVEventHandler) sk.attachment();
						FVLog.log(LogLevel.DEBUG, null, "sending IO Event= ",
								sk.readyOps(), " to ", handler.getName());
						startCounter = System.currentTimeMillis();
						FVIOEvent ioEvent = new FVIOEvent(sk, null, handler);
						handler.handleEvent(ioEvent);
						FVEventUtils.starvationTest(startCounter, handler,
								ioEvent);
					}
				}
				selector.selectedKeys().clear(); // mark all keys as processed
			}
		}
	}
}
