package org.flowvisor.events;

import org.flowvisor.config.FVConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FVEventUtils {

	final static Logger logger = LoggerFactory.getLogger(FVEventUtils.class);
	/**
	 * Test to see if more than FVConfig.DelayWarning ms have passed, and if so,
	 * issue a warning log msg
	 *
	 * @param startCounter
	 * @param handler
	 * @param e
	 */
	static public void starvationTest(long startCounter,
			FVEventHandler handler, FVEvent e) {
		long stopCounter = System.currentTimeMillis();
		if ((stopCounter - startCounter) > FVConfig.DelayWarning) {
			logger.error(e.getDst().getName()+
					"STARVING: handling event took "
							+ (stopCounter - startCounter) + "ms: " + e);
		}
	}
}
