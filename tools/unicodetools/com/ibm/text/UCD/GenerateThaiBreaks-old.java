/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateThaiBreaks-old.java,v $
* $Date: 2002/10/05 01:28:58 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import java.io.*;
import com.ibm.text.utility.*;
import com.ibm.text.UnicodeSet;
import java.util.*;

public class GenerateThaiBreaks {
  public static void main(String [] args) throws IOException {
    
    BufferedReader br = new BufferedReader(
      new InputStreamReader(
        new FileInputStream("\\icu4j\\src\\data\\thai6.ucs"), "UnicodeLittle"));
    try {
        Main.setUCD();
        UnicodeSet ignorables = new UnicodeSet("[:M:]");
        ignorables.retain(0x0E00, 0x0E7F); // just Thai block
        ignorables.add(0x0E40, 0x0E44); // add logical order exception
        ignorables.add(0, ' '); // add controls
        ignorables.add('.');
        
        UnicodeSet initials = new UnicodeSet();
        UnicodeSet finals = new UnicodeSet();
        UnicodeSet medials = new UnicodeSet();
        while (true) {
            String line = br.readLine();
            if (line == null) break;
            int end;
            
            // find final consonant
            for (int i = line.length() - 1; ; --i) {
                char c = line.charAt(i);
                if (!ignorables.contains(c)) {
                    finals.add(c);
                    end = i;
                    break;
                }
            }
            
            boolean haveFirst = false;
            for (int i = 0; i < end; ++i) {
                char c = line.charAt(i);
                if (ignorables.contains(c)) continue;
                if (!haveFirst) {
                    initials.add(c);
                    haveFirst = true;
                } else {
                    medials.add(c);
                }
            }
        }
        
        initials.removeAll(medials);
        finals.removeAll(medials);
        Utility.showSetNames("initials: ", initials, false, Main.ucd);
        Utility.showSetNames("finals: ", finals, false, Main.ucd);
        Utility.showSetNames("medials: ", medials, false, Main.ucd);
    } finally {
        br.close();
    }
  }
}