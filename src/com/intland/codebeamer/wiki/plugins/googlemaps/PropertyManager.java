package com.intland.codebeamer.wiki.plugins.googlemaps;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class PropertyManager {
    final static private Logger logger = Logger.getLogger(PropertyManager.class);

    public static final String PROPERTY_FILE = "/googlemaps/googlemaps.properties";

    private static Properties properties = null;

    public static void init() throws Exception {
		if (properties == null) {
			InputStream is = PropertyManager.class.getResourceAsStream(PROPERTY_FILE);

			if (is == null) {
				logger.error("Cannot open resource: " + PROPERTY_FILE);
				throw new Exception("Cannot open resource: " + PROPERTY_FILE);
			}

			try {
				Properties prop = new Properties();
				prop.load(is);
				properties = prop;
			} catch (IOException ex) {
				logger.error("Cannot load resource: " + PROPERTY_FILE, ex);
				throw new Exception("Cannot load resource: " + PROPERTY_FILE);
			} catch (IllegalArgumentException ex) {
				logger.error("Malformed Unicode escape sequence in resource: " + PROPERTY_FILE, ex);
				throw new Exception("Malformed Unicode escape sequence in resource: " + PROPERTY_FILE);
			} finally {
				try {
					is.close();
				} catch (IOException ex) {
				}
			}
		}
	}

    public static String getProperty(String key) {
    	// lazy init
    	if ( properties == null) {
    		try {
				init();
			} catch (Exception e) {
			}
    	}
		return properties != null ? properties.getProperty(key) : null;
	}
}
