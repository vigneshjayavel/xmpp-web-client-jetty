package com.vigneshjayavel.api.pojo;

public class UserDetails {
	private String userName;
	private String emailId;
	private String orgName;
	private String ticket;
	private String status;
	private String xmppAuthStatus = "existinguser";
	public String getXmppAuthStatus() {
		return xmppAuthStatus;
	}
	public void setXmppAuthStatus(String xmppAuthStatus) {
		this.xmppAuthStatus = xmppAuthStatus;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getEmailId() {
		return emailId;
	}
	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}
	public String getOrgName() {
		return orgName;
	}
	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}
	public String getTicket() {
		return ticket;
	}
	public void setTicket(String ticket) {
		this.ticket = ticket;
	}
}