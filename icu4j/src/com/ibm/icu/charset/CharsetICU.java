/**
*******************************************************************************
* Copyright (C) 2006, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/ 

package com.ibm.icu.charset;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;

import com.ibm.icu.lang.UCharacter;



public abstract class CharsetICU extends Charset{
	
    protected String icuCanonicalName;
    protected String javaCanonicalName;
    protected int options;

    protected int maxBytesPerChar;
    protected int minBytesPerChar;
    protected float  maxCharsPerByte;
    protected byte subChar1 = 0x00; 
    
    protected int mode;
    protected boolean flush;
    protected boolean useFallback;
    
    /**
     * 
     * @param icuCanonicalName
     * @param canonName
     * @param aliases
     * @draft ICU 3.6
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
     * @param cs charset to test
     * @return true if the given charset is a subset of this charset
     */
    public boolean contains(Charset cs){
        if (null == cs) {
            return false;
        } else if (this.equals(cs)) {
            return true;
        }
        return false;
    }
    private static final HashMap algorithmicCharsets = new HashMap();
    static{
        algorithmicCharsets.put("BOCU-1",                "com.ibm.icu.impl.CharsetBOCU1" );
        algorithmicCharsets.put("CESU-8",                "com.ibm.icu.impl.CharsetCESU8" );
        algorithmicCharsets.put("HZ",                    "com.ibm.icu.impl.CharsetHZ" );
        algorithmicCharsets.put("imapmailboxname",       "com.ibm.icu.impl.CharsetIMAP" );
        algorithmicCharsets.put("ISCII",                 "com.ibm.icu.impl.CharsetISCII" );
        algorithmicCharsets.put("iso2022",               "com.ibm.icu.impl.CharsetISO2022" );
        algorithmicCharsets.put("iso88591",              "com.ibm.icu.impl.CharsetBOCU1" );
        algorithmicCharsets.put("lmbcs1",                "com.ibm.icu.impl.CharsetLMBCS1" );
        algorithmicCharsets.put("lmbcs11",               "com.ibm.icu.impl.CharsetLMBCS11" );
        algorithmicCharsets.put("lmbcs16",               "com.ibm.icu.impl.CharsetLMBCS16" );
        algorithmicCharsets.put("lmbcs17",               "com.ibm.icu.impl.CharsetLMBCS17" );
        algorithmicCharsets.put("lmbcs18",               "com.ibm.icu.impl.CharsetLMBCS18" );
        algorithmicCharsets.put("lmbcs19",               "com.ibm.icu.impl.CharsetLMBCS19" );
        algorithmicCharsets.put("lmbcs2",                "com.ibm.icu.impl.CharsetLMBCS2" );
        algorithmicCharsets.put("lmbcs3",                "com.ibm.icu.impl.CharsetLMBCS3" );
        algorithmicCharsets.put("lmbcs4",                "com.ibm.icu.impl.CharsetLMBCS4" );
        algorithmicCharsets.put("lmbcs5",                "com.ibm.icu.impl.CharsetLMBCS5" );
        algorithmicCharsets.put("lmbcs6",                "com.ibm.icu.impl.CharsetLMBCS6" );
        algorithmicCharsets.put("lmbcs8",                "com.ibm.icu.impl.CharsetLMBCS8" );
        algorithmicCharsets.put("scsu",                  "com.ibm.icu.impl.CharsetSCSU" );
        algorithmicCharsets.put("usascii",               "com.ibm.icu.impl.CharsetUSASCII" );
        algorithmicCharsets.put("UTF-16",                "com.ibm.icu.impl.CharsetUTF16" );
        algorithmicCharsets.put("UTF-16BE",              "com.ibm.icu.impl.CharsetUTF16" );
        algorithmicCharsets.put("UTF-16LE",              "com.ibm.icu.impl.CharsetUTF16LE" );
        algorithmicCharsets.put("UTF16_OppositeEndian",  "com.ibm.icu.impl.CharsetUTF16LE" );
        algorithmicCharsets.put("UTF16_PlatformEndian",  "com.ibm.icu.impl.CharsetUTF16" );
        algorithmicCharsets.put("UTF-32",                "com.ibm.icu.impl.CharsetUTF32" );
        algorithmicCharsets.put("UTF-32BE",              "com.ibm.icu.impl.CharsetUTF32" );
        algorithmicCharsets.put("UTF-32LE",              "com.ibm.icu.impl.CharsetUTF32LE" );
        algorithmicCharsets.put("UTF32_PlatformEndian",  "com.ibm.icu.impl.CharsetUTF32LE" );
        algorithmicCharsets.put("UTF32_OppositeEndian",  "com.ibm.icu.impl.CharsetUTF32" );
        algorithmicCharsets.put("UTF-7",                 "com.ibm.icu.impl.CharsetUTF7" );
        algorithmicCharsets.put("UTF-8",                 "com.ibm.icu.impl.CharsetUTF8" );
    }

    /*public*/ static final Charset getCharset(String icuCanonicalName, String javaCanonicalName, String[] aliases){
       String className = (String) algorithmicCharsets.get(icuCanonicalName);
       if(className==null){
           //all the cnv files are loaded as MBCS
           className = "com.ibm.icu.impl.CharsetMBCS";
       }
       try{
           CharsetICU conv = null;
           Class cs = Class.forName(className);
           Class[] paramTypes = new Class[]{ String.class, String.class,  String[].class};
           final Constructor c = cs.getConstructor(paramTypes);
           Object[] params = new Object[]{ icuCanonicalName, javaCanonicalName, aliases};
           
           java.security.AccessController.doPrivileged
           (new java.security.PrivilegedAction() {
                   public Object run() {
                       c.setAccessible(true);
                       return null;
                   }
               });

           // Run constructor
           try {
               Object obj = c.newInstance(params);
               if(obj!=null && obj instanceof CharsetICU){
                   conv = (CharsetICU)obj;
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
    
    /** Always use fallbacks from codepage to Unicode */
    protected final boolean isToUUseFallback() {
        return true;
    }    
    
    /** Use fallbacks from Unicode to codepage when useFallback or for private-use code points */
    protected final boolean isFromUUseFallback(int c) {
        return (useFallback) || isPrivateUse(c);
    }
    
    /**
     * 
     */
    public static final String getDefaultCharsetName(){
        String defaultEncoding = new InputStreamReader(new ByteArrayInputStream(new byte[0])).getEncoding();
        return defaultEncoding;
    }
    
    /*public*/ static final boolean isPrivateUse(int c) {
        return (UCharacter.getType(c) == UCharacter.PRIVATE_USE);
    }

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
     */
    public static Charset forNameICU(String charsetName) throws IllegalCharsetNameException, UnsupportedCharsetException {
        CharsetProviderICU icuProvider = new CharsetProviderICU();
        Charset cs = icuProvider.charsetForName(charsetName);
        if (cs != null) {
            return cs;
        }
        return Charset.forName(charsetName);
    }
}

