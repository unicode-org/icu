// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.locale;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.icu.impl.locale.XCldrStub.ImmutableMultimap;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableSet;
import com.ibm.icu.impl.locale.XCldrStub.LinkedHashMultimap;
import com.ibm.icu.impl.locale.XCldrStub.Multimap;
import com.ibm.icu.impl.locale.XLikelySubtags.LSR;
import com.ibm.icu.impl.locale.XLocaleDistance.DistanceOption;
import com.ibm.icu.util.LocalePriorityList;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

/**
 * Immutable class that picks best match between user's desired locales and application's supported locales.
 * @author markdavis
 */
public class XLocaleMatcher {
    private static final LSR UND = new LSR("und","","");
    private static final ULocale UND_LOCALE = new ULocale("und");

    // Activates debugging output to stderr with details of GetBestMatch.
    private static final boolean TRACE_MATCHER = false;

    // normally the default values, but can be set via constructor

    private final XLocaleDistance localeDistance;
    private final int thresholdDistance;
    private final int demotionPerAdditionalDesiredLocale;
    private final DistanceOption distanceOption;

    // built based on application's supported languages in constructor

    private final Map<LSR, Set<ULocale>> supportedLanguages; // the locales in the collection are ordered!
    private final Set<ULocale> exactSupportedLocales; // the locales in the collection are ordered!
    private final ULocale defaultLanguage;

    public static class Builder {
        private Set<ULocale> supportedLanguagesList;
        private int thresholdDistance = -1;
        private int demotionPerAdditionalDesiredLocale = -1;;
        private ULocale defaultLanguage;
        private XLocaleDistance localeDistance;
        private DistanceOption distanceOption;
        /**
         * @param languagePriorityList the languagePriorityList to set
         * @return this Builder object
         */
        public Builder setSupportedLocales(String languagePriorityList) {
            this.supportedLanguagesList = asSet(LocalePriorityList.add(languagePriorityList).build());
            return this;
        }
        public Builder setSupportedLocales(LocalePriorityList languagePriorityList) {
            this.supportedLanguagesList = asSet(languagePriorityList);
            return this;
        }
        public Builder setSupportedLocales(Set<ULocale> languagePriorityList) {
            Set<ULocale> temp = new LinkedHashSet<ULocale>(); // maintain order
            temp.addAll(languagePriorityList);
            this.supportedLanguagesList = temp;
            return this;
        }

        /**
         * @param thresholdDistance the thresholdDistance to set, with -1 = default
         * @return this Builder object
         */
        public Builder setThresholdDistance(int thresholdDistance) {
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
         * @param localeDistance the localeDistance to set, with default = XLocaleDistance.getDefault().
         * @return this Builder object
         */
        public Builder setLocaleDistance(XLocaleDistance localeDistance) {
            this.localeDistance = localeDistance;
            return this;
        }

        /**
         * Set the default language, with null = default = first supported language
         * @param defaultLanguage the default language
         * @return this Builder object
         */
        public Builder setDefaultLanguage(ULocale defaultLanguage) {
            this.defaultLanguage = defaultLanguage;
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
          if (!supportedLanguagesList.isEmpty()) {
            s.append(" supported={").append(supportedLanguagesList.toString()).append("}");
          }
          if (defaultLanguage != null) {
            s.append(" default=").append(defaultLanguage.toString());
          }
          if (thresholdDistance >= 0) {
            s.append(String.format(" thresholdDistance=%d", thresholdDistance));
          }
          s.append(" preference=").append(distanceOption.name());
          return s.append("}").toString();
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
     * Create a locale matcher with the given parameters.
     * @param supportedLocales
     * @param thresholdDistance
     * @param demotionPerAdditionalDesiredLocale
     * @param localeDistance
     * @param likelySubtags
     */
    private XLocaleMatcher(Builder builder) {
        localeDistance = builder.localeDistance == null ? XLocaleDistance.getDefault()
            : builder.localeDistance;
        thresholdDistance = builder.thresholdDistance < 0 ? localeDistance.getDefaultScriptDistance()
            : builder.thresholdDistance;
        // only do AFTER above are set
        Set<LSR> paradigms = extractLsrSet(localeDistance.getParadigms());
        final Multimap<LSR, ULocale> temp2 = extractLsrMap(builder.supportedLanguagesList, paradigms);
        supportedLanguages = temp2.asMap();
        exactSupportedLocales = ImmutableSet.copyOf(temp2.values());
        defaultLanguage = builder.defaultLanguage != null ? builder.defaultLanguage
            : supportedLanguages.isEmpty() ? null
                : supportedLanguages.entrySet().iterator().next().getValue().iterator().next(); // first language
        demotionPerAdditionalDesiredLocale = builder.demotionPerAdditionalDesiredLocale < 0 ? localeDistance.getDefaultRegionDistance()+1
            : builder.demotionPerAdditionalDesiredLocale;
        distanceOption = builder.distanceOption;
    }

    // Result is not immutable!
    private Set<LSR> extractLsrSet(Set<ULocale> languagePriorityList) {
        Set<LSR> result = new LinkedHashSet<LSR>();
        for (ULocale item : languagePriorityList) {
            final LSR max = item.equals(UND_LOCALE) ? UND : LSR.fromMaximalized(item);
            result.add(max);
        }
        return result;
    }

    private Multimap<LSR,ULocale> extractLsrMap(Set<ULocale> languagePriorityList, Set<LSR> priorities) {
        Multimap<LSR, ULocale> builder = LinkedHashMultimap.create();
        for (ULocale item : languagePriorityList) {
            final LSR max = item.equals(UND_LOCALE) ? UND :
            LSR.fromMaximalized(item);
            builder.put(max, item);
        }
        if (builder.size() > 1 && priorities != null) {
            // for the supported list, we put any priorities before all others, except for the first.
            Multimap<LSR, ULocale> builder2 = LinkedHashMultimap.create();

            // copy the long way so the priorities are in the same order as in the original
            boolean first = true;
            for (Entry<LSR, Set<ULocale>> entry : builder.asMap().entrySet()) {
                final LSR key = entry.getKey();
                if (first || priorities.contains(key)) {
                    builder2.putAll(key, entry.getValue());
                    first = false;
                }
            }
            // now copy the rest
            builder2.putAll(builder);
            if (!builder2.equals(builder)) {
                throw new IllegalArgumentException();
            }
            builder = builder2;
        }
        return ImmutableMultimap.copyOf(builder);
    }


    /** Convenience method */
    public ULocale getBestMatch(ULocale ulocale) {
        return getBestMatch(ulocale, null);
    }
    /** Convenience method */
    public ULocale getBestMatch(String languageList) {
        return getBestMatch(LocalePriorityList.add(languageList).build(), null);
    }
    /** Convenience method */
    public ULocale getBestMatch(ULocale... locales) {
        return getBestMatch(new LinkedHashSet<ULocale>(Arrays.asList(locales)), null);
    }
    /** Convenience method */
    public ULocale getBestMatch(Set<ULocale> desiredLanguages) {
        return getBestMatch(desiredLanguages, null);
    }
    /** Convenience method */
    public ULocale getBestMatch(LocalePriorityList desiredLanguages) {
        return getBestMatch(desiredLanguages, null);
    }
    /** Convenience method */
    public ULocale getBestMatch(LocalePriorityList desiredLanguages, Output<ULocale> outputBestDesired) {
        return getBestMatch(asSet(desiredLanguages), outputBestDesired);
    }

    // TODO add LocalePriorityList method asSet() for ordered Set view backed by LocalePriorityList
    private static Set<ULocale> asSet(LocalePriorityList languageList) {
        Set<ULocale> temp = new LinkedHashSet<ULocale>(); // maintain order
        for (ULocale locale : languageList) {
            temp.add(locale);
        };
        return temp;
    }

    /**
     * Get the best match between the desired languages and supported languages
     * @param desiredLanguages Typically the supplied user's languages, in order of preference, with best first.
     * @param outputBestDesired The one of the desired languages that matched best.
     * Set to null if the best match was not below the threshold distance.
     * @return the best match.
     */
    public ULocale getBestMatch(Set<ULocale> desiredLanguages, Output<ULocale> outputBestDesired) {
        // fast path for singleton
        if (desiredLanguages.size() == 1) {
            return getBestMatch(desiredLanguages.iterator().next(), outputBestDesired);
        }
        // TODO produce optimized version for single desired ULocale
        Multimap<LSR, ULocale> desiredLSRs = extractLsrMap(desiredLanguages,null);
        int bestDistance = Integer.MAX_VALUE;
        ULocale bestDesiredLocale = null;
        Collection<ULocale> bestSupportedLocales = null;
        int delta = 0;
    mainLoop:
        for (final Entry<LSR, Set<ULocale>> desiredLsrAndLocales : desiredLSRs.asMap().entrySet()) {
          LSR desiredLSR = desiredLsrAndLocales.getKey();
          for (ULocale desiredLocale : desiredLsrAndLocales.getValue()) {
            // quick check for exact match
            if (delta < bestDistance) {
              if (exactSupportedLocales.contains(desiredLocale)) {
                if (outputBestDesired != null) {
                  outputBestDesired.value = desiredLocale;
                }
                if (TRACE_MATCHER) {
                    System.err.printf(
                              "Returning %s, which is an exact match for a supported language\n",
                              desiredLocale);
                 }
                return desiredLocale;
              }
              // quick check for maximized locale
              Collection<ULocale> found = supportedLanguages.get(desiredLSR);
              if (found != null) {
                // if we find one in the set, return first (lowest). We already know the exact one isn't
                // there.
                if (outputBestDesired != null) {
                  outputBestDesired.value = desiredLocale;
                }
                ULocale result = found.iterator().next();
                if (TRACE_MATCHER) {
                  System.err.printf("Returning %s\n", result.toString());
                }
                return result;
              }
            }
            for (final Entry<LSR, Set<ULocale>> supportedLsrAndLocale : supportedLanguages.entrySet()) {
              int distance =
                  delta
                      + localeDistance.distanceRaw(
                          desiredLSR,
                          supportedLsrAndLocale.getKey(),
                          thresholdDistance,
                          distanceOption);
              if (distance < bestDistance) {
                bestDistance = distance;
                bestDesiredLocale = desiredLocale;
                bestSupportedLocales = supportedLsrAndLocale.getValue();
                if (distance == 0) {
                  break mainLoop;
                }
              }
            }
            delta += demotionPerAdditionalDesiredLocale;
          }
        }
        if (bestDistance >= thresholdDistance) {
            if (outputBestDesired != null) {
                outputBestDesired.value = null;
            }
            if (TRACE_MATCHER) {
              System.err.printf("Returning default %s\n", defaultLanguage.toString());
            }
            return defaultLanguage;
        }
        if (outputBestDesired != null) {
            outputBestDesired.value = bestDesiredLocale;
        }
        // pick exact match if there is one
        if (bestSupportedLocales.contains(bestDesiredLocale)) {
            if (TRACE_MATCHER) {
              System.err.printf(
                  "Returning %s which matches a supported language\n", bestDesiredLocale.toString());
            }
            return bestDesiredLocale;
        }
        // otherwise return first supported, combining variants and extensions from bestDesired
        ULocale result = bestSupportedLocales.iterator().next();
        if (TRACE_MATCHER) {
          System.err.printf("Returning first supported language %s\n", result.toString());
        }
        return result;
    }

    /**
     * Get the best match between the desired languages and supported languages
     * @param desiredLocale the supplied user's language.
     * @param outputBestDesired The one of the desired languages that matched best.
     * Set to null if the best match was not below the threshold distance.
     * @return the best match.
     */
    public ULocale getBestMatch(ULocale desiredLocale, Output<ULocale> outputBestDesired) {
        int bestDistance = Integer.MAX_VALUE;
        ULocale bestDesiredLocale = null;
        Collection<ULocale> bestSupportedLocales = null;

        // quick check for exact match, with hack for und
        final LSR desiredLSR = desiredLocale.equals(UND_LOCALE) ? UND : LSR.fromMaximalized(desiredLocale);

        if (exactSupportedLocales.contains(desiredLocale)) {
            if (outputBestDesired != null) {
                outputBestDesired.value = desiredLocale;
            }
            if (TRACE_MATCHER) {
              System.err.printf("Exact match with a supported locale.\n");
            }
            return desiredLocale;
        }
        // quick check for maximized locale
        if (distanceOption == DistanceOption.REGION_FIRST) {
            Collection<ULocale> found = supportedLanguages.get(desiredLSR);
            if (found != null) {
                // if we find one in the set, return first (lowest). We already know the exact one isn't there.
                if (outputBestDesired != null) {
                    outputBestDesired.value = desiredLocale;
                }
                ULocale result = found.iterator().next();
                if (TRACE_MATCHER) {
                  System.err.printf("Matches a maximized supported locale: %s\n", result);
                }
                return result;
            }
        }
        for (final Entry<LSR, Set<ULocale>> supportedLsrAndLocale : supportedLanguages.entrySet()) {
            int distance = localeDistance.distanceRaw(desiredLSR, supportedLsrAndLocale.getKey(),
                thresholdDistance, distanceOption);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestDesiredLocale = desiredLocale;
                bestSupportedLocales = supportedLsrAndLocale.getValue();
                if (distance == 0) {
                    break;
                }
            }
        }
        if (bestDistance >= thresholdDistance) {
            if (outputBestDesired != null) {
                outputBestDesired.value = null;
            }
            if (TRACE_MATCHER) {
              System.err.printf(
                  "Returning default %s because everything exceeded the threshold of %d.\n",
                  defaultLanguage, thresholdDistance);
            }
            return defaultLanguage;
        }
        if (outputBestDesired != null) {
            outputBestDesired.value = bestDesiredLocale;
        }
        // pick exact match if there is one
        if (bestSupportedLocales.contains(bestDesiredLocale)) {
            return bestDesiredLocale;
        }
        // otherwise return first supported, combining variants and extensions from bestDesired
        ULocale result = bestSupportedLocales.iterator().next();
        if (TRACE_MATCHER) {
          System.err.printf("First in the list of supported locales: %s\n", result);
        }
        return result;
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
        return localeDistance.distanceRaw(
            LSR.fromMaximalized(desired),
            LSR.fromMaximalized(supported), thresholdDistance, distanceOption);
    }

    /** Convenience method */
    public int distance(String desiredLanguage, String supportedLanguage) {
        return localeDistance.distanceRaw(
            LSR.fromMaximalized(new ULocale(desiredLanguage)),
            LSR.fromMaximalized(new ULocale(supportedLanguage)),
            thresholdDistance, distanceOption);
    }

    @Override
    public String toString() {
        return exactSupportedLocales.toString();
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
