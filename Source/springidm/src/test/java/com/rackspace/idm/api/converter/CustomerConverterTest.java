package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.IdentityProfile;
import com.rackspace.idm.domain.entity.Customer;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/11/12
 * Time: 1:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class CustomerConverterTest {

    CustomerConverter customerConverter;
    Customer customerDO;
    IdentityProfile jaxbCustomer;

    @Before
    public void setUp() throws Exception {
        customerConverter = new CustomerConverter();
        customerDO = new Customer();
        jaxbCustomer = new IdentityProfile();
    }

    @Test
    public void toCustomerDO_withJaxbCustomer_setsId() throws Exception {
        jaxbCustomer.setId("Id");
        Customer customer = customerConverter.toCustomerDO(jaxbCustomer);
        assertThat("id", customer.getId(), equalTo("Id"));
    }

    @Test
    public void toCustomerDO_withJaxbCustomer_setsRCN() throws Exception {
        jaxbCustomer.setCustomerId("customerId");
        Customer customer = customerConverter.toCustomerDO(jaxbCustomer);
        assertThat("customer id", customer.getRcn(), equalTo("customerId"));
    }

    @Test
    public void toCustomerDO_withJaxbCustomer_setsEnabled() throws Exception {
        jaxbCustomer.setEnabled(true);
        Customer customer = customerConverter.toCustomerDO(jaxbCustomer);
        assertThat("enabled", customer.isEnabled(), equalTo(true));
    }

    @Test
    public void toCustomerDO_withJaxbCustomerWithNoEnabled_setsNullEnabled() throws Exception {
        Customer customer = customerConverter.toCustomerDO(jaxbCustomer);
        assertThat("enabled", customer.isEnabled(), nullValue());
    }

    @Test
    public void toCustomerDO_withCustomerDo_setsId() throws Exception {
        customerDO.setId("Id");
        JAXBElement<IdentityProfile> customer = customerConverter.toJaxbCustomer(customerDO);
        assertThat("id", customer.getValue().getId(), equalTo("Id"));
    }

    @Test
    public void toCustomerDO_withCustomerDo_setsRCN() throws Exception {
        customerDO.setRcn("customerId");
        JAXBElement<IdentityProfile> customer = customerConverter.toJaxbCustomer(customerDO);
        assertThat("customer id", customer.getValue().getCustomerId(), equalTo("customerId"));
    }

    @Test
    public void toCustomerDO_withCustomerDo_setsEnabled() throws Exception {
        customerDO.setEnabled(true);
        JAXBElement<IdentityProfile> customer = customerConverter.toJaxbCustomer(customerDO);
        assertThat("enabled", customer.getValue().isEnabled(), equalTo(true));
    }


}
