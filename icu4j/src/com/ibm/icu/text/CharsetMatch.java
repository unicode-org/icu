/**
*******************************************************************************
* Copyright (C) 2005, International Business Machines Corporation and         *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
package com.ibm.icu.text;

import java.io.InputStream;
import java.io.Reader;


/**
 * This class represents a charset that has been identified by a CharsetDetector
 * as a possible encoding for a set of input data.  From an instance of this
 * class, you can ask for a confidence level in the charset identification,
 * or for Java Reader or String to access the original byte data in Unicode form.
 * <p/>
 * Instances of this class are created only by CharsetDetectors.
 */
public class CharsetMatch {

    
    /**
     * Create a java.io.Reader for reading the Unicode character data corresponding
     * to the original byte data supplied to the Charset detect operation.
     *
     * @return the Reader for the Unicode character data.
     */
    public Reader getReader() {
        return null;
    }
    
    

    /**
     * Create a Java String from Unicode character data corresponding
     * to the original byte data supplied to the Charset detect operation.
     *
     * @return a String created from the converted input data.
     */
    public String getString() {
        return null;

    }
    /**
     * Create a Java String from Unicode character data corresponding
     * to the original byte data supplied to the Charset detect operation.
     * The length of the returned string is limited to the specified size;
     * the string will be trunctated to this length if necessary.  A limit value of
     * zero or less is ignored, and treated as no limit.
     *
     * @param maxLength The maximium length of the String to be created.
     * @return a String created from the converted input data.
     */
    public String getString(int maxLength) {
        return null;

    }
    
    /**
     * Get an indication of the confidence in the charset detected.
     * Confidence values range from 0-100, with larger numbers indicating
     * a better match of the input data to the characteristics of the
     * charset.
     *
     * @return the confidence in the charset match
     */
    public int getConfidence() {
        return 0;
    }
    
    /**
     * Return an indication of what it was about input data that 
     * that caused this charset to be considered as a possible match.
     * <p>
     * TODO: create a list of enum-like constants for the possible types of matches.
     * 
     * @return the type of match found for this charset.
     */
    public int getMatchType() {
        return 0;
    }
 
    

    /**
     * Get the name of the detected charset.  
     * The name will be one that can be used with other APIs on the
     * platform that accept charset names.  It is the "Canonical name"
     * as defined by the class java.nio.charset.Charset; for
     * charsets that are registered with the IANA charset registry,
     * this is the MIME-preferred registerd name.
     *
     * @see java.nio.charset.Charset
     * @see java.io.InputStreamReader
     *
     * @return The name of the charset.
     */
    public String getName() {
        return "";
    }

}
