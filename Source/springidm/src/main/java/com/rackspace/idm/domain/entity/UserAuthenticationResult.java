package com.rackspace.idm.domain.entity;

import org.joda.time.DateTime;

public class UserAuthenticationResult extends AuthenticationResult {

    private BaseUser user;
    
    private DateTime passwordExpirationDate;

    public UserAuthenticationResult(BaseUser user, boolean authenticated) {
        super(authenticated);
        this.user = user;
    }
    
    public BaseUser getUser() {
        return user;
    }
    
    public DateTime getPasswordExpirationDate() {
        passwordExpirationDate = calculatePasswordExpirationDate();
        return passwordExpirationDate;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UserAuthenticationResult other = (UserAuthenticationResult) obj;
        if (user == null) {
            if (other.user != null) {
                return false;
            }
        } else if (!user.equals(other.user)) {
            return false;
        }
        return true;
    }
    
    private DateTime calculatePasswordExpirationDate() {
        int futureDayWhenPwdExpires = user.getPasswordRotationDuration();
        DateTime lastPasswordChange = user.getLastPasswordUpdateTimeStamp();
        
        if (lastPasswordChange == null) {
            return null;
        }
        
        int monthsOfDayInYear [] = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        
        int yearOfLastUpdate = lastPasswordChange.getYear();
        int dayOfLastUpdate = lastPasswordChange.getDayOfYear();
        
        futureDayWhenPwdExpires += dayOfLastUpdate;
       
        int yearOfPasswordExpiration = yearOfLastUpdate;
        int month = -1;
        
        while (futureDayWhenPwdExpires >= 366) {
            if (isLeap(yearOfPasswordExpiration)) {
                futureDayWhenPwdExpires -= 366;
            }
            else {
                futureDayWhenPwdExpires -= 365;        
            }            
            yearOfPasswordExpiration++;
        }
        
        while (futureDayWhenPwdExpires >= 30) {
            
            month++;
            if (month == 1) {
                if (isLeap(yearOfPasswordExpiration) ) {
                    futureDayWhenPwdExpires -= 29;
                    continue;
                }
            }
            
            futureDayWhenPwdExpires -= monthsOfDayInYear[month];
        }
        
        
        DateTime passwordExpiryDate = new DateTime(yearOfPasswordExpiration, month + 2, futureDayWhenPwdExpires,0,0,0,0);
        return passwordExpiryDate;
    }
    
    private boolean isLeap(int year) {
        return year % 4 == 0;
    }
}
