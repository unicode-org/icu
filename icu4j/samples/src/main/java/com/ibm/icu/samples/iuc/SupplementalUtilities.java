// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2013-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.samples.iuc;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.util.UResourceBundle;

/**
 * @author srl
 *
 */
public class SupplementalUtilities {
    /**
     * Convert LDML2ICUConverter format floating point (territoryF, etc)
     * into double.
     * See: SupplementalMapper.java
     * @param n input number, such as -48123456
     * @return double value, such as -123.456
     * @internal
     */
    public static double ldml2d(int n) {
        if(n == 0) {
          return 0.;
        }
        boolean neg = false;
        if(n < 0) {
          n = -n;
          neg = true;
        }
        int exp = (n/1000000);
        n -= (exp * 1000000);
        int sexp = exp - 50; // signed exponent
        double d = n;
        d = d * Math.pow(10, (sexp-5));  // -5 because 50 isn't quite right..
        if(neg) {
          d = -d;
        }
        return d;
    }

    /** Test function */
    public static void main(String... args)    {
        
    System.out.println("Testingldml2d");
      int junk[] = { 
        49990000, // 99%
        48680000,  // 6.8%
        57344400, // ?
        52940000,  // ?
        0,
        -48123456, // gets -0.012346  not -123.456
        -52123456, // this one gets -123.456
        46100000,
        63146600
      };
    
      for(int i=0;i<junk.length;i++) {
          System.out.println(Integer.toString(junk[i]) + " -> " + Double.toString(ldml2d(junk[i])));
      }
      
      System.out.println();
      System.out.println("Testing getICUSupplementalData");
      System.out.println("SupplementalData has " + getICUSupplementalData().getSize() + " size. (nonzero is good!)" );
    }

    /**
     * Open ICU supplemental data
     * @return the bundle
     */
    public static UResourceBundle getICUSupplementalData() {
        UResourceBundle suppData = UResourceBundle.getBundleInstance(
                ICUData.ICU_BASE_NAME,
                "supplementalData",
                ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        return suppData;
    }
}
