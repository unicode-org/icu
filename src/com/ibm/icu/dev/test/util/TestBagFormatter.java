//##header J2SE15
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
/*
 *******************************************************************************
 * Copyright (C) 2002-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

// TODO integrate this into the test framework

import java.io.IOException;
import java.io.PrintWriter;
import java.text.Collator;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

// TODO change to use test framework
public class TestBagFormatter {
    
    static final void generatePropertyAliases(boolean showValues) {
        generatePropertyAliases(showValues, ICUPropertyFactory.make());
    }
    
    static final void generatePropertyAliases(boolean showValues, UnicodeProperty.Factory ups) {
        Collator order = Collator.getInstance(Locale.ENGLISH);
        TreeSet props = new TreeSet(order);
        TreeSet values = new TreeSet(order);
        BagFormatter bf = new BagFormatter();
        props.addAll(ups.getAvailableNames());
        for (int i = UnicodeProperty.BINARY; i < UnicodeProperty.LIMIT_TYPE; ++i) {
            System.out.println(UnicodeProperty.getTypeName(i));
            Iterator it = props.iterator();
            while (it.hasNext()) {
                String propAlias = (String)it.next();
                UnicodeProperty up = ups.getProperty(propAlias);
                int type = up.getType();
                if (type != i) continue;                
                System.out.println();
                System.out.println(propAlias + "\t" + bf.join(up.getNameAliases()));
                if (!showValues) continue;
                values.clear();
                if (type == UnicodeProperty.NUMERIC || type == UnicodeProperty.EXTENDED_NUMERIC) {
                    UnicodeMap um = new UnicodeMap();
                    um.putAll(up);
                    System.out.println(um.toString(new NumberComparator()));
                    continue;
                }
                values.clear();
                values.addAll(up.getAvailableValues());
                Iterator it2 = values.iterator();
                while (it2.hasNext()) {
                    String valueAlias = (String)it2.next();
                    System.out.println("\t" + bf.join(valueAlias + "\t" + up.getValueAliases(valueAlias)));
                }
            }
        }
    }
    
    static class NumberComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            if (o1 == o2) return 0;
            if (o1 == null) return 1;
            if (o2 == null) return -1;
            double n1 = Double.parseDouble((String)o1);
            double n2 = Double.parseDouble((String)o2);
            return n1 < n2 ? -1 : n1 > n2 ? 1 : 0;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Start");
        try {
            //readCharacters();
            UnicodeProperty prop = ICUPropertyFactory.make().getProperty("Canonicalcombiningclass");
            prop.getAvailableValues();
            
            generatePropertyAliases(true);
            
            BagFormatter bf = new BagFormatter();

            UnicodeSet us = new UnicodeSet("[:gc=nd:]");  
            BagFormatter.CONSOLE.println("[:gc=nd:]");
            bf.showSetNames(BagFormatter.CONSOLE,us);

            us = new UnicodeSet("[:numeric_value=2:]");  
            BagFormatter.CONSOLE.println("[:numeric_value=2:]");
            bf.showSetNames(BagFormatter.CONSOLE,us);
            
            us = new UnicodeSet("[:numeric_type=numeric:]");   
            BagFormatter.CONSOLE.println("[:numeric_type=numeric:]");
            bf.showSetNames(BagFormatter.CONSOLE,us);
            
            UnicodeProperty.Factory ups = ICUPropertyFactory.make();
            us = ups.getSet("gc=mn", null, null); 
            BagFormatter.CONSOLE.println("gc=mn");
            bf.showSetNames(BagFormatter.CONSOLE, us);
            
            if (true) return;
            //showNames("Name", ".*MARK.*");
            //showNames("NFD", "a.+");
            //showNames("NFD", false);
            //showNames("Lowercase_Mapping", false);
            //TestUnicodePropertySource.test(true);
            //showNames(".*\\ \\-.*");


            //checkHTML();
            //testIsRTL();
           
            //TestTokenizer.test();
            //RandomCollator.generate("collationTest.txt", null);
            
            //TestPick.test();
            //printRandoms();
            //if (true) return;
            //testLocales();
            //if (true) return;
            /*
            TestCollator tc = new TestCollator();
            tc.test(RuleBasedCollator.getInstance(),1000);
            */
            /*
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < 100; ++i) {
                sb.setLength(0);
                rc.nextRule(sb);
                System.out.println(sb);
            }
            */
        } finally {
            System.out.println("End");
       }

    }
    
    static void testLocales() throws IOException {
        Locale[] locales = Collator.getAvailableLocales();
        Set s = new TreeSet(Collator.getInstance());
        for (int i = 0; i < locales.length; ++i) {
            String lang = locales[i].getLanguage();
            String dlang = locales[i].getDisplayLanguage();
            String country = locales[i].getCountry();
            String dcountry = locales[i].getDisplayCountry();
            if (country.equals("")) continue;
            s.add(""
                + "\t" + dcountry 
                + "\t" + country 
                + "\t" + dlang
                + "\t" + lang 
            );
        }
        //CollectionFormatter cf = new CollectionFormatter();
        PrintWriter pw = BagFormatter.openUTF8Writer("", "countries.txt");
        Iterator it = s.iterator();
        while (it.hasNext()) {
            pw.println(it.next());
        }
        pw.close();
    }
    
    
    /*
     * Use the number of significant digits to round get a rounding value.
     */
/*    static final double LOG10 = Math.log(10);
    public static void useSignificantDigits(double value, int digits) {
        double log10 = Math.log(value)/LOG10; // log[e]
        
    }*/
    
    static final UnicodeSet RTL = new UnicodeSet("[[:L:]&[[:bidi class=R:][:bidi class=AL:]]]");
    
    static boolean isRTL(Locale loc) {        
        // in 2.8 we can use the exemplar characters, but for 2.6 we have to work around it
        int[] scripts = UScript.getCode(loc);
        return new UnicodeSet()
            .applyIntPropertyValue(UProperty.SCRIPT, scripts == null ? UScript.LATIN : scripts[0])
            .retainAll(RTL).size() != 0;
    }
    
    static void testIsRTL() {
        Locale[] locales = Locale.getAvailableLocales();
        Set s = new TreeSet();
        for (int i = 0; i < locales.length; ++i) {
            s.add((isRTL(locales[i]) ? "R " : "L ") + locales[i].getDisplayName());
        }
        Iterator it = s.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }
    }

    static final Transliterator toHTML = Transliterator.createFromRules(
        "any-html",        
            "'<' > '&lt;' ;" +
            "'&' > '&amp;' ;" +
            "'>' > '&gt;' ;" +
            "'\"' > '&quot;' ; ",
        Transliterator.FORWARD);
    static final Transliterator fromHTML = Transliterator.createFromRules(
        "html-any",        
            "'<' < '&'[lL][Tt]';' ;" +
            "'&' < '&'[aA][mM][pP]';' ;" +
            "'>' < '&'[gG][tT]';' ;" +
            "'\"' < '&'[qQ][uU][oO][tT]';' ; ",
        Transliterator.REVERSE);
        
    static void checkHTML() {
        String foo = "& n < b < \"ab\"";
        String fii = toHTML.transliterate(foo);
        System.out.println("in: " + foo);
        System.out.println("out: " + fii);
        System.out.println("in*: " + fromHTML.transliterate(fii));
        System.out.println("IN*: " + fromHTML.transliterate(fii.toUpperCase()));
    }
    /*
    static void showNames(String propAlias, boolean matches) {
        BagFormatter bf = new BagFormatter();
        UnicodeSet stuff;
        stuff = new UnicodePropertySource.ICU()
            .setPropertyAlias(propAlias)
            .getPropertySet(matches, null);
        System.out.println(bf.showSetNames(propAlias + " with " + matches, stuff));
    }
    
    static void showNames(String propAlias, String pattern) {
        BagFormatter bf = new BagFormatter();
        UnicodeSet stuff;
        stuff = new UnicodePropertySource.ICU()
            .setPropertyAlias(propAlias)
            .getPropertySet(Pattern.compile(pattern).matcher(""), null);
        System.out.println(bf.showSetNames(propAlias + "with " + pattern, stuff));
    }
    */
}
//#endif
