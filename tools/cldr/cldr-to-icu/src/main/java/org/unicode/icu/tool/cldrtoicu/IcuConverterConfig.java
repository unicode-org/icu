// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.joining;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrDraftStatus;
import org.unicode.icu.tool.cldrtoicu.LdmlConverter.OutputType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.google.common.collect.TreeMultimap;

/**
 * The converter config intended to generate the standard ICU data files. This used to be something
 * that was configured by text files such as "icu-locale-deprecates.xml" and "icu-config.
 */
public final class IcuConverterConfig implements LdmlConverterConfig {
    private static final Optional<Path> DEFAULT_ICU_DIR =
        Optional.ofNullable(System.getProperty("ICU_DIR", null))
            .map(d -> Paths.get(d).toAbsolutePath());

    /** The builder with which to specify configuration for the {@link LdmlConverter}. */
    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {
        private Path outputDir =
            DEFAULT_ICU_DIR.map(d -> d.resolve("icu4c/source/data")).orElse(null);
        private Path specialsDir =
            DEFAULT_ICU_DIR.map(d -> d.resolve("icu4c/source/data/xml")).orElse(null);
        private ImmutableSet<OutputType> outputTypes = OutputType.ALL;
        private Optional<String> icuVersion = Optional.empty();
        private Optional<String> icuDataVersion = Optional.empty();
        private Optional<String> cldrVersion = Optional.empty();
        private CldrDraftStatus minimumDraftStatus = CldrDraftStatus.CONTRIBUTED;
        private boolean emitReport = false;
        private final SetMultimap<IcuLocaleDir, String> localeIdsMap = TreeMultimap.create();
        private final Table<IcuLocaleDir, String, String> forcedAliases = TreeBasedTable.create();
        private final Table<IcuLocaleDir, String, String> forcedParents = TreeBasedTable.create();

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

        public Builder setIcuVersion(String version) {
            if (!version.isEmpty()) {
                this.icuVersion = Optional.of(version);
            }
            return this;
        }

        public Builder setIcuDataVersion(String version) {
            if (!version.isEmpty()) {
                this.icuDataVersion = Optional.of(version);
            }
            return this;
        }

        public Builder setCldrVersion(String version) {
            if (!version.isEmpty()) {
                this.cldrVersion = Optional.of(version);
            }
            return this;
        }

        public void setMinimumDraftStatus(CldrDraftStatus minimumDraftStatus) {
            this.minimumDraftStatus = checkNotNull(minimumDraftStatus);
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

        public Builder addForcedParent(IcuLocaleDir dir, String localeId, String parent) {
            forcedParents.put(dir, localeId, parent);
            return this;
        }

        /** Returns a converter config from the current builder state. */
        public LdmlConverterConfig build() {
            return new IcuConverterConfig(this);
        }
    }

    private final Path outputDir;
    private final Path specialsDir;
    private final ImmutableSet<OutputType> outputTypes;
    private final IcuVersionInfo versionInfo;
    private final CldrDraftStatus minimumDraftStatus;
    private final boolean emitReport;
    private final ImmutableSet<String> allLocaleIds;
    private final ImmutableSetMultimap<IcuLocaleDir, String> localeIdsMap;
    private final ImmutableTable<IcuLocaleDir, String, String> forcedAliases;
    private final ImmutableTable<IcuLocaleDir, String, String> forcedParents;

    private IcuConverterConfig(Builder builder) {
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
        this.versionInfo = new IcuVersionInfo(
            builder.icuVersion.orElseThrow(() -> new IllegalStateException("missing ICU version")),
            builder.icuDataVersion.orElseThrow(() -> new IllegalStateException("missing ICU data version")),
            builder.cldrVersion.orElse(CldrDataSupplier.getCldrVersionString()));
        this.minimumDraftStatus = checkNotNull(builder.minimumDraftStatus);
        this.emitReport = builder.emitReport;
        // getAllLocaleIds() returns the union of all the specified IDs in the map.
        this.allLocaleIds = ImmutableSet.copyOf(builder.localeIdsMap.values());
        this.localeIdsMap = ImmutableSetMultimap.copyOf(builder.localeIdsMap);
        this.forcedAliases = ImmutableTable.copyOf(builder.forcedAliases);
        this.forcedParents = ImmutableTable.copyOf(builder.forcedParents);
    }

    public static Builder builder() {
        return new Builder();
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
    public Path getSpecialsDir() {
        return specialsDir;
    }

    @Override
    public IcuVersionInfo getVersionInfo() {
        return versionInfo;
    }

    @Override
    public CldrDraftStatus getMinimumDraftStatus() {
        return minimumDraftStatus;
    }

    @Override
    public boolean emitReport() {
        return emitReport;
    }

    @Override
    public ImmutableMap<String, String> getForcedAliases(IcuLocaleDir dir) {
        return forcedAliases.row(dir);
    }

    @Override
    public ImmutableMap<String, String> getForcedParents(IcuLocaleDir dir) {
        return forcedParents.row(dir);
    }

    @Override public ImmutableSet<String> getAllLocaleIds() {
        return allLocaleIds;
    }

    @Override public ImmutableSet<String> getTargetLocaleIds(IcuLocaleDir dir) {
        return localeIdsMap.get(dir);
    }
}
