// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.ime.indic;

import java.awt.AWTEvent;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.im.spi.InputMethod;
import java.awt.im.spi.InputMethodContext;
import java.lang.Character.Subset;
import java.util.Locale;

/**
 * This stub delegates to the simpler IndicInputMethodImpl.
 */
class IndicInputMethod implements InputMethod {
    private IndicInputMethodImpl impl;
    private Locale locale;
    
    IndicInputMethod(Locale theLocale, IndicInputMethodImpl theImplementation) {
        locale = theLocale;
        impl = theImplementation;
    }
    
    public void setInputMethodContext(InputMethodContext context) {
        impl.setInputMethodContext(context);
    }

    public boolean setLocale(Locale locale) {
        return locale.getLanguage().equals(this.locale.getLanguage());
    }

    public Locale getLocale() {
        return locale;
    }
    
    public void setCharacterSubsets(Subset[] subsets) {
    }

    public void setCompositionEnabled(boolean enable) {
        throw new UnsupportedOperationException();
    }

    public boolean isCompositionEnabled() {
        return true;
    }

    public void reconvert() {
        throw new UnsupportedOperationException("This input method does not reconvert.");
    }

    public void dispatchEvent(AWTEvent event) {
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) event;
            if (event.getID() == KeyEvent.KEY_TYPED) {
                impl.handleKeyTyped(keyEvent);
            }

        }
    }

    public void notifyClientWindowChange(Rectangle bounds) {
    }

    public void activate() {
    }

    public void deactivate(boolean isTemporary) {
    }

    public void hideWindows() {
    }
  
    public void removeNotify() {
    }

    public void endComposition() {
        impl.endComposition();
    }

    public void dispose() {
    }

    public Object getControlObject() {
        return null;
    }
}
