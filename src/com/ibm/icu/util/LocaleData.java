/*
 *******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
*/
package com.ibm.icu.util;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.text.UnicodeSet;

/**
 * A class for accessing miscelleneous data in the locale bundles
 * @author ram
 * @draft ICU 2.8
 * @deprecated This is a draft API and might change in a future release of ICU.
 */
public final class LocaleData {
    
    private static final String EXEMPLAR_CHARS      = "ExemplarCharacters";
    private static final String MEASUREMENT_SYSTEM  = "MeasurementSystem";
    private static final String PAPER_SIZE          = "PaperSize";
    
    // private constructor to prevent default construction
    ///CLOVER:OFF
    private LocaleData(){}
    ///CLOVER:ON
    /**
     * Returns the set of exemplar characters for a locale.
     *
     * @param locale    Locale for which the exemplar character set
     *                  is to be retrieved.
     * @param options   Bitmask for options to apply to the exemplar pattern.
     *                  Specify zero to retrieve the exemplar set as it is
     *                  defined in the locale data.  Specify
     *                  UnicodeSet.CASE to retrieve a case-folded exemplar
     *                  set.  See {@link UnicodeSet#applyPattern(String,
     *                  int)} for a complete list of valid options.  The
     *                  IGNORE_SPACE bit is always set, regardless of the
     *                  value of 'options'.
     * @return          The set of exemplar characters for the given locale.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static UnicodeSet getExemplarSet(ULocale locale, int options) {
        ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
        String pattern = bundle.getString(EXEMPLAR_CHARS);
        return new UnicodeSet(pattern, UnicodeSet.IGNORE_SPACE | options);
    }
    
    /**
     * Enumeration for representing the measurement systems.
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final class MeasurementSystem{
        /** 
         * Measurement system specified by Le Syst&#x00E8;me International d'Unit&#x00E9;s (SI)
         * otherwise known as Metric system. 
         * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
         */
        public static final MeasurementSystem SI = new MeasurementSystem(0);
 
        /** 
         * Measurement system followed in the United States of America. 
         * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
         */ 
        public static final MeasurementSystem US = new MeasurementSystem(1);
    
        private int systemID;
        private MeasurementSystem(int id){
            systemID = id;
        }
        private boolean equals(int id){
            return systemID == id;
        }
    }
   
    /**
     * Returns the measurement system used in the locale specified by the locale.
     *
     * @param locale      The locale for which the measurement system to be retrieved.
     * @return MeasurementSystem the measurement system used in the locale.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final MeasurementSystem getMeasurementSystem(ULocale locale){
        ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
        ICUResourceBundle sysBundle = bundle.get(MEASUREMENT_SYSTEM);
        
        int system = sysBundle.getInt();
        if(MeasurementSystem.US.equals(system)){
            return MeasurementSystem.US;
        }
        if(MeasurementSystem.SI.equals(system)){
            return MeasurementSystem.SI;
        }
        // return null if the object is null or is not an instance
        // of integer indicating an error
        return null;
    }
    
    /**
     * A class that represents the size of letter head 
     * used in the country
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final class PaperSize{
        private int height;
        private int width;
        
        private PaperSize(int h, int w){
            height = h;
            width = w;
        }
        /** 
         * Retruns the height of the paper
         * @return the height 
         * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
         */
        public int getHeight(){
            return height;
        }
        /**
         * Returns the width of hte paper
         * @return the width
         * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
         */
        public int getWidth(){
            return width;
        }
    }
    
    /**
     * Returns the size of paper used in the locale. The paper sizes returned are always in 
     * <em> milli-meters<em>.
     * @param locale The locale for which the measurement system to be retrieved. 
     * @return The paper size used in the locale
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static final PaperSize getPaperSize(ULocale locale){
        ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
        ICUResourceBundle obj = bundle.get(PAPER_SIZE);
        int[] size = obj.getIntVector();
        return new PaperSize(size[0], size[1]);
    }
}
