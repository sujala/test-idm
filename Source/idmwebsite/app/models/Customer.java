package models;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import play.db.jpa.Model;

public class Customer extends Model {

    String customerNumber;
    String name;
    CustomerType type;

    public Customer() { }

    public Customer(String name, CustomerType type) {

        this.name = name;
        this.type = type;
    }

    public static Customer newInstance(String xml) {
        Customer customer = null;
        
        try {
            customer = new Customer();

            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setNamespaceAware(true);
            DocumentBuilder builder = domFactory.newDocumentBuilder();
            StringReader xmlSource = new StringReader(xml);
            Document xmlDoc = builder.parse(new InputSource(xmlSource));
            Node rootNode = xmlDoc.getFirstChild();
            
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();

            XPathExpression customerNumberExpr = xpath.compile("//customer-identifier/number/text()");
            Node customerNumberNode = (Node) customerNumberExpr.evaluate(rootNode, XPathConstants.NODE);
            String customerNumber = customerNumberNode != null ? customerNumberNode.getNodeValue() : StringUtils.EMPTY;
            customer.setCustomerNumber(customerNumber);
            
            XPathExpression nameExpr = xpath.compile("//name/text()");
            Node nameNode = (Node) nameExpr.evaluate(xmlDoc, XPathConstants.NODE);
            String name = nameNode != null ? nameNode.getNodeValue() : StringUtils.EMPTY;
            customer.setName(name);

            XPathExpression typeExpr = xpath.compile("//customer-type/text()");
            Node typeNode = (Node) typeExpr.evaluate(xmlDoc, XPathConstants.NODE);
            String custType = typeNode != null ? typeNode.getNodeValue() : StringUtils.EMPTY;
            customer.setCustomerType(
                    StringUtils.isNotEmpty(custType) ?
                        CustomerType.valueOf(custType) :
                        CustomerType.ORGANIZATION);

            return customer;
        }
        catch(Exception ex) {
            customer = null;
        }

        return customer;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setCustomerType(CustomerType type) {
        this.type = type;
    }

    public CustomerType getCustomerType() {
        return type;
    }

    public String getXml() {
        
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<ns2:customer xmlns:ns2=\"http://rackspace.com/foundation/service/customer\">");

        if (StringUtils.isNotEmpty(customerNumber)) {
            sb.append("<customer-identifier>");
            sb.append(String.format("<number>%s</number>", customerNumber));
            sb.append("</customer-identifier>");
        }
        
        sb.append(String.format("<name>%s</name>", name));
        sb.append(String.format("<customer-type>%s</customer-type>", type));

        sb.append("</ns2:customer>");
                
        return sb.toString();
    }
}
