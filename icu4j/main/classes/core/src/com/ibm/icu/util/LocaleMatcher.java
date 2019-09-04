// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
 ****************************************************************************************
 * Copyright (C) 2009-2016, Google, Inc.; International Business Machines Corporation
 * and others. All Rights Reserved.
 ****************************************************************************************
 */
package com.ibm.icu.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.ibm.icu.impl.locale.LSR;
import com.ibm.icu.impl.locale.LocaleDistance;
import com.ibm.icu.impl.locale.XLikelySubtags;

/**
 * Immutable class that picks the best match between a user's desired locales and
 * an application's supported locales.
 *
 * <p>Example:
 * <pre>
 * LocaleMatcher matcher = LocaleMatcher.builder().setSupportedLocales("fr, en-GB, en").build();
 * Locale bestSupported = matcher.getBestLocale(Locale.US);  // "en"
 * </pre>
 *
 * <p>A matcher takes into account when languages are close to one another,
 * such as Danish and Norwegian,
 * and when regional variants are close, like en-GB and en-AU as opposed to en-US.
 *
 * <p>If there are multiple supported locales with the same (language, script, region)
 * likely subtags, then the current implementation returns the first of those locales.
 * It ignores variant subtags (except for pseudolocale variants) and extensions.
 * This may change in future versions.
 *
 * <p>For example, the current implementation does not distinguish between
 * de, de-DE, de-Latn, de-1901, de-u-co-phonebk.
 *
 * <p>If you prefer one equivalent locale over another, then provide only the preferred one,
 * or place it earlier in the list of supported locales.
 *
 * <p>Otherwise, the order of supported locales may have no effect on the best-match results.
 * The current implementation compares each desired locale with supported locales
 * in the following order:
 * 1. Default locale, if supported;
 * 2. CLDR "paradigm locales" like en-GB and es-419;
 * 3. other supported locales.
 * This may change in future versions.
 *
 * <p>Often a product will just need one matcher instance, built with the languages
 * that it supports. However, it may want multiple instances with different
 * default languages based on additional information, such as the domain.
 *
 * <p>This class is not intended for public subclassing.
 *
 * @author markdavis@google.com
 * @stable ICU 4.4
 */
public final class LocaleMatcher {
    private static final LSR UND_LSR = new LSR("und","","");
    // In ULocale, "und" and "" make the same object.
    private static final ULocale UND_ULOCALE = new ULocale("und");
    // In Locale, "und" and "" make different objects.
    private static final Locale UND_LOCALE = new Locale("und");
    private static final Locale EMPTY_LOCALE = new Locale("");

    // Activates debugging output to stderr with details of GetBestMatch.
    private static final boolean TRACE_MATCHER = false;

    private static abstract class LsrIterator implements Iterator<LSR> {
        int bestDesiredIndex = -1;

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        public abstract void rememberCurrent(int desiredIndex);
    }

    /**
     * Builder option for whether the language subtag or the script subtag is most important.
     *
     * @see LocaleMatcher.Builder#setFavorSubtag(LocaleMatcher.FavorSubtag)
     * @draft ICU 65
     * @provisional This API might change or be removed in a future release.
     */
    public enum FavorSubtag {
        /**
         * Language differences are most important, then script differences, then region differences.
         * (This is the default behavior.)
         *
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        LANGUAGE,
        /**
         * Makes script differences matter relatively more than language differences.
         *
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        SCRIPT
    }

    /**
     * Builder option for whether all desired locales are treated equally or
     * earlier ones are preferred.
     *
     * @see LocaleMatcher.Builder#setDemotionPerDesiredLocale(LocaleMatcher.Demotion)
     * @draft ICU 65
     * @provisional This API might change or be removed in a future release.
     */
    public enum Demotion {
        /**
         * All desired locales are treated equally.
         *
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        NONE,
        /**
         * Earlier desired locales are preferred.
         *
         * <p>From each desired locale to the next,
         * the distance to any supported locale is increased by an additional amount
         * which is at least as large as most region mismatches.
         * A later desired locale has to have a better match with some supported locale
         * due to more than merely having the same region subtag.
         *
         * <p>For example: <code>Supported={en, sv}  desired=[en-GB, sv]</code>
         * yields <code>Result(en-GB, en)</code> because
         * with the demotion of sv its perfect match is no better than
         * the region distance between the earlier desired locale en-GB and en=en-US.
         *
         * <p>Notes:
         * <ul>
         *   <li>In some cases, language and/or script differences can be as small as
         *       the typical region difference. (Example: sr-Latn vs. sr-Cyrl)
         *   <li>It is possible for certain region differences to be larger than usual,
         *       and larger than the demotion.
         *       (As of CLDR 35 there is no such case, but
         *        this is possible in future versions of the data.)
         * </ul>
         *
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        REGION
    }

    /**
     * Data for the best-matching pair of a desired and a supported locale.
     *
     * @draft ICU 65
     * @provisional This API might change or be removed in a future release.
     */
    public static final class Result {
        private final ULocale desiredULocale;
        private final ULocale supportedULocale;
        private final Locale desiredLocale;
        private final Locale supportedLocale;
        private final int desiredIndex;
        private final int supportedIndex;

        private Result(ULocale udesired, ULocale usupported,
                Locale desired, Locale supported,
                int desIndex, int suppIndex) {
            desiredULocale = udesired;
            supportedULocale = usupported;
            desiredLocale = desired;
            supportedLocale = supported;
            desiredIndex = desIndex;
            supportedIndex = suppIndex;
        }

        /**
         * Returns the best-matching desired locale.
         * null if the list of desired locales is empty or if none matched well enough.
         *
         * @return the best-matching desired locale, or null.
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public ULocale getDesiredULocale() {
            return desiredULocale == null && desiredLocale != null ?
                    ULocale.forLocale(desiredLocale) : desiredULocale;
        }
        /**
         * Returns the best-matching desired locale.
         * null if the list of desired locales is empty or if none matched well enough.
         *
         * @return the best-matching desired locale, or null.
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public Locale getDesiredLocale() {
            return desiredLocale == null && desiredULocale != null ?
                    desiredULocale.toLocale() : desiredLocale;
        }

        /**
         * Returns the best-matching supported locale.
         * If none matched well enough, this is the default locale.
         * The default locale is null if the list of supported locales is empty and
         * no explicit default locale is set.
         *
         * @return the best-matching supported locale, or null.
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public ULocale getSupportedULocale() { return supportedULocale; }
        /**
         * Returns the best-matching supported locale.
         * If none matched well enough, this is the default locale.
         * The default locale is null if the list of supported locales is empty and
         * no explicit default locale is set.
         *
         * @return the best-matching supported locale, or null.
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public Locale getSupportedLocale() { return supportedLocale; }

        /**
         * Returns the index of the best-matching desired locale in the input Iterable order.
         * -1 if the list of desired locales is empty or if none matched well enough.
         *
         * @return the index of the best-matching desired locale, or -1.
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public int getDesiredIndex() { return desiredIndex; }

        /**
         * Returns the index of the best-matching supported locale in the
         * constructor’s or builder’s input order (“set” Collection plus “added” locales).
         * If the matcher was built from a locale list string, then the iteration order is that
         * of a LocalePriorityList built from the same string.
         * -1 if the list of supported locales is empty or if none matched well enough.
         *
         * @return the index of the best-matching supported locale, or -1.
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public int getSupportedIndex() { return supportedIndex; }

        /**
         * Takes the best-matching supported locale and adds relevant fields of the
         * best-matching desired locale, such as the -t- and -u- extensions.
         * May replace some fields of the supported locale.
         * The result is the locale that should be used for date and number formatting, collation, etc.
         * Returns null if getSupportedLocale() returns null.
         *
         * <p>Example: desired=ar-SA-u-nu-latn, supported=ar-EG, resolved locale=ar-SA-u-nu-latn
         *
         * @return a locale combining the best-matching desired and supported locales.
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public ULocale makeResolvedULocale() {
            ULocale bestDesired = getDesiredULocale();
            if (supportedULocale == null || bestDesired == null ||
                    supportedULocale.equals(bestDesired)) {
                return supportedULocale;
            }
            ULocale.Builder b = new ULocale.Builder().setLocale(supportedULocale);

            // Copy the region from bestDesired, if there is one.
            String region = bestDesired.getCountry();
            if (!region.isEmpty()) {
                b.setRegion(region);
            }

            // Copy the variants from bestDesired, if there are any.
            // Note that this will override any supportedULocale variants.
            // For example, "sco-ulster-fonipa" + "...-fonupa" => "sco-fonupa" (replacing ulster).
            String variants = bestDesired.getVariant();
            if (!variants.isEmpty()) {
                b.setVariant(variants);
            }

            // Copy the extensions from bestDesired, if there are any.
            // Note that this will override any supportedULocale extensions.
            // For example, "th-u-nu-latn-ca-buddhist" + "...-u-nu-native" => "th-u-nu-native"
            // (replacing calendar).
            for (char extensionKey : bestDesired.getExtensionKeys()) {
                b.setExtension(extensionKey, bestDesired.getExtension(extensionKey));
            }
            return b.build();
        }

        /**
         * Takes the best-matching supported locale and adds relevant fields of the
         * best-matching desired locale, such as the -t- and -u- extensions.
         * May replace some fields of the supported locale.
         * The result is the locale that should be used for
         * date and number formatting, collation, etc.
         * Returns null if getSupportedLocale() returns null.
         *
         * <p>Example: desired=ar-SA-u-nu-latn, supported=ar-EG, resolved locale=ar-SA-u-nu-latn
         *
         * @return a locale combining the best-matching desired and supported locales.
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public Locale makeResolvedLocale() {
            ULocale resolved = makeResolvedULocale();
            return resolved != null ? resolved.toLocale() : null;
        }
    }

    private final int thresholdDistance;
    private final int demotionPerDesiredLocale;
    private final FavorSubtag favorSubtag;

    // These are in input order.
    private final ULocale[] supportedULocales;
    private final Locale[] supportedLocales;
    // These are in preference order: 1. Default locale 2. paradigm locales 3. others.
    private final Map<LSR, Integer> supportedLsrToIndex;
    // Array versions of the supportedLsrToIndex keys and values.
    // The distance lookup loops over the supportedLSRs and returns the index of the best match.
    private final LSR[] supportedLSRs;
    private final int[] supportedIndexes;
    private final ULocale defaultULocale;
    private final Locale defaultLocale;
    private final int defaultLocaleIndex;

    /**
     * LocaleMatcher Builder.
     *
     * @see LocaleMatcher#builder()
     * @draft ICU 65
     * @provisional This API might change or be removed in a future release.
     */
    public static final class Builder {
        private List<ULocale> supportedLocales;
        private int thresholdDistance = -1;
        private Demotion demotion;
        private ULocale defaultLocale;
        private FavorSubtag favor;

        private Builder() {}

        /**
         * Parses the string like {@link LocalePriorityList} does and
         * sets the supported locales accordingly.
         * Clears any previously set/added supported locales first.
         *
         * @param locales the string of locales to set, to be parsed like LocalePriorityList does
         * @return this Builder object
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setSupportedLocales(String locales) {
            return setSupportedULocales(LocalePriorityList.add(locales).build().getULocales());
        }

        /**
         * Copies the supported locales, preserving iteration order.
         * Clears any previously set/added supported locales first.
         * Duplicates are allowed, and are not removed.
         *
         * @param locales the list of locales
         * @return this Builder object
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setSupportedULocales(Collection<ULocale> locales) {
            supportedLocales = new ArrayList<>(locales);
            return this;
        }

        /**
         * Copies the supported locales, preserving iteration order.
         * Clears any previously set/added supported locales first.
         * Duplicates are allowed, and are not removed.
         *
         * @param locales the list of locale
         * @return this Builder object
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setSupportedLocales(Collection<Locale> locales) {
            supportedLocales = new ArrayList<>(locales.size());
            for (Locale locale : locales) {
                supportedLocales.add(ULocale.forLocale(locale));
            }
            return this;
        }

        /**
         * Adds another supported locale.
         * Duplicates are allowed, and are not removed.
         *
         * @param locale another locale
         * @return this Builder object
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public Builder addSupportedULocale(ULocale locale) {
            if (supportedLocales == null) {
                supportedLocales = new ArrayList<>();
            }
            supportedLocales.add(locale);
            return this;
        }

        /**
         * Adds another supported locale.
         * Duplicates are allowed, and are not removed.
         *
         * @param locale another locale
         * @return this Builder object
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public Builder addSupportedLocale(Locale locale) {
            return addSupportedULocale(ULocale.forLocale(locale));
        }

        /**
         * Sets the default locale; if null, or if it is not set explicitly,
         * then the first supported locale is used as the default locale.
         *
         * @param defaultLocale the default locale
         * @return this Builder object
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setDefaultULocale(ULocale defaultLocale) {
            this.defaultLocale = defaultLocale;
            return this;
        }

        /**
         * Sets the default locale; if null, or if it is not set explicitly,
         * then the first supported locale is used as the default locale.
         *
         * @param defaultLocale the default locale
         * @return this Builder object
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setDefaultLocale(Locale defaultLocale) {
            this.defaultLocale = ULocale.forLocale(defaultLocale);
            return this;
        }

        /**
         * If SCRIPT, then the language differences are smaller than script differences.
         * This is used in situations (such as maps) where
         * it is better to fall back to the same script than a similar language.
         *
         * @param subtag the subtag to favor
         * @return this Builder object
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setFavorSubtag(FavorSubtag subtag) {
            this.favor = subtag;
            return this;
        }

        /**
         * Option for whether all desired locales are treated equally or
         * earlier ones are preferred (this is the default).
         *
         * @param demotion the demotion per desired locale to set.
         * @return this Builder object
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public Builder setDemotionPerDesiredLocale(Demotion demotion) {
            this.demotion = demotion;
            return this;
        }

        /**
         * <i>Internal only!</i>
         *
         * @param thresholdDistance the thresholdDistance to set, with -1 = default
         * @return this Builder object
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        public Builder internalSetThresholdDistance(int thresholdDistance) {
            if (thresholdDistance > 100) {
                thresholdDistance = 100;
            }
            this.thresholdDistance = thresholdDistance;
            return this;
        }

        /**
         * Builds and returns a new locale matcher.
         * This builder can continue to be used.
         *
         * @return new LocaleMatcher.
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        public LocaleMatcher build() {
            return new LocaleMatcher(this);
        }

        /**
         * {@inheritDoc}
         * @draft ICU 65
         * @provisional This API might change or be removed in a future release.
         */
        @Override
        public String toString() {
            StringBuilder s = new StringBuilder().append("{LocaleMatcher.Builder");
            if (supportedLocales != null && !supportedLocales.isEmpty()) {
                s.append(" supported={").append(supportedLocales.toString()).append('}');
            }
            if (defaultLocale != null) {
                s.append(" default=").append(defaultLocale.toString());
            }
            if (favor != null) {
                s.append(" distance=").append(favor.toString());
            }
            if (thresholdDistance >= 0) {
                s.append(String.format(" threshold=%d", thresholdDistance));
            }
            if (demotion != null) {
                s.append(" demotion=").append(demotion.toString());
            }
            return s.append('}').toString();
        }
    }

    /**
     * Returns a builder used in chaining parameters for building a LocaleMatcher.
     *
     * @return a new Builder object
     * @draft ICU 65
     * @provisional This API might change or be removed in a future release.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Copies the supported locales, preserving iteration order, and constructs a LocaleMatcher.
     * The first locale is used as the default locale for when there is no good match.
     *
     * @param supportedLocales list of locales
     * @stable ICU 4.4
     */
    public LocaleMatcher(LocalePriorityList supportedLocales) {
        this(builder().setSupportedULocales(supportedLocales.getULocales()));
    }

    /**
     * Parses the string like {@link LocalePriorityList} does and
     * constructs a LocaleMatcher for the supported locales parsed from the string.
     * The first one (in LocalePriorityList iteration order) is used as the default locale for
     * when there is no good match.
     *
     * @param supportedLocales the string of locales to set,
     *          to be parsed like LocalePriorityList does
     * @stable ICU 4.4
     */
    public LocaleMatcher(String supportedLocales) {
        this(builder().setSupportedLocales(supportedLocales));
    }

    private LocaleMatcher(Builder builder) {
        thresholdDistance = builder.thresholdDistance < 0 ?
                LocaleDistance.INSTANCE.getDefaultScriptDistance() : builder.thresholdDistance;
        int supportedLocalesLength = builder.supportedLocales != null ?
                builder.supportedLocales.size() : 0;
        ULocale udef = builder.defaultLocale;
        Locale def = null;
        int idef = -1;
        // Store the supported locales in input order,
        // so that when different types are used (e.g., java.util.Locale)
        // we can return those by parallel index.
        supportedULocales = new ULocale[supportedLocalesLength];
        supportedLocales = new Locale[supportedLocalesLength];
        // Supported LRSs in input order.
        LSR lsrs[] = new LSR[supportedLocalesLength];
        // Also find the first supported locale whose LSR is
        // the same as that for the default locale.
        LSR defLSR = null;
        if (udef != null) {
            def = udef.toLocale();
            defLSR = getMaximalLsrOrUnd(udef);
        }
        int i = 0;
        if (supportedLocalesLength > 0) {
            for (ULocale locale : builder.supportedLocales) {
                supportedULocales[i] = locale;
                supportedLocales[i] = locale.toLocale();
                LSR lsr = lsrs[i] = getMaximalLsrOrUnd(locale);
                if (idef < 0 && defLSR != null && lsr.equals(defLSR)) {
                    idef = i;
                }
                ++i;
            }
        }

        // We need an unordered map from LSR to first supported locale with that LSR,
        // and an ordered list of (LSR, supported index).
        // We use a LinkedHashMap for both,
        // and insert the supported locales in the following order:
        // 1. Default locale, if it is supported.
        // 2. Priority locales (aka "paradigm locales") in builder order.
        // 3. Remaining locales in builder order.
        supportedLsrToIndex = new LinkedHashMap<>(supportedLocalesLength);
        // Note: We could work with a single LinkedHashMap by storing ~i (the binary-not index)
        // for the default and paradigm locales, counting the number of those locales,
        // and keeping two indexes to fill the LSR and index arrays with
        // priority vs. normal locales. In that loop we would need to entry.setValue(~i)
        // to restore non-negative indexes in the map.
        // Probably saves little but less readable.
        Map<LSR, Integer> otherLsrToIndex = null;
        if (idef >= 0) {
            supportedLsrToIndex.put(defLSR, idef);
        }
        i = 0;
        for (ULocale locale : supportedULocales) {
            if (i == idef) {
                ++i;
                continue;
            }
            LSR lsr = lsrs[i];
            if (defLSR == null) {
                assert i == 0;
                udef = locale;
                def = supportedLocales[0];
                defLSR = lsr;
                idef = 0;
                supportedLsrToIndex.put(lsr, 0);
            } else if (idef >= 0 && lsr.equals(defLSR)) {
                // lsr.equals(defLSR) means that this supported locale is
                // a duplicate of the default locale.
                // Either an explicit default locale is supported, and we added it before the loop,
                // or there is no explicit default locale, and this is
                // a duplicate of the first supported locale.
                // In both cases, idef >= 0 now, so otherwise we can skip the comparison.
                // For a duplicate, putIfAbsent() is a no-op, so nothing to do.
            } else if (LocaleDistance.INSTANCE.isParadigmLSR(lsr)) {
                putIfAbsent(supportedLsrToIndex, lsr, i);
            } else {
                if (otherLsrToIndex == null) {
                    otherLsrToIndex = new LinkedHashMap<>(supportedLocalesLength);
                }
                putIfAbsent(otherLsrToIndex, lsr, i);
            }
            ++i;
        }
        if (otherLsrToIndex != null) {
            supportedLsrToIndex.putAll(otherLsrToIndex);
        }
        int supportedLSRsLength = supportedLsrToIndex.size();
        supportedLSRs = new LSR[supportedLSRsLength];
        supportedIndexes = new int[supportedLSRsLength];
        i = 0;
        for (Map.Entry<LSR, Integer> entry : supportedLsrToIndex.entrySet()) {
            supportedLSRs[i] = entry.getKey();  // = lsrs[entry.getValue()]
            supportedIndexes[i++] = entry.getValue();
        }

        defaultULocale = udef;
        defaultLocale = def;
        defaultLocaleIndex = idef;
        demotionPerDesiredLocale =
                builder.demotion == Demotion.NONE ? 0 :
                    LocaleDistance.INSTANCE.getDefaultDemotionPerDesiredLocale();  // null or REGION
        favorSubtag = builder.favor;
    }

    private static final void putIfAbsent(Map<LSR, Integer> lsrToIndex, LSR lsr, int i) {
        Integer index = lsrToIndex.get(lsr);
        if (index == null) {
            lsrToIndex.put(lsr, i);
        }
    }

    private static final LSR getMaximalLsrOrUnd(ULocale locale) {
        if (locale.equals(UND_ULOCALE)) {
            return UND_LSR;
        } else {
            return XLikelySubtags.INSTANCE.makeMaximizedLsrFrom(locale);
        }
    }

    private static final LSR getMaximalLsrOrUnd(Locale locale) {
        if (locale.equals(UND_LOCALE) || locale.equals(EMPTY_LOCALE)) {
            return UND_LSR;
        } else {
            return XLikelySubtags.INSTANCE.makeMaximizedLsrFrom(locale);
        }
    }

    private static final class ULocaleLsrIterator extends LsrIterator {
        private Iterator<ULocale> locales;
        private ULocale current, remembered;

        ULocaleLsrIterator(Iterator<ULocale> locales) {
            this.locales = locales;
        }

        @Override
        public boolean hasNext() {
            return locales.hasNext();
        }

        @Override
        public LSR next() {
            current = locales.next();
            return getMaximalLsrOrUnd(current);
        }

        @Override
        public void rememberCurrent(int desiredIndex) {
            bestDesiredIndex = desiredIndex;
            remembered = current;
        }
    }

    private static final class LocaleLsrIterator extends LsrIterator {
        private Iterator<Locale> locales;
        private Locale current, remembered;

        LocaleLsrIterator(Iterator<Locale> locales) {
            this.locales = locales;
        }

        @Override
        public boolean hasNext() {
            return locales.hasNext();
        }

        @Override
        public LSR next() {
            current = locales.next();
            return getMaximalLsrOrUnd(current);
        }

        @Override
        public void rememberCurrent(int desiredIndex) {
            bestDesiredIndex = desiredIndex;
            remembered = current;
        }
    }

    /**
     * Returns the supported locale which best matches the desired locale.
     *
     * @param desiredLocale Typically a user's language.
     * @return the best-matching supported locale.
     * @stable ICU 4.4
     */
    public ULocale getBestMatch(ULocale desiredLocale) {
        LSR desiredLSR = getMaximalLsrOrUnd(desiredLocale);
        int suppIndex = getBestSuppIndex(desiredLSR, null);
        return suppIndex >= 0 ? supportedULocales[suppIndex] : defaultULocale;
    }

    /**
     * Returns the supported locale which best matches one of the desired locales.
     *
     * @param desiredLocales Typically a user's languages, in order of preference (descending).
     *          (In ICU 4.4..63 this parameter had type LocalePriorityList.)
     * @return the best-matching supported locale.
     * @stable ICU 4.4
     */
    public ULocale getBestMatch(Iterable<ULocale> desiredLocales) {
        Iterator<ULocale> desiredIter = desiredLocales.iterator();
        if (!desiredIter.hasNext()) {
            return defaultULocale;
        }
        ULocaleLsrIterator lsrIter = new ULocaleLsrIterator(desiredIter);
        LSR desiredLSR = lsrIter.next();
        int suppIndex = getBestSuppIndex(desiredLSR, lsrIter);
        return suppIndex >= 0 ? supportedULocales[suppIndex] : defaultULocale;
    }

    /**
     * Parses the string like {@link LocalePriorityList} does and
     * returns the supported locale which best matches one of the desired locales.
     *
     * @param desiredLocaleList Typically a user's languages,
     *          as a string which is to be parsed like LocalePriorityList does.
     * @return the best-matching supported locale.
     * @stable ICU 4.4
     */
    public ULocale getBestMatch(String desiredLocaleList) {
        return getBestMatch(LocalePriorityList.add(desiredLocaleList).build());
    }

    /**
     * Returns the supported locale which best matches the desired locale.
     *
     * @param desiredLocale Typically a user's language.
     * @return the best-matching supported locale.
     * @draft ICU 65
     * @provisional This API might change or be removed in a future release.
     */
    public Locale getBestLocale(Locale desiredLocale) {
        LSR desiredLSR = getMaximalLsrOrUnd(desiredLocale);
        int suppIndex = getBestSuppIndex(desiredLSR, null);
        return suppIndex >= 0 ? supportedLocales[suppIndex] : defaultLocale;
    }

    /**
     * Returns the supported locale which best matches one of the desired locales.
     *
     * @param desiredLocales Typically a user's languages, in order of preference (descending).
     * @return the best-matching supported locale.
     * @draft ICU 65
     * @provisional This API might change or be removed in a future release.
     */
    public Locale getBestLocale(Iterable<Locale> desiredLocales) {
        Iterator<Locale> desiredIter = desiredLocales.iterator();
        if (!desiredIter.hasNext()) {
            return defaultLocale;
        }
        LocaleLsrIterator lsrIter = new LocaleLsrIterator(desiredIter);
        LSR desiredLSR = lsrIter.next();
        int suppIndex = getBestSuppIndex(desiredLSR, lsrIter);
        return suppIndex >= 0 ? supportedLocales[suppIndex] : defaultLocale;
    }

    private Result defaultResult() {
        return new Result(null, defaultULocale, null, defaultLocale, -1, defaultLocaleIndex);
    }

    private Result makeResult(ULocale desiredLocale, ULocaleLsrIterator lsrIter, int suppIndex) {
        if (suppIndex < 0) {
            return defaultResult();
        } else if (desiredLocale != null) {
            return new Result(desiredLocale, supportedULocales[suppIndex],
                    null, supportedLocales[suppIndex], 0, suppIndex);
        } else {
            return new Result(lsrIter.remembered, supportedULocales[suppIndex],
                    null, supportedLocales[suppIndex], lsrIter.bestDesiredIndex, suppIndex);
        }
    }

    private Result makeResult(Locale desiredLocale, LocaleLsrIterator lsrIter, int suppIndex) {
        if (suppIndex < 0) {
            return defaultResult();
        } else if (desiredLocale != null) {
            return new Result(null, supportedULocales[suppIndex],
                    desiredLocale, supportedLocales[suppIndex], 0, suppIndex);
        } else {
            return new Result(null, supportedULocales[suppIndex],
                    lsrIter.remembered, supportedLocales[suppIndex],
                    lsrIter.bestDesiredIndex, suppIndex);
        }
    }

    /**
     * Returns the best match between the desired locale and the supported locales.
     *
     * @param desiredLocale Typically a user's language.
     * @return the best-matching pair of the desired and a supported locale.
     * @draft ICU 65
     * @provisional This API might change or be removed in a future release.
     */
    public Result getBestMatchResult(ULocale desiredLocale) {
        LSR desiredLSR = getMaximalLsrOrUnd(desiredLocale);
        int suppIndex = getBestSuppIndex(desiredLSR, null);
        return makeResult(desiredLocale, null, suppIndex);
    }

    /**
     * Returns the best match between the desired and supported locales.
     *
     * @param desiredLocales Typically a user's languages, in order of preference (descending).
     * @return the best-matching pair of a desired and a supported locale.
     * @draft ICU 65
     * @provisional This API might change or be removed in a future release.
     */
    public Result getBestMatchResult(Iterable<ULocale> desiredLocales) {
        Iterator<ULocale> desiredIter = desiredLocales.iterator();
        if (!desiredIter.hasNext()) {
            return defaultResult();
        }
        ULocaleLsrIterator lsrIter = new ULocaleLsrIterator(desiredIter);
        LSR desiredLSR = lsrIter.next();
        int suppIndex = getBestSuppIndex(desiredLSR, lsrIter);
        return makeResult(null, lsrIter, suppIndex);
    }

    /**
     * Returns the best match between the desired locale and the supported locales.
     *
     * @param desiredLocale Typically a user's language.
     * @return the best-matching pair of the desired and a supported locale.
     * @draft ICU 65
     * @provisional This API might change or be removed in a future release.
     */
    public Result getBestLocaleResult(Locale desiredLocale) {
        LSR desiredLSR = getMaximalLsrOrUnd(desiredLocale);
        int suppIndex = getBestSuppIndex(desiredLSR, null);
        return makeResult(desiredLocale, null, suppIndex);
    }

    /**
     * Returns the best match between the desired and supported locales.
     *
     * @param desiredLocales Typically a user's languages, in order of preference (descending).
     * @return the best-matching pair of a desired and a supported locale.
     * @draft ICU 65
     * @provisional This API might change or be removed in a future release.
     */
    public Result getBestLocaleResult(Iterable<Locale> desiredLocales) {
        Iterator<Locale> desiredIter = desiredLocales.iterator();
        if (!desiredIter.hasNext()) {
            return defaultResult();
        }
        LocaleLsrIterator lsrIter = new LocaleLsrIterator(desiredIter);
        LSR desiredLSR = lsrIter.next();
        int suppIndex = getBestSuppIndex(desiredLSR, lsrIter);
        return makeResult(null, lsrIter, suppIndex);
    }

    /**
     * @param desiredLSR The first desired locale's LSR.
     * @param remainingIter Remaining desired LSRs, null or empty if none.
     * @return the index of the best-matching supported locale, or -1 if there is no good match.
     */
    private int getBestSuppIndex(LSR desiredLSR, LsrIterator remainingIter) {
        int desiredIndex = 0;
        int bestSupportedLsrIndex = -1;
        for (int bestDistance = thresholdDistance;;) {
            // Quick check for exact maximized LSR.
            Integer index = supportedLsrToIndex.get(desiredLSR);
            if (index != null) {
                int suppIndex = index;
                if (TRACE_MATCHER) {
                    System.err.printf("Returning %s: desiredLSR=supportedLSR\n",
                            supportedULocales[suppIndex]);
                }
                if (remainingIter != null) { remainingIter.rememberCurrent(desiredIndex); }
                return suppIndex;
            }
            int bestIndexAndDistance = LocaleDistance.INSTANCE.getBestIndexAndDistance(
                    desiredLSR, supportedLSRs, bestDistance, favorSubtag);
            if (bestIndexAndDistance >= 0) {
                bestDistance = bestIndexAndDistance & 0xff;
                if (remainingIter != null) { remainingIter.rememberCurrent(desiredIndex); }
                bestSupportedLsrIndex = bestIndexAndDistance >> 8;
            }
            if ((bestDistance -= demotionPerDesiredLocale) <= 0) {
                break;
            }
            if (remainingIter == null || !remainingIter.hasNext()) {
                break;
            }
            desiredLSR = remainingIter.next();
            ++desiredIndex;
        }
        if (bestSupportedLsrIndex < 0) {
            if (TRACE_MATCHER) {
                System.err.printf("Returning default %s: no good match\n", defaultULocale);
            }
            return -1;
        }
        int suppIndex = supportedIndexes[bestSupportedLsrIndex];
        if (TRACE_MATCHER) {
            System.err.printf("Returning %s: best matching supported locale\n",
                    supportedULocales[suppIndex]);
        }
        return suppIndex;
    }

    /**
     * Returns a fraction between 0 and 1, where 1 means that the languages are a
     * perfect match, and 0 means that they are completely different.
     *
     * <p>This is mostly an implementation detail, and the precise values may change over time.
     * The implementation may use either the maximized forms or the others ones, or both.
     * The implementation may or may not rely on the forms to be consistent with each other.
     *
     * <p>Callers should construct and use a matcher rather than match pairs of locales directly.
     *
     * @param desired Desired locale.
     * @param desiredMax Maximized locale (using likely subtags).
     * @param supported Supported locale.
     * @param supportedMax Maximized locale (using likely subtags).
     * @return value between 0 and 1, inclusive.
     * @deprecated ICU 65 Build and use a matcher rather than comparing pairs of locales.
     */
    @Deprecated
    public double match(ULocale desired, ULocale desiredMax, ULocale supported, ULocale supportedMax) {
        // Returns the inverse of the distance: That is, 1-distance(desired, supported).
        int distance = LocaleDistance.INSTANCE.getBestIndexAndDistance(
                getMaximalLsrOrUnd(desired),
                new LSR[] { getMaximalLsrOrUnd(supported) },
                thresholdDistance, favorSubtag) & 0xff;
        return (100 - distance) / 100.0;
    }

    /**
     * Partially canonicalizes a locale (language). Note that for now, it is canonicalizing
     * according to CLDR conventions (he vs iw, etc), since that is what is needed
     * for likelySubtags.
     *
     * <p>Currently, this is a much simpler canonicalization than what the ULocale class does:
     * The language/script/region subtags are each mapped separately, ignoring the other subtags.
     * If none of these change, then the input locale is returned.
     * Otherwise a new ULocale with only those subtags is returned, removing variants and extensions.
     *
     * @param locale language/locale code
     * @return ULocale with remapped subtags.
     * @stable ICU 4.4
     */
    public ULocale canonicalize(ULocale locale) {
        return XLikelySubtags.INSTANCE.canonicalize(locale);
    }

    /**
     * {@inheritDoc}
     * @stable ICU 4.4
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder().append("{LocaleMatcher");
        if (supportedULocales.length > 0) {
            s.append(" supported={").append(supportedULocales[0].toString());
            for (int i = 1; i < supportedULocales.length; ++i) {
                s.append(", ").append(supportedULocales[i].toString());
            }
            s.append('}');
        }
        s.append(" default=").append(Objects.toString(defaultULocale));
        if (favorSubtag != null) {
            s.append(" distance=").append(favorSubtag.toString());
        }
        if (thresholdDistance >= 0) {
            s.append(String.format(" threshold=%d", thresholdDistance));
        }
        s.append(String.format(" demotion=%d", demotionPerDesiredLocale));
        return s.append('}').toString();
    }
}
