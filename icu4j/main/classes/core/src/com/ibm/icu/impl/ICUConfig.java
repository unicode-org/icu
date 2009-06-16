/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;
import java.util.Properties;

/**
 * ICUConfig is a class used for accessing ICU4J runtime configuration.
 */
public class ICUConfig {
    public static final String CONFIG_PROPS_FILE = "/com/ibm/icu/ICUConfig.properties";
    private static final Properties CONFIG_PROPS;

    static {
        CONFIG_PROPS = new Properties();
        try {
            InputStream is = ICUData.getStream(CONFIG_PROPS_FILE);
            if (is != null) {
                CONFIG_PROPS.load(is);
            }
        } catch (MissingResourceException mre) {
            // If it does not exist, ignore.
        } catch (IOException ioe) {
            // Any IO errors, ignore
        }
    }

    /**
     * Get ICU configuration property value for the given name.
     * @param name The configuration property name
     * @return The configuration property value, or null if it does not exist.
     */
    public static String get(String name) {
        return get(name, null);
    }

    /**
     * Get ICU configuration property value for the given name.
     * @param name The configuration property name
     * @param def The default value
     * @return The configuration property value.  If the property does not
     * exist, <code>def</code> is returned.
     */
    public static String get(String name, String def) {
        String val = null;
        // Try the system property first
        try {
            val = System.getProperty(name);
        } catch (SecurityException e) {
            // Ignore and fall through
        }

        if (val == null) {
            val = CONFIG_PROPS.getProperty(name, def);
        }
        return val;
    }
}
