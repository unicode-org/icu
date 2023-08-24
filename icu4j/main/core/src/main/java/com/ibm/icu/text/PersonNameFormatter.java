// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.text;

import java.util.Locale;

import com.ibm.icu.impl.personname.PersonNameFormatterImpl;

/**
 * A class for formatting names of people.  Takes raw name data for a person and renders it into a string according to
 * the caller's specifications, taking into account how people's names are rendered in the caller's locale.
 *
 * The Length, Usage, and Formality options can be used to get a wide variety of results.  In English, they would
 * produce results along these lines:
 * <table border="1">
 *     <tr>
 *         <th rowspan="2">
 *         </th>
 *         <th colspan="2">
 *             REFERRING
 *         </th>
 *         <th colspan="2">
 *             ADDRESSING
 *         </th>
 *         <th colspan="2">
 *             MONOGRAM
 *         </th>
 *     </tr>
 *     <tr>
 *         <th>FORMAL</th>
 *         <th>INFORMAL</th>
 *         <th>FORMAL</th>
 *         <th>INFORMAL</th>
 *         <th>FORMAL</th>
 *         <th>INFORMAL</th>
 *     </tr>
 *     <tr>
 *         <th>LONG</th>
 *         <td>James Earl Carter Jr.</td>
 *         <td>Jimmy Carter</td>
 *         <td>Mr. Carter</td>
 *         <td>Jimmy</td>
 *         <td>JEC</td>
 *         <td>JC</td>
 *     </tr>
 *     <tr>
 *         <th>MEDIUM</th>
 *         <td>James E. Carter Jr.</td>
 *         <td>Jimmy Carter</td>
 *         <td>Mr. Carter</td>
 *         <td>Jimmy</td>
 *         <td>C</td>
 *         <td>J</td>
 *     </tr>
 *     <tr>
 *         <th>SHORT</th>
 *         <td>J. E. Carter</td>
 *         <td>Jimmy Carter</td>
 *         <td>Mr. Carter</td>
 *         <td>Jimmy</td>
 *         <td>C</td>
 *         <td>J</td>
 *     </tr>
 * </table>
 *
 * @draft ICU 73
 */
public class PersonNameFormatter {
    //==============================================================================
    // Parameters that control formatting behavior

    /**
     * Specifies the desired length of the formatted name.
     * @draft ICU 73
     */
    public enum Length {
        /**
         * The longest name length.  Generally uses most of the fields in the name object.
         * @draft ICU 73
         */
        LONG,

        /**
         * The most typical name length.  Generally includes the given name and surname, but generally
         * not most of the other fields.
         * @draft ICU 73
         */
        MEDIUM,

        /**
         * A shortened name.  Skips most fields and may abbreviate some name fields to just their initials.
         * When Formality is INFORMAL, may only include one field.
         * @draft ICU 73
         */
        SHORT,

        /**
         * The default name length for the locale.  For most locales, this is the same as MEDIUM.
         * @draft ICU 74
         */
        DEFAULT
    }

    /**
     * Specifies the intended usage of the formatted name.
     * @draft ICU 73
     */
    public enum Usage {
        /**
         * Used for when the name is going to be used to address the user directly: "Turn left here, John."
         * @draft ICU 73
         */
        ADDRESSING,

        /**
         * Used in general cases, when the name is used to refer to somebody else.
         * @draft ICU 73
         */
        REFERRING,

        /**
         * Used to generate monograms, short 1 to 3-character versions of the name suitable for use in things
         * like chat avatars.  In English, this is usually the person's initials, but this isn't true in all
         * languages.  When the caller specifies Usage.MONOGRAM, the Length parameter can be used to get different
         * lengths of monograms: Length.SHORT is generally a single letter; Length.LONG may be as many as three or four.
         * @draft ICU 73
         */
        MONOGRAM
    }

    /**
     * Specifies the intended formality of the formatted name.
     * @draft ICU 73
     */
    public enum Formality {
        /**
         * The more formal version of the name.
         * @draft ICU 73
         */
        FORMAL,

        /**
         * The more informal version of the name.  In English, this might omit fields or use the "informal" variant
         * of the given name.
         * @draft ICU 73
         */
        INFORMAL,

        /**
         * The default formality for the locale.  For most locales, this is the same as FORMAL, but for English,
         * this is the same as INFORMAL.
         * @draft ICU 74
         */
        DEFAULT
    }

    /**
     * An enum indicating the desired display order for a formatted name.
     * @draft ICU 73
     */
    public enum DisplayOrder {
        /**
         * The default display order; used to indicate normal formatting.
         * @draft ICU 73
         */
        DEFAULT,

        /**
         * Used to indicate a display order suitable for use in a sorted list:
         * For English, this would put the surnames first, with a comma between them and the rest
         * of the name: "Smith, John".
         * @draft ICU 73
         */
        SORTING,

        /**
         * Forces the formatter to format the name in given-first order.  If the name itself specifies
         * a display order, this overrides it.
         * @draft ICU 74
         */
        FORCE_GIVEN_FIRST,

        /**
         * Forces the formatter to format the name in surname-first order.  If the name itself specifies
         * a display order, this overrides it.
         * @draft ICU 74
         */
        FORCE_SURNAME_FIRST,
    }

    private final PersonNameFormatterImpl impl;

    //==============================================================================
    // Builder for PersonNameFormatter

    /**
     * A utility class that can be used to construct a PersonNameFormatter.
     * Use PersonNameFormatter.builder() to get a new instance.
     * @draft ICU 73
     */
    public static class Builder {
        /**
         * Sets the locale for the formatter to be constructed.
         * @param locale The new formatter locale.  May not be null.
         * @return This builder.
         * @draft ICU 73
         */
        public Builder setLocale(Locale locale) {
            if (locale != null) {
                this.locale = locale;
            }
            return this;
        }

        /**
         * Sets the name length for the formatter to be constructed.
         * @param length The new name length.
         * @return This builder.
         * @draft ICU 73
         */
        public Builder setLength(Length length) {
            this.length = length;
            return this;
        }

        /**
         * Sets the name usage for the formatter to be constructed.
         * @param usage The new name length.
         * @return This builder.
         * @draft ICU 73
         */
        public Builder setUsage(Usage usage) {
            this.usage = usage;
            return this;
        }

        /**
         * Sets the name formality for the formatter to be constructed.
         * @param formality The new name length.
         * @return This builder.
         * @draft ICU 73
         */
        public Builder setFormality(Formality formality) {
            this.formality = formality;
            return this;
        }

        /**
         * Specifies the desired display order for the formatted names.  This can be either SORTING,
         * which requests that names be formatted in a manner suitable for inclusion in a sorted list
         * (e.g., in English, "Smith, John"), or DEFAULT, which gives the standard field order suitable
         * for most contexts (e.g., in English, "John Smith").
         * @param order The desired display order for formatted names.
         * @return This builder.
         * @draft ICU 73
         */
        public Builder setDisplayOrder(DisplayOrder order) {
            this.displayOrder = order;
            return this;
        }

        /**
         * Requests that the surname in the formatted result be rendered in ALL CAPS.  This is often done with
         * Japanese names to highlight which name is the surname.
         * @param allCaps If true, the surname in the formatted result will be rendered in ALL CAPS.
         * @return This builder.
         * @draft ICU 73
         */
        public Builder setSurnameAllCaps(boolean allCaps) {
            this.surnameAllCaps = allCaps;
            return this;
        }

        /**
         * Returns a new PersonNameFormatter with the values that were passed to this builder.
         * This method doesn't freeze or delete the builder; you can call build() more than once
         * (presumably after calling the other methods to change the parameter) to create more
         * than one PersonNameFormatter; you don't need a new Builder for each PersonNameFormatter.
         * @return A new PersonNameFormatter.
         * @draft ICU 73
         */
        public PersonNameFormatter build() {
            return new PersonNameFormatter(locale, length, usage, formality, displayOrder, surnameAllCaps);
        }

        private Builder() {
       }

        private Locale locale = Locale.getDefault();
        private Length length = Length.DEFAULT;
        private Usage usage = Usage.REFERRING;
        private Formality formality = Formality.DEFAULT;
        private DisplayOrder displayOrder = DisplayOrder.DEFAULT;
        private boolean surnameAllCaps = false;
    }

    //==============================================================================
    // Public API on PersonNameFormatter

    /**
     * Returns a Builder object that can be used to construct a new PersonNameFormatter.
     * @return A new Builder.
     * @draft ICU 73
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a Builder object whose fields match those used to construct this formatter,
     * allowing a new formatter to be created based on this one.
     * @return A new Builder that can be used to create a new formatter based on this formatter.
     * @draft ICU 73
     */
    public Builder toBuilder() {
        Builder builder = builder();
        builder.setLocale(impl.getLocale());
        builder.setLength(impl.getLength());
        builder.setUsage(impl.getUsage());
        builder.setFormality(impl.getFormality());
        builder.setDisplayOrder(impl.getDisplayOrder());
        builder.setSurnameAllCaps(impl.getSurnameAllCaps());
        return builder;
    }

    /**
     * Formats a name.
     * @param name A PersonName object that supplies individual field values (optionally, with modifiers applied)
     *             to the formatter for formatting.
     * @return The name, formatted according to the locale and other parameters passed to the formatter's constructor.
     * @draft ICU 73
     */
    public String formatToString(PersonName name) {
        // TODO: Add a format() method that returns a FormattedPersonName object that descends from FormattedValue.
        return impl.formatToString(name);
    }

    //==============================================================================
    // Internal implementation
    private PersonNameFormatter(Locale locale, Length length, Usage usage, Formality formality, DisplayOrder displayOrder, boolean surnameAllCaps) {
        this.impl = new PersonNameFormatterImpl(locale, length, usage, formality, displayOrder, surnameAllCaps);
    }

    /**
     * @internal For unit testing only!
     * @deprecated This API is for unit testing only.
     */
    @Deprecated
    public PersonNameFormatter(Locale locale, String[] gnFirstPatterns, String[] snFirstPatterns, String[] gnFirstLocales, String[] snFirstLocales) {
        this.impl = new PersonNameFormatterImpl(locale, gnFirstPatterns, snFirstPatterns, gnFirstLocales, snFirstLocales);
    }

    /**
     * @internal For debugging only!
     * @deprecated This API is for debugging only.
     */
    @Override
    public String toString() {
        return impl.toString();
    }
}
