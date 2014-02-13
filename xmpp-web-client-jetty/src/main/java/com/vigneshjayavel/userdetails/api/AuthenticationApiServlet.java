package com.vigneshjayavel.userdetails.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;

import com.vigneshjayavel.userdetails.pojo.Ticket;
import com.vigneshjayavel.userdetails.pojo.UserDetails;

/**
 * This servlet runs in Jetty. This acts as a API for the JS client to get the
 * userdetails based on the ticket that is fetched from the cookie
 * 
 */
public class AuthenticationApiServlet extends HttpServlet {

	private static final long serialVersionUID = -6154475799000019575L;

	private static ObjectMapper mapper = new ObjectMapper();

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String ticketValue = request.getParameter("ticket");
		Ticket ticket = new Ticket(ticketValue);
		System.out.println("Ticket : " + ticket);
		UserDetails userDetails = validateAndGetUserDetails(ticket);
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println(mapper.writeValueAsString(userDetails));
	}
	
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException{
		this.doGet(request, response);
	}

	/*
	 * public static void main(String[] args) { new
	 * AuthenticationServlet().validateAndGetUserDetails(new Ticket(
	 * "TICKET_4f5b2833e9638815aee4df378a73dddbbe4e6ca2")); }
	 */

	private UserDetails validateAndGetUserDetails(Ticket ticket) {

		String ticketValue = ticket.getTicket();
		// get today's date
		Date date = new Date();
		SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd");
		String today = df.format(date);
		// do a db query in table "apistats_today"
		// this table stores the log of the api calls from the clients
		// for a valid ticket, there must be a record corresponding to the login
		// api call from the user's browser
		String tableName = "apistats_" + today;
		String queryString = "SELECT * FROM " + tableName
				+ " WHERE ACTION = 'login' AND resultcode=200 AND ticket='"
				+ ticketValue + "'";
		Map<String, String> resultMap = doDbQuery(queryString);
		for (Map.Entry<String, String> field : resultMap.entrySet()) {
			System.out.println(field.getKey() + ">>" + field.getValue());
		}
		String requestString = resultMap.get("requeststring");
		// for a succesful db query, we get a record.
		// there is field called "requeststring" that stores details about the
		// user's login details.
		// we need to parse this field to get the userName and orgName.
		Map<String, String> userLoginDetails = processLoginApiRequestString(requestString);
		UserDetails userDetails = new UserDetails();
		userDetails.setUserName(userLoginDetails.get("userName"));
		userDetails.setOrgName(userLoginDetails.get("orgName"));
		userDetails.setStatus("200");
		return userDetails;
	}

	private Map<String, String> processLoginApiRequestString(
			String requestString) {
		Map<String, String> result = new HashMap<String, String>();
		String[] components = requestString.split("=");
		System.out.println("requeststring components.........");
		for (String s : components) {
			System.out.println(s);
		}
		String userName = components[4].split("&")[0];
		String orgName = components[1].split("&")[0];
		result.put("userName", userName);
		result.put("orgName", orgName);
		return result;
	}

	private Map<String, String> doDbQuery(String queryString) {

		Connection connect = null;
		Statement statement = null;
		ResultSet resultSet = null;
		System.out.println(queryString);
		Map<String, String> resultMap = new HashMap<String, String>();
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Properties properties = new Properties();
			properties.load(this.getClass().getResourceAsStream(
					"/server.properties"));
			connect = DriverManager.getConnection("jdbc:mysql://"
					+ properties.getProperty("dbserver") + "/"
					+ properties.getProperty("dbname") + "?" + "user="
					+ properties.getProperty("dbusername") + "&password="
					+ properties.getProperty("dbpassword"));
			statement = connect.createStatement();
			resultSet = statement.executeQuery(queryString);
			// process resultset
			if (resultSet.next()) {
				ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
				int columns = resultSetMetaData.getColumnCount();
				for (int i = 1; i <= columns; ++i) {
					resultMap.put(resultSetMetaData.getColumnName(i), resultSet
							.getObject(i).toString());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (resultSet != null) {
					resultSet.close();
				}
				if (statement != null) {
					statement.close();
				}
				if (connect != null) {
					connect.close();
				}
			} catch (Exception e) {

			}
		}
		return resultMap;
	}
}
