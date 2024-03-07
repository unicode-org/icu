// © 2021 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test for {@link UnicodeKnownIssues}
 */
@RunWith(JUnit4.class)
public class TestUnicodeKnownIssues {
    @Test
    public void TestBasic() {
        UnicodeKnownIssues uki = new UnicodeKnownIssues(true);
        uki.logKnownIssue("a/b/c", "ICU-21756", "Something is working!");
        List<String> l = printToList(uki);
        assertNotNull("no list", l);
        assertEquals("message count Of Three", 3, l.size());
        assertTrue(l.get(0) + "#0 says Known Issues", l.get(0).contains("Known Issues"));
        assertTrue(l.get(1) + "#1 has atlassian URL", l.get(1).contains("browse/ICU-21756"));
        assertTrue(l.get(2) + "#2 says a/b/c", l.get(2).contains("a/b/c"));
    }

    @Test
    public void TestNotCurtailed() {
        UnicodeKnownIssues uki = new UnicodeKnownIssues(true);
        uki.logKnownIssue("a/b/c", "ICU-21756", "Something is working!");
        uki.logKnownIssue("d/e/f", "ICU-21756", "Something is working!");
        uki.logKnownIssue("g/h/i", "ICU-21756", "Something is working!");
        List<String> l = printToList(uki);
        assertNotNull("no list", l);
        assertEquals("message count Of 5", 5, l.size());
        assertTrue(l.get(0) + "#0 says Known Issues", l.get(0).contains("Known Issues"));
        assertTrue(l.get(1) + "#1 has atlassian URL", l.get(1).contains("browse/ICU-21756"));
        // Not curtailed: test shows up in a/b/c, d/e/f, and g/h/i
        assertTrue(l.get(2) + "#2 says a/b/c", l.get(2).contains("a/b/c"));
        assertTrue(l.get(3) + "#3 says d/e/f", l.get(3).contains("d/e/f"));
        assertTrue(l.get(4) + "#4 says g/h/i", l.get(4).contains("g/h/i"));
    }

    @Test
    public void TestCurtailed() {
        UnicodeKnownIssues uki = new UnicodeKnownIssues(false);
        uki.logKnownIssue("a/b/c", "ICU-21756", "Something is working!");
        uki.logKnownIssue("d/e/f", "ICU-21756", "Something is working!");
        uki.logKnownIssue("g/h/i", "ICU-21756", "Something is working!");
        List<String> l = printToList(uki);
        assertNotNull("no list", l);
        assertEquals("message count", 4, l.size());
        assertTrue(l.get(0) + "#0 says Known Issues", l.get(0).contains("Known Issues"));
        assertTrue(l.get(1) + "#1 has atlassian URL", l.get(1).contains("browse/ICU-21756"));
        assertTrue(l.get(2) + "#2 says a/b/c", l.get(2).contains("a/b/c"));
        // Curtailed: the next line has "... and 2 more"
        assertTrue(l.get(3) + "#3 has 'and 2 more'", l.get(3).contains("and 2 more"));
    }

    @Test
    public void TestBare() {
        UnicodeKnownIssues uki = new UnicodeKnownIssues(true);
        uki.logKnownIssue("a/b/c", "21756", "Something is working!");
        List<String> l = printToList(uki);
        assertNotNull("no list", l);
        assertEquals("message count Of Three", 3, l.size());
        assertTrue(l.get(0) + "#0 says Known Issues", l.get(0).contains("Known Issues"));
        assertTrue(l.get(1) + "#1 has atlassian URL", l.get(1).contains("browse/ICU-21756"));
        assertTrue(l.get(2) + "#2 says a/b/c", l.get(2).contains("a/b/c"));
    }

    @Test
    public void TestUnknown() {
        UnicodeKnownIssues uki = new UnicodeKnownIssues(true);
        uki.logKnownIssue("a/b/c", "zzz", "Something is working!");
        List<String> l = printToList(uki);
        assertNotNull("no list", l);
        assertEquals("message count Of Three", 3, l.size());
        assertTrue(l.get(0) + "#0 says Known Issues", l.get(0).contains("Known Issues"));
        assertTrue(l.get(1) + "#1 has unknown ticket", l.get(1).contains("Unknown Ticket"));
        assertTrue(l.get(2) + "#2 says a/b/c", l.get(2).contains("a/b/c"));
    }

    @Test
    public void TestCldrLink() {
        UnicodeKnownIssues uki = new UnicodeKnownIssues(true);
        uki.logKnownIssue("a/b/c", "CLDR-9787", "Something is working!");
        List<String> l = printToList(uki);
        assertNotNull("no list", l);
        assertEquals("message count Of Three", 3, l.size());
        assertTrue(l.get(0) + "#0 says Known Issues", l.get(0).contains("Known Issues"));
        assertTrue(l.get(1) + "#1 has atlassian URL", l.get(1).contains("browse/CLDR-9787"));
        assertTrue(l.get(2) + "#2 says a/b/c", l.get(2).contains("a/b/c"));
    }

    @Test
    public void TestCldrBug() {
        UnicodeKnownIssues uki = new UnicodeKnownIssues(true);
        uki.logKnownIssue("a/b/c", "cldrbug:9787", "Something is working!");
        List<String> l = printToList(uki);
        assertNotNull("no list", l);
        assertEquals("message count Of Three", 3, l.size());
        assertTrue(l.get(0) + "#0 says Known Issues", l.get(0).contains("Known Issues"));
        assertTrue(l.get(1) + "#1 has atlassian URL", l.get(1).contains("browse/CLDR-9787"));
        assertTrue(l.get(2) + "#2 says a/b/c", l.get(2).contains("a/b/c"));
    }


    @Test
    public void TestNoProblem() {
        UnicodeKnownIssues uki = new UnicodeKnownIssues(true);
        List<String> l = printToList(uki);
        assertNotNull("no list", l);
        assertEquals("message count Of Zero", 0, l.size());
    }

    List<String> printToList(UnicodeKnownIssues uki) {
        // TODO: for JDK 1.8
        // final List<String> l = new LinkedList<>();
        // uki.printKnownIssues(s -> l.add(s));

        // TODO: Pre JDK 1.8 below
        MyConsumer m = new MyConsumer();
        uki.printKnownIssues(m);
        return m.l;
    }

    // TODO: remove for JDK 1.8
    static final class MyConsumer implements UnicodeKnownIssues.Consumer<String> {
        final List<String> l = new LinkedList<>();
        @Override
        public void accept(String t) {
            l.add(t);
        }
    }
}
