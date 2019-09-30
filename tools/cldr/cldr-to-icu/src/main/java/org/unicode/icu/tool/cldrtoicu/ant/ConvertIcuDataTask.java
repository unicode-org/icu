// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.ant;

import static com.google.common.base.CharMatcher.inRange;
import static com.google.common.base.CharMatcher.is;
import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDraftStatus;
import org.unicode.icu.tool.cldrtoicu.IcuConverterConfig;
import org.unicode.icu.tool.cldrtoicu.LdmlConverter;
import org.unicode.icu.tool.cldrtoicu.LdmlConverter.OutputType;
import org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir;
import org.unicode.icu.tool.cldrtoicu.SupplementalData;

import com.google.common.base.Ascii;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;

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
    private ImmutableSet<String> localeIdSpec;
    // Per directory overrides (fully specified locale IDs).
    private final SetMultimap<IcuLocaleDir, String> perDirectoryIds = HashMultimap.create();
    private final IcuConverterConfig.Builder config = IcuConverterConfig.builder();

    @SuppressWarnings("unused")
    public void setOutputDir(Path path) {
        config.setOutputDir(path);
    }

    @SuppressWarnings("unused")
    public void setCldrDir(Path path) {
        this.cldrPath = checkNotNull(path);
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
    public void setSpecialsDir(Path path) {
        config.setSpecialsDir(path);
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

    public static final class DirectoryFilter extends Task {
        private IcuLocaleDir dir;
        private ImmutableSet<String> ids;

        @SuppressWarnings("unused")
        public void setDir(String directory) {
            this.dir = resolve(IcuLocaleDir.class, directory);
        }

        @SuppressWarnings("unused")
        public void addText(String localeIds) {
            this.ids = parseLocaleIds(localeIds);
        }

        @Override
        public void init() throws BuildException {
            checkBuild(dir != null, "Directory must be specified");
            checkBuild(!ids.isEmpty(), "Locale IDs must be specified");
        }
    }

    public static final class ForcedAlias extends Task {
        private Optional<IcuLocaleDir> dir = Optional.empty();
        private String source = "";
        private String target = "";

        @SuppressWarnings("unused")
        public void setDir(String directory) {
            this.dir = resolveOpt(IcuLocaleDir.class, directory);
        }

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

    @SuppressWarnings("unused")
    public void addConfiguredLocaleIds(LocaleIds localeIds) {
        checkBuild(this.localeIdSpec == null, "Cannot add more that one <localeIds> element");
        this.localeIdSpec =  localeIds.ids;
    }

    @SuppressWarnings("unused")
    public void addConfiguredDirectoryFilter(DirectoryFilter filter) {
        perDirectoryIds.putAll(filter.dir, filter.ids);
    }

    @SuppressWarnings("unused")
    public void addConfiguredForcedAlias(ForcedAlias alias) {
        if (alias.dir.isPresent()) {
            config.addForcedAlias(alias.dir.get(), alias.source, alias.target);
        } else {
            for (IcuLocaleDir dir : IcuLocaleDir.values()) {
                config.addForcedAlias(dir, alias.source, alias.target);
            }
        }
    }

    @SuppressWarnings("unused")
    public void execute() throws BuildException {
        CldrDataSupplier src =
            CldrDataSupplier.forCldrFilesIn(cldrPath).withDraftStatusAtLeast(minimumDraftStatus);
        SupplementalData supplementalData = SupplementalData.create(src);
        ImmutableSet<String> defaultTargetIds =
            LocaleIdResolver.expandTargetIds(this.localeIdSpec, supplementalData);
        for (IcuLocaleDir dir : IcuLocaleDir.values()) {
            config.addLocaleIds(dir, perDirectoryIds.asMap().getOrDefault(dir, defaultTargetIds));
        }
        config.setMinimumDraftStatus(minimumDraftStatus);
        LdmlConverter.convert(src, supplementalData, config.build());
    }

    private static void checkBuild(boolean condition, String message) {
        if (!condition) {
            throw new BuildException(message);
        }
    }

    private static ImmutableSet<String> parseLocaleIds(String localeIds) {
        // Need to filter out '//' style end-of-line comments first (replace with \n to avoid
        // inadvertantly joining two elements.
        localeIds = localeIds.replaceAll("//[^\n]*\n", "\n");
        return ImmutableSet.copyOf(LIST_SPLITTER.splitToList(localeIds));
    }

    private static <T extends Enum<T>> Optional<T> resolveOpt(Class<T> enumClass, String name) {
        return !name.isEmpty() ? Optional.of(resolve(enumClass, name)) : Optional.empty();
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
