/*
 *****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and  *
 * others. All Rights Reserved.                                              *
 *****************************************************************************
 */
package com.ibm.rbm;


import java.io.*;
import java.net.URL;
import java.net.JarURLConnection;
import java.text.MessageFormat;
import java.util.*;
import java.util.zip.ZipEntry;

import com.ibm.rbm.gui.RBManagerGUI;

/**
 * A class not to be instantiated. Provides methods for translating items from a resource bundle. To use this class
 * make sure you first call initBundle(). Once this is done, calling any of the getTranslation() methods will return
 * the appropriate String.
 * 
 * @author Jared Jackson
 * @see com.ibm.rbm.RBManager
 */
public class Resources {
    private static ResourceBundle resource_bundle;
    private static Locale locale;

    /**
     * Initialize the properties of this resource bundle. This method must be called once before any
     * other method can be called (unless that method performs a check that calls this method). This is
     * also the method to use in case the resource data on the data store has changed and those changes
     * need to be reflected in future use of this class.
     */
	
    public static void initBundle() {
        try {
            if (locale == null) locale = Locale.getDefault();
            resource_bundle = ResourceBundle.getBundle("com/ibm/rbm/resources/RBManager", locale);
        } catch(MissingResourceException mre) {
            System.err.println("Missing Resource for default locale, " + Locale.getDefault().toString());
            mre.printStackTrace(System.err);
        }
    }
    
    /**
     * Set the locale to be used when making a query on the resource
     */
    
    public static void setLocale(Locale locale) {
        try {
            Resources.locale = locale;
            Resources.resource_bundle = ResourceBundle.getBundle("com/ibm/rbm/resources/RBManager", locale);
        } catch (MissingResourceException mre) {
            System.err.println("Missing Resource for locale, " + locale.toString());
            mre.printStackTrace(System.err);
        }
    }

    /**
     * Returns the currently set Locales object.
     */
	
    public static Locale getLocale() {
        if (Resources.locale == null) Resources.initBundle();
        return Resources.locale;
    }
    
    /**
     * Returns an array of strings containing the locale encoding (e.g. 'en_US', 'de', etc.)
     * of the locales defined in the current resource bundle. These are all of the locales for
     * which unique translation of resource bundle items are possible. If a locale encoding is
     * used to query on that is not in this array, the base class translation will be returned.
     */
	
    public static String[] getAvailableLocales() {
        //Locale loc[] = null;
        String list[] = new String[0];
        Vector locVect = new Vector();
        try {
            URL resURL = ClassLoader.getSystemResource("com/ibm/rbm/resources/RBManager.properties");
            JarURLConnection resConnection = (JarURLConnection)resURL.openConnection();
            Enumeration enumRes = resConnection.getJarFile().entries();
            String baseName = "com/ibm/rbm/resources/RBManager";
            while (enumRes.hasMoreElements()) {
                String entryName = ((ZipEntry)enumRes.nextElement()).getName();
                if (entryName.startsWith(baseName)) {
                    entryName = entryName.substring(baseName.length(), entryName.lastIndexOf('.'));
                    if (entryName.startsWith("_")) {
                        entryName = entryName.substring(1);
                    }
                    else if (entryName.length() == 0) {
                        /* For our resources we consider root as English */
                        entryName = "en";
                    }
                    locVect.add(entryName);
                }
            }

/*            File locDir = new File("resources");
            list = locDir.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    boolean accept = true;
                    if (!name.toLowerCase().endsWith(".properties")) accept = false; // Must be property file
                    if (!name.startsWith("RBManager")) accept = false;               // Must be a RBManager file
                    if (name.equals("RBManager.properties")) accept = false;         // Base class does not count
                    return accept;
                }
            });
*/
            int listSize = locVect.size();
            list = new String[listSize];
            for (int i=0; i < listSize; i++) {
                list[i] = (String)locVect.get(i);
            }
        }
        catch (IOException ioe) {
            System.err.println("Can't get resources");
            ioe.printStackTrace(System.err);
        }
        catch (ClassCastException cce) {
            System.err.println("Can't get resources");
            cce.printStackTrace(System.err);
        }
        return list;
    }

    /**
     * Looks up a translation in the currently set Locale by the NLS lookup key provided.
     * If no resource by that key is found, the key itself is returned.
     */
	
    public static String getTranslation(String key) {
        if (key == null || resource_bundle == null) return "";
        try {
            String retStr = resource_bundle.getString(key);
            return retStr;
        } catch (Exception e) {
            return key;
        }
    }
	
    /**
     * Given a locale encoding, returns the language portion of that encoding.
     *<br>
     * For instance 'en', 'en_US', 'en_GB', all return 'en'
     */
        
    public static String getLanguage(String encoding) {
        if (encoding == null) return null;
        if (encoding.indexOf("_") < 0) return encoding.trim();
        return encoding.substring(0, encoding.indexOf("_"));
    }
    
    /**
     * Given a locale encoding, returns the country portion of that encoding.
     * <br>
     * For instance 'en_US', 'sp_US', both return 'US'
     * <br>
     * Returns null if no country is specified (e.g. 'en' or 'de')
     */
    
    public static String getCountry(String encoding) {
        if (encoding == null) return null;
        if (encoding.indexOf("_") < 0) return null;
        String result = encoding.substring(encoding.indexOf("_")+1, encoding.length());
        if (result.indexOf("_") < 0) return result.trim();
        return result.substring(0, encoding.indexOf("_"));
    }
    
    /**
     * Given a locale encoding, returns the variant portion of that encoding.
     * <br>
     * For instance 'en_GB_EURO', 'de_DE_EURO', both return 'EURO'
     * <br>
     * Returns null if no variant is specified (e.g. 'en' or 'en_US')
     */
    
    public static String getVariant(String encoding) {
        RBManagerGUI.debugMsg(encoding);
        if (encoding == null) return null;
        if (encoding.indexOf("_") < 0) return null;
        String result = encoding.substring(encoding.indexOf("_")+1, encoding.length());
        if (result.indexOf("_") < 0) return null;
        result = result.substring(result.indexOf("_")+1, result.length());
        return result.trim();
    }

    /**
     * Gets a translation given the currently set locale, a lookup key, and a single lookup item replacement.
     * For an understanding of the lookup item replacement see getTranslation(String key, String[] lookup).
     */
	
    public static String getTranslation(String key, String lookup) {
        if (key == null || resource_bundle == null) return "";
        try {
            Object objects[] = {lookup};
            String retStr = resource_bundle.getString(key);
            retStr = MessageFormat.format(retStr, objects);
            return retStr;
        } catch (Exception e) {
            return key;
        }
    }
    
    /**
     * Gets a translation given the currently set locale, a lookup key, and zero or more lookup item replacements.
     * Lookup items are contextual translation material stored in the resource according to the format dictated in
     * the java.text package. In short, numbered markers are replaced by strings passed in as parameters.
     * <p>
     * For example, suppose you have the following resource:
     * <p>
     * myResource = Hello {1}, Isn't this a great {2}?
     * <p>
     * You want to replace the '{1}' witht the current user name and the '{2}' with the name of the day today.
     * You can do this by calling:
     * <p>
     * String lookups[] = { "Joe", "Friday" };
     * <br>
     * Resources.getTranslation("myResource", lookups);
     * <p>
     * The result would be:
     * <p>
     * 'Hello Joe, Isn't this a great Friday?'
     * <p>
     * This method (as well as the getTranslation(String key, String lookup) method) is useful for using nested
     * lookups from the resource bundle. For instance, the above line could be replaced with:
     * <p>
     * String lookups[] = { getUserName(), Resources.getTranslation("Friday") };
     */
    
    public static String getTranslation(String key, String[] lookup) {
        if (key == null || resource_bundle == null) return "";
        try {
            Object objects[] = new Object[lookup.length];
            for (int i=0; i < lookup.length; i++) objects[i] = lookup[i];
            String retStr = resource_bundle.getString(key);
            retStr = MessageFormat.format(retStr, lookup);
            return retStr;
        } catch (Exception e) {
            return key;
        }
    }
}