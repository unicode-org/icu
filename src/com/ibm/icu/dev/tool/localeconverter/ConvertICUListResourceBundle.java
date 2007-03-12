/**
*******************************************************************************
* Copyright (C) 2002-2004, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
package com.ibm.icu.dev.tool.localeconverter;

import java.io.*;
import java.util.*;

/**
 * @author ram
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class ConvertICUListResourceBundle {
    private static final byte OPT_11 = (byte)0x01;
    private static final byte OPT_12 = (byte)0x02;
    private static final byte OPT_ICU = (byte)0x04;
    private static final byte OPT_PACKAGE = (byte)0x08;
    private static final byte OPT_BUNDLE = (byte)0x10;
    private static final byte OPT_UNKNOWN = (byte)0x80;
    private static final String USER_OPTIONS[] = {
        "-11", 
        "-12", 
        "-icu",
        "-package",
        "-bundle-name"
    };
    
    public static void main(String args[]) {
        try {
            new ConvertICUListResourceBundle(args, System.out);
        } catch (Throwable t) {
            System.err.println("Unknown error: "+t);
        }
    }
    
    public ConvertICUListResourceBundle(String args[], PrintStream out) {
        process(args, out);
    }
    
    public void process(String args[], PrintStream out) {
        short options = identifyOptions(args);
        if ((args.length < 1) || ((options & OPT_UNKNOWN) != 0)) {
            printUsage();
        } else {
            String localeName = null;
            String packagename = null;
            String bundleName = null;
            for (int i = 0; i < args.length; i++) {
                //final String thisArg = args[i];

                if(args[i].equalsIgnoreCase("-package")){
                   i++;
                   packagename = args[i];
                }else if(args[i].equalsIgnoreCase("-icu")){
                }else if (!args[i].startsWith("-")) {
                    localeName = args[i];
                }else if(args[i].equalsIgnoreCase("-bundle-name")){
                    bundleName = args[++i];
                }
            }
            final Hashtable data = new Hashtable();
            final String localeElements = packagename
                    + (String)((bundleName != null) ? "."+ bundleName : ".LocaleElements" )
                    + (String)((localeName != null) ? "_"+ localeName : "");

           // final String DateFormatZoneData = packagename+".DateFormatZoneData" +
           //         (String)((localeName != null) ? "_"+localeName : "");

            addLocaleData(localeElements, data);
            //addLocaleData(DateFormatZoneData, data);
            
            Locale locale;
            if(localeName==null){
                locale = localeFromString("root");
            }else{
                locale = localeFromString(localeName);
            }
            if ((options & OPT_11) != 0) {
                new Java1LocaleWriter(out, System.err).write(locale, data);
            }
            if ((options & OPT_12) != 0) {
                new JavaLocaleWriter(out, System.err).write(locale, data);
            }
            if ((options & OPT_ICU) != 0) {
                 new ICU3LocaleWriter(getBundle(localeElements),out, System.err).write(locale);
            }
        }
    }
    
    private ListResourceBundle getBundle(final String bundleClassName){
        try {
            final Class bundleClass = Class.forName(bundleClassName);
            final ListResourceBundle bundle = (ListResourceBundle)bundleClass.newInstance();
            return bundle;

        } catch (ClassNotFoundException e) {
            System.err.println("Could not find bundle class for bundle: "+bundleClassName);
        } catch (InstantiationException e) {
            System.err.println("Could not create bundle instance for bundle: "+bundleClassName);
        } catch (IllegalAccessException e) {
            System.err.println("Could not create bundle instance for bundle: "+bundleClassName);
        }
        return null;
    }
    private void addLocaleData(final String bundleClassName, final Hashtable data) {
        ResourceBundle bundle = getBundle(bundleClassName);
        Enumeration keys = bundle.getKeys();
        while(keys.hasMoreElements()){
            String key = (String) keys.nextElement();
            Object o = bundle.getObject(key);
            data.put(key, o);
        }
    }
    
    private void printUsage() {
        System.err.println("Usage: ConvertICUListResourceBundle [-11] [-12] [-icu] [-package] <package name> [-bundle-name] <bundle name> localeName");
    }
    
    private short identifyOptions(String[] options) {
        short result = 0;
        for (int j = 0; j < options.length; j++) {
            String option = options[j];
            if (option.startsWith("-")) {
                boolean optionRecognized = false;
                for (short i = 0; i < USER_OPTIONS.length; i++) {
                    if (USER_OPTIONS[i].equals(option)) {
                        result |= (short)(1 << i);
                        optionRecognized = true;
                        break;
                    }
                }
                if (!optionRecognized) {
                    result |= OPT_UNKNOWN;
                }
            }
        }
        return result;
    }
    
    private Locale localeFromString(final String localeName) {
        if (localeName == null) return new Locale("", "", "");
        String language = localeName;
        String country = "";
        String variant = "";
        
        int ndx = language.indexOf('_');
        if (ndx >= 0) {
            country = language.substring(ndx+1);
            language = language.substring(0, ndx);
        }
        ndx = country.indexOf('_');
        if (ndx >= 0) {
            variant = country.substring(ndx+1);
            country = country.substring(0, ndx);
        }
        return new Locale(language, country, variant);
    }
}
