/*
 * Created on Apr 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.ibm.icu.text;

/**
 * CharsetRecognizer implemenation for Asian  - double or multi-byte - charsets.
 *                   Match is determined mostly by the input data adhering to the
 *                   encoding scheme for the charset, although the hooks are here
 *                   to also check language based character occurence frequencies if that
 *                   proves to be necessary.
 * <p/>
 *                   Instances of this class are singletons, one per encoding
 *                   being recognized.  They are created in the main
 *                   CharsetDetector class and kept in the global list of available
 *                   encodings to be checked.  The specific encoding being recognized
 *                   is determined by the CharsetDetectEncoding provided when an
 *                   instance of this class is created.
 *                   
 */
class CharsetRecog_mbcs extends CharsetRecognizer {

    private CharsetDetectEncoding fEnc;
    private String                fCharsetName;
    
    /**
     * Constructor.  
     * @param enc
     */
    CharsetRecog_mbcs(String charsetName, CharsetDetectEncoding enc) {
        fEnc = enc;
        fCharsetName = charsetName;
    }
    
    /**
     * Get the IANA name of this charset.
     * @return the charset name.
     */
    String      getName() {
        return fCharsetName;
    }
    
    
    /**
     * Test the match of this charset with the input text data
     *      which is obtained via the CharsetDetector object.
     * 
     * @param det  The CharsetDetector, which contains the input text
     *             to be checked for being in this charset.
     * @return     Two values packed into one int  (Damn java, anyhow)
     *             <br/>
     *             bits 0-7:  the match confidence, ranging from 0-100
     *             <br/>
     *             bits 8-15: The match reason, an enum-like value.
     */
     int         match(CharsetDetector det) {
        int   singleByteCharCount = 0;
        int   doubleByteCharCount = 0;
        int   badCharCount        = 0;
        int   totalCharCount      = 0;
        
        CharsetDetectEncoding.iteratedChar   ichar = new CharsetDetectEncoding.iteratedChar();
        
        for (ichar.reset(); fEnc.nextChar(ichar, det);) {
            totalCharCount++;
            if (ichar.error) {
                badCharCount++; 
            } else {
                
                if (ichar.charValue <= 0xff) {
                    singleByteCharCount++;
                } else {
                    doubleByteCharCount++;
                }
            }
        }
        
        int confidence = 40 + doubleByteCharCount - 10*badCharCount;
        if (confidence < 0) {
            confidence = 0;
        }
        if (confidence > 100) {
            confidence = 100;
        }
         
        return confidence;
    }

}
