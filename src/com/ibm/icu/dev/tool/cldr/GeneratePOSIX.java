/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Mark Davis
**********************************************************************
*/
package com.ibm.icu.dev.tool.cldr;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.xml.sax.SAXException;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

import com.ibm.icu.dev.test.util.Relation;
import com.ibm.icu.dev.test.util.SortedBag;
import com.ibm.icu.dev.tool.UOption;

/**
 * Class to generate POSIX format from CLDR. This is just a prototype version,
 * that is driven off of ICU4J. The eventual version will want to take CLDR
 * data directly from the XML.
 * TODO Get the data directly from the CLDR tree.
 * @author medavis
 */
public class GeneratePOSIX {
    // http://www.opengroup.org/onlinepubs/009695399/basedefs/xbd_chap07.html

    private static final int
        HELP1 = 0,
        HELP2 = 1,
        SOURCEDIR = 2,
        DESTDIR = 3,
        MATCH = 4,
        UNICODESET = 5,
        CHARSET = 6;

    private static final UOption[] options = {
        UOption.HELP_H(),
        UOption.HELP_QUESTION_MARK(),
        UOption.SOURCEDIR().setDefault("C:\\ICU4C\\locale\\common\\"),
        UOption.DESTDIR().setDefault("C:\\DATA\\GEN\\"),
        UOption.create("match", 'm', UOption.REQUIRES_ARG).setDefault("hu"),
        UOption.create("unicodeset", 'u', UOption.REQUIRES_ARG).setDefault("[\\u0000-\\U0010FFFF]"),
        UOption.create("charset", 'c', UOption.REQUIRES_ARG).setDefault("iso8859-2"),
    };

    public static void main(String[] args) throws IOException {
        UOption.parseArgs(args, options);
        //testSortedBag();
        // TODO change to walk through available locales
        String locale = options[MATCH].value;
        GeneratePOSIX gp = new GeneratePOSIX(new ULocale(locale),
//                    new UnicodeSet("[\u0000-\u00FF]")
              new UnicodeSet(options[UNICODESET].value),
              Charset.forName(options[CHARSET].value)
        );
        PrintWriter out = BagFormatter.openUTF8Writer(options[DESTDIR].value,locale + "_posix.txt");
        gp.write(out);
        out.close();
    }



    /**
     *
     */
    private static void testSortedBag() {
        SortedBag foo = new SortedBag(Collator.getInstance());
        foo.add("\u0001");
        foo.add("\u0002");
        for (Iterator it = foo.iterator(); it.hasNext();) {
            System.out.println(Utility.hex(((String)it.next()).charAt(0),4));
        }
    }

    SortedBag allItems;
    SortedBag contractions;
    //PrintWriter out;
    //Map definedID = new HashMap();
    RuleBasedCollator col;
    UnicodeSet chars;
    Charset cs;

    public GeneratePOSIX(ULocale locale, UnicodeSet chars, Charset cs) {
        this.cs = cs;
        if (cs != null) {
            UnicodeSet csset = new SimpleConverter(cs).getCharset();
            chars = new UnicodeSet(chars).retainAll(csset);
        }
        this.chars = chars;
        System.out.println("Generating: " + locale.getDisplayName());
        col = (RuleBasedCollator) RuleBasedCollator.getInstance(locale);
        allItems = new SortedBag(col);
        contractions = new SortedBag(col);

        // add all the chars
        for (UnicodeSetIterator it = new UnicodeSetIterator(chars); it.next();) {
            allItems.add(it.getString());
        }
        // get the tailored contractions
        // we need to filter only the ones in chars
        UnicodeSet tailored = col.getTailoredSet();
        getFilteredSet(chars, tailored);
        getFilteredSet(uca_contractions, tailored);
    }

    /**
     * @param chars
     * @param tailored
     */
    private void getFilteredSet(UnicodeSet chars, UnicodeSet tailored) {
        for (UnicodeSetIterator it = new UnicodeSetIterator(tailored); it.next();) {
            if (it.codepoint != it.IS_STRING) continue;
            String s = it.getString();
            s = Normalizer.compose(s,false);    // normalize to make sure
            if (!UTF16.hasMoreCodePointsThan(s, 1)) continue;
            if (!chars.containsAll(s)) continue;
            System.out.println("Contractions: " + it.getString());
            contractions.add(s);
            allItems.add(s);
        }
    }

    public void write(PrintWriter out) {
        out.println("######################");
        out.println("# POSIX locale");
        out.println("# Generated automatically from the Unicode Character Database and Common Locale Data Repository");
        out.println("# see http://www.opengroup.org/onlinepubs/009695399/basedefs/xbd_chap07.html");
        out.println("# charset:\t" + cs);
        out.println("# characters:\t" + chars.toPattern(true));
        out.println("######################");
        out.println();
        doCtype(out);
        out.println("######################");
        out.println();
        doCollate(out);
        out.println("######################");
    }

    /**
     * @param out
     */
    private void doCollate(PrintWriter out) {
        out.println("LC_COLLATE");
        out.println();
        writeDefinitions(out);
        out.println();
        out.println("order_start forward;" +
                (col.isFrenchCollation() ? "backward" : "forward")
                + ";forward");
        out.println();
        out.println("# weights used, in order");
        out.println();
        writeList(out);
        out.println();
        out.println("# assignment of characters to weights");
        out.println();
        for (Iterator it = allItems.iterator(); it.hasNext();) {
            out.println(showLine(col, (String) it.next()));
        }
        out.println("UNDEFINED \t...;" + getID('X', 5) + ";" + getID('X', 5));
        out.println();
        out.println("order_end");
        out.println("END LC_COLLATE");
    }

    /**
     * @param out
     */
    private void doCtype(PrintWriter out) {
        out.println("LC_CTYPE");
//      digit        <zero>;\
//      ..           <eight>;\
//                   <nine>
        String[][] types = { { "alpha", "[:Alphabetic:]" },
                { "upper", "[:Uppercase:]" },
                { "lower", "[:Lowercase:]" },
                { "space", "[:Whitespace:]" },
                { "cntrl", "[:Control:]" },
                { "graph", "[^[:Whitespace:][:Control:][:Format:][:Surrogate:][:Unassigned:]]" },
                { "print", "[[:Whitespace:]-[[:Control:][:Format:][:Surrogate:][:Unassigned:]]]" },
                { "punct", "[:Punctuation:]" },
                { "digit", "[:Decimal_Number:]" },
                { "xdigit", "[[:Decimal_Number:]a-f, A-F, \uff21-\uff26, \uff41-\uff46]" },
                { "blank", "[[:Whitespace:]-[\\u000A-\\u000D \\u0085 [:Line_Separator:][:Paragraph_Separator:]]]" } };
        // print character types, restricted to the charset
        int item, last;
        for (int i = 0; i < types.length; ++i) {
            UnicodeSet us = new UnicodeSet(types[i][1]).retainAll(chars);
            item = 0;
            last = us.size() - 1;
            for (UnicodeSetIterator it = new UnicodeSetIterator(us); it.next(); ++item) {
                if (item == 0) out.print(types[i][0]);
                out.print("\t" + getID('U',it.codepoint));
                if (item != last) out.print(";\\");
                out.println(" \t# " + getName(it.getString()));
            }
            out.println();
        }
/*
toupper (<a>,<A>);(<b>,<B>);(<c>,<C>);(<d>,<D>);(<e>,<E>);\
        (<f>,<F>);(<g>,<G>);(<h>,<H>);(<i>,<I>);(<j>,<J>);\
        (<k>,<K>);(<l>,<L>);(<m>,<M>);(<n>,<N>);(<o>,<O>);\
        (<p>,<P>);(<q>,<Q>);(<r>,<R>);(<s>,<S>);(<t>,<T>);\
        (<u>,<U>);(<v>,<V>);(<w>,<W>);(<x>,<X>);(<y>,<Y>);\
        (<z>,<Z>);\
        ...
        (<t-cedilla>,<T-cedilla>)
*/
        UnicodeSet us = new UnicodeSet();
        for (UnicodeSetIterator it = new UnicodeSetIterator(chars); it.next();) {
            int low = UCharacter.toUpperCase(it.codepoint);
            if (low != it.codepoint) us.add(it.codepoint);
        }
        item = 0;
        last = chars.size() - 1;
        for (UnicodeSetIterator it = new UnicodeSetIterator(us); it.next(); ++item) {
            if (item == 0) out.print("toupper");
            out.print("\t(<" + getID('U',it.codepoint) + ">,<" +
                    getID('U',UCharacter.toUpperCase(it.codepoint)) + ">)");
            if (item != last) out.print(";\\");
            out.println(" \t# " + getName(it.getString()));
        }
        out.println();
    }

    private void writeDefinitions(PrintWriter out) {
        //collating-element <A-A> from "<U0041><U0041>"
        StringBuffer buffer = new StringBuffer();
        for (Iterator it = contractions.iterator(); it.hasNext();) {
            buffer.setLength(0);
            String s = (String) it.next();
            buffer.append("collating-element ")
            .append(getID(s, true))
            .append(" from ")
            .append(getID(s, false));
            out.println(buffer.toString());
        }
    }

    private class IntList {
        private BitSet stuff = new BitSet();
        private int leastItem = Integer.MAX_VALUE;
        void add(int item) {
            stuff.set(item);
            if (item < leastItem) leastItem = item;
        }
        void remove(int item) {
            stuff.clear(item);
            if (item == leastItem) {
                // search for new least
                for (int i = item+1; i < stuff.size(); ++i) {
                    if (stuff.get(i)) {
                        leastItem = i;
                        return;
                    }
                }
                leastItem = Integer.MAX_VALUE; // failed, now empty
            }
        }
        int getLeast() {
            return leastItem;
        }
    }

    IntList needToWritePrimary = new IntList();
    Set nonUniqueWeights = new HashSet();
    Set allWeights = new HashSet();
    Map stringToWeights = new HashMap();

    private void writeList(PrintWriter out) {
        BitSet alreadySeen = new BitSet();
        BitSet needToWrite = new BitSet();
        needToWrite.set(1); // special weight for uniqueness
        int maxSeen = 0;
        for (Iterator it1 = allItems.iterator(); it1.hasNext();) {
            String string = (String) it1.next();
            Weights w = new Weights(col.getCollationElementIterator(string));
            w.primaries.setBits(needToWrite);
            w.secondaries.setBits(needToWrite);
            w.tertiaries.setBits(needToWrite);
            if (allWeights.contains(w)) nonUniqueWeights.add(w);
            allWeights.add(w);
            stringToWeights.put(string, w);
        }
        for (int i = 0; i < needToWrite.size(); ++i) {
            if (needToWrite.get(i)) out.println(getID('X', i));
        }
    }

    /**
     * @param col
     * @param string
     */
    private String showLine(RuleBasedCollator col, String string) {
        String prefix = "";
        StringBuffer result = new StringBuffer();
        result.append(getID(string, true));
        result.append(" \t");
        // gather data
        Weights w = (Weights) stringToWeights.get(string);
        result.append(w.primaries)
        .append(";")
        .append(w.secondaries)
        .append(";")
        .append(w.tertiaries)
        .append(";")
        .append(nonUniqueWeights.contains(w)
                ? getID(Normalizer.decompose(string,false), false)
                : getID('X', 1))
        .append(" \t# ")
        .append(getName(string));


        if (prefix.length() != 0) result.insert(0,prefix);
        return result.toString();
    }

    /* primaries.size() == 0
                    && secondaries.size() == 0
                    && tertiaries.size() == 0
                ? "IGNORE"
                        : */

    /**
     * @param string
     * @return
     */
    private Object getName(String s) {
        int cp;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            if (i != 0) result.append(", ");
            String n = UCharacter.getExtendedName(cp);
            result.append(n);
        }
        return result.toString();
    }

    /**
     * @param leadChar TODO
     * @param i
     * @param intList
     * @return
     */
    private static String getID(char leadChar, int i) {
        return "<" + leadChar + Utility.hex(i,4)+ ">";
    }

    /**
     * @param i
     * @param intList
     */
    private class Weights {
        WeightList primaries = new WeightList();
        WeightList secondaries = new WeightList();
        WeightList tertiaries = new WeightList();
        public Weights(CollationElementIterator it) {
            while (true) {
                int ce = it.next();
                if (ce == it.NULLORDER) break;
                int p = it.primaryOrder(ce);
                primaries.append(p);
                secondaries.append(it.secondaryOrder(ce));
                tertiaries.append(it.tertiaryOrder(ce));
            }
        }
        public boolean equals(Object other) {
            Weights that = (Weights)other;
            return primaries.equals(that.primaries)
                && secondaries.equals(that.secondaries)
                && tertiaries.equals(that.tertiaries);
        }
        public int hashCode() {
            return (primaries.hashCode()*37
                + secondaries.hashCode())*37
                + tertiaries.hashCode();
        }
    }

    private class WeightList {
        char[] weights = new char[5];
        // TODO lengthen on demand
        int count = 0;
        public void append(int i) {
            // add each 16-bit quantity
            for (int j = 16; j >= 0; j -= 16) {
                char b = (char)(i >>> j);
                if (b == 0) continue;
                weights[count++] = b;
            }
        }
        public void setBits(BitSet s) {
            for (int j = 0; j < count; ++j) s.set(weights[j]);
        }

        public String toString() {
            if (count == 0) return "IGNORE";
            if (count == 1) return getID('X', weights[0]);
            String result = "\"";
            for (int i = 0; i < count; ++i) {
                result += getID('X', weights[i]);
            }
            return result + "\"";
        }
        public boolean equals(Object other) {
            WeightList that = (WeightList)other;
            for (int j = 0; j < count; ++j) {
                if (weights[j] != that.weights[j]) return false;
            }
            return true;
        }
        public int hashCode() {
            int result = count;
            for (int j = 0; j < count; ++j) result = result*37 + weights[j];
            return result;
        }
    }

    private String getID(String s, boolean isSingleID) {
        //Object defined = definedID.get(s);
        //if (defined != null) return (String) defined;
        //if (defined != null) return (String) defined;

        StringBuffer result = new StringBuffer();
        if (!UTF16.hasMoreCodePointsThan(s, 1)) {
            // single code point
            appendID(UTF16.charAt(s,0), result, false);
        } else if (isSingleID) {
            result.append('<');
            int cp;
            for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(s, i);
                if (i != 0) result.append('-');
                appendID(cp, result, true);
            }
            result.append('>');
        } else {
            result.append('"');
            int cp;
            for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
                cp = UTF16.charAt(s, i);
                appendID(cp, result, false);
            }
            result.append('"');
        }
        return result.toString();
    }

    private StringBuffer appendID(int cp, StringBuffer result, boolean nakedID) {
        if (!nakedID) result.append('<');
        result.append('U').append(Utility.hex(cp,4));
        if (!nakedID) result.append('>');
        return result;
    }

    UnicodeSet uca_contractions = new UnicodeSet("[{\u0406\u0308}{\u0410\u0306}{\u0410\u0308}{\u0413\u0301}{\u0413\u0341}{\u0415\u0306}{\u0416\u0308}{\u0417\u0308}{\u0418\u0306}{\u0418\u0308}{\u041A\u0301}{\u041A\u0341}{\u041E\u0308}{\u0423\u0306}{\u0423\u0308}{\u0423\u030B}{\u0427\u0308}{\u042B\u0308}{\u042D\u0308}{\u0430\u0306}{\u0430\u0308}{\u0433\u0301}{\u0433\u0341}{\u0435\u0306}{\u0436\u0308}{\u0437\u0308}{\u0438\u0306}{\u0438\u0308}{\u043A\u0301}{\u043A\u0341}{\u043E\u0308}{\u0443\u0306}{\u0443\u0308}{\u0443\u030B}{\u0447\u0308}{\u044B\u0308}{\u044D\u0308}{\u0456\u0308}{\u0474\u030F}{\u0475\u030F}{\u04D8\u0308}{\u04D9\u0308}{\u04E8\u0308}{\u04E9\u0308}{\u0627\u0653}{\u0627\u0654}{\u0627\u0655}{\u0648\u0654}{\u064A\u0654}{\u09C7\u09BE}{\u09C7\u09D7}{\u0B47\u0B3E}{\u0B47\u0B56}{\u0B47\u0B57}{\u0B92\u0BD7}{\u0BC6\u0BBE}{\u0BC6\u0BD7}{\u0BC7\u0BBE}{\u0C46\u0C56}{\u0CBF\u0CD5}{\u0CC6\u0CC2}{\u0CC6\u0CC2\u0CD5}{\u0CC6\u0CD5}{\u0CC6\u0CD6}{\u0CCA\u0CD5}{\u0D46\u0D3E}{\u0D46\u0D57}{\u0D47\u0D3E}{\u0DD9\u0DCA}{\u0DD9\u0DCF}{\u0DD9\u0DCF\u0DCA}{\u0DD9\u0DDF}{\u0DDC\u0DCA}{\u0E4D\u0E32}{\u0ECD\u0EB2}{\u0F71\u0F72}{\u0F71\u0F74}{\u0F71\u0F80}{\u0FB2\u0F71}{\u0FB2\u0F71\u0F80}{\u0FB2\u0F80}{\u0FB2\u0F81}{\u0FB3\u0F71}{\u0FB3\u0F71\u0F80}{\u0FB3\u0F80}{\u0FB3\u0F81}{\u1025\u102E}]");
}