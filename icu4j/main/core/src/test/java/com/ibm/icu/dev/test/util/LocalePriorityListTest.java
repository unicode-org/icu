// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 ******************************************************************************************
 * Copyright (C) 2009-2010, Google, Inc.; International Business Machines Corporation and *
 * others. All Rights Reserved.                                                           *
 ******************************************************************************************
 */

package com.ibm.icu.dev.test.util;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.util.LocalePriorityList;
import com.ibm.icu.util.ULocale;

/**
 * Test the LanguagePriorityList
 * @author markdavis@google.com
 */
@RunWith(JUnit4.class)
public class LocalePriorityListTest extends CoreTestFmwk {
    @Test
    public void testLanguagePriorityList() {
        final String expected = "af, en, fr";

        LocalePriorityList list = LocalePriorityList.add("af, en, fr;q=0.9").build();
        assertEquals(expected, list.toString());

        // check looseness, and that later values win
        LocalePriorityList list2 = LocalePriorityList.add(
            ", fr ; q = 0.9 ,   en;q=0.1 , af, en, de;q=0, ").build();
        assertEquals(expected, list2.toString());
        assertEquals(list, list2);

        LocalePriorityList list3 = LocalePriorityList
            .add(new ULocale("af"))
            .add(ULocale.FRENCH, 0.9d)
            .add(ULocale.ENGLISH)
            .build();
        assertEquals(expected, list3.toString());
        assertEquals(list, list3);

        LocalePriorityList list4 = LocalePriorityList.add(list).build();
        assertEquals(expected, list4.toString());
        assertEquals(list, list4);

        LocalePriorityList list5 = LocalePriorityList.add("af, fr;q=0.9, en").build(true);
        assertEquals("af, en, fr;q=0.9", list5.toString());
    }

    @Test
    public void testGetULocales() {
        LocalePriorityList list = LocalePriorityList.add("af, en, fr").build();
        Set<ULocale> locales = list.getULocales();
        assertEquals("number of locales", 3, locales.size());
        assertTrue("fr", locales.contains(ULocale.FRENCH));
    }

    @Test
    public void testIterator() {
        LocalePriorityList list = LocalePriorityList.add("af, en, fr").build();
        ULocale af = new ULocale("af");
        int count = 0;
        for (ULocale locale : list) {
            assertTrue("expected locale",
                    locale.equals(af) || locale.equals(ULocale.ENGLISH) ||
                    locale.equals(ULocale.FRENCH));
            ++count;
        }
        assertEquals("number of locales", 3, count);
    }

    @Test
    public void testQValue() {
        try {
            LocalePriorityList.add("de;q=-0.1");
            errln("negative accept-language qvalue should fail");
        } catch(IllegalArgumentException expected) {
            // good
        }
        try {
            LocalePriorityList.add("de;q=1.001");
            errln("accept-language qvalue > 1 should fail");
        } catch(IllegalArgumentException expected) {
            // good
        }
        LocalePriorityList list = LocalePriorityList.add("de;q=0.555555555").build(true);
        double weight = list.getWeight(ULocale.GERMAN);
        assertTrue("many decimals", 0.555 <= weight && weight <= 0.556);
    }

    @Test
    public void testReuse() {
        // Test reusing a Builder after build(), and some other code coverage.
        LocalePriorityList.Builder builder =
                LocalePriorityList.add("el;q=0.5, de, fr;q=0.2, el;q=0");
        LocalePriorityList list = builder.build(true);
        assertEquals("initial list", "de, fr;q=0.2", list.toString());
        list = builder.add(ULocale.FRENCH, 1.0).build(true);
        assertEquals("upgrade French", "de, fr", list.toString());
        list = builder.add(ULocale.ITALIAN, 0.1).build(true);
        assertEquals("add Italian", "de, fr, it;q=0.1", list.toString());
        builder = LocalePriorityList.add(list);
        list = builder.build(true);
        assertEquals("cloned Builder", "de, fr, it;q=0.1", list.toString());
        list = builder.add(ULocale.ITALIAN).build(true);
        assertEquals("upgrage Italian", "de, fr, it", list.toString());
        // Start over with all 1.0 weights.
        builder = LocalePriorityList.add("de, fr");
        list = builder.build(true);
        assertEquals("simple", "de, fr", list.toString());
        // Add another list.
        LocalePriorityList list2 = LocalePriorityList.add(ULocale.ITALIAN, 0.2).build(true);
        assertEquals("list2", "it;q=0.2", list2.toString());
        list = builder.add(list2).build(true);
        assertEquals("list+list2", "de, fr, it;q=0.2", list.toString());
        list = builder.add(ULocale.JAPANESE).build(true);
        assertEquals("list+list2+ja", "de, fr, ja, it;q=0.2", list.toString());
    }

    private void assertEquals(Object expected, Object string) {
        assertEquals("", expected, string);
    }
}
