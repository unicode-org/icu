/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/lang/TestCharacter.java,v $
 * $Date: 2003/02/05 05:50:05 $
 * $Revision: 1.1 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.lang;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

public class TestCharacter extends TestGroup {
    public static void main(String[] args) {
        new TestCharacter().run(args);
    }

    public TestCharacter() {
        super(
              new String[] { 
                  "UCharacterTest", 
                  "UCharacterCaseTest", 
                  "UCharacterIteratorTest",
                  "UCharacterCategoryTest", 
                  "UCharacterDirectionTest", 
                  "UPropertyAliasesTest",
                  "UTF16Test" 
              },
              "Character Property and UTF16 Tests");
    }
}
