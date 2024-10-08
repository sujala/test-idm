package com.rackspace.idm.domain.entity;

import junit.framework.Assert;

import org.junit.Test;

import com.rackspace.idm.domain.entity.Password;
import com.rackspace.idm.domain.entity.UserCredential;

public class UserCredentialsTests {

    private String password = "Password";
    private String question = "Question";
    private String answer = "Answer";
    private String questionId = "Id";
    
    private UserCredential getTestCredentials() {
        return new UserCredential(Password.newInstance(password), question, answer, questionId);
    }
    
    @Test
    public void shouldReturnToString() {
        UserCredential cred = getTestCredentials();
        
        Assert.assertEquals("UserCredential [password=Password [******], secretAnswer="
            + answer + ", secretQuestion=" + question + ", secretQuestionId=" + questionId + "]", cred.toString());
    }
    
    @Test
    public void shouldReturnHashCode() {
        UserCredential cred = getTestCredentials();
        
        Assert.assertEquals(-270192857, cred.hashCode());
        
        cred.setPassword(null);
        cred.setSecretAnswer(null);
        cred.setSecretQuestion(null);
        cred.setSecretQuestionId(null);
        
        Assert.assertEquals(923521, cred.hashCode());
    }
    
    @Test
    public void shouldReturnTrueForEquals() {
        UserCredential cred1 = getTestCredentials();
        UserCredential cred2 = getTestCredentials();
        
        Assert.assertTrue(cred1.equals(cred1));
        Assert.assertTrue(cred1.equals(cred2));
        
        cred1.setPassword(null);
        cred1.setSecretAnswer(null);
        cred1.setSecretQuestion(null);
        cred1.setSecretQuestionId(null);
        
        cred2.setPassword(null);
        cred2.setSecretAnswer(null);
        cred2.setSecretQuestion(null);
        cred2.setSecretQuestionId(null);
        
        Assert.assertTrue(cred1.equals(cred2));
    }
    
    @Test
    public void shouldReturnFalseForEquals() {
        UserCredential cred1 = getTestCredentials();
        UserCredential cred2 = getTestCredentials();
        
        Assert.assertFalse(cred1.equals(null));
        Assert.assertFalse(cred1.equals(1));
        
        cred2.setSecretQuestion("NewQuestion");
        Assert.assertFalse(cred1.equals(cred2));
        cred2.setSecretQuestion(null);
        Assert.assertFalse(cred2.equals(cred1));
        cred2.setSecretQuestion(cred1.getSecretQuestion());
        
        cred2.setSecretAnswer("NewAnswer");
        Assert.assertFalse(cred1.equals(cred2));
        cred2.setSecretAnswer(null);
        Assert.assertFalse(cred2.equals(cred1));
        cred2.setSecretAnswer(cred1.getSecretAnswer());
        
        cred2.setPassword(Password.newInstance("NewPassword"));
        Assert.assertFalse(cred1.equals(cred2));
        cred2.setPassword(null);
        Assert.assertFalse(cred2.equals(cred1));
    }
}
