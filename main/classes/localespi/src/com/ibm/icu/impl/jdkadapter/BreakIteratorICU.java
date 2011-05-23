/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.jdkadapter;

import java.text.CharacterIterator;

import com.ibm.icu.text.BreakIterator;

/**
 * BreakIteratorICU is an adapter class which wraps ICU4J BreakIterator and
 * implements java.text.BreakIterator APIs.
 */
public class BreakIteratorICU extends java.text.BreakIterator {

    private BreakIterator fIcuBrkItr;

    private BreakIteratorICU(BreakIterator icuBrkItr) {
        fIcuBrkItr = icuBrkItr;
    }

    public static java.text.BreakIterator wrap(BreakIterator icuBrkItr) {
        return new BreakIteratorICU(icuBrkItr);
    }

    public BreakIterator unwrap() {
        return fIcuBrkItr;
    }

    @Override
    public Object clone() {
        BreakIteratorICU other = (BreakIteratorICU)super.clone();
        other.fIcuBrkItr = (BreakIterator)fIcuBrkItr.clone();
        return other;
    }

    @Override
    public int current() {
        return fIcuBrkItr.current();
    }

    @Override
    public int first() {
        return fIcuBrkItr.first();
    }

    @Override
    public int following(int offset) {
        return fIcuBrkItr.following(offset);
    }

    @Override
    public CharacterIterator getText() {
        return fIcuBrkItr.getText();
    }

    @Override
    public boolean isBoundary(int offset) {
        return fIcuBrkItr.isBoundary(offset);
    }

    @Override
    public int last() {
        return fIcuBrkItr.last();
    }

    @Override
    public int next() {
        return fIcuBrkItr.next();
    }

    @Override
    public int next(int n) {
        return fIcuBrkItr.next(n);
    }

    @Override
    public int preceding(int offset) {
        return fIcuBrkItr.preceding(offset);
    }

    @Override
    public int previous() {
        return fIcuBrkItr.previous();
    }

    @Override
    public void setText(CharacterIterator newText) {
        fIcuBrkItr.setText(newText);
    }

    @Override
    public void setText(String newText) {
        fIcuBrkItr.setText(newText);
    }

}
