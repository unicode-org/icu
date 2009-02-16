/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.util.ArrayList;
import java.util.Locale;
import java.util.MissingResourceException;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;
import com.ibm.icu.text.UCharacterIterator;

/**
 * <code>NumberingSystem</code> is the base class for all number
 * systems. This class provides the interface for setting different numbering
 * system types, whether it be a simple alternate digit system such as 
 * Thai digits or Devanagari digits, or an algorithmic numbering system such
 * as Hebrew numbering or Chinese numbering.
 *
 * @author       John Emmons
 * @draft ICU 4.2
 */

public class NumberingSystem {

    public NumberingSystem() {
        radix = 10;
        algorithmic = false;
        desc = "0123456789";
    }

    public static NumberingSystem getInstance(int radix_in, boolean isAlgorithmic_in, String desc_in ) {
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
        return ns;
    }

    public static NumberingSystem getInstance(Locale inLocale) {
        return getInstance(ULocale.forLocale(inLocale));
    }

    public static NumberingSystem getInstance(ULocale locale) {

        String numbersKeyword = locale.getKeywordValue("numbers");
        if (numbersKeyword != null) {
            NumberingSystem ns = getInstanceByName(numbersKeyword);
            if ( ns != null ) {
                return ns;
            }
        }

        String defaultNumberingSystem;

        try {
            ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME,locale);
            defaultNumberingSystem = rb.getString("defaultNumberingSystem");
        } catch (MissingResourceException ex) {
            return new NumberingSystem();
        }

        NumberingSystem ns = getInstanceByName(defaultNumberingSystem);
        if ( ns != null ) {
           return ns;
        }

        return new NumberingSystem();
    }

    public static NumberingSystem getInstance() {
        return getInstance(ULocale.getDefault());
    }

    public static NumberingSystem getInstanceByName(String name) {
        int radix;
        boolean isAlgorithmic;
        String description;
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

        return getInstance(radix,isAlgorithmic,description); 
    }

    public static String [] getAvailableNames() {
    
            UResourceBundle numberingSystemsInfo = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "numberingSystems");
            UResourceBundle nsCurrent = numberingSystemsInfo.get("numberingSystems");
            UResourceBundle temp;

            String nsName;
            ArrayList output = new ArrayList();
            UResourceBundleIterator it = nsCurrent.getIterator();
            while (it.hasNext()) {
                temp = it.next();
                nsName = temp.getKey();
                output.add(nsName);
            }
            return (String[]) output.toArray(new String[output.size()]);
    }

    public static boolean isValidDigitString(String str) {

        int c;
        int prev = 0;
        int i = 0;
        UCharacterIterator it = UCharacterIterator.getInstance(str);

        it.setToStart();
        while ( (c = it.nextCodePoint()) != UCharacterIterator.DONE) {
            if ( UCharacter.digit(c) != i ) { // Digits outside the Unicode decimal digit class are not currently supported
                return false;
            }
            if ( prev != 0 && c != prev + 1 ) { // Non-contiguous digits are not currently supported
                return false;
            }
            if ( UCharacter.isSupplementary(c)) { // Digits outside the BMP are not currently supported
                return false;
            }
            i++;
            prev = c;
        }
        return true;
    }

    public int getRadix() {
        return radix;
    }

    public String getDescription() {
        return desc;
    }

    public boolean isAlgorithmic() {
        return algorithmic;
    }


    private String desc;
    private int radix;
    private boolean algorithmic;

}
