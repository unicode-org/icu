// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.unicode.cldr.api.CldrDraftStatus;
import org.unicode.icu.tool.cldrtoicu.LdmlConverter.OutputType;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.common.collect.TreeMultimap;

/**
 * The converter config intended to generate the standard ICU data files. This used to be something
 * that was configured by text files such as "icu-locale-deprecates.xml" and "icu-config.
 */
public final class IcuConverterConfig implements LdmlConverterConfig {

    private static final Optional<Path> DEFAULT_CLDR_DIR =
        Optional.ofNullable(System.getProperty("CLDR_DIR", null))
            .map(d -> Paths.get(d).toAbsolutePath());

    private static final Optional<Path> DEFAULT_ICU_DIR =
        Optional.ofNullable(System.getProperty("ICU_DIR", null))
            .map(d -> Paths.get(d).toAbsolutePath());

    /** The builder with which to specify configuration for the {@link LdmlConverter}. */
    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {
        private Path cldrDir = DEFAULT_CLDR_DIR.orElse(null);
        private Path outputDir =
            DEFAULT_ICU_DIR.map(d -> d.resolve("icu4c/source/data")).orElse(null);
        private Path specialsDir =
            DEFAULT_ICU_DIR.map(d -> d.resolve("icu4c/source/data/xml")).orElse(null);
        private ImmutableSet<OutputType> outputTypes = OutputType.ALL;
        private CldrDraftStatus minimalDraftStatus = CldrDraftStatus.CONTRIBUTED;
        private boolean emitReport = false;
        private final SetMultimap<IcuLocaleDir, String> localeIdsMap = TreeMultimap.create();
        private final Table<IcuLocaleDir, String, String> forcedAliases = TreeBasedTable.create();

        /**
         * Sets the CLDR base directory from which to load all CLDR data. This is optional if the
         * {@code CLDR_DIR} environment variable is set, which will be used instead.
         */
        public Builder setCldrDir(Path cldrDir) {
            this.cldrDir = checkNotNull(cldrDir.toAbsolutePath());
            return this;
        }

        /**
         * Sets the output directory in which the ICU data directories and files will go. This is
         * optional if the {@code ICU_DIR} system property is set, which will be used to generate
         * the path instead (i.e. {@code "icu4c/source/data"} inside the ICU release directory).
         */
        public Builder setOutputDir(Path outputDir) {
            this.outputDir = checkNotNull(outputDir);
            return this;
        }

        /**
         * Sets the "specials" directory containing additional ICU specific data to be processed.
         * This is optional if the {@code ICU_DIR} system property is set, which will be used to
         * generate the path instead (i.e. {@code "icu4c/source/data/xml"} inside the ICU release
         * directory).
         */
        public Builder setSpecialsDir(Path specialsDir) {
            this.specialsDir = checkNotNull(specialsDir);
            return this;
        }

        /**
         * Sets the output types which will be converted. This is optional and defaults to {@link
         * OutputType#ALL}.
         */
        public Builder setOutputTypes(Iterable<OutputType> types) {
            this.outputTypes = ImmutableSet.copyOf(types);
            return this;
        }

        /**
         * Sets the minimum draft status for CLDR data to be converted (paths below this status are
         * ignored during conversion). This is optional and defaults to {@link
         * CldrDraftStatus#CONTRIBUTED}.
         */
        public Builder setMinimalDraftStatus(CldrDraftStatus minimalDraftStatus) {
            this.minimalDraftStatus = checkNotNull(minimalDraftStatus);
            return this;
        }

        public Builder setEmitReport(boolean emitReport) {
            this.emitReport = emitReport;
            return this;
        }

        public Builder addLocaleIds(IcuLocaleDir dir, Iterable<String> localeIds) {
            localeIdsMap.putAll(dir, localeIds);
            return this;
        }

        public Builder addForcedAlias(IcuLocaleDir dir, String source, String target) {
            forcedAliases.put(dir, source, target);
            return this;
        }

        /** Returns a converter config from the current builder state. */
        public LdmlConverterConfig build() {
            return new IcuConverterConfig(this);
        }
    }

    private final Path cldrDir;
    private final Path outputDir;
    private final Path specialsDir;
    private final ImmutableSet<OutputType> outputTypes;
    private final CldrDraftStatus minimalDraftStatus;
    private final boolean emitReport;
    private final ImmutableSetMultimap<IcuLocaleDir, String> localeIdsMap;
    private final ImmutableTable<IcuLocaleDir, String, String> forcedAliases;

    private IcuConverterConfig(Builder builder) {
        this.cldrDir = checkNotNull(builder.cldrDir,
            "must set a CLDR directory, or the CLDR_DIR system property");
        if (DEFAULT_CLDR_DIR.isPresent() && !this.cldrDir.equals(DEFAULT_CLDR_DIR.get())) {
            System.err.format(
                "Warning: Specified CLDR base directory does not appear to match the"
                    + " directory inferred by the 'CLDR_DIR' system property.\n"
                    + "Specified: %s\n"
                    + "Inferred: %s\n",
                this.cldrDir, DEFAULT_CLDR_DIR.get());
        }
        this.outputDir = checkNotNull(builder.outputDir);
        checkArgument(!Files.isRegularFile(outputDir),
            "specified output directory if not a directory: %s", outputDir);
        this.specialsDir = checkNotNull(builder.specialsDir,
            "must specify a 'specials' XML directory");
        checkArgument(Files.isDirectory(specialsDir),
            "specified specials directory does not exist: %s", specialsDir);
        this.outputTypes = builder.outputTypes;
        checkArgument(!this.outputTypes.isEmpty(),
            "must specify at least one output type to be generated (possible values are: %s)",
            Arrays.asList(OutputType.values()));
        this.minimalDraftStatus = builder.minimalDraftStatus;
        this.emitReport = builder.emitReport;
        this.localeIdsMap = ImmutableSetMultimap.copyOf(builder.localeIdsMap);
        this.forcedAliases = ImmutableTable.copyOf(builder.forcedAliases);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Path getCldrDirectory() {
        return cldrDir;
    }

    @Override
    public Path getOutputDir() {
        return outputDir;
    }

    @Override
    public Set<OutputType> getOutputTypes() {
        return outputTypes;
    }

    @Override
    public CldrDraftStatus getMinimumDraftStatus() {
        return minimalDraftStatus;
    }

    @Override
    public Path getSpecialsDir() {
        return specialsDir;
    }

    @Override
    public boolean emitReport() {
        return emitReport;
    }

    @Override
    public Map<String, String> getForcedAliases(IcuLocaleDir dir) {
        return forcedAliases.row(dir);
    }

    @Override public ImmutableSet<String> getTargetLocaleIds(IcuLocaleDir dir) {
        return localeIdsMap.get(dir);
    }
}
