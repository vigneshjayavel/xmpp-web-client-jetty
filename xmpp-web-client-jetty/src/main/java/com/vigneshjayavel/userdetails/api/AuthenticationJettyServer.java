package com.vigneshjayavel.userdetails.api;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class AuthenticationJettyServer {
	public static void main(String[] args) throws Exception {

		Server server = new Server(8888);
		ServletContextHandler context = new ServletContextHandler(
				ServletContextHandler.SESSIONS);
		context.setContextPath("/getUserDetails");
		server.setHandler(context);

		context.addServlet(new ServletHolder(new AuthenticationServlet()), "/*");
		server.start();

	}
}
