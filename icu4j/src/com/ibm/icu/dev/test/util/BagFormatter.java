/*
 *******************************************************************************
 * Copyright (C) 2002, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/util/BagFormatter.java,v $
 * $Date: 2003/12/17 04:55:27 $
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.util;

import com.ibm.icu.text.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.impl.*;

import java.io.*;
import java.util.*;
//import java.util.regex.*;
import java.text.MessageFormat;

public class BagFormatter {
    
    boolean abbreviated = false;
    
    /**
     * Compare two UnicodeSets, and show the differences
     * @param name1 name of first set to be compared
     * @param set1 first set
     * @param name2 name of second set to be compared
     * @param set2 second set
     * @return formatted string
     */
    public String showSetDifferences(
        String name1,
        UnicodeSet set1,
        String name2,
        UnicodeSet set2) {
            
        StringWriter sw = new StringWriter();
        showSetDifferences(new PrintWriter(sw), name1, set1, name2, set2);
        sw.flush();
        return sw.getBuffer().toString();
    }
    
    public String showSetDifferences(
        String name1,
        Collection set1,
        String name2,
        Collection set2) {
            
        StringWriter sw = new StringWriter();
        showSetDifferences(new PrintWriter(sw), name1, set1, name2, set2);
        sw.flush();
        return sw.getBuffer().toString();
    }

    /**
     * Compare two UnicodeSets, and show the differences
     * @param name1 name of first set to be compared
     * @param set1 first set
     * @param name2 name of second set to be compared
     * @param set2 second set
     * @return formatted string
     */
    public void showSetDifferences(
        PrintWriter pw,
        String name1,
        UnicodeSet set1,
        String name2,
        UnicodeSet set2) {
            
        String[] names = { name1, name2 };

        UnicodeSet temp = new UnicodeSet(set1).removeAll(set2);
        pw.println();
        showSetNames(pw, inOut.format(names), temp);

        temp = new UnicodeSet(set2).removeAll(set1);
        pw.println();
        showSetNames(pw, outIn.format(names), temp);

        temp = new UnicodeSet(set2).retainAll(set1);
        pw.println();
        showSetNames(pw, inIn.format(names), temp);
    }
    
    public void showSetDifferences(
        PrintWriter pw,
        String name1,
        Collection set1,
        String name2,
        Collection set2) {
            
        String[] names = { name1, name2 };
        // damn'd collection doesn't have a clone, so
        // we go with Set, even though that
        // may not preserve order and duplicates
        Collection temp = new HashSet(set1);
        temp.removeAll(set2);
        pw.println();
        showSetNames(pw, inOut.format(names), temp);

        temp.clear();
        temp.addAll(set2);
        temp.removeAll(set1);
        pw.println();
        showSetNames(pw, outIn.format(names), temp);

        temp.clear();
        temp.addAll(set1);
        temp.retainAll(set2);
        pw.println();
        showSetNames(pw, inIn.format(names), temp);
    }

    public String showSetNames(String title, Object set1) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        showSetNames(pw, title, set1);
        pw.flush();
        String result = sw.getBuffer().toString();
        pw.close();
        return result;
    }

    /**
     * Returns a list of items in the collection, with each separated by the separator.
     * Each item must not be null; its toString() is called for a printable representation
     * @param c source collection
     * @param separator to be placed between any strings
     * @return
     * @internal
     */
    public void showSetNames(PrintWriter output, String title, Object c) {
        output.println(title);
        mainVisitor.output = output;
        mainVisitor.doAt(c);
    }

    /**
     * Returns a list of items in the collection, with each separated by the separator.
     * Each item must not be null; its toString() is called for a printable representation
     * @param c source collection
     * @param separator to be placed between any strings
     * @return
     * @internal
     */
    public void showSetNames(String filename, String title, Object c) throws IOException {
      /*  PrintWriter pw = */new PrintWriter(
            new OutputStreamWriter(
                new FileOutputStream(filename),"utf-8"));
    }
    
    public String getAbbreviatedName(
        String source,
        String pattern,
        String substitute) {
            
        int matchEnd = NameIterator.findMatchingEnd(source, pattern);
        int sdiv = source.length() - matchEnd;
        int pdiv = pattern.length() - matchEnd;
        StringBuffer result = new StringBuffer();
        addMatching(
            source.substring(0, sdiv),
            pattern.substring(0, pdiv),
            substitute,
            result);
        addMatching(
            source.substring(sdiv),
            pattern.substring(pdiv),
            substitute,
            result);
        return result.toString();
    }

    abstract public static class Relation {
        abstract public String getRelation(String a, String b);
    }
    
    static class NullRelation extends Relation {
        public String getRelation(String a, String b) { return ""; }
    }
    
    private Relation r = new NullRelation();
   
    public BagFormatter setRelation(Relation r) {
        this.r = r;
        return this; // for chaining
    }
    
    public Relation getRelation() {
        return r;
    }
            
    /*
     r.getRelati on(last, s) + quote(s) + "\t#" + UnicodeSetFormatter.getResolvedName(s)
    */
    /*
    static final UnicodeSet NO_NAME =
        new UnicodeSet("[\\u0080\\u0081\\u0084\\u0099\\p{Cn}\\p{Co}]");
    static final UnicodeSet HAS_NAME = new UnicodeSet(NO_NAME).complement();
    static final UnicodeSet NAME_CHARACTERS =
        new UnicodeSet("[A-Za-z0-9\\<\\>\\-\\ ]");

    public UnicodeSet getSetForName(String namePattern) {
        UnicodeSet result = new UnicodeSet();
        Matcher m = Pattern.compile(namePattern).matcher("");
        // check for no-name items, and add in bulk
        m.reset("<no name>");
        if (m.matches()) {
            result.addAll(NO_NAME);
        }
        // check all others
        UnicodeSetIterator usi = new UnicodeSetIterator(HAS_NAME);
        while (usi.next()) {
            String name = getName(usi.codepoint);
            if (name == null)
                continue;
            m.reset(name);
            if (m.matches()) {
                result.add(usi.codepoint);
            }
        }
        // Note: if Regex had some API so that if we could tell that
        // an initial substring couldn't match, e.g. "CJK IDEOGRAPH-"
        // then we could optimize by skipping whole swathes of characters
        return result;
    }
    */
    
    public void setMergeRanges(boolean in) {
        mergeRanges = in;
    }
    public void setShowSetAlso(boolean b) {
        showSetAlso = b;
    }
    public String getName(int codePoint) {
        String hcp = "U+" + Utility.hex(codePoint, 4) + " ";
        String result = nameProp.getPropertyValue(codePoint);
        if (result != null)
            return hcp + result;
        String prop = catProp.getPropertyValue(codePoint);
        if (prop.equals("Control")) {
            result = nameProp.getPropertyValue(codePoint);
            if (result != null)
                return hcp + "<" + result + ">";
        }
        return hcp + "<reserved>";
    }
    
    UnicodePropertySource source;
    UnicodePropertySource labelSource;

    UnicodePropertySource nameProp;
    UnicodePropertySource name1Prop;
    UnicodePropertySource catProp;
    UnicodePropertySource shortCatProp;

    public void setUnicodePropertySource(UnicodePropertySource source) {
        this.source = source;
        nameProp = ((UnicodePropertySource)source.clone())
            .setPropertyAlias("Name");

        name1Prop = ((UnicodePropertySource)source.clone())
            .setPropertyAlias("Unicode_1_Name");

        catProp = ((UnicodePropertySource)source.clone())
            .setPropertyAlias("General_Category");

        shortCatProp = ((UnicodePropertySource)source.clone())
            .setPropertyAlias("General_Category")
            .setNameChoice(UProperty.NameChoice.SHORT);
    }
    
    {
        setUnicodePropertySource(new UnicodePropertySource.ICU());
        Map labelMap = new HashMap();
        labelMap.put("Lo","L&");
        labelMap.put("Lu","L&");
        labelMap.put("Lt","L&");
        setLabelSource(new UnicodePropertySource.ICU()
            .setPropertyAlias("General_Category")
            .setNameChoice(UProperty.NameChoice.SHORT)
            .setFilter(
                new UnicodePropertySource.MapFilter().setMap(labelMap)));
    }

    // ===== PRIVATES =====
    
    private Visitor.Join labelVisitor = new Visitor.Join();
    
    private boolean mergeRanges = true;
    private boolean literalCharacter = false;
    private boolean showSetAlso = false;

    private RangeFinder rf = new RangeFinder();

    private MessageFormat inOut = new MessageFormat("In {0}, but not in {1}:");
    private MessageFormat outIn = new MessageFormat("Not in {0}, but in {1}:");
    private MessageFormat inIn = new MessageFormat("In both {0}, and in {1}:");

    private MyVisitor mainVisitor = new MyVisitor();

    /*
    private String getLabels(int start, int end) {
        Set names = new TreeSet();
        for (int cp = start; cp <= end; ++cp) {
            names.add(getLabel(cp));
        }
        return labelVisitor.join(names);
    }
    */

    private void addMatching(
        String source,
        String pattern,
        String substitute,
        StringBuffer result) {
        NameIterator n1 = new NameIterator(source);
        NameIterator n2 = new NameIterator(pattern);
        boolean first = true;
        while (true) {
            String s1 = n1.next();
            if (s1 == null)
                break;
            String s2 = n2.next();
            if (!first)
                result.append(" ");
            first = false;
            if (s1.equals(s2))
                result.append(substitute);
            else
                result.append(s1);
        }
    }

    private Tabber singleTabber =
        new Tabber.MonoTabber(
            new int[] {
                0,
                Tabber.LEFT,
                6,
                Tabber.LEFT,
                10,
                Tabber.LEFT,
                14,
                Tabber.LEFT });
    private Tabber rangeTabber =
        new Tabber.MonoTabber(
            new int[] {
                0,
                Tabber.LEFT,
                14,
                Tabber.LEFT,
                18,
                Tabber.LEFT,
                27,
                Tabber.LEFT,
                34,
                Tabber.LEFT });

    private static NumberFormat nf =
        NumberFormat.getIntegerInstance(Locale.ENGLISH);

    private class MyVisitor extends Visitor {
        PrintWriter output;

        public String format(Object o) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            doAt(o);
            pw.flush();
            String result = sw.getBuffer().toString();
            pw.close();
            return result;
        }

        protected void doBefore(Object container, Object o) {
            if (showSetAlso && container instanceof UnicodeSet) {
                output.println("# " + container);
            }
        }

        protected void doBetween(Object container, Object lastItem, Object nextItem) {
        }

        protected void doAfter(Object container, Object o) {
            output.println("# Total: " + nf.format(count(container)));
        }

        protected void doSimpleAt(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry oo = (Map.Entry)o;
                Object key = oo.getKey();
                Object value = oo.getValue();
                doBefore(o, key);
                doAt(key);
                output.print("->");
                doAt(value);
                doAfter(o, value);
            } else if (o instanceof Visitor.CodePointRange) {
                doAt((Visitor.CodePointRange) o);
            } else {
                output.print(o.toString());
            }
        }

        protected void doAt(Visitor.CodePointRange usi) {
            if (!mergeRanges) {
                for (int cp = usi.codepoint; cp <= usi.codepointEnd; ++cp) {
                    String label = labelSource.getPropertyValue(cp);
                    if (label.length() != 0)
                        label += " ";
                    output.println(
                        singleTabber.process(
                            Utility.hex(cp, 4)
                                + " \t# "
                                + label
                                + (literalCharacter
                                    && (cp >= 0x20)
                                        ? " \t(" + UTF16.valueOf(cp) + ") "
                                        : "")
                                + " \t"
                                + getName(cp)));
                }
            } else {
                rf.reset(usi.codepoint, usi.codepointEnd + 1);
                String label;
                while ((label = rf.next()) != null) {
                    /*
                    String label = (usi.codepoint != usi.codepointEnd) 
                        ? label = getLabels(usi.codepoint, usi.codepointEnd) 
                        : getLabel(usi.codepoint);
                    */
                    int start = rf.start;
                    int end = rf.limit - 1;
                    if (label.length() != 0)
                        label += " ";
                    output.println(
                        rangeTabber.process(
                            Utility.hex(start, 4)
                                + ((start != end)
                                    ? (".." + Utility.hex(end, 4))
                                    : "")
                                + " \t# "
                                + label
                                + " \t["
                                + nf.format(end - start + 1)
                                + "]"
                                + (literalCharacter
                                    && (start >= 0x20)
                                        ? " \t("
                                            + UTF16.valueOf(start)
                                            + ((start != end)
                                                ? (".." + UTF16.valueOf(end))
                                                : "")
                                            + ") "
                                        : "")
                                + " \t"
                                + getName(start)
                                + ((start != end)
                                    ? (".."
                                        + (abbreviated
                                           ? getAbbreviatedName(
                                                getName(end),
                                                getName(start),
                                                "~")
                                           : getName(end)))
                                    : "")));
                }
            }
        }
    }

    /**
     * Iterate through a string, breaking at words.
     * @author Davis
     */
    private static class NameIterator {
        String source;
        int position;
        int start;
        int limit;

        NameIterator(String source) {
            this.source = source;
            this.start = 0;
            this.limit = source.length();
        }
        /** 
         * Find next word, including trailing spaces
         * @return
         */
        String next() {
            if (position >= limit)
                return null;
            int pos = source.indexOf(' ', position);
            if (pos < 0 || pos >= limit)
                pos = limit;
            String result = source.substring(position, pos);
            position = pos + 1;
            return result;
        }

        static int findMatchingEnd(String s1, String s2) {
            int i = s1.length();
            int j = s2.length();
            try {
                while (true) {
                    --i; // decrement both before calling function!
                    --j;
                    if (s1.charAt(i) != s2.charAt(j))
                        break;
                }
            } catch (Exception e) {} // run off start

            ++i; // counteract increment
            i = s1.indexOf(' ', i); // move forward to space
            if (i < 0)
                return 0;
            return s1.length() - i;
        }
    }

    private class RangeFinder {
        int start, limit;
        private int veryLimit;
        void reset(int start, int end) {
            this.limit = start;
            this.veryLimit = end;
        }
        String next() {
            if (limit >= veryLimit)
                return null;
            start = limit;
            String label = labelSource.getPropertyValue(limit++);
            for (; limit < veryLimit; ++limit) {
                String s = labelSource.getPropertyValue(limit);
                if (!s.equals(label))
                    break;
            }
            return label;
        }
    }

    public boolean isAbbreviated() {
        return abbreviated;
    }

    public void setAbbreviated(boolean b) {
        abbreviated = b;
    }

    public UnicodePropertySource getSource() {
        return source;
    }

    public UnicodePropertySource getLabelSource() {
        return labelSource;
    }

    public void setLabelSource(UnicodePropertySource source) {
        labelSource = source;
    }
    
    /**
     * @deprecated
     */
    public static void addAll(UnicodeSet source, Collection target) {
        source.addAllTo(target);
    }
    
    // UTILITIES
    
    public static final Transliterator hex = Transliterator.getInstance(
        "[^\\u0021-\\u007E\\u00A0-\\u00FF] hex");
    
    public interface Shower {
        public void println(String arg);
    }
    
    public static Shower CONSOLE = new Shower() {
        public void println(String arg) {
            System.out.println(arg);
        }
    };
        
    public static BufferedReader openUTF8Reader(String dir, String filename, Shower shower) throws IOException {
        File file = new File(dir + filename);
        if (shower != null) {
            shower.println("Creating File: " 
                + file.getCanonicalPath());
        }
        return new BufferedReader(
            new InputStreamReader(
                new FileInputStream(file),
                "UTF-8"),
            4*1024);       
    }
    
    public static PrintWriter openUTF8Writer(String dir, String filename, Shower shower) throws IOException {
        File file = new File(dir + filename);
        if (shower != null) {
            shower.println("Creating File: " 
                + file.getCanonicalPath());
        }
        //File parent = new File(file.getParent());
        //parent.mkdirs();
        return new PrintWriter(
            new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(file),
                    "UTF-8"),
                4*1024));       
    }

}