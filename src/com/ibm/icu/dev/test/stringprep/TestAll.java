/*
 *******************************************************************************
 * Copyright (C) 2003, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/stringprep/TestAll.java,v $
 * $Date: 2003/08/28 23:03:06 $
 * $Revision: 1.2 $
 *
 *******************************************************************************
*/
package com.ibm.icu.dev.test.stringprep;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * @author ram
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class TestAll extends TestGroup {
   
    public static void main(String[] args) throws Exception {
        new TestAll().run(args);
    }

    public TestAll() {
        super(
                  new String[] { 
                      "TestIDNA", 
                      "TestStringPrep",
                      "TestIDNARef" 
                  },
                  "StringPrep and IDNA test"
              );
    }

    public static final String CLASS_TARGET_NAME = "StringPrep";
   

}
