/*
*******************************************************************************
* Copyright (C) 1996-2005, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICUBinary;
import java.io.IOException;
import java.io.ByteArrayInputStream;

/**
* Testing class for Trie. Tests here will be simple, since both CharTrie and 
* IntTrie are very similar and are heavily used in other parts of ICU4J.
* Codes using Tries are expected to have detailed tests.
* @author Syn Wee Quek
* @since release 2.1 Jan 01 2002
*/
public final class ICUBinaryTest extends TestFmwk 
{ 
    // constructor ---------------------------------------------------
  
    /**
    * Constructor
    */
    public ICUBinaryTest()
    {
    }
      
    // public methods -----------------------------------------------
    
    public static void main(String arg[]) 
    {
        ICUBinaryTest test = new ICUBinaryTest();
        try {
            test.run(arg);
        } catch (Exception e) {
            test.errln("Error testing icubinarytest");
        }
    }
    
    /**
     * Testing the constructors of the Tries
     */
    public void TestReadHeader() 
    {
        byte formatid[] = {1, 2, 3, 4};
        byte array[] = {
            // header size
            0, 0x18, 
            // magic numbers
            (byte)0xda, 0x27, 
            // size
            0, 0, 
            // reserved word
            0, 0, 
            // bigendian
            1, 
            // charset 
            0,
            // charsize
            2, 
            // reserved byte
            0, 
            // data format id
            1, 2, 3, 4,
            // dataVersion
            1, 2, 3, 4,
            // unicodeVersion
            3, 2, 0, 0
        };
        ByteArrayInputStream inputstream = new ByteArrayInputStream(array);
        ICUBinary.Authenticate authenticate 
                = new ICUBinary.Authenticate() {
                    public boolean isDataVersionAcceptable(byte version[])
                    {
                        return version[0] == 1;
                    }
                };
        // check full data version
        try {
            ICUBinary.readHeader(inputstream, formatid, authenticate);
        } catch (IOException e) {
            errln("Failed: Lenient authenticate object should pass ICUBinary.readHeader");
        }
        // no restriction to the data version
        try {
            inputstream.reset();
            ICUBinary.readHeader(inputstream, formatid, null);
        } catch (IOException e) {
            errln("Failed: Null authenticate object should pass ICUBinary.readHeader");
        }
        // lenient data version
        try {
            inputstream.reset();
            ICUBinary.readHeader(inputstream, formatid, authenticate);
        } catch (IOException e) {
            errln("Failed: Lenient authenticate object should pass ICUBinary.readHeader");
        }
        // changing the version to an incorrect one, expecting failure
        array[16] = 2;
        try {
            inputstream.reset();
            ICUBinary.readHeader(inputstream, formatid, authenticate);
            errln("Failed: Invalid version number should not pass authenticate object");
        } catch (IOException e) {
        	logln("PASS: ICUBinary.readHeader with invalid version number failed as expected");
        }
    }
}

