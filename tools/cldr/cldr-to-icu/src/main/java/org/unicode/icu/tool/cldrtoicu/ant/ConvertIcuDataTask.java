// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.ant;

import static com.google.common.base.CharMatcher.inRange;
import static com.google.common.base.CharMatcher.is;
import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableTable.toImmutableTable;
import static com.google.common.collect.Tables.immutableCell;
import static java.util.stream.Collectors.joining;
import static org.unicode.cldr.api.CldrPath.parseDistinguishingPath;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDraftStatus;
import org.unicode.cldr.api.CldrPath;
import org.unicode.icu.tool.cldrtoicu.AlternateLocaleData;
import org.unicode.icu.tool.cldrtoicu.IcuConverterConfig;
import org.unicode.icu.tool.cldrtoicu.LdmlConverter;
import org.unicode.icu.tool.cldrtoicu.LdmlConverter.OutputType;
import org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir;
import org.unicode.icu.tool.cldrtoicu.PseudoLocales;
import org.unicode.icu.tool.cldrtoicu.SupplementalData;

import com.google.common.base.Ascii;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table.Cell;

// Note: Auto-magical Ant methods are listed as "unused" by IDEs, unless the warning is suppressed.
public final class ConvertIcuDataTask extends Task {
    private static final Splitter LIST_SPLITTER =
        Splitter.on(CharMatcher.anyOf(",\n")).trimResults(whitespace()).omitEmptyStrings();

    private static final CharMatcher DIGIT_OR_UNDERSCORE = inRange('0', '9').or(is('_'));
    private static final CharMatcher UPPER_UNDERSCORE = inRange('A', 'Z').or(DIGIT_OR_UNDERSCORE);
    private static final CharMatcher LOWER_UNDERSCORE = inRange('a', 'z').or(DIGIT_OR_UNDERSCORE);
    private static final CharMatcher VALID_ENUM_CHAR = LOWER_UNDERSCORE.or(UPPER_UNDERSCORE);

    private Path cldrPath;
    private CldrDraftStatus minimumDraftStatus;
    // Set of default locale ID specifiers (wildcard IDs which are expanded).
    private LocaleIds localeIds = null;
    // Per directory overrides (fully specified locale IDs).
    private final SetMultimap<IcuLocaleDir, String> perDirectoryIds = HashMultimap.create();
    private final SetMultimap<IcuLocaleDir, String> inheritLanguageSubtag = HashMultimap.create();
    private final IcuConverterConfig.Builder config = IcuConverterConfig.builder();
    // Don't try and resolve actual paths until inside the execute method.
    private final List<AltPath> altPaths = new ArrayList<>();
    // TODO(CLDR-13381): Move into CLDR API; e.g. withPseudoLocales()
    private boolean includePseudoLocales = false;
    private Predicate<String> idFilter = id -> true;

    @SuppressWarnings("unused")
    public void setOutputDir(String path) {
        // Use String here since on some systems Ant doesn't support automatically converting Path instances.
        config.setOutputDir(Paths.get(path));
    }

    @SuppressWarnings("unused")
    public void setCldrDir(String path) {
        // Use String here since on some systems Ant doesn't support automatically converting Path instances.
        this.cldrPath = checkNotNull(Paths.get(path));
    }

    @SuppressWarnings("unused")
    public void setIcuVersion(String icuVersion) {
        config.setIcuVersion(icuVersion);
    }

    @SuppressWarnings("unused")
    public void setIcuDataVersion(String icuDataVersion) {
        config.setIcuDataVersion(icuDataVersion);
    }

    @SuppressWarnings("unused")
    public void setCldrVersion(String cldrVersion) {
        config.setCldrVersion(cldrVersion);
    }

    @SuppressWarnings("unused")
    public void setMinimalDraftStatus(String status) {
        minimumDraftStatus = resolve(CldrDraftStatus.class, status);
    }

    @SuppressWarnings("unused")
    public void setOutputTypes(String types) {
        ImmutableList<OutputType> typeList =
            LIST_SPLITTER
                .splitToList(types).stream()
                .map(s -> resolve(OutputType.class, s))
                .collect(toImmutableList());
        if (!typeList.isEmpty()) {
            config.setOutputTypes(typeList);
        }
    }

    @SuppressWarnings("unused")
    public void setSpecialsDir(String path) {
        // Use String here since on some systems Ant doesn't support automatically converting Path instances.
        config.setSpecialsDir(Paths.get(path));
    }

    @SuppressWarnings("unused")
    public void setIncludePseudoLocales(boolean includePseudoLocales) {
        this.includePseudoLocales = includePseudoLocales;
    }

    @SuppressWarnings("unused")
    public void setLocaleIdFilter(String idFilterRegex) {
        this.idFilter = Pattern.compile(idFilterRegex).asPredicate();
    }

    @SuppressWarnings("unused")
    public void setEmitReport(boolean emit) {
        config.setEmitReport(emit);
    }

    public static final class LocaleIds extends Task {
        private ImmutableSet<String> ids;

        @SuppressWarnings("unused")
        public void addText(String localeIds) {
            this.ids = parseLocaleIds(localeIds);
        }

        @Override
        public void init() throws BuildException {
            checkBuild(!ids.isEmpty(), "Locale IDs must be specified");
        }
    }

    public static final class Directory extends Task {
        private IcuLocaleDir dir;
        private ImmutableSet<String> inheritLanguageSubtag = ImmutableSet.of();
        private final List<ForcedAlias> forcedAliases = new ArrayList<>();
        private LocaleIds localeIds = null;

        @SuppressWarnings("unused")
        public void setDir(String directory) {
            this.dir = resolve(IcuLocaleDir.class, directory);
        }

        @SuppressWarnings("unused")
        public void setInheritLanguageSubtag(String localeIds) {
            this.inheritLanguageSubtag = parseLocaleIds(localeIds);
        }

        @SuppressWarnings("unused")
        public void addConfiguredForcedAlias(ForcedAlias alias) {
            forcedAliases.add(alias);
        }

        @SuppressWarnings("unused")
        public void addConfiguredLocaleIds(LocaleIds localeIds) {
            checkBuild(this.localeIds == null,
                "Cannot add more that one <localeIds> element for <directory>: %s", dir);
            this.localeIds =  localeIds;
        }

        @Override
        public void init() throws BuildException {
            checkBuild(dir != null, "Directory attribute 'dir' must be specified");
            checkBuild(localeIds != null, "<localeIds> must be specified for <directory>: %s", dir);
        }
    }

    public static final class ForcedAlias extends Task {
        private String source = "";
        private String target = "";

        @SuppressWarnings("unused")
        public void setSource(String source) {
            this.source = whitespace().trimFrom(source);
        }

        @SuppressWarnings("unused")
        public void setTarget(String target) {
            this.target = whitespace().trimFrom(target);
        }

        @Override
        public void init() throws BuildException {
            checkBuild(!source.isEmpty(), "Alias source must not be empty");
            checkBuild(!target.isEmpty(), "Alias target must not be empty");
        }
    }

    public static final class AltPath extends Task {
        private String source = "";
        private String target = "";
        private ImmutableSet<String> localeIds = ImmutableSet.of();

        @SuppressWarnings("unused")
        public void setTarget(String target) {
            this.target = target.replace('\'', '"');
        }

        @SuppressWarnings("unused")
        public void setSource(String source) {
            this.source = source.replace('\'', '"');
        }

        @SuppressWarnings("unused")
        public void setLocales(String localeIds) {
            this.localeIds = parseLocaleIds(localeIds);
        }

        @Override
        public void init() throws BuildException {
            checkBuild(!source.isEmpty(), "Source path not be empty");
            checkBuild(!target.isEmpty(), "Target path not be empty");
        }
    }

    @SuppressWarnings("unused")
    public void addConfiguredLocaleIds(LocaleIds localeIds) {
        checkBuild(this.localeIds == null, "Cannot add more that one <localeIds> element");
        this.localeIds =  localeIds;
    }

    @SuppressWarnings("unused")
    public void addConfiguredDirectory(Directory filter) {
        checkState(!perDirectoryIds.containsKey(filter.dir),
            "directory %s specified twice", filter.dir);
        ImmutableSet<String> ids = filter.localeIds.ids;
        perDirectoryIds.putAll(filter.dir, ids);

        // Check that any locale IDs marked to inherit the base language (instead of root) are
        // listed in the set of generated locales.
        inheritLanguageSubtag.putAll(filter.dir, filter.inheritLanguageSubtag);
        if (!ids.containsAll(filter.inheritLanguageSubtag)) {
            log(String.format(
                "WARNING: Locale IDs listed in 'inheritLanguageSubtag' should also be listed "
                    + "in <localeIds> for that directory (%s): %s",
                filter.dir, String.join(", ", Sets.difference(filter.inheritLanguageSubtag, ids))));
            perDirectoryIds.putAll(filter.dir, filter.inheritLanguageSubtag);
        }

        // Check that locales specified for forced aliases in this directory are also listed in
        // the set of generated locales.
        filter.forcedAliases.forEach(a -> config.addForcedAlias(filter.dir, a.source, a.target));
        Set<String> sourceIds =
            filter.forcedAliases.stream().map(a -> a.source).collect(Collectors.toSet());
        if (!ids.containsAll(sourceIds)) {
            Set<String> missingIds = Sets.difference(sourceIds, ids);
            log(String.format(
                "WARNING: Locale IDs listed as sources of a <forcedAlias> should also be listed "
                    + "in <localeIds> for that directory (%s): %s",
                filter.dir, String.join(", ", missingIds)));
            perDirectoryIds.putAll(filter.dir, missingIds);
        }
        Set<String> targetIds =
            filter.forcedAliases.stream().map(a -> a.target).collect(Collectors.toSet());
        if (!ids.containsAll(targetIds)) {
            Set<String> missingIds = Sets.difference(targetIds, ids);
            log(String.format(
                "WARNING: Locale IDs listed as targets of a <forcedAlias> should also be listed "
                    + "in <localeIds> for that directory (%s): %s",
                filter.dir, String.join(", ", missingIds)));
            perDirectoryIds.putAll(filter.dir, missingIds);
        }
    }

    // Aliases on the outside are applied to all directories.
    @SuppressWarnings("unused")
    public void addConfiguredForcedAlias(ForcedAlias alias) {
        for (IcuLocaleDir dir : IcuLocaleDir.values()) {
            config.addForcedAlias(dir, alias.source, alias.target);
        }
    }

    @SuppressWarnings("unused")
    public void addConfiguredAltPath(AltPath altPath) {
        // Don't convert to CldrPath here (it triggers a bunch of CLDR data loading for the DTDs).
        // Wait until the "execute()" method since in future we expect to use the configured CLDR
        // directory explicitly there.
        altPaths.add(altPath);
    }

    @SuppressWarnings("unused")
    public void execute() throws BuildException {
        checkBuild(localeIds != null, "<localeIds> must be specified");

        CldrDataSupplier src = CldrDataSupplier
            .forCldrFilesIn(cldrPath)
            .withDraftStatusAtLeast(minimumDraftStatus);

        // We must do this wrapping of the data supplier _before_ creating the supplemental data
        // instance since adding pseudo locales affects the set of available locales.
        // TODO: Move some/all of this into the base converter and control it via the config.
        if (!altPaths.isEmpty()) {
            src = AlternateLocaleData.transform(src, getGlobalAltPaths(), getLocaleAltPaths());
        }
        if (includePseudoLocales) {
            src = PseudoLocales.addPseudoLocalesTo(src);
        }

        SupplementalData supplementalData = SupplementalData.create(src);
        ImmutableSet<String> defaultTargetIds =
            LocaleIdResolver.expandTargetIds(this.localeIds.ids, supplementalData);
        for (IcuLocaleDir dir : IcuLocaleDir.values()) {
            Iterable<String> ids = perDirectoryIds.asMap().getOrDefault(dir, defaultTargetIds);
            config.addLocaleIds(dir, Iterables.filter(ids, idFilter::test));

            // We should only have locale IDs like "zh_Hant" here (language + script) and only
            // those which would naturally inherit to "root"
            inheritLanguageSubtag.get(dir).forEach(id -> {
                checkArgument(id.matches("[a-z]{2}_[A-Z][a-z]{3}"),
                    "Invalid locale ID for inheritLanguageSubtag (expect '<lang>_<Script>'): ", id);
                checkArgument(supplementalData.getParent(id).equals("root"),
                    "Invalid locale ID for inheritLanguageSubtag (parent must be 'root'): ", id);
                config.addForcedParent(dir, id, id.substring(0, 2));
            });
        }
        config.setMinimumDraftStatus(minimumDraftStatus);
        LdmlConverter.convert(src, supplementalData, config.build());
    }

    private ImmutableMap<CldrPath, CldrPath> getGlobalAltPaths() {
        // This fails if the same key appears more than once.
        return altPaths.stream()
            .filter(a -> a.localeIds.isEmpty())
            .collect(toImmutableMap(
                a -> parseDistinguishingPath(a.target),
                a -> parseDistinguishingPath(a.source)));
    }

    private ImmutableTable<String, CldrPath, CldrPath> getLocaleAltPaths() {
        return altPaths.stream()
            .flatMap(
                a -> a.localeIds.stream().map(
                    id -> immutableCell(
                        id,
                        parseDistinguishingPath(a.target),
                        parseDistinguishingPath(a.source))))
            // Weirdly there's no collector method to just collect cells.
            .collect(toImmutableTable(Cell::getRowKey, Cell::getColumnKey, Cell::getValue));
    }

    private static void checkBuild(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new BuildException(String.format(message, args));
        }
    }

    private static ImmutableSet<String> parseLocaleIds(String localeIds) {
        // Need to filter out '//' style end-of-line comments first (replace with \n to avoid
        // inadvertantly joining two elements.
        localeIds = localeIds.replaceAll("//[^\n]*\n", "\n");
        return ImmutableSet.copyOf(LIST_SPLITTER.splitToList(localeIds));
    }

    private static <T extends Enum<T>> T resolve(Class<T> enumClass, String name) {
        checkArgument(!name.isEmpty(), "enumeration name cannot be empty");
        checkArgument(VALID_ENUM_CHAR.matchesAllOf(name),
            "invalid enumeration name '%s'; expected only ASCII letters or '_'", name);
        CaseFormat format;
        if (UPPER_UNDERSCORE.matchesAllOf(name)) {
            format = CaseFormat.UPPER_UNDERSCORE;
        } else if (LOWER_UNDERSCORE.matchesAllOf(name)) {
            format = CaseFormat.LOWER_UNDERSCORE;
        } else {
            // Mixed case with '_' is not permitted.
            checkArgument(!name.contains("_"),
                "invalid enumeration name '%s'; mixed case with underscore not allowed: %s", name);
            format =
                Ascii.isLowerCase(name.charAt(0)) ? CaseFormat.LOWER_CAMEL : CaseFormat.UPPER_CAMEL;
        }
        try {
            return Enum.valueOf(enumClass, format.to(CaseFormat.UPPER_UNDERSCORE, name));
        } catch (IllegalArgumentException e) {
            String validNames =
                Arrays.stream(enumClass.getEnumConstants())
                    .map(Object::toString)
                    .collect(joining(", "));
            throw new IllegalArgumentException(
                "invalid enumeration name " + name + "; expected one of; " + validNames);
        }
    }
}
