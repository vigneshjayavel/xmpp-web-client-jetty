package com.vigneshjayavel.userdetails.api;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
public class AuthenticationServlet extends HttpServlet {

	private static final long serialVersionUID = -6154475799000019575L;

	private static ObjectMapper mapper = new ObjectMapper();

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String jsonRequest = request.getParameter("ticket");
		Ticket ticket = mapper.readValue(jsonRequest, Ticket.class);
		System.out.println("Ticket : " + ticket);
		UserDetails userDetails = validateAndGetUserDetails(ticket);
		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println(userDetails);
	}

	public static void main(String[] args) {
		new AuthenticationServlet().validateAndGetUserDetails(new Ticket(
				"TICKET_236e363af975416eed99df8c84890dc1dcbbd702"));
	}

	private UserDetails validateAndGetUserDetails(Ticket ticket) {

		String ticketValue = ticket.getTicket();
		// get today's date
		Date today = new Date();
		SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd");
		String formattedDate = df.format(today);
		// do a db query in table "apistats_formattedDate"
		String tableName = "apistats_" + formattedDate;
		String queryString = "SELECT * FROM " + tableName
				+ " WHERE ACTION = 'login' AND resultcode=200 AND ticket='"
				+ ticketValue + "'";
		Map<String, Object> resultMap = doDbQuery(queryString);
		for (Map.Entry<String, Object> field : resultMap.entrySet()) {
			System.out.println(field.getKey() + ":" + field.getValue());
		}
		return null;
	}

	private Connection connect = null;
	private Statement statement = null;
	private ResultSet resultSet = null;

	private Map<String, Object> doDbQuery(String queryString) {

		Map<String, Object> resultMap = new HashMap<String, Object>();
		try {
			// This will load the MySQL driver, each DB has its own driver
			Class.forName("com.mysql.jdbc.Driver");
			Properties properties = new Properties();
			properties.load(this.getClass().getResourceAsStream(
					"/server.properties"));
			// Setup the connection with the DB
			connect = DriverManager.getConnection("jdbc:mysql://"
					+ properties.getProperty("dbserver") + "/"
					+ properties.getProperty("dbname") + "?" + "user="
					+ properties.getProperty("dbusername") + "&password="
					+ properties.getProperty("dbpassword"));
			resultSet = statement.executeQuery(queryString);
			// process resultset
			if (resultSet.next()) {
				ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
				int columns = resultSetMetaData.getColumnCount();
				for (int i = 1; i <= columns; ++i) {
					resultMap.put(resultSetMetaData.getColumnName(i),
							resultSet.getObject(i));
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
