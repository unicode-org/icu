// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.RESOLVED;
import static org.unicode.cldr.api.CldrDataSupplier.CldrResolution.UNRESOLVED;
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDataType;
import org.unicode.cldr.api.CldrPath;
import org.unicode.cldr.api.PathMatcher;
import org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir;
import org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuVersionInfo;
import org.unicode.icu.tool.cldrtoicu.localedistance.LocaleDistanceMapper;
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
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
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
    private static final Predicate<CldrPath> GENDER_LIST_PATHS =
        supplementalMatcher("gender");
    private static final Predicate<CldrPath> METAZONE_PATHS =
        supplementalMatcher("metaZones", "primaryZones");
    private static final Predicate<CldrPath> METADATA_PATHS =
        supplementalMatcher("metadata");
    private static final Predicate<CldrPath> SUPPLEMENTAL_DATA_PATHS =
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
            "personNamesDefaults",
            "subdivisionContainment",
            "territoryContainment",
            "territoryInfo",
            "timeData",
            "weekData",
            "weekOfPreference");
    private static final Predicate<CldrPath> CURRENCY_DATA_PATHS =
        supplementalMatcher("currencyData");
    private static final Predicate<CldrPath> UNITS_DATA_PATHS =
        supplementalMatcher(
            "convertUnits",
            "unitConstants",
            "unitQuantities",
            "unitPreferenceData",
            "unitPrefixes");
    private static final Predicate<CldrPath> GRAMMATICAL_FEATURES_PATHS =
        supplementalMatcher("grammaticalData");
    private static final Predicate<CldrPath> NUMBERING_SYSTEMS_PATHS =
        supplementalMatcher("numberingSystems");
    private static final Predicate<CldrPath> WINDOWS_ZONES_PATHS =
        supplementalMatcher("windowsZones");

    private static Predicate<CldrPath> supplementalMatcher(String... spec) {
        checkArgument(spec.length > 0, "must supply at least one matcher spec");
        if (spec.length == 1) {
            return PathMatcher.of("//supplementalData/" + spec[0])::matchesPrefixOf;
        }
        return
            Arrays.stream(spec)
                .map(s -> PathMatcher.of("//supplementalData/" + s))
                .map(m -> ((Predicate<CldrPath>) m::matchesPrefixOf))
                .reduce(p -> false, Predicate::or);
    }

    private static RbPath RB_PARENT = RbPath.of("%%Parent");
    // The quotes below are only so we achieve parity with the manually written alias files.
    // TODO: Remove unnecessary quotes once the migration to this code is complete.
    private static RbPath RB_ALIAS = RbPath.of("\"%%ALIAS\"");
    // Special path for adding to empty files which only exist to complete the parent chain.
    // TODO: Confirm that this has no meaningful effect and unify "empty" file contents.
    private static RbPath RB_EMPTY_ALIAS = RbPath.of("___");

    /**
     * Output types defining specific subsets of the ICU data which can be converted separately.
     * This closely mimics the original "NewLdml2IcuConverter" behaviour but could be simplified to
     * hide what are essentially implementation specific data splits.
     */
    public enum OutputType {
        LOCALES(LDML),
        BRKITR(LDML),
        COLL(LDML),
        RBNF(LDML),
        DAY_PERIODS(SUPPLEMENTAL),
        GENDER_LIST(SUPPLEMENTAL),
        SUPPLEMENTAL_DATA(SUPPLEMENTAL),
        UNITS(SUPPLEMENTAL),
        CURRENCY_DATA(SUPPLEMENTAL),
        GRAMMATICAL_FEATURES(SUPPLEMENTAL),
        METADATA(SUPPLEMENTAL),
        META_ZONES(SUPPLEMENTAL),
        NUMBERING_SYSTEMS(SUPPLEMENTAL),
        PLURALS(SUPPLEMENTAL),
        PLURAL_RANGES(SUPPLEMENTAL),
        WINDOWS_ZONES(SUPPLEMENTAL),
        TRANSFORMS(SUPPLEMENTAL),
        LOCALE_DISTANCE(SUPPLEMENTAL),
        VERSION(SUPPLEMENTAL),
        KEY_TYPE_DATA(BCP47);

        public static final ImmutableSet<OutputType> ALL = ImmutableSet.copyOf(OutputType.values());

        private final CldrDataType type;

        OutputType(CldrDataType type) {
            this.type = checkNotNull(type);
        }

        CldrDataType getCldrType() {
            return type;
        }
    }

    // Map to convert the rather arbitrarily defined "output types" to the directories into which
    // the data is written. This is only for "LDML" types since other mappers don't need to split
    // data into multiple directories.
    private static final ImmutableListMultimap<OutputType, IcuLocaleDir> TYPE_TO_DIR =
        ImmutableListMultimap.<OutputType, IcuLocaleDir>builder()
            .putAll(OutputType.LOCALES, CURR, LANG, LOCALES, REGION, UNIT, ZONE)
            .putAll(OutputType.BRKITR, BRKITR)
            .putAll(OutputType.COLL, COLL)
            .putAll(OutputType.RBNF, RBNF)
            .build();

    /** Converts CLDR data according to the given configuration. */
    public static void convert(
        CldrDataSupplier src, SupplementalData supplementalData, LdmlConverterConfig config) {
        new LdmlConverter(src, supplementalData, config).convertAll();
    }

    // The supplier for all data to be converted.
    private final CldrDataSupplier src;
    // Supplemental data available to mappers if needed.
    private final SupplementalData supplementalData;
    // The configuration controlling conversion behaviour.
    private final LdmlConverterConfig config;
    // The set of expanded target locale IDs.
    // TODO: Make available IDs include specials files (or fail if specials are not available).
    private final ImmutableSet<String> availableIds;
    // Transformer for locale data.
    private final PathValueTransformer localeTransformer;
    // Transformer for supplemental data.
    private final PathValueTransformer supplementalTransformer;
    // Header string to go into every ICU data and transliteration rule file (comment prefixes
    // are not present and must be added by the code writing the file).
    private final ImmutableList<String> fileHeader;

    private LdmlConverter(
        CldrDataSupplier src, SupplementalData supplementalData, LdmlConverterConfig config) {
        this.src = checkNotNull(src);
        this.supplementalData = checkNotNull(supplementalData);
        this.config = checkNotNull(config);
        this.availableIds = ImmutableSet.copyOf(
            Sets.intersection(supplementalData.getAvailableLocaleIds(), config.getAllLocaleIds()));
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
        this.fileHeader = readLinesFromResource("/ldml2icu_header.txt");
    }

    private void convertAll() {
        processLdml();
        processSupplemental();
        if (config.emitReport()) {
            System.out.println("Supplemental Data Transformer=" + supplementalTransformer);
            System.out.println("Locale Data Transformer=" + localeTransformer);
        }
    }

    private static ImmutableList<String> readLinesFromResource(String name) {
        try (InputStream in = LdmlConverter.class.getResourceAsStream(name)) {
            return ImmutableList.copyOf(CharStreams.readLines(new InputStreamReader(in, UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException("cannot read resource: " + name, e);
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

    private void processLdml() {
        ImmutableList<IcuLocaleDir> splitDirs =
            config.getOutputTypes().stream()
                .filter(t -> t.getCldrType() == LDML)
                .flatMap(t -> TYPE_TO_DIR.get(t).stream())
                .collect(toImmutableList());
        if (splitDirs.isEmpty()) {
            return;
        }

        String cldrVersion = config.getVersionInfo().getCldrVersion();

        Map<IcuLocaleDir, DependencyGraph> graphMetadata = new HashMap<>();
        splitDirs.forEach(d -> graphMetadata.put(d, new DependencyGraph(cldrVersion)));

        SetMultimap<IcuLocaleDir, String> writtenLocaleIds = HashMultimap.create();
        Path baseDir = config.getOutputDir();

        System.out.println("processing standard ldml files");
        for (String id : config.getAllLocaleIds()) {
            // Skip "target" IDs that are aliases (they are handled later).
            if (!availableIds.contains(id)) {
                continue;
            }
            // TODO: Remove the following skip when ICU-20997 is fixed
            if (id.contains("VALENCIA") || id.contains("TARASK")) {
                System.out.println("(skipping " + id + " until ICU-20997 is fixed)");
                continue;
            }
            // Now that former CLDR see locales are in common, there are some language
            // variants that are not at a high enough coverage level to pick up.
            // TODO need a better way of handling this.
             if (id.contains("POLYTON")) {
                System.out.println("(skipping " + id + ", insufficient coverage level)");
                continue;
            }

            IcuData icuData = new IcuData(id, true);

            Optional<CldrData> specials = loadSpecialsData(id);
            CldrData unresolved = src.getDataForLocale(id, UNRESOLVED);

            BreakIteratorMapper.process(icuData, unresolved, specials);
            CollationMapper.process(icuData, unresolved, specials, cldrVersion);
            RbnfMapper.process(icuData, unresolved, specials);

            CldrData resolved = src.getDataForLocale(id, RESOLVED);
            Optional<String> defaultCalendar = supplementalData.getDefaultCalendar(id);
            LocaleMapper.process(
                icuData, unresolved, resolved, specials, localeTransformer, defaultCalendar);

            ListMultimap<IcuLocaleDir, RbPath> splitPaths = LinkedListMultimap.create();
            for (RbPath p : icuData.getPaths()) {
                String rootName = getBaseSegmentName(p.getSegment(0));
                splitPaths.put(LOCALE_SPLIT_INFO.getOrDefault(rootName, LOCALES), p);
            }

            Optional<String> parent = supplementalData.getExplicitParentLocaleOf(id);
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

                // The split data can still be empty for this directory, but that's expected (it
                // might only be written because it has an explicit parent added below).
                splitPaths.get(dir).forEach(p -> splitData.add(p, icuData.get(p)));

                // If we add an explicit parent locale, it forces the data to be written. This is
                // where we check for forced overrides of the parent relationship (which is a per
                // directory thing).
                getIcuParent(id, parent, dir).ifPresent(p -> {
                    splitData.add(RB_PARENT, p);
                    graphMetadata.get(dir).addParent(id, p);
                });

                if (!splitData.getPaths().isEmpty() || isBaseLanguage || dir.includeEmpty()) {
                    if (id.equals("root")) {
                        splitData.setVersion(cldrVersion);
                    }
                    write(splitData, outDir, false);
                    writtenLocaleIds.put(dir, id);
                }
            }
        }

        System.out.println("processing alias ldml files");
        for (IcuLocaleDir dir : splitDirs) {
            Path outDir = baseDir.resolve(dir.getOutputDir());
            Set<String> targetIds = config.getTargetLocaleIds(dir);
            DependencyGraph depGraph = graphMetadata.get(dir);

            // TODO: Maybe calculate alias map directly into the dependency graph?
            Map<String, String> aliasMap = getAliasMap(targetIds, dir);
            aliasMap.forEach((s, t) -> {
                depGraph.addAlias(s, t);
                writeAliasFile(s, t, outDir);
                // It's only important to record which alias files are written because of forced
                // aliases, but since it's harmless otherwise, we just do it unconditionally.
                // Normal alias files don't affect the empty file calculation, but forced ones can.
                writtenLocaleIds.put(dir, s);
            });

            calculateEmptyFiles(writtenLocaleIds.get(dir), aliasMap.values())
                .forEach(id -> writeEmptyFile(id, outDir, aliasMap.values()));

            writeDependencyGraph(outDir, depGraph);
        }
    }


    private static final CharMatcher PATH_MODIFIER = CharMatcher.anyOf(":%");

    // Resource bundle paths elements can have variants (e.g. "Currencies%narrow) or type
    // annotations (e.g. "languages:intvector"). We strip these when considering the element name.
    private static String getBaseSegmentName(String segment) {
        int idx = PATH_MODIFIER.indexIn(segment);
        return idx == -1 ? segment : segment.substring(0, idx);
    }

    /*
     * There are four reasons for treating a locale ID as an alias.
     * 1: It contains deprecated subtags (e.g. "sr_YU", which should be "sr_Cyrl_RS").
     * 2: It has no CLDR data but is missing a script subtag.
     * 3: It is one of the special "phantom" alias which cannot be represented normally
     *    and must be manually mapped (e.g. legacy locale IDs which don't even parse).
     * 4: It is a "super special" forced alias, which might replace existing aliases in
     *    some output directories.
     */
    private Map<String, String> getAliasMap(Set<String> localeIds, IcuLocaleDir dir) {
        // Even forced aliases only apply if they are in the set of locale IDs for the directory.
        Map<String, String> forcedAliases =
            Maps.filterKeys(config.getForcedAliases(dir), localeIds::contains);

        Map<String, String> aliasMap = new LinkedHashMap<>();
        for (String id : localeIds) {
            if (forcedAliases.containsKey(id)) {
                // Forced aliases will be added later and don't need to be processed here. This
                // is especially necessary if the ID is not structurally valid (e.g. "no_NO_NY")
                // since that cannot be processed by the code below.
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
        aliasMap.putAll(forcedAliases);
        return aliasMap;
    }

    /*
     * Helper to determine the correct parent ID to be written into the ICU data file. The rules
     * are:
     * 1: If no forced parent exists (common) write the explicit parent (if that exists)
     * 2: If a forced parent exists, but the forced value is what you would get by just truncating
     *    the current locale ID, write nothing (ICU libraries truncate when no parent is set).
     * 3: Write the forced parent (this is an exceptional case, and may not even occur in data).
     */
    private Optional<String> getIcuParent(String id, Optional<String> parent, IcuLocaleDir dir) {
        String forcedParentId = config.getForcedParents(dir).get(id);
        if (forcedParentId == null) {
            return parent;
        }
        return id.contains("_") && forcedParentId.regionMatches(0, id, 0, id.lastIndexOf('_'))
            ? Optional.empty() : Optional.of(forcedParentId);
    }

    private void processSupplemental() {
        for (OutputType type : config.getOutputTypes()) {
            if (type.getCldrType() == LDML) {
                continue;
            }
            System.out.println("processing supplemental type " + type);
            switch (type) {
            case DAY_PERIODS:
                write(DayPeriodsMapper.process(src), "misc");
                break;

            case GENDER_LIST:
                processSupplemental("genderList", GENDER_LIST_PATHS, "misc", false);
                break;

            case SUPPLEMENTAL_DATA:
                processSupplemental("supplementalData", SUPPLEMENTAL_DATA_PATHS, "misc", true);
                break;

            case UNITS:
                processSupplemental("units", UNITS_DATA_PATHS, "misc", true);
                break;

            case CURRENCY_DATA:
                processSupplemental("supplementalData", CURRENCY_DATA_PATHS, "curr", false);
                break;

            case GRAMMATICAL_FEATURES:
                processSupplemental("grammaticalFeatures", GRAMMATICAL_FEATURES_PATHS, "misc", false);
                break;

            case METADATA:
                processSupplemental("metadata", METADATA_PATHS, "misc", false);
                break;

            case META_ZONES:
                processSupplemental("metaZones", METAZONE_PATHS, "misc", false);
                break;

            case NUMBERING_SYSTEMS:
                processSupplemental("numberingSystems", NUMBERING_SYSTEMS_PATHS, "misc", false);
                break;

            case PLURALS:
                write(PluralsMapper.process(src), "misc");
                break;

            case PLURAL_RANGES:
                write(PluralRangesMapper.process(src), "misc");
                break;

            case LOCALE_DISTANCE:
                write(LocaleDistanceMapper.process(src), "misc");
                break;

            case WINDOWS_ZONES:
                processSupplemental("windowsZones", WINDOWS_ZONES_PATHS, "misc", false);
                break;

            case TRANSFORMS:
                Path transformDir = createDirectory(config.getOutputDir().resolve("translit"));
                write(TransformsMapper.process(src, transformDir, fileHeader), transformDir, false);
                break;

            case VERSION:
                writeIcuVersionInfo();
                break;

            case KEY_TYPE_DATA:
                Bcp47Mapper.process(src).forEach(d -> write(d, "misc"));
                break;

            default:
                throw new AssertionError("Unsupported supplemental type: " + type);
            }
        }
    }

    private static final RbPath RB_CLDR_VERSION = RbPath.of("cldrVersion");

    private void processSupplemental(
        String label, Predicate<CldrPath> paths, String dir, boolean addCldrVersion) {
        IcuData icuData =
            SupplementalMapper.process(src, supplementalTransformer, label, paths);
        // A hack for "supplementalData.txt" since the "cldrVersion" value doesn't come from the
        // supplemental data XML files.
        if (addCldrVersion) {
            // Not the same path as used by "setVersion()"
            icuData.add(RB_CLDR_VERSION, config.getVersionInfo().getCldrVersion());
        }
        write(icuData, dir);
    }

    private void writeAliasFile(String srcId, String destId, Path dir) {
        IcuData icuData = new IcuData(srcId, true);
        icuData.add(RB_ALIAS, destId);
        // Allow overwrite for aliases since some are "forced" and overwrite existing targets.
        // TODO: Maybe tighten this up so only forced aliases for existing targets are overwritten.
        write(icuData, dir, true);
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
            // No need to add data: Just write a resource bundle with an empty top-level table.
        }
        write(icuData, dir, false);
    }

    private void writeIcuVersionInfo() {
        IcuVersionInfo versionInfo = config.getVersionInfo();
        IcuData versionData = new IcuData("icuver", false);
        versionData.add(RbPath.of("ICUVersion"), versionInfo.getIcuVersion());
        versionData.add(RbPath.of("DataVersion"), versionInfo.getIcuDataVersion());
        versionData.add(RbPath.of("CLDRVersion"), versionInfo.getCldrVersion());
        // Write file via non-helper methods since we need to include a legacy copyright.
        Path miscDir = config.getOutputDir().resolve("misc");
        createDirectory(miscDir);
        ImmutableList<String> versionHeader = ImmutableList.<String>builder()
            .addAll(fileHeader)
            .add(
                "***************************************************************************",
                "*",
                "* Copyright (C) 2010-2016 International Business Machines",
                "* Corporation and others.  All Rights Reserved.",
                "*",
                "***************************************************************************")
            .build();
        IcuTextWriter.writeToFile(versionData, miscDir, versionHeader, false);
    }

    // Commonest case for writing data files in "normal" directories.
    private void write(IcuData icuData, String dir) {
        write(icuData, config.getOutputDir().resolve(dir), false);
    }

    private void write(IcuData icuData, Path dir, boolean allowOverwrite) {
        createDirectory(dir);
        IcuTextWriter.writeToFile(icuData, dir, fileHeader, allowOverwrite);
    }

    private Path createDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("cannot create directory: " + dir, e);
        }
        return dir;
    }

    private void writeDependencyGraph(Path dir, DependencyGraph depGraph) {
        createDirectory(dir);
        try (BufferedWriter w = Files.newBufferedWriter(dir.resolve("LOCALE_DEPS.json"), UTF_8);
            PrintWriter out = new PrintWriter(w)) {
            depGraph.writeJsonTo(out, fileHeader);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("cannot write dependency graph file: " + dir, e);
        }
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
            .put("extensions", BRKITR)
            .put("lstm", BRKITR)
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
