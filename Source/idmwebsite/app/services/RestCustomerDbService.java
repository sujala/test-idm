package services;

import configuration.CustomerDbServiceConfiguration;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Resource;
import models.Customer;
import models.Person;
import models.PersonRole;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

@Resource
public class RestCustomerDbService implements CustomerDbService {

    private CustomerDbServiceConfiguration config;
    private String serverRequestType;
    private String customerDbUrl;
    private String customerResourceUri;
    private String personResourceUri;

    public RestCustomerDbService() {

        config = new CustomerDbServiceConfiguration();

        serverRequestType = config.getServerRequestType();
        customerDbUrl = config.getServerAddress();
        customerResourceUri = config.getCustomerResourceUri();
        personResourceUri = config.getPersonResourceUri();
    }

    public Customer addCustomer(Customer customer) {

        Map headers = new HashMap();

        String customerNumber = "";

        try {

            String data = customer.getXml();
            String url = customerDbUrl + customerResourceUri;

            HttpClientService httpClient = new HttpClientService();
            HttpResponse response = httpClient.sendPost(url, serverRequestType, headers, data);

            Header[] locationHeaders = response.getHeaders("Location");
            if (locationHeaders.length > 0) {
                String location = locationHeaders[0].getValue();
                customerNumber = location.substring(location.lastIndexOf("/") + 1);
            }
        }
        catch (Exception ex)
        {
            customerNumber = "";
        }
        
        customer.setCustomerNumber(customerNumber);
        
        return customer;
    }

    public Customer getCustomer(String customerNumber) {

        Map headers = new HashMap();
        HttpClientService httpClient = new HttpClientService();

        Customer customer = null;
        try {
            String url = customerDbUrl + customerResourceUri;
            HttpResponse response = httpClient.sendGet(
                    url + "/" + customerNumber, headers);
            String responseXml = httpClient.getResponseBody(response);
            customer = Customer.newInstance(responseXml);
        }
        catch (Exception ex) {
            customer = null;
        }
        return customer;
    }

    public Person addPerson(Person person) {

        Map headers = new HashMap();

        String personNumber = "";
        try {

            String data = person.getXml();

            HttpClientService httpClient = new HttpClientService();
            String url = customerDbUrl + personResourceUri;
            HttpResponse response = httpClient.sendPost(url, serverRequestType, headers, data);

            Header[] locationHeaders = response.getHeaders("Location");
            if (locationHeaders.length > 0) {
                String location = locationHeaders[0].getValue();
                personNumber = location.substring(location.lastIndexOf("/") + 1);
            }
        }
        catch (Exception ex)
        {
            personNumber = "";
        }
        person.setPersonNumber(personNumber);

        return person;
    }

    public Person getPerson(String personNumber) {
        Map headers = new HashMap();

        Person person;
        try {
            String url = customerDbUrl + personResourceUri;
            HttpClientService httpClient = new HttpClientService();
            HttpResponse response = httpClient.sendGet(
                    url + "/" + personNumber, headers);
            String responseXml = httpClient.getResponseBody(response);
            person = Person.newInstance(responseXml);
        }
        catch (Exception ex)
        {
            person = null;
        }

        return person;
    }

    public boolean updatePersonRole(Person person, PersonRole role) {

        boolean success = false;
        Map headers = new HashMap();

        String customerNumber = person.getCustomer().getCustomerNumber();
        String personNumber = person.getPersonNumber();

        String url = String.format("%s/%s/persons/%s/roles/%s",
                customerDbUrl + customerResourceUri,
                customerNumber,
                personNumber,
                role);

        try {
            HttpClientService httpClient = new HttpClientService();
            HttpResponse response = httpClient.sendPut(url, serverRequestType, headers, "");
            success = true;
        }
        catch(Exception ex) {
            success = false;
        }

        return success;
    }
}
