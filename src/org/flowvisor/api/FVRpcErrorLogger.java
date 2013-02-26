package org.flowvisor.api;

import org.apache.xmlrpc.server.XmlRpcErrorLogger;
import org.flowvisor.exceptions.FVException;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;


/**
 * A wrapper around FVLog for the XMLRPC webserver
 *
 * @author capveg
 *
 */

public class FVRpcErrorLogger extends XmlRpcErrorLogger {
	
	final static Logger logger = Logger.getLogger(FVRpcErrorLogger.class);

	/**
	 * Wrapper around FVLog;
	 *
	 * If the throwable is an FVException or SSLException, assume
	 * that it's just an API call that is intentionally propagating an
	 * error back to the caller, so we just log it as DEBUG.
	 * Else log as WARN.
	 * @param msg A string to log
	 * @param throwable an exception to log
	 */

	@SuppressWarnings("deprecation")
	@Override
	public void log(String msg, Throwable throwable) {
		Priority pLevel = Priority.WARN;
		Throwable cause = throwable.getCause();
		if (cause instanceof FVException ||
				cause instanceof javax.net.ssl.SSLException)
			pLevel = Priority.DEBUG;
		if (cause != null)
			throwable = cause;	// skip down to the inner exception
		 StackTraceElement[] stackTrace= throwable.getStackTrace();
		logger.log(pLevel, msg + " exception  = " + throwable);
		for(int i=0; i< stackTrace.length; i++)
			logger.log(pLevel, "at " + stackTrace[i]);

		//logger.l7dlog(pLevel, msg + "\n The exception is = ", throwable);
	}

	@Override
	public void log(String msg) {
		logger.info(msg);
	}
}
