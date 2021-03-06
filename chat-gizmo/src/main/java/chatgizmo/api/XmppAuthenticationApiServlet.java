package chatgizmo.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

import chatgizmo.api.pojo.UserDetails;

public class XmppAuthenticationApiServlet  extends HttpServlet {

	private static final long serialVersionUID = -6154475799000019579L;
	private static ObjectMapper mapper;
	private static XMPPConnection xmppConnection;
	private static Logger logger = Logger.getLogger(XmppAuthenticationApiServlet.class.getName());
	
	public XmppAuthenticationApiServlet(){
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
			// Login to XMPP server
			xmppConnection = new XMPPConnection(config);
			xmppConnection.connect();
			mapper = new ObjectMapper();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		logger.setLevel(Level.ALL);
	}
	
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String userName = request.getParameter("userName");
		String orgName = request.getParameter("orgName");
		String password = userName+orgName;
		System.out.println("userName : " + userName +"; orgName : "+orgName);
		UserDetails userDetails = authenticate(userName, password, orgName);
		disconnectXmpp();
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().println(mapper.writeValueAsString(userDetails));
	}
	
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException{
		this.doGet(request, response);
	}
	
	/* 
	 * // get username,orgname
	 * // then check whether the user is present in openfire's db 
	 * // if the user is present in the openfire's db
	 * 		// login into the openfire server and an xmpp session is created. 
	 * // else if the user is not present in the openfire's db 
	 * 		// create a record for the user in the openfire db 
	 * 		// login into the openfire server and an xmpp session is created. 
	 * 		// the session is closed and a response is returned with the xmpp username and success param 
	 */

	public UserDetails authenticate(String userName, String password, String orgName) {
		UserDetails userDetails = new UserDetails();
		if (!isUserRegisteredInOpenFire(userName)) {
			// create the user.
			// he is a firsttime user of the xmpp chat system.
			addNewUserToOpenFire(userName, password, null, orgName);
			userDetails.setXmppAuthStatus("newuser");
		}
		else{
			userDetails.setXmppAuthStatus("existinguser");
		}
		//now the user must be a registered xmpp user
		//we can set the user details
		userDetails.setUserName(userName);
		userDetails.setOrgName(orgName);
		userDetails.setPassword(password);
		userDetails.setStatus("200");
		return userDetails;
	}
	
	public boolean isUserRegisteredInOpenFire(String userName) {
		// code for checking whether the username is present in the openfire
		// database
		// this does involve api call to the openfire server to retrieve the
		// user's registration status
		try {
			if(!xmppConnection.isConnected()){
				connectXmpp();
			}
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
			System.out.println(e.getMessage());
		}
		return false;
	}

	public void addNewUserToOpenFire(String userName, String password,
			String emailId, String orgName) {
		// register newuser
		try{
			Map<String, String> userAttributes = new HashMap<String, String>();
			userAttributes.put("user", userName);
			userAttributes.put("email", emailId);
			userAttributes.put("group", orgName);
			if(!xmppConnection.isConnected()){
				connectXmpp();
			}
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
			if(!xmppConnection.isConnected()){
				connectXmpp();
			}
			xmppConnection.login(userName, password);
			logger.fine(xmppConnection.getUser());
			Roster roster = xmppConnection.getRoster();
			List<String> jids = getAllUsersInOrganization(null);
			logger.info("Jids that are present in the organization " + jids.toArray().length);
			//update the roster entry with all users in the openfire chat system irrespective of the user's group
			addRosterEntriesForUser(roster,jids,userName,null);
			listRosterEntriesForUser(roster);
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}
	
	public List<String> getAllUsersInOrganization(String orgName){
		List<String> resultJids = new ArrayList<String>();
		try {
			if(!xmppConnection.isConnected()){
				connectXmpp();
			}
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
	
	public void addRosterEntriesForUser(Roster roster, List<String> jids, String userName, String orgName) {
		try{
			for(String jid : jids){
				String friend = jid.substring(0, jid.indexOf("@"));
				if(!userName.equals(friend)){
					roster.createEntry(jid, friend, null);
				}
			}
		}
		catch(XMPPException e){
			e.printStackTrace();
		}
	}
	
	private void connectXmpp(){
		try {
			xmppConnection.connect();
		} catch (XMPPException e) {
			e.printStackTrace();
		}
	}
	
	private void disconnectXmpp(){
		if(xmppConnection.isConnected()){
			xmppConnection.disconnect();
		}
	}

}
