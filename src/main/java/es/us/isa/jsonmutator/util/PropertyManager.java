package es.us.isa.jsonmutator.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static java.lang.System.exit;

/**
 * 
 * @author Alberto Martin-Lopez
 */
public class PropertyManager {

//	private static String propertyFilePath = "src/main/resources/json-mutation.properties";	Original (does not work in JAR)
	private static String propertyFilePath = "/json-mutation.properties";
	private static 	Properties properties = null;

	public static String readProperty(String name) {
		loadProperties();
		return properties.getProperty(name);
	}

	public static void setProperty(String propertyName, String propertyValue) {
		loadProperties();
		properties.setProperty(propertyName, propertyValue);
	}

	public static void resetProperties() {
		properties = new Properties();
		try {
			//	properties.load(new FileInputStream(propertyFilePath));	Original (does not work in JAR)
			InputStream inputStream = PropertyManager.class.getResourceAsStream(propertyFilePath);
			properties.load(inputStream);
		} catch (IOException e) {
			System.err.println("Error reading property file: " + e.getMessage());
			e.printStackTrace();
			exit(1);
		}
	}

	private static void loadProperties() {
		if (properties==null) {
			resetProperties();
		}
	}
}
