// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2004-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.ime.translit;

import java.awt.Image;
import java.awt.im.spi.InputMethod;
import java.awt.im.spi.InputMethodDescriptor;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
* The TransliteratorInputMethodDescriptor class is used to identify this package
* as an input method editor.
*/

public class TransliteratorInputMethodDescriptor implements InputMethodDescriptor {

    private ResourceBundle rb = null;

    /**
    * Creates the Transliterator IME this is automatically called by the
    * JVM when the Transliterator IME is selected from the input method list.
    *
    * @return InputMethod The Transliterator IME object.
    */
    public InputMethod createInputMethod() throws Exception {
        return new TransliteratorInputMethod();
    }
  
    /**
    * Get the list of locales that this IME supports.
    *
    * @return Locale[] This will always have one locale. By default
    *                  we just return the current locale. Therefore
    *                  the Transliterator IME works in all locales.
    */
    // use the current active locale
    public Locale[] getAvailableLocales() {
        return new Locale[] {Locale.getDefault()};
    }

    /**
    * The Transliterator IME does not support dynamic locales. The Transliterator
    * IME's functionality does not depend upon any locale.
    *
    * @return boolean This will always be false.
    */
    public boolean hasDynamicLocaleList() {
        return false;
    }

    /**
    * Obtain the localized name of the Transliterator IME
    *
    * @param inputLocale the requested input method locale
    * @param displayLanguage The requested translation of the Transliterator IME
    * @return the localized name for the Transliterator IME
    */
    public String getInputMethodDisplayName(Locale inputLocale,
                                            Locale displayLanguage) {
        String name = null;

        try {
            rb = ResourceBundle.getBundle("com.ibm.icu.dev.tool.ime.translit.Transliterator", displayLanguage);
            name = rb.getString("name");
        }
        catch (MissingResourceException m) {
            // use a hardcoded value
            name = "Transliterator";
        }
        return name;
    }

    /**
    * Get the icon for the Transliterator IME. This is not supported.
    *
    * @param inputLocale (This is ignored).
    *
    * @return Image This will always be null.
    */
    public Image getInputMethodIcon(Locale inputLocale) {
       return null;
    }
}

