/*
 *******************************************************************************
 * Copyright (C) 2004-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
*/
package com.ibm.icu.util;

import java.util.MissingResourceException;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.text.UnicodeSet;

/**
 * A class for accessing miscelleneous data in the locale bundles
 * @author ram
 * @stable ICU 2.8
 */
public final class LocaleData {
    
    private static final String EXEMPLAR_CHARS      = "ExemplarCharacters";
    private static final String MEASUREMENT_SYSTEM  = "MeasurementSystem";
    private static final String PAPER_SIZE          = "PaperSize";
    private static final String LOCALE_DISPLAY_PATTERN  = "localeDisplayPattern";
    private static final String PATTERN             = "pattern";
    private static final String SEPARATOR           = "separator";
    private boolean noSubstitute;
    private ICUResourceBundle bundle;

    /**
     * EXType for {@link #getExemplarSet(int, int)}.
     * @stable ICU 3.4
     */
    public static final int ES_STANDARD = 0;

    /**
     * EXType for {@link #getExemplarSet(int, int)}.
     * @stable ICU 3.4
     */
    public static final int ES_AUXILIARY = 1;

    /**
     * Count of EXTypes for {@link #getExemplarSet(int, int)}.
     * @stable ICU 3.4
     */
    public static final int ES_COUNT = 2;
    
    /**
     * Delimiter type for {@link #getDelimiter(int)}.
     * @stable ICU 3.4
     */
    public static final int QUOTATION_START = 0;

    /**
     * Delimiter type for {@link #getDelimiter(int)}.
     * @stable ICU 3.4
     */
    public static final int QUOTATION_END = 1;

    /**
     * Delimiter type for {@link #getDelimiter(int)}.
     * @stable ICU 3.4
     */
    public static final int ALT_QUOTATION_START = 2;

    /**
     * Delimiter type for {@link #getDelimiter(int)}.
     * @stable ICU 3.4
     */
    public static final int ALT_QUOTATION_END = 3;

    /**
     * Count of delimiter types for {@link #getDelimiter(int)}.
     * @stable ICU 3.4
     */
    public static final int DELIMITER_COUNT = 4;

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
     * @stable ICU 3.0
     */
    public static UnicodeSet getExemplarSet(ULocale locale, int options) {
        ICUResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
        String pattern = bundle.getString(EXEMPLAR_CHARS);
        return new UnicodeSet(pattern, UnicodeSet.IGNORE_SPACE | options);
    }
    
    /**
     * Returns the set of exemplar characters for a locale.
     *
     * @param options   Bitmask for options to apply to the exemplar pattern.
     *                  Specify zero to retrieve the exemplar set as it is
     *                  defined in the locale data.  Specify
     *                  UnicodeSet.CASE to retrieve a case-folded exemplar
     *                  set.  See {@link UnicodeSet#applyPattern(String,
     *                  int)} for a complete list of valid options.  The
     *                  IGNORE_SPACE bit is always set, regardless of the
     *                  value of 'options'.
     * @param extype      The type of exemplar set to be retrieved,
     *                  ES_STANDARD or ES_AUXILIARY
     * @return          The set of exemplar characters for the given locale.
     * @stable ICU 3.4
     */
    public UnicodeSet getExemplarSet(int options, int extype) {
        String [] exemplarSetTypes = { "ExemplarCharacters", "AuxExemplarCharacters" };
        try{
            ICUResourceBundle stringBundle = (ICUResourceBundle) bundle.get(exemplarSetTypes[extype]);
    
            if ( noSubstitute && (stringBundle.getLoadingStatus() == ICUResourceBundle.FROM_ROOT) )
               return null;
    
            return new UnicodeSet(stringBundle.getString(), UnicodeSet.IGNORE_SPACE | options);
        }catch(MissingResourceException ex){
            if(extype==LocaleData.ES_AUXILIARY){
                return new UnicodeSet();
            }
            throw ex;
        }
    }

    /**
     * Gets the LocaleData object associated with the ULocale specified in locale
     *
     * @param locale    Locale with thich the locale data object is associated.
     * @return          A locale data object.
     * @stable ICU 3.4
     */
    public static final LocaleData getInstance(ULocale locale) {
       LocaleData ld = new LocaleData();
       ld.bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale );
       ld.noSubstitute = false;
       return ld;
    }

    /**
     * Gets the LocaleData object associated with the default locale
     *
     * @return          A locale data object.
     * @stable ICU 3.4
     */
    public static final LocaleData getInstance() {
       return LocaleData.getInstance(ULocale.getDefault());
    }

    /**
     * Sets the "no substitute" behavior of this locale data object.
     *
     * @param setting   Value for the no substitute behavior.  If TRUE,
     *                  methods of this locale data object will return
     *                  an error when no data is available for that method,
     *                  given the locale ID supplied to the constructor.
     * @return 
     * @stable ICU 3.4
     */
    public LocaleData setNoSubstitute(boolean setting) {
       noSubstitute = setting;
       return this;
    }

    /**
     * Gets the "no substitute" behavior of this locale data object.
     *
     * @return          Value for the no substitute behavior.  If TRUE,
     *                  methods of this locale data object will return
     *                  an error when no data is available for that method,
     *                  given the locale ID supplied to the constructor.
     * @stable ICU 3.4
     */
    public boolean getNoSubstitute() {
       return noSubstitute;
    }

    /**
     * Retrieves a delimiter string from the locale data.
     *
     * @param type      The type of delimiter string desired.  Currently,
     *                  the valid choices are QUOTATION_START, QUOTATION_END,
     *                  ALT_QUOTATION_START, or ALT_QUOTATION_END.
     * @return          The desired delimiter string.
     * @stable ICU 3.4
     */
    public String getDelimiter(int type) {
        String [] delimiterTypes = { "quotationStart", 
                                     "quotationEnd", 
                                     "alternateQuotationStart", 
                                     "alternateQuotationEnd" };

        ICUResourceBundle stringBundle = (ICUResourceBundle) bundle.get("delimiters").get(delimiterTypes[type]);

        if ( noSubstitute && (stringBundle.getLoadingStatus() == ICUResourceBundle.FROM_ROOT) )
           return null;

        return new String (stringBundle.getString());
    }

    /**
     * Enumeration for representing the measurement systems.
     * @stable ICU 2.8
     */
    public static final class MeasurementSystem{
        /** 
         * Measurement system specified by Le Syst&#x00E8;me International d'Unit&#x00E9;s (SI)
         * otherwise known as Metric system. 
         * @stable ICU 2.8
         */
        public static final MeasurementSystem SI = new MeasurementSystem(0);
 
        /** 
         * Measurement system followed in the United States of America. 
         * @stable ICU 2.8
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
     * @stable ICU 3.0
     */
    public static final MeasurementSystem getMeasurementSystem(ULocale locale){
        UResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
        UResourceBundle sysBundle = bundle.get(MEASUREMENT_SYSTEM);
        
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
     * @stable ICU 2.8
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
         * @stable ICU 2.8
         */
        public int getHeight(){
            return height;
        }
        /**
         * Returns the width of hte paper
         * @return the width
         * @stable ICU 2.8
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
     * @stable ICU 3.0
     */
    public static final PaperSize getPaperSize(ULocale locale){
        UResourceBundle bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
        UResourceBundle obj = bundle.get(PAPER_SIZE);
        int[] size = obj.getIntVector();
        return new PaperSize(size[0], size[1]);
    }
    
    /**
     * Returns LocaleDisplayPattern for this locale, e.g., {0}({1})
     * @return locale display pattern as a String.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */ 
    public String getLocaleDisplayPattern() {
      if (bundle == null) {
        bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(
                        ICUResourceBundle.ICU_BASE_NAME, ULocale.getDefault());
      }
      ICUResourceBundle locDispBundle = (ICUResourceBundle) bundle.get(LOCALE_DISPLAY_PATTERN);
      String localeDisplayPattern = locDispBundle.getStringWithFallback(PATTERN);
      return localeDisplayPattern;
    }
    
    /**
     * Returns LocaleDisplaySeparator for this locale.
     * @return locale display separator as a char.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */ 
    public String getLocaleSeparator() {
      if (bundle == null) {
        bundle = (ICUResourceBundle)UResourceBundle.getBundleInstance(
                        ICUResourceBundle.ICU_BASE_NAME, ULocale.getDefault());
      }
      ICUResourceBundle locDispBundle = (ICUResourceBundle) bundle.get(LOCALE_DISPLAY_PATTERN);
      String  localeSeparator = locDispBundle.getStringWithFallback(SEPARATOR);
      return localeSeparator;
    }
    
    private static VersionInfo gCLDRVersion = null;
    
    /**
     * Returns the current CLDR version
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public static VersionInfo getCLDRVersion() {
        // fetching this data should be idempotent.
        if(gCLDRVersion == null) {
            // from ZoneMeta.java
            UResourceBundle supplementalDataBundle = UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
            UResourceBundle cldrVersionBundle = supplementalDataBundle.get("cldrVersion");
            gCLDRVersion = VersionInfo.getInstance(cldrVersionBundle.getString());
        }
        return gCLDRVersion;
    }
}
