/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
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
                  "UTF16Test",
          "UCharacterSurrogateTest"
              },
              "Character Property and UTF16 Tests");
    }
}
