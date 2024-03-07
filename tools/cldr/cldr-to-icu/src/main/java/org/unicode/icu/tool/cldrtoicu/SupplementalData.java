// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.function.Function.identity;
import static org.unicode.cldr.api.AttributeKey.keyOf;
import static org.unicode.cldr.api.CldrData.PathOrder.ARBITRARY;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.unicode.cldr.api.AttributeKey;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.PathMatcher;

import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

/**
 * Auxiliary APIs for processing locale IDs and other supplemental data needed by business logic
 * in some mapper classes.
 *
 * When a {@link SupplementalData} instance is used in a mapper class, it is imperative that it is
 * build using the same underlying CLDR data. The only reason mapper classes do not create their
 * own instances directly is the relative cost of processing all the supplemental data each time.
 */
// TODO: This should be moved into the API and leverage some of the existing utility functions.
public final class SupplementalData {
    // Special IDs which are not supported via CLDR, but for which synthetic data is injected.
    // The "TRADITIONAL" variants are here because their calendar differs from the non-variant
    // locale. However CLDR cannot represent this currently because calendar defaults are in
    // supplemental data (rather than locale data) and are keyed only on territory.
    private static final ImmutableSet<String> PHANTOM_LOCALE_IDS =
        ImmutableSet.of("ja_JP_TRADITIONAL", "th_TH_TRADITIONAL");

    private static final Pattern SCRIPT_SUBTAG = Pattern.compile("[A-Z][a-z]{3}");

    private static final PathMatcher ALIAS =
        PathMatcher.of("//supplementalData/metadata/alias/*[@type=*]");

    private static final PathMatcher PARENT_LOCALE =
        PathMatcher.of("//supplementalData/parentLocales/parentLocale[@parent=*]");
    private static final AttributeKey COMPONENT = keyOf("parentLocales", "component");
    private static final AttributeKey PARENT = keyOf("parentLocale", "parent");
    private static final AttributeKey LOCALES = keyOf("parentLocale", "locales");

    private static final PathMatcher CALENDER_PREFERENCE =
        PathMatcher.of("//supplementalData/calendarPreferenceData/calendarPreference[@territories=*]");
    private static final AttributeKey CALENDER_TERRITORIES =
        keyOf("calendarPreference", "territories");
    private static final AttributeKey CALENDER_ORDERING =
        keyOf("calendarPreference", "ordering");

    private static final PathMatcher LIKELY_SUBTAGS =
        PathMatcher.of("//supplementalData/likelySubtags/likelySubtag[@from=*]");
    private static final AttributeKey SUBTAG_FROM = keyOf("likelySubtag", "from");
    private static final AttributeKey SUBTAG_TO = keyOf("likelySubtag", "to");

    private static final Splitter LIST_SPLITTER =
        Splitter.on(whitespace()).omitEmptyStrings();

    // Aliases come in three flavours. Note that the TERRITORY aliases map to a _list_ rather than
    // a single value (it's structurally always a list, but only territory aliases have a need for
    // more than one value).
    private enum Alias {
        LANGUAGE, SCRIPT, TERRITORY;

        private static final ImmutableMap<String, Alias> TYPE_MAP =
            Arrays.stream(values())
                .collect(toImmutableMap(a -> Ascii.toLowerCase(a.name()) + "Alias", identity()));

        private final String elementName = Ascii.toLowerCase(name()) + "Alias";
        final AttributeKey typeKey = AttributeKey.keyOf(elementName, "type");
        final AttributeKey replacementKey = AttributeKey.keyOf(elementName, "replacement");

        static Optional<Alias> forElementName(String name) {
            return Optional.ofNullable(TYPE_MAP.get(name));
        }
    }

    /**
     * Creates a supplemental data API instance from the given CLDR data supplier.
     *
     * @param src the CLDR data supplier.
     * @return the supplemental data API.
     */
    public static SupplementalData create(CldrDataSupplier src) {
        Table<Alias, String, String> aliasTable = HashBasedTable.create();
        Map<String, String> parentLocaleMap = new HashMap<>();
        Map<String, String> defaultCalendarMap = new HashMap<>();
        Map<String, String> likelySubtagMap = new HashMap<>();

        src.getDataForType(CldrDataType.SUPPLEMENTAL).accept(
            ARBITRARY,
            v -> {
                if (ALIAS.matches(v.getPath())) {
                    // Territory alias replacements can be a list of values (e.g. when countries
                    // break up). We use the first (geo-politically most significant) value. This
                    // doesn't happen for languages or scripts, but could in theory.
                    Alias.forElementName(v.getPath().getName()).ifPresent(
                        alias -> aliasTable.put(
                            alias,
                            alias.typeKey.valueFrom(v),
                            alias.replacementKey.valueFrom(v)));
                } else if (PARENT_LOCALE.matches(v.getPath()) && !COMPONENT.optionalValueFrom(v).isPresent()) {
                    // CLDR-16253 added component-specific parents, which we ignore for now.
                    // TODO(ICU-22289): Handle these properly.
                    String p = PARENT.valueFrom(v);
                    LOCALES.listOfValuesFrom(v).forEach(c -> parentLocaleMap.put(c, p));
                } else if (CALENDER_PREFERENCE.matches(v.getPath())) {
                    String c = CALENDER_ORDERING.listOfValuesFrom(v).get(0);
                    CALENDER_TERRITORIES.listOfValuesFrom(v).forEach(t -> defaultCalendarMap.put(t, c));
                } else if (LIKELY_SUBTAGS.matches(v.getPath())) {
                    likelySubtagMap.put(SUBTAG_FROM.valueFrom(v), SUBTAG_TO.valueFrom(v));
                }
            });

        Set<String> availableIds = Sets.union(src.getAvailableLocaleIds(), PHANTOM_LOCALE_IDS);
        return new SupplementalData(
            availableIds, aliasTable, parentLocaleMap, defaultCalendarMap, likelySubtagMap);
    }

    // A simple-as-possible, mutable, locale ID data "struct" to handle the IDs used during ICU
    // data generation. Because this is mutable, it is thoroughly unsuitable for general use.
    private static final class LocaleId {
        // From: https://unicode.org/reports/tr35/#Identifiers
        // Locale ID is:
        //   (<language>(_<script>)?|<script>)(_<region>)?(_<variant>)*
        //
        // However in CLDR data, there's always a language (even if it's "und"), and never more
        // than one variant, so this can be simplified to:
        //   <language>(_<script>)?(_<region>)?(_<variant>)?
        //
        // * Required language is lowercase 2 or 3 letter language ID (e.g. "en", "gsw").
        //   Note that the specification allows for languages 5-8 characters long, but in reality
        //   this has never occurred yet, so it's ignored in this code.
        //
        // * Script is 4-letter Xxxx script identifier (e.g. "Latn").
        //   The specification permits any casing for script subtags, but since all the data uses
        //   the capitalized "Xxxx" form, that's what this code expects.
        //
        // * Region is the uppercase 2-letter CLDR region code ("GB") or the 3-digit numeric
        //   identifier (e.g. "001").
        //
        // * Variants are a bit complex; either 5-8 length alphanumerics, or length 4 but starting
        //   with a digit (this avoids any ambiguity with script subtags). However because ICU
        //   violates this rule by using "TRADITIONAL" (11-letters) the length restriction is
        //   merely "longer than 5".
        //
        // Finaly, CLDR data only uses an '_' as the separator, whereas the specification allows
        // for either '-' or '_').
        //
        // The regex for unambiguously capturing the parts of a locale ID from the CLDR data is:
        private static final Pattern LOCALE_ID =
            Pattern.compile("([a-z]{2,3})"
                + "(?:_([A-Z][a-z]{3}))?"
                + "(?:_([A-Z]{2}|[0-9]{3}))?"
                + "(?:_([a-zA-Z]{5,}|[0-9][a-zA-Z0-9]{3}))?");

        static LocaleId parse(String localeId) {
            Matcher m = LOCALE_ID.matcher(checkNotNull(localeId, "locale ID cannot be null"));
            checkArgument(m.matches(), "invalid locale ID: %s", localeId);
            return of(m.group(1), m.group(2), m.group(3)).setVariant(m.group(4));
        }

        static LocaleId of(String language, String script, String region) {
            return new LocaleId().setLanguage(language).setScript(script).setRegion(region);
        }

        // Only the language subtag is non-nullable.
        private String languageSubtag;
        private String scriptSubtag;
        private String regionSubtag;
        private String variantSubtag;

        String getLanguage() {
            return languageSubtag;
        }

        String getScript() {
            return scriptSubtag;
        }

        String getRegion() {
            return regionSubtag;
        }

        String getVariant() {
            return variantSubtag;
        }

        LocaleId setLanguage(String languageSubtag) {
            checkNotNull(languageSubtag, "language subtag must not be null");
            checkArgument(!languageSubtag.isEmpty(), "language subtag must not be empty");
            this.languageSubtag = languageSubtag;
            return this;
        }

        LocaleId setScript(String scriptSubtag) {
            this.scriptSubtag = Strings.emptyToNull(scriptSubtag);
            return this;
        }

        LocaleId setRegion(String regionSubtag) {
            this.regionSubtag = Strings.emptyToNull(regionSubtag);
            return this;
        }

        LocaleId setVariant(String variantSubtag) {
            this.variantSubtag = Strings.emptyToNull(variantSubtag);
            return this;
        }

        @Override public String toString() {
            StringBuilder id = new StringBuilder(languageSubtag);
            if (scriptSubtag != null) {
                id.append("_").append(scriptSubtag);
            }
            if (regionSubtag != null) {
                id.append("_").append(regionSubtag);
            }
            if (variantSubtag != null) {
                id.append("_").append(variantSubtag);
            }
            return id.toString();
        }

        @Override public boolean equals(Object o) {
            if (!(o instanceof LocaleId)) {
                return false;
            }
            LocaleId other = (LocaleId) o;
            return Objects.equals(languageSubtag, other.languageSubtag)
                && Objects.equals(scriptSubtag, other.scriptSubtag)
                && Objects.equals(regionSubtag, other.regionSubtag)
                && Objects.equals(variantSubtag, other.variantSubtag);
        }

        @Override public int hashCode() {
            return Objects.hash(languageSubtag, scriptSubtag, regionSubtag, variantSubtag);
        }
    }

    private final ImmutableSet<String> availableIds;
    private final ImmutableTable<Alias, String, String> aliasTable;
    private final ImmutableMap<String, String> parentLocaleMap;
    private final ImmutableMap<String, String> defaultCalendarMap;
    private final ImmutableMap<String, String> likelySubtagMap;

    private SupplementalData(
        Set<String> availableIds,
        Table<Alias, String, String> aliasTable,
        Map<String, String> parentLocaleMap,
        Map<String, String> defaultCalendarMap,
        Map<String, String> likelySubtagMap) {

        this.availableIds = ImmutableSet.copyOf(availableIds);
        this.aliasTable = ImmutableTable.copyOf(aliasTable);
        this.parentLocaleMap = ImmutableMap.copyOf(parentLocaleMap);
        this.defaultCalendarMap = ImmutableMap.copyOf(defaultCalendarMap);
        this.likelySubtagMap = ImmutableMap.copyOf(likelySubtagMap);
    }

    public ImmutableSet<String> getAvailableLocaleIds() {
        return availableIds;
    }

    /**
     * Returns the "maximized" form of a given locale ID, by adding likely subtags where possible.
     */
    public Optional<String> maximize(String localeId) {
        return addLikelySubtags(localeId).map(Object::toString);
    }

    /**
     * Returns the locale ID with any deprecated elements replaced. This is an
     * implementation of the algorithm specified in
     * <a href="http://unicode.org/reports/tr35/#Canonical_Unicode_Locale_Identifiers">the LDML
     * specification</a> but without any "minimizing" of the final result (as happens for
     * canonicalization in the CLDR tools).
     */
    public String replaceDeprecatedTags(String localeId) {
        if (localeId.equals("root")) {
            return localeId;
        }
        LocaleId id = LocaleId.parse(localeId);

        // ---- LDML Specification ----
        // If the region subtag matches the type attribute of a territoryAlias element in
        // Supplemental Data, replace the region subtag with the replacement value, as follows:
        //
        // * If there is a single territory in the replacement, use it.
        // * If there are multiple territories:
        //   * Look up the most likely territory for the base language code (and script, if there
        //     is one).
        //   * If that likely territory is in the list, use it.
        //   * Otherwise, use the first territory in the list.
        // ----
        // However there is a footnote that says:
        //   Formally, replacement of multiple territories uses Section 4.3 Likely Subtags.
        //   However, there are a small number of cases of multiple territories, so the mappings
        //   can be precomputed. This results in a faster lookup with a very small subset of the
        //   likely subtags data.
        //
        // Note that (contrary to the order implied by the LDML specification) this step is
        // performed _before_ the language alias lookup. This is to allow ID such as "sr_YU" to
        // work, where "YU" should be replaced with "RS" and _then_ "sr_RS" is expanded to
        // "sr_Cryl_RS" by the language alias lookup. In the other order, you just get "sr_RS" out.
        //
        // TODO: Can we simplify this my just using "addLikelySubtags()" when region is missing?
        if (id.getRegion() != null) {
            String replacementRegions = aliasTable.get(Alias.TERRITORY, id.getRegion());
            if (replacementRegions != null) {
                List<String> regions = LIST_SPLITTER.splitToList(replacementRegions);
                checkArgument(!regions.isEmpty(), "invalid empty region list for %s", localeId);
                if (regions.size() == 1) {
                    id.setRegion(regions.get(0));
                } else {
                    LocaleId key = LocaleId.of(id.getLanguage(), id.getScript(), null);
                    String likelyId = likelySubtagMap.get(key.toString());
                    if (likelyId == null) {
                        likelyId = likelySubtagMap.get(key.setScript(null).toString());
                    }
                    String likelyRegion =
                        likelyId != null ? LocaleId.parse(likelyId).getRegion() : null;
                    if (regions.contains(likelyRegion)) {
                        id.setRegion(likelyRegion);
                    } else {
                        id.setRegion(regions.get(0));
                    }
                }
            }
        }

        // While it's not mentioned in the LDML specification, there is data in the alias table for
        // replacement scripts (currently it contains exactly one entry with one value). Because
        // its not clear if this is intended to only be single values or a list (and how to handle
        // it if it were a list), there's a hard check to ensure it's only ever a single value.
        if (id.getScript() != null) {
            String replacementScript = aliasTable.get(Alias.SCRIPT, id.getScript());
            if (replacementScript != null) {
                checkArgument(whitespace().matchesNoneOf(replacementScript),
                    "unexpected list of replacement scripts: %s", replacementScript);
                id.setScript(replacementScript);
            }
        }

        // ---- LDML Specification ----
        // If the language subtag matches the type attribute of a languageAlias element in
        // Supplemental Data, replace the language subtag with the replacement value.
        //
        // If there are additional subtags in the replacement value, add them to the result, but
        // only if there is no corresponding subtag already in the tag.
        // ----
        // Contrary to the precise wording of the specification, we don't just check the language
        // subtag, since language aliases can contain script and even region information. Instead
        // we check the alias table using the same order as defined in subtag maximizing:
        //
        // <language>_<script>_<region>
        // <language>_<region>
        // <language>_<script>
        // <language>
        //
        // There is no need to check for "und" however since that's not aliased anything, but since
        // it shares the same code it's harmless to do.
        resolveLocaleId(id, s -> aliasTable.get(Alias.LANGUAGE, s))
            .ifPresent(resolvedId -> {
                id.setLanguage(checkNotNull(resolvedId.getLanguage(),
                     "missing language subtag in language alias: %s", resolvedId));
                if (id.getScript() == null) {
                    id.setScript(resolvedId.getScript());
                }
                if (id.getRegion() == null) {
                    id.setRegion(resolvedId.getRegion());
                }
                if (id.getVariant() == null) {
                    id.setVariant(resolvedId.getVariant());
                }
            });
        return id.toString();
    }

    /**
     * Returns a suitable default calendar for a given locale if it's different from the default
     * calendar inferred by the locale's parent.
     *
     * <p>Note that since the default calendar data is keyed from territory (region subtag) rather
     * than the complete locale ID, it is impossible to encode some real life cases (e.g. the fact
     * that "ja_JP_TRADITIONAL" has a different default calendar to "ja_JP"). This is currently
     * handled with hard-code special casing, but should probably be data driven eventually.
     */
    public Optional<String> getDefaultCalendar(String localeId) {
        Optional<String> calendar = getSpecialCaseCalendar(localeId);
        if (calendar.isPresent()) {
            return calendar;
        }
        String t = territoryOf(localeId);
        calendar = Optional.ofNullable(defaultCalendarMap.get(t));
        if (!calendar.isPresent()) {
            return Optional.empty();
        }
        String rootCalendar = defaultCalendarMap.get("001");
        checkState(!rootCalendar.isEmpty(), "missing root calendar");
        if (localeId.equals("root")) {
            return Optional.of(rootCalendar);
        }
        // All locales reach "root" eventually, and that maps to territory "001" which
        // we already know has a value, so this loop *must* exit.
        String parentCalendar;
        do {
            localeId = getParent(localeId);
            String territory = territoryOf(localeId);
            parentCalendar = defaultCalendarMap.get(territory);
        } while (parentCalendar == null);
        return parentCalendar.equals(calendar.get()) ? Optional.empty() : calendar;
    }

    // Hack to work around the limitation that CLDR data cannot represent default calendars that
    // change because of non-territory information. Since this is limited to exactly two cases at
    // the moment, and is unlikely to be expanded, it's being done directly in code.
    private Optional<String> getSpecialCaseCalendar(String localeId) {
        Optional<String> maximized = maximize(localeId);
        if (maximized.isPresent()) {
            switch (maximized.get()) {
            case "ja_Jpan_JP_TRADITIONAL":
                return Optional.of("japanese");
            case "th_Thai_TH_TRADITIONAL":
                return Optional.of("buddhist");
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the parent of a non-root locale ID. This is more complex than simple truncation for
     * two reasons:
     * <ul>
     *     <li>There may be an explicit parent locale ID specified in the CLDR data.
     *     <li>Removal of non-default script subtags makes the parent locale "root" (unless there
     *         was an explicit parent specified).
     * </ul>
     * Note that all valid locale ID parent "chains" must end up at "root" eventually.
     *
     * For example (showing parent "chains"):
     * <ul>
     *     <li>{@code en_GB} --> {@code en_001} --> {@code en} --> {@code root}
     *     <li>{@code en_Cyrl_RU} --> {@code en_Cyrl} --> {@code root}
     * </ul>
     *
     * @throws IllegalArgumentException if the given locale ID is invalid or "root".
     */
    public String getParent(String localeId) {
        checkState(!localeId.equals("root"), "cannot ask for parent of 'root' locale");
        // We probably want to fully canonicalize here. But in the absence of that we
        // at least need to do the following canonicalization:
        if (localeId.equals("no_NO_NY")) {
            localeId = "nn_NO";
        }
        // Always defer to an explicit parent locale set in the CLDR data.
        Optional<String> explicitParent = getExplicitParentLocaleOf(localeId);
        if (explicitParent.isPresent()) {
            return explicitParent.get();
        }
        // Now look for the start of the last ID "part" in order to truncate.
        int lastPartSeperatorIndex = localeId.lastIndexOf('_');
        // The parent of a base language ID (e.g. "en" or "fr") is always "root".
        if (lastPartSeperatorIndex == -1) {
            return "root";
        }
        String parentId = localeId.substring(0, lastPartSeperatorIndex);

        // However, if the script of the locale is what's being truncated and it's NOT the default
        // script for the language, return "root" as the parent rather than truncating.
        String lastPart = localeId.substring(lastPartSeperatorIndex + 1);
        if (SCRIPT_SUBTAG.matcher(lastPart).matches() && !lastPart.equals(scriptOf(parentId))) {
            return "root";
        }
        return !parentId.isEmpty() ? parentId : "root";
    }

    /**
     * Returns the explicit parent of a locale ID if specified in the CLDR data.
     *
     * Note that this method will not return a value for most locale IDs, since they do not have
     * an explicit parent set. If you just want "normal" parent of a locale ID, use {@link
     * #getParent(String)}.
     */
    public Optional<String> getExplicitParentLocaleOf(String localeId) {
        return Optional.ofNullable(parentLocaleMap.get(localeId));
    }

    private String territoryOf(String localeId) {
        return localeId.equals("root")
            ? "001"
            : addLikelySubtags(localeId).map(LocaleId::getRegion).orElse("ZZ");
    }

    private String scriptOf(String localeId) {
        return addLikelySubtags(localeId).map(LocaleId::getScript).orElse("Zzzz");
    }

    // From: https://unicode.org/reports/tr35/#Likely_Subtags
    //
    // Add Likely Subtags
    // ------------------
    // Given a source locale X, to return a locale Y where the empty subtags have been filled in
    // by the most likely subtags. A subtag is called empty if it is a missing script or region
    // subtag, or it is a base language subtag with the value "und".
    //
    // Canonicalize
    // ------------
    // Make sure the input locale is in canonical form ...
    // ...
    // Remove the script code 'Zzzz' and the region code 'ZZ' if they occur.
    //
    // Note that this implementation does not need to handle
    // legacy language tags (marked as “Type: grandfathered” in BCP 47).
    private Optional<LocaleId> addLikelySubtags(String localeId) {
        if (localeId.equals("root")) {
            return Optional.empty();
        }

        LocaleId id = LocaleId.parse(localeId);
        // ---- LDML Specification ----
        // Remove the script code 'Zzzz' and the region code 'ZZ' if they occur.
        if ("Zzzz".equals(id.getScript())) {
            id.setScript(null);
        }
        if ("ZZ".equals(id.getRegion())) {
            id.setRegion(null);
        }
        // ---- LDML Specification ----
        // A subtag is called empty if it is a missing script or region subtag, or it is a base
        // language subtag with the value "und"
        if (!id.getLanguage().equals("und") && id.getScript() != null && id.getRegion() != null) {
            // We are already canonical, so just return.
            return Optional.of(id);
        }
        Optional<LocaleId> optTags = resolveLocaleId(id, likelySubtagMap::get);
        if (!optTags.isPresent()) {
            return Optional.empty();
        }
        LocaleId subtags = optTags.get();
        checkArgument(!subtags.getLanguage().equals("und"), "invalid subtags: %s", subtags);
        // Replace "missing" elements in the original ID with likely subtags.
        if (id.getLanguage().equals("und")) {
            id.setLanguage(subtags.getLanguage());
        }
        if (id.getScript() == null) {
            id.setScript(checkNotNull(subtags.getScript()));
        }
        if (id.getRegion() == null) {
            id.setRegion(checkNotNull(subtags.getRegion()));
        }
        // Language is not "und" and both script and region subtags are set!
        return Optional.of(id);
    }

    // From: https://unicode.org/reports/tr35/#Likely_Subtags
    //
    // Lookup
    // ------
    // Lookup each of the following in order, and stop on the first match:
    // <language>_<script>_<region>
    // <language>_<region>
    // <language>_<script>
    // <language>
    // "und"_<script>
    private Optional<LocaleId> resolveLocaleId(LocaleId id, Function<String, String> fn) {
        String lang = id.getLanguage();
        String script = id.getScript();
        String region = id.getRegion();
        Stream<LocaleId> candidateIds = Stream.of(
            LocaleId.of(lang, script, region),
            LocaleId.of(lang, null, region),
            LocaleId.of(lang, script, null),
            LocaleId.of(lang, null, null));
        // Only add "und"_<script> if there's a script, otherwise you end up maximizing "und" on
        // its own ("en_Latn_US") which is not intended.
        if (script != null) {
            candidateIds = Stream.concat(candidateIds, Stream.of(LocaleId.of("und", script, null)));
        }
        return candidateIds
            // Remove duplicate IDs (keeps the first one encountered).
            .distinct()
            .map(Object::toString)
            .map(fn)
            .filter(Objects::nonNull)
            .findFirst()
            .map(LocaleId::parse);
    }
}
