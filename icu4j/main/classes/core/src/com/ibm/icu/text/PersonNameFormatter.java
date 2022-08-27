// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.text;

import com.ibm.icu.impl.personname.PersonNameFormatterImpl;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A class for formatting names of people.  Takes raw name data for a person and renders it into a string according to
 * the caller's specifications, taking into account how people's names are rendered in the caller's locale.
 *
 * The Length, Usage, and Formality options can be used to get a wide variety of results.  In English, they would
 * produce results along these lines:
 *
 * |        | REFERRING             | REFERRING    | ADDRESSING | ADDRESSING | MONOGRAM | MONOGRAM |
 * |        | FORMAL                | INFORMAL     | FORMAL     | INFORMAL   | FORMAL   | INFORMAL |
 * |--------|-----------------------|--------------|------------|------------|----------|----------|
 * | LONG   | James Earl Carter Jr. | Jimmy Carter | Mr. Carter | Jimmy      | JEC      | JC       |
 * | MEDIUM | James E. Carter Jr.   | Jimmy Carter | Mr. Carter | Jimmy      | C        | J        |
 * | SHORT  | J. E. Carter          | Jimmy Carter | Mr. Carter | Jimmy      | C        | J        |
 *
 * @internal ICU 72 technology preview
 * @deprecated This API is for technology preview only.
 */
public class PersonNameFormatter {
    //==============================================================================
    // Parameters that control formatting behavior

    /**
     * Specifies the desired length of the formatted name.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    public enum Length {
        /**
         * The longest name length.  Generally uses most of the fields in the name object.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        LONG,

        /**
         * The most typical name length.  Generally includes the given name and surname, but generally
         * not most of the other fields.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        MEDIUM,

        /**
         * A shortened name.  Skips most fields and may abbreviate some name fields to just their initials.
         * When Formality is INFORMAL, may only include one field.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        SHORT
    }

    /**
     * Specifies the intended usage of the formatted name.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    public enum Usage {
        /**
         * Used for when the name is going to be used to address the user directly: "Turn left here, John."
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        ADDRESSING,

        /**
         * Used in general cases, when the name is used to refer to somebody else.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        REFERRING,

        /**
         * Used to generate monograms, short 1 to 3-character versions of the name suitable for use in things
         * like chat avatars.  In English, this is usually the person's initials, but this isn't true in all
         * languages.  When the caller specifies Usage.MONOGRAM, the Length parameter can be used to get different
         * lengths of monograms: Length.SHORT is generally a single letter; Length.LONG may be as many as three or four.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        MONOGRAM
    }

    /**
     * Specifies the intended formality of the formatted name.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    public enum Formality {
        /**
         * The more formal version of the name.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        FORMAL,

        /**
         * The more informal version of the name.  In English, this might omit fields or use the "informal" variant
         * of the given name.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        INFORMAL
    }

    /**
     * Additional options to customize the behavior of the formatter.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    public enum Options {
        /**
         * Causes the formatter to generate results suitable for inclusion in a sorted list.  For GN-first languages,
         * this generally means moving the surname to the beginning of the string, with a comma between it and
         * the rest of the name: e.g., "Carter, James E. Jr.".
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        SORTING,

        /**
         * Requests that the surname in the formatted result be rendered in ALL CAPS.  This is often done with
         * Japanese names to highlight which name is the surname.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        SURNAME_ALLCAPS
    }

    //==============================================================================
    // Identifiers used to request field values from the PersonName object

    /**
     * Identifiers for the name fields supported by the PersonName object.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    public enum NameField {
        /**
         * Contains titles and other words that precede the actual name, such as "Mr."
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        PREFIX("prefix"),

        /**
         * The given name.  May contain more than one token.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        GIVEN("given"),

        /**
         * Additional given names.  (In English, this is usually the "middle name" and
         * may contain more than one word.)
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        GIVEN2("given2"),

        /**
         * The surname.  In Spanish, this is the patronymic surname.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        SURNAME("surname"),

        /**
         * Additional surnames.  This is only used in a few languages, such as Spanish,
         * where it is the matronymic surname.  (In most languages, multiple surnames all
         * just go in the SURNAME field.)
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        SURNAME2("surname2"),

        /**
         * Generational and professional qualifiers that generally follow the actual name,
         * such as "Jr." or "M.D."
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        SUFFIX("suffix"),

        /**
         * The preferred field order for the name.  PersonName objects generally shouldn't provide
         * this field, allowing the PersonNameFormatter to deduce the proper field order based on
         * the locales of the name of the formatter.  But this can be used to force a particular
         * field order, generally in cases where the deduction logic in PersonNameFormatter would
         * guess wrong.  When used, the only valid values are "givenFirst" and "surnameFirst".
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        PREFERRED_ORDER("preferredOrder");

        private final String name;

        private NameField(String name) {
            this.name = name;
        }

        /**
         * Returns the NameField's display name.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Override
        public String toString() {
            return name;
        }

        /**
         * Returns the appropriate NameField for its display name.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        public static NameField forString(String name) {
            for (NameField field : values()) {
                if (field.name.equals(name)) {
                    return field;
                }
            }
            throw new IllegalArgumentException("Invalid field name " + name);
        }
    }

    /**
     * Identifiers for the name field modifiers supported by the PersonName and PersonNameFormatter objects.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    public enum FieldModifier {
        /**
         * Requests an "informal" variant of the field, generally a nickname of some type:
         * if "given" is "James", "given-informal" might be "Jimmy".  Only applied to the "given"
         * field.  If the PersonName object doesn't apply this modifier, PersonNameFormatter just
         * uses the unmodified version of "given".
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        INFORMAL("informal"),

        /**
         * If the field contains a main word with one or more separate prefixes, such as
         * "van den Hul", this requests just the prefixes ("van den").  Only applied to the "surname"
         * field.  If the PersonName object doesn't apply this modifier, PersonNameFormatter
         * assumes there are no prefixes.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        PREFIX("prefix"),

        /**
         * If the field contains a main word with one or more separate prefixes, such as
         * "van den Hul", this requests just the main word ("Hul").  Only applied to the "surname"
         * field.  If the implementing class doesn't apply this modifier, PersonNameFormatter
         * assumes the entire "surname" field is the "core".
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        CORE("core"),

        /**
         * Requests an initial for the specified field.  PersonNameFormatter will do
         * this algorithmically, but a PersonName object can apply this modifier itself if it wants
         * different initial-generation logic (or stores the initial separately).
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        INITIAL("initial"),

        /**
         * Requests an initial for the specified field, suitable for use in a monogram
         * (this usually differs from "initial" in that "initial" often adds a period and "monogram"
         * never does).  PersonNameFormatter will do this algorithmically, but a PersonName object can
         * apply this modifier itself if it wants different monogram-generation logic.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        MONOGRAM("monogram"),

        /**
         * Requests the field value converted to ALL CAPS.  PersonName objects
         * generally won't need to handle this modifier themselves.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        ALL_CAPS("allCaps"),

        /**
         * Requests the field value with the first grapheme of each word converted to titlecase.
         * A PersonName object might handle this modifier itself to capitalize words more
         * selectively.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        INITIAL_CAP("initialCap");

        private final String name;

        private FieldModifier(String name) {
            this.name = name;
        }

        /**
         * Returns the FieldModifier's display name.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Override
        public String toString() {
            return name;
        }

        /**
         * Returns the appropriate fieldModifier for its display name.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        public static FieldModifier forString(String name) {
            for (FieldModifier modifier : values()) {
                if (modifier.name.equals(name)) {
                    return modifier;
                }
            }
            throw new IllegalArgumentException("Invalid modifier name " + name);
        }
    }

    //==============================================================================
    // The PersonName object

    /**
     * An object used to provide name data to the PersonNameFormatter for formatting.
     * Clients can implement this interface to talk directly to some other subsystem
     * that actually contains the name data (instead of having to copy it into a separate
     * object just for formatting) or to override the default modifier behavior described
     * above.  A concrete SimplePersonName object that does store the field values directly
     * is provided.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     * @see SimplePersonName
     */
    public interface PersonName {
        /**
         * Returns the locale of the name-- that is, the language or country of origin for the person being named.
         * @return The name's locale, or null if it's not known.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        public Locale getNameLocale();

        /**
         * Returns one field of the name, possibly in a modified form.
         * @param identifier The identifier of the requested field.
         * @param modifiers An **IN/OUT** parameter that specifies modifiers to apply to the basic field value.
         *                  An implementing class can choose to handle or ignore any modifiers; it should modify
         *                  this parameter so that on exit, it contains only the requested modifiers that it
         *                  DIDN'T handle.
         * @return The value of the requested field, optionally modified by some or all of the requested modifiers, or
         * null if the requested field isn't present in the name.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        public String getFieldValue(NameField identifier, Set<FieldModifier> modifiers);
    }

    private final PersonNameFormatterImpl impl;

    //==============================================================================
    // Builder for PersonNameformatter

    /**
     * A utility class that can be used to construct a PersonNameFormatter.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    public static class Builder {
        /**
         * Sets the locale for the formatter to be constructed.
         * @param locale The new formatter locale.
         * @return This builder.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        public Builder setLocale(Locale locale) {
            this.locale = locale;
            return this;
        }

        /**
         * Sets the name length for the formatter to be constructed.
         * @param length The new name length.
         * @return This builder.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        public Builder setLength(Length length) {
            this.length = length;
            return this;
        }

        /**
         * Sets the name usage for the formatter to be constructed.
         * @param usage The new name length.
         * @return This builder.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        public Builder setUsage(Usage usage) {
            this.usage = usage;
            return this;
        }

        /**
         * Sets the name formality for the formatter to be constructed.
         * @param formality The new name length.
         * @return This builder.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        public Builder setFormality(Formality formality) {
            this.formality = formality;
            return this;
        }

        /**
         * Sets the options set for the formatter to be constructed.
         * @param options The new options set.
         * @return This builder.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        public Builder setOptions(Set<Options> options) {
            this.options = options;
            return this;
        }

        /**
         * Returns a new PersonNameFormatter with the values that were passed to this builder.
         * @return A new PersonNameFormatter.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        public PersonNameFormatter build() {
            return new PersonNameFormatter(locale, length, usage, formality, options);
        }

        private Builder() {
       }

        private Locale locale = null;
        private Length length = Length.MEDIUM;
        private Usage usage = Usage.REFERRING;
        private Formality formality = Formality.FORMAL;
        private Set<Options> options = new HashSet<>();
    }

    //==============================================================================
    // Public API on PersonNameFormatter

    /**
     * Returns a Builder object that can be used to construct a new PersonNameFormatter.
     * @return A new Builder.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a Builder object whose fields match those used to construct this formatter,
     * allowing a new formatter to be created based on this one.
     * @return A new Builder that can be used to create a new formatter based on this formatter.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    public Builder builderFromThis() {
        Builder builder = builder();
        builder.setLocale(impl.getLocale());
        builder.setLength(impl.getLength());
        builder.setUsage(impl.getUsage());
        builder.setFormality(impl.getFormality());
        builder.setOptions(impl.getOptions());
        return builder;
    }

    /**
     * Formats a name.
     * @param name A PersonName object that supplies individual field values (optionally, with modifiers applied)
     *             to the formatter for formatting.
     * @return The name, formatted according to the locale and other parameters passed to the formatter's constructor.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    public String formatToString(PersonName name) {
        // TODO: Add a format() method that returns a FormattedPersonName object that descends from FormattedValue.
        return impl.formatToString(name);
    }

    //==============================================================================
    // Internal implementation
    private PersonNameFormatter(Locale locale, Length length, Usage usage, Formality formality, Set<Options> options) {
        this.impl = new PersonNameFormatterImpl(locale, length, usage, formality, options);
    }

    /**
     * @internal For unit testing only!
     */
    public PersonNameFormatter(Locale locale, String[] patterns) {
        this.impl = new PersonNameFormatterImpl(locale, patterns);
    }
}
