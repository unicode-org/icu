/*
 *******************************************************************************
 * Copyright (C) 2003, International Business Machines Corporation and         *
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
     * Returns the exemplar characters for the given locale ID.
     *
     * @param locale    The locale for which the exemplar character set 
     *                  needs to be retrieved.
     * @return UnicodeSet The set representing the exemplar characters
     * @draft ICU 3.0
     */
    public static UnicodeSet getExemplarSet(ULocale locale){
        ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(UResourceBundle.ICU_BASE_NAME, locale);
        String pattern = bundle.getString(EXEMPLAR_CHARS);
        return new UnicodeSet(pattern);
    }
    
    /**
     * Enumeration for representing the measurement systems.
     * @draft ICU 2.8
     */
    public static final class MeasurementSystem{
        /** 
         * Measurement system specified by Le Syst&#x00E8;me International d'Unit&#x00E9;s (SI)
         * otherwise known as Metric system. 
         * @draft ICU 2.8
         */
        public static final MeasurementSystem SI = new MeasurementSystem(0);
 
        /** 
         * Measurement system followed in the United States of America. 
         * @draft ICU 2.8
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
     */
    public static final MeasurementSystem getMeasurementSystem(ULocale locale){
        ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(UResourceBundle.ICU_BASE_NAME, locale);
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
         */
        public int getHeight(){
            return height;
        }
        /**
         * Returns the width of hte paper
         * @return the width
         * @draft ICU 2.8
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
     */
    public static final PaperSize getPaperSize(ULocale locale){
        ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(UResourceBundle.ICU_BASE_NAME, locale);
        ICUResourceBundle obj = bundle.get(PAPER_SIZE);
        int[] size = obj.getIntVector();
        return new PaperSize(size[0], size[1]);
    }
}
