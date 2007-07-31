/*
 *******************************************************************************
 * Copyright (C) 2003-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;

/**
 *
 * IDNA API implements the IDNA protocol as defined in the <a href="http://www.ietf.org/rfc/rfc3490.txt">IDNA RFC</a>.
 * The draft defines 2 operations: ToASCII and ToUnicode. Domain labels 
 * containing non-ASCII code points are required to be processed by
 * ToASCII operation before passing it to resolver libraries. Domain names
 * that are obtained from resolver libraries are required to be processed by
 * ToUnicode operation before displaying the domain name to the user.
 * IDNA requires that implementations process input strings with 
 * <a href="http://www.ietf.org/rfc/rfc3491.txt">Nameprep</a>, 
 * which is a profile of <a href="http://www.ietf.org/rfc/rfc3454.txt">Stringprep</a> , 
 * and then with <a href="http://www.ietf.org/rfc/rfc3492.txt">Punycode</a>. 
 * Implementations of IDNA MUST fully implement Nameprep and Punycode; 
 * neither Nameprep nor Punycode are optional.
 * The input and output of ToASCII and ToUnicode operations are Unicode 
 * and are designed to be chainable, i.e., applying ToASCII or ToUnicode operations
 * multiple times to an input string will yield the same result as applying the operation
 * once.
 * ToUnicode(ToUnicode(ToUnicode...(ToUnicode(string)))) == ToUnicode(string) 
 * ToASCII(ToASCII(ToASCII...(ToASCII(string))) == ToASCII(string).
 * 
 * @author Ram Viswanadha
 * @stable ICU 2.8
 */
public final class IDNA {

    /* IDNA ACE Prefix is "xn--" */
    private static char[] ACE_PREFIX                = new char[]{ 0x0078,0x006E,0x002d,0x002d } ;
    //private static final int ACE_PREFIX_LENGTH      = ACE_PREFIX.length;

    private static final int MAX_LABEL_LENGTH       = 63;
    private static final int HYPHEN                 = 0x002D;
    private static final int CAPITAL_A              = 0x0041;
    private static final int CAPITAL_Z              = 0x005A;
    private static final int LOWER_CASE_DELTA       = 0x0020;
    private static final int FULL_STOP              = 0x002E;
    private static final int MAX_DOMAIN_NAME_LENGTH = 255;
    /** 
     * Option to prohibit processing of unassigned codepoints in the input and
     * do not check if the input conforms to STD-3 ASCII rules.
     * 
     * @see  #convertToASCII #convertToUnicode
     * @stable ICU 2.8
     */
    public static final int DEFAULT             = 0x0000;
    /** 
     * Option to allow processing of unassigned codepoints in the input
     * 
     * @see  #convertToASCII #convertToUnicode
     * @stable ICU 2.8
     */
    public static final int ALLOW_UNASSIGNED    = 0x0001;
    /** 
     * Option to check if input conforms to STD-3 ASCII rules
     * 
     * @see #convertToASCII #convertToUnicode
     * @stable ICU 2.8
     */
    public static final int USE_STD3_RULES      = 0x0002;
    
    // static final singleton object that is initialized
    // at class initialization time, hence guaranteed to
    // be initialized and thread safe
    private static final IDNA singleton  = new IDNA();
    
    // The NamePrep profile object
    private StringPrep namePrep;
    
    /* private constructor to prevent construction of the object */
    private IDNA(){
        try{
           InputStream stream = ICUData.getRequiredStream(ICUResourceBundle.ICU_BUNDLE+"/uidna.spp");
           namePrep = new StringPrep(stream);
           stream.close();
        }catch (IOException e){
            throw new MissingResourceException(e.toString(),"","");
        }
    }
    
    private static boolean startsWithPrefix(StringBuffer src){
        boolean startsWithPrefix = true;

        if(src.length() < ACE_PREFIX.length){
            return false;
        }
        for(int i=0; i<ACE_PREFIX.length;i++){
            if(toASCIILower(src.charAt(i)) != ACE_PREFIX[i]){
                startsWithPrefix = false;
            }
        }
        return startsWithPrefix;
    }

    private static char toASCIILower(char ch){
        if(CAPITAL_A <= ch && ch <= CAPITAL_Z){
            return (char)(ch + LOWER_CASE_DELTA);
        }
        return ch;
    }

    private static StringBuffer toASCIILower(StringBuffer src){
        StringBuffer dest = new StringBuffer();
        for(int i=0; i<src.length();i++){
            dest.append(toASCIILower(src.charAt(i)));
        }
        return dest;
    }

    private static int compareCaseInsensitiveASCII(StringBuffer s1, StringBuffer s2){
        char c1,c2;
        int rc;
        for(int i =0;/* no condition */;i++) {
            /* If we reach the ends of both strings then they match */
            if(i == s1.length()) {
                return 0;
            }

            c1 = s1.charAt(i);
            c2 = s2.charAt(i);
        
            /* Case-insensitive comparison */
            if(c1!=c2) {
                rc=toASCIILower(c1)-toASCIILower(c2);
                if(rc!=0) {
                    return rc;
                }
            }
        }
    }
   
    private static int getSeparatorIndex(char[] src,int start, int limit){
        for(; start<limit;start++){
            if(isLabelSeparator(src[start])){
                return start;
            }
        }
        // we have not found the separator just return length
        return start;
    }
    
    /*
    private static int getSeparatorIndex(UCharacterIterator iter){
        int currentIndex = iter.getIndex();
        int separatorIndex = 0;
        int ch;
        while((ch=iter.next())!= UCharacterIterator.DONE){
            if(isLabelSeparator(ch)){
                separatorIndex = iter.getIndex();
                iter.setIndex(currentIndex);
                return separatorIndex;
            }
        }
        // reset index
        iter.setIndex(currentIndex);
        // we have not found the separator just return the length
       
    }
    */
    

    private static boolean isLDHChar(int ch){
        // high runner case
        if(ch>0x007A){
            return false;
        }
        //[\\u002D \\u0030-\\u0039 \\u0041-\\u005A \\u0061-\\u007A]
        if( (ch==0x002D) || 
            (0x0030 <= ch && ch <= 0x0039) ||
            (0x0041 <= ch && ch <= 0x005A) ||
            (0x0061 <= ch && ch <= 0x007A)
          ){
            return true;
        }
        return false;
    }
    
    /**
     * Ascertain if the given code point is a label separator as 
     * defined by the IDNA RFC
     * 
     * @param ch The code point to be ascertained
     * @return true if the char is a label separator
     * @stable ICU 2.8
     */
    private static boolean isLabelSeparator(int ch){
        switch(ch){
            case 0x002e:
            case 0x3002:
            case 0xFF0E:
            case 0xFF61:
                return true;
            default:
                return false;           
        }
    }
       
    /**
     * This function implements the ToASCII operation as defined in the IDNA RFC.
     * This operation is done on <b>single labels</b> before sending it to something that expects
     * ASCII names. A label is an individual part of a domain name. Labels are usually
     * separated by dots; e.g." "www.example.com" is composed of 3 labels 
     * "www","example", and "com".
     *
     * @param src       The input string to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @stable ICU 2.8
     */    
    public static StringBuffer convertToASCII(String src, int options)
        throws StringPrepParseException{
        UCharacterIterator iter = UCharacterIterator.getInstance(src);
        return convertToASCII(iter,options);
    }
    
    /**
     * This function implements the ToASCII operation as defined in the IDNA RFC.
     * This operation is done on <b>single labels</b> before sending it to something that expects
     * ASCII names. A label is an individual part of a domain name. Labels are usually
     * separated by dots; e.g." "www.example.com" is composed of 3 labels 
     * "www","example", and "com".
     *
     * @param src       The input string as StringBuffer to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @stable ICU 2.8
     */
    public static StringBuffer convertToASCII(StringBuffer src, int options)
        throws StringPrepParseException{
        UCharacterIterator iter = UCharacterIterator.getInstance(src);
        return convertToASCII(iter,options);
    }
    
    /**
     * This function implements the ToASCII operation as defined in the IDNA RFC.
     * This operation is done on <b>single labels</b> before sending it to something that expects
     * ASCII names. A label is an individual part of a domain name. Labels are usually
     * separated by dots; e.g." "www.example.com" is composed of 3 labels 
     * "www","example", and "com".
     *
     * @param src       The input string as UCharacterIterator to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @stable ICU 2.8
     */
    public static StringBuffer convertToASCII(UCharacterIterator src, int options)
                throws StringPrepParseException{
        
        boolean[] caseFlags = null;
    
        // the source contains all ascii codepoints
        boolean srcIsASCII  = true;
        // assume the source contains all LDH codepoints
        boolean srcIsLDH = true; 

        //get the options
        boolean useSTD3ASCIIRules = ((options & USE_STD3_RULES) != 0);
        int ch;
        // step 1
        while((ch = src.next())!= UCharacterIterator.DONE){
            if(ch> 0x7f){
                srcIsASCII = false;
            }
        }
        int failPos = -1;
        src.setToStart();
        StringBuffer processOut = null;
        // step 2 is performed only if the source contains non ASCII
        if(!srcIsASCII){
            // step 2
            processOut = singleton.namePrep.prepare(src, options);
        }else{
            processOut = new StringBuffer(src.getText());
        }
        int poLen = processOut.length();
        
        if(poLen==0){
            throw new StringPrepParseException("Found zero length lable after NamePrep.",StringPrepParseException.ZERO_LENGTH_LABEL);
        }
        StringBuffer dest = new StringBuffer();
        
        // reset the variable to verify if output of prepare is ASCII or not
        srcIsASCII = true;
        
        // step 3 & 4
        for(int j=0;j<poLen;j++ ){
            ch=processOut.charAt(j);
            if(ch > 0x7F){
                srcIsASCII = false;
            }else if(isLDHChar(ch)==false){
                // here we do not assemble surrogates
                // since we know that LDH code points
                // are in the ASCII range only
                srcIsLDH = false;
                failPos = j;
            }
        }
    
        if(useSTD3ASCIIRules == true){
            // verify 3a and 3b
            if( srcIsLDH == false /* source contains some non-LDH characters */
                || processOut.charAt(0) ==  HYPHEN 
                || processOut.charAt(processOut.length()-1) == HYPHEN){

                /* populate the parseError struct */
                if(srcIsLDH==false){
                     throw new StringPrepParseException( "The input does not conform to the STD 3 ASCII rules",
                                              StringPrepParseException.STD3_ASCII_RULES_ERROR,
                                              processOut.toString(),
                                             (failPos>0) ? (failPos-1) : failPos);
                }else if(processOut.charAt(0) == HYPHEN){
                    throw new StringPrepParseException("The input does not conform to the STD 3 ASCII rules",
                                              StringPrepParseException.STD3_ASCII_RULES_ERROR,processOut.toString(),0);
     
                }else{
                     throw new StringPrepParseException("The input does not conform to the STD 3 ASCII rules",
                                              StringPrepParseException.STD3_ASCII_RULES_ERROR,
                                              processOut.toString(),
                                              (poLen>0) ? poLen-1 : poLen);

                }
            }
        }
        if(srcIsASCII){
            dest =  processOut;
        }else{
            // step 5 : verify the sequence does not begin with ACE prefix
            if(!startsWithPrefix(processOut)){

                //step 6: encode the sequence with punycode
                caseFlags = new boolean[poLen];

                StringBuffer punyout = Punycode.encode(processOut,caseFlags);

                // convert all codepoints to lower case ASCII
                StringBuffer lowerOut = toASCIILower(punyout);

                //Step 7: prepend the ACE prefix
                dest.append(ACE_PREFIX,0,ACE_PREFIX.length);
                //Step 6: copy the contents in b2 into dest
                dest.append(lowerOut);
            }else{

                throw new StringPrepParseException("The input does not start with the ACE Prefix.",
                                         StringPrepParseException.ACE_PREFIX_ERROR,processOut.toString(),0);
            }
        }
        if(dest.length() > MAX_LABEL_LENGTH){
            throw new StringPrepParseException("The labels in the input are too long. Length > 63.", 
                                     StringPrepParseException.LABEL_TOO_LONG_ERROR,dest.toString(),0);
        }
        return dest;
    }
        
    /**
     * Convenience function that implements the IDNToASCII operation as defined in the IDNA RFC.
     * This operation is done on complete domain names, e.g: "www.example.com". 
     * It is important to note that this operation can fail. If it fails, then the input 
     * domain name cannot be used as an Internationalized Domain Name and the application
     * should have methods defined to deal with the failure.
     * 
     * <b>Note:</b> IDNA RFC specifies that a conformant application should divide a domain name
     * into separate labels, decide whether to apply allowUnassigned and useSTD3ASCIIRules on each, 
     * and then convert. This function does not offer that level of granularity. The options once  
     * set will apply to all labels in the domain name
     *
     * @param src       The input string as UCharacterIterator to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @stable ICU 2.8
     */
    public static StringBuffer convertIDNToASCII(UCharacterIterator src, int options)
            throws StringPrepParseException{
        return convertIDNToASCII(src.getText(), options);          
    }
    
    /**
     * Convenience function that implements the IDNToASCII operation as defined in the IDNA RFC.
     * This operation is done on complete domain names, e.g: "www.example.com". 
     * It is important to note that this operation can fail. If it fails, then the input 
     * domain name cannot be used as an Internationalized Domain Name and the application
     * should have methods defined to deal with the failure.
     * 
     * <b>Note:</b> IDNA RFC specifies that a conformant application should divide a domain name
     * into separate labels, decide whether to apply allowUnassigned and useSTD3ASCIIRules on each, 
     * and then convert. This function does not offer that level of granularity. The options once  
     * set will apply to all labels in the domain name
     *
     * @param src       The input string as a StringBuffer to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @stable ICU 2.8
     */
    public static StringBuffer convertIDNToASCII(StringBuffer src, int options)
            throws StringPrepParseException{
            return convertIDNToASCII(src.toString(), options);          
    }
    
    /**
     * Convenience function that implements the IDNToASCII operation as defined in the IDNA RFC.
     * This operation is done on complete domain names, e.g: "www.example.com". 
     * It is important to note that this operation can fail. If it fails, then the input 
     * domain name cannot be used as an Internationalized Domain Name and the application
     * should have methods defined to deal with the failure.
     * 
     * <b>Note:</b> IDNA RFC specifies that a conformant application should divide a domain name
     * into separate labels, decide whether to apply allowUnassigned and useSTD3ASCIIRules on each, 
     * and then convert. This function does not offer that level of granularity. The options once  
     * set will apply to all labels in the domain name
     *
     * @param src       The input string to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @stable ICU 2.8
     */
    public static StringBuffer convertIDNToASCII(String src,int options)
            throws StringPrepParseException{

        char[] srcArr = src.toCharArray();
        StringBuffer result = new StringBuffer();
        int sepIndex=0;
        int oldSepIndex=0;
        for(;;){
            sepIndex = getSeparatorIndex(srcArr,sepIndex,srcArr.length);
            String label = new String(srcArr,oldSepIndex,sepIndex-oldSepIndex);
            //make sure this is not a root label separator.
            if(!(label.length()==0 && sepIndex==srcArr.length)){
                UCharacterIterator iter = UCharacterIterator.getInstance(label);
                result.append(convertToASCII(iter,options));
            }
            if(sepIndex==srcArr.length){
                break;
            }
            
            // increment the sepIndex to skip past the separator
            sepIndex++;
            oldSepIndex = sepIndex;
            result.append((char)FULL_STOP);
        }
        if(result.length() > MAX_DOMAIN_NAME_LENGTH){
            throw new StringPrepParseException("The output exceed the max allowed length.", StringPrepParseException.DOMAIN_NAME_TOO_LONG_ERROR);
        }
        return result;
    }

    
    /**
     * This function implements the ToUnicode operation as defined in the IDNA RFC.
     * This operation is done on <b>single labels</b> before sending it to something that expects
     * Unicode names. A label is an individual part of a domain name. Labels are usually
     * separated by dots; for e.g." "www.example.com" is composed of 3 labels 
     * "www","example", and "com".
     * 
     * @param src       The input string to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @stable ICU 2.8
     */
    public static StringBuffer convertToUnicode(String src, int options)
           throws StringPrepParseException{
        UCharacterIterator iter = UCharacterIterator.getInstance(src);
        return convertToUnicode(iter,options);
    }
    
    /**
     * This function implements the ToUnicode operation as defined in the IDNA RFC.
     * This operation is done on <b>single labels</b> before sending it to something that expects
     * Unicode names. A label is an individual part of a domain name. Labels are usually
     * separated by dots; for e.g." "www.example.com" is composed of 3 labels 
     * "www","example", and "com".
     * 
     * @param src       The input string as StringBuffer to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @stable ICU 2.8
     */
    public static StringBuffer convertToUnicode(StringBuffer src, int options)
           throws StringPrepParseException{
        UCharacterIterator iter = UCharacterIterator.getInstance(src);
        return convertToUnicode(iter,options);
    }
       
    /**
     * Function that implements the ToUnicode operation as defined in the IDNA RFC.
     * This operation is done on <b>single labels</b> before sending it to something that expects
     * Unicode names. A label is an individual part of a domain name. Labels are usually
     * separated by dots; for e.g." "www.example.com" is composed of 3 labels 
     * "www","example", and "com".
     * 
     * @param src       The input string as UCharacterIterator to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @stable ICU 2.8
     */
    public static StringBuffer convertToUnicode(UCharacterIterator src, int options)
           throws StringPrepParseException{
        
        boolean[] caseFlags = null;
                
        // the source contains all ascii codepoints
        boolean srcIsASCII  = true;
        // assume the source contains all LDH codepoints
        //boolean srcIsLDH = true; 
        
        //get the options
        //boolean useSTD3ASCIIRules = ((options & USE_STD3_RULES) != 0);
        
        //int failPos = -1;
        int ch;
        int saveIndex = src.getIndex();
        // step 1: find out if all the codepoints in src are ASCII  
        while((ch=src.next())!= UCharacterIterator.DONE){
            if(ch>0x7F){
                srcIsASCII = false;
            }/*else if((srcIsLDH = isLDHChar(ch))==false){
                failPos = src.getIndex();
            }*/
        }
        StringBuffer processOut;
        
        if(srcIsASCII == false){
            try {
                // step 2: process the string
                src.setIndex(saveIndex);
                processOut = singleton.namePrep.prepare(src,options);
            } catch (StringPrepParseException ex) {
                return new StringBuffer(src.getText());
            }

        }else{
            //just point to source
            processOut = new StringBuffer(src.getText());
        }
        // TODO:
        // The RFC states that 
        // <quote>
        // ToUnicode never fails. If any step fails, then the original input
        // is returned immediately in that step.
        // </quote>
        
        //step 3: verify ACE Prefix
        if(startsWithPrefix(processOut)){
            StringBuffer decodeOut = null;

            //step 4: Remove the ACE Prefix
            String temp = processOut.substring(ACE_PREFIX.length,processOut.length());

            //step 5: Decode using punycode
            try {
                decodeOut = Punycode.decode(new StringBuffer(temp),caseFlags);
            } catch (StringPrepParseException e) {
                decodeOut = null;
            }
        
            //step 6:Apply toASCII
            if (decodeOut != null) {
                StringBuffer toASCIIOut = convertToASCII(decodeOut, options);
    
                //step 7: verify
                if(compareCaseInsensitiveASCII(processOut, toASCIIOut) !=0){
//                    throw new StringPrepParseException("The verification step prescribed by the RFC 3491 failed",
//                                             StringPrepParseException.VERIFICATION_ERROR); 
                    decodeOut = null;
                }
            }

            //step 8: return output of step 5
             if (decodeOut != null) {
                 return decodeOut;
             }
        }
            
//        }else{
//            // verify that STD3 ASCII rules are satisfied
//            if(useSTD3ASCIIRules == true){
//                if( srcIsLDH == false /* source contains some non-LDH characters */
//                    || processOut.charAt(0) ==  HYPHEN 
//                    || processOut.charAt(processOut.length()-1) == HYPHEN){
//    
//                    if(srcIsLDH==false){
//                        throw new StringPrepParseException("The input does not conform to the STD 3 ASCII rules",
//                                                 StringPrepParseException.STD3_ASCII_RULES_ERROR,processOut.toString(),
//                                                 (failPos>0) ? (failPos-1) : failPos);
//                    }else if(processOut.charAt(0) == HYPHEN){
//                        throw new StringPrepParseException("The input does not conform to the STD 3 ASCII rules",
//                                                 StringPrepParseException.STD3_ASCII_RULES_ERROR,
//                                                 processOut.toString(),0);
//         
//                    }else{
//                        throw new StringPrepParseException("The input does not conform to the STD 3 ASCII rules",
//                                                 StringPrepParseException.STD3_ASCII_RULES_ERROR,
//                                                 processOut.toString(),
//                                                 processOut.length());
//    
//                    }
//                }
//            }
//            // just return the source
//            return new StringBuffer(src.getText());
//        }  
        
        return new StringBuffer(src.getText());
    }
    
    /**
     * Convenience function that implements the IDNToUnicode operation as defined in the IDNA RFC.
     * This operation is done on complete domain names, e.g: "www.example.com". 
     *
     * <b>Note:</b> IDNA RFC specifies that a conformant application should divide a domain name
     * into separate labels, decide whether to apply allowUnassigned and useSTD3ASCIIRules on each, 
     * and then convert. This function does not offer that level of granularity. The options once  
     * set will apply to all labels in the domain name
     *
     * @param src       The input string as UCharacterIterator to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @stable ICU 2.8
     */
    public static StringBuffer convertIDNToUnicode(UCharacterIterator src, int options)
        throws StringPrepParseException{
        return convertIDNToUnicode(src.getText(), options);
    }
    
    /**
     * Convenience function that implements the IDNToUnicode operation as defined in the IDNA RFC.
     * This operation is done on complete domain names, e.g: "www.example.com". 
     *
     * <b>Note:</b> IDNA RFC specifies that a conformant application should divide a domain name
     * into separate labels, decide whether to apply allowUnassigned and useSTD3ASCIIRules on each, 
     * and then convert. This function does not offer that level of granularity. The options once  
     * set will apply to all labels in the domain name
     *
     * @param src       The input string as StringBuffer to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @stable ICU 2.8
     */
    public static StringBuffer convertIDNToUnicode(StringBuffer src, int options)
        throws StringPrepParseException{
        return convertIDNToUnicode(src.toString(), options);
    }
    
    /**
     * Convenience function that implements the IDNToUnicode operation as defined in the IDNA RFC.
     * This operation is done on complete domain names, e.g: "www.example.com". 
     *
     * <b>Note:</b> IDNA RFC specifies that a conformant application should divide a domain name
     * into separate labels, decide whether to apply allowUnassigned and useSTD3ASCIIRules on each, 
     * and then convert. This function does not offer that level of granularity. The options once  
     * set will apply to all labels in the domain name
     *
     * @param src       The input string to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @stable ICU 2.8
     */
    public static StringBuffer convertIDNToUnicode(String src, int options)
        throws StringPrepParseException{
            
        char[] srcArr = src.toCharArray();
        StringBuffer result = new StringBuffer();
        int sepIndex=0;
        int oldSepIndex=0;
        for(;;){
            sepIndex = getSeparatorIndex(srcArr,sepIndex,srcArr.length);
            String label = new String(srcArr,oldSepIndex,sepIndex-oldSepIndex);
            if(label.length()==0 && sepIndex!=srcArr.length ){
                throw new StringPrepParseException("Found zero length lable after NamePrep.",StringPrepParseException.ZERO_LENGTH_LABEL);
            }
            UCharacterIterator iter = UCharacterIterator.getInstance(label);
            result.append(convertToUnicode(iter,options));
            if(sepIndex==srcArr.length){
                break;
            }
            // Unlike the ToASCII operation we don't normalize the label separators
            result.append(srcArr[sepIndex]);
            // increment the sepIndex to skip past the separator
            sepIndex++;
            oldSepIndex =sepIndex;
        }
        if(result.length() > MAX_DOMAIN_NAME_LENGTH){
            throw new StringPrepParseException("The output exceed the max allowed length.", StringPrepParseException.DOMAIN_NAME_TOO_LONG_ERROR);
        }
        return result;
    }
    
    /**
     * Compare two IDN strings for equivalence.
     * This function splits the domain names into labels and compares them.
     * According to IDN RFC, whenever two labels are compared, they are 
     * considered equal if and only if their ASCII forms (obtained by 
     * applying toASCII) match using an case-insensitive ASCII comparison.
     * Two domain names are considered a match if and only if all labels 
     * match regardless of whether label separators match.
     * 
     * @param s1        First IDN string as StringBuffer
     * @param s2        Second IDN string as StringBuffer
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED    Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES      Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return 0 if the strings are equal, > 0 if s1 > s2 and < 0 if s1 < s2
     * @throws ParseException
     * @stable ICU 2.8
     */
    //  TODO: optimize
    public static int compare(StringBuffer s1, StringBuffer s2, int options)
        throws StringPrepParseException{
        if(s1==null || s2 == null){
            throw new IllegalArgumentException("One of the source buffers is null");
        }
        StringBuffer s1Out = convertIDNToASCII(s1.toString(),options);
        StringBuffer s2Out = convertIDNToASCII(s2.toString(), options);
        return compareCaseInsensitiveASCII(s1Out,s2Out);
    }
    
    /**
     * Compare two IDN strings for equivalence.
     * This function splits the domain names into labels and compares them.
     * According to IDN RFC, whenever two labels are compared, they are 
     * considered equal if and only if their ASCII forms (obtained by 
     * applying toASCII) match using an case-insensitive ASCII comparison.
     * Two domain names are considered a match if and only if all labels 
     * match regardless of whether label separators match.
     * 
     * @param s1        First IDN string 
     * @param s2        Second IDN string
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED    Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES      Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return 0 if the strings are equal, > 0 if s1 > s2 and < 0 if s1 < s2
     * @throws ParseException
     * @stable ICU 2.8
     */
    //  TODO: optimize
    public static int compare(String s1, String s2, int options)
        throws StringPrepParseException{
        if(s1==null || s2 == null){
            throw new IllegalArgumentException("One of the source buffers is null");
        }
        StringBuffer s1Out = convertIDNToASCII(s1, options);
        StringBuffer s2Out = convertIDNToASCII(s2, options);
        return compareCaseInsensitiveASCII(s1Out,s2Out);
    }
    /**
     * Compare two IDN strings for equivalence.
     * This function splits the domain names into labels and compares them.
     * According to IDN RFC, whenever two labels are compared, they are 
     * considered equal if and only if their ASCII forms (obtained by 
     * applying toASCII) match using an case-insensitive ASCII comparison.
     * Two domain names are considered a match if and only if all labels 
     * match regardless of whether label separators match.
     * 
     * @param s1        First IDN string as UCharacterIterator
     * @param s2        Second IDN string as UCharacterIterator
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return 0 if the strings are equal, > 0 if i1 > i2 and < 0 if i1 < i2
     * @throws ParseException
     * @stable ICU 2.8
     */
    //  TODO: optimize
    public static int compare(UCharacterIterator s1, UCharacterIterator s2, int options)
        throws StringPrepParseException{
        if(s1==null || s2 == null){
            throw new IllegalArgumentException("One of the source buffers is null");
        }
        StringBuffer s1Out = convertIDNToASCII(s1.getText(), options);
        StringBuffer s2Out = convertIDNToASCII(s2.getText(), options);
        return compareCaseInsensitiveASCII(s1Out,s2Out);
    }
}
