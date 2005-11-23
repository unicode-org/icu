/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

import java.util.*;

public class PosixToNeutralConverter extends LocaleConverter {
    public static final byte LC_CTYPE           = 0x01;
    public static final byte LC_TIME            = 0x02;
    public static final byte LC_NUMERIC         = 0x04;
    public static final byte LC_MONETARY        = 0x08;
    public static final byte LC_MESSAGES        = 0x10;
    public static final byte LC_MEASUREMENT     = 0x11;
    public static final byte LC_ADDRESS         = 0x12;
    public static final byte LC_PAPER           = 0x13;
    public static final byte LC_NAME            = 0x14;
    public static final byte LC_IDENTIFICATION  = 0x15;
    public static final byte LC_TELEPHONE       = 0x16;
    
    private static final byte[] masks = { 
        LC_CTYPE, LC_TIME, LC_NUMERIC, LC_MONETARY, LC_MESSAGES,
        LC_MEASUREMENT,LC_ADDRESS,LC_PAPER,LC_NAME,LC_IDENTIFICATION,
        LC_TELEPHONE
        };

    private static final String[][] props = {
        //LC_CTYPE
        { 
            "upper", "lower", "alpha", "digit", "space", "cntrl", 
            "punct", "graph", "print", "xdigit", "blank", 
            "toupper", "tolower" 
        },
        //LC_TIME
        {
            "abday", "day", "abmon", "mon", "d_t_fmt", "d_ftm", "t_fmt", 
            "am_pm", "t_fmt_ampm", "era", "era_year", "era_d_fmt", "alt_digits"
        },
        //LC_NUMERIC
        {
            "decimal_point", "thousands_sep", "grouping"
        },
        //LC_MONETARY
        {
            "int_curr_symbol", "currency_symbol", "mon_decimal_point",
            "mon_thousands_sep", "mon_grouping", "positive_sign",
            "negative_sign", "int_frac_digits", "frac_digits", "p_cs_precedes",
            "p_sep_by_space", "n_cs_precedes", "n_sep_by_space", "p_sign_posn",
        },
        //LC_MESSAGES
        {
            "yesexpr", "noexpr"
        },
           
        //LC_MEASUREMENT
        {
           "measurement"
        },
        //LC_ADDRESS
        {
           "copy","postal_fmt","country_name","country_post",
           "country_ab2","country_ab3","country_num","country_car",
           "country_isbn","lang_name","lang_ab","lang_term","lang_lib"
            
        },
        //LC_PAPER
        {
            "height","width","copy"
        },
        //LC_NAME        
        {
            "copy","name_fmt","name_gen","name_miss","name_mr","name_mrs","name_ms"  
        },
        //LC_IDENTIFICATION
        {
            "title","source","address","contact","email","tel","fax","language",
            "territory","revision ","date"     
        },
        //LC_TELEPHONE
        {
            "copy","tel_int_fmt","tel_dom_fmt","int_select","int_prefix" 
        },
        
            
    };
    private final byte flags;
    private final Locale locale;
    private final Locale parentLocale;
    private final String sfileName;
    public PosixToNeutralConverter(byte flags, final Locale locale,final String fileName) {
        this.flags = flags;
        this.locale = locale;
        this.sfileName=fileName;
        
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = "";
        
        int ndx = language.indexOf('_');
        if (ndx >= 0) {
            country = language.substring(ndx+1);
            language = language.substring(0, ndx);
        }
        ndx = country.indexOf('_');
        if (ndx >= 0) {
            variant = country.substring(ndx);
            country = country.substring(0, ndx);
        }
        
        if ("".equals(country)) {
            language = "";
            variant = "";
        } else if ("".equals(variant)) {
            country = "";
        }
        
        parentLocale = new Locale(language, country, variant);
            //{{INIT_CONTROLS
//}}
    }

    protected void convert(final Hashtable result, final Hashtable source) 
            throws ConversionError {
//        convertMESSAGES(result,source);
        writePosixCompData(result,source);
        convertMEASUREMENT(result, source);
        convertCOUNTRYNUMBER(result, source);
        convertCOUNTRYISBNNUMBER(result, source);
        convertLANGUAGELIB(result, source);
        convertPAPERSIZE(result, source);
        convertMONETARY(result, source);
        convertNUMERIC(result, source);
        convertTIME(result, source);
        convertCOLLATE(result, source);
        for (int i = 0; i < masks.length; i++) {
            if ((flags & masks[i]) != 0) {
                for (int j = 0; j < props[i].length; j++) {
                    final String prop = props[i][j];
                    final Object val = source.get(prop);
                    if (val != null) {
                        resultPut(result, prop, val);
                    }
                }
            }
        }
    }
    public String[][] clone2DArr(String[][]str2Darray){
        String[][] newStr2DArr = new String[str2Darray.length][str2Darray[0].length]; 
        for(int i=0; i<str2Darray.length;i++){
            for(int j=0; j<str2Darray[i].length;j++){
                newStr2DArr[i][j] = new String(str2Darray[i][j]);
            }
        }
        return newStr2DArr;           
    }
    public Object[][] clone2DArr(Object[][]str2Darray){
        Object[][] newStr2DArr = new Object[str2Darray.length][str2Darray[0].length]; 
        for(int i=0; i<str2Darray.length;i++){
            for(int j=0; j<str2Darray[i].length;j++){
                newStr2DArr[i][j] = (Object)(new String((String)str2Darray[i][j]));
            }
        }
        return newStr2DArr;           
    }
    public void writePosixCompData(final Hashtable result, final Hashtable source){        
        convertMESSAGES(result,source);
        convertIDENTIFICATION(result,source);
        convertNAMEFORMAT(result,source);
        convertADDRESSFORMAT(result,source);
        convertTELEPHONEFORMAT(result,source);
    }


    private void convertMESSAGES(final Hashtable result, final Hashtable source){
        final String[][]DEFAULT_MESSAGES = (String[][]) getDefault("Messages");
        final String[][] myMessages = clone2DArr(DEFAULT_MESSAGES);

        String value =(String) source.get("yesexpr");
        
        if(value!=null){
             myMessages[0][1]  = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("noexpr");
        if(value != null){
            myMessages[1][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("yesstr");
        if(value != null){
            myMessages[2][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("nostr");
        if(value != null){
            myMessages[3][1] = PosixCollationBuilder.unescape(value);
        }
        resultPut(result,"Messages",myMessages);
    }
    private void convertIDENTIFICATION(final Hashtable result, final Hashtable source){
        final String[][]DEFAULT_MESSAGES = (String[][]) getDefault("Identification");
        final String[][] myIdentification = clone2DArr(DEFAULT_MESSAGES);
                              
        String value =(String) source.get("title");
        if(value!=null){
            myIdentification[0][1] = PosixCollationBuilder.unescape(value);
        }                        
        value = (String) source.get("source");
        if(value != null){
            myIdentification[1][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("address");
        if(value != null){
            myIdentification[2][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("contact");
        if(value != null){
            myIdentification[3][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("email");
        if(value != null){
            myIdentification[4][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("tel");
        if(value != null){

            myIdentification[5][1] = PosixCollationBuilder.unescape(value);
        }
        
        value = (String) source.get("fax");
        if(value != null){
            myIdentification[6][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("language");
        if(value != null){
            myIdentification[7][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("territory");
        if(value != null){
            myIdentification[8][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("audience");
        if(value != null){
            myIdentification[9][1] = PosixCollationBuilder.unescape(value);
        }
        
        value = (String) source.get("application");
        if(value != null){
            myIdentification[10][1] = PosixCollationBuilder.unescape(value);
        }
        else
        value = (String) source.get("abbreviation");
        if(value != null){
            myIdentification[11][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("revision");
        if(value != null){
            myIdentification[12][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("date");
        if(value != null){
            myIdentification[13][1] = PosixCollationBuilder.unescape(value);
        }
        resultPut(result,"Identification",myIdentification);
    }
    private void convertNAMEFORMAT(final Hashtable result, final Hashtable source){
        final String[][]DEFAULT_MESSAGES = (String[][]) getDefault("NameFormat");
        final String[][] myNameFormat = clone2DArr(DEFAULT_MESSAGES);
        String value =(String) source.get("name_mr");
        if(value != null){
            myNameFormat[2][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("name_miss");
        if(value != null){
            myNameFormat[3][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("name_ms");
        if(value != null){
            myNameFormat[6][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("name_mrs");
        if(value != null){
            myNameFormat[4][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("name_gender");
        if(value != null){
            myNameFormat[1][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("name_fmt");
        if(value != null){
            value = PosixCollationBuilder.unescape(value);
            myNameFormat[0][1] = value;
        }
         resultPut(result,"NameFormat",myNameFormat);
    }
    private void convertADDRESSFORMAT(final Hashtable result, final Hashtable source){
        final String[][]DEFAULT_MESSAGES = (String[][]) getDefault("AddressFormat");
        final String[][] myAddressFormat =( clone2DArr(DEFAULT_MESSAGES));
        String value =(String) source.get("postal_fmt");
        if(value != null){
            value = PosixCollationBuilder.unescape(value);
            myAddressFormat[0][1] = value;
        }
         resultPut(result,"AddressFormat",myAddressFormat);
    }
    private void convertTELEPHONEFORMAT(final Hashtable result, final Hashtable source){
        final String[][]DEFAULT_MESSAGES = (String[][]) getDefault("TelephoneFormat");
        final String[][] myTelephoneFormat = clone2DArr(DEFAULT_MESSAGES);
        String value =(String) source.get("tel_int_fmt");
        if(value != null){
            value = PosixCollationBuilder.unescape(value);
            myTelephoneFormat[0][1] = value;
        }
        value = (String) source.get("dom_fmt");
        if(value != null){
            value = PosixCollationBuilder.unescape(value);
            myTelephoneFormat[1][1] = value;
        }
        value = (String) source.get("int_select");
        if(value != null){
            myTelephoneFormat[2][1] = PosixCollationBuilder.unescape(value);
        }
        value = (String) source.get("int_prefix");
        if(value != null){
            myTelephoneFormat[3][1] = PosixCollationBuilder.unescape(value);
        }
         resultPut(result,"TelephoneFormat",myTelephoneFormat);
    }
    private void convertMONETARY(final Hashtable result, final Hashtable source) {
        final String[] DEFAULT_CURRENCY_ELEMENTS = (String[])getDefault("CurrencyElements");
        final String[] elements = (String[])DEFAULT_CURRENCY_ELEMENTS.clone();
        String value = (String)source.get("currency_symbol");
        //Replaceable temp = new Replaceable(value);
       
        if (value != null) {
            elements[0] = PosixCollationBuilder.unescape(value);
        }
        value = (String)source.get("int_curr_symbol");
        if (value != null) {
            elements[1] = PosixCollationBuilder.unescape(value);
        }
        value = (String)source.get("mon_decimal_point");
        if (value != null) {
            elements[2] = PosixCollationBuilder.unescape(value);
        }
        resultPut(result, "CurrencyElements", elements);
    }
    

    private void convertMEASUREMENT(final Hashtable result, final Hashtable source){
        final String DEFAULT_MEASUREMENT =(String)getDefault("Measurement");
        String elements = (String)DEFAULT_MEASUREMENT;
        String value =(String) source.get("measurement");
        if(value!=null){
            if(value.equals("1"))
                elements = "Metric";
            else if(value.equals("2"))
                elements = "American";
            else if(value.equals("3"))
                elements = "Other";
        }
        resultPut(result,"Measurement",elements);
    }
    private void convertCOUNTRYNUMBER (final Hashtable result, final Hashtable source){
        final String DEFAULT_COUNTRYNUMBER =(String)getDefault("CountryNumber");
        String elements = (String)DEFAULT_COUNTRYNUMBER;
        String value =(String) source.get("country_num");
        if(value!=null){
            elements = value;
        }
        resultPut(result,"CountryNumber",elements);
    }
    private void convertCOUNTRYISBNNUMBER (final Hashtable result, final Hashtable source){
        final String DEFAULT_COUNTRYNUMBER =(String)getDefault("CountryISBNNumber");
        String elements = new String((String)DEFAULT_COUNTRYNUMBER);
        String value =(String) source.get("country_num");
        if(value!=null){
            elements = value;
        }
        resultPut(result,"CountryISBNNumber",elements);
    }
    private void convertLANGUAGELIB (final Hashtable result, final Hashtable source){
        final String DEFAULT_LANGUAGELIB =(String)getDefault("LanguageLibraryUse");
        String elements =new String( (String)DEFAULT_LANGUAGELIB);
        String value =(String) source.get("lang_lib");
        if(value!=null){
            elements = value;
        }
        resultPut(result,"LanguageLibraryUse",elements);
    }
    private void convertPAPERSIZE (final Hashtable result, final Hashtable source){
        final String[][] DEFAULT_PAPERSIZE=(String[][]) getDefault("PaperSize");
        final String[][] elements=clone2DArr(DEFAULT_PAPERSIZE);
        String value=(String) source.get("height");
        if (value!=null){
            elements[0][1]= value;
        }
        value =  (String) source.get("width");
        if(value!=null){
            elements[1][1] = value;
        }
        elements[2][1]=(String)"mm";
        
        resultPut(result,"PaperSize",elements);
    }
    
   /* private void convertIDENTIFICATION(final Hashtable result, final Hashtable source){
        final String[][]DEFAULT_IDENTIFICATION =(String[][]) getDefault("Identification");
        final String[][] elements = (String[][])DEFAULT_IDENTIFICATION.cloane();
        
    }*/
    private void convertNUMERIC(final Hashtable result, final Hashtable source) {
        //Build NumberElements data array
        final String[] DEFAULT_NUMBER_ELEMENTS = (String[])getDefault("NumberElements");
        //Set new number elements to a copy of the default value from the parent.
        final String[] elements = (String[])DEFAULT_NUMBER_ELEMENTS.clone();
        //put decimal seperator into the NumberElements array
        String decimalPoint = (String)source.get("decimal_point");
        if (decimalPoint != null) { //if we have a POSIX value, stomp on the default
            elements[0] = decimalPoint;
        }

        //put thousands seperator into the NumberElements array
        String thousandsSep = (String)source.get("thousands_sep");
        if (thousandsSep != null) { //if we have a POSIX value, stomp on the default
            elements[1] = thousandsSep;
        }
        
        //Add NumberElements to result
        resultPut(result, "NumberElements", elements);

        //use canonical decimal and thousands seperators for patterns
        decimalPoint = ".";
        thousandsSep = ",";
        
        //NumberPatterns
        final String[] DEFAULT_NUMBER_PATTERNS = (String[])getDefault("NumberPatterns");
        final String[] patterns = (String[])DEFAULT_NUMBER_PATTERNS.clone();
        
        final String patternDigit = elements[5];
        final String zeroDigit = elements[4];
        final String negativeSign = elements[6];
        final String percentSign = elements[3];

        String groupingString;
        Object groupingObj = source.get("grouping");
        boolean isStrArray = groupingObj instanceof String[];
        if (isStrArray) {
            groupingString = ((String[])groupingObj)[0];
        } else {
            groupingString = (String)groupingObj;
        }
        
        if (groupingString == null) {
            patterns[0] = replace(patterns[0], elements[0], "<DECIMAL>");
            patterns[0] = replace(patterns[0], elements[1], "<THOUSAND>");
            patterns[0] = replace(patterns[0], "<DECIMAL>", decimalPoint);
            patterns[0] = replace(patterns[0], "<THOUSAND>", thousandsSep);

            patterns[2] = replace(patterns[2], elements[1], "<THOUSAND>");
            patterns[2] = replace(patterns[2], "<THOUSAND>", thousandsSep);
        } else {
            
            /* grouping:
             * Defines the size of each group of digits in formatted monetary 
             * quantities. The operand for the grouping keyword consists of a 
             * sequence of semicolon-separated integers. Each integer specifies the 
             * number of digits in a group. The initial integer defines the size of 
             * the group immediately to the left of the decimal delimiter. 
             * The following integers define succeeding groups to the left of the 
             * previous group. If the last integer is not -1, the size of the 
             * previous group (if any) is used repeatedly for the remainder of the 
             * digits. If the last integer is -1, no further grouping is performed.
             * Grouping Value    Formatted Value
             *   3;-1               123456'789
             *   3                  123'456'789
             *   3;2;-1             1234'56'789
             *   3;2                12'34'56'789
             *   -1                 123456789
             */
            //for a grouping of 5
            //#<thousandsSep>####0<decimalPoint>#####
            final StringBuffer pattern = new StringBuffer();
            int semiColonIndex = groupingString.indexOf(';') ;
            int grouping = 0;
            if(semiColonIndex < 0 ){
                grouping = Integer.parseInt(groupingString);    
                pattern.append(patternDigit);
                pattern.append(thousandsSep);
                for (int i = Math.max(1, grouping - 1); i > 0; i--) {
                    pattern.append(patternDigit);
                }
                pattern.append(zeroDigit);
                pattern.append(decimalPoint);
                for (int i = Math.max(1, grouping - 1); i >= 0; i--) {
                    pattern.append(patternDigit);
                }
            }else{
            /*    int prevIndex = 0;
                int[] groupings = new int[4];
                int j =0;
                
                for(int i=0; i<groupingString.length(); i++){
                    if(groupingString.charAt(i)==';'){
                        groupings[j++] = Integer.parseInt(groupingString.substring(prevIndex,i));
                        prevIndex = i;
                    }
                }
                for(int i = j-1 ;i<-1; i--){
                    if(groupings[i] == -1){
                        pattern.append(p);
                    }else{
                        
                    }    
                }
              */
              System.err.println("WARNING: unimplemented pattern parser. Pattern: " + groupingString);
            }
            
            final String patternString = pattern.toString();
            patterns[0] = patternString + ";" + negativeSign + patternString;
            
            pattern.setLength(0);
            pattern.append(patternDigit);
            pattern.append(thousandsSep);
            for (int i = Math.max(1, grouping - 1); i > 0; i--) {
                pattern.append(patternDigit);
            }
            pattern.append(zeroDigit);
            pattern.append(percentSign);            
            patterns[2] = pattern.toString();

           // final String[] currencyElements = ((String[])getDefault("CurrencyElements"));
            String currency_symbol = (String)source.get("currency_symbol");
            currency_symbol = (currency_symbol != null) ? currency_symbol : (String)source.get("int_curr_symbol");
            currency_symbol = (currency_symbol != null) ? currency_symbol : "";
            
            String mon_decimal_point = (String)source.get("mon_decimal_point");
            mon_decimal_point = (mon_decimal_point != null) ? mon_decimal_point : "";

            String mon_thousands_sep = (String)source.get("mon_thousands_sep");
            mon_thousands_sep = (mon_thousands_sep != null) ? mon_thousands_sep : "";

            String mon_grouping_string;
            /*
             * mon_grouping:
             * 
             * Specifies a string that defines the size of each group of digits 
             * in formatted monetary quantities. The operand for the 
             * mon_grouping keyword consists of a sequence of 
             * semicolon-separated integers. Each integer specifies the number 
             * of digits in a group. The initial integer defines the size of the
             * group immediately to the left of the decimal delimiter. 
             * The following integers define succeeding groups to the left of 
             * the previous group. If the last integer is not -1, the size of 
             * the previous group (if any) is repeatedly used for the remainder 
             * of the digits. If the last integer is -1, no further grouping is 
             * performed.
             * 
             * Value    Formatted Value
             *  3;-1    123456'789
             *  3       123'456'789
             *  3;2;-1  1234'56'789
             *  3;2     12'34'56'789
             *   -1     123456789 
             */ 
            final Object monGroupingObj = source.get("mon_grouping");
            if (monGroupingObj instanceof String[]) {
                mon_grouping_string = ((String[])monGroupingObj)[0];
            } else {
                mon_grouping_string = (String)monGroupingObj;
            }
            final int mon_grouping = (mon_grouping_string == null) ? grouping : Integer.parseInt(mon_grouping_string);
            
            final String frac_digits_string = (String)source.get("frac_digits");
            final int frac_digits = (frac_digits_string == null) ? mon_grouping : Integer.parseInt(frac_digits_string);
            
            String positive_sign = (String)source.get("positive_sign");
            positive_sign = (positive_sign != null) ? positive_sign : "";
            
            String negative_sign = (String)source.get("negative_sign");
            negative_sign = (negative_sign != null) ? negative_sign : "";
            
            String p_sign_posn = (String)source.get("p_sign_posn");
            p_sign_posn = (p_sign_posn != null) ? p_sign_posn : "";
            
            String n_sign_posn = (String)source.get("n_sign_posn");
            n_sign_posn = (n_sign_posn != null) ? n_sign_posn : "";

            final boolean p_cs_precedes = !"0".equals(source.get("p_cs_precedes"));
            final String p_sep_by_space = (String)source.get("p_sep_by_space");
            final boolean n_cs_precedes = !"0".equals(source.get("n_cs_precedes"));
            final String n_sep_by_space = (String)source.get("n_sep_by_space");

            pattern.setLength(0);
            
/*            
            patterns[1] =
                createPatternString(currency_symbol, mon_decimal_point, mon_thousands_sep, mon_grouping, frac_digits,
                    positive_sign, p_cs_precedes, p_sep_by_space, p_sign_posn, patternDigit, zeroDigit)
                + ";"
                + createPatternString(currency_symbol, mon_decimal_point, mon_thousands_sep, mon_grouping, frac_digits,
                    negative_sign, n_cs_precedes, n_sep_by_space, n_sign_posn, patternDigit, zeroDigit);
*/
            
/*            patterns[1] =
                createPatternString(currency_symbol, ".", ",", mon_grouping, frac_digits,
                    positive_sign, p_cs_precedes, p_sep_by_space, p_sign_posn, patternDigit, zeroDigit)
                + ";"
                + createPatternString(currency_symbol, ".", ",", mon_grouping, frac_digits,
                    negative_sign, n_cs_precedes, n_sep_by_space, n_sign_posn, patternDigit, zeroDigit);
*/
            patterns[1] =
                createPatternString(".", ",", mon_grouping, frac_digits,
                    positive_sign, p_cs_precedes, p_sep_by_space, p_sign_posn, patternDigit, zeroDigit)
                + ";"
                + createPatternString(".", ",", mon_grouping, frac_digits,
                    negative_sign, n_cs_precedes, n_sep_by_space, n_sign_posn, patternDigit, zeroDigit);

        }
        resultPut(result, "NumberPatterns", patterns);
    }

    /**
     * This routine creates currency formats from all the posix stuff.
     */
    private String createPatternString(String decimal, String thousands, int grouping, int fracDigits,
            String sign, boolean preceeds, String sep_by_space, String sign_posn, String digit,
            String requiredDigit) {
        StringBuffer buffer = new StringBuffer();
        final String currency = "\u00A4";
        if ("2".equals(sep_by_space) && ("".equals(currency) || "".equals(sign))) {
            sep_by_space = "0";
        }
        if ("1".equals(sep_by_space) && "".equals(currency)) {
            sep_by_space = "0";
        }
        
        final String sign_currency_seperator = ("2".equals(sep_by_space)) ? " " : "";
        final String quantity_currency_seperator = ("1".equals(sep_by_space)) ? " " : "";
        
        if ("0".equals(sign_posn)) {
            buffer.append('(');
        }
        if ("1".equals(sign_posn)) {
            buffer.append(sign);
            if (preceeds) {
                buffer.append(sign_currency_seperator);
            }
        }
        if (preceeds) {
            if ("3".equals(sign_posn)) {
                buffer.append(sign);
                buffer.append(sign_currency_seperator);
            }
            buffer.append(currency);
            if ("4".equals(sign_posn)) {
                buffer.append(sign_currency_seperator);
                buffer.append(sign);
            }
            buffer.append(quantity_currency_seperator);
        }       
        
        buffer.append(digit);
        if (grouping > 0) {
            buffer.append(thousands);
            for (int i = grouping-1; i > 0; i--) {
                buffer.append(digit);
            }
        }
        buffer.append(requiredDigit);
        buffer.append(decimal);
        if (fracDigits > 0) {
            for (int i = fracDigits-1; i >= 0; i--) {
                buffer.append(requiredDigit);
            }
        } else {
            buffer.append(digit);
        }
        
        if (!preceeds) {
            buffer.append(quantity_currency_seperator);
            if ("1".equals(sign_posn)) {
                buffer.append(sign_currency_seperator);
            }
            if ("3".equals(sign_posn)) {
                buffer.append(sign);
                buffer.append(sign_currency_seperator);
            }
            buffer.append(currency);
            if ("4".equals(sign_posn)) {
                buffer.append(sign_currency_seperator);
                buffer.append(sign);
            }
        }       
        if ("2".equals(sign_posn)) {
            buffer.append(sign_currency_seperator);
            buffer.append(sign);
        }
        if ("0".equals(sign_posn)) {
            buffer.append(')');
        }
        
        return buffer.toString();
    }
    
    private void convertTIME(Hashtable result, Hashtable source) {
        resultPut(result, "DayNames", source.get("day"));
        resultPut(result, "DayAbbreviations", source.get("abday"));
        String[] temp = (String[])source.get("am_pm");
        if (temp != null) {
            final String[] defaultAMPM = (String[])getDefault("AmPmMarkers");
            if ("".equals(temp[0])) temp[0] = defaultAMPM[0];
            if ("".equals(temp[1])) temp[1] = defaultAMPM[1];
            resultPut(result, "AmPmMarkers", temp);
        }
        
        temp = (String[])source.get("mon");
        if (temp != null) {
            //add empty 13th month
            String[] newTemp = new String[13];
            System.arraycopy(temp, 0, newTemp, 0, 12);
            newTemp[12] = "";
            resultPut(result, "MonthNames", newTemp);
        }
        temp = (String[])source.get("abmon");
        if (temp != null) {
            //add empty 13th month
            String[] newTemp = new String[13];
            System.arraycopy(temp, 0, newTemp, 0, 12);
            newTemp[12] = "";
            resultPut(result, "MonthAbbreviations", newTemp);
        }
        
        final String t_fmt_ampm = (String)source.get("t_fmt_ampm");
        final String t_fmt = (String)source.get("t_fmt");
        final String d_t_fmt = (String)source.get("d_t_fmt");
        final String d_fmt = (String)source.get("d_fmt");
        final String nlldate = (String)source.get("nlldate");   //non-standard IBM thing
        
        final String DEFAULT_DATETIME_ELEMENTS[] = (String[])getDefault("DateTimePatterns");
        final String[] elements = (String[])DEFAULT_DATETIME_ELEMENTS.clone();
        final String X_pattern = elements[3];
        final String x_pattern = elements[7];
        final String c_pattern = elements[4] + " " + elements[0];
        
        elements[0] = (t_fmt_ampm != null && t_fmt_ampm.length() > 0) ? t_fmt_ampm : t_fmt;
        elements[1] = (t_fmt != null) ? t_fmt : t_fmt_ampm;
        elements[2] = (t_fmt != null) ? t_fmt : t_fmt_ampm;
        elements[3] = (t_fmt != null) ? t_fmt : t_fmt_ampm;
        
        String longishDateFormat = (nlldate != null) ? nlldate : d_fmt;
        if (d_t_fmt != null) {
            if (t_fmt != null) {
                //try to build a detailed data format by taking the 
                //date-time format and removing the time portion
                int ndx = d_t_fmt.indexOf(t_fmt);
                if (ndx >= 0) {
                    if (ndx < (d_t_fmt.length() - t_fmt.length())/2) {
                        elements[8] = "{0} {1}";
                    }
                
                    longishDateFormat = replace(d_t_fmt, t_fmt, "");
                    longishDateFormat = replace(longishDateFormat, "%Z", "");
                    longishDateFormat = replace(longishDateFormat, "  ", " ");
                    longishDateFormat = replace(longishDateFormat, "  ", " ");
                    if (longishDateFormat.charAt(0) == ' ') {
                        longishDateFormat = longishDateFormat.substring(1);
                    }
                    longishDateFormat.trim();
                }
            }
        }
        elements[4] = longishDateFormat;
        elements[5] = d_t_fmt;
        elements[6] = (nlldate != null) ? nlldate : d_fmt;
        elements[7] = d_fmt;
        
        for (int i = 0; i < 8; i++) {
            if (elements[i] != null) {
                elements[i] = convertFormats(elements[i], X_pattern, x_pattern, c_pattern);
            } else {
                elements[i] = DEFAULT_DATETIME_ELEMENTS[i];
            }
        }
        resultPut(result, "DateTimePatterns", elements);
    }
   
    private String convertFormats(String pattern, String X_pattern, 
            String x_pattern, String c_pattern) {
        
        String tpattern = PosixCollationBuilder.unescape(pattern);
        StringBuffer result = new StringBuffer();
        boolean singleDigit = false;
        for (int i = 0; i < tpattern.length(); i++) {
            char c = tpattern.charAt(i);
            if (c != '%') {
                result.append(c);
            } else {
                singleDigit = false;
                i++;
                c = tpattern.charAt(i);
                switch (c) {
                case 'a':
                    result.append("EEE");
                    break;
                case 'A':
                    result.append("EEEE");
                    break;
                case 'b':
                    result.append("MMM");
                    break;
                case 'B':
                    result.append("MMMM");
                    break;
                case 'c':
                    result.append(c_pattern);
                    break;
                case 'C':   //** hey {jf} - this is supposed to be the century only.
                    result.append("YYYY");
                    break;
                case 'd':
                    if(singleDigit == true){
                        result.append("d");
                    }else{
                        result.append("dd");
                    }
                    break;
                case 'D':
                    result.append("mm/dd/yy");
                    break;
                case 'e':
                    if(singleDigit == true){
                        result.append("d");
                    }else{
                        result.append("dd");
                    }
                    break;
                case 'h':
                    result.append("MMM");
                    break;
                case 'H':
                    if(singleDigit == true){
                        result.append("H");
                    }else{
                        result.append("HH");
                    }
                    break;
                case 'I':
                    if(singleDigit == true){
                        result.append("h");
                    }else{
                        result.append("hh");
                    }
                    break;
                case 'j':
                    result.append("DDD");
                    break;
                case 'm':
                    if(singleDigit == true){
                        result.append("M");
                    }else{
                        result.append("MM");
                    }
                    result.append("MM");
                    break;
                case 'M':
                    if(singleDigit == true){
                        result.append("m");
                    }else{
                        result.append("mm");
                    };
                    break;
                case 'n':
                    result.append('\n');
                    break;
                case 'p':
                    result.append("aa");
                    break;
                case 'r':
                    result.append(convertFormats("%I:%M:%S %p", X_pattern, x_pattern, c_pattern));
                    break;
                case 'S':
                    result.append("ss");
                    break;
                case 't':
                    result.append('\t');
                    break;
                case 'T':
                    result.append(convertFormats("%I:%M:%S", X_pattern, x_pattern, c_pattern));
                    break;
                case 'U':
                    result.append("ww");
                    break;
                case 'w':
                    result.append("E");
                    break;
                case 'W':
                    result.append("ww");
                    break;
                case 'x':
                    result.append(x_pattern);
                    break;
                case 'X':
                    result.append(X_pattern);
                    break;
                case 'y':
                    result.append("yy");
                    break;
                case 'Y':
                    result.append("yyyy");
                    break;
                case 'Z':
                    result.append("z");
                    break;
                case '%':
                    result.append("%");
                    break;
                case '.':
                    char c1 = tpattern.charAt(i+1);
                    if(c1=='1'){
                        singleDigit = true;
                        i++;
                        break; 
                    }
                         
                default:
                    result.append('%');
                    result.append(c);
                    break;
                }
            }
        }
        return result.toString();
    }
    
    private void convertCOLLATE(Hashtable result, Hashtable source) {
        String[] sortOrder = (String[])source.get("sort_order");    
        final Object[][] DEFAULT_COLLATION=(Object[][]) getDefault("collations");
        final Object[][] elements=(Object[][])clone2DArr(DEFAULT_COLLATION);        
        if (sortOrder != null) {
            if (!"forward".equals(sortOrder[0])) {
                System.err.println("ERROR: Unsupported primary sort order: "+sortOrder[0]);
            }
            if (sortOrder.length == 2 && !"forward".equals(sortOrder[1]) && !"backward".equals(sortOrder[1])) {
                System.err.println("ERROR: Unsupported secondary sort order: "+sortOrder[1]);
            }
            if (sortOrder.length == 3 && !"forward".equals(sortOrder[2])) {
                System.err.println("ERROR: Unsupported tertiary sort order: "+sortOrder[2]);
            }
            if (sortOrder.length > 3) {
                System.err.println("WARNING: Sort levels of order greater than three ignored.");
            }
        }
        
        PosixCollationBuilder.CollationRule[] ruleSource = 
            (PosixCollationBuilder.CollationRule[])source.get("posix_sort_rules");

        if (ruleSource != null) {
                //allocate a list of collationItems.  Add an extra entry for secondary ordering
            CollationItem[] rules = new CollationItem[ruleSource.length+1];
            PosixCollationBuilder.CollationRule prevRule = null;
                //add all the rules for non-expanding characters
            int i = 0;
            for (int ndx = 0; ndx < ruleSource.length; ndx++) {
                PosixCollationBuilder.CollationRule rule = ruleSource[ndx];
                    //add non-expanding characters to the sort list
                if (rule.getSize() <= 1) {
                    int diff;
                    if (prevRule == null) {
                        //if it's the first rule, don't compare to anything,
                        //seek back so it can be appended to the default rules
                        rules[i] = new CollationItem(rule.getSymbol());
                    } else {
                        //compare to previous item
                        diff = prevRule.compare(rule);
                        rules[i] = new CollationItem(diff, rule.getSymbol());
                    }
                    rules[i++].setComment(rule.getSource());
                    prevRule = rule;
                }
            }
                //add rules for expanding characters
            String prevSeek = null;
            prevRule = null;
            for (int ndx = 0; ndx < ruleSource.length; ndx++) {
                PosixCollationBuilder.CollationRule rule = ruleSource[ndx];
                if (rule.getSize() > 1) {
                        //find out what this character expands to
                    String seek = rule.getExpansion();
                    if (!seek.equals(prevSeek)) {
                            //if it's not the same as the previous character
                            //then seek to the first character of the expansion
                            //and compare to that
                        PosixCollationBuilder.CollationRule seekRule = rule.seeksToRule();
                        rules[i] = new CollationItem(
                                seekRule.compare(rule), rule.getSymbol(), rule.getExpansion());
                        prevSeek = seek;
                    } else if (prevRule != null) {
                            //it expands to the same characters as the previous expansion,
                            //so compare to the previous expansion
                        rules[i] = new CollationItem(
                                prevRule.compare(rule), rule.getSymbol(), prevRule.getSymbol());
                    } else {
                            //The unlikely case that the first character will
                            //be an expanding character...I don't think
                            //this is even possible...
                        rules[i] = new CollationItem(rule.getSymbol());
                    }
                    rules[i++].setComment(rule.getSource());
                }
                prevRule = rule;
            }
            if (sortOrder.length>0 && "backward".equals(sortOrder[0])) {
                elements[1][1] = "true";
            } else {
                elements[1][1] = "false";
            }
            elements[2][1]=(Object)rules;
            resultPut(result, "collations", elements);
        }
    }
        
    private void resultPut(Hashtable resultTable, String tag, Object value) {
        if (value == null) return;
        resultTable.put(tag, value);
    }
    
    private Object getDefault(String desiredResource) {
        return getParentBundle().getObject(desiredResource);
    }
    
    private ResourceBundle getParentBundle() {
        return ResourceBundle.getBundle("com.ibm.icu.dev.tool.localeconverter.myLocaleElements", parentLocale);
    }
    
    private String replace(String source, String target, String replacement) {
        if (target.equals(replacement)) {
            return source;
        } else {
            StringBuffer result = new StringBuffer();
            int lastNdx = 0;
            int ndx = source.indexOf(target);
            while (ndx >= 0) {
                result.append(source.substring(lastNdx, ndx));
                result.append(replacement);
                ndx += target.length();
                lastNdx = ndx;
                ndx = source.indexOf(target, ndx);
            }
            result.append(source.substring(lastNdx));
            return result.toString();
        }
    }
//{{DECLARE_CONTROLS
//}}
}