package com.rackspace.idm.domain.entity;


public class PasswordCredentials {

    protected Password newPassword;
    protected Password currentPassword;
    protected Boolean verifyCurrentPassword;

    public Password getNewPassword() {
		return newPassword;
	}
    
	public void setNewPassword(Password newPassword) {
		this.newPassword = newPassword;
	}
	
	public Password getCurrentPassword() {
		return currentPassword;
	}
	
	public void setCurrentPassword(Password currentPassword) {
		this.currentPassword = currentPassword;
	}
	
	public Boolean isVerifyCurrentPassword() {
		return verifyCurrentPassword;
	}
	
	public void setVerifyCurrentPassword(Boolean verifyCurrentPassword) {
		this.verifyCurrentPassword = verifyCurrentPassword;
	}
}
