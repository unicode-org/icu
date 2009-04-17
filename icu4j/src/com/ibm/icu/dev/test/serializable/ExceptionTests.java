/*
 *******************************************************************************
 * Copyright (C) 1996-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.test.serializable;

import java.util.Locale;

import com.ibm.icu.impl.IllegalIcuArgumentException;
import com.ibm.icu.impl.InvalidFormatException;
import com.ibm.icu.impl.locale.LocaleSyntaxException;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.StringPrepParseException;
import com.ibm.icu.util.IllformedLocaleException;
import com.ibm.icu.util.UResourceTypeMismatchException;

/**
 * @author emader
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ExceptionTests
{
    static abstract class ExceptionHandler implements SerializableTest.Handler
    {
        abstract public Object[] getTestObjects();
        
        public boolean hasSameBehavior(Object a, Object b)
        {
            Exception ea = (Exception) a;
            Exception eb = (Exception) b;
            
            return ea.toString().equals(eb.toString());
        }
    }
    
    static class ArabicShapingExceptionHandler extends ExceptionHandler
    {
        public Object[] getTestObjects()
        {
            Locale locales[] = SerializableTest.getLocales();
            ArabicShapingException exceptions[] = new ArabicShapingException[locales.length];
            
            for (int i = 0; i < locales.length; i += 1) {
                exceptions[i] = new ArabicShapingException(locales[i].toString());
            }
            
            return exceptions;
        }
    }
    
    static class StringPrepParseExceptionHandler extends ExceptionHandler
    {
        public Object[] getTestObjects()
        {
            Locale locales[] = SerializableTest.getLocales();
            String rules = "This is a very odd little set of rules, just for testing, you know...";
            StringPrepParseException exceptions[] = new StringPrepParseException[locales.length];
            
            for (int i = 0; i < locales.length; i += 1) {
                exceptions[i] = new StringPrepParseException(locales[i].toString(), i, rules, i);
            }
            
            return exceptions;
        }
    }
    
    static class UResourceTypeMismatchExceptionHandler extends ExceptionHandler
    {
        public Object[] getTestObjects()
        {
            Locale locales[] = SerializableTest.getLocales();
            UResourceTypeMismatchException exceptions[] = new UResourceTypeMismatchException[locales.length];
            
            for (int i = 0; i < locales.length; i += 1) {
                exceptions[i] = new UResourceTypeMismatchException(locales[i].toString());
            }
            
            return exceptions;
        }
    }
    
    static class InvalidFormatExceptionHandler extends ExceptionHandler
    {
        public Object[] getTestObjects()
        {
            Locale locales[] = SerializableTest.getLocales();
            InvalidFormatException exceptions[] = new InvalidFormatException[locales.length];
            
            for (int i = 0; i < locales.length; i += 1) {
                exceptions[i] = new InvalidFormatException(locales[i].toString());
            }
            
            return exceptions;
        }
    }

    static class IllformedLocaleExceptionHandler extends ExceptionHandler
    {
        public Object[] getTestObjects()
        {
            IllformedLocaleException[] exceptions = new IllformedLocaleException[2];
            exceptions[0] = new IllformedLocaleException("msg1");
            exceptions[1] = new IllformedLocaleException("msg2", 5);
            return exceptions;
        }
        public boolean hasSameBehavior(Object a, Object b)
        {
            IllformedLocaleException ifeA = (IllformedLocaleException) a;
            IllformedLocaleException ifeB = (IllformedLocaleException) b;
            if (ifeA.getErrorIndex() != ifeB.getErrorIndex()) {
                return false;
            }
            return super.hasSameBehavior(a, b);
        }
    }

    static class LocaleSyntaxExceptionHandler extends ExceptionHandler
    {
        public Object[] getTestObjects()
        {
            LocaleSyntaxException[] exceptions = new LocaleSyntaxException[2];
            exceptions[0] = new LocaleSyntaxException("msg1");
            exceptions[1] = new LocaleSyntaxException("msg2", 5);
            return exceptions;
        }
        public boolean hasSameBehavior(Object a, Object b)
        {
            LocaleSyntaxException ifeA = (LocaleSyntaxException) a;
            LocaleSyntaxException ifeB = (LocaleSyntaxException) b;
            if (ifeA.getErrorIndex() != ifeB.getErrorIndex()) {
                return false;
            }
            return super.hasSameBehavior(a, b);
        }
    }

    static class IllegalIcuArgumentExceptionHandler extends ExceptionHandler
    {
        public Object[] getTestObjects()
        {
            IllegalIcuArgumentException[] exceptions = {
                new IllegalIcuArgumentException("Bad argument FOO")
            };
            return exceptions;
        }
    }

    public static void main(String[] args)
    {
    }
}
