package com.rackspace.idm.domain.entity;

import lombok.Data;
import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Data
public class AuthData {

    /**
    * @deprecated
    * using scope access to retain token info
    */
    @Deprecated
    private String accessToken;

    /**
     * @deprecated
     * using scope access to retain token info
     */
    @Deprecated
    private String refreshToken;

    /**
     * @deprecated
     * using scope access to retain token info
     */
    @Deprecated
    private Date accessTokenExpiration;

    private boolean isPasswordResetOnlyToken;
    private EndUser user;
    private Application application;
    private DateTime passwordExpirationDate;
    private Racker racker;
    private ScopeAccess token;
    private List<OpenstackEndpoint> endpoints;

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
