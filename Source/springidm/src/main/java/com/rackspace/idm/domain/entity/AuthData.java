package com.rackspace.idm.domain.entity;

import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AuthData {

    private String accessToken;
    private String refreshToken;
    private Date accessTokenExpiration;
    private User user;
    private Application application;
    private Racker racker;
    private boolean isPasswordResetOnlyToken;
    private DateTime passwordExpirationDate;

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
        if (passwordExpirationDate != null) {
            Calendar date = Calendar.getInstance();
            long daysToPasswordExpiry = 0;
            while (date.before(passwordExpirationDate.toCalendar(Locale.getDefault()))) {
                date.add(Calendar.DAY_OF_MONTH, 1);
                daysToPasswordExpiry++;
            }
            if (daysToPasswordExpiry > 0) {
                return (int) daysToPasswordExpiry;
            }
        }

        return 0;
    }
}
