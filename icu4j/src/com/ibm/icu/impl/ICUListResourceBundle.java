/**
 *******************************************************************************
 * Copyright (C) 2001-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/ICUListResourceBundle.java,v $
 * $Date: 2003/10/16 23:42:48 $
 * $Revision: 1.13 $
 *
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

        ArrayList vec = new ArrayList();
        int count = 0;
        int pos = 0;
        final int MAXLENGTH = 0x8000; // max buffer size - 32K
        int length = 0x80; // start with small buffers and work up
        do {
            pos = 0;
            length = length >= MAXLENGTH ? MAXLENGTH : length * 2;
            byte[] buffer = new byte[length];
            try {
                do {
                    int n = stream.read(buffer, pos, length - pos);
                    if (n == -1) {
                    break;
                    }
                    pos += n;
                } while (pos < length);
            }
            catch (IOException e) {
            }
            vec.add(buffer);
            count += pos;
        } while (pos == length);


        byte[] data = new byte[count];
        pos = 0;
        for (int i = 0; i < vec.size(); ++i) {
            byte[] buf = (byte[])vec.get(i);
            int len = Math.min(buf.length, count - pos);
            System.arraycopy(buf, 0, data, pos, len);
            pos += len;
        }
        return data;
    }

    private static char[] readToEOS(InputStreamReader stream) {
        ArrayList vec = new ArrayList();
        int count = 0;
        int pos = 0;
        final int MAXLENGTH = 0x8000; // max buffer size - 32K
        int length = 0x80; // start with small buffers and work up
        do {
            pos = 0;
            length = length >= MAXLENGTH ? MAXLENGTH : length * 2;
            char[] buffer = new char[length];
            try {
                do {
                    int n = stream.read(buffer, pos, length - pos);
                    if (n == -1) {
                    break;
                    }
                    pos += n;
                } while (pos < length);
            }
            catch (IOException e) {
            }
            vec.add(buffer);
            count += pos;
        } while (pos == length);

        char[] data = new char[count];
        pos = 0;
        for (int i = 0; i < vec.size(); ++i) {
            char[] buf = (char[])vec.get(i);
            int len = Math.min(buf.length, count - pos);
            System.arraycopy(buf, 0, data, pos, len);
            pos += len;
        }
        return data;
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
                return null;
            }

            if(expanded==null){
                expanded= Utility.RLEStringToByteArray(compressed);
            }
            return expanded;
        }

    }
    private interface RedirectedResource{
        public Object getResource(Object obj);
    }

    public static class ResourceBinary implements RedirectedResource{
        private byte[] expanded=null;
        private String resName=null;
        public ResourceBinary(String name){
            resName=name;
        }
        public Object getResource(Object obj){
            if(expanded==null){
                InputStream stream = obj.getClass().getResourceAsStream(resName);
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
            resName=name;
        }
        public Object getResource(Object obj){
            if(expanded==null){
                // Resource strings are always UTF-8
                InputStream stream = obj.getClass().getResourceAsStream(resName);
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

    public static class Alias{
        public Alias(String path){
            pathToResource = path;
        };
        private final char RES_PATH_SEP_CHAR = '/';
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
                bundleName=className.substring(j+1,className.indexOf("_"));
                keyPath=pathToResource.substring(i+1);

                if(i!=-1){
                    locale = pathToResource.substring(0,i);
                }else{
                    locale=keyPath;
                    keyPath=parentKey;
                    className=packageName+"."+bundleName+"_"+locale;
                }

            }
            ResourceBundle bundle = null;
            // getResourceBundle guarantees that the CLASSPATH will be searched
            // for loading the resource with name <bundleName>_<localeName>.class
            bundle = ICULocaleData.getResourceBundle(packageName,bundleName,locale);
            
            return findResource(bundle, className, parentKey, index, keyPath, visited);
        
        }



        private boolean isIndex(String s){
             if(s.length()==1){
                char c = s.charAt(0);
                return Character.isDigit(c);
             }
             return false;
        }
        private int getIndex(String s){
             if(s.length()==1){
                char c = s.charAt(0);
                if(Character.isDigit(c)){
                  return Integer.valueOf(s).intValue();
                }
             }
             return -1;
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
                o[i][1]=resolveAliases((Object)o[i][1],className,key,visited);
                i++;
            }
            return o;
        }
        private Object resolveAliases(Object[] o,String className, String key,Hashtable visited){
            int i =0;
            while(i<o.length){
                o[i]=resolveAliases((Object)o[i],className,key,visited);
                i++;
            }
            return o;
        }

        private String[] split(String source, char delimiter){

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
    }

}

