/*
 **********************************************************************
 * Copyright (c) 2009, Google, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 */
package com.ibm.icu.dev.test.translit;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.PrettyPrinter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class PrettyPrinterTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new PrettyPrinterTest().run(args);
    }
    public static final UnicodeSet TO_QUOTE = new UnicodeSet("[[:z:][:me:][:mn:][:di:][:c:]-[\u0020]]");

    public void TestBasicUnicodeSet() {

        final PrettyPrinter PRETTY_PRINTER = new PrettyPrinter()
        .setOrdering(Collator.getInstance(ULocale.ROOT))
        .setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(Collator.PRIMARY))
        .setToQuote(TO_QUOTE)
        .setSpaceComparator(Collator.getInstance(ULocale.ROOT).setStrength2(Collator.PRIMARY));

        UnicodeSet expected = new UnicodeSet("[:L:]");
        String formatted = PRETTY_PRINTER.format(expected);
        logln(formatted);
        UnicodeSet actual = new UnicodeSet(formatted);
        assertEquals("PrettyPrinter preserves meaning", expected, actual);
    }
}
