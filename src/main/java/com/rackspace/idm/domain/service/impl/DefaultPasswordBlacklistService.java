package com.rackspace.idm.domain.service.impl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.newrelic.api.agent.Trace;
import com.rackspace.idm.domain.config.DynamoDBClientConfiguration;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.service.PasswordBlacklistService;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DefaultPasswordBlacklistService implements PasswordBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPasswordBlacklistService.class);

    @Autowired
    AmazonDynamoDB dynamoDB;

    @Autowired
    IdentityConfig identityConfig;

    @Trace
    @Override
    public boolean isPasswordInBlacklist(String password) {
        try {
            String passwordHash = DigestUtils.sha1Hex(password).toUpperCase();
            int thresholdCount = identityConfig.getReloadableConfig().getDynamoDBPasswordBlacklistCountMaxAllowed();

            Map<String, AttributeValue> key = new HashMap<>();
            key.put(DynamoDBClientConfiguration.PASSWORD_BLACKLIST_PASWORDHASH_COLUMN_NAME, new AttributeValue(passwordHash));
            GetItemRequest request = new GetItemRequest()
                    .withKey(key)
                    .withTableName(DynamoDBClientConfiguration.PASSWORD_BLACKLIST_TABLE_NAME);


            Map<String, AttributeValue> returnedItem = dynamoDB.getItem(request).getItem();
            if (returnedItem != null) {
                logger.debug("Password found in password blacklist in dynamoDb");
                String countString = returnedItem.get(DynamoDBClientConfiguration.PASSWORD_BLACKLIST_COUNT_COLUMN_NAME).getN();
                int pwdCount = Integer.parseInt(countString);
                if (pwdCount > thresholdCount) {
                    logger.debug("Password has been publicly compromised more then password threshold limit which set to " + thresholdCount);
                    return true;
                }
            }
        } catch (AmazonServiceException ase) {
            logger.error("Could not complete operation" +
                    System.lineSeparator() + "Error Message:  " + ase.getMessage() +
                    System.lineSeparator() + "HTTP Status:    " + ase.getStatusCode() +
                    System.lineSeparator() + "AWS Error Code: " + ase.getErrorCode() +
                    System.lineSeparator() + "Error Type:     " + ase.getErrorType() +
                    System.lineSeparator() + "Request ID:     " + ase.getRequestId());

        } catch (AmazonClientException ace) {
            logger.error("Internal error occurred communicating with DynamoDB");
            logger.error("Error Message:  " + ace.getMessage());
        } catch (Exception e) {
            logger.error("Unable to check the password blacklist for user password due to error.", e);
        }

        return false;
    }
}
