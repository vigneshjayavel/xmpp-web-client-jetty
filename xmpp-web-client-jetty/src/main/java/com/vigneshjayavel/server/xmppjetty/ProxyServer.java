package com.vigneshjayavel.server.xmppjetty;

import java.util.Properties;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

public class ProxyServer {

	public static void main(String[] args) throws Exception {
		new ProxyServer();
	}

	public ProxyServer() throws Exception {

		Properties properties = new Properties();
		properties.load(this.getClass().getResourceAsStream("/server.properties"));

		Server jettyServer = new Server(Integer.parseInt(properties.getProperty("port")));

		ResourceHandler resourceHandler = new ResourceHandler();
		resourceHandler.setResourceBase(this.getClass().getClassLoader().getResource("html")
				.toExternalForm());

		XmppWebSocketServlet xmppWebSocketServlet = new XmppWebSocketServlet();
		ServletHolder servletHolder = new ServletHolder(xmppWebSocketServlet);
		//Preventing the WebSocket connection from terminating after 5mins of idle time
		servletHolder.setInitParameter("maxIdleTime", "-1");
		ServletContextHandler servletContextHandler = new ServletContextHandler();
		servletContextHandler.addServlet(servletHolder, "/websocket/*");

		HandlerList handlerList = new HandlerList();
		handlerList.setHandlers(new Handler[] { resourceHandler, servletContextHandler });
		jettyServer.setHandler(handlerList);

		jettyServer.start();

		Handler handler = jettyServer.getHandler();
		if (handler instanceof WebAppContext) {
			System.out.println("found WebAppContext");
			WebAppContext rctx = (WebAppContext) handler;
			rctx.getSessionHandler().getSessionManager()
					.setMaxInactiveInterval(600);
		}

	}

}