/*
 *******************************************************************************
 * Copyright (C) 2003, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/stringprep/NFS4StringPrep.java,v $
 * $Date: 2003/08/27 21:14:23 $
 * $Revision: 1.4 $
 *
 *******************************************************************************
*/
package com.ibm.icu.dev.test.stringprep;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.text.StringPrep;
import com.ibm.icu.text.UCharacterIterator;

/**
 * @author ram
 *
 * This is a dumb implementation of NFS4 profiles. It is a direct port of
 * C code, does not use Object Oriented principles. Quick and Dirty implementation
 * for testing.
 */
public final class NFS4StringPrep {
    private static final String[] NFS4DataFileNames ={
        "nfscss.spp",
        "nfscsi.spp",
        "nfscis.spp",
        "nfsmxp.spp",
        "nfsmxs.spp"
    };
    private StringPrep nfscss = null;
    private StringPrep nfscsi = null;
    private StringPrep nfscis = null;
    private StringPrep nfsmxp = null;
    private StringPrep nfsmxs = null;
    //singleton instance
    private static NFS4StringPrep prep = null;
    

    private  NFS4StringPrep ()throws IOException{
      
      InputStream  nfscssFile = TestUtil.getDataStream(NFS4DataFileNames[0]);
      nfscss = StringPrep.getInstance(nfscssFile);
      nfscssFile.close();
      
      InputStream  nfscsiFile = TestUtil.getDataStream(NFS4DataFileNames[1]);
      nfscsi = StringPrep.getInstance(nfscsiFile);
      nfscsiFile.close();
      
      InputStream  nfscisFile = TestUtil.getDataStream(NFS4DataFileNames[2]);
      nfscis = StringPrep.getInstance(nfscisFile);
      nfscsiFile.close();
      
      InputStream  nfsmxpFile = TestUtil.getDataStream(NFS4DataFileNames[3]);
      nfsmxp = StringPrep.getInstance(nfsmxpFile);
      nfscsiFile.close();
      
      InputStream  nfsmxsFile = TestUtil.getDataStream(NFS4DataFileNames[4]);
      nfsmxs = StringPrep.getInstance(nfsmxsFile);
      nfsmxsFile.close();
      
    }
    
    public static synchronized final NFS4StringPrep getInstance()
                        throws IOException{
        if(prep==null){
            prep = new NFS4StringPrep();
        } 
        return prep;
    }
    
    private static byte[] prepare(byte[] src, StringPrep prep)
                throws StringPrepParseException, UnsupportedEncodingException{
        String s = new String(src, "UTF-8");
        UCharacterIterator iter =  UCharacterIterator.getInstance(s);
        StringBuffer out = prep.prepare(iter,StringPrep.DEFAULT);
        return out.toString().getBytes("UTF-8");
    }
    
    public static byte[] cs_prepare(byte[] src, boolean caseInsensitive)
                         throws IOException, StringPrepParseException, UnsupportedEncodingException{
        NFS4StringPrep prep = getInstance();
        if(caseInsensitive){
            return prepare(src, prep.nfscsi);
        }else{
            return prepare(src, prep.nfscss);
        }
    }
    
    public static byte[] cis_prepare(byte[] src)
                         throws IOException, StringPrepParseException, UnsupportedEncodingException{
        NFS4StringPrep prep = getInstance();
        return prepare(src, prep.nfscis);
    }  
    
    /* sorted array for binary search*/
    private static final String[] special_prefixes={
        "ANONYMOUS",    
        "AUTHENTICATED",
        "BATCH", 
        "DIALUP", 
        "EVERYONE", 
        "GROUP",
        "INTERACTIVE",  
        "NETWORK", 
        "OWNER",
    };


    /* binary search the sorted array */
    private static final int findStringIndex(String[] sortedArr,String target){

        int left, middle, right,rc;

        left =0;
        right= sortedArr.length-1;

        while(left <= right){
            middle = (left+right)/2;
            rc= sortedArr[middle].compareTo(target);
        
            if(rc<0){
                left = middle+1;
            }else if(rc >0){
                right = middle -1;
            }else{
                return middle;
            }
        }
        return -1;
    }
    private static final char AT_SIGN = '@';
    
    public static byte[] mixed_prepare(byte[] src)
                         throws IOException, StringPrepParseException, UnsupportedEncodingException{
        String s = new String(src, "UTF-8");
        int index = s.indexOf(AT_SIGN);
        StringBuffer out = new StringBuffer();
        NFS4StringPrep prep = getInstance();
        if(index > -1){
            /* special prefixes must not be followed by suffixes! */
            String prefixString = s.substring(0,index);
            int i= findStringIndex(special_prefixes, prefixString);
            String suffixString = s.substring(index+1, s.length());
            if(i>-1 && !suffixString.equals("")){
                throw new StringPrepParseException("Suffix following a special index", StringPrepParseException.INVALID_CHAR_FOUND);
            }
            UCharacterIterator prefix = UCharacterIterator.getInstance(prefixString);
            UCharacterIterator suffix = UCharacterIterator.getInstance(suffixString);
            out.append(prep.nfsmxp.prepare(prefix,StringPrep.DEFAULT));
            out.append(AT_SIGN); // add the delimiter
            out.append(prep.nfsmxs.prepare(suffix, StringPrep.DEFAULT));
        }else{
            UCharacterIterator iter = UCharacterIterator.getInstance(s);
            out.append(prep.nfsmxp.prepare(iter,StringPrep.DEFAULT));
            
        }
       return out.toString().getBytes("UTF-8");
    }
    
}
