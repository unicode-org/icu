// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.text;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents all the display options that are supported by CLDR such as grammatical case, noun
 * class, ... etc. It currently supports enums, but may be extended in the future to have other
 * types of data. It replaces a DisplayContext[] as a method parameter.
 * <p>
 * NOTE: This class is Immutable, and uses a Builder interface.
 * <p>For example:
 * {@code DisplayOptions x =
 *                DisplayOptions.builder()
 *                             .setNounClass(NounClass.DATIVE)
 *                             .setPluralCategory(PluralCategory.FEW)
 *                             .build();
 *                             }
 *
 * @stable ICU 72
 */
public final class DisplayOptions {
    private final GrammaticalCase grammaticalCase;
    private final NounClass nounClass;
    private final PluralCategory pluralCategory;
    private final Capitalization capitalization;
    private final NameStyle nameStyle;
    private final DisplayLength displayLength;
    private final SubstituteHandling substituteHandling;

    private DisplayOptions(Builder builder) {
        this.grammaticalCase = builder.grammaticalCase;
        this.nounClass = builder.nounClass;
        this.pluralCategory = builder.pluralCategory;
        this.capitalization = builder.capitalization;
        this.nameStyle = builder.nameStyle;
        this.displayLength = builder.displayLength;
        this.substituteHandling = builder.substituteHandling;
    }

    /**
     * Creates a builder with the {@code UNDEFINED} value for all the parameters.
     *
     * @return Builder
     * @stable ICU 72
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder with the same parameters from this object.
     *
     * @return Builder
     * @stable ICU 72
     */
    public Builder copyToBuilder() {
        return new Builder(this);
    }

    /**
     * Gets the grammatical case.
     *
     * @return GrammaticalCase
     * @stable ICU 72
     */
    public GrammaticalCase getGrammaticalCase() {
        return this.grammaticalCase;
    }

    /**
     * Gets the noun class.
     *
     * @return NounClass
     * @stable ICU 72
     */
    public NounClass getNounClass() {
        return this.nounClass;
    }

    /**
     * Gets the plural category.
     *
     * @return PluralCategory
     * @stable ICU 72
     */
    public PluralCategory getPluralCategory() {
        return this.pluralCategory;
    }

    /**
     * Gets the capitalization.
     *
     * @return Capitalization
     * @stable ICU 72
     */
    public Capitalization getCapitalization() {
        return this.capitalization;
    }

    /**
     * Gets the name style.
     *
     * @return NameStyle
     * @stable ICU 72
     */
    public NameStyle getNameStyle() {
        return this.nameStyle;
    }

    /**
     * Gets the display length.
     *
     * @return DisplayLength
     * @stable ICU 72
     */
    public DisplayLength getDisplayLength() {
        return this.displayLength;
    }

    /**
     * Gets the substitute handling.
     *
     * @return SubstituteHandling
     * @stable ICU 72
     */
    public SubstituteHandling getSubstituteHandling() {
        return this.substituteHandling;
    }

    /**
     * Responsible for building {@code DisplayOptions}.
     *
     * @stable ICU 72
     */
    public static class Builder {
        private GrammaticalCase grammaticalCase;
        private NounClass nounClass;
        private PluralCategory pluralCategory;
        private Capitalization capitalization;
        private NameStyle nameStyle;
        private DisplayLength displayLength;
        private SubstituteHandling substituteHandling;

        /**
         * Creates a {@code DisplayOptions.Builder} with the default values.
         *
         * @stable ICU 72
         */
        private Builder() {
            this.grammaticalCase = GrammaticalCase.UNDEFINED;
            this.nounClass = NounClass.UNDEFINED;
            this.pluralCategory = PluralCategory.UNDEFINED;
            this.capitalization = Capitalization.UNDEFINED;
            this.nameStyle = NameStyle.UNDEFINED;
            this.displayLength = DisplayLength.UNDEFINED;
            this.substituteHandling = SubstituteHandling.UNDEFINED;
        }

        /**
         * Creates a {@code Builder} with all the information from a {@code DisplayOptions}.
         *
         * @param displayOptions Options to be copied.
         * @stable ICU 72
         */
        private Builder(DisplayOptions displayOptions) {
            this.grammaticalCase = displayOptions.grammaticalCase;
            this.nounClass = displayOptions.nounClass;
            this.pluralCategory = displayOptions.pluralCategory;
            this.capitalization = displayOptions.capitalization;
            this.nameStyle = displayOptions.nameStyle;
            this.displayLength = displayOptions.displayLength;
            this.substituteHandling = displayOptions.substituteHandling;
        }

        /**
         * Sets the grammatical case.
         *
         * @param grammaticalCase The grammatical case.
         * @return Builder
         * @stable ICU 72
         */
        public Builder setGrammaticalCase(GrammaticalCase grammaticalCase) {
            this.grammaticalCase = grammaticalCase;
            return this;
        }

        /**
         * Sets the noun class.
         *
         * @param nounClass The noun class.
         * @return Builder
         * @stable ICU 72
         */
        public Builder setNounClass(NounClass nounClass) {
            this.nounClass = nounClass;
            return this;
        }

        /**
         * Sets the plural category.
         *
         * @param pluralCategory The plural category.
         * @return Builder
         * @stable ICU 72
         */
        public Builder setPluralCategory(PluralCategory pluralCategory) {
            this.pluralCategory = pluralCategory;
            return this;
        }

        /**
         * Sets the capitalization.
         *
         * @param capitalization The capitalization.
         * @return Builder
         * @stable ICU 72
         */
        public Builder setCapitalization(Capitalization capitalization) {
            this.capitalization = capitalization;
            return this;
        }

        /**
         * Sets the name style.
         *
         * @param nameStyle The name style.
         * @return Builder
         * @stable ICU 72
         */
        public Builder setNameStyle(NameStyle nameStyle) {
            this.nameStyle = nameStyle;
            return this;
        }

        /**
         * Sets the display length.
         *
         * @param displayLength The display length.
         * @return Builder
         * @stable ICU 72
         */
        public Builder setDisplayLength(DisplayLength displayLength) {
            this.displayLength = displayLength;
            return this;
        }

        /**
         * Sets the substitute handling.
         *
         * @param substituteHandling The substitute handling.
         * @return Builder
         * @stable ICU 72
         */
        public Builder setSubstituteHandling(SubstituteHandling substituteHandling) {
            this.substituteHandling = substituteHandling;
            return this;
        }

        /**
         * Builds the display options.
         *
         * @return DisplayOptions
         * @stable ICU 72
         */
        public DisplayOptions build() {
            DisplayOptions displayOptions = new DisplayOptions(this);
            return displayOptions;
        }
    }

    /**
     * Represents all the grammatical noun classes that are supported by CLDR.
     *
     * @stable ICU 72
     */
    public enum NounClass {
        /**
         * A possible setting for NounClass. The noun class context to be used is unknown (this is the
         * default value).
         *
         * @stable ICU 72
         */
        UNDEFINED("undefined"),
        /**
         * @stable ICU 72
         */
        OTHER("other"),
        /**
         * @stable ICU 72
         */
        NEUTER("neuter"),
        /**
         * @stable ICU 72
         */
        FEMININE("feminine"),
        /**
         * @stable ICU 72
         */
        MASCULINE("masculine"),
        /**
         * @stable ICU 72
         */
        ANIMATE("animate"),
        /**
         * @stable ICU 72
         */
        INANIMATE("inanimate"),
        /**
         * @stable ICU 72
         */
        PERSONAL("personal"),
        /**
         * @stable ICU 72
         */
        COMMON("common");

        private final String identifier;

        private NounClass(String identifier) {
            this.identifier = identifier;
        }

        /**
         * Unmodifiable List of all noun classes constants. List version of {@link #values()}.
         *
         * @stable ICU 72
         */
        public static final List<NounClass> VALUES =
                Collections.unmodifiableList(Arrays.asList(NounClass.values()));

        /**
         * @return the lowercase CLDR keyword string for the noun class.
         * @stable ICU 72
         */
        public final String getIdentifier() {
            return this.identifier;
        }

        /**
         * @param identifier in lower case such as "feminine" or "masculine"
         * @return the plural category corresponding to the identifier, or {@code UNDEFINED}
         * @stable ICU 72
         */
        public static final NounClass fromIdentifier(String identifier) {
            if (identifier == null) {
                return NounClass.UNDEFINED;
            }

            for (NounClass nounClass : VALUES) {
                if (identifier.equals(nounClass.getIdentifier())) {
                    return nounClass;
                }
            }

            return NounClass.UNDEFINED;
        }
    }

    /**
     * Represents all the name styles.
     *
     * @stable ICU 72
     */
    public enum NameStyle {
        /**
         * A possible setting for NameStyle. The NameStyle context to be used is unknown (this is the
         * default value).
         *
         * @stable ICU 72
         */
        UNDEFINED,
        /**
         * Use standard names when generating a locale name, e.g. en_GB displays as 'English (United
         * Kingdom)'.
         *
         * @stable ICU 72
         */
        STANDARD_NAMES,

        /**
         * Use dialect names, when generating a locale name, e.g. en_GB displays as 'British English'.
         *
         * @stable ICU 72
         */
        DIALECT_NAMES;

        /**
         * Unmodifiable List of all name styles constants. List version of {@link #values()}.
         *
         * @stable ICU 72
         */
        public static final List<NameStyle> VALUES =
                Collections.unmodifiableList(Arrays.asList(NameStyle.values()));
    }

    /**
     * Represents all the substitute handlings.
     *
     * @stable ICU 72
     */
    public enum SubstituteHandling {
        /**
         * A possible setting for SubstituteHandling. The SubstituteHandling context to be used is
         * unknown (this is the default value).
         *
         * @stable ICU 72
         */
        UNDEFINED,
        /**
         * Returns a fallback value (e.g., the input code) when no data is available. This is the
         * default behaviour.
         *
         * @stable ICU 72
         */
        SUBSTITUTE,

        /**
         * Returns a null value when no data is available.
         *
         * @stable ICU 72
         */
        NO_SUBSTITUTE;

        /**
         * Unmodifiable List of all substitute handlings constants. List version of {@link #values()}.
         *
         * @stable ICU 72
         */
        public static final List<SubstituteHandling> VALUES =
                Collections.unmodifiableList(Arrays.asList(SubstituteHandling.values()));
    }

    /**
     * Represents all the display lengths.
     *
     * @stable ICU 72
     */
    public enum DisplayLength {
        /**
         * A possible setting for DisplayLength. The DisplayLength context to be used is unknown (this
         * is the default value).
         *
         * @stable ICU 72
         */
        UNDEFINED,
        /**
         * Uses full names when generating a locale name, e.g. "United States" for US.
         *
         * @stable ICU 72
         */
        LENGTH_FULL,

        /**
         * Use short names when generating a locale name, e.g. "U.S." for US.
         *
         * @stable ICU 72
         */
        LENGTH_SHORT;

        /**
         * Unmodifiable List of all display lengths constants. List version of {@link #values()}.
         *
         * @stable ICU 72
         */
        public static final List<DisplayLength> VALUES =
                Collections.unmodifiableList(Arrays.asList(DisplayLength.values()));
    }

    /**
     * Represents all the capitalization options.
     *
     * @stable ICU 72
     */
    public enum Capitalization {
        /**
         * A possible setting for Capitalization. The capitalization context to be used is unknown (this
         * is the default value).
         *
         * @stable ICU 72
         */
        UNDEFINED,

        /**
         * The capitalization context if a date, date symbol or display name is to be formatted with
         * capitalization appropriate for the beginning of a sentence.
         *
         * @stable ICU 72
         */
        BEGINNING_OF_SENTENCE,

        /**
         * The capitalization context if a date, date symbol or display name is to be formatted with
         * capitalization appropriate for the middle of a sentence.
         *
         * @stable ICU 72
         */
        MIDDLE_OF_SENTENCE,

        /**
         * The capitalization context if a date, date symbol or display name is to be formatted with
         * capitalization appropriate for stand-alone usage such as an isolated name on a calendar
         * page.
         *
         * @stable ICU 72
         */
        STANDALONE,

        /**
         * The capitalization context if a date, date symbol or display name is to be formatted with
         * capitalization appropriate for a user-interface list or menu item.
         *
         * @stable ICU 72
         */
        UI_LIST_OR_MENU;

        /**
         * Unmodifiable List of all the capitalizations constants. List version of {@link #values()}.
         *
         * @stable ICU 72
         */
        public static final List<Capitalization> VALUES =
                Collections.unmodifiableList(Arrays.asList(Capitalization.values()));
    }

    /**
     * Standard CLDR plural category constants. See http://www.unicode.org/reports/tr35/tr35-numbers.html#Language_Plural_Rules
     *
     * @stable ICU 72
     */
    public enum PluralCategory {
        /**
         * A possible setting for PluralCategory. The plural category context to be used is unknown
         * (this is the default value).
         *
         * @stable ICU 72
         */
        UNDEFINED("undefined"),

        /**
         * @stable ICU 72
         */
        ZERO("zero"),

        /**
         * @stable ICU 72
         */
        ONE("one"),

        /**
         * @stable ICU 72
         */
        TWO("two"),

        /**
         * @stable ICU 72
         */
        FEW("few"),

        /**
         * @stable ICU 72
         */
        MANY("many"),

        /**
         * @stable ICU 72
         */
        OTHER("other");

        private final String identifier;

        private PluralCategory(String identifier) {
            this.identifier = identifier;
        }

        /**
         * Unmodifiable List of all plural categories constants. List version of {@link #values()}.
         *
         * @stable ICU 72
         */
        public static final List<PluralCategory> VALUES =
                Collections.unmodifiableList(Arrays.asList(PluralCategory.values()));

        /**
         * @return the lowercase CLDR keyword string for the plural category
         * @stable ICU 72
         */
        public final String getIdentifier() {
            return this.identifier;
        }

        /**
         * @param identifier in lower case such as "few" or "other"
         * @return the plural category corresponding to the identifier, or {@code UNDEFINED}
         * @stable ICU 72
         */
        public static final PluralCategory fromIdentifier(String identifier) {
            if (identifier == null) {
                return PluralCategory.UNDEFINED;
            }

            for (PluralCategory pluralCategory : VALUES) {
                if (identifier.equals(pluralCategory.getIdentifier())) {
                    return pluralCategory;
                }
            }

            return PluralCategory.UNDEFINED;
        }
    }

    /**
     * Represents all the grammatical cases that are supported by CLDR.
     *
     * @stable ICU 72
     */
    public enum GrammaticalCase {
        /**
         * A possible setting for GrammaticalCase. The grammatical case context to be used is unknown
         * (this is the default value).
         *
         * @stable ICU 72
         */
        UNDEFINED("undefined"),

        /**
         * @stable ICU 72
         */
        ABLATIVE("ablative"),

        /**
         * @stable ICU 72
         */
        ACCUSATIVE("accusative"),

        /**
         * @stable ICU 72
         */
        COMITATIVE("comitative"),

        /**
         * @stable ICU 72
         */
        DATIVE("dative"),

        /**
         * @stable ICU 72
         */
        ERGATIVE("ergative"),

        /**
         * @stable ICU 72
         */
        GENITIVE("genitive"),

        /**
         * @stable ICU 72
         */
        INSTRUMENTAL("instrumental"),

        /**
         * @stable ICU 72
         */
        LOCATIVE("locative"),

        /**
         * @stable ICU 72
         */
        LOCATIVE_COPULATIVE("locative_copulative"),

        /**
         * @stable ICU 72
         */
        NOMINATIVE("nominative"),

        /**
         * @stable ICU 72
         */
        OBLIQUE("oblique"),

        /**
         * @stable ICU 72
         */
        PREPOSITIONAL("prepositional"),

        /**
         * @stable ICU 72
         */
        SOCIATIVE("sociative"),

        /**
         * @stable ICU 72
         */
        VOCATIVE("vocative");

        private final String identifier;

        private GrammaticalCase(String identifier) {
            this.identifier = identifier;
        }

        /**
         * Unmodifiable List of all grammatical cases constants. List version of {@link #values()}.
         *
         * @stable ICU 72
         */
        public static final List<GrammaticalCase> VALUES =
                Collections.unmodifiableList(Arrays.asList(GrammaticalCase.values()));

        /**
         * @return the lowercase CLDR keyword string for the grammatical case.
         * @stable ICU 72
         */
        public final String getIdentifier() {
            return this.identifier;
        }

        /**
         * @param identifier in lower case such as "dative" or "nominative"
         * @return the plural category corresponding to the identifier, or {@code UNDEFINED}
         * @stable ICU 72
         */
        public static final GrammaticalCase fromIdentifier(String identifier) {
            if (identifier == null) {
                return GrammaticalCase.UNDEFINED;
            }

            for (GrammaticalCase grammaticalCase : VALUES) {
                if (identifier.equals(grammaticalCase.getIdentifier())) {
                    return grammaticalCase;
                }
            }

            return GrammaticalCase.UNDEFINED;
        }
    }
}
