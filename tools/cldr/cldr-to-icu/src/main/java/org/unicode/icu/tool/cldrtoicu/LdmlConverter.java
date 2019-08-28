// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static org.unicode.cldr.api.CldrDataType.BCP47;
import static org.unicode.cldr.api.CldrDataType.LDML;
import static org.unicode.cldr.api.CldrDataType.SUPPLEMENTAL;
import static org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir.BRKITR;
import static org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir.COLL;
import static org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir.CURR;
import static org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir.LANG;
import static org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir.LOCALES;
import static org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir.RBNF;
import static org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir.REGION;
import static org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir.UNIT;
import static org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir.ZONE;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir;
import org.unicode.icu.tool.cldrtoicu.mapper.Bcp47Mapper;
import org.unicode.icu.tool.cldrtoicu.mapper.BreakIteratorMapper;
import org.unicode.icu.tool.cldrtoicu.mapper.CollationMapper;
import org.unicode.icu.tool.cldrtoicu.mapper.DayPeriodsMapper;
import org.unicode.icu.tool.cldrtoicu.mapper.LocaleMapper;
import org.unicode.icu.tool.cldrtoicu.mapper.PluralRangesMapper;
import org.unicode.icu.tool.cldrtoicu.mapper.PluralsMapper;
import org.unicode.icu.tool.cldrtoicu.mapper.RbnfMapper;
import org.unicode.icu.tool.cldrtoicu.mapper.SupplementalMapper;
import org.unicode.icu.tool.cldrtoicu.mapper.TransformsMapper;
import org.unicode.icu.tool.cldrtoicu.regex.RegexTransformer;

import com.google.common.base.CharMatcher;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;

/**
 * The main converter tool for CLDR to ICU data. To run this tool, you need to supply a suitable
 * {@link LdmlConverterConfig} instance. There is a simple {@code main()} method available in this
 * class which can be invoked passing just the desired output directory and which relies on the
 * presence of several system properties for the remainder of its parameters:
 * <ul>
 *     <li>CLDR_DIR: The root of the CLDR release from which CLDR data is read.
 *     <li>ICU_DIR: The root of the ICU release from which additional "specials" XML data is read.
 *     <li>CLDR_DTD_CACHE: A temporary directory with the various DTDs cached (this is a legacy
 *         requirement from the underlying CLDR libraries and might go away one day).
 * </ul>
 */
public final class LdmlConverter {
    // TODO: Do all supplemental data in one go and split similarly to locale data (using RbPath).
    private static final PathMatcher GENDER_LIST_PATHS =
        supplementalMatcher("gender");
    private static final PathMatcher LIKELY_SUBTAGS_PATHS =
        supplementalMatcher("likelySubtags");
    private static final PathMatcher METAZONE_PATHS =
        supplementalMatcher("metaZones", "primaryZones");
    private static final PathMatcher METADATA_PATHS =
        supplementalMatcher("metadata");
    private static final PathMatcher SUPPLEMENTAL_DATA_PATHS =
        supplementalMatcher(
            "calendarData",
            "calendarPreferenceData",
            "codeMappings",
            "codeMappingsCurrency",
            "idValidity",
            "languageData",
            "languageMatching",
            "measurementData",
            "parentLocales",
            "subdivisionContainment",
            "territoryContainment",
            "territoryInfo",
            "timeData",
            "unitPreferenceData",
            "weekData",
            "weekOfPreference");
    private static final PathMatcher CURRENCY_DATA_PATHS =
        supplementalMatcher("currencyData");
    private static final PathMatcher NUMBERING_SYSTEMS_PATHS =
        supplementalMatcher("numberingSystems");
    private static final PathMatcher WINDOWS_ZONES_PATHS =
        supplementalMatcher("windowsZones");

    // Special IDs which are not supported via CLDR, but for which synthetic data is injected.
    // The "TRADITIONAL" variants are here because their calendar differs from the non-variant
    // locale. However CLDR cannot represent this currently because calendar defaults are in
    // supplemental data (rather than locale data) and are keyed only on territory.
    private static final ImmutableSet<String> PHANTOM_LOCALE_IDS =
        ImmutableSet.of("ja_JP_TRADITIONAL", "th_TH_TRADITIONAL");

    // Special alias mapping which exists in ICU even though "no_NO_NY" is simply not a
    // structurally valid locale ID. This is injected manually when creating the alias map.
    // This does mean that nobody can ever parse the _keys_ of the alias map, but so far there
    // has been no need for that.
    // TODO: Get "ars" into CLDR and remove this hack.
    private static final Map<String, String> PHANTOM_ALIASES =
        ImmutableMap.of("ars", "ar_SA", "no_NO_NY", "nn_NO");

    private static PathMatcher supplementalMatcher(String... spec) {
        checkArgument(spec.length > 0, "must supply at least one matcher spec");
        if (spec.length == 1) {
            return PathMatcher.of("supplementalData/" + spec[0]);
        }
        return PathMatcher.anyOf(
            Arrays.stream(spec)
                .map(s -> PathMatcher.of("supplementalData/" + s))
                .toArray(PathMatcher[]::new));
    }

    private static RbPath RB_PARENT = RbPath.of("%%Parent");
    // The quotes below are only so we achieve parity with the manually written alias files.
    // TODO: Remove unnecessary quotes once the migration to this code is complete.
    private static RbPath RB_ALIAS = RbPath.of("\"%%ALIAS\"");
    // Special path for adding to empty files which only exist to complete the parent chain.
    // TODO: Confirm that this has no meaningful effect and unify "empty" file contents.
    private static RbPath RB_EMPTY_ALIAS = RbPath.of("___");

    /** Provisional entry point until better config support exists. */
    public static void main(String... args) {
        convert(IcuConverterConfig.builder()
            .setOutputDir(Paths.get(args[0]))
            .setEmitReport(true)
            .build());
    }

    /**
     * Output types defining specific subsets of the ICU data which can be converted separately.
     * This closely mimics the original "NewLdml2IcuConverter" behaviour but could be simplified to
     * hide what are essentially implementation specific data splits.
     */
    public enum OutputType {
        LOCALES(LDML, LdmlConverter::processLocales),
        BRKITR(LDML, LdmlConverter::processBrkitr),
        COLL(LDML, LdmlConverter::processCollation),
        RBNF(LDML, LdmlConverter::processRbnf),

        DAY_PERIODS(
            SUPPLEMENTAL,
            LdmlConverter::processDayPeriods),
        GENDER_LIST(
            SUPPLEMENTAL,
            c -> c.processSupplemental("genderList", GENDER_LIST_PATHS, "misc", false)),
        LIKELY_SUBTAGS(
            SUPPLEMENTAL,
            c -> c.processSupplemental("likelySubtags", LIKELY_SUBTAGS_PATHS, "misc", false)),
        SUPPLEMENTAL_DATA(
            SUPPLEMENTAL,
            c -> c.processSupplemental("supplementalData", SUPPLEMENTAL_DATA_PATHS, "misc", true)),
        CURRENCY_DATA(
            SUPPLEMENTAL,
            c -> c.processSupplemental("supplementalData", CURRENCY_DATA_PATHS, "curr", true)),
        METADATA(
            SUPPLEMENTAL,
            c -> c.processSupplemental("metadata", METADATA_PATHS, "misc", false)),
        META_ZONES(
            SUPPLEMENTAL,
            c -> c.processSupplemental("metaZones", METAZONE_PATHS, "misc", false)),
        NUMBERING_SYSTEMS(
            SUPPLEMENTAL,
            c -> c.processSupplemental("numberingSystems", NUMBERING_SYSTEMS_PATHS, "misc", false)),
        PLURALS(
            SUPPLEMENTAL,
            LdmlConverter::processPlurals),
        PLURAL_RANGES(
            SUPPLEMENTAL,
            LdmlConverter::processPluralRanges),
        WINDOWS_ZONES(
            SUPPLEMENTAL,
            c -> c.processSupplemental("windowsZones", WINDOWS_ZONES_PATHS, "misc", false)),
        TRANSFORMS(
            SUPPLEMENTAL,
            LdmlConverter::processTransforms),
        KEY_TYPE_DATA(
            BCP47,
            LdmlConverter::processKeyTypeData),

        // Batching by type.
        DTD_LDML(LDML, c -> c.processAll(LDML)),
        DTD_SUPPLEMENTAL(SUPPLEMENTAL, c -> c.processAll(SUPPLEMENTAL)),
        DTD_BCP47(BCP47, c -> c.processAll(BCP47));

        public static final ImmutableSet<OutputType> ALL =
            ImmutableSet.of(DTD_BCP47, DTD_SUPPLEMENTAL, DTD_LDML);

        private final CldrDataType type;
        private final Consumer<LdmlConverter> converterFn;

        OutputType(CldrDataType type, Consumer<LdmlConverter> converterFn) {
            this.type = checkNotNull(type);
            this.converterFn = checkNotNull(converterFn);
        }

        void convert(LdmlConverter converter) {
            converterFn.accept(converter);
        }

        CldrDataType getCldrType() {
            return type;
        }
    }

    /** Converts CLDR data according to the given configuration. */
    public static void convert(LdmlConverterConfig config) {
        CldrDataSupplier src = CldrDataSupplier
            .forCldrFilesIn(config.getCldrDirectory())
            .withDraftStatusAtLeast(config.getMinimumDraftStatus());
        new LdmlConverter(config, src).convertAll(config);
    }

    // The configuration controlling conversion behaviour.
    private final LdmlConverterConfig config;
    // The supplier for all data to be converted.
    private final CldrDataSupplier src;
    // The set of available locale IDs.
    // TODO: Make available IDs include specials files (or fail if specials are not available).
    private final ImmutableSet<String> availableIds;
    // Supplemental data available to mappers if needed.
    private final SupplementalData supplementalData;
    // Transformer for locale data.
    private final PathValueTransformer localeTransformer;
    // Transformer for supplemental data.
    private final PathValueTransformer supplementalTransformer;
    // Header string to go into every ICU data file.
    private final ImmutableList<String> icuFileHeader;

    private LdmlConverter(LdmlConverterConfig config, CldrDataSupplier src) {
        this.config = checkNotNull(config);
        this.src = checkNotNull(src);
        this.supplementalData = SupplementalData.create(src.getDataForType(SUPPLEMENTAL));
        // Sort the set of available locale IDs but add "root" at the front. This is the
        // set of non-alias locale IDs to be processed.
        Set<String> localeIds = new LinkedHashSet<>();
        localeIds.add("root");
        localeIds.addAll(
            Sets.intersection(src.getAvailableLocaleIds(), config.getTargetLocaleIds(LOCALES)));
        localeIds.addAll(PHANTOM_LOCALE_IDS);
        this.availableIds = ImmutableSet.copyOf(localeIds);

        // Load the remaining path value transformers.
        this.supplementalTransformer =
            RegexTransformer.fromConfigLines(readLinesFromResource("/ldml2icu_supplemental.txt"),
                IcuFunctions.ALGORITHM_FN,
                IcuFunctions.DATE_FN,
                IcuFunctions.DAY_NUMBER_FN,
                IcuFunctions.EXP_FN,
                IcuFunctions.YMD_FN);
        this.localeTransformer =
            RegexTransformer.fromConfigLines(readLinesFromResource("/ldml2icu_locale.txt"),
                IcuFunctions.CONTEXT_TRANSFORM_INDEX_FN);
        this.icuFileHeader = ImmutableList.copyOf(readLinesFromResource("/ldml2icu_header.txt"));
    }

    private void convertAll(LdmlConverterConfig config) {
        ListMultimap<CldrDataType, OutputType> groupByType = LinkedListMultimap.create();
        for (OutputType t : config.getOutputTypes()) {
            groupByType.put(t.getCldrType(), t);
        }
        for (CldrDataType cldrType : groupByType.keySet()) {
            for (OutputType t : groupByType.get(cldrType)) {
                t.convert(this);
            }
        }
        if (config.emitReport()) {
            System.out.println("Supplemental Data Transformer=" + supplementalTransformer);
            System.out.println("Locale Data Transformer=" + localeTransformer);
        }
    }

    private static List<String> readLinesFromResource(String name) {
        try (InputStream in = LdmlConverter.class.getResourceAsStream(name)) {
            return CharStreams.readLines(new InputStreamReader(in));
        } catch (IOException e) {
            throw new RuntimeException("cannot read resource: " + name, e);
        }
    }

    private PathValueTransformer getLocaleTransformer() {
        return localeTransformer;
    }

    private PathValueTransformer getSupplementalTransformer() {
        return supplementalTransformer;
    }

    private void processAll(CldrDataType cldrType) {
        List<OutputType> targets = Arrays.stream(OutputType.values())
            .filter(t -> t.getCldrType().equals(cldrType))
            .filter(t -> !t.name().startsWith("DTD_"))
            .collect(toList());
        for (OutputType t : targets) {
            t.convert(this);
        }
    }

    private Optional<CldrData> loadSpecialsData(String localeId) {
        String expected = localeId + ".xml";
        try (Stream<Path> files = Files.walk(config.getSpecialsDir())) {
            Set<Path> xmlFiles = files
                .filter(Files::isRegularFile)
                .filter(f -> f.getFileName().toString().equals(expected))
                .collect(Collectors.toSet());
            return !xmlFiles.isEmpty()
                ? Optional.of(
                CldrDataSupplier.forCldrFiles(LDML, config.getMinimumDraftStatus(), xmlFiles))
                : Optional.empty();
        } catch (IOException e) {
            throw new RuntimeException(
                "error processing specials directory: " + config.getSpecialsDir(), e);
        }
    }

    private void processLocales() {
        // TODO: Pre-load specials files to avoid repeatedly re-loading them.
        processAndSplitLocaleFiles(
            id -> LocaleMapper.process(
                id, src, loadSpecialsData(id), getLocaleTransformer(), supplementalData),
            CURR, LANG, LOCALES, REGION, UNIT, ZONE);
    }

    private void processBrkitr() {
        processAndSplitLocaleFiles(
            id -> BreakIteratorMapper.process(id, src, loadSpecialsData(id)), BRKITR);
    }

    private void processCollation() {
        processAndSplitLocaleFiles(
            id -> CollationMapper.process(id, src, loadSpecialsData(id)), COLL);
    }

    private void processRbnf() {
        processAndSplitLocaleFiles(
            id -> RbnfMapper.process(id, src, loadSpecialsData(id)), RBNF);
    }

    private void processAndSplitLocaleFiles(
        Function<String, IcuData> icuFn, IcuLocaleDir... splitDirs) {

        SetMultimap<IcuLocaleDir, String> writtenLocaleIds = HashMultimap.create();
        Path baseDir = config.getOutputDir();

        for (String id : config.getTargetLocaleIds(LOCALES)) {
            // Skip "target" IDs that are aliases (they are handled later).
            if (!availableIds.contains(id)) {
                continue;
            }
            IcuData icuData = icuFn.apply(id);

            ListMultimap<IcuLocaleDir, RbPath> splitPaths = LinkedListMultimap.create();
            for (RbPath p : icuData.getPaths()) {
                String rootName = getBaseSegmentName(p.getSegment(0));
                splitPaths.put(LOCALE_SPLIT_INFO.getOrDefault(rootName, LOCALES), p);
            }

            // We always write base languages (even if empty).
            boolean isBaseLanguage = !id.contains("_");
            // Run through all directories (not just the keySet() of the split path map) since we
            // sometimes write empty files.
            for (IcuLocaleDir dir : splitDirs) {
                Set<String> targetIds = config.getTargetLocaleIds(dir);
                if (!targetIds.contains(id)) {
                    if (!splitPaths.get(dir).isEmpty()) {
                        System.out.format(
                            "target IDs for %s does not contain %s, but it has data: %s\n",
                            dir, id, splitPaths.get(dir));
                    }
                    continue;
                }
                Path outDir = baseDir.resolve(dir.getOutputDir());
                IcuData splitData = new IcuData(icuData.getName(), icuData.hasFallback());
                // The split data can still be empty for this directory, but that's expected.
                splitPaths.get(dir).forEach(p -> splitData.add(p, icuData.get(p)));
                // Adding a parent locale makes the data non-empty and forces it to be written.
                supplementalData.getExplicitParentLocaleOf(splitData.getName())
                    .ifPresent(p -> splitData.add(RB_PARENT, p));
                if (!splitData.isEmpty() || isBaseLanguage || dir.includeEmpty()) {
                    splitData.setVersion(CldrDataSupplier.getCldrVersionString());
                    write(splitData, outDir);
                    writtenLocaleIds.put(dir, id);
                }
            }
        }

        for (IcuLocaleDir dir : splitDirs) {
            Path outDir = baseDir.resolve(dir.getOutputDir());
            Set<String> targetIds = config.getTargetLocaleIds(dir);

            Map<String, String> aliasMap = getAliasMap(targetIds, dir);
            aliasMap.forEach((s, t) -> {
                // It's only important to record which alias files are written because of forced
                // aliases, but since it's harmless otherwise, we just do it unconditionally.
                // Normal alias files don't affect the empty file calculation, but forced ones can.
                writtenLocaleIds.put(dir, s);
                writeAliasFile(s, t, outDir);
            });

            calculateEmptyFiles(writtenLocaleIds.get(dir), aliasMap.values())
                .forEach(id -> writeEmptyFile(id, outDir, aliasMap.values()));
        }
    }

    private Map<String, String> getAliasMap(Set<String> localeIds, IcuLocaleDir dir) {
        // There are four reasons for treating a locale ID as an alias.
        // 1: It contains deprecated subtags (e.g. "sr_YU", which should be "sr_Cyrl_RS").
        // 2: It has no CLDR data but is missing a script subtag.
        // 3: It is one of the special "phantom" alias which cannot be represented normally
        //    and must be manually mapped (e.g. legacy locale IDs which don't even parse).
        // 4: It is a "super special" forced alias, which might replace existing aliases in
        //    some output directories.
        Map<String, String> aliasMap = new LinkedHashMap<>();
        for (String id : localeIds) {
            if (PHANTOM_ALIASES.keySet().contains(id)) {
                checkArgument(!availableIds.contains(id),
                    "phantom aliases should never be otherwise supported: %s\n"
                        + "(maybe the phantom alias can now be removed?)", id);
                aliasMap.put(id, PHANTOM_ALIASES.get(id));
                continue;
            }
            String canonicalId = supplementalData.replaceDeprecatedTags(id);
            if (!canonicalId.equals(id)) {
                // If the canonical form of an ID differs from the requested ID, the this is an
                // alias, and just needs to point to the canonical ID.
                aliasMap.put(id, canonicalId);
                continue;
            }
            if (availableIds.contains(id)) {
                // If it's canonical and supported, it's not an alias.
                continue;
            }
            // If the requested locale is not supported, maximize it and alias to that.
            String maximizedId = supplementalData.maximize(id)
                .orElseThrow(() -> new IllegalArgumentException("unsupported locale ID: " + id));
            // We can't alias to ourselves and we shouldn't be here is the ID was already maximal.
            checkArgument(!maximizedId.equals(id), "unsupported maximized locale ID: %s", id);
            aliasMap.put(id, maximizedId);
        }
        // Important that we overwrite entries which might already exist here, since we might have
        // already calculated a "natural" alias for something that we want to force (and we should
        // replace the existing target, since that affects how we determine empty files later).
        aliasMap.putAll(config.getForcedAliases(dir));
        return aliasMap;
    }

    private static final CharMatcher PATH_MODIFIER = CharMatcher.anyOf(":%");

    // Resource bundle paths elements can have variants (e.g. "Currencies%narrow) or type
    // annotations (e.g. "languages:intvector"). We strip these when considering the element name.
    private static String getBaseSegmentName(String segment) {
        int idx = PATH_MODIFIER.indexIn(segment);
        return idx == -1 ? segment : segment.substring(0, idx);
    }

    private void processDayPeriods() {
        write(DayPeriodsMapper.process(src), "misc");
    }

    private void processPlurals() {
        write(PluralsMapper.process(src), "misc");
    }

    private void processPluralRanges() {
        write(PluralRangesMapper.process(src), "misc");
    }

    private void processKeyTypeData() {
        Bcp47Mapper.process(src).forEach(d -> write(d, "misc"));
    }

    private void processTransforms() {
        Path transformDir = createDirectory(config.getOutputDir().resolve("translit"));
        write(TransformsMapper.process(src, transformDir), transformDir);
    }

    private static final RbPath RB_CLDR_VERSION = RbPath.of("cldrVersion");

    private void processSupplemental(
        String label, PathMatcher paths, String dir, boolean addCldrVersion) {
        IcuData icuData =
            SupplementalMapper.process(src, getSupplementalTransformer(), label, paths);
        // A hack for "supplementalData.txt" since the "cldrVersion" value doesn't come from the
        // supplemental data XML files.
        if (addCldrVersion) {
            icuData.add(RB_CLDR_VERSION, CldrDataSupplier.getCldrVersionString());
        }
        write(icuData, dir);
    }

    private void writeAliasFile(String srcId, String destId, Path dir) {
        IcuData icuData = new IcuData(srcId, true);
        icuData.add(RB_ALIAS, destId);
        write(icuData, dir);
    }

    private void writeEmptyFile(String id, Path dir, Collection<String> aliasTargets) {
        IcuData icuData = new IcuData(id, true);
        // TODO: Document the reason for this (i.e. why does it matter what goes into empty files?)
        if (aliasTargets.contains(id)) {
            icuData.setFileComment("generated alias target");
            icuData.add(RB_EMPTY_ALIAS, "");
        } else {
            // These empty files only exist because the target of an alias has a parent locale
            // which is itself not in the set of written ICU files. An "indirect alias target".
            icuData.setVersion(CldrDataSupplier.getCldrVersionString());
        }
        write(icuData, dir);
    }

    private void write(IcuData icuData, String dir) {
        write(icuData, config.getOutputDir().resolve(dir));
    }

    private void write(IcuData icuData, Path dir) {
        createDirectory(dir);
        IcuTextWriter.writeToFile(icuData, dir, icuFileHeader);
    }

    private Path createDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("cannot create directory: " + dir, e);
        }
        return dir;
    }

    // The set of IDs to process is:
    // * any file that was written
    // * any alias target (not written)
    //
    // From which we generate the complete "closure" under the "getParent()" function. This set
    // contains all file (written or not) which need to exist to complete the locale hierarchy.
    //
    // Then we remove all the written files to just leave the ones that need to be generated.
    // This is a simple and robust approach that handles things like "gaps" in non-aliased
    // locale IDs, where an intermediate parent is not present.
    private ImmutableSet<String> calculateEmptyFiles(
        Set<String> writtenIds, Collection<String> aliasTargetIds) {

        Set<String> seedIds = new HashSet<>(writtenIds);
        seedIds.addAll(aliasTargetIds);
        // Be nice and sort the output (makes easier debugging).
        Set<String> allIds = new TreeSet<>();
        for (String id : seedIds) {
            while (!id.equals("root") && !allIds.contains(id)) {
                allIds.add(id);
                id = supplementalData.getParent(id);
            }
        }
        return ImmutableSet.copyOf(Sets.difference(allIds, writtenIds));
    }

    private static final ImmutableMap<String, IcuLocaleDir> LOCALE_SPLIT_INFO =
        ImmutableMap.<String, IcuLocaleDir>builder()
            // BRKITR
            .put("boundaries", BRKITR)
            .put("dictionaries", BRKITR)
            .put("exceptions", BRKITR)
            // COLL
            .put("collations", COLL)
            .put("depends", COLL)
            .put("UCARules", COLL)
            // CURR
            .put("Currencies", CURR)
            .put("CurrencyPlurals", CURR)
            .put("CurrencyUnitPatterns", CURR)
            .put("currencySpacing", CURR)
            // LANG
            .put("Keys", LANG)
            .put("Languages", LANG)
            .put("Scripts", LANG)
            .put("Types", LANG)
            .put("Variants", LANG)
            .put("characterLabelPattern", LANG)
            .put("codePatterns", LANG)
            .put("localeDisplayPattern", LANG)
            // RBNF
            .put("RBNFRules", RBNF)
            // REGION
            .put("Countries", REGION)
            // UNIT
            .put("durationUnits", UNIT)
            .put("units", UNIT)
            .put("unitsShort", UNIT)
            .put("unitsNarrow", UNIT)
            // ZONE
            .put("zoneStrings", ZONE)
            .build();
}
