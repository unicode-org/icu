/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/TestUtility.java,v $
* $Date: 2002/07/14 22:04:49 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.utility;

import java.util.*;
import java.text.*;
import java.io.*;

public class TestUtility {
    
    static public class MyEnum extends EnumBase {
        public static MyEnum
            ZEROED = (MyEnum) makeNext(myEnum.getClass()),
            SHIFTED = (MyEnum) makeNext(),
            NON_IGNORABLE = (MyEnum) makeNext(),
                
            FIRST_ENUM = ZEROED,
            LAST_ENUM = NON_IGNORABLE;
        public MyEnum next(int value) {
            return (MyEnum) internalNext(value);
        }
        protected MyEnum() {}
    }
    
    static public void main (String[] args) {
        for (MyEnum i = MyEnum.FIRST_ENUM; i != null; i = i.next()) {
            System.out.println(i.getValue());
        }
    }
    
}