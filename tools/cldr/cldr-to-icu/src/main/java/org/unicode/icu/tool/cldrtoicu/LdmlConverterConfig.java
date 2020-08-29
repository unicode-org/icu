// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import org.unicode.cldr.api.CldrDraftStatus;
import org.unicode.icu.tool.cldrtoicu.LdmlConverter.OutputType;

import com.google.common.base.Ascii;

import static com.google.common.base.Preconditions.checkNotNull;

/** API for configuring the LDML converter. */
public interface LdmlConverterConfig {
    /** Output directories for ICU locale data (this is not used for supplemental data). */
    enum IcuLocaleDir {
        /** Data for the break-iterator library. */
        BRKITR(true),
        /** Data for the collations library. */
        COLL(true),
        /** Currency data. */
        CURR(false),
        /** Language data. */
        LANG(false),
        /** General locale data. */
        LOCALES(true),
        /** Rule-based number formatter data. */
        RBNF(true),
        /** Region data. */
        REGION(false),
        /** Measurement and units data. */
        UNIT(false),
        /** Timezone data. */
        ZONE(false);

        private final String dirName = Ascii.toLowerCase(name());
        private final boolean includeEmpty;

        IcuLocaleDir(boolean includeEmpty) {
            this.includeEmpty = includeEmpty;
        }

        /** Returns the relative output directory name. */
        public String getOutputDir() {
            return dirName;
        }

        /**
         * Whether the directory is expected to contain empty data files (used to advertise
         * the supported set of locales for the "service" provided by the data in that
         * directory).
         */
        // TODO: Document why there's a difference between directories for empty files.
        boolean includeEmpty() {
            return includeEmpty;
        }
    }

    final class IcuVersionInfo {
        private final String icuVersion;
        private final String icuDataVersion;
        private final String cldrVersion;

        public IcuVersionInfo(String icuVersion, String icuDataVersion, String cldrVersion) {
            this.icuVersion = checkNotNull(icuVersion);
            this.icuDataVersion = checkNotNull(icuDataVersion);
            this.cldrVersion = checkNotNull(cldrVersion);
        }

        public String getIcuVersion() {
            return icuVersion;
        }

        public String getIcuDataVersion() {
            return icuDataVersion;
        }

        public String getCldrVersion() {
            return cldrVersion;
        }
    }

    /**
     * Returns the set of output types to be converted. Use {@link OutputType#ALL} to convert
     * everything.
     */
    Set<OutputType> getOutputTypes();

    /**
     * Returns an additional "specials" directory containing additional ICU specific XML
     * files depending on the given output type. This is where the converter finds any XML
     * files using the "icu:" namespace.
     */
    Path getSpecialsDir();

    /**
     * Returns the root of the ICU output directory hierarchy into which ICU data file are
     * written.
     */
    Path getOutputDir();

    /**
     * Returns a CLDR version String (e.g. {@code "36.1"}) according to either the specified option
     * or (as a fallback) the version specified by the CLDR library against which this code is run.
     */
    IcuVersionInfo getVersionInfo();

    /** Returns the minimal draft status for CLDR data to be converted. */
    CldrDraftStatus getMinimumDraftStatus();

    /**
     * Returns the complete set of locale IDs which should be considered for processing for this
     * configuration.
     *
     * <p>Note that this set can contain IDs which have no CLDR data associated with them if they
     * are suitable aliases (e.g. they are deprecated versions of locale IDs for which data does
     * exist).
     */
    Set<String> getAllLocaleIds();

    /**
     * Returns the set of locale IDs to be processed for the given directory. This set must always
     * be a subset of {@link #getAllLocaleIds()}.
     */
    Set<String> getTargetLocaleIds(IcuLocaleDir dir);

    /**
     * Returns a map of locale IDs which specifies aliases which are applied to the given directory
     * in contradiction to the natural alias which would otherwise be generated. This mechanism
     * allows for restructuring locale relationships on a per directory basis for special-case
     * behaviour (such as sharing data which would otherwise need to be copied).
     */
    Map<String, String> getForcedAliases(IcuLocaleDir dir);

    /**
     * Returns a map of locale IDs which specifies aliases which are applied to the given directory
     * in contradiction to the natural parent which would otherwise be generated. This mechanism
     * allows for restructuring locale relationships on a per directory basis for special-case
     * behaviour (such as sharing data which would otherwise need to be copied).
     */
    // TODO: Combine this and the force aliases into a single mechanism at this level.
    Map<String, String> getForcedParents(IcuLocaleDir dir);

    /**
     * Whether to emit a summary report for debug purposes after conversion is complete.
     */
    boolean emitReport();
}
