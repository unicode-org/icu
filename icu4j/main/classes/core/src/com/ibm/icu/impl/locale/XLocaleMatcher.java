// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.locale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.ibm.icu.impl.locale.LocaleDistance.DistanceOption;
import com.ibm.icu.util.LocalePriorityList;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

/**
 * Immutable class that picks best match between user's desired locales and application's supported locales.
 * @author markdavis
 */
public final class XLocaleMatcher {
    private static final LSR UND_LSR = new LSR("und","","");
    private static final ULocale UND_LOCALE = new ULocale("und");
    private static final Iterator<ULocale> NULL_ITERATOR = null;

    // Activates debugging output to stderr with details of GetBestMatch.
    private static final boolean TRACE_MATCHER = false;

    // List of indexes, optimized for one or two.
    private static final class Indexes {
        // Some indexes without further object creation and auto-boxing.
        int first, second = -1;
        // We could turn the List into an int array + length and manage its growth.
        List<Integer> remaining;

        Indexes(int firstIndex) {
            first = firstIndex;
        }
        void add(int i) {
            if (second < 0) {
                second = i;
            } else {
                if (remaining == null) {
                    remaining = new ArrayList<>();
                }
                remaining.add(i);
            }
        }
        int getFirst() { return first; }
        int get(int i) {  // returns -1 when i >= length
            if (i == 0) {
                return first;
            } else if (i == 1) {
                return second;
            } else if (remaining != null && (i -= 2) < remaining.size()) {
                return remaining.get(i);
            } else {
                return -1;
            }
        }
    }

    // TODO: Make public, and add public methods that return it.
    private static final class Result {
        private Result(ULocale desired, ULocale supported,
                /* Locale jdesired, */ Locale jsupported,
                int desIndex, int suppIndex) {
            desiredLocale = desired;
            supportedLocale = supported;
            // desiredJavaLocale = jdesired;
            supportedJavaLocale = jsupported;
            desiredIndex = desIndex;
            supportedIndex = suppIndex;
        }

        ULocale desiredLocale;
        ULocale supportedLocale;
        // Locale desiredJavaLocale;
        Locale supportedJavaLocale;
        int desiredIndex;
        @SuppressWarnings("unused")  // unused until public, for other wrappers
        int supportedIndex;
    }

    // normally the default values, but can be set via constructor

    private final int thresholdDistance;
    private final int demotionPerAdditionalDesiredLocale;
    private final DistanceOption distanceOption;

    // built based on application's supported languages in constructor

    private final ULocale[] supportedLocales;
    private final Locale[] supportedJavaLocales;
    private final Map<ULocale, Integer> supportedToIndex;
    private final Map<LSR, Indexes> supportedLsrToIndexes;
    // Array versions of the supportedLsrToIndexes keys and values.
    // The distance lookup loops over the supportedLsrs and returns the index of the best match.
    private final LSR[] supportedLsrs;
    private final Indexes[] supportedIndexes;
    private final ULocale defaultLocale;
    private final Locale defaultJavaLocale;
    private final int defaultLocaleIndex;

    public static class Builder {
        /**
         * Supported locales. A Set, to avoid duplicates.
         * Maintains iteration order for consistent matching behavior (first best match wins).
         */
        private Set<ULocale> supportedLocales;
        private int thresholdDistance = -1;
        private int demotionPerAdditionalDesiredLocale = -1;;
        private ULocale defaultLocale;
        private DistanceOption distanceOption;
        /**
         * @param locales the languagePriorityList to set
         * @return this Builder object
         */
        public Builder setSupportedLocales(String locales) {
            return setSupportedLocales(LocalePriorityList.add(locales).build());
        }
        public Builder setSupportedLocales(Iterable<ULocale> locales) {
            supportedLocales = new LinkedHashSet<>(); // maintain order
            for (ULocale locale : locales) {
                supportedLocales.add(locale);
            }
            return this;
        }
        public Builder setSupportedLocales(Collection<ULocale> locales) {
            supportedLocales = new LinkedHashSet<>(locales); // maintain order
            return this;
        }
        public Builder setSupportedJavaLocales(Collection<Locale> locales) {
            supportedLocales = new LinkedHashSet<>(locales.size()); // maintain order
            for (Locale locale : locales) {
                supportedLocales.add(ULocale.forLocale(locale));
            }
            return this;
        }
        public Builder addSupportedLocale(ULocale locale) {
            if (supportedLocales == null) {
                supportedLocales = new LinkedHashSet<>();
            }
            supportedLocales.add(locale);
            return this;
        }
        public Builder addSupportedLocale(Locale locale) {
            return addSupportedLocale(ULocale.forLocale(locale));
        }

        /**
         * @param thresholdDistance the thresholdDistance to set, with -1 = default
         * @return this Builder object
         */
        public Builder setThresholdDistance(int thresholdDistance) {
            if (thresholdDistance > 100) {
                thresholdDistance = 100;
            }
            this.thresholdDistance = thresholdDistance;
            return this;
        }
        /**
         * @param demotionPerAdditionalDesiredLocale the demotionPerAdditionalDesiredLocale to set, with -1 = default
         * @return this Builder object
         */
        public Builder setDemotionPerAdditionalDesiredLocale(int demotionPerAdditionalDesiredLocale) {
            this.demotionPerAdditionalDesiredLocale = demotionPerAdditionalDesiredLocale;
            return this;
        }

        /**
         * Set the default language, with null = default = first supported language
         * @param defaultLocale the default language
         * @return this Builder object
         */
        public Builder setDefaultLanguage(ULocale defaultLocale) {
            this.defaultLocale = defaultLocale;
            return this;
        }

        /**
         * If true, then the language differences are smaller than than script differences.
         * This is used in situations (such as maps) where it is better to fall back to the same script than a similar language.
         * @param distanceOption the distance option
         * @return this Builder object
         */
        public Builder setDistanceOption(DistanceOption distanceOption) {
            this.distanceOption = distanceOption;
            return this;
        }

        public XLocaleMatcher build() {
            return new XLocaleMatcher(this);
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder().append("{XLocaleMatcher.Builder");
            if (!supportedLocales.isEmpty()) {
                s.append(" supported={").append(supportedLocales.toString()).append('}');
            }
            if (defaultLocale != null) {
                s.append(" default=").append(defaultLocale.toString());
            }
            if (distanceOption != null) {
                s.append(" distance=").append(distanceOption.toString());
            }
            if (thresholdDistance >= 0) {
                s.append(String.format(" threshold=%d", thresholdDistance));
            }
            if (demotionPerAdditionalDesiredLocale >= 0) {
                s.append(String.format(" demotion=%d", demotionPerAdditionalDesiredLocale));
            }
            return s.append('}').toString();
        }
    }

    /**
     * Returns a builder used in chaining parameters for building a Locale Matcher.
     * @return this Builder object
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Convenience method */
    public XLocaleMatcher(String supportedLocales) {
        this(builder().setSupportedLocales(supportedLocales));
    }
    /** Convenience method */
    public XLocaleMatcher(LocalePriorityList supportedLocales) {
        this(builder().setSupportedLocales(supportedLocales));
    }
    /** Convenience method */
    public XLocaleMatcher(Set<ULocale> supportedLocales) {
        this(builder().setSupportedLocales(supportedLocales));
    }

    /**
     * Creates a locale matcher with the given Builder parameters.
     */
    private XLocaleMatcher(Builder builder) {
        thresholdDistance = builder.thresholdDistance < 0 ?
                LocaleDistance.INSTANCE.getDefaultScriptDistance() : builder.thresholdDistance;
        // Store the supported locales in input order,
        // so that when different types are used (e.g., java.util.Locale)
        // we can return those by parallel index.
        int supportedLocalesLength = builder.supportedLocales.size();
        supportedLocales = new ULocale[supportedLocalesLength];
        supportedJavaLocales = new Locale[supportedLocalesLength];
        supportedToIndex = new HashMap<>(supportedLocalesLength);
        // We need an unordered map from LSR to first supported locale with that LSR,
        // and an ordered list of (LSR, Indexes).
        // We use a LinkedHashMap for both,
        // and insert the supported locales in the following order:
        // 1. First supported locale.
        // 2. Priority locales in builder order.
        // 3. Remaining locales in builder order.
        supportedLsrToIndexes = new LinkedHashMap<>(supportedLocalesLength);
        Map<LSR, Indexes> otherLsrToIndexes = null;
        LSR firstLSR = null;
        int i = 0;
        for (ULocale locale : builder.supportedLocales) {
            supportedLocales[i] = locale;
            supportedJavaLocales[i] = locale.toLocale();
            // supportedToIndex.putIfAbsent(locale, i)
            Integer oldIndex = supportedToIndex.get(locale);
            if (oldIndex == null) {
                supportedToIndex.put(locale, i);
            }
            LSR lsr = getMaximalLsrOrUnd(locale);
            if (i == 0) {
                firstLSR = lsr;
                supportedLsrToIndexes.put(lsr, new Indexes(0));
            } else if (lsr.equals(firstLSR) || LocaleDistance.INSTANCE.isParadigmLSR(lsr)) {
                addIndex(supportedLsrToIndexes, lsr, i);
            } else {
                if (otherLsrToIndexes == null) {
                    otherLsrToIndexes = new LinkedHashMap<>(supportedLocalesLength);
                }
                addIndex(otherLsrToIndexes, lsr, i);
            }
            ++i;
        }
        if (otherLsrToIndexes != null) {
            supportedLsrToIndexes.putAll(otherLsrToIndexes);
        }
        int numSuppLsrs = supportedLsrToIndexes.size();
        supportedLsrs = supportedLsrToIndexes.keySet().toArray(new LSR[numSuppLsrs]);
        supportedIndexes = supportedLsrToIndexes.values().toArray(new Indexes[numSuppLsrs]);
        ULocale def;
        Locale jdef = null;
        int idef = -1;
        if (builder.defaultLocale != null) {
            def = builder.defaultLocale;
        } else if (supportedLocalesLength > 0) {
            def = supportedLocales[0]; // first language
            jdef = supportedJavaLocales[0];
            idef = 0;
        } else {
            def = null;
        }
        if (jdef == null && def != null) {
            jdef = def.toLocale();
        }
        defaultLocale = def;
        defaultJavaLocale = jdef;
        defaultLocaleIndex = idef;
        demotionPerAdditionalDesiredLocale = builder.demotionPerAdditionalDesiredLocale < 0 ?
                LocaleDistance.INSTANCE.getDefaultRegionDistance() + 1 :
                    builder.demotionPerAdditionalDesiredLocale;
        distanceOption = builder.distanceOption;
    }

    private static final void addIndex(Map<LSR, Indexes> lsrToIndexes, LSR lsr, int i) {
        Indexes indexes = lsrToIndexes.get(lsr);
        if (indexes == null) {
            lsrToIndexes.put(lsr, new Indexes(i));
        } else {
            indexes.add(i);
        }
    }

    private static final LSR getMaximalLsrOrUnd(ULocale locale) {
        if (locale.equals(UND_LOCALE)) {
            return UND_LSR;
        } else {
            return XLikelySubtags.INSTANCE.makeMaximizedLsrFrom(locale);
        }
    }

    /** Convenience method */
    public ULocale getBestMatch(ULocale ulocale) {
        return getBestMatch(ulocale, NULL_ITERATOR).supportedLocale;
    }
    /** Convenience method */
    public ULocale getBestMatch(String languageList) {
        return getBestMatch(LocalePriorityList.add(languageList).build(), null);
    }
    /** Convenience method */
    public ULocale getBestMatch(ULocale... locales) {
        return getBestMatch(Arrays.asList(locales), null);
    }
    /** Convenience method */
    public ULocale getBestMatch(Iterable<ULocale> desiredLocales) {
        return getBestMatch(desiredLocales, null);
    }

    /**
     * Get the best match between the desired languages and supported languages
     * @param desiredLocales Typically the supplied user's languages, in order of preference, with best first.
     * @param outputBestDesired The one of the desired languages that matched best (can be null).
     * Set to null if the best match was not below the threshold distance.
     * @return the best match.
     */
    public ULocale getBestMatch(Iterable<ULocale> desiredLocales, Output<ULocale> outputBestDesired) {
        Iterator<ULocale> desiredIter = desiredLocales.iterator();
        if (!desiredIter.hasNext()) {
            if (outputBestDesired != null) {
                outputBestDesired.value = null;
            }
            if (TRACE_MATCHER) {
                System.err.printf("Returning default %s: no desired languages\n", defaultLocale);
            }
            return defaultLocale;
        }
        ULocale desiredLocale = desiredIter.next();
        return getBestMatch(desiredLocale, desiredIter, outputBestDesired);
    }

    /**
     * @param desiredLocale First desired locale.
     * @param remainingIter Remaining desired locales, null or empty if none.
     * @param outputBestDesired If not null,
     *     will be set to the desired locale that matches the best supported one.
     * @return the best supported locale.
     */
    private ULocale getBestMatch(ULocale desiredLocale, Iterator<ULocale> remainingIter,
            Output<ULocale> outputBestDesired) {
        Result result = getBestMatch(desiredLocale, remainingIter);
        if (outputBestDesired != null) {
            outputBestDesired.value = result.desiredLocale;
        }
        return result.supportedLocale;
    }

    private Result getBestMatch(ULocale desiredLocale, Iterator<ULocale> remainingIter) {
        int desiredIndex = 0;
        int bestDesiredIndex = -1;
        ULocale bestDesiredLocale = null;
        int bestSupportedLsrIndex = 0;
        for (int bestDistance = thresholdDistance; bestDistance > 0;
                bestDistance -= demotionPerAdditionalDesiredLocale) {
            // Quick check for exact locale match.
            Integer supportedIndex = supportedToIndex.get(desiredLocale);
            if (supportedIndex != null) {
                if (TRACE_MATCHER) {
                    System.err.printf("Returning %s: desired=supported\n", desiredLocale);
                }
                int suppIndex = supportedIndex;
                return new Result(desiredLocale, supportedLocales[suppIndex],
                        supportedJavaLocales[suppIndex], desiredIndex, suppIndex);
            }
            // Quick check for exact maximized LSR.
            LSR desiredLSR = getMaximalLsrOrUnd(desiredLocale);
            Indexes indexes = supportedLsrToIndexes.get(desiredLSR);
            if (indexes != null) {
                // If this is a supported LSR, return the first locale.
                // We already know the exact locale isn't there.
                int suppIndex = indexes.getFirst();
                ULocale result = supportedLocales[suppIndex];
                if (TRACE_MATCHER) {
                    System.err.printf("Returning %s: desiredLSR=supportedLSR\n", result);
                }
                return new Result(desiredLocale, result,
                        supportedJavaLocales[suppIndex], desiredIndex, suppIndex);
            }
            int bestIndexAndDistance = LocaleDistance.INSTANCE.getBestIndexAndDistance(
                    desiredLSR, supportedLsrs, bestDistance, distanceOption);
            if (bestIndexAndDistance >= 0) {
                bestDistance = bestIndexAndDistance & 0xff;
                bestDesiredIndex = desiredIndex;
                bestDesiredLocale = desiredLocale;
                bestSupportedLsrIndex = bestIndexAndDistance >> 8;
                if (bestDistance == 0) {
                    break;
                }
            }
            if (remainingIter == null || !remainingIter.hasNext()) {
                break;
            }
            desiredLocale = remainingIter.next();
            ++desiredIndex;
        }
        if (bestDesiredIndex < 0) {
            if (TRACE_MATCHER) {
                System.err.printf("Returning default %s: no good match\n", defaultLocale);
            }
            return new Result(null, defaultLocale, defaultJavaLocale, -1, defaultLocaleIndex);
        }
        // Pick exact match if there is one.
        // The length of the list is normally 1.
        Indexes bestSupportedIndexes = supportedIndexes[bestSupportedLsrIndex];
        int suppIndex;
        for (int i = 0; (suppIndex = bestSupportedIndexes.get(i)) >= 0; ++i) {
            ULocale locale = supportedLocales[suppIndex];
            if (bestDesiredLocale.equals(locale)) {
                if (TRACE_MATCHER) {
                    System.err.printf("Returning %s: desired=best matching supported language\n",
                            bestDesiredLocale);
                }
                return new Result(bestDesiredLocale, locale,
                        supportedJavaLocales[suppIndex], bestDesiredIndex, suppIndex);
            }
        }
        // Otherwise return the first of the supported languages that share the best-matching LSR.
        suppIndex = bestSupportedIndexes.getFirst();
        ULocale result = supportedLocales[suppIndex];
        if (TRACE_MATCHER) {
            System.err.printf("Returning %s: first best matching supported language\n", result);
        }
        return new Result(bestDesiredLocale, result,
                supportedJavaLocales[suppIndex], bestDesiredIndex, suppIndex);
    }

    /**
     * Get the best match between the desired languages and supported languages
     * @param desiredLocale the supplied user's language.
     * @param outputBestDesired The one of the desired languages that matched best.
     * Set to null if the best match was not below the threshold distance.
     * @return the best match.
     */
    public ULocale getBestMatch(ULocale desiredLocale, Output<ULocale> outputBestDesired) {
        return getBestMatch(desiredLocale, null, outputBestDesired);
    }

    /**
     * Converts Locales to ULocales on the fly.
     */
    private static final class LocalesWrapper implements Iterator<ULocale> {
        private Iterator<Locale> locales;
        // Cache locales to avoid conversion of the result.
        private Locale first, second;
        private List<Locale> remaining;

        LocalesWrapper(Iterator<Locale> locales) {
            this.locales = locales;
        }

        @Override
        public boolean hasNext() {
            return locales.hasNext();
        }

        @Override
        public ULocale next() {
            Locale locale = locales.next();
            if (first == null) {
                first = locale;
            } else if (second == null) {
                second = locale;
            } else {
                if (remaining == null) {
                    remaining = new ArrayList<>();
                }
                remaining.add(locale);
            }
            return ULocale.forLocale(locale);
        }

        Locale getJavaLocale(int i) {
            if (i == 0) {
                return first;
            } else if (i == 1) {
                return second;
            } else {
                // TODO: test code coverage
                return remaining.get(i - 2);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public Locale getBestJavaMatch(Iterable<Locale> desiredLocales, Output<Locale> outputBestDesired) {
        Iterator<Locale> desiredIter = desiredLocales.iterator();
        if (!desiredIter.hasNext()) {
            if (outputBestDesired != null) {
                outputBestDesired.value = null;
            }
            if (TRACE_MATCHER) {
                System.err.printf("Returning default %s: no desired languages\n", defaultLocale);
            }
            return defaultJavaLocale;
        }
        LocalesWrapper wrapper = new LocalesWrapper(desiredIter);
        ULocale desiredLocale = wrapper.next();
        Result result = getBestMatch(desiredLocale, NULL_ITERATOR);
        if (outputBestDesired != null) {
            outputBestDesired.value = result.desiredIndex >= 0 ?
                    wrapper.getJavaLocale(result.desiredIndex) : null;
        }
        return result.supportedJavaLocale;
    }

    public Locale getBestJavaMatch(Locale desiredLocale, Output<Locale> outputBestDesired) {
        ULocale desiredULocale = ULocale.forLocale(desiredLocale);
        Result result = getBestMatch(desiredULocale, NULL_ITERATOR);
        if (outputBestDesired != null) {
            outputBestDesired.value = result.desiredIndex >= 0 ? desiredLocale : null;
        }
        return result.supportedJavaLocale;
    }

    /** Combine features of the desired locale into those of the supported, and return result. */
    public static ULocale combine(ULocale bestSupported, ULocale bestDesired) {
        // for examples of extensions, variants, see
        //  http://unicode.org/repos/cldr/tags/latest/common/bcp47/
        //  http://unicode.org/repos/cldr/tags/latest/common/validity/variant.xml

        if (!bestSupported.equals(bestDesired) && bestDesired != null) {
            // add region, variants, extensions
            ULocale.Builder b = new ULocale.Builder().setLocale(bestSupported);

            // copy the region from the desired, if there is one
            String region = bestDesired.getCountry();
            if (!region.isEmpty()) {
                b.setRegion(region);
            }

            // copy the variants from desired, if there is one
            // note that this will override any subvariants. Eg "sco-ulster-fonipa" + "…-fonupa" => "sco-fonupa" (nuking ulster)
            String variants = bestDesired.getVariant();
            if (!variants.isEmpty()) {
                b.setVariant(variants);
            }

            // copy the extensions from desired, if there are any
            // note that this will override any subkeys. Eg "th-u-nu-latn-ca-buddhist" + "…-u-nu-native" => "th-u-nu-native" (nuking calendar)
            for (char extensionKey : bestDesired.getExtensionKeys()) {
                b.setExtension(extensionKey, bestDesired.getExtension(extensionKey));
            }
            bestSupported = b.build();
        }
        return bestSupported;
    }

    /** Returns the distance between the two languages. The values are not necessarily symmetric.
     * @param desired A locale desired by the user
     * @param supported A locale supported by a program.
     * @return A return of 0 is a complete match, and 100 is a failure case (above the thresholdDistance).
     * A language is first maximized with add likely subtags, then compared.
     */
    public int distance(ULocale desired, ULocale supported) {
        return LocaleDistance.INSTANCE.getBestIndexAndDistance(
            XLikelySubtags.INSTANCE.makeMaximizedLsrFrom(desired),
            new LSR[] { XLikelySubtags.INSTANCE.makeMaximizedLsrFrom(supported) },
            thresholdDistance, distanceOption) & 0xff;
    }

    /** Convenience method */
    public int distance(String desiredLanguage, String supportedLanguage) {
        return LocaleDistance.INSTANCE.getBestIndexAndDistance(
            XLikelySubtags.INSTANCE.makeMaximizedLsrFrom(new ULocale(desiredLanguage)),
            new LSR[] { XLikelySubtags.INSTANCE.makeMaximizedLsrFrom(new ULocale(supportedLanguage)) },
            thresholdDistance, distanceOption) & 0xff;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder().append("{XLocaleMatcher");
        if (supportedLocales.length > 0) {
            s.append(" supported={").append(supportedLocales[0].toString());
            for (int i = 1; i < supportedLocales.length; ++i) {
                s.append(", ").append(supportedLocales[1].toString());
            }
            s.append('}');
        }
        s.append(" default=").append(Objects.toString(defaultLocale));
        if (distanceOption != null) {
            s.append(" distance=").append(distanceOption.toString());
        }
        if (thresholdDistance >= 0) {
            s.append(String.format(" threshold=%d", thresholdDistance));
        }
        s.append(String.format(" demotion=%d", demotionPerAdditionalDesiredLocale));
        return s.append('}').toString();
    }

    /** Return the inverse of the distance: that is, 1-distance(desired, supported) */
    public double match(ULocale desired, ULocale supported) {
        return (100-distance(desired, supported))/100.0;
    }

    /**
     * Returns a fraction between 0 and 1, where 1 means that the languages are a
     * perfect match, and 0 means that they are completely different. This is (100-distance(desired, supported))/100.0.
     * <br>Note that
     * the precise values may change over time; no code should be made dependent
     * on the values remaining constant.
     * @param desired Desired locale
     * @param desiredMax Maximized locale (using likely subtags)
     * @param supported Supported locale
     * @param supportedMax Maximized locale (using likely subtags)
     * @return value between 0 and 1, inclusive.
     * @deprecated Use the form with 2 parameters instead.
     */
    @Deprecated
    public double match(ULocale desired, ULocale desiredMax, ULocale supported, ULocale supportedMax) {
        return match(desired, supported);
    }

    /**
     * Canonicalize a locale (language). Note that for now, it is canonicalizing
     * according to CLDR conventions (he vs iw, etc), since that is what is needed
     * for likelySubtags.
     * @param ulocale language/locale code
     * @return ULocale with remapped subtags.
     * @stable ICU 4.4
     */
    public ULocale canonicalize(ULocale ulocale) {
        // TODO
        return null;
    }

    /**
     * @return the thresholdDistance. Any distance above this value is treated as a match failure.
     */
    public int getThresholdDistance() {
        return thresholdDistance;
    }
}
