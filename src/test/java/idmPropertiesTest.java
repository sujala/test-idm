import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static junit.framework.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 8/14/12
 * Time: 1:24 PM
 * To change this template use File | Settings | File Templates.
 */

//TODO: Rewrite this based on verification needs.
// This test is pretty fragile and I'm not certain it actually verifies what it's trying to verify. It does a union of all the properties in a set of idm.properties (some excluded) and then verifies that all the
//the idm.properties contain all the properties. However, the configs excluded are all the development ones; where new properties would be initially added. So it will only
//catch an error if someone remembered to add it to one of the included set after development, at which point the person would probably remember
//to add to all. Also, I'm not sure if the included set of config files are used during deploy anyway, so this may be verifying files that are not even used.
public class idmPropertiesTest {

    private ArrayList<String> idmPropertyLocationList = new ArrayList<String>();


    @Test
    public void checkingIfAllIdmPropertiesContainsSamePropertyNames() throws Exception{
        Set<String> propertyNameSetList = propertyNames();
        HashMap<String, ArrayList<String>> propertyHashMap = createPropertyHashMap(propertyNameSetList);
        String error = "";
        for (String fileName : propertyHashMap.keySet()) {
            ArrayList<String> propertyNameErrorList = propertyHashMap.get(fileName);
            propertyNameErrorList.remove("virtualPath");
            propertyNameErrorList.remove("atom.hopper.dataCenter");
            propertyNameErrorList.remove("cloudAuthUK11url");
            propertyNameErrorList.remove("cloudAuthUK20url");
            propertyNameErrorList.remove("migrationAdminGroup");
            if (!propertyNameErrorList.isEmpty()) {
                error += fileName + ":\n";
                error += "\t" + propertyNameErrorList.toString() + "\n\n";
            }
        }
        if (!error.equals("")) {
            Assert.fail("Missing property fields in these files.\n" + error);
        }
        assertTrue("All the idm properties config file contains same properties.", true);
    }

    private Set<String> propertyNames() throws IOException {
        Set<String> propertyNameSetList = new HashSet<String>();
        setIdmPropertyLocations(new File("src/main/config"));
        for (String idmProperty : idmPropertyLocationList) {
            propertyNameSetList.addAll(getProperties(idmProperty));
        }
        return  propertyNameSetList;
    }

    private HashMap<String, ArrayList<String>> createPropertyHashMap(Set<String> propertyNameSetList) throws Exception {
        HashMap<String, ArrayList<String>>  propertyHashMap = new HashMap<String, ArrayList<String>>();
        for (String idmPropertyLocationFile : idmPropertyLocationList) {
            Set<String> properties = getProperties(idmPropertyLocationFile);
            ArrayList<String> propertyErrorList = new ArrayList<String>();
            for (String property : propertyNameSetList) {
                if (!properties.contains(property)) {
                    propertyErrorList.add(property);
                }
            }
            propertyHashMap.put(idmPropertyLocationFile, propertyErrorList);
        }
        return  propertyHashMap;
    }

    private void setIdmPropertyLocations(File idmPropertyLocation) throws IOException{
        if (idmPropertyLocation.isDirectory() &&
                !idmPropertyLocation.getName().contains("LOCAL-DEV") &&
                !idmPropertyLocation.getName().contains("JENKINS") &&
                !idmPropertyLocation.getName().contains("TEST") &&
                !idmPropertyLocation.getName().contains("OPENLDAP") &&
                !idmPropertyLocation.getName().contains("VAGRANT") &&
                !idmPropertyLocation.getName().contains("GENERIC")
                ) {

            for (File file : idmPropertyLocation.listFiles()) {
                setIdmPropertyLocations(file);
            }
        }
        else if (idmPropertyLocation.getName().equals("idm.properties")) {
            idmPropertyLocationList.add(idmPropertyLocation.getCanonicalPath());
        }
    }

    private Set<String> getProperties(String fileLocation) throws IOException {
        Properties properties = new Properties();
        File file = new File(fileLocation);
        FileInputStream inputStream = new FileInputStream(file);
        properties.load(inputStream);
        inputStream.close();
        return (Set)properties.keySet();
    }
}
