/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/utility/DifferTest.java,v $
* $Date: 2001/08/31 00:19:16 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.utility;


public class DifferTest {
    public static final String copyright =
      "Copyright (C) 2000, IBM Corp. and others. All Rights Reserved.";

    static final void main(String[] args) { // for testing

        String[] as = {"a", "b", "20D4", "0344", "20D5", "20D6", "20D7", "20D8", "20D9"};
        String[] bs = {"a", "b", "20D4", "20D5", "0344", "20D6", "20D7", "20D8", "20D9"};
        Differ differ = new Differ(50,2);
        int max = as.length;
        if (max < bs.length) max = bs.length;
        for (int j = 0; j <= max; ++j) {
            if (j < as.length) differ.addA(as[j]);
            if (j < bs.length) differ.addB(bs[j]);
            differ.checkMatch(j == max);

            if (differ.getACount() != 0 || differ.getBCount() != 0) {
                if (differ.getACount() != 0) {
                    for (int i = -1; i < differ.getACount()+1; ++i) {
                        System.out.println("a: " + differ.getALine(i) + " " + differ.getA(i));
                    }
                }
                if (differ.getBCount() != 0) {
                    if (differ.getACount() != 0) System.out.println();
                    for (int i = -1; i < differ.getBCount()+1; ++i) {
                        System.out.println("b: " + differ.getBLine(i) + " " + differ.getB(i));
                    }
                }
            }
            System.out.println("----");
            //differ.flush();
        }
    }
}