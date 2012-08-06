package com.rackspace.idm.domain.entity;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class UserCredential {
    @NotNull
    @Valid
    private Password password = new Password();

    private String secretQuestion = null;

    private String secretAnswer = null;

    /**
     * Discourage the use of the default constructor outside User
     */
    UserCredential() {

    }

    public UserCredential(Password password, String secretQuestion,
        String secretAnswer) {
        this.password = password;
        this.secretQuestion = secretQuestion;
        this.secretAnswer = secretAnswer;
    }

    public Password getPassword() {
        return password;
    }

    /**
     * Discourage the use of the setter outside User
     * 
     * @param password
     */
    public void setPassword(Password password) {
        this.password = password;
    }

    public String getSecretQuestion() {
        return secretQuestion;
    }

    /**
     * Discourage the use of the setter outside User
     * 
     * @param secretQuestion
     */
    public void setSecretQuestion(String secretQuestion) {
        this.secretQuestion = secretQuestion;
    }

    public String getSecretAnswer() {
        return secretAnswer;
    }

    /**
     * Discourage the use of the setter outside User
     * 
     * @param secretAnswer
     */
    public void setSecretAnswer(String secretAnswer) {
        this.secretAnswer = secretAnswer;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((password == null) ? 0 : password.hashCode());
        result = prime * result
            + ((secretAnswer == null) ? 0 : secretAnswer.hashCode());
        result = prime * result
            + ((secretQuestion == null) ? 0 : secretQuestion.hashCode());
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
        UserCredential other = (UserCredential) obj;
        if (password == null) {
            if (other.password != null) {
                return false;
            }
        } else if (!password.equals(other.password)) {
            return false;
        }
        if (secretAnswer == null) {
            if (other.secretAnswer != null) {
                return false;
            }
        } else if (!secretAnswer.equals(other.secretAnswer)) {
            return false;
        }
        if (secretQuestion == null) {
            if (other.secretQuestion != null) {
                return false;
            }
        } else if (!secretQuestion.equals(other.secretQuestion)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UserCredential [password=" + password + ", secretAnswer="
            + secretAnswer + ", secretQuestion=" + secretQuestion + "]";
    }
}
