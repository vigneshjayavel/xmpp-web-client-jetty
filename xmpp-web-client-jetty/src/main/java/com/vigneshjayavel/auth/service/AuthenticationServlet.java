package com.vigneshjayavel.auth.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet runs in Jetty.
 * This acts as a API for the JS client to get the userdetails based on the ticket that is fetched from the cookie
 * 
 */
public class AuthenticationServlet extends HttpServlet{
	
	private static final long serialVersionUID = -6154475799000019575L;
	
	private static final String userDetails = "Hello World";

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException,
			IOException {
		
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println(userDetails);
	}
	
	private Map<String, String> process(String ticket){
		
		return validateAndGetUserDetails(ticket);
		
	}
	
	private Map<String,String> validateAndGetUserDetails(String ticket){
		
		return null;
	}
		
	private List<String> doQuery(String query){
		
		return null;
	}
}
