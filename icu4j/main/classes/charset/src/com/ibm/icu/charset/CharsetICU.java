/**
*******************************************************************************
* Copyright (C) 2006-2009, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/ 

package com.ibm.icu.charset;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;

import com.ibm.icu.text.UnicodeSet;

/**
 * <p>A subclass of java.nio.Charset for providing implementation of ICU's charset converters.
 * This API is used to convert codepage or character encoded data to and
 * from UTF-16. You can open a converter with {@link Charset#forName } and {@link #forNameICU }. With that
 * converter, you can get its properties, set options, convert your data.</p>
 *
 * <p>Since many software programs recogize different converter names for
 * different types of converters, there are other functions in this API to
 * iterate over the converter aliases. 
 * 
 * @stable ICU 3.6
 */
public abstract class CharsetICU extends Charset{

     String icuCanonicalName;
     String javaCanonicalName;
     int options;

     float  maxCharsPerByte;
    
     String name; /* +4: 60  internal name of the converter- invariant chars */

     int codepage;               /* +64: 4 codepage # (now IBM-$codepage) */

     byte platform;                /* +68: 1 platform of the converter (only IBM now) */
     byte conversionType;          /* +69: 1 conversion type */

     int minBytesPerChar;         /* +70: 1 Minimum # bytes per char in this codepage */
     int maxBytesPerChar;         /* +71: 1 Maximum # bytes output per UChar in this codepage */

     byte subChar[/*UCNV_MAX_SUBCHAR_LEN*/]; /* +72: 4  [note:  4 and 8 byte boundary] */
     byte subCharLen;              /* +76: 1 */
    
     byte hasToUnicodeFallback;   /* +77: 1 UBool needs to be changed to UBool to be consistent across platform */
     byte hasFromUnicodeFallback; /* +78: 1 */
     short unicodeMask;            /* +79: 1  bit 0: has supplementary  bit 1: has single surrogates */
     byte subChar1;               /* +80: 1  single-byte substitution character for IBM MBCS (0 if none) */
     //byte reserved[/*19*/];           /* +81: 19 to round out the structure */
     
     
    // typedef enum UConverterUnicodeSet {
     /** 
      * Parameter that select the set of roundtrippable Unicode code points. 
      * @stable ICU 4.0
      */
      public static final int ROUNDTRIP_SET=0; 
      /**
       * Select the set of Unicode code points with roundtrip or fallback mappings.
       * Not supported at this point.
       * @internal
       * @deprecated This API is ICU internal only.
       */
      public static final int ROUNDTRIP_AND_FALLBACK_SET =1;
      
    //} UConverterUnicodeSet;
     
    /**
     * 
     * @param icuCanonicalName
     * @param canonicalName
     * @param aliases
     * @stable ICU 3.6
     */
    protected CharsetICU(String icuCanonicalName, String canonicalName, String[] aliases) {
        super(canonicalName,aliases);
        if(canonicalName.length() == 0){
            throw new IllegalCharsetNameException(canonicalName);
        }
        this.javaCanonicalName = canonicalName;
        this.icuCanonicalName  = icuCanonicalName;
    }
    
    /**
     * Ascertains if a charset is a sub set of this charset
     * Implements the abstract method of super class.
     * @param cs charset to test
     * @return true if the given charset is a subset of this charset
     * @stable ICU 3.6
     */
    public boolean contains(Charset cs){
        if (null == cs) {
            return false;
        } else if (this.equals(cs)) {
            return true;
        }
        return false;
    }
    private static final HashMap<String, String> algorithmicCharsets = new HashMap<String, String>();
    static{
        algorithmicCharsets.put("LMBCS-1",               "com.ibm.icu.charset.CharsetLMBCS");
        algorithmicCharsets.put("BOCU-1",                "com.ibm.icu.charset.CharsetBOCU1" );
        algorithmicCharsets.put("SCSU",                  "com.ibm.icu.charset.CharsetSCSU" ); 
        algorithmicCharsets.put("US-ASCII",              "com.ibm.icu.charset.CharsetASCII" );
        algorithmicCharsets.put("ISO-8859-1",            "com.ibm.icu.charset.Charset88591" );
        algorithmicCharsets.put("UTF-16",                "com.ibm.icu.charset.CharsetUTF16" );
        algorithmicCharsets.put("UTF-16BE",              "com.ibm.icu.charset.CharsetUTF16BE" );
        algorithmicCharsets.put("UTF-16LE",              "com.ibm.icu.charset.CharsetUTF16LE" );
        algorithmicCharsets.put("UTF16_OppositeEndian",  "com.ibm.icu.charset.CharsetUTF16LE" );
        algorithmicCharsets.put("UTF16_PlatformEndian",  "com.ibm.icu.charset.CharsetUTF16" );
        algorithmicCharsets.put("UTF-32",                "com.ibm.icu.charset.CharsetUTF32" );
        algorithmicCharsets.put("UTF-32BE",              "com.ibm.icu.charset.CharsetUTF32BE" );
        algorithmicCharsets.put("UTF-32LE",              "com.ibm.icu.charset.CharsetUTF32LE" );
        algorithmicCharsets.put("UTF32_OppositeEndian",  "com.ibm.icu.charset.CharsetUTF32LE" );
        algorithmicCharsets.put("UTF32_PlatformEndian",  "com.ibm.icu.charset.CharsetUTF32" );
        algorithmicCharsets.put("UTF-8",                 "com.ibm.icu.charset.CharsetUTF8" );
        algorithmicCharsets.put("CESU-8",                "com.ibm.icu.charset.CharsetCESU8" );
        algorithmicCharsets.put("UTF-7",                 "com.ibm.icu.charset.CharsetUTF7" );
        algorithmicCharsets.put("ISCII,version=0",       "com.ibm.icu.charset.CharsetISCII" );
        algorithmicCharsets.put("ISCII,version=1",       "com.ibm.icu.charset.CharsetISCII" );
        algorithmicCharsets.put("ISCII,version=2",       "com.ibm.icu.charset.CharsetISCII" );
        algorithmicCharsets.put("ISCII,version=3",       "com.ibm.icu.charset.CharsetISCII" );
        algorithmicCharsets.put("ISCII,version=4",       "com.ibm.icu.charset.CharsetISCII" );
        algorithmicCharsets.put("ISCII,version=5",       "com.ibm.icu.charset.CharsetISCII" );
        algorithmicCharsets.put("ISCII,version=6",       "com.ibm.icu.charset.CharsetISCII" );
        algorithmicCharsets.put("ISCII,version=7",       "com.ibm.icu.charset.CharsetISCII" );
        algorithmicCharsets.put("ISCII,version=8",       "com.ibm.icu.charset.CharsetISCII" );
        algorithmicCharsets.put("IMAP-mailbox-name",     "com.ibm.icu.charset.CharsetUTF7" );
        algorithmicCharsets.put("HZ",                    "com.ibm.icu.charset.CharsetHZ" );
        algorithmicCharsets.put("ISO_2022,locale=ja,version=0",               "com.ibm.icu.charset.CharsetISO2022" );
        algorithmicCharsets.put("ISO_2022,locale=ja,version=1",               "com.ibm.icu.charset.CharsetISO2022" );
        algorithmicCharsets.put("ISO_2022,locale=ja,version=2",               "com.ibm.icu.charset.CharsetISO2022" );
        algorithmicCharsets.put("ISO_2022,locale=ja,version=3",               "com.ibm.icu.charset.CharsetISO2022" );
        algorithmicCharsets.put("ISO_2022,locale=ja,version=4",               "com.ibm.icu.charset.CharsetISO2022" );
        algorithmicCharsets.put("ISO_2022,locale=zh,version=0",               "com.ibm.icu.charset.CharsetISO2022" );
        algorithmicCharsets.put("ISO_2022,locale=zh,version=1",               "com.ibm.icu.charset.CharsetISO2022" );
        algorithmicCharsets.put("ISO_2022,locale=ko,version=0",               "com.ibm.icu.charset.CharsetISO2022" );
        algorithmicCharsets.put("ISO_2022,locale=ko,version=1",               "com.ibm.icu.charset.CharsetISO2022" );
        }

    /*public*/ static final Charset getCharset(String icuCanonicalName, String javaCanonicalName, String[] aliases){
       String className = (String) algorithmicCharsets.get(icuCanonicalName);
       if(className==null){
           //all the cnv files are loaded as MBCS
           className = "com.ibm.icu.charset.CharsetMBCS";
       }
       try{
           CharsetICU conv = null;
           Class<? extends CharsetICU> cs = Class.forName(className).asSubclass(CharsetICU.class);
           Class<?>[] paramTypes = new Class<?>[]{ String.class, String.class,  String[].class};
           final Constructor<? extends CharsetICU> c = cs.getConstructor(paramTypes);
           Object[] params = new Object[]{ icuCanonicalName, javaCanonicalName, aliases};
           
           // Run constructor
           try {
               conv = c.newInstance(params);
               if (conv != null) {
                   return conv;
               }
           }catch (InvocationTargetException e) {
               throw new UnsupportedCharsetException( icuCanonicalName+": "+"Could not load " + className+ ". Exception:" + e.getTargetException());    
           }
       }catch(ClassNotFoundException ex){
       }catch(NoSuchMethodException ex){
       }catch (IllegalAccessException ex){ 
       }catch (InstantiationException ex){ 
       }
       throw new UnsupportedCharsetException( icuCanonicalName+": "+"Could not load " + className);    
    }
    
    static final boolean isSurrogate(int c){
        return (((c)&0xfffff800)==0xd800);
    }
    
    /*
     * Returns the default charset name 
     */
//    static final String getDefaultCharsetName(){
//        String defaultEncoding = new InputStreamReader(new ByteArrayInputStream(new byte[0])).getEncoding();
//        return defaultEncoding;
//    }

    /**
     * Returns a charset object for the named charset.
     * This method gurantee that ICU charset is returned when
     * available.  If the ICU charset provider does not support
     * the specified charset, then try other charset providers
     * including the standard Java charset provider.
     * 
     * @param charsetName The name of the requested charset,
     * may be either a canonical name or an alias
     * @return A charset object for the named charset
     * @throws IllegalCharsetNameException If the given charset name
     * is illegal
     * @throws UnsupportedCharsetException If no support for the
     * named charset is available in this instance of th Java
     * virtual machine
     * @stable ICU 3.6
     */
    public static Charset forNameICU(String charsetName) throws IllegalCharsetNameException, UnsupportedCharsetException {
        CharsetProviderICU icuProvider = new CharsetProviderICU();
        CharsetICU cs = (CharsetICU) icuProvider.charsetForName(charsetName);
        if (cs != null) {
            return cs;
        }
        return Charset.forName(charsetName);
    }

//    /**
//     * @see java.lang.Comparable#compareTo(java.lang.Object)
//     * @stable 3.8
//     */
//    public int compareTo(Object otherObj) {
//        if (!(otherObj instanceof CharsetICU)) {
//            return -1;
//        }
//        return icuCanonicalName.compareTo(((CharsetICU)otherObj).icuCanonicalName);
//    }

    /**
     * This follows ucnv.c method ucnv_detectUnicodeSignature() to detect the
     * start of the stream for example U+FEFF (the Unicode BOM/signature
     * character) that can be ignored.
     * 
     * Detects Unicode signature byte sequences at the start of the byte stream
     * and returns number of bytes of the BOM of the indicated Unicode charset.
     * 0 is returned when no Unicode signature is recognized.
     * 
     */
    // TODO This should be proposed as CharsetDecoderICU API.
//    static String detectUnicodeSignature(ByteBuffer source) {
//        int signatureLength = 0; // number of bytes of the signature
//        final int SIG_MAX_LEN = 5;
//        String sigUniCharset = null; // states what unicode charset is the BOM
//        int i = 0;
//
//        /*
//         * initial 0xa5 bytes: make sure that if we read <SIG_MAX_LEN bytes we
//         * don't misdetect something
//         */
//        byte start[] = { (byte) 0xa5, (byte) 0xa5, (byte) 0xa5, (byte) 0xa5,
//                (byte) 0xa5 };
//
//        while (i < source.remaining() && i < SIG_MAX_LEN) {
//            start[i] = source.get(i);
//            i++;
//        }
//
//        if (start[0] == (byte) 0xFE && start[1] == (byte) 0xFF) {
//            signatureLength = 2;
//            sigUniCharset = "UTF-16BE";
//            source.position(signatureLength);
//            return sigUniCharset;
//        } else if (start[0] == (byte) 0xFF && start[1] == (byte) 0xFE) {
//            if (start[2] == (byte) 0x00 && start[3] == (byte) 0x00) {
//                signatureLength = 4;
//                sigUniCharset = "UTF-32LE";
//                source.position(signatureLength);
//                return sigUniCharset;
//            } else {
//                signatureLength = 2;
//                sigUniCharset = "UTF-16LE";
//                source.position(signatureLength);
//                return sigUniCharset;
//            }
//        } else if (start[0] == (byte) 0xEF && start[1] == (byte) 0xBB
//                && start[2] == (byte) 0xBF) {
//            signatureLength = 3;
//            sigUniCharset = "UTF-8";
//            source.position(signatureLength);
//            return sigUniCharset;
//        } else if (start[0] == (byte) 0x00 && start[1] == (byte) 0x00
//                && start[2] == (byte) 0xFE && start[3] == (byte) 0xFF) {
//            signatureLength = 4;
//            sigUniCharset = "UTF-32BE";
//            source.position(signatureLength);
//            return sigUniCharset;
//        } else if (start[0] == (byte) 0x0E && start[1] == (byte) 0xFE
//                && start[2] == (byte) 0xFF) {
//            signatureLength = 3;
//            sigUniCharset = "SCSU";
//            source.position(signatureLength);
//            return sigUniCharset;
//        } else if (start[0] == (byte) 0xFB && start[1] == (byte) 0xEE
//                && start[2] == (byte) 0x28) {
//            signatureLength = 3;
//            sigUniCharset = "BOCU-1";
//            source.position(signatureLength);
//            return sigUniCharset;
//        } else if (start[0] == (byte) 0x2B && start[1] == (byte) 0x2F
//                && start[2] == (byte) 0x76) {
//
//            if (start[3] == (byte) 0x38 && start[4] == (byte) 0x2D) {
//                signatureLength = 5;
//                sigUniCharset = "UTF-7";
//                source.position(signatureLength);
//                return sigUniCharset;
//            } else if (start[3] == (byte) 0x38 || start[3] == (byte) 0x39
//                    || start[3] == (byte) 0x2B || start[3] == (byte) 0x2F) {
//                signatureLength = 4;
//                sigUniCharset = "UTF-7";
//                source.position(signatureLength);
//                return sigUniCharset;
//            }
//        } else if (start[0] == (byte) 0xDD && start[2] == (byte) 0x73
//                && start[2] == (byte) 0x66 && start[3] == (byte) 0x73) {
//            signatureLength = 4;
//            sigUniCharset = "UTF-EBCDIC";
//            source.position(signatureLength);
//            return sigUniCharset;
//        }
//
//        /* no known Unicode signature byte sequence recognized */
//        return null;
//    }
    
    
    abstract void getUnicodeSetImpl(UnicodeSet setFillIn, int which);
    
    /**
    * <p>Returns the set of Unicode code points that can be converted by an ICU Converter. 
    * <p>
    * The current implementation returns only one kind of set (UCNV_ROUNDTRIP_SET): The set of all Unicode code points that can be 
    * roundtrip-converted (converted without any data loss) with the converter This set will not include code points that have fallback 
    * mappings or are only the result of reverse fallback mappings.  See UTR #22 "Character Mapping Markup Language" at  <a href="http://www.unicode.org/reports/tr22/">http://www.unicode.org/reports/tr22/</a>
    * <p>* In the future, there may be more UConverterUnicodeSet choices to select sets with different properties.
    * <p>
    * <p>This is useful for example for
    * <ul><li>checking that a string or document can be roundtrip-converted with a converter,
    *   without/before actually performing the conversion</li>
    * <li>testing if a converter can be used for text for typical text for a certain locale,
    *   by comparing its roundtrip set with the set of ExemplarCharacters from
    *   ICU's locale data or other sources</li></ul>
    *
    * @param setFillIn A valid UnicodeSet. It will be cleared by this function before 
    *                   the converter's specific set is filled in.
    * @param which A selector; currently ROUNDTRIP_SET is the only supported value.
    * @throws IllegalArgumentException if the parameters does not match.              
    * @stable ICU 4.0
    */
       public void getUnicodeSet(UnicodeSet setFillIn, int which){
           if( setFillIn == null || which != ROUNDTRIP_SET ){
               throw new IllegalArgumentException();
           }
           setFillIn.clear();
           getUnicodeSetImpl(setFillIn, which);
       }
      
       static void getNonSurrogateUnicodeSet(UnicodeSet setFillIn){
           setFillIn.add(0, 0xd7ff);
           setFillIn.add(0xe000, 0x10ffff);
       }
       
       static void getCompleteUnicodeSet(UnicodeSet setFillIn){
           setFillIn.add(0, 0x10ffff);
       }

}
