// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.impl;

import static org.junit.Assert.*;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.ICUResourceBundle.OpenType;
import com.ibm.icu.util.ULocale;
import java.util.MissingResourceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests that bundle lookup fails with a controlled {@link MissingResourceException} rather than a
 * {@link StackOverflowError} when no bundle data can be found at all and the default locale is
 * {@link ULocale#ROOT}.
 *
 * <p>When the default locale is ROOT, {@code ICUResourceBundle.instantiateBundle()} gets a blank
 * {@code defaultID}. If the requested bundle is missing, the "go to the default locale before root"
 * fallback step used to re-enter itself with identical arguments (localeID == defaultID == "" and
 * no originating locale) until the stack was exhausted (ICU-23291).
 */
@RunWith(JUnit4.class)
public class ICUResourceBundleFallbackTest {

    private ULocale withDefaultLocale(ULocale newDefault) {
        ULocale saved = ULocale.getDefault();
        ULocale.setDefault(newDefault);
        return saved;
    }

    /**
     * With a ROOT default (blank defaultID) and no bundle data at all, lookup must fail with
     * MissingResourceException. Before the ICU-23291 fix this recursed until StackOverflowError.
     */
    @Test
    public void testMissingBundleWithRootDefaultFailsGracefully() {
        ULocale saved = withDefaultLocale(ULocale.ROOT);
        try {
            assertThrows(
                    MissingResourceException.class,
                    () ->
                            ICUResourceBundle.getBundleInstance(
                                    "com/ibm/icu/impl/data/icudata/no_such_bundle",
                                    "",
                                    ICUResourceBundle.ICU_DATA_CLASS_LOADER,
                                    OpenType.LOCALE_DEFAULT_ROOT));
        } finally {
            ULocale.setDefault(saved);
        }
    }

    /**
     * Same for a request for a specific locale of missing bundle data (for example a slimmed-down
     * data directory): the fallback chain must end at root and then fail cleanly.
     */
    @Test
    public void testMissingTreeWithRootDefaultFailsGracefully() {
        ULocale saved = withDefaultLocale(ULocale.ROOT);
        try {
            assertThrows(
                    MissingResourceException.class,
                    () ->
                            ICUResourceBundle.getBundleInstance(
                                    "com/ibm/icu/impl/data/icudata/no_such_bundle",
                                    "zz_ZZ",
                                    ICUResourceBundle.ICU_DATA_CLASS_LOADER,
                                    OpenType.LOCALE_DEFAULT_ROOT));
        } finally {
            ULocale.setDefault(saved);
        }
    }

    /**
     * Control: normal fallback is unchanged. With a ROOT default, a locale without data still falls
     * back to the root bundle.
     */
    @Test
    public void testFallbackToRootStillWorksWithRootDefault() {
        ULocale saved = withDefaultLocale(ULocale.ROOT);
        try {
            ICUResourceBundle b =
                    ICUResourceBundle.getBundleInstance(
                            ICUData.ICU_BASE_NAME,
                            "zz_ZZ",
                            ICUResourceBundle.ICU_DATA_CLASS_LOADER,
                            OpenType.LOCALE_DEFAULT_ROOT);
            assertNotNull("fallback to root bundle must still work", b);
            // the root bundle of the ICU data tree is returned for an unknown locale
            assertNotNull(b.getULocale());
        } finally {
            ULocale.setDefault(saved);
        }
    }

    /** Control: a normal resource load is unchanged. */
    @Test
    public void testNormalLoadStillWorks() {
        ICUResourceBundle b =
                ICUResourceBundle.getBundleInstance(
                        ICUData.ICU_BASE_NAME,
                        "root",
                        ICUResourceBundle.ICU_DATA_CLASS_LOADER,
                        OpenType.DIRECT);
        assertNotNull(b);
        ICUResourceBundle keyTypeData =
                ICUResourceBundle.getBundleInstance(
                        ICUData.ICU_BASE_NAME,
                        "keyTypeData",
                        ICUResourceBundle.ICU_DATA_CLASS_LOADER,
                        OpenType.DIRECT);
        assertNotNull(keyTypeData);
        assertNotNull(keyTypeData.get("keyInfo"));
    }

    /**
     * Control: with a non-ROOT default a fully missing bundle already failed cleanly, and must
     * still do so.
     */
    @Test
    public void testMissingBundleWithNonRootDefaultFailsGracefully() {
        ULocale saved = withDefaultLocale(ULocale.forLocale(java.util.Locale.US));
        try {
            assertThrows(
                    MissingResourceException.class,
                    () ->
                            ICUResourceBundle.getBundleInstance(
                                    "com/ibm/icu/impl/data/icudata/no_such_bundle",
                                    "",
                                    ICUResourceBundle.ICU_DATA_CLASS_LOADER,
                                    OpenType.LOCALE_DEFAULT_ROOT));
        } finally {
            ULocale.setDefault(saved);
        }
    }
}
