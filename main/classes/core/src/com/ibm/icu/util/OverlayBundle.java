/**
 *******************************************************************************
 * Copyright (C) 2001-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.util.Enumeration;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * A ResourceBundle that overlays one hierarchy atop another.  This is
 * best explained by example.  Suppose one wants to use the
 * resource hiararchy (in JDK 1.2 and 1.3, but not 1.4) at 
 * "java.text.resources.LocaleElements", but one wants to use
 * a modified version of the "NumberPatterns" resource in the
 * fr_FR locale.  One way to do this is to add special case code
 * to the lookup operation to check for fr_FR and the key
 * "NumberPatterns", and in that case, load up custom data.  However,
 * this becomes unwieldy and places some information about the
 * effective resource hierarchy into the code.
 *
 * The OverlayBundle solves this problem by layering another
 * hierarchy, e.g, "com.acme.resources.LocaleElements", on top of a
 * base hierarchy.  When a resource is requested, it is first sought
 * in the overlay hierarchy, and if not found there, it is sought in
 * the base hierarchy.  Multiple overlays are supported, but in
 * practice one is usually sufficient.
 * 
 * The OverlayBundle also addresses the problem of country-oriented
 * data.  To specify the default data for a language, one just sets
 * the language resource bundle data.  However, specifying the default
 * data for a country using the standard ResourceBundle mechanism is
 * impossible.  The OverlayBundle recognizes "wildcard" locales with
 * the special language code "xx".  When looking up data for a locale
 * with a non-empty country, if an exact locale match cannot be found,
 * the OverlayBundle looks for data in the locale xx_YY, where YY is
 * the country being sought.  This effectively adds another entry in
 * the fallback sequence for a locale aa_BB: aa_BB, xx_BB, aa, root.
 * Wildcard locales are not implemented for the base hierarchy, only
 * for overlays.
 *
 * The OverlayBundle is implemented as an array of n ResourceBundle
 * base names.  The base names are searched from 0 to n-1.  Base name
 * n-1 is special; it is the base hierarchy.  This should be a
 * well-populated hierarchy with most of the default data, typically,
 * the icu or sun core hierarchies.  The base hierarchy is
 * treated differently from the overlays above it.  It does not get
 * wildcard resolution, and the getKeys() framework method is
 * delegated to the base hierarchy bundle.
 *
 * Usage: Instantiate an OverlayBundle directly (not via a factory
 * method as in ResourceBundle).  Instead of specifying a single base
 * name, pass it an array of 2 or more base names.  After that, use it
 * exactly as you would use ResourceBundle.
 *
 * @see java.util.ResourceBundle
 * @author Alan Liu
 * @internal
 * @deprecated ICU 2.4. This class may be removed or modified.
 */
// prepare to deprecate in next release
///CLOVER:OFF
public class OverlayBundle extends ResourceBundle {

    /**
     * The array of base names, with the length-1 entry being the base
     * hierarchy, typically "sun.text.resources.LocaleElements".
     */
    private String[] baseNames;

    /**
     * The requested locale.
     */
    private Locale locale;

    /**
     * Loaded bundles.  These will be null until they are loaded on
     * demand.
     */
    private ResourceBundle[] bundles;

    /**
     * Construct an overlay bundle given a sequence of base names and
     * a locale.
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    public OverlayBundle(String[] baseNames,
                         Locale locale) {
        this.baseNames = baseNames;
        this.locale = locale;
        bundles = new ResourceBundle[baseNames.length];
    }

    /**
     * ResourceBundle framework method.  Delegates to
     * bundles[i].getObject().
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */ 
   protected Object handleGetObject(String key) 
        throws MissingResourceException {

        Object o = null;

        for (int i=0; i<bundles.length; ++i) {
            load(i);
            try {
                o = bundles[i].getObject(key);
            } catch (MissingResourceException e) {
                if (i == bundles.length-1) {
                    throw e;
                }
            }
            if (o != null) {
                break;
            }
        }

        return o;
    }

    /**
     * ResourceBundle framework method.  Delegates to
     * bundles[bundles.length-1].getKeys().
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    public Enumeration<String> getKeys() {
        // Return the enumeration of the last bundle, which is the base
        // of our hierarchy stack.
        int i = bundles.length - 1;
        load(i);
        return bundles[i].getKeys();
    }

    /**
     * Load the i-th bundle and implement wildcard resolution.
     */
    private void load(int i)
        throws MissingResourceException {

        if (bundles[i] == null) {
            boolean tryWildcard = false;
            try {
                bundles[i] = ResourceBundle.getBundle(baseNames[i], locale);
                if (bundles[i].getLocale().equals(locale)) {
                    return;
                }
                if (locale.getCountry().length() != 0 && i != bundles.length-1) {
                    tryWildcard = true;
                }
            } catch (MissingResourceException e) {
                if (i == bundles.length-1) {
                    throw e;
                }
                tryWildcard = true;
            }
            if (tryWildcard) {
                Locale wildcard = new Locale("xx", locale.getCountry(),
                                             locale.getVariant());
                try {
                    bundles[i] = ResourceBundle.getBundle(baseNames[i], wildcard);
                } catch (MissingResourceException e) {
                    if (bundles[i] == null) {
                        throw e;
                    }
                }
            }
        }
    }
}
