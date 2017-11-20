package com.rackspace.idm.event;

public interface IdmEvent {

    /**
     * The transactionId associated with the event
     *
     * @return
     */
    String getEventId();

    /**
     * Unique identifier of the API Node processing the request
     * @return
     */
    String getNodeName();

    /**
     * A classification of the event
     *
     * @return
     */
    String getEventType();
}
