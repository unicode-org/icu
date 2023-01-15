// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.text;

import java.util.Locale;
import java.util.Set;

/**
 * An object used to provide name data to the PersonNameFormatter for formatting.
 * Clients can implement this interface to talk directly to some other subsystem
 * that actually contains the name data (instead of having to copy it into a separate
 * object just for formatting) or to override the default modifier behavior described
 * above.  A concrete SimplePersonName object that does store the field values directly
 * is provided.
 *
 * @internal ICU 72 technology preview
 * @see SimplePersonName
 * @deprecated This API is for technology preview only.
 */
@Deprecated
public interface PersonName {
    //==============================================================================
    // Identifiers used to request field values from the PersonName object

    /**
     * Identifiers for the name fields supported by the PersonName object.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    enum NameField {
        /**
         * Contains titles such as "Mr.", "Dr." (in English these typically
         * precede the name)
         * @internal ICU 73 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        TITLE("title"),

        /**
         * The given name.  May contain more than one token.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        GIVEN("given"),

        /**
         * Additional given names.  (In English, this is usually the "middle name" and
         * may contain more than one word.)
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        GIVEN2("given2"),

        /**
         * The surname.  In Spanish, this is the patronymic surname.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        SURNAME("surname"),

        /**
         * Additional surnames.  This is only used in a few languages, such as Spanish,
         * where it is the matronymic surname.  (In most languages, multiple surnames all
         * just go in the SURNAME field.)
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        SURNAME2("surname2"),

        /**
         * Generational qualifiers that in English generally follow the actual name,
         * such as "Jr." or "III".
         * @internal ICU 73 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        GENERATION("generation"),

        /**
         * Professional qualifiers that in English generally follow the actual name,
         * such as "M.D." or "J.D.".
         * @internal ICU 73 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        CREDENTIALS("credentials"),

        /**
         * The preferred field order for the name.  PersonName objects generally shouldn't provide
         * this field, allowing the PersonNameFormatter to deduce the proper field order based on
         * the locales of the name of the formatter.  But this can be used to force a particular
         * field order, generally in cases where the deduction logic in PersonNameFormatter would
         * guess wrong.  When used, the only valid values are "givenFirst" and "surnameFirst".
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
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
        @Deprecated
        @Override
        public String toString() {
            return name;
        }

        /**
         * Returns the appropriate NameField for its display name.
         * @internal
         * @deprecated This API is for ICU internal use only.
         */
        @Deprecated
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
    @Deprecated
    enum FieldModifier {
        /**
         * Requests an "informal" variant of the field, generally a nickname of some type:
         * if "given" is "James", "given-informal" might be "Jimmy".  Only applied to the "given"
         * field.  If the PersonName object doesn't apply this modifier, PersonNameFormatter just
         * uses the unmodified version of "given".
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        INFORMAL("informal"),

        /**
         * If the field contains a main word with one or more separate prefixes, such as
         * "van den Hul", this requests just the prefixes ("van den").  Only applied to the "surname"
         * field.  If the PersonName object doesn't apply this modifier, PersonNameFormatter
         * assumes there are no prefixes.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        PREFIX("prefix"),

        /**
         * If the field contains a main word with one or more separate prefixes, such as
         * "van den Hul", this requests just the main word ("Hul").  Only applied to the "surname"
         * field.  If the implementing class doesn't apply this modifier, PersonNameFormatter
         * assumes the entire "surname" field is the "core".
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        CORE("core"),

        /**
         * Requests an initial for the specified field.  PersonNameFormatter will do
         * this algorithmically, but a PersonName object can apply this modifier itself if it wants
         * different initial-generation logic (or stores the initial separately).
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        INITIAL("initial"),

        /**
         * Requests an initial for the specified field, suitable for use in a monogram
         * (this usually differs from "initial" in that "initial" often adds a period and "monogram"
         * never does).  PersonNameFormatter will do this algorithmically, but a PersonName object can
         * apply this modifier itself if it wants different monogram-generation logic.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        MONOGRAM("monogram"),

        /**
         * Requests the field value converted to ALL CAPS.  PersonName objects
         * generally won't need to handle this modifier themselves.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        ALL_CAPS("allCaps"),

        /**
         * Requests the field value with the first grapheme of each word converted to titlecase.
         * A PersonName object might handle this modifier itself to capitalize words more
         * selectively.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
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
        @Deprecated
        @Override
        public String toString() {
            return name;
        }

        /**
         * Returns the appropriate fieldModifier for its display name.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
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
    // Public API on PersonName
    /**
     * Returns the locale of the name-- that is, the language or country of origin for the person being named.
     * An implementing class is allowed to return null here to indicate the name's locale is unknown.
     *
     * @return The name's locale, or null if it's not known.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public Locale getNameLocale();

    /**
     * Returns one field of the name, possibly in a modified form.
     *
     * @param identifier The identifier of the requested field.
     * @param modifiers  An **IN/OUT** parameter that specifies modifiers to apply to the basic field value.
     *                   An implementing class can choose to handle or ignore any modifiers; it should modify
     *                   the passed-in Set so that on exit, it contains only the requested modifiers that it
     *                   DIDN'T handle.  This parameter may not be null, and must either be mutable or empty.
     * @return The value of the requested field, optionally modified by some or all of the requested modifiers, or
     * null if the requested field isn't present in the name.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public String getFieldValue(NameField identifier, Set<FieldModifier> modifiers);
}
