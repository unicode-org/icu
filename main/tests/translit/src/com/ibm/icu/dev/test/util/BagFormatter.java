/*
 *******************************************************************************
 * Copyright (C) 2002-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class BagFormatter {
    static final boolean DEBUG = false;
    public static final boolean SHOW_FILES;
    static {
    boolean showFiles = false;
    try {
        showFiles = System.getProperty("SHOW_FILES") != null;
    }
    catch (SecurityException e) {
    }
    SHOW_FILES = showFiles;
    }

    public static final PrintWriter CONSOLE = new PrintWriter(System.out,true);

    private static PrintWriter log = CONSOLE;

    private boolean abbreviated = false;
    private String separator = ",";
    private String prefix = "[";
    private String suffix = "]";
    private UnicodeProperty.Factory source;
    private UnicodeLabel nameSource;
    private UnicodeLabel labelSource;
    private UnicodeLabel rangeBreakSource;
    private UnicodeLabel valueSource;
    private String propName = "";
    private boolean showCount = true;
    //private boolean suppressReserved = true;
    private boolean hexValue = false;
    private static final String NULL_VALUE = "_NULL_VALUE_";
    private int fullTotal = -1;
    private boolean showTotal = true;
    private String lineSeparator = "\r\n";
    private Tabber tabber = new Tabber.MonoTabber();

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

        StringWriter result = new StringWriter();
        showSetDifferences(new PrintWriter(result),name1,set1,name2,set2);
        result.flush();
        return result.getBuffer().toString();
    }

    public String showSetDifferences(
        String name1,
        Collection set1,
        String name2,
        Collection set2) {

        StringWriter result = new StringWriter();
        showSetDifferences(new PrintWriter(result), name1, set1, name2, set2);
        result.flush();
        return result.getBuffer().toString();
    }

    public void showSetDifferences(
            PrintWriter pw,
            String name1,
            UnicodeSet set1,
            String name2,
            UnicodeSet set2) {
        showSetDifferences(pw, name1, set1, name2, set2, -1);
    }
    /**
     * Compare two UnicodeSets, and show the differences
     * @param name1 name of first set to be compared
     * @param set1 first set
     * @param name2 name of second set to be compared
     * @param set2 second set
     */
    public void showSetDifferences(
        PrintWriter pw,
        String name1,
        UnicodeSet set1,
        String name2,
        UnicodeSet set2,
        int flags) 
    {
        if (pw == null) pw = CONSOLE;
        String[] names = { name1, name2 };

        UnicodeSet temp;
        
        if ((flags&1) != 0) {
            temp = new UnicodeSet(set1).removeAll(set2);
            pw.print(lineSeparator);
            pw.print(inOut.format(names));
            pw.print(lineSeparator);
            showSetNames(pw, temp);
        }

        if ((flags&2) != 0) {
            temp = new UnicodeSet(set2).removeAll(set1);
            pw.print(lineSeparator);
            pw.print(outIn.format(names));
            pw.print(lineSeparator);
            showSetNames(pw, temp);
        }

        if ((flags&4) != 0) {
            temp = new UnicodeSet(set2).retainAll(set1);
            pw.print(lineSeparator);
            pw.print(inIn.format(names));
            pw.print(lineSeparator);
            showSetNames(pw, temp);
        }
        pw.flush();
    }

    public void showSetDifferences(
        PrintWriter pw,
        String name1,
        Collection set1,
        String name2,
        Collection set2) {

        if (pw == null) pw = CONSOLE;
        String[] names = { name1, name2 };
        // damn'd collection doesn't have a clone, so
        // we go with Set, even though that
        // may not preserve order and duplicates
        Collection temp = new HashSet(set1);
        temp.removeAll(set2);
        pw.println();
        pw.println(inOut.format(names));
        showSetNames(pw, temp);

        temp.clear();
        temp.addAll(set2);
        temp.removeAll(set1);
        pw.println();
        pw.println(outIn.format(names));
        showSetNames(pw, temp);

        temp.clear();
        temp.addAll(set1);
        temp.retainAll(set2);
        pw.println();
        pw.println(inIn.format(names));
        showSetNames(pw, temp);
    }

    /**
     * Returns a list of items in the collection, with each separated by the separator.
     * Each item must not be null; its toString() is called for a printable representation
     * @param c source collection
     * @return a String representation of the list
     */
    public String showSetNames(Object c) {
        StringWriter buffer = new StringWriter();
        PrintWriter output = new PrintWriter(buffer);
        showSetNames(output,c);
        return buffer.toString();
    }

    /**
     * Returns a list of items in the collection, with each separated by the separator.
     * Each item must not be null; its toString() is called for a printable representation
     * @param output destination to which to write names
     * @param c source collection
     */
    public void showSetNames(PrintWriter output, Object c) {
        mainVisitor.doAt(c, output);
        output.flush();
    }

    /**
     * Returns a list of items in the collection, with each separated by the separator.
     * Each item must not be null; its toString() is called for a printable representation
     * @param filename destination to which to write names
     * @param c source collection
     */
    public void showSetNames(String filename, Object c) throws IOException {
        PrintWriter pw = new PrintWriter(
            new OutputStreamWriter(
                new FileOutputStream(filename),"utf-8"));
        showSetNames(log,c);
        pw.close();
    }

    public String getAbbreviatedName(
        String src,
        String pattern,
        String substitute) {

        int matchEnd = NameIterator.findMatchingEnd(src, pattern);
        int sdiv = src.length() - matchEnd;
        int pdiv = pattern.length() - matchEnd;
        StringBuffer result = new StringBuffer();
        addMatching(
            src.substring(0, sdiv),
            pattern.substring(0, pdiv),
            substitute,
            result);
        addMatching(
            src.substring(sdiv),
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

    public BagFormatter setMergeRanges(boolean in) {
        mergeRanges = in;
        return this;
    }
    public BagFormatter setShowSetAlso(boolean b) {
        showSetAlso = b;
        return this;
    }

    public String getName(int codePoint) {
        return getName("", codePoint, codePoint);
    }

    public String getName(String sep, int start, int end) {
        if (getNameSource() == null || getNameSource() == UnicodeLabel.NULL) return "";
        String result = getName(start, false);
        if (start == end) return sep + result;
        String endString = getName(end, false);
        if (result.length() == 0 && endString.length() == 0) return sep;
        if (abbreviated) endString = getAbbreviatedName(endString,result,"~");
        return sep + result + ".." + endString;
    }

    public String getName(String s) {
        return getName(s, false);
    }

    public static class NameLabel extends UnicodeLabel {
        UnicodeProperty nameProp;
        UnicodeSet control;
        UnicodeSet private_use;
        UnicodeSet noncharacter;
        UnicodeSet surrogate;

        public NameLabel(UnicodeProperty.Factory source) {
            nameProp = source.getProperty("Name");
            control = source.getSet("gc=Cc");
            private_use = source.getSet("gc=Co");
            surrogate = source.getSet("gc=Cs");
            noncharacter = source.getSet("noncharactercodepoint=yes");
        }

        public String getValue(int codePoint, boolean isShort) {
            String hcp = !isShort
                ? "U+" + Utility.hex(codePoint, 4) + " "
                : "";
            String result = nameProp.getValue(codePoint);
            if (result != null)
                return hcp + result;
            if (control.contains(codePoint)) {
                return "<control-" + Utility.hex(codePoint, 4) + ">";
            }
            if (private_use.contains(codePoint)) {
                return "<private-use-" + Utility.hex(codePoint, 4) + ">";
            }
            if (surrogate.contains(codePoint)) {
                return "<surrogate-" + Utility.hex(codePoint, 4) + ">";
            }
            if (noncharacter.contains(codePoint)) {
                return "<noncharacter-" + Utility.hex(codePoint, 4) + ">";
            }
            //if (suppressReserved) return "";
            return hcp + "<reserved-" + Utility.hex(codePoint, 4) + ">";
        }

    }

    // refactored
    public String getName(int codePoint, boolean withCodePoint) {
        String result = getNameSource().getValue(codePoint, !withCodePoint);
        return fixName == null ? result : fixName.transliterate(result);
    }

    public String getName(String s, boolean withCodePoint) {
           String result = getNameSource().getValue(s, separator, !withCodePoint);
        return fixName == null ? result : fixName.transliterate(result);
     }

    public String hex(String s) {
        return hex(s,separator);
    }

    public String hex(String s, String sep) {
        return UnicodeLabel.HEX.getValue(s, sep, true);
    }

    public String hex(int start, int end) {
        String s = Utility.hex(start,4);
        if (start == end) return s;
        return s + ".." + Utility.hex(end,4);
    }

    public BagFormatter setUnicodePropertyFactory(UnicodeProperty.Factory source) {
        this.source = source;
        return this;
    }

    public UnicodeProperty.Factory getUnicodePropertyFactory() {
        if (source == null) source = ICUPropertyFactory.make();
        return source;
    }

    public BagFormatter () {
    }

    public BagFormatter (UnicodeProperty.Factory source) {
        setUnicodePropertyFactory(source);
    }

    public String join(Object o) {
        return labelVisitor.join(o);
    }

    // ===== PRIVATES =====

    private Join labelVisitor = new Join();

    private boolean mergeRanges = true;
    private Transliterator showLiteral = null;
    private Transliterator fixName = null;
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
        String src,
        String pattern,
        String substitute,
        StringBuffer result) {
        NameIterator n1 = new NameIterator(src);
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

    private static NumberFormat nf =
        NumberFormat.getIntegerInstance(Locale.ENGLISH);
    static {
        nf.setGroupingUsed(false);
    }

    private class MyVisitor extends Visitor {
        private PrintWriter output;
        String commentSeparator;
        int counter;
        int valueSize;
        int labelSize;
        boolean isHtml;
        boolean inTable = false;
        
        public void toOutput(String s) {
          if (isHtml) {
            if (inTable) {
              output.print("</table>");
              inTable = false;
            }
            output.print("<p>");
          }
          output.print(s);
          if (isHtml)
            output.println("</p>");
          else
            output.print(lineSeparator);
        }
        
        public void toTable(String s) {
          if (isHtml && !inTable) {
            output.print("<table>");
            inTable = true;
          }
          output.print(tabber.process(s) +  lineSeparator);
        }

        public void doAt(Object c, PrintWriter out) {
            output = out;
            isHtml = tabber instanceof Tabber.HTMLTabber;
            counter = 0;
            
            tabber.clear();
            // old:
            // 0009..000D    ; White_Space # Cc   [5] <control-0009>..<control-000D>
            // new
            // 0009..000D    ; White_Space #Cc  [5] <control>..<control>
            tabber.add(mergeRanges ? 14 : 6,Tabber.LEFT);

            if (propName.length() > 0) {
                tabber.add(propName.length() + 2,Tabber.LEFT);
            }

            valueSize = getValueSource().getMaxWidth(shortValue);
            if (DEBUG) System.out.println("ValueSize: " + valueSize);
            if (valueSize > 0) {
                tabber.add(valueSize + 2,Tabber.LEFT); // value
            }

            tabber.add(3,Tabber.LEFT); // comment character

            labelSize = getLabelSource(true).getMaxWidth(shortLabel);
            if (labelSize > 0) {
                tabber.add(labelSize + 1,Tabber.LEFT); // value
            }

            if (mergeRanges && showCount) {
                tabber.add(5,Tabber.RIGHT);
            }

            if (showLiteral != null) {
                tabber.add(4,Tabber.LEFT);
            }
            //myTabber.add(7,Tabber.LEFT);

            commentSeparator = (showCount || showLiteral != null
              || getLabelSource(true) != UnicodeLabel.NULL
              || getNameSource() != UnicodeLabel.NULL)
            ? "\t #" : "";

            if (DEBUG) System.out.println("Tabber: " + tabber.toString());
            if (DEBUG) System.out.println("Tabber: " + tabber.process(
                    "200C..200D\t; White_Space\t #\tCf\t [2]\t ZERO WIDTH NON-JOINER..ZERO WIDTH JOINER"));
            doAt(c);
        }

        @SuppressWarnings("unused")
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
              toOutput("#" + container);
            }
        }

        protected void doBetween(Object container, Object lastItem, Object nextItem) {
        }

        protected void doAfter(Object container, Object o) {
            if (fullTotal != -1 && fullTotal != counter) {
                if (showTotal) {
                    toOutput("");
                    toOutput("# The above property value applies to " + nf.format(fullTotal-counter) + " code points not listed here.");
                    toOutput("# Total code points: " + nf.format(fullTotal));
                }
                fullTotal = -1;
            } else if (showTotal) {
                toOutput("");
                toOutput("# Total code points: " + nf.format(counter));
            }
        }

        protected void doSimpleAt(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry oo = (Map.Entry)o;
                Object key = oo.getKey();
                Object value = oo.getValue();
                doBefore(o, key);
                doAt(key);
                output.println("\u2192");
                doAt(value);
                doAfter(o, value);
                counter++;
            } else if (o instanceof Visitor.CodePointRange) {
                doAt((Visitor.CodePointRange) o);
            } else {
                String thing = o.toString();
                String value = getValueSource() == UnicodeLabel.NULL ? "" : getValueSource().getValue(thing, ",", true);
                if (getValueSource() != UnicodeLabel.NULL) value = "\t; " + value;
                String label = getLabelSource(true) == UnicodeLabel.NULL ? "" : getLabelSource(true).getValue(thing, ",", true);
                if (label.length() != 0) label = " " + label;
                toTable(
                    hex(thing)
                    + value
                    + commentSeparator
                    + label
                    + insertLiteral(thing)
                    + "\t"
                    + getName(thing));
                counter++;
            }
        }

        protected void doAt(Visitor.CodePointRange usi) {
            if (!mergeRanges) {
                for (int cp = usi.codepoint; cp <= usi.codepointEnd; ++cp) {
                    showLine(cp, cp);
                }
            } else {
                rf.reset(usi.codepoint, usi.codepointEnd + 1);
                while (rf.next()) {
                    showLine(rf.start, rf.limit - 1);
                }
            }
        }

        private void showLine(int start, int end) {
            String label = getLabelSource(true).getValue(start, shortLabel);
            String value = getValue(start, shortValue);
            if (value == NULL_VALUE) return;

            counter += end - start + 1;
            String pn = propName;
            if (pn.length() != 0) {
                pn = "\t; " + pn;
            }
            if (valueSize > 0) {
                value = "\t; " + value;
            } else if (value.length() > 0) {
                throw new IllegalArgumentException("maxwidth bogus " + value + "," + getValueSource().getMaxWidth(shortValue));
            }
            if (labelSize > 0) {
                label = "\t" + label;
            } else if (label.length() > 0) {
                throw new IllegalArgumentException("maxwidth bogus " + label + ", " + getLabelSource(true).getMaxWidth(shortLabel));
            }

            String count = "";
            if (mergeRanges && showCount) {
                if (end == start) count = "\t";
                else count = "\t ["+ nf.format(end - start + 1)+ "]";
           }

            toTable(
                hex(start, end)
                + pn
                + value
                + commentSeparator
                + label
                + count
                + insertLiteral(start, end)
                + getName("\t ", start, end));
        }

        private String insertLiteral(String thing) {
            return (showLiteral == null ? ""
                :  " \t(" + showLiteral.transliterate(thing) + ") ");
        }

        private String insertLiteral(int start, int end) {
            return (showLiteral == null ? "" :
                " \t(" + showLiteral.transliterate(UTF16.valueOf(start))
                        + ((start != end)
                            ? (".." + showLiteral.transliterate(UTF16.valueOf(end)))
                            : "")
                + ") ");
        }
        /*
        private String insertLiteral(int cp) {
            return (showLiteral == null ? ""
                :  " \t(" + showLiteral.transliterate(UTF16.valueOf(cp)) + ") ");
        }
        */
    }

    /**
     * Iterate through a string, breaking at words.
     * @author Davis
     */
    private static class NameIterator {
        String source;
        int position;
        int limit;

        NameIterator(String source) {
            this.source = source;
            this.limit = source.length();
        }
        /**
         * Find next word, including trailing spaces
         * @return the next word
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
        //String label, value;
        void reset(int rangeStart, int rangeLimit) {
            limit = rangeStart;
            veryLimit = rangeLimit;
        }
        boolean next() {
            if (limit >= veryLimit)
                return false;
            start = limit; // set to end of last
            String label = getLabelSource(false).getValue(limit, true);
            String value = getValue(limit, true);
            String breaker = getRangeBreakSource().getValue(limit,true);
            if (DEBUG && limit < 0x7F) System.out.println("Label: " + label + ", Value: " + value + ", Break: " + breaker);
            limit++;
            for (; limit < veryLimit; limit++) {
                String s = getLabelSource(false).getValue(limit, true);
                String v = getValue(limit, true);
                String b = getRangeBreakSource().getValue(limit, true);
                if (DEBUG && limit < 0x7F) System.out.println("*Label: " + label + ", Value: " + value + ", Break: " + breaker);
                if (!equalTo(s, label) || !equalTo(v, value) || !equalTo(b, breaker)) break;
            }
            // at this point, limit is the first item that has a different label than source
            // OR, we got to the end, and limit == veryLimit
            return true;
        }
    }

    boolean equalTo(Object a, Object b) {
        if (a == b) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    boolean shortLabel = true;
    boolean shortValue = true;

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public BagFormatter setPrefix(String string) {
        prefix = string;
        return this;
    }

    public BagFormatter setSuffix(String string) {
        suffix = string;
        return this;
    }

    public boolean isAbbreviated() {
        return abbreviated;
    }

    public BagFormatter setAbbreviated(boolean b) {
        abbreviated = b;
        return this;
    }

    public UnicodeLabel getLabelSource(boolean visible) {
        if (labelSource == null) {
            Map labelMap = new HashMap();
            //labelMap.put("Lo","L&");
            labelMap.put("Lu","L&");
            labelMap.put("Lt","L&");
            labelMap.put("Ll","L&");
            labelSource = new UnicodeProperty.FilteredProperty(
                getUnicodePropertyFactory().getProperty("General_Category"),
                new UnicodeProperty.MapFilter(labelMap)
            ).setAllowValueAliasCollisions(true);
        }
        return labelSource;
    }

    /**
     * @deprecated
     */
    public static void addAll(UnicodeSet source, Collection target) {
        source.addAllTo(target);
    }

    // UTILITIES

    public static final Transliterator hex = Transliterator.getInstance(
        "[^\\u0009\\u0020-\\u007E\\u00A0-\\u00FF] hex");

    public static BufferedReader openUTF8Reader(String dir, String filename) throws IOException {
        return openReader(dir,filename,"UTF-8");
    }

    public static BufferedReader openReader(String dir, String filename, String encoding) throws IOException {
        File file = dir.length() == 0 ? new File(filename) : new File(dir, filename);
        if (SHOW_FILES && log != null) {
            log.println("Opening File: "
                + file.getCanonicalPath());
        }
        return new BufferedReader(
            new InputStreamReader(
                new FileInputStream(file),
                encoding),
            4*1024);
    }

    public static PrintWriter openUTF8Writer(String dir, String filename) throws IOException {
        return openWriter(dir,filename,"UTF-8");
    }

    public static PrintWriter openWriter(String dir, String filename, String encoding) throws IOException {
        File file = new File(dir, filename);
        if (SHOW_FILES && log != null) {
            log.println("Creating File: "
                + file.getCanonicalPath());
        }
        String parentName = file.getParent();
        if (parentName != null) {
            File parent = new File(parentName);
            parent.mkdirs();
        }
        return new PrintWriter(
            new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(file),
                    encoding),
                4*1024));
    }
    public static PrintWriter getLog() {
        return log;
    }
    public BagFormatter setLog(PrintWriter writer) {
        log = writer;
        return this;
    }
    public String getSeparator() {
        return separator;
    }
    public BagFormatter setSeparator(String string) {
        separator = string;
        return this;
    }
    public Transliterator getShowLiteral() {
        return showLiteral;
    }
    public BagFormatter setShowLiteral(Transliterator transliterator) {
        showLiteral = transliterator;
        return this;
    }

    // ===== CONVENIENCES =====
    private class Join extends Visitor {
        StringBuffer output = new StringBuffer();
        int depth = 0;
        String join (Object o) {
            output.setLength(0);
            doAt(o);
            return output.toString();
        }
        protected void doBefore(Object container, Object item) {
            ++depth;
            output.append(prefix);
        }
        protected void doAfter(Object container, Object item) {
            output.append(suffix);
            --depth;
        }
        protected void doBetween(Object container, Object lastItem, Object nextItem) {
            output.append(separator);
        }
        protected void doSimpleAt(Object o) {
            if (o != null) output.append(o.toString());
        }
    }

    /**
     * @param label
     */
    public BagFormatter setLabelSource(UnicodeLabel label) {
        if (label == null) label = UnicodeLabel.NULL;
        labelSource = label;
        return this;
    }

    /**
     * @return the NameLable representing the source
     */
    public UnicodeLabel getNameSource() {
        if (nameSource == null) {
            nameSource = new NameLabel(getUnicodePropertyFactory());
        }
        return nameSource;
    }

    /**
     * @param label
     */
    public BagFormatter setNameSource(UnicodeLabel label) {
        if (label == null) label = UnicodeLabel.NULL;
        nameSource = label;
        return this;
    }

    /**
     * @return the UnicodeLabel representing the value
     */
    public UnicodeLabel getValueSource() {
        if (valueSource == null) valueSource = UnicodeLabel.NULL;
        return valueSource;
    }

    private String getValue(int cp, boolean shortVal) {
        String result = getValueSource().getValue(cp, shortVal);
        if (result == null) return NULL_VALUE;
        if (hexValue) result = hex(result, " ");
        return result;
    }

    /**
     * @param label
     */
    public BagFormatter setValueSource(UnicodeLabel label) {
        if (label == null) label = UnicodeLabel.NULL;
        valueSource = label;
        return this;
    }

    public BagFormatter setValueSource(String label) {
        return setValueSource(new UnicodeLabel.Constant(label));
    }

    /**
     * @return true if showCount is true
     */
    public boolean isShowCount() {
        return showCount;
    }

    /**
     * @param b true to show the count
     * @return this (for chaining)
     */
    public BagFormatter setShowCount(boolean b) {
        showCount = b;
        return this;
    }

    /**
     * @return the property name
     */
    public String getPropName() {
        return propName;
    }

    /**
     * @param string
     * @return this (for chaining)
     */
    public BagFormatter setPropName(String string) {
        if (string == null) string = "";
        propName = string;
        return this;
    }

    /**
     * @return true if this is a hexValue
     */
    public boolean isHexValue() {
        return hexValue;
    }

    /**
     * @param b
     * @return this (for chaining)
     */
    public BagFormatter setHexValue(boolean b) {
        hexValue = b;
        return this;
    }

    /**
     * @return the full total
     */
    public int getFullTotal() {
        return fullTotal;
    }

    /**
     * @param i set the full total
     * @return this (for chaining)
     */
    public BagFormatter setFullTotal(int i) {
        fullTotal = i;
        return this;
    }

    /**
     * @return the line separator
     */
    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * @param string
     * @return this (for chaining)
     */
    public BagFormatter setLineSeparator(String string) {
        lineSeparator = string;
        return this;
    }

    /**
     * @return the UnicodeLabel representing the range break source
     */
    public UnicodeLabel getRangeBreakSource() {
        if (rangeBreakSource == null) {
            Map labelMap = new HashMap();
            // reflects the code point types on p 25
            labelMap.put("Lo", "G&");
            labelMap.put("Lm", "G&");
            labelMap.put("Lu", "G&");
            labelMap.put("Lt", "G&");
            labelMap.put("Ll", "G&");
            labelMap.put("Mn", "G&");
            labelMap.put("Me", "G&");
            labelMap.put("Mc", "G&");
            labelMap.put("Nd", "G&");
            labelMap.put("Nl", "G&");
            labelMap.put("No", "G&");
            labelMap.put("Zs", "G&");
            labelMap.put("Pd", "G&");
            labelMap.put("Ps", "G&");
            labelMap.put("Pe", "G&");
            labelMap.put("Pc", "G&");
            labelMap.put("Po", "G&");
            labelMap.put("Pi", "G&");
            labelMap.put("Pf", "G&");
            labelMap.put("Sm", "G&");
            labelMap.put("Sc", "G&");
            labelMap.put("Sk", "G&");
            labelMap.put("So", "G&");

            labelMap.put("Zl", "Cf");
            labelMap.put("Zp", "Cf");

            rangeBreakSource =
                new UnicodeProperty
                    .FilteredProperty(
                        getUnicodePropertyFactory().getProperty(
                            "General_Category"),
                        new UnicodeProperty.MapFilter(labelMap))
                    .setAllowValueAliasCollisions(true);

            /*
            "Cn", // = Other, Not Assigned 0
            "Cc", // = Other, Control 15
            "Cf", // = Other, Format 16
            UnicodeProperty.UNUSED, // missing
            "Co", // = Other, Private Use 18
            "Cs", // = Other, Surrogate 19
            */
        }
        return rangeBreakSource;
    }

    /**
     * @param label
     */
    public BagFormatter setRangeBreakSource(UnicodeLabel label) {
        if (label == null) label = UnicodeLabel.NULL;
        rangeBreakSource = label;
        return this;
    }

    /**
     * @return Returns the fixName.
     */
    public Transliterator getFixName() {
        return fixName;
    }
    /**
     * @param fixName The fixName to set.
     */
    public BagFormatter setFixName(Transliterator fixName) {
        this.fixName = fixName;
        return this;
    }

    public Tabber getTabber() {
        return tabber;
    }

    public void setTabber(Tabber tabber) {
        this.tabber = tabber;
    }

    public boolean isShowTotal() {
        return showTotal;
    }

    public void setShowTotal(boolean showTotal) {
        this.showTotal = showTotal;
    }
}
