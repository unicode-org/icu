/**
 *******************************************************************************
 * Copyright (C) 2001-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

/**
 * A list resource bundle that does redirection
 * because otherwise some of our resource class files
 * are too big for the java runtime to handle.
 */

package com.ibm.icu.impl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Hashtable;

public class ICUListResourceBundle extends ListResourceBundle {
    private static final String ICUDATA = "ICUDATA";
    private static final String ICU_BUNDLE_NAME = "LocaleElements";
    private static final String ICU_PACKAGE_NAME ="com.ibm.icu.impl.data";
    private static final String ENCODING="UTF-8";

    /* package */ Locale icuLocale;
    /* package */ void setParentX(ResourceBundle b) {
        setParent(b);
    }

    public Locale getLocale() {
        return icuLocale;
    }

    protected ICUListResourceBundle() {
    }

    private Hashtable visited = new Hashtable();
    /**
     * Subclassers must statically initialize this
     */
    protected Object[][] contents;

    /**
     * This is our cache
     */
    private Object[][] realContents;

    /**
     * See base class description
     */
    protected Object[][] getContents(){
        // we replace any redirected values with real values in a cloned array
        if (realContents == null) {
            realContents = contents;
            for (int i = 0; i < contents.length; ++i) {
                Object newValue = getRedirectedResource((String)contents[i][0],contents[i][1], -1);
                if (newValue != null) {
                    if (realContents == contents) {
                        realContents = (Object[][])contents.clone();
                    }
                    realContents[i] = new Object[] { contents[i][0], newValue };
                }
            }
        }
        return realContents;
    }

    /**
     * Return null if value is already in existing contents array, otherwise fetch the
     * real value and return it.
     */
    private Object getRedirectedResource(String key, Object value, int index) {

        if (value instanceof Object[][]) {
            Object[][] aValue = (Object[][])value;
            int i=0;
            while(i < aValue.length){
                int j=0;
                while(j < aValue[i].length){
                    aValue[i][j] = getRedirectedResource((String)aValue[i][0],aValue[i][j], i);
                    j++;
                }
                i++;
            }
        }else if (value instanceof Object[]){
            Object[] aValue = (Object[]) value;
            int i=0;
            while( i < aValue.length){
                aValue[i] = getRedirectedResource(key,aValue[i], i);
                i++;
            }
        }else if(value instanceof Alias){
        
            String cName = this.getClass().getName();
            visited.clear();
            visited.put(cName+key,"");
            return ((Alias)value).getResource(cName,key,index, visited);
        }else if(value instanceof RedirectedResource){
            return ((RedirectedResource)value).getResource(this);
        }

        return value;
    }

    private static byte[] readToEOS(InputStream stream) {
        // As of 3.0 this method reads streams of length 264..274008
        // from the core data.  We progressively double the buffer
        // size to reduce the number of allocations required.
        try {
            ArrayList vec = new ArrayList();
            int count = 0;
            int length = 0x200; // smallest 2^n >= min stream len
            final int MAXLENGTH = 0x8000;
            int pos = -1;
            for (;;) {
                byte[] buffer = new byte[length];
                pos = 0;
                do {
                    int n = stream.read(buffer, pos, length - pos);
                    if (n == -1) {
                        break;
                    }
                    pos += n;
                } while (pos < length);
                count += pos;
                vec.add(buffer);
                if (pos < length) {
                    break;
                }
                if (length < MAXLENGTH) {
                    length <<= 1;
                }
            }

            // System.out.println("\ncount " + count + " bytes from " + stream);

            byte[] data = new byte[count];
            pos = 0;
            for (int i = 0; i < vec.size(); ++i) {
                byte[] buf = (byte[])vec.get(i);
                int len = Math.min(buf.length, count - pos);
                System.arraycopy(buf, 0, data, pos, len);
                pos += len;
            }
            // assert pos==count;
            return data;
        } catch (IOException e) {
            throw new MissingResourceException(e.getMessage(),"","");
        }
    }

    private static char[] readToEOS(InputStreamReader stream) {
        // As of 3.0 this method reads streams of length 41990..41994
        // from the core data.  The IBM 1.4 UTF8 converter doesn't
        // handle buffering reliably (it throws an exception) so we
        // are forced to read everything in one chunk.
        try {
            int length = 0x10000; // smallest 2^n >= max stream len
            final int MAXLENGTH = 0x40000000;
            int n;
            char[] buffer;
            for (;;) {
                buffer = new char[length];
                n = stream.read(buffer, 0, length);
                if (n >= 0 && n < length) {
                    break;
                }
                if (length < MAXLENGTH) {
                    stream.reset();
                    length <<= 1;
                } else {
                    throw new IllegalStateException("maximum input stream length exceeded");
                }
            }

            // System.out.println("\ncount " + n + " chars from " + stream);

            char[] data = new char[n];
            System.arraycopy(buffer, 0, data, 0, n);
            return data;
        } catch (IOException e) {
            throw new MissingResourceException(e.getMessage(),"","");
        }
    }
    /*
    public static class CompressedString implements RedirectedResource{
        private String expanded=null;
        private String compressed=null;
        public CompressedString(String str){
           compressed=str;
        }
        public Object getResource(Object obj){
            if(compressed==null){
                return null;
            }
            if(expanded==null){
                expanded= new String(Utility.RLEStringToCharArray(compressed));
            }
            return expanded;
        }
    }
    */
    public static class CompressedBinary implements RedirectedResource{
        private byte[] expanded=null;
        private String compressed=null;
        public CompressedBinary(String str){
           compressed = str;
        }
        public Object getResource(Object obj){
            if(compressed==null){
                return new byte[0];
            }

            if(expanded==null){
                expanded= Utility.RLEStringToByteArray(compressed);
            }
            return expanded ==null ? new byte[0]: expanded;
        }

    }
    private interface RedirectedResource{
        public Object getResource(Object obj);
    }

    public static class ResourceBinary implements RedirectedResource{
        private byte[] expanded=null;
        private String resName=null;
        public ResourceBinary(String name){
            resName="data/" + name;
        }
        public Object getResource(Object obj) {
            if(expanded==null){
                InputStream stream = ICUData.getStream(resName);
                if(stream!=null){
                    //throw new MissingResourceException("",obj.getClass().getName(),resName);
                    expanded = readToEOS(stream);
                    return expanded;
                }
            }
            return "";
        }
    }

    public static class ResourceString implements RedirectedResource{
        private char[] expanded=null;
        private String resName=null;
        public ResourceString(String name){
            resName="data/"+name;
        }
        public Object getResource(Object obj) {
            if(expanded==null){
                // Resource strings are always UTF-8
                InputStream stream = ICUData.getStream(resName);
                if(stream!=null){
                    //throw new MissingResourceException("",obj.getClass().getName(),resName);

                    try{
                        InputStreamReader reader =  new InputStreamReader(stream,ENCODING);
                        expanded = readToEOS(reader);
                    }catch(UnsupportedEncodingException ex){
                        throw new RuntimeException("Could open converter for encoding: " +ENCODING);
                    }
                    return new String(expanded);
                }

            }
            return "";
        }
    }
    
    private static final char RES_PATH_SEP_CHAR = '/';
    
    public static class Alias{
        public Alias(String path){
            pathToResource = path;
        }
        
        private String pathToResource;

        private Object getResource(String className, String parentKey, int index, Hashtable visited){
            String packageName=null,bundleName=null, locale=null, keyPath=null;

            if(pathToResource.indexOf(RES_PATH_SEP_CHAR)==0){
                int i =pathToResource.indexOf(RES_PATH_SEP_CHAR,1);
                int j =pathToResource.indexOf(RES_PATH_SEP_CHAR,i+1);
                bundleName=pathToResource.substring(1,i);
                locale=pathToResource.substring(i+1);
                if(j!=-1){
                    locale=pathToResource.substring(i+1,j);
                    keyPath=pathToResource.substring(j+1,pathToResource.length());
                }
                //there is a path included
                if(bundleName.equals(ICUDATA)){
                    bundleName = ICU_BUNDLE_NAME;
                    packageName = ICU_PACKAGE_NAME;
                }

            }else{
                //no path start with locale
                int i =pathToResource.indexOf(RES_PATH_SEP_CHAR);
                //If this is a bundle with locale name following it
                //then it should be of type <bundle name>_<locale>
                //if not we donot guarantee that this will work
                int j = className.lastIndexOf(".");
                packageName=className.substring(0,j);
                int underScoreIndex = className.indexOf("_");
                if(underScoreIndex>=0){
                    bundleName=className.substring(j+1,className.indexOf("_"));
                }else{
                    bundleName = className.substring(j+1,className.length());
                }
                keyPath=pathToResource.substring(i+1);

                if(i!=-1){
                    locale = pathToResource.substring(0,i);
                }else{
                    locale=keyPath;
                    keyPath=parentKey;
                    if(locale==null || locale.equals("root")){
                        className=packageName+"."+bundleName;
                    }else{
                        className=packageName+"."+bundleName+"_"+ locale;
                    }
                    
                }

            }
            
            ResourceBundle bundle = null;
            // getResourceBundle guarantees that the CLASSPATH will be searched
            // for loading the resource with name <bundleName>_<localeName>.class
            if(locale==null || locale.equals("root")){
                bundle = ICULocaleData.getResourceBundle(packageName,bundleName,"");
            }else{
                bundle = ICULocaleData.getResourceBundle(packageName,bundleName,locale);
            }
            
            return findResource(bundle, className, parentKey, index, keyPath, visited);
        
        }
        
        private Object findResource(Object[][] contents, String key){
            for (int i = 0; i < contents.length; ++i) {
                // key must be non-null String, value must be non-null
                String tempKey = (String) contents[i][0];
                Object value = contents[i][1];
                if (tempKey == null || value == null) {
                    throw new NullPointerException();
                }
                if(tempKey.equals(key)){
                    return value;
                }
            }
            return null;
        }
        
        private Object findResource(Object o , String[] keys, int start, int index){
            Object obj = o;
            if( start < keys.length && keys[start] !=null){
                if(obj instanceof Object[][]){
                    obj = findResource((Object[][])obj,keys[start]);
                }else if(obj instanceof Object[] && isIndex(keys[start])){
                    obj = ((Object[])obj)[getIndex(keys[start])];
                }
                if(start+1 < keys.length && keys[start+1] !=null){
                    obj = findResource(obj,keys,start+1, index);
                }
            }else{
                //try to find the corresponding index resource
                if(index>=0){
                    if(obj instanceof Object[][]){
                        obj = findResource((Object[][])obj,Integer.toString(index));
                    }else if(obj instanceof Object[]){
                        obj = ((Object[])obj)[index];
                    }
                }
            }
            return obj;
        }
        private Object findResource(ResourceBundle bundle, String className, String requestedKey, int index, String aliasKey, Hashtable visited){

            if(aliasKey != null && visited.get(className+aliasKey)!=null){
                throw new MissingResourceException("Circular Aliases in bundle.",bundle.getClass().getName(),requestedKey);
            }
            if(aliasKey==null){
                // currently we do an implicit key lookup
                // return ((ICUListResourceBundle)bundle).getContents();
                aliasKey = requestedKey;
            }
            
            visited.put(className+requestedKey,"");

            String[] keys = split(aliasKey,RES_PATH_SEP_CHAR);
            Object o =null;
            if(keys.length>0){
                o = bundle.getObject(keys[0]);
                o = findResource(o, keys, 1, index);
            }
            o=resolveAliases(o,className,aliasKey,visited);
            return o;
        }
        private  Object resolveAliases(Object o,String className,String key, Hashtable visited){
            if(o instanceof Object[][]){
                o = resolveAliases((Object[][])o,className,key, visited);
            }else if(o instanceof Object[]){
                 o = resolveAliases((Object[])o,className,key, visited);
            }else if(o instanceof Alias){
                return ((Alias)o).getResource(className,key, -1, visited);
            }
            return o;
        }
        private Object resolveAliases(Object[][] o,String className, String key,Hashtable visited){
            int i =0;
            while(i<o.length){
                o[i][1]=resolveAliases(o[i][1],className,key,visited);
                i++;
            }
            return o;
        }
        private Object resolveAliases(Object[] o,String className, String key,Hashtable visited){
            int i =0;
            while(i<o.length){
                o[i]=resolveAliases(o[i],className,key,visited);
                i++;
            }
            return o;
        }


    }
    private static String[] split(String source, char delimiter){

        char[] src = source.toCharArray();
        int index = 0;
        int numdelimit=0;
        // first count the number of delimiters
        for(int i=0;i<source.length();i++){
            if(src[i]==delimiter){
                numdelimit++;
            }
        }
        String[] values =null;
        values = new String[numdelimit+2];
        // now split
        int old=0;
        for(int j=0;j<src.length;j++){
            if(src[j]==delimiter){
                values[index++] = new String(src,old,j-old);
                old=j+1/* skip after the delimiter*/;
            }
        }
        if(old <src.length)
            values[index++]=new String(src,old,src.length-old);
        return values;
    }
    
    /**
     * This method performs multilevel fallback for fetching items from the bundle
     * e.g:
     * If resource is in the form
     * de__PHONEBOOK{
     *      collations{
     *              default{ "phonebook"}
     *      }
     * }
     * If the value of "default" key needs to be accessed, then do:
     * <code>
     *  ResourceBundle bundle = new ResourceBundle(getLocaleFromString("de__PHONEBOOK"));
     *  Object result = null;
     *  if(bundle instanceof ICUListResourceBundle){
     *      result = ((ICUListResourceBundle) bundle).getObjectWithFallback("collations/default");
     *  }
     * </code>
     * @param path  The path to the required resource key
     * @return Object represented by the key
     * @exception MissingResourceException
     */
    public final Object getObjectWithFallback(String path) 
                  throws MissingResourceException{
        String[] keys = split(path, RES_PATH_SEP_CHAR);
        Object result = null;      
        ICUListResourceBundle actualBundle = this;
        
        
        // now recuse to pick up sub levels of the items
        result = findResourceWithFallback(keys, actualBundle);
                    
        if(result == null){
            throw new MissingResourceException("Could not find the resource in ",this.getClass().getName(),path);
        }
        return result;
    }
    

    private Object findResourceWithFallback(String[] keys,
                                            ICUListResourceBundle actualBundle){

        Object obj = null; 
        
        while(actualBundle != null){
            // get the top level resource 
            // getObject is a method on the ResourceBundle class that
            // performs the normal fallback
            obj = actualBundle.getObject(keys[0], actualBundle);
            
            // now find the bundle from the actual bundle 
            // if this bundle does not contain the top level resource,
            // then we can be sure that it does not contain the sub elements
            obj = findResourceWithFallback(obj, keys, 1, 0);
            // did we get the contents? the break
            if(obj != null){
                break;
            }
            // if not try the parent bundle
            actualBundle = (ICUListResourceBundle) actualBundle.parent;
        
        }
        
        return obj;
    }
    private Object findResourceWithFallback(Object o , String[] keys, int start, 
                                            int index){
        Object obj = o;
        
        
            if( start < keys.length && keys[start] !=null){
                if(obj instanceof Object[][]){
                    obj = findResourceWithFallback((Object[][])obj,keys[start]);
                }else if(obj instanceof Object[] && isIndex(keys[start])){
                    obj = ((Object[])obj)[getIndex(keys[start])];
                }
                if(start+1 < keys.length && keys[start+1] !=null){
                    obj = findResourceWithFallback(obj,keys,start+1, index);
                }
            }else{
                //try to find the corresponding index resource
                if(index>=0){
                    if(obj instanceof Object[][]){
                        obj = findResourceWithFallback((Object[][])obj,
                                                        Integer.toString(index));
                    }else if(obj instanceof Object[]){
                        obj = ((Object[])obj)[index];
                    }
                }
            }

        return obj;
    }
    
    private Object findResourceWithFallback(Object[][] cnts, String key){
        Object obj = null;

        for (int i = 0; i < cnts.length; ++i) {
            // key must be non-null String
            String tempKey = (String) cnts[i][0];
            obj = cnts[i][1];
            if(tempKey != null && tempKey.equals(key)){
                return obj;
            }
        }

        return null;
    }
    
    private final Object getObject(String key, 
                                   ICUListResourceBundle actualBundle) {
        Object obj = handleGetObject(key);
        if (obj == null) {
            ICUListResourceBundle p = (ICUListResourceBundle) this.parent;
            while( p!=null){
                obj = p.handleGetObject(key);
                if(obj != null){
                    actualBundle = p;
                    break; 
                }
                p = (ICUListResourceBundle) p.parent;
            }
        }
        return obj;
    }
    private static boolean isIndex(String s){
         if(s.length()==1){
            char c = s.charAt(0);
            return Character.isDigit(c);
         }
         return false;
    }
    private static int getIndex(String s){
         if(s.length()==1){
            char c = s.charAt(0);
            if(Character.isDigit(c)){
              return Integer.valueOf(s).intValue();
            }
         }
         return -1;
    }
}

