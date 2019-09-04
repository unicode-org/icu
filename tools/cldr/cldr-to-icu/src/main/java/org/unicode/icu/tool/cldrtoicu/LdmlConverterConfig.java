// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.unicode.cldr.api.CldrDraftStatus;
import org.unicode.icu.tool.cldrtoicu.LdmlConverter.OutputType;

import com.google.common.base.Ascii;

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
        String getOutputDir() {
            return dirName;
        }

        /**
         * Whether the directory is expected to contain empty data files (used to advertise
         * the supported set of locales for the "service" provided by the data in that
         * directory).
         */
        // TODO: Document why there's a difference between directories for empty directories.
        boolean includeEmpty() {
            return includeEmpty;
        }
    }

    /**
     * Returns the set of output types to be converted. Use {@link OutputType#ALL} to convert
     * everything.
     */
    Set<OutputType> getOutputTypes();

    /** Returns the root directory in which the CLDR release is located. */
    Path getCldrDirectory();

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

    /** Returns the minimal draft status for CLDR data to be converted. */
    CldrDraftStatus getMinimumDraftStatus();

    /**
     * Returns the set of locale IDs to be processed for the given directory.
     *
     * <p>This set can contain IDs which have noICU data associated with them if they are
     * suitable aliases (e.g. they are deprecated versions of locale IDs for which data does
     * exist).
     */
    Set<String> getTargetLocaleIds(IcuLocaleDir dir);

    /**
     * Return a map of locale IDs which specifies aliases which are applied to the given
     * directory in contradiction to the natural alias or parent ID which would otherwise
     * be generated. This is a mechanism for restructuring the parent chain and linking
     * locales together in non-standard and unexpected ways.
     */
    Map<String, String> getForcedAliases(IcuLocaleDir dir);

    /**
     * Whether to emit a summary report for debug purposes after conversion is complete.
     */
    boolean emitReport();
}
