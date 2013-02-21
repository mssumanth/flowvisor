package org.flowvisor;

import org.flowvisor.config.FVConfigurationController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Attempts to hook into the VM to detect a shutdown 
 * procedure. It is not guaranteed but better than 
 * nothing.
 * 
 * Shuts down the db backend cleanly.
 * 
 * 
 * @author ash
 *
 */
public class ShutdownHook extends Thread {
	
	final static Logger logger = LoggerFactory.getLogger(ShutdownHook.class);
	
	public void run() {
		logger.info("Shutting down config database.");
		FVConfigurationController.instance().shutdown();
    }
}
