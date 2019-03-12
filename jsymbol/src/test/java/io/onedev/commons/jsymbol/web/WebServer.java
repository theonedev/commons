package io.onedev.commons.jsymbol.web;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServer {
	
	private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
	
	public static void main(String[] args) {
		System.setProperty("wicket.configuration", "development");
		System.setProperty("org.eclipse.jetty.server.Request.maxFormContentSize", "5000000");		

		Server server = new Server();
		HttpConfiguration http_config = new HttpConfiguration();
		http_config.setOutputBufferSize(32768);

		ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
		http.setPort(8080);
		http.setIdleTimeout(1000 * 60 * 60);

		server.addConnector(http); 

		WebAppContext bb = new WebAppContext();
		bb.setServer(server);
		bb.setContextPath("/");
		bb.setWar("src/test/webapp");

		server.setHandler(bb);

		try {
			server.start();
			
			logger.info("Running! Point your browsers to http://localhost:8080");
			
			server.join();
		} catch (Exception e) {
			logger.error("Error running web server", e);
			System.exit(1);
		}
	}
}
