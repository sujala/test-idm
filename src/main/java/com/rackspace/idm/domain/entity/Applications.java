package com.rackspace.idm.domain.entity;

import java.util.List;

public class Applications {
    private int totalRecords;
    private int offset;
    private int limit;
    private List<Application> clients;
    
    public int getTotalRecords() {
        return totalRecords;
    }
    
    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }
    
    public int getOffset() {
        return offset;
    }
    
    public void setOffset(int offset) {
        this.offset = offset;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public List<Application> getClients() {
        return clients;
    }
    
    public void setClients(List<Application> clients) {
        this.clients = clients;
    }
}
