// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.impl;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.CSCharacterIterator;

@RunWith(JUnit4.class)
public class CSCharacterIteratorTest extends TestFmwk {
    public CSCharacterIteratorTest() {};

    @Test
    public void TestAPI() {
        String text = "Hello, World";

        CharSequence cs = text;
        CharacterIterator csci = new CSCharacterIterator(cs);
        CharacterIterator sci = new StringCharacterIterator(text);

        assertEquals("", sci.setIndex(6), csci.setIndex(6));
        assertEquals("", sci.getIndex(), csci.getIndex());
        assertEquals("", sci.current(), csci.current());
        assertEquals("", sci.previous(), csci.previous());
        assertEquals("", sci.next(), csci.next());
        assertEquals("", sci.getBeginIndex(), csci.getBeginIndex());
        assertEquals("", sci.getEndIndex(), csci.getEndIndex());
        assertEquals("", sci.first(), csci.first());
        assertEquals("", sci.last(), csci.last());

        csci.setIndex(4);
        sci.setIndex(4);
        CharacterIterator clci = (CharacterIterator)csci.clone();
        for (int i=0; i<50; ++i) {
            assertEquals("", sci.next(), clci.next());
        }
        for (int i=0; i<50; ++i) {
            assertEquals("", sci.previous(), clci.previous());
        }
    }
}
