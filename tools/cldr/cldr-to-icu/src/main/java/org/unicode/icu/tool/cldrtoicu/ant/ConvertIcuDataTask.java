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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.unicode.cldr.api.CldrDraftStatus;
import org.unicode.icu.tool.cldrtoicu.IcuConverterConfig;
import org.unicode.icu.tool.cldrtoicu.LdmlConverter;
import org.unicode.icu.tool.cldrtoicu.LdmlConverterConfig.IcuLocaleDir;

import com.google.common.base.Ascii;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

// Note: Auto-magical Ant methods are listed as "unused" by IDEs, unless the warning is suppressed.
public final class ConvertIcuDataTask extends Task {
    private static final Splitter LIST_SPLITTER =
        Splitter.on(CharMatcher.anyOf(",\n")).trimResults(whitespace()).omitEmptyStrings();

    private static final CharMatcher DIGIT_OR_UNDERSCORE = inRange('0', '9').or(is('_'));
    private static final CharMatcher UPPER_UNDERSCORE = inRange('A', 'Z').or(DIGIT_OR_UNDERSCORE);
    private static final CharMatcher LOWER_UNDERSCORE = inRange('a', 'z').or(DIGIT_OR_UNDERSCORE);
    private static final CharMatcher VALID_ENUM_CHAR = LOWER_UNDERSCORE.or(UPPER_UNDERSCORE);

    private final IcuConverterConfig.Builder config = IcuConverterConfig.builder();

    @SuppressWarnings("unused")
    public void setOutputDir(Path path) {
        config.setOutputDir(path);
    }

    @SuppressWarnings("unused")
    public void setCldrDir(Path path) {
        config.setCldrDir(path);
    }

    @SuppressWarnings("unused")
    public void setMinimalDraftStatus(String status) {
        config.setMinimalDraftStatus(resolve(CldrDraftStatus.class, status));
    }

    @SuppressWarnings("unused")
    public void setOutputTypes(String types) {
        config.setOutputTypes(
            LIST_SPLITTER
                .splitToList(types).stream()
                .map(s -> resolve(LdmlConverter.OutputType.class, s))
                .collect(toImmutableList()));
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
        private ImmutableList<IcuLocaleDir> dirs = ImmutableList.of();
        private ImmutableList<String> ids = ImmutableList.of();

        @SuppressWarnings("unused")
        public void setDirs(String directories) {
            this.dirs = LIST_SPLITTER.splitToList(directories).stream()
                .map(s -> resolve(IcuLocaleDir.class, s))
                .collect(toImmutableList());
        }

        @SuppressWarnings("unused")
        public void addText(String localeIds) {
            // Need to filter out '//' style end-of-line comments first (replace with \n to avoid
            // inadvertantly joining two elements.
            localeIds = localeIds.replaceAll("//[^\n]*\n", "\n");
            this.ids = ImmutableList.copyOf(LIST_SPLITTER.splitToList(localeIds));
        }
    }

    public static final class ForcedAlias extends Task {
        private IcuLocaleDir dir;
        private String source;
        private String target;

        @SuppressWarnings("unused")
        public void setDir(String directory) {
            this.dir = resolve(IcuLocaleDir.class, directory);
        }

        @SuppressWarnings("unused")
        public void setSource(String source) {
            this.source = checkNotNull(source);
        }

        @SuppressWarnings("unused")
        public void setTarget(String target) {
            this.target = checkNotNull(target);
        }
    }

    @SuppressWarnings("unused")
    public void addConfiguredLocaleIds(LocaleIds localeIds) {
        localeIds.dirs.forEach(d -> config.addLocaleIds(d, localeIds.ids));
    }

    @SuppressWarnings("unused")
    public void addConfiguredForcedAlias(ForcedAlias alias) {
        config.addForcedAlias(alias.dir, alias.source, alias.target);
    }

    @SuppressWarnings("unused")
    public void execute() throws BuildException {
        LdmlConverter.convert(config.build());
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
