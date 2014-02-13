package com.vigneshjayavel.api.pojo;

public class LoginStatus {

	String accountid;
	String homeSite;
	boolean success;
	String ticket;
	String creatorIpAddress;
	String protocol;
	String currentTime;
	String lastLoginMethod;
	String lastLoginTime;
	boolean changepasswordonnextlogin;
	public String getAccountid() {
		return accountid;
	}
	public void setAccountid(String accountid) {
		this.accountid = accountid;
	}
	public String getHomeSite() {
		return homeSite;
	}
	public void setHomeSite(String homeSite) {
		this.homeSite = homeSite;
	}
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
	public String getTicket() {
		return ticket;
	}
	public void setTicket(String ticket) {
		this.ticket = ticket;
	}
	public String getCreatorIpAddress() {
		return creatorIpAddress;
	}
	public void setCreatorIpAddress(String creatorIpAddress) {
		this.creatorIpAddress = creatorIpAddress;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public String getCurrentTime() {
		return currentTime;
	}
	public void setCurrentTime(String currentTime) {
		this.currentTime = currentTime;
	}
	public String getLastLoginMethod() {
		return lastLoginMethod;
	}
	public void setLastLoginMethod(String lastLoginMethod) {
		this.lastLoginMethod = lastLoginMethod;
	}
	public String getLastLoginTime() {
		return lastLoginTime;
	}
	public void setLastLoginTime(String lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}
	public boolean isChangepasswordonnextlogin() {
		return changepasswordonnextlogin;
	}
	public void setChangepasswordonnextlogin(boolean changepasswordonnextlogin) {
		this.changepasswordonnextlogin = changepasswordonnextlogin;
	}

	
}
