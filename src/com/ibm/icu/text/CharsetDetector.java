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
 *
 * <code>CharsetDetector</code> provides a facility for detecting the
 * charset or encoding of character data in an unknown format.
 * The input data can either be from an input stream or an array of bytes.
 * The result of the detection operation is a list of possibly matching
 * charsets, or, for simple use, you can just ask for a Java Reader that
 * will will work over the input data.
 * <p/>
 * Character set detection is at best an imprecise operation.  The detection
 * process will attempt to identify the charset that best matches the characteristics
 * of the byte data, but the process is partly statistical in nature, and
 * the results can not be guaranteed to always be correct.
 * <p/>
 * For best accuracy in charset detection, the input data should be primarily
 * in a single language, and a minimum of a few hundred bytes worth of plain text
 * in the language are needed.  The detection process will attempt to
 * ignore html or xml style markup that could otherwise obscure the content.
 * <p/>
 * <b>Question:</b>Should we have getters corresponding to the setters for inut text
 * and declared encoding?
 * <p/>
 * <b>A thought:</b>  If we were to create our own type of Java Reader, we could defer
 * figuring out an actual charset for data that starts out with too much English
 *  only ASCII until the user actually read through to something that didn't look
 * like 7 bit English.  If  nothing else ever appeared, we would never need to
 *  actually choose the "real" charset.  All assuming that the application just
 *   wants the data, and doesn't care about a char set name.
 *
 *
 */
public class CharsetDetector {


    /**
     *   Constructor
     */
    public CharsetDetector() {
    }

    /**
     * Set the declared encoding for charset detection.
    *  The declared encoding of an input text is an encoding obtained
    *  from an http header or xml declaration or similar source that
    *  can be provided as additional information to the charset detector.  
    *  A match between a declared encoding and a possible detected encoding
    *  will raise the quality of that detected encoding by a small delta,
    *  and will also appear as a "reason" for the match.
    * <p/>
    * A declared encoding that is incompatible with the input data being
    * analyzed will not be added to the list of possible encodings.
    * 
    *  @param encoding The declared encoding 
    */
    public CharsetDetector setDecaredEncoding(String encoding) {
        return this;
    }
    
    /**
     * Set the input text (byte) data whose charset is to be detected.
     * @param in the input text of unknown encoding
     * @return This CharsetDetector
     */
    public CharsetDetector setText(byte in[]) {
        return this;
    }
    
    /**
     * Set the input text (byte) data whose charset is to be detected.
     *  <p/>
     *   The input stream that supplies the character data must have markSupported()
     *   == true; the charset detection process will read a small amount of data,
     *   then return the stream to its original position via
     *   the InputStream.reset() operation.  The exact amount that will
     *   be read depends on the characteristics of the data itself.

     * @param in the input text of unknown encoding
     * @return This CharsetDetector
     */
    public CharsetDetector setText(InputStream in) {
        return this;
    }

  
    /**
     * Return the charset that best matches the supplied input data.
     * 
     * Note though, that because the detection 
     * only looks at the start of the input data,
     * there is a possibility that the returned charset will fail to handle
     * the full set of input data.
     * <p/>
     * Raise an exception if 
     *  <ul>
     *    <li>no charset appears to match the data.</li>
     *    <li>no input text has been provided</li>
     *  </ul>
     *
     * @return a CharsetMatch object representing the best matching charset.
     */
    public CharsetMatch detect() {
        return null;
    }
    
    /**
     *  Return an array of all charsets that appear to be plausible
     *  matches with the input data.  The array is ordered with the
     *  best quality match first.
     * <p/>
     * Raise an exception if 
     *  <ul>
     *    <li>no charsets appear to match the input data.</li>
     *    <li>no input text has been provided</li>
     *  </ul>
      * 
     * @return An array of CharsetMatch objects representing possibly matching charsets.
     */
    public CharsetMatch[] detectAll() {
        return null;
    }

    
    /**
     * Autodetect the charset of an inputStream, and return a Java Reader
     * to access the converted input data.
     * <p/>
     * This is a convenience method that is equivalent to
     *   <code>this.setDeclaredEncoding(declaredEncoding).setText(in).detect().getReader();</code>
     * <p/>
     *   For the input stream that supplies the character data, markSupported()
     *   must be true; the  charset detection will read a small amount of data,
     *   then return the stream to its original position via
     *   the InputStream.reset() operation.  The exact amount that will
     *    be read depends on the characteristics of the data itself.
     *<p/>
     * Raise an exception if no charsets appear to match the input data.
     * 
     * @param in The source of the byte data in the unknown charset.
     *
     * @param declaredEncoding  A declared encoding for the data, if available,
     *           or null or an empty string if none is available.
     */
    public Reader getReader(InputStream in, String declaredEncoding) {
        return null;
    }

    /**
     * Autodetect the charset of an inputStream, and return a String
     * containing the converted input data.
     * <p/>
     * This is a convenience method that is equivalent to
     *   <code>this.setDeclaredEncoding(declaredEncoding).setText(in).detect().getString();</code>
     *<p/>
     * Raise an exception if no charsets appear to match the input data.
     * 
     * @param in The source of the byte data in the unknown charset.
     *
     * @param declaredEncoding  A declared encoding for the data, if available,
     *           or null or an empty string if none is available.
     */
    public String getString(byte[] in, String declaredEncoding) {
        return null;
    }

 
    /**
     * Get the names of all char sets that can be recognized by the char set detector.
     *
     * @return an array of the names of all charsets that can be recognized
     * by the charset detector.
     */
    public static String[] getAllDetectableCharsets() {
        return null;
    }


}
