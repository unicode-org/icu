/*
 *******************************************************************************
 * Copyright (C) 2002-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public abstract class Visitor {
    
    public void doAt(Object item) {
        if (item instanceof Collection) {
            doAt((Collection) item);
        } else if (item instanceof Map) {
            doAt((Map) item);
        } else if (item instanceof Object[]) {
            doAt((Object[]) item);
        } else if (item instanceof UnicodeSet) {
            doAt((UnicodeSet) item);
        } else {
            doSimpleAt(item);
        }
    }

    public int count(Object item) {
        if (item instanceof Collection) {
            return ((Collection) item).size();
        } else if (item instanceof Map) {
            return ((Map) item).size();
        } else if (item instanceof Object[]) {
            return ((Object[]) item).length;
        } else if (item instanceof UnicodeSet) {
            return ((UnicodeSet) item).size();
        } else {
            return 1;
        }
    }

    // the default implementation boxing
    
    public void doAt(int o) {
        doSimpleAt(new Integer(o));
    }
    public void doAt(double o) {
        doSimpleAt(new Double(o));
    }
    public void doAt(char o) {
        doSimpleAt(new Character(o));
    }
    
    // for subclassing
    
    protected void doAt (Collection c) {
        if (c.size() == 0) doBefore(c, null);
        Iterator it = c.iterator();
        boolean first = true;
        Object last = null;
        while (it.hasNext()) {
            Object item = it.next();
            if (first) {
                doBefore(c, item);
                first = false;
            } else {
                doBetween(c, last, item);
            }    
            doAt(last=item);
        }
        doAfter(c, last);
    }

    protected void doAt (Map c) {
        doAt(c.entrySet());
    }

    protected void doAt (UnicodeSet c) {
        if (c.size() == 0) doBefore(c, null);
        UnicodeSetIterator it = new UnicodeSetIterator(c);
        boolean first = true;
        Object last = null;
        Object item;
        CodePointRange cpr0 = new CodePointRange();
        CodePointRange cpr1 = new CodePointRange();
        CodePointRange cpr;
        
        while(it.nextRange()) {
            if (it.codepoint == UnicodeSetIterator.IS_STRING) {
                item = it.string;
            } else {
                cpr = last == cpr0 ? cpr1 : cpr0;   // make sure we don't override last
                cpr.codepoint = it.codepoint;
                cpr.codepointEnd = it.codepointEnd;
                item = cpr;
            }           
            if (!first) {
                doBefore(c, item);
                first = true;
            } else {
                doBetween(c, last, item);
            }
            doAt(last = item);
        }
        doAfter(c, last);
    }
    
    protected void doAt (Object[] c) {
        doBefore(c, c.length == 0 ? null : c[0]);
        Object last = null;
        for (int i = 0; i < c.length; ++i) {
            if (i != 0) doBetween(c, last, c[i]);
            doAt(last = c[i]);
        }
        doAfter(c, last);
    }
    
    public static class CodePointRange{
        public int codepoint, codepointEnd;
    }
    
    // ===== MUST BE OVERRIDEN =====
    
    abstract protected void doBefore(Object container, Object item);
    abstract protected void doBetween(Object container, Object lastItem, Object nextItem);
    abstract protected void doAfter(Object container, Object item);   
    abstract protected void doSimpleAt(Object o);
    
}