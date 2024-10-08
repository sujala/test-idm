package com.rackspace.idm.domain.entity;

import com.rackspace.idm.util.HashHelper;
import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;
import org.apache.commons.lang.StringUtils;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Date;
import java.util.Stack;
import java.security.SecureRandom;

/**
 * Let's keep this immutable.
 *
 */
@XmlRootElement
public final class Password {

    public static final String VALID_NON_ALPHA_CHARS = "!@#$%^+=?:";
    public static final int BASE = 10;
    public static final int MAX_ALPHABET_LENGTH = 26;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String value;
    private boolean isNew;
    private Date lastUpdated = new Date();
    private boolean wasSelfUpdated;

    public Password() {
        // Needed by JAX-RS
    }

    public Password(String value, boolean isNew, Date lastUpdated, boolean wasSelfUpdated) {
        this.value = value;
        this.isNew = isNew;
        this.lastUpdated = lastUpdated;
        this.wasSelfUpdated = wasSelfUpdated;
    }

    @XmlTransient
    public boolean isNew() {
        return isNew;
    }

    public String getValue() {
        return value;
    }

    /**
     * Don't use this method.
     */
    public void setValue(String value) {
    	this.value = value;
//        throw new UnsupportedOperationException(
//            "Do not use this method. It's there to comply with the JavaBean spec.");
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public boolean wasSelfUpdated() {
        return wasSelfUpdated;
    }

    /**
     * @return A new Password instance, with the "new" status set to false.
     */
    public Password toExisting() {
        return new Password(value, false, lastUpdated, wasSelfUpdated);
    }

    public static Password existingInstance(String existingPassword, Date lasteUpdated,
        boolean wasSelfUpdated) {
        if (StringUtils.isBlank(existingPassword)) {
            throw new IllegalArgumentException("Null or blank password was passed in");
        }

        if (lasteUpdated == null) {
            throw new IllegalArgumentException("Null date entered.");
        }

        return new Password(existingPassword, false, lasteUpdated, wasSelfUpdated);
    }

    public static Password newInstance(String newPassword) {
        if (StringUtils.isBlank(newPassword)) {
            throw new IllegalArgumentException("Null or blank password was passed in.");
        }
        return new Password(newPassword, true, new Date(), false);
    }

    public static Password generateRandom(boolean isSelfUpdate, User user) {
        String randomPassword = HashHelper.getRandomSha1();
        randomPassword = randomPassword.substring(0, BASE);
        char[] pw = randomPassword.toCharArray();
        SecureRandom secureRandomNumber = HashHelper.getSecureRandom();

        Stack<Integer> usedIndexes = new Stack<Integer>();
        int randomIndex = (int) (secureRandomNumber.nextDouble() * randomPassword.length());
        usedIndexes.push(randomIndex);

        // insert random number
        char randomNumeric = (char) ('0' + (int) (secureRandomNumber.nextDouble() * BASE));
        pw[randomIndex] = randomNumeric;

        // insert random upper case letter
        while (usedIndexes.contains(randomIndex)) {
            randomIndex = (int) (secureRandomNumber.nextDouble() * randomPassword.length());
        }
        char randomUpper = (char) ('A' + (int) (secureRandomNumber.nextDouble() * MAX_ALPHABET_LENGTH));
        pw[randomIndex] = randomUpper;
        usedIndexes.push(randomIndex);

        // insert random lower case letter
        while (usedIndexes.contains(randomIndex)) {
            randomIndex = (int) (secureRandomNumber.nextDouble() * randomPassword.length());
        }
        char randomLower = (char) ('a' + (int) (secureRandomNumber.nextDouble() * MAX_ALPHABET_LENGTH));
        pw[randomIndex] = randomLower;
        usedIndexes.push(randomIndex);

        // insert random non-alphanumeric
        while (usedIndexes.contains(randomIndex)) {
            randomIndex = (int) (secureRandomNumber.nextDouble() * randomPassword.length());
        }
        int randomInt = (int) (secureRandomNumber.nextDouble() * VALID_NON_ALPHA_CHARS.length());
        int randomNonAlpha = VALID_NON_ALPHA_CHARS.charAt(randomInt);
        pw[randomIndex] = (char) randomNonAlpha;

        randomPassword = new String(pw);

        user.setUserPassword(randomPassword);
        user.setPassword(randomPassword);
        user.setPasswordIsNew(true);
        user.setPasswordLastUpdated(new Date());
        user.setPasswordWasSelfUpdated(isSelfUpdate);

        return new Password(randomPassword, true, new Date(), isSelfUpdate);
    }

    @Override
    public String toString() {
        return "Password [******]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        final int newPrime = 1231;
        final int oldPrime = 1237;
        int result = 1;
        result = prime * result + (isNew ? newPrime : oldPrime);
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Password other = (Password) obj;
        // If the password has been saved, it has been hashed. Thus the values
        // between pre-save and post-save passwords cannot be compared.
        if (isNew != other.isNew) {
            return false;
        } else {
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
        }
        return true;
    }
}
