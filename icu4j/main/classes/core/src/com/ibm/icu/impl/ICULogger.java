/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 * Extends the Java Logger class adding a method to turn off/on logging.
 * Classes where logging is wanted contains a static ICULogger object
 * with logging turned off by default unless the system property 
 * "icu4j.debug.logging" is set to "all"
 * 
 * If "icu4j.debug.logging" is not set to "all", then the individual loggers needs
 * to be turned on manually. (e.g. TimeZone.TimeZoneLogger.turnLoggingOn())
 * <p>
 * To use logging, the system property "icu4j.debug.logging" must be set to "on" or "all",
 * otherwise the static ICULogger object will be null. This will help lower any unneccessary
 * resource usage when logging is not desired.
 * <P>
 * <strong>Examples</strong>:<P>
 * Usage in code
 * <blockquote>
 * <pre>
 * public class Class {
 *     // Create logger object (usually with the class name)
 *     public static ICULogger ClassLogger = ICULogger.getICULogger(Class.class.getName());
 *     
 *     // Method that will use logger.
 *     public boolean hasSomething(Object obj) {
 *         if (obj == null) {
 *              // Log that obj is null.
 *              // Note: Good to check for null and if logging is turned on to minimize resource usage when logging is not needed.
 *              if (ClassLogger != null && ClassLogger.isLoggingOn()) {
 *                  ClassLogger.warning("obj is null so false was returned by default.");
 *              }
 *             return false;
 *         }
 *         
 *         ...
 *         
 *     }
 * }
 * </pre>
 * </blockquote>
 * Turning on logging (using the default settings)
 * <blockquote>
 * <pre>
 * java -Dicu4j.debug.logging=all program
 * </pre>
 * </blockquote>
 */

public class ICULogger extends Logger {
    private static enum LOGGER_STATUS { ON, OFF, NULL };
    private static final String GLOBAL_FLAG_TURN_ON_LOGGING = "all";
    private static final String SYSTEM_PROP_LOGGER = "icu4j.debug.logging";
    
    private LOGGER_STATUS currentStatus;
    
    /**
     * ICULogger constructor that calls the parent constructor with the desired parameters.
     */
    private ICULogger(String name, String resourceBundleName) {
        super(name, resourceBundleName);
    }
    
    /**
     * Set the status to either on or off. Set the level of the logger to INFO.
     */
    private void setStatus(LOGGER_STATUS newStatus) {
        if (currentStatus != newStatus) {
            /* Default to level INFO */
            if (currentStatus == LOGGER_STATUS.OFF && newStatus == LOGGER_STATUS.ON) {
                this.setLevel(Level.INFO);
            }
            
            currentStatus = newStatus;
            
            if (currentStatus == LOGGER_STATUS.OFF){
                this.setLevel(Level.OFF);
            }
        }
    }
    
    /**
     * Check the system property SYSTEM_PROP_LOGGER to see if it is set.
     * return true if it is otherwise return false.
     */
    private static LOGGER_STATUS checkGlobalLoggingFlag() {
        String prop = System.getProperty(SYSTEM_PROP_LOGGER);
        
        if (prop != null) {
            if (prop.equals(GLOBAL_FLAG_TURN_ON_LOGGING)) {
                return LOGGER_STATUS.ON;
            }
            return LOGGER_STATUS.OFF;
        }
        
        return LOGGER_STATUS.NULL;
    }
    
    /**
     * Instantiates a new ICULogger object with logging turned off by default.
     *
     * @param name to be use by the logger (usually is the class name)
     * @return a new ICULogger object
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public static ICULogger getICULogger(String name) {
        return getICULogger(name, null);
    }
    
    /**
     * Instantiates a new ICULogger object with logging turned off by default
     * unless the system property "icu4j.debug.logging" is set to "all"
     *
     * @param name to be use by the logger (usually is the class name)
     * @param resourceBundleName name to localize messages (can be null)
     * @return a new ICULogger object
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public static ICULogger getICULogger(String name, String resourceBundleName) {
        LOGGER_STATUS flag = checkGlobalLoggingFlag();
        if (flag != LOGGER_STATUS.NULL) {
            ICULogger logger = new ICULogger(name, resourceBundleName);
            
            /* Add a default handler to logger*/
            logger.addHandler(new ConsoleHandler());
            
            /* Turn off logging by default unless SYSTEM_PROP_LOGGER property is set to "all" */
            if (flag == LOGGER_STATUS.ON) {
                logger.turnOnLogging();
            } else {
                logger.turnOffLogging();
            }
            
            return logger;
        }
        return null;
    }
    
    /**
     * Determined if logging is turned on or off. The return value is true if logging is on.
     *
     * @return whether logging is turned on or off.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public boolean isLoggingOn() {
        if (currentStatus == LOGGER_STATUS.ON) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Turn logging on.
     *
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public void turnOnLogging() {
        setStatus(LOGGER_STATUS.ON);
    }
    
    /**
     * Turn logging off.
     *
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public void turnOffLogging() {
        setStatus(LOGGER_STATUS.OFF);
    }
    
}
