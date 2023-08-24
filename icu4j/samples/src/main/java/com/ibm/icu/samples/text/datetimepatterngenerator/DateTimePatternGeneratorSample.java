// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.samples.text.datetimepatterngenerator;
// ---getBestPatternExample
import java.util.Date;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
// ---getBestPatternExample
/**
 * com.ibm.icu.text.DateTimePatternGenerator Sample Code
 */
public class DateTimePatternGeneratorSample {
    
    public static void main (String[] args) {
        getBestPatternExample();
        addPatternExample();
        replaceFieldTypesExample();
    }
    
    public static void   getBestPatternExample() {
        System.out.println("========================================================================");
        System.out.println(" getBestPatternExample()");
        System.out.println();
        System.out.println(" Use DateTimePatternGenerator to create customized date/time pattern:");
        System.out.println(" yQQQQ,yMMMM, MMMMd, hhmm, jjmm per locale");
        System.out.println("========================================================================");
     // ---getBestPatternExample
        final String[] skeletons = {
                "yQQQQ", // year + full name of quarter, i.e., 4th quarter 1999
                "yMMMM", // year + full name of month, i.e., October 1999
                "MMMMd", // full name of month + day of the month, i.e., October 25
                "hhmm",  // 12-hour-cycle format, i.e., 1:32 PM
                "jjmm"   // preferred hour format for the given locale, i.e., 24-hour-cycle format for fr_FR
                };
        final ULocale[] locales = {
                new ULocale ("en_US"),
                new ULocale ("fr_FR"),
                new ULocale ("zh_CN"),
                };
        DateTimePatternGenerator dtfg = null;
        Date date= new GregorianCalendar(1999,9,13,23,58,59).getTime();
        System.out.printf("%-20s%-35s%-35s%-35s\n\n", "Skeleton", "en_US", "fr_FR","zh_CN");
        for (String skeleton:skeletons) {
             System.out.printf("%-20s", skeleton);
            for (ULocale locale:locales) {
                // create a DateTimePatternGenerator instance for given locale
                dtfg = DateTimePatternGenerator.getInstance(locale);
                // use getBestPattern method to get the best pattern for the given skeleton
                String pattern = dtfg.getBestPattern(skeleton);
                // Constructs a SimpleDateFormat with the best pattern generated above and the given locale
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, locale);
                // Get the format of the given date
                System.out.printf("%-35s",sdf.format(date));
            }
            System.out.println("\n");
        }
        /** output of the sample code:
         *************************************************************************************************************
           Skeleton            en_US                              fr_FR                              zh_CN

           yQQQQ               4th quarter 1999                   4e trimestre 1999                  1999年第四季度

           yMMMM               October 1999                       octobre 1999                       1999年10月 

           MMMMd               October 13                         13 octobre                         10月13日 
           
           hhmm                11:58 PM                           11:58 PM                           下午11:58

           jjmm                11:58 PM                           23:58                              下午11:58

        **************************************************************************************************************/
        // Use DateTime.getPatternInstance to produce the same Date/Time format with predefined constant field value
        final String[] dtfskeleton = {
                DateFormat.YEAR_QUARTER, // year + full name of quarter, i.e., 4th quarter 1999
                DateFormat.YEAR_MONTH,   // year + full name of month, i.e., October 1999
                DateFormat.MONTH_DAY     // full name of month + day of the month, i.e., October 25
                };
        System.out.printf("%-20s%-35s%-35s%-35s\n\n", "Skeleton", "en_US", "fr_FR","zh_CN");
        for (String skeleton:dtfskeleton) {
            System.out.printf("%-20s", skeleton);
            for (ULocale locale:locales) {
                // Use DateFormat.getPatternInstance to get the date/time format for the locale,
                // and apply the format to the given date
                String df=DateFormat.getPatternInstance(skeleton,locale).format(date);
                System.out.printf("%-35s",df);
            }
            System.out.println("\n");
        }

        /** output of the sample code:
         ************************************************************************************************************
         Skeleton            en_US                              fr_FR                              zh_CN

         yQQQQ               4th quarter 1999                   4e trimestre 1999                  1999年第四季度

         yMMMM               October 1999                       octobre 1999                       1999年10月

         MMMMd               October 13                         13 octobre                         10月13日 
         ************************************************************************************************************/
// ---getBestPatternExample
}

    public static void addPatternExample() {
        System.out.println("========================================================================");
        System.out.println(" addPatternExample()");
        System.out.println();
        System.out.println(" Use addPattern API to add new '. von' to existing pattern");
        System.out.println("========================================================================");
    // ---addPatternExample
        Date date= new GregorianCalendar(1999,9,13,23,58,59).getTime();
        ULocale locale = ULocale.FRANCE;
        // Create an DateTimePatternGenerator instance for the given locale
        DateTimePatternGenerator gen = DateTimePatternGenerator.getInstance(locale);
        SimpleDateFormat format = new SimpleDateFormat(gen.getBestPattern("MMMMddHmm"), locale);
        DateTimePatternGenerator.PatternInfo returnInfo = new DateTimePatternGenerator.PatternInfo();
        // Add '. von' to the existing pattern
        gen.addPattern("dd'. von' MMMM", true, returnInfo);
        // Apply the new pattern
        format.applyPattern(gen.getBestPattern("MMMMddHmm"));
        System.out.println("New Pattern for FRENCH: "+format.toPattern());
        System.out.println("Date Time in new Pattern: "+format.format(date));
      
        /** output of the sample code:
        **************************************************************************************************
         New Pattern for FRENCH: dd. 'von' MMMM HH:mm
         Date Time in new Pattern: 13. von octobre 23:58
     
        *************************************************************************************************/
    // ---addPatternExample
}

    public static void replaceFieldTypesExample() {
        // Use repalceFieldTypes API to replace zone 'zzzz' with 'vvvv'
        System.out.println("========================================================================");
        System.out.println(" replaceFieldTypeExample()");
        System.out.println();
        System.out.println(" Use replaceFieldTypes API to replace zone 'zzzz' with 'vvvv");
        System.out.println("========================================================================");
    // ---replaceFieldTypesExample
        Date date= new GregorianCalendar(1999,9,13,23,58,59).getTime();
        TimeZone zone = TimeZone.getTimeZone("Europe/Paris");
        ULocale locale = ULocale.FRANCE;
        DateTimePatternGenerator gen = DateTimePatternGenerator.getInstance(locale);
        SimpleDateFormat format = new SimpleDateFormat("EEEE d MMMM y HH:mm:ss zzzz",locale);
        format.setTimeZone(zone);
        String pattern = format.toPattern();
        System.out.println("Pattern before replacement:");
        System.out.println(pattern);
        System.out.println("Date/Time format in fr_FR:");
        System.out.println(format.format(date));
        // Replace zone "zzzz" in the pattern with "vvvv"
        String newPattern = gen.replaceFieldTypes(pattern, "vvvv");
        // Apply the new pattern
        format.applyPattern(newPattern);
        System.out.println("Pattern after replacement:");
        System.out.println(newPattern);
        System.out.println("Date/Time format in fr_FR:");
        System.out.println(format.format(date));

        /** output of the sample code:
        ***************************************************************************************************
         Pattern before replacement:
         EEEE d MMMM y HH:mm:ss zzzz
         Date/Time format in fr_FR:
         jeudi 14 octobre 1999 05:58:59 heure avancée d’Europe centrale
         Pattern after replacement:
         EEEE d MMMM y HH:mm:ss vvvv
         Date/Time format in fr_FR:
         jeudi 14 octobre 1999 05:58:59 heure de l’Europe centrale

        **************************************************************************************************/
 // ---replaceFieldTypesExample
    }
}
