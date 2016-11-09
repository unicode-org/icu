/*
 ******************************************************************************
 * Copyright (C) 2003-2008, International Business Machines Corporation and   *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 *
 * Created on May 2, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.ibm.icu.impl;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;



/**
 * @author ram
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public final class StringPrepDataReader implements ICUBinary.Authenticate {
    private final static boolean debug = ICUDebug.enabled("NormalizerDataReader");
    
   /**
    * <p>private constructor.</p>
    * @param inputStream ICU uprop.dat file input stream
    * @exception IOException throw if data file fails authentication 
    */
    public StringPrepDataReader(InputStream inputStream) 
                                        throws IOException{
        if(debug) System.out.println("Bytes in inputStream " + inputStream.available());
        
        unicodeVersion = ICUBinary.readHeader(inputStream, DATA_FORMAT_ID, this);
        
        if(debug) System.out.println("Bytes left in inputStream " +inputStream.available());
        
        dataInputStream = new DataInputStream(inputStream);
        
        if(debug) System.out.println("Bytes left in dataInputStream " +dataInputStream.available());
    }
    
    public void read(byte[] idnaBytes,
                        char[] mappingTable) 
                        throws IOException{

        //Read the bytes that make up the idnaTrie  
        dataInputStream.readFully(idnaBytes);
        
        //Read the extra data
        for(int i=0;i<mappingTable.length;i++){
            mappingTable[i]=dataInputStream.readChar();
        }
    }
    
    public byte[] getDataFormatVersion(){
        return DATA_FORMAT_VERSION;
    }
    
    public boolean isDataVersionAcceptable(byte version[]){
        return version[0] == DATA_FORMAT_VERSION[0] 
               && version[2] == DATA_FORMAT_VERSION[2] 
               && version[3] == DATA_FORMAT_VERSION[3];
    }
    public int[] readIndexes(int length)throws IOException{
        int[] indexes = new int[length];
        //Read the indexes
        for (int i = 0; i <length ; i++) {
             indexes[i] = dataInputStream.readInt();
        }
        return indexes;
    } 
    
    public byte[] getUnicodeVersion(){
        return unicodeVersion;
    }
    // private data members -------------------------------------------------
      

    /**
    * ICU data file input stream
    */
    private DataInputStream dataInputStream;
    private byte[] unicodeVersion;                             
    /**
    * File format version that this class understands.
    * No guarantees are made if a older version is used
    * see store.c of gennorm for more information and values
    */
    ///* dataFormat="SPRP" 0x53, 0x50, 0x52, 0x50  */ 
    private static final byte DATA_FORMAT_ID[] = {(byte)0x53, (byte)0x50, 
                                                    (byte)0x52, (byte)0x50};
    private static final byte DATA_FORMAT_VERSION[] = {(byte)0x3, (byte)0x2, 
                                                        (byte)0x5, (byte)0x2};
    
}
