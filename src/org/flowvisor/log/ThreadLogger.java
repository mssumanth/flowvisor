package org.flowvisor.log;

import java.lang.Thread.UncaughtExceptionHandler;

public class ThreadLogger implements UncaughtExceptionHandler {

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {

		FVLog.log(LogLevel.FATAL, null, "----- exception in thread "
				+ thread.toString() + "::" + exception.toString());
		StackTraceElement[] stackTrace = exception.getStackTrace();
		for (int i = 0; i < stackTrace.length; i++)
			FVLog.log(LogLevel.FATAL, null, stackTrace[i].toString());
	}

}
