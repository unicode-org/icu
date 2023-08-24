// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2013-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.samples.text.pluralformat;
// ---PluralFormatExample
import java.text.FieldPosition;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.PluralFormat;
import com.ibm.icu.util.ULocale;
// ---PluralFormatExample

public class PluralFormatSample {

  public static void main(String[] args) {
      PluralFormatExample();
      }

  private static void PluralFormatExample(){

      System.out.println("=======================================================================================");
      System.out.println(" PluralFormatExample()");
      System.out.println();
      System.out.println(" Use PluralFormat and Messageformat to get appropriate Plural Form for languages below:");
      System.out.println(" English, Slovenian");
      System.out.println("=======================================================================================");
      // ---PluralFormatExample
      ULocale locEn = new ULocale("en");
      ULocale locSl = new ULocale("sl");

      String patEn = "one{dog} other{dogs}";                      // English 'dog'
      String patSl = "one{pes} two{psa} few{psi} other{psov}";    // Slovenian translation of dog in Plural Form

      // Create a new PluralFormat for a given locale locale and pattern string
      PluralFormat plfmtEn = new PluralFormat(locEn, patEn);
      PluralFormat plfmtSl = new PluralFormat(locSl, patSl);
      // Constructs a MessageFormat for the specified locale and pattern.
      MessageFormat msgfmtEn = new MessageFormat("{0,number} {1}", locEn);
      MessageFormat msgfmtSl = new MessageFormat("{0,number} {1}", locSl);

      final int[] numbers = {0, 1, 2, 3, 4, 5, 10, 100, 101, 102};
      System.out.println("Output by using PluralFormat and MessageFormat API\n");
      System.out.printf("%-16s%-16s%-16s\n", "Number", "English", "Slovenian");
 
      // Use MessageFormat.format () to format the objects and appends to the given StringBuffer
      for (int num : numbers) {
          StringBuffer msgEn = new StringBuffer();
          StringBuffer msgSl = new StringBuffer();

          msgfmtEn.format(new Object[] {num, plfmtEn.format(num)}, msgEn, new FieldPosition(0));
          msgfmtSl.format(new Object[] {num, plfmtSl.format(num)}, msgSl, new FieldPosition(0));

          System.out.printf("%-16s%-16s%-16s\n", num, msgEn, msgSl);
      }

      System.out.println();

      // Equivalent code with message format pattern
      String msgPatEn = "{0,plural, one{# dog} other{# dogs}}";
      String msgPatSl = "{0,plural, one{# pes} two{# psa} few{# psi} other{# psov}}";
 
      MessageFormat altMsgfmtEn = new MessageFormat(msgPatEn, locEn);
      MessageFormat altMsgfmtSl = new MessageFormat(msgPatSl, locSl);
      System.out.println("Same Output by using MessageFormat API only\n");
      System.out.printf("%-16s%-16s%-16s\n", "Number", "English", "Slovenian");
      for (int num : numbers) {
          StringBuffer msgEn = new StringBuffer();
          StringBuffer msgSl = new StringBuffer();

          altMsgfmtEn.format(new Object[] {num}, msgEn, new FieldPosition(0));
          altMsgfmtSl.format(new Object[] {num}, msgSl, new FieldPosition(0));

          System.out.printf("%-16s%-16s%-16s\n", num, msgEn, msgSl);
      }
      /** output of the sample code:
       ********************************************************************
        Number          English         Slovenian
        0               0 dogs          0 psov
        1               1 dog           1 pes
        2               2 dogs          2 psa
        3               3 dogs          3 psi
        4               4 dogs          4 psi
        5               5 dogs          5 psov
        10              10 dogs         10 psov
        100             100 dogs        100 psov
        101             101 dogs        101 pes
        102             102 dogs        102 psa

       *******************************************************************/
      // ---PluralFormatExample
  }
}
