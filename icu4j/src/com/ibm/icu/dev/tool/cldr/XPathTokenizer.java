/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 */
package com.ibm.icu.dev.tool.cldr;

/**
 * @author ram
 */

public class XPathTokenizer{
    char[] xpath;
    int current;
    
    public XPathTokenizer(StringBuffer path){
        this(path.toString());
    }
    public XPathTokenizer(String path){
        xpath = path.toCharArray();
        if(path.indexOf("..")== 0){
            current = 0;
        }else{
            // this is absolute
            // since xpath starts with "//"
            current = 2;
        }
    }
    public String nextToken(){
        boolean inquote = false;
        String retval;
        int save = current; 
        while(current < xpath.length){
            switch(xpath[current]){
                case '\'':
                   inquote = (inquote==true)? false:true;
                   current++;
                   break;
                case '/':
                    if(inquote==false){
                        retval  = new String(xpath,save, (current-save));
                        current++; //skip past the separator
                        return retval;
                    }
                    //fall through
                default:
                    current++;
            }
        }
        if(current == xpath.length){
            retval = new String(xpath,save, (current-save));
            current++;
            return retval;
        }
        return null;
    }
    public static StringBuffer deleteToken(StringBuffer xpath){
        int length = xpath.length(); 
        int current =  length - 1;
        while(current > 0 ){
            boolean inquote = false;
            switch(xpath.charAt(current)){
                case '\'':
                    inquote = (inquote==true)? false:true;
                    current--;
                    break;
                case '/':
                    if(inquote==false){
                        if(current < length){
                            xpath.delete(current, length);
                        }
                        return xpath;
                    }
                    //fall through
                default:
                    current--;
            }
        }
        return xpath;
    }
    /**
     * This method will try to convert a relative xpath to absolute
     * xpath. 
     * TODO: The method will only resolve relative tokens in the begining
     * of the string. Try to handle embedded ".."
     * @param xpath
     * @param fullPath
     * @return
     */
    public static StringBuffer relativeToAbsolute(String xpath, StringBuffer fullPath){
        if(!xpath.startsWith("..")){
            fullPath.setLength(0);
            fullPath.append(xpath);
            return fullPath;
        }
        XPathTokenizer tokenizer = new XPathTokenizer(xpath);
        String token=tokenizer.nextToken();
        StringBuffer retVal = new StringBuffer();
        retVal.append(fullPath);
        while(token.equals("..")){
            deleteToken(retVal);
            token = tokenizer.nextToken();
        }
        while(token!=null){
            retVal.append("/");
            retVal.append(token);
            token = tokenizer.nextToken();
        }
        return retVal;
    }
    
}

