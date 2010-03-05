/**
 *******************************************************************************
 * Copyright (C) 2001-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.demo.translit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class TransliterationChart {
    public static void main(String[] args) throws IOException {
        System.out.println("Start");
        UnicodeSet lengthMarks = new UnicodeSet("[\u09D7\u0B56-\u0B57\u0BD7\u0C56\u0CD5-\u0CD6\u0D57\u0C55\u0CD5]");
        int[] indicScripts = {
            UScript.LATIN,
            UScript.DEVANAGARI,
            UScript.BENGALI,
            UScript.GURMUKHI,
            UScript.GUJARATI,
            UScript.ORIYA,
            UScript.TAMIL,
            UScript.TELUGU,
            UScript.KANNADA,
            UScript.MALAYALAM,
        };
        String[] names = new String[indicScripts.length];
        UnicodeSet[] sets = new UnicodeSet[indicScripts.length];
        Transliterator[] fallbacks = new Transliterator[indicScripts.length];
        for (int i = 0; i < indicScripts.length; ++i) {
            names[i] = UScript.getName(indicScripts[i]);
            sets[i] = new UnicodeSet("[[:" + names[i] + ":]&[[:L:][:M:]]&[:age=3.1:]]");
            fallbacks[i] = Transliterator.getInstance("any-" + names[i]);
        }
        EquivClass eq = new EquivClass(new ReverseComparator());
        PrintWriter pw = openPrintWriter("transChart.html");
        pw.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        pw.println("<title>Indic Transliteration Chart</title><style>");
        pw.println("td { text-align: Center; font-size: 200% }");
        pw.println("tt { font-size: 50% }");
        pw.println("td.miss { background-color: #CCCCFF }");
        pw.println("</style></head><body bgcolor='#FFFFFF'>");

        Transliterator anyToLatin = Transliterator.getInstance("any-latin");
        
        String testString = "\u0946\u093E";
        
        UnicodeSet failNorm = new UnicodeSet();
        Set latinFail = new TreeSet();
        
        for (int i = 0; i < indicScripts.length; ++i) {
            if (indicScripts[i] == UScript.LATIN) continue;
            String source = names[i];
            System.out.println(source);
            UnicodeSet sourceChars = sets[i];

            for (int j = 0; j < indicScripts.length; ++j) {
                if (i == j) continue;
                String target = names[j];
                Transliterator forward = Transliterator.getInstance(source + '-' + target);
                Transliterator backward = forward.getInverse();
                UnicodeSetIterator it = new UnicodeSetIterator(sourceChars);
                while (it.next()) {
                    if (lengthMarks.contains(it.codepoint)) continue;
                    String s = Normalizer.normalize(it.codepoint,Normalizer.NFC,0);
                    //if (!Normalizer.isNormalized(s,Normalizer.NFC,0)) continue;
                    if (!s.equals(Normalizer.normalize(s,Normalizer.NFD,0))) {
                        failNorm.add(it.codepoint);
                    } 
                    String t = fix(forward.transliterate(s));
                    if (t.equals(testString)) {
                        System.out.println("debug");
                    }

                    String r = fix(backward.transliterate(t));
                    if (Normalizer.compare(s,r,0) == 0) {
                        if (indicScripts[j] != UScript.LATIN) eq.add(s,t);
                    } else {
                        if (indicScripts[j] == UScript.LATIN) {
                            latinFail.add(s + " - " + t + " - " + r);
                        }
                    }
                }
            }
        }
        // collect equivalents
        pw.println("<table border='1' cellspacing='0'><tr>");
        for (int i = 0; i < indicScripts.length; ++i) {
            pw.print("<th width='10%'>" + names[i].substring(0,3) + "</th>");
        }
        pw.println("</tr>");

        Iterator rit = eq.getSetIterator(new MyComparator());
        while(rit.hasNext()) {
            Set equivs = (Set)rit.next();
            pw.print("<tr>");
            Iterator sit = equivs.iterator();
            String source = (String)sit.next();
            String item = anyToLatin.transliterate(source);
            if (item.equals("") || source.equals(item)) item = "&nbsp;";
            pw.print("<td>" + item + "</td>");
            for (int i = 1; i < indicScripts.length; ++i) {
                sit = equivs.iterator();
                item = "";
                while (sit.hasNext()) {
                    String trial = (String)sit.next();
                    if (!sets[i].containsAll(trial)) continue;
                    item = trial;
                    break;
                }
                String classString = "";
                if (item.equals("")) {
                    classString = " class='miss'";
                    String temp = fallbacks[i].transliterate(source);
                    if (!temp.equals("") && !temp.equals(source)) item = temp;
                } 
                String backup = item.equals("") ? "&nbsp;" : item;
                pw.print("<td" + classString + " title='" + getName(item, "; ") + "'>" 
                    + backup + "<br><tt>" + Utility.hex(item) + "</tt></td>");
            }
            /*
            Iterator sit = equivs.iterator();
            while (sit.hasNext()) {
                String item = (String)sit.next();
                pw.print("<td>" + item + "</td>");
            }
            */
            pw.println("</tr>");
        }
        pw.println("</table>");
        if (true) {
            pw.println("<h2>Failed Normalization</h2>");
    
            UnicodeSetIterator it = new UnicodeSetIterator(failNorm);
            UnicodeSet pieces = new UnicodeSet();
            while (it.next()) {
                String s = UTF16.valueOf(it.codepoint);
                String d = Normalizer.normalize(s,Normalizer.NFD,0);
                pw.println("Norm:" + s + ", " + Utility.hex(s) + " " + UCharacter.getName(it.codepoint)
                     + "; " + d + ", " + Utility.hex(d) + ", ");
                pw.println(UCharacter.getName(d.charAt(1)) + "<br>");
                if (UCharacter.getName(d.charAt(1)).indexOf("LENGTH") >= 0) pieces.add(d.charAt(1));
            }
            pw.println(pieces);
            
            pw.println("<h2>Failed Round-Trip</h2>");
            Iterator cit = latinFail.iterator();
            while (cit.hasNext()) {
                pw.println(cit.next() + "<br>");
            }
        } 

        pw.println("</table></body></html>");
        pw.close();     
        System.out.println("Done");
    }
    
    public static String fix(String s) {
        if (s.equals("\u0946\u093E")) return "\u094A";
        if (s.equals("\u0C46\u0C3E")) return "\u0C4A";
        if (s.equals("\u0CC6\u0CBE")) return "\u0CCA";

        if (s.equals("\u0947\u093E")) return "\u094B";
        if (s.equals("\u0A47\u0A3E")) return "\u0A4B";
        if (s.equals("\u0AC7\u0ABE")) return "\u0ACB";
        if (s.equals("\u0C47\u0C3E")) return "\u0C4B";
        if (s.equals("\u0CC7\u0CBE")) return "\u0CCB";
       
        //return Normalizer.normalize(s,Normalizer.NFD,0);
        return s;
    }
    
    public static PrintWriter openPrintWriter(String fileName) throws IOException {
        File lf = new File(fileName);
        System.out.println("Creating file: " + lf.getAbsoluteFile());
    
        return new PrintWriter(
                new BufferedWriter(
                    new OutputStreamWriter(
                        new FileOutputStream(fileName), "UTF8"), 4*1024));
    }

    
    public static String getName(String s, String separator) {
        int cp;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s,i);
            if (i != 0) sb.append(separator);
            sb.append(UCharacter.getName(cp));
        }
        return sb.toString();
    }
    
    static class MyComparator implements Comparator {
       public int compare(Object o1, Object o2) {
            Iterator i1 = ((TreeSet) o1).iterator();
            Iterator i2 = ((TreeSet) o2).iterator();
            while (i1.hasNext() && i2.hasNext()) {
                String a = (String)i1.next();
                String b = (String)i2.next();
                int result = a.compareTo(b);
                if (result != 0) return result;
            }
            if (i1.hasNext()) return 1;
            if (i2.hasNext()) return -1;
            return 0;
        }
        
    }
    static class ReverseComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            String a = o1.toString();
            char a1 = a.charAt(0);
            String b = o2.toString();
            char b1 = b.charAt(0);
            if (a1 < 0x900 && b1 > 0x900) return -1;
            if (a1 > 0x900 && b1 < 0x900) return +1;
            return a.compareTo(b);
        }       
    }
      
    static class EquivClass {
        EquivClass(Comparator c) {
            comparator = c;
        }
        private HashMap itemToSet = new HashMap();
        private Comparator comparator;
        
        void add(Object a, Object b) {
            Set sa = (Set)itemToSet.get(a);
            Set sb = (Set)itemToSet.get(b);
            if (sa == null && sb == null) { // new set!
                Set s = new TreeSet(comparator);
                s.add(a);
                s.add(b);
                itemToSet.put(a, s);
                itemToSet.put(b, s);
            } else if (sa == null) {
                sb.add(a);
            } else if (sb == null) {
                sa.add(b);
            } else { // merge sets, dumping sb
                sa.addAll(sb);
                Iterator it = sb.iterator();
                while (it.hasNext()) {
                    itemToSet.put(it.next(), sa);
                }
            }
        }
        
        private class MyIterator implements Iterator {
            private Iterator it;
            MyIterator (Comparator comp) {
                TreeSet values = new TreeSet(comp);
                values.addAll(itemToSet.values());
                it = values.iterator();
            }
        
            public boolean hasNext() {
                return it.hasNext();
            }
            public Object next() {
                return it.next();
            }
            public void remove() {
                throw new IllegalArgumentException("can't remove");
            }        
        }

        public Iterator getSetIterator (Comparator comp) {
            return new MyIterator(comp);
        }

    }
}