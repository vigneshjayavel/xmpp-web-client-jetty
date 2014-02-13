package com.vigneshjayavel.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.map.ObjectMapper;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.ReportedData;
import org.jivesoftware.smackx.search.UserSearchManager;

import com.vigneshjayavel.api.pojo.LoginStatus;

public class XmppAuthenticationServlet {

	/* 
	 * // get username,orgname, password 
	 * // call tcc login api and parse the result 
	 * // if the result is success, 
	 * 			// then check whether the user is present in openfire's db 
	 * 			// if the user is present in the openfire's db
	 * 				// login into the openfire server and an xmpp session is created. 
	 * 			// else if the user is not present in the openfire's db 
	 * 				// create a record for the user in the openfire db 
	 * 				// login into the openfire server and an xmpp session is created. 
	 * // else if the result is failure 
	 * 			// throw authentication failure exception
	 */
	private static ObjectMapper mapper;
	private static XMPPConnection xmppConnection;
	private static Logger logger = Logger.getLogger(XmppAuthenticationServlet.class.getName());
	public XmppAuthenticationServlet(){
		// load xmpp server props
		Properties properties = new Properties();
		try {
			properties.load(this.getClass().getResourceAsStream(
					"/server.properties"));

			// Set XMPP connection
			SmackConfiguration.setPacketReplyTimeout(5000);
			ConnectionConfiguration config = new ConnectionConfiguration(
					properties.getProperty("xmpphost"), 5222, "localhost");
			config.setSASLAuthenticationEnabled(false);

			// Log in to XMPP server
			xmppConnection = new XMPPConnection(config);
			xmppConnection.connect();
			mapper = new ObjectMapper();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		logger.setLevel(Level.ALL);
	}

	public boolean isAuthenticTccUser(String userName, String password,
			String orgName) {
		String urlFormat = "http://localhost/tcc/login?username=%s&password=%s&orgname=%s";
		String url = String.format(urlFormat, "tccadmin", "tcc4ever!", "tcc");
		LoginStatus loginStatus = null;
		String response = Util.sendGetRequest(url);

		try {
			loginStatus = mapper.readValue(response, LoginStatus.class);
			Util.prettyPrint(loginStatus);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return loginStatus.isSuccess();

	}

	public boolean isUserRegisteredInOpenFire(XMPPConnection xmppConnection, String userName) {
		// code for checking whether the username is present in the openfire
		// database
		// this does involve api call to the openfire server to retrieve the
		// user's registration status
		try {
			UserSearchManager search = new UserSearchManager(xmppConnection);
			String searchService = "search.localhost";
			Form searchForm = search.getSearchForm(searchService);
			Form answerForm = searchForm.createAnswerForm();
			answerForm.setAnswer("search", userName);
			answerForm.setAnswer("Username", true);
			ReportedData data = search.getSearchResults(answerForm,searchService);
			Iterator<ReportedData.Row> rows = data.getRows();
			if(rows.hasNext()){
				return true;
			}
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void authenticate(String userName, String password, String orgName) {
		if (isAuthenticTccUser(userName, password, orgName)) {
			if (isUserRegisteredInOpenFire(xmppConnection, userName)) {
				// then just login using the smack api
				// the user is in no way prompted for the username and password
				// here
			} else {
				// create the user.
				// he is a firsttime user of the system.
			}
		}
	}

	public void addNewUserToOpenFire(String userName, String password,
			String emailId, String orgId) {
		// register newuser
		try{
			Map<String, String> userAttributes = new HashMap<String, String>();
			userAttributes.put("user", userName);
			userAttributes.put("email", emailId);
			userAttributes.put("group", orgId);
			AccountManager accountManager = new AccountManager(xmppConnection);
			accountManager.createAccount(userName, password, userAttributes);
			logger.info("added newuser: " + userName);
			
		} catch (XMPPException e) {
			if(e.getXMPPError().toString().contains("409")){
				logger.severe("The user is already registered!");
//				return;
			}
			else{
				logger.severe("Unknown registration error has occured!");
				logger.severe(e.getMessage());
			}
		}
		//Update the roster for the existing user or newly created user.
		try {
			xmppConnection.login(userName, password);
			logger.fine(xmppConnection.getUser());
			Roster roster = xmppConnection.getRoster();
			List<String> jids = getAllUsersInOrganization(xmppConnection,null);
			logger.info("Jids that are present in the organization " + jids.toArray().length);
			//update the roster entry with all users in the openfire chat system irrespective of the user's group
			addRosterEntriesForUser(roster,jids,null);
			listRosterEntriesForUser(roster);
			xmppConnection.disconnect();
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}
	

	
	public List<String> getAllUsersInOrganization(XMPPConnection xmppConnection, String orgName){
		List<String> resultJids = new ArrayList<String>();
		try {
			UserSearchManager search = new UserSearchManager(xmppConnection);
			String searchService = "search."+xmppConnection.getServiceName();
			Form searchForm = search.getSearchForm(searchService);
			Form answerForm = searchForm.createAnswerForm();
			answerForm.setAnswer("search", "*");
			answerForm.setAnswer("Username", true);
			ReportedData data = search.getSearchResults(answerForm,searchService);
			Iterator<ReportedData.Row> rows = data.getRows();
			while (rows.hasNext()) {
                ReportedData.Row row = rows.next();
                Iterator<String> jids = row.getValues("jid");
                while (jids.hasNext()) {
                   resultJids.add(jids.next());
                }
            }
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		return resultJids;
	}
	
	public void listRosterEntriesForUser(Roster roster){
		Collection<RosterEntry> rosterEntries = roster.getEntries();
		if(rosterEntries.size()>0){
			logger.info("Listing all roster entries.. Total : "+rosterEntries.size());
			for(RosterEntry entry : rosterEntries){
				System.out.println(entry.toString());
			}
		}
	}
	
	public void addRosterEntriesForUser(Roster roster, List<String> jids, String orgName) {
		try{
			for(String jid : jids){
				roster.createEntry(jid, jid.substring(0, jid.indexOf("@")), null);
			}
		}
		catch(XMPPException e){
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new XmppAuthenticationServlet().addNewUserToOpenFire("faa", "aaa", "aaa@aaa.com", "Some sample organization");
	}

}
