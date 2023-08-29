// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
*******************************************************************************
* Copyright (C) 2006-2015, International Business Machines Corporation and
* others. All Rights Reserved.
*******************************************************************************
*/

package com.ibm.icu.charset;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.charset.spi.CharsetProvider;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.ibm.icu.impl.InvalidFormatException;


/**
 * A concrete subclass of CharsetProvider for loading and providing charset converters
 * in ICU.
 * @stable ICU 3.6
 */
public final class CharsetProviderICU extends CharsetProvider{
    /**
     * List of available ICU Charsets, empty during static initialization.
     * Not a Set or Map, so that we can add different Charset objects with the same name(),
     * which means that they are .equals(). See ICU ticket #11493.
     */
    private static List<Charset> icuCharsets = Collections.<Charset>emptyList();

    /**
     * Default constructor
     * @stable ICU 3.6
     */
    public CharsetProviderICU() {
    }

    /**
     * Constructs a Charset for the given charset name.
     * Implements the abstract method of super class.
     * @param charsetName charset name
     * @return Charset object for the given charset name, null if unsupported
     * @stable ICU 3.6
     */
    @Override
    public final Charset charsetForName(String charsetName){
        try{
            // extract the options from the charset name
            String optionsString = "";
            if (charsetName.endsWith(UConverterConstants.OPTION_SWAP_LFNL_STRING)) {
                /* Remove and save the swap lfnl option string portion of the charset name. */
                optionsString = UConverterConstants.OPTION_SWAP_LFNL_STRING;
                charsetName = charsetName.substring(0, charsetName.length() - optionsString.length());
            }
            // get the canonical name
            String icuCanonicalName = getICUCanonicalName(charsetName);

            // create the converter object and return it
            if(icuCanonicalName==null || icuCanonicalName.length()==0){
                // Try the original name, may be something added and not in the alias table.
                // Will get an unsupported encoding exception if it doesn't work.
                icuCanonicalName = charsetName;
            }
            return getCharset(icuCanonicalName, optionsString);
        }catch(UnsupportedCharsetException ex){
        }catch(IOException ex){
        }
        return null;
    }

    /**
     * Constructs a charset for the given ICU conversion table from the specified class path.
     * Example use: <code>cnv = CharsetProviderICU.charsetForName("myConverter", "com/myCompany/myDataPackage");</code>.
     * In this example myConverter.cnv would exist in the com/myCompany/myDataPackage Java package.
     * Conversion tables can be made with ICU4C's makeconv tool.
     * This function allows you to allows you to load user defined conversion
     * tables that are outside of ICU's core data.
     * @param charsetName The name of the charset conversion table.
     * @param classPath The class path that contain the conversion table.
     * @return charset object for the given charset name, null if unsupported
     * @stable ICU 3.8
     */
    public final Charset charsetForName(String charsetName, String classPath) {
        return charsetForName(charsetName, classPath, null);
    }

    /**
     * Constructs a charset for the given ICU conversion table from the specified class path.
     * This function is similar to {@link #charsetForName(String, String)}.
     * @param charsetName The name of the charset conversion table.
     * @param classPath The class path that contain the conversion table.
     * @param loader the class object from which to load the charset conversion table
     * @return charset object for the given charset name, null if unsupported
     * @stable ICU 3.8
     */
    public Charset charsetForName(String charsetName, String classPath, ClassLoader loader) {
        CharsetMBCS cs = null;
        try {
             cs = new CharsetMBCS(charsetName, charsetName, new String[0], classPath, loader);
        } catch (InvalidFormatException e) {
            // return null;
        }
        return cs;
    }

    /**
     * Gets the canonical name of the converter as defined by Java
     * @param enc converter name
     * @return canonical name of the converter
     * @internal
     * @deprecated This API is ICU internal only.
     */
     @Deprecated
     public static final String getICUCanonicalName(String enc)
                                throws UnsupportedCharsetException{
        String canonicalName = null;
        String ret = null;
        try{
            if(enc!=null){
                 if((canonicalName = UConverterAlias.getCanonicalName(enc, "MIME"))!=null){
                    ret = canonicalName;
                } else if((canonicalName = UConverterAlias.getCanonicalName(enc, "IANA"))!=null){
                    ret = canonicalName;
                } else if((canonicalName = UConverterAlias.getAlias(enc, 0))!=null){
                    /* we have some aliases in the form x-blah .. match those */
                    ret = canonicalName;
                }/*else if((canonicalName = UConverterAlias.getCanonicalName(enc, ""))!=null){
                    ret = canonicalName;
                }*/else if(enc.indexOf("x-")==0 || enc.indexOf("X-")==0){
                    /* TODO: Match with getJavaCanonicalName method */
                    /*
                    char temp[ UCNV_MAX_CONVERTER_NAME_LENGTH] = {0};
                    strcpy(temp, encName+2);
                    */
                    // Remove the 'x-' and get the ICU canonical name
                    if ((canonicalName = UConverterAlias.getAlias(enc.substring(2), 0))!=null) {
                        ret = canonicalName;
                    } else {
                        ret = "";
                    }

                }else{
                    /* unsupported encoding */
                   ret = "";
                }
            }
            return ret;
        }catch(IOException ex){
            throw new UnsupportedCharsetException(enc);
        }
    }
    private static final Charset getCharset(String icuCanonicalName, String optionsString)
            throws IOException {
       String[] aliases = getAliases(icuCanonicalName);
       String canonicalName = getJavaCanonicalName(icuCanonicalName);

       /* Concat the option string to the icuCanonicalName so that the options can be handled properly
        * by the actual charset.
        */
       return (CharsetICU.getCharset(icuCanonicalName + optionsString, canonicalName, aliases));
    }
    /**
     * Gets the canonical name of the converter as defined by Java
     * @param charsetName converter name
     * @return canonical name of the converter
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static String getJavaCanonicalName(String charsetName){
        /*
        If a charset listed in the IANA Charset Registry is supported by an implementation
        of the Java platform then its canonical name must be the name listed in the registry.
        Many charsets are given more than one name in the registry, in which case the registry
        identifies one of the names as MIME-preferred. If a charset has more than one registry
        name then its canonical name must be the MIME-preferred name and the other names in
        the registry must be valid aliases. If a supported charset is not listed in the IANA
        registry then its canonical name must begin with one of the strings "X-" or "x-".
        */
        if(charsetName==null ){
            return null;
        }
        try{
            String cName = null;
            /* find out the alias with MIME tag */
            if((cName=UConverterAlias.getStandardName(charsetName, "MIME"))!=null){
            /* find out the alias with IANA tag */
            }else if((cName=UConverterAlias.getStandardName(charsetName, "IANA"))!=null){
            }else {
                /*
                    check to see if an alias already exists with x- prefix, if yes then
                    make that the canonical name
                */
                int aliasNum = UConverterAlias.countAliases(charsetName);
                String name;
                for(int i=0;i<aliasNum;i++){
                    name = UConverterAlias.getAlias(charsetName, i);
                    if(name!=null && name.indexOf("x-")==0){
                        cName = name;
                        break;
                    }
                }
                /* last resort just append x- to any of the alias and
                make it the canonical name */
                if((cName==null || cName.length()==0)){
                    name = UConverterAlias.getStandardName(charsetName, "UTR22");
                    if(name==null && charsetName.indexOf(",")!=-1){
                        name = UConverterAlias.getAlias(charsetName, 1);
                    }
                    /* if there is no UTR22 canonical name .. then just return itself*/
                    if(name==null){
                        name = charsetName;
                    }
                    cName = "x-"+ name;
                }
            }
            return cName;
        }catch (IOException ex){

        }
        return null;
     }

    /**
     * Gets the aliases associated with the converter name
     * @param encName converter name
     * @return converter names as elements in an object array
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    private static final String[] getAliases(String encName)throws IOException{
        String[] ret = null;
        int aliasNum = 0;
        int i=0;
        int j=0;
        String aliasArray[/*50*/] = new String[50];

        if(encName != null){
            aliasNum = UConverterAlias.countAliases(encName);
            for(i=0,j=0;i<aliasNum;i++){
                String name = UConverterAlias.getAlias(encName,i);
                if(name.indexOf(',')==-1){
                    aliasArray[j++]= name;
                }
            }
            ret = new String[j];
            for(;--j>=0;) {
                ret[j] = aliasArray[j];
            }

        }
        return (ret);

    }

    /**
     * Lazy-init the icuCharsets list.
     * Could be done during static initialization if constructing all of the Charsets
     * were cheap enough. See ICU ticket #11481.
     */
    private static final synchronized void loadAvailableICUCharsets() {
        if (!icuCharsets.isEmpty()) {
            return;
        }
        List<Charset> icucs = new LinkedList<Charset>();
        int num = UConverterAlias.countAvailable();
        for (int i = 0; i < num; ++i) {
            String name = UConverterAlias.getAvailableName(i);
            try {
                Charset cs = getCharset(name, "");
                icucs.add(cs);
            } catch(UnsupportedCharsetException ex) {
            } catch(IOException e) {
            }
            // add only charsets that can be created!
        }
        // Unmodifiable so that charsets().next().remove() cannot change it.
        icuCharsets = Collections.unmodifiableList(icucs);
    }

    /**
     * Returns an iterator for the available ICU Charsets.
     * Implements the abstract method of super class.
     * @return the Charset iterator
     * @stable ICU 3.6
     */
    @Override
    public final Iterator<Charset> charsets() {
        loadAvailableICUCharsets();
        return icuCharsets.iterator();
    }

    /**
     * Gets the canonical names of available ICU converters
     * @return array of available converter names
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
     public static final String[] getAvailableNames() {
        loadAvailableICUCharsets();
        String[] names = new String[icuCharsets.size()];
        int i = 0;
        for (Charset cs : icuCharsets) {
            names[i++] = cs.name();
        }
        return names;
    }

    /**
     * Return all names available
     * @return String[] an array of all available names
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
     public static final String[] getAllNames(){
        int num = UConverterAlias.countAvailable();
        String[] names = new String[num];
        for(int i=0;i<num;i++) {
            names[i] = UConverterAlias.getAvailableName(i);
        }
        return names;
    }
}
