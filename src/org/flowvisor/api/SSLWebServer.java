package org.flowvisor.api;

import java.net.ServerSocket;

import javax.net.ssl.SSLServerSocketFactory;

import org.apache.xmlrpc.webserver.WebServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSLWebServer extends WebServer {
	
	final static Logger logger = LoggerFactory.getLogger(SSLWebServer.class);

	public SSLWebServer(int pPort) {
		super(pPort);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected ServerSocket createServerSocket(int pPort, int backlog,
			java.net.InetAddress addr) {
		try {
			//ServerSocketFactory sslFactory = (ServerSocketFactory) ServerSocketFactory
			SSLServerSocketFactory sslFactory = (SSLServerSocketFactory) SSLServerSocketFactory
					.getDefault();
			String[] ciphers = sslFactory.getDefaultCipherSuites();
			if (ciphers.length == 0)
				throw new RuntimeException(
						"Need to configure SSL: no ciphers found");
			else {
				logger.debug("SSL Supports {} Ciphers:: "
						, ciphers.length);
				for (int i = 0; i < ciphers.length; i++)
					logger.debug("	{}	" , ciphers[i]);
			}
			return sslFactory.createServerSocket(pPort, backlog, addr);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

}
