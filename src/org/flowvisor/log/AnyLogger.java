package org.flowvisor.log;

import org.apache.log4j.PropertyConfigurator;
import org.flowvisor.config.ConfigError;
import org.flowvisor.config.FVConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.flowvisor.events.FVEventHandler;
import org.productivity.java.syslog4j.SyslogRuntimeException;
import org.productivity.java.syslog4j.impl.log4j.Syslog4jAppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

public class AnyLogger extends Syslog4jAppenderSkeleton implements FVLogInterface {
	
	private static final long serialVersionUID = 1L;
	static public String DEFAULT_LOGGING_FACILITY = "LOCAL7";
	static public String DEFAULT_LOGGING_IDENT = "flowvisor";
	
	
	static Logger logger = LoggerFactory.getLogger(AnyLogger.class.getName());

	//static Logger logger = Logger.getLogger(AnyLogger.class.getName());
	

	@Override
	public boolean init() {
		String propFile = System.getProperty("fvlog.configuration");
		PropertyConfigurator.configureAndWatch(propFile, 60000);
		
		logger.info("started flowvisor logger");
		return false;
	}
	
	public static void setThreshold() {
		if (logger.isTraceEnabled())
			FVLog.setThreshold(LogLevel.TRACE);
		else if (logger.isDebugEnabled())
			FVLog.setThreshold(LogLevel.DEBUG);
		else if (logger.isInfoEnabled())
			FVLog.setThreshold(LogLevel.INFO);		
		else if (logger.isWarnEnabled())
				FVLog.setThreshold(LogLevel.WARN);		
		else if (logger.isErrorEnabled())
			FVLog.setThreshold(LogLevel.ERROR);
	}
	
	
	@Override
	public void log(LogLevel level, long time, FVEventHandler source, String msg) {

		// ALI addition:
        String srcString = null;
        if (source != null)
            srcString = source.getName();
        else
            srcString = "none";

        msg = srcString + " " + msg;

		switch(level){
			case TRACE: 
				logger.trace(msg);
				break;
		
			case DEBUG: 
				logger.debug(msg);
				break;
				
			case INFO: 
				logger.info(msg);
				break;
				
			case WARN: 
				logger.warn(msg);
				break;
			case ERROR: 
				logger.error(msg);
				break;
				
			case FATAL: 
				logger.error(msg);
				break;
				
			default:
				// ALI: if we get here then we have a problem....
				logger.error("There is a problem here as it has entered the default logger level!");
				if (msg != null)
					logger.info(msg);
		}

	}
	
	
	/*
	 * (non-Javadoc)
	 * @see org.productivity.java.syslog4j.impl.log4j.Syslog4jAppenderSkeleton#requiresLayout()
	 * 
	 * Stupid Syslog4jAppender doesn't layout stuff to syslog, and also breaks log4j's 
	 * requirement to return true from this method even if you don't layout.
	 */
	
	public boolean requiresLayout() {
		return true;
	}

	@Override
	public String initialize() throws SyslogRuntimeException {
		if (this.protocol == null)
			this.protocol = TCP;//IS this ok?
		
		try {
			String fac = FVConfig.getLogFacility();
			this.facility = fac;
			if (this.facility == null) {
				this.facility = DEFAULT_LOGGING_FACILITY;
				System.err
						.println("Invalid logging facitily: failing back to default: '"
								+ fac + "'");
			}
		} catch (Exception e) {
			try {
				FVConfig.setLogFacility(DEFAULT_LOGGING_FACILITY);
				this.facility = DEFAULT_LOGGING_FACILITY;
			} catch (ConfigError e1) {
				System.err.println("Failed to set logging facility"
						+ " to '" + this.facility + ": " + e1);
			}

		}
		try {
			this.ident = FVConfig.getLogIdent();
		} catch (Exception e) {
			try {
				FVConfig.setLogIdent(DEFAULT_LOGGING_IDENT);
				this.ident = DEFAULT_LOGGING_IDENT;
			} catch (ConfigError e1) {
				System.err.println("Failed to set logging identifier " 
						+ " to '" + this.ident + ": " + e1);
			}

		}

		return this.protocol;
	}

}
