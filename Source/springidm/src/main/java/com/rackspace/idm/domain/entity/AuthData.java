package com.rackspace.idm.domain.entity;

import java.util.Date;

import org.joda.time.DateTime;

public class AuthData {

	private String accessToken;
	private String refreshToken;
	private Date accessTokenExpiration;
	private User user;
	private Application application;
	private Racker racker;
	private boolean isPasswordResetOnlyToken;
	private DateTime passwordExpirationDate;
	private int daysUntilPasswordExpiration;
	
	public String getAccessToken() {
		return accessToken;
	}
	
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	
	public String getRefreshToken() {
		return refreshToken;
	}
	
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}
	
	public Date getAccessTokenExpiration() {
		return accessTokenExpiration;
	}
	
	public void setAccessTokenExpiration(Date accessTokenExpiration) {
		this.accessTokenExpiration = accessTokenExpiration;
	}
	
	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}
	
	public Application getApplication() {
		return application;
	}
	
	public void setApplication(Application application) {
		this.application = application;
	}
	
	public Racker getRacker() {
		return racker;
	}
	
	public void setRacker(Racker racker) {
		this.racker = racker;
	}
	
	public boolean isPasswordResetOnlyToken() {
		return isPasswordResetOnlyToken;
	}
	
	public void setPasswordResetOnlyToken(boolean isPasswordResetOnlyToken) {
		this.isPasswordResetOnlyToken = isPasswordResetOnlyToken;
	}
	
	public DateTime getPasswordExpirationDate() {
		return passwordExpirationDate;
	}
	
	public void setPasswordExpirationDate(DateTime passwordExpirationDate) {
		this.passwordExpirationDate = passwordExpirationDate;
	}
	
	public int getDaysUntilPasswordExpiration() {
		return daysUntilPasswordExpiration;
	}
	
	public void setDaysUntilPasswordExpiration(int daysUntilPasswordExpiration) {
		this.daysUntilPasswordExpiration = daysUntilPasswordExpiration;
	}
	
	public Integer getDaysToPasswordExpiry() {
		if (passwordExpirationDate != null) {
			DateTime today = new DateTime();
			int daysToPasswordExpiry = passwordExpirationDate.getDayOfYear() - today.getDayOfYear();
			if (daysToPasswordExpiry > 0) {
				return daysToPasswordExpiry;
			}
		}
		
		return null;
	}
}
