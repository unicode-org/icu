/*
 *******************************************************************************
 * Copyright (C) 2011-2012, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.translit;

import java.util.List;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.ICUPropertyFactory;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.dev.util.UnicodeProperty.Factory;
import com.ibm.icu.dev.util.UnicodePropertySymbolTable;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author markdavis
 *
 */
public class TestUnicodeProperty extends TestFmwk{
    public static void main(String[] args) {
        new TestUnicodeProperty().run(args);
    }
    static final UnicodeSet casedLetter = new UnicodeSet("[:gc=cased letter:]");
    static final UnicodeSet letter = new UnicodeSet("[:gc=L:]");


    public void TestBasic() {
        Factory factory = ICUPropertyFactory.make();
        UnicodeProperty property = factory.getProperty("gc");
        List values = property.getAvailableValues();
        assertTrue("Values contain GC values", values.contains("Unassigned"));
        final UnicodeSet lu = property.getSet("Lu");
        if (!assertTrue("Gc=L contains 'A'", lu.contains('A'))) {
            errln("Contents:\t" + lu.complement().complement().toPattern(false));
        }
    }

    public void TestSymbolTable() {
        Factory factory = ICUPropertyFactory.make();
        UnicodePropertySymbolTable upst = new UnicodePropertySymbolTable(factory);
        UnicodeSet.setDefaultXSymbolTable(upst);
        try {
            final UnicodeSet luSet = new UnicodeSet("[:gc=L:]");
            assertTrue("Gc=L contains 'A'", luSet.contains('A'));
            assertTrue("Gc=L contains 'Z'", luSet.contains('Z'));
            assertFalse("Gc=L contains 'a'", luSet.contains('1'));
            UnicodeSet casedLetter2 = new UnicodeSet("[:gc=cased letter:]");
            assertEquals("gc=lc are equal", casedLetter, casedLetter2);
        } finally {
            // restore the world
            UnicodeSet.setDefaultXSymbolTable(null);
        }
    }

    public void TestSymbolTable2() {
        Factory factory = new MyUnicodePropertyFactory();
        UnicodePropertySymbolTable upst = new UnicodePropertySymbolTable(factory);
        UnicodeSet.setDefaultXSymbolTable(upst);
        try {
            final UnicodeSet luSet = new UnicodeSet("[:gc=L:]");
            assertFalse("Gc=L contains 'A'", luSet.contains('A'));
            if (!assertTrue("Gc=L contains 'Z'", luSet.contains('Z'))) {
                errln("Contents:\t" + luSet.complement().complement().toPattern(false));
            }
            assertFalse("Gc=L contains 'a'", luSet.contains('1'));
            UnicodeSet casedLetter2 = new UnicodeSet("[:gc=cased letter:]");
            assertNotEquals("gc=lc should not be equal", casedLetter, casedLetter2);
        } finally {
            // restore the world
            UnicodeSet.setDefaultXSymbolTable(null);
        }
    }


    /**
     * For testing, override to set A-M to Cn.
     */
    static class MyUnicodeGCProperty extends UnicodeProperty.SimpleProperty {
        UnicodeProperty icuProperty = ICUPropertyFactory.make().getProperty("Gc");
        {
            setName(icuProperty.getName());
            setType(icuProperty.getType());
        }
        @Override
        protected String _getValue(int codepoint) {
            if (codepoint >= 'A' && codepoint <= 'M') {
                return "Unassigned";
            } else {
                return icuProperty.getValue(codepoint);
            }
        }
        @Override
        protected List _getValueAliases(String valueAlias, List result) {
            return icuProperty.getValueAliases(valueAlias, result);
        }
        @Override
        public List _getNameAliases(List result) {
            return icuProperty.getNameAliases();
        }
    }

    /**
     * For testing, override to set A-Z to Cn.
     */
    static class MyUnicodePropertyFactory extends ICUPropertyFactory {
        private MyUnicodePropertyFactory() {
            add(new MyUnicodeGCProperty());
        }
    }

    static class MyUnicodePropertySymbolTable extends UnicodePropertySymbolTable {
        public MyUnicodePropertySymbolTable(Factory factory) {
            super(factory);
        }
    }
}
