/*
 *******************************************************************************
 * Copyright (C) 2002-2003, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/util/TestBagFormatter.java,v $
 * $Date: 2004/02/12 00:47:30 $
 * $Revision: 1.8 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.util;

// TODO integrate this into the test framework

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;
import java.util.Iterator;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.Collator;
import java.util.Locale;
import java.util.Set;

import com.ibm.icu.lang.UScript;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UnicodeSet;

public class TestBagFormatter {
    
    static final void generatePropertyAliases(boolean showValues) {
        Collator order = Collator.getInstance(Locale.ENGLISH);
        UnicodeProperty.Factory ups = ICUPropertyFactory.make();
        TreeSet props = new TreeSet(order);
        TreeSet values = new TreeSet(order);
        Collection aliases = new ArrayList();
        BagFormatter bf = new BagFormatter();
        ups.getAvailableAliases(props);
        Iterator it = props.iterator();
        while (it.hasNext()) {
            String propAlias = (String)it.next();
            UnicodeProperty up = ups.getProperty(propAlias);
            System.out.println();
            aliases.clear();
            System.out.println(bf.join(up.getAliases(aliases)));
            if (!showValues) continue;
            values.clear();
            up.getAvailableValueAliases(values);
            Iterator it2 = values.iterator();
            while (it2.hasNext()) {
                String valueAlias = (String)it2.next();
                aliases.clear();
                System.out.println("\t" + bf.join(up.getValueAliases(valueAlias, aliases)));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Start");
        try {
            //readCharacters();
            
            generatePropertyAliases(true);
            
            BagFormatter bf = new BagFormatter();

            UnicodeSet us = new UnicodeSet("[:numeric_value=2:]");  
            BagFormatter.CONSOLE.println("[:numeric_value=2:]");
            bf.showSetNames(BagFormatter.CONSOLE,us);
            us = new UnicodeSet("[:numeric_type=numeric:]");   
            BagFormatter.CONSOLE.println("[:numeric_type=numeric:]");
            bf.showSetNames(BagFormatter.CONSOLE,us);
            
            UnicodeProperty.Factory ups = ICUPropertyFactory.make();
            us = ups.getSet("gc=mn", null, null); 
            BagFormatter.CONSOLE.println("gc=mn");
            bf.showSetNames(bf.CONSOLE, us);
            
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
    static final double LOG10 = Math.log(10);
    public static void useSignificantDigits(double value, int digits) {
        double log10 = Math.log(value)/LOG10; // log[e]
        
    }
    
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