/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateThaiBreaks.java,v $
* $Date: 2002/08/04 21:38:45 $
* $Revision: 1.3 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import java.io.*;
import com.ibm.icu.text.UTF16;
import com.ibm.text.utility.*;
import com.ibm.icu.text.UnicodeSet;
import java.util.*;

public class GenerateThaiBreaks {
  public static void main(String [] args) throws IOException {
    
    BufferedReader br = new BufferedReader(
      new InputStreamReader(
        new FileInputStream("c:\\icu4j\\src\\com\\ibm\\icu\\dev\\data\\thai6.ucs"), "UnicodeLittle"));
    PrintWriter out = null;
    
    try {
        Default.setUCD();
        UnicodeSet ignorables = new UnicodeSet(); 
        /* new UnicodeSet(0xE30, 0xE3A);
        ignorables.add(0x0E40, 0x0E44); // add logical order exception
        ignorables.add(0x0E47, 0x0E4E);
        */
        ignorables.add(0, ' '); // add controls
        ignorables.add('.');
        
        
        UnicodeSet initials = new UnicodeSet();
        UnicodeSet finals = new UnicodeSet();
        UnicodeSet medials = new UnicodeSet();
        
        char[] buffer = new char[100];
        
        while (true) {
            String line = br.readLine();
            if (line == null) break;
            int end = 0;
            
            // find 'real' characters
            for (int i = 0; i < line.length(); ++i) {
                char c = line.charAt(i);
                if (ignorables.contains(c)) continue;
                buffer[end++] = c;
            }
            String temp = new String(buffer, 0, end);
            
            if (temp.length() <= 1) {
                initials.add(temp);
                finals.add(temp);
                continue;
            }
            
            initials.add(temp.substring(0,1));
            //initials.add(temp.substring(0,2));
            finals.add(temp.substring(temp.length()-1));
            //finals.add(temp.substring(temp.length()-1));
            
            for (int i = 1; i < temp.length() - 1; ++i) {
                //medials.add(temp.substring(i, i+2));
                medials.add(temp.substring(i, i+1));
            }
            //medials.add(temp.substring(temp.length() - 2, temp.length() - 1));
        }
        
        System.out.println("initials size: " + initials.size());
        System.out.println("finals size: " + finals.size());
        System.out.println("medials size: " + medials.size());
        
        //out = Utility.openPrintWriter("ThaiData.txt", Utility.UTF8_WINDOWS);
       // out.write('\uFEFF');
       
        UnicodeSet marks = new UnicodeSet("[[\u0e00-\u0e7f]&[[:mn:][:me:]]]");
        finals.addAll(marks);
        
        UnicodeSet all = new UnicodeSet(initials).addAll(medials).addAll(finals);
        
        UnicodeSet missingThai = new UnicodeSet("[[\u0e00-\u0e7f]-[:Cn:]]").removeAll(all);
        
        System.out.println("Never occur: " + missingThai.toPattern(true));
        Utility.showSetNames("", missingThai, true, Default.ucd);
        System.out.println();
        
        UnicodeSet neverInitial = new UnicodeSet(all).removeAll(initials);
        UnicodeSet neverFinal = new UnicodeSet(all).removeAll(finals);
        
        System.out.println("Never initial: " + neverInitial.toPattern(true));
        Utility.showSetNames("", neverInitial, true, Default.ucd);
        System.out.println();
        
        System.out.println("Never final: " + neverFinal.toPattern(true));
        Utility.showSetNames("", neverFinal, true, Default.ucd);
        System.out.println();
        
        initials.removeAll(medials);
        finals.removeAll(medials);

        System.out.println("initials size: " + initials.size());
        System.out.println("finals size: " + finals.size());
        
        System.out.println("Only Initials" + initials.toPattern(true));
        Utility.showSetNames("", initials, true, Default.ucd);
        System.out.println();
        
        System.out.println("Only Finals" + finals.toPattern(true));
        Utility.showSetNames("", finals, true, Default.ucd);
    } finally {
        br.close();
        if (out != null) out.close();
    }
  }
  
  static class MyBreaker implements Utility.Breaker {
        public String get(Object current, Object old) {
          if (old == null || UTF16.charAt(current.toString(), 0) == UTF16.charAt(old.toString(), 0)) {
            return current.toString() + "(" + Default.ucd.getCode(current.toString().substring(1)) + "))";
          } else {
            return "\r\n" + current + "(" + Default.ucd.getCode(current.toString()) + "))";
          }
        }
        public boolean filter(Object current) { return true; }
  }
}