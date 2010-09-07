/*
 *******************************************************************************
 * Copyright (C) 2009-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.util.ArrayList;
import java.util.Locale;
import java.util.MissingResourceException;

import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.SimpleCache;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;


/**
 * <code>NumberingSystem</code> is the base class for all number
 * systems. This class provides the interface for setting different numbering
 * system types, whether it be a simple alternate digit system such as 
 * Thai digits or Devanagari digits, or an algorithmic numbering system such
 * as Hebrew numbering or Chinese numbering.
 *
 * @author       John Emmons
 * @stable ICU 4.2
 */
public class NumberingSystem {

    /**
     * Default constructor.  Returns a numbering system that uses the Western decimal
     * digits 0 through 9.
     * @stable ICU 4.2
     */
    public NumberingSystem() {
        radix = 10;
        algorithmic = false;
        desc = "0123456789";
        name = "latn";
    }

    /**
     * Factory method for creating a numbering system.
     * @param radix_in The radix for this numbering system.  ICU currently 
     * supports only numbering systems whose radix is 10.
     * @param isAlgorithmic_in Specifies whether the numbering system is algorithmic
     * (true) or numeric (false).
     * @param desc_in String used to describe the characteristics of the numbering
     * system.  For numeric systems, this string contains the digits used by the
     * numbering system, in order, starting from zero.  For algorithmic numbering
     * systems, the string contains the name of the RBNF ruleset in the locale's
     * NumberingSystemRules section that will be used to format numbers using
     * this numbering system.
     * @stable ICU 4.2
     */
    public static NumberingSystem getInstance(int radix_in, boolean isAlgorithmic_in, String desc_in ) {
        return getInstance(null,radix_in,isAlgorithmic_in,desc_in);
    }
    
    /**
     * Factory method for creating a numbering system.
     * @param name_in The string representing the name of the numbering system.
     * @param radix_in The radix for this numbering system.  ICU currently 
     * supports only numbering systems whose radix is 10.
     * @param isAlgorithmic_in Specifies whether the numbering system is algorithmic
     * (true) or numeric (false).
     * @param desc_in String used to describe the characteristics of the numbering
     * system.  For numeric systems, this string contains the digits used by the
     * numbering system, in order, starting from zero.  For algorithmic numbering
     * systems, the string contains the name of the RBNF ruleset in the locale's
     * NumberingSystemRules section that will be used to format numbers using
     * this numbering system.
     * @draft ICU 4.6
     */
   
    private static NumberingSystem getInstance(String name_in, int radix_in, boolean isAlgorithmic_in, String desc_in ) {
        if ( radix_in < 2 ) {
            throw new IllegalArgumentException("Invalid radix for numbering system");
        }

        if ( !isAlgorithmic_in ) {
            if ( desc_in.length() != radix_in || !isValidDigitString(desc_in)) {
                throw new IllegalArgumentException("Invalid digit string for numbering system");
            }
        }
        NumberingSystem ns = new NumberingSystem();
        ns.radix = radix_in;
        ns.algorithmic = isAlgorithmic_in;
        ns.desc = desc_in;
        ns.name = name_in;
        return ns;
    }

    /**
     * Returns the default numbering system for the specified locale.
     * @stable ICU 4.2
     */
    public static NumberingSystem getInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale));
    }

    /**
     * Returns the default numbering system for the specified ULocale.
     * @stable ICU 4.2
     */
    public static NumberingSystem getInstance(ULocale locale) {

        NumberingSystem ns;
        String defaultNumberingSystem;
        
        // Check for @numbers
        String numbersKeyword = locale.getKeywordValue("numbers");
        if (numbersKeyword != null) {
            ns = getInstanceByName(numbersKeyword);
            if ( ns != null ) {
                return ns;
            }
        }

        // Get the numbering system from the cache
        String baseName = locale.getBaseName();
        ns = cachedLocaleData.get(baseName);
        if (ns != null ) {
            return ns;
        }
        
        // Cache miss, create new instance
        try {
            ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME,locale);
            rb = rb.getWithFallback("NumberElements");
            defaultNumberingSystem = rb.getStringWithFallback("default");
        } catch (MissingResourceException ex) {
            ns = new NumberingSystem();
            cachedLocaleData.put(baseName, ns);
            return ns;
        }

        ns = getInstanceByName(defaultNumberingSystem);
        if ( ns != null ) {
           cachedLocaleData.put(baseName, ns);
           return ns;
        }

        ns = new NumberingSystem();
        cachedLocaleData.put(baseName, ns);
        return ns;        
        
    }

    /**
     * Returns the default numbering system for the default locale.
     * @stable ICU 4.2
     */
    public static NumberingSystem getInstance() {
        return getInstance(ULocale.getDefault());
    }

    /**
     * Returns a numbering system from one of the predefined numbering systems
     * known to ICU.  Numbering system names are based on the numbering systems
     * defined in CLDR.  To get a list of available numbering systems, use the
     * getAvailableNames method.
     * @param name The name of the desired numbering system.  Numbering system
     * names often correspond with the name of the script they are associated
     * with.  For example, "thai" for Thai digits, "hebr" for Hebrew numerals.
     * @stable ICU 4.2
     */
    public static NumberingSystem getInstanceByName(String name) {
        int radix;
        boolean isAlgorithmic;
        String description;
        
        // Get the numbering system from the cache
        NumberingSystem ns = cachedStringData.get(name);
        if (ns != null ) {
            return ns;
        }        
        
        try {
            UResourceBundle numberingSystemsInfo = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "numberingSystems");
            UResourceBundle nsCurrent = numberingSystemsInfo.get("numberingSystems");
            UResourceBundle nsTop = nsCurrent.get(name);

            description = nsTop.getString("desc");
            UResourceBundle nsRadixBundle = nsTop.get("radix");
            UResourceBundle nsAlgBundle = nsTop.get("algorithmic");
            radix = nsRadixBundle.getInt();
            int algorithmic = nsAlgBundle.getInt();

            isAlgorithmic = ( algorithmic == 1 );

        } catch (MissingResourceException ex) {
            return null;
        }

        ns = getInstance(name,radix,isAlgorithmic,description);
        cachedStringData.put(name, ns);                       
        return ns;     
    }

    /**
     * Returns a string array containing a list of the names of numbering systems
     * currently known to ICU.
     * @stable ICU 4.2
     */
    public static String [] getAvailableNames() {
    
            UResourceBundle numberingSystemsInfo = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "numberingSystems");
            UResourceBundle nsCurrent = numberingSystemsInfo.get("numberingSystems");
            UResourceBundle temp;

            String nsName;
            ArrayList<String> output = new ArrayList<String>();
            UResourceBundleIterator it = nsCurrent.getIterator();
            while (it.hasNext()) {
                temp = it.next();
                nsName = temp.getKey();
                output.add(nsName);
            }
            return output.toArray(new String[output.size()]);
    }

    /**
     * Convenience method to determine if a given digit string is valid for use as a 
     * descriptor of a numeric ( non-algorithmic ) numbering system.  In order for
     * a digit string to be valid, it must meet the following criteria:
     * 1. Digits must be in Unicode's basic multilingual plane.
     * @stable ICU 4.2
     */
    public static boolean isValidDigitString(String str) {

        int c;
        int i = 0;
        UCharacterIterator it = UCharacterIterator.getInstance(str);

        it.setToStart();
        while ( (c = it.nextCodePoint()) != UCharacterIterator.DONE) {
            if ( UCharacter.isSupplementary(c)) { // Digits outside the BMP are not currently supported
                return false;
            }
            i++;
        }
        if ( i != 10 ) {
            return false;
        }
        return true;
    }

    /**
     * Returns the radix of the current numbering system.
     * @stable ICU 4.2
     */
    public int getRadix() {
        return radix;
    }

    /**
     * Returns the description string of the current numbering system.
     * The description string describes the characteristics of the numbering
     * system.  For numeric systems, this string contains the digits used by the
     * numbering system, in order, starting from zero.  For algorithmic numbering
     * systems, the string contains the name of the RBNF ruleset in the locale's
     * NumberingSystemRules section that will be used to format numbers using
     * this numbering system.
     * @stable ICU 4.2
     */
    public String getDescription() {
        return desc;
    }

    /**
     * Returns the string representing the name of the numbering system.
     * @draft ICU 4.6
     */
    public String getName() {
        return name;
    }
    /**
     * Returns the numbering system's algorithmic status.  If true,
     * the numbering system is algorithmic and uses an RBNF formatter to
     * format numerals.  If false, the numbering system is numeric and
     * uses a fixed set of digits. 
     * @stable ICU 4.2
     */
    public boolean isAlgorithmic() {
        return algorithmic;
    }

    private String desc;
    private int radix;
    private boolean algorithmic;
    private String name;

    /**
     * Cache to hold the NumberingSystems by Locale.
     */
    private static ICUCache<String, NumberingSystem> cachedLocaleData = new SimpleCache<String, NumberingSystem>();
        
    /**
     * Cache to hold the NumberingSystems by name.
     */
    private static ICUCache<String, NumberingSystem> cachedStringData = new SimpleCache<String, NumberingSystem>();
    
}
