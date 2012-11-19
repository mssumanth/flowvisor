package org.flowvisor.api;

import org.flowvisor.log.FVLog;
import org.flowvisor.log.LogLevel;


/**
 * Thread-local list of this User's credentials
 *
 * used as a HACK to get around the fact that servlets are stateless
 *
 * @author capveg
 *
 */
public class APIUserCred {
	String userName;
	String ip;

	private final static ThreadLocal<APIUserCred> threadCred = new ThreadLocal<APIUserCred>();

	public APIUserCred() {
		this.userName = "unknown";
		this.ip = "unknown";
	}

	static public String getIP() {
		return APIUserCred.getThreadCred().ip;
	}

	static public void setIP(String ip) {
		APIUserCred.getThreadCred().ip = ip;
	}

	static public String getUserName() {
		FVLog.log(LogLevel.INFO,null,"APIUserCred: UserName- "+APIUserCred.getThreadCred().userName);
		return APIUserCred.getThreadCred().userName;
	}

	static public void setUserName(String userName) {
		APIUserCred cred = APIUserCred.getThreadCred();
		cred.userName = userName;
	}

	static public void setThreadCred(APIUserCred cred) {
		threadCred.set(cred);
	}

	static public APIUserCred getThreadCred() {
		FVLog.log(LogLevel.TRACE, null, "APIUserCred: getThreadCred" );
		APIUserCred cred = threadCred.get();
		if (cred == null) {
			cred = new APIUserCred();
			threadCred.set(cred);
		}
		return cred;
	}
}