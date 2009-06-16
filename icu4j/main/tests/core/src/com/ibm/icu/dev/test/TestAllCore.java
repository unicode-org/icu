package com.ibm.icu.dev.test;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

public class TestAllCore extends TestGroup {

    public static void main(String[] args) {
        new TestAllCore().run(args);
    }

    public TestAllCore() {
        super(
              new String[] {
                  "com.ibm.icu.dev.test.format.TestAll",
                  "com.ibm.icu.dev.test.compression.TestAll",
                  "com.ibm.icu.dev.test.rbbi.TestAll",
                  "com.ibm.icu.dev.test.translit.TestAll",
                  "com.ibm.icu.dev.test.search.SearchTest", // not a group
                  "com.ibm.icu.dev.test.collator.TestAll",
                  "com.ibm.icu.dev.test.shaping.ArabicShapingRegTest",
                  "com.ibm.icu.dev.test.calendar.TestAll",
                  "com.ibm.icu.dev.test.timezone.TestAll",
                  "com.ibm.icu.dev.test.lang.TestAll",
                  "com.ibm.icu.dev.test.normalizer.TestAll",
                  "com.ibm.icu.dev.test.util.TestAll",
                  "com.ibm.icu.dev.test.iterator.TestUCharacterIterator", // not a group
                  "com.ibm.icu.dev.test.bigdec.DiagBigDecimal", // not a group
                  "com.ibm.icu.dev.test.impl.TestAll",
                  "com.ibm.icu.dev.test.stringprep.TestAll",
                  "com.ibm.icu.dev.test.timescale.TestAll",
                  "com.ibm.icu.dev.test.charsetdet.TestCharsetDetector",
                  "com.ibm.icu.dev.test.bidi.TestAll",
                  "com.ibm.icu.dev.test.duration.TestAll",
                  "com.ibm.icu.dev.test.serializable.SerializableTest" // *is* a group
              },
              "All core tests in ICU");
    }

    public static final String CLASS_TARGET_NAME  = "Core";
}
