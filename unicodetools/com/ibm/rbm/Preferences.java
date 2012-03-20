/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;

import java.util.*;
import java.io.*;

/**
 * This class defines the methods used by RBManager to access, set, and store
 * individual user preferences for the application. All of the public methods defined
 * in this class are static, and so the class need not be instantiated.
 * 
 * @author Jared Jackson
 * @see com.ibm.rbm.RBManager
 */
public class Preferences {
    // Default values
    private static final int NUM_RECENT_FILES = 4;
    private static final String EMPTY_STRING = "";
    private static Properties prop;
	
    /**
     * Retrieve a preference by its key name
     * @param name The name of the key associated with one preference
     * @return The value of the preference sought
     */
	
    public static String getPreference(String name) {
        if (prop == null) init();
        Object o = prop.get(name);
        if (o == null || !(o instanceof String)) return EMPTY_STRING;
        return (String)o;
    }
	
    /**
     * Sets a preference by key name and value. If the key name all ready exists, that
     * preference is overwritten without warning.
     * @param name The name of the key associated with the preference
     * @param value The value of the preference to be set and later retrieved. If this value is null, the property of this name is erased.
     */
	
    public static void setPreference(String name, String value) {
        if (prop == null) init();
        if (value == null) {
            // In this case, we will remove the property
            prop.remove(name);
        }
        prop.put(name, value);
    }
	
    /**
     * Writes the results of the buffered preferences to file. There is no option for
     * where this file is saved on the file system.
     */
	
    public static void savePreferences() throws IOException {
        if (prop == null) init();
        FileOutputStream fos = new FileOutputStream("preferences.properties");
        prop.store(fos, "RBManager Preferences");
        fos.flush();
        fos.close();
    }
	
    /**
     * Given the name of a resource bundle and the file path location of the base
     * document for that resource bundle, this method will insert that file into
     * a list of recent files. Currently the past 4 resource bundles visited will
     * be displayed. This method also sorts the prefences so that the most recently
     * added will be the first returned, even if that file had all ready existed
     * in the preferences when it was added.
     * @param name The name of this file as it will be displayed to the user
     * @param location The file path to this file (should be absolute).
     */
	
    public static void addRecentFilePreference(String name, String location) {
        Vector existingNames = new Vector();
        Vector existingLocations = new Vector();
        for (int i=0; i < NUM_RECENT_FILES; i++) {
            String oldName = getPreference("recentfileid" + String.valueOf(i));
            String oldLocation = getPreference("recentfileloc" + String.valueOf(i));
            if (oldName.equals(EMPTY_STRING) || oldLocation.equals(EMPTY_STRING)) break;
            existingNames.addElement(oldName);
            existingLocations.addElement(oldLocation);
        }
        // Check to see if the file is all ready in there
        int swap_start = 0;
        int old_size = existingLocations.size();
        for (int i=0; i <= old_size; i++) {
            if (i == existingLocations.size()) {
                // No match was found, pull all the elements down one
                swap_start = i;
                if (swap_start >= NUM_RECENT_FILES) swap_start = NUM_RECENT_FILES-1;
                else {
                    // Extend the length of the vectors
                    existingNames.addElement(EMPTY_STRING);
                    existingLocations.addElement(EMPTY_STRING);
                }
            } else {
                String oldLocation = (String)existingLocations.elementAt(i);
                if (oldLocation.equals(location)) {
                    // We found a match, pull this one to the front
                    swap_start = i;
                    break;
                }
            }
        }
		
        // Move the files down the line as appropriate
        for (int i=swap_start; i > 0; i--) {
            existingLocations.setElementAt(existingLocations.elementAt(i-1),i);
            existingNames.setElementAt(existingNames.elementAt(i-1),i);
        }
        existingLocations.setElementAt(location, 0);
        existingNames.setElementAt(name, 0);
		
        // Set the properties
        for (int i=0; i < existingLocations.size(); i++) {
            setPreference("recentfileid" + String.valueOf(i), (String)existingNames.elementAt(i));
            setPreference("recentfileloc" + String.valueOf(i), (String)existingLocations.elementAt(i));
        }
        for (int i=existingLocations.size(); i < NUM_RECENT_FILES; i++) {
            setPreference("recentfileid" + String.valueOf(i), EMPTY_STRING);
            setPreference("recentfileloc" + String.valueOf(i), EMPTY_STRING);
        }
        try {
            savePreferences();
        } catch (IOException ioe) {} // Ignore, its not critical
    }
    
    /**
     * Returns a list of the names and locations of the various recently used files.
     * @return A Vector of Strings which is twice in length the number of files known about. The vector contains name 1 then location 1, then name 2 ...
     */
	
    public static Vector getRecentFilesPreferences() {
        if (prop == null) init();
        Vector existing = new Vector();
        for (int i=0; i < NUM_RECENT_FILES; i++) {
            String name = getPreference("recentfileid" + String.valueOf(i));
            String location = getPreference("recentfileloc" + String.valueOf(i));
            if (name.equals(EMPTY_STRING) || location.equals(EMPTY_STRING)) break;
            existing.addElement(name);
            existing.addElement(location);
        }
        return existing;
    }

    private static void init() {
        Properties defaults = new Properties();
        // This values are needed and are specified by default
        // If they exist in the file, they will be overwritten
        defaults.put("username", Resources.getTranslation("unknown_user"));
        defaults.put("locale", "en");
        defaults.put("lookandfeel", "");
		
        prop = new Properties(defaults);
        try {
            FileInputStream fis = new FileInputStream("preferences.properties");
            prop.load(fis);
        } catch (IOException ioe) {
            System.err.println("Error reading properties");
            ioe.printStackTrace(System.err);
        }
        try {
            savePreferences();
        } catch (IOException ioe) {
            System.err.println("Error saving preferences " + ioe.getMessage());
        }
    }
	
    /*
    public static void main(String args[]) {
        // Test
        init();
    }
    */
}
