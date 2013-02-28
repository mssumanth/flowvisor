package org.flowvisor;

import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadLogger implements UncaughtExceptionHandler {
	
	final static Logger logger = LoggerFactory.getLogger(ThreadLogger.class);

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {

		logger.error("----- exception in thread {} :: {}"
				, thread.toString() , exception.toString());
		StackTraceElement[] stackTrace = exception.getStackTrace();
		for (int i = 0; i < stackTrace.length; i++)
			logger.error(stackTrace[i].toString());
	}

}
