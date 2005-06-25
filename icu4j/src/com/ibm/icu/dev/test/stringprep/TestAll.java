/*
 *******************************************************************************
 * Copyright (C) 2003-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
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
                      "TestIDNARef",
                      "IDNAConformanceTest",
                  },
                  "StringPrep and IDNA test"
              );
    }

    public static final String CLASS_TARGET_NAME = "StringPrep";
   

}
