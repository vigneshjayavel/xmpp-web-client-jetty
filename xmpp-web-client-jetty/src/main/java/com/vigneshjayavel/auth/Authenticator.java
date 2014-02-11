package com.vigneshjayavel.auth;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.vigneshjayavel.auth.pojo.LoginStatus;

public class Authenticator {
	//get username,orgname, password
	//call tcc login api and parse the result
	//if the result is success, 
		//then check whether the user is present in openfire's db
		//if the user is present in the openfire's db
			//login into the openfire server and an xmpp session is created.
		//else if the user is not present in the openfire's db
			//create a record for the user in the openfire db 
			//login into the openfire server and an xmpp session is created.
	//else if the result is failure
		//throw authentication failure exception
	
	private static ObjectMapper mapper = new ObjectMapper();

	public boolean isAuthenticTccUser(String userName, String password, String orgName){
		String urlFormat="http://localhost/tcc/login?username=%s&password=%s&orgname=%s";
		String url = String.format(urlFormat, "tccadmin","tcc4ever!","tcc");
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
	
	public boolean isPresentInOpenFire(String userName){
		
		return false;
	}
	
	public void authenticate(String userName, String password, String orgName){
		if(isAuthenticTccUser(userName, password, orgName)){
			if(isPresentInOpenFire(userName)){
				
			}
			else{
				
			}
		}
	}
	
	public static void main(String[] args) {
		String urlFormat="http://localhost/tcc/login?username=%s&password=%s&orgname=%s";
		String url = String.format(urlFormat, "tccadmin","tcc4ever!","tcc");
		LoginStatus loginStatus = null;
		String response = Util.sendGetRequest(url);

		try {
			loginStatus = mapper.readValue(response, LoginStatus.class);
			Util.prettyPrint(loginStatus);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(loginStatus.getTicket());
	}
	
	
	
}
