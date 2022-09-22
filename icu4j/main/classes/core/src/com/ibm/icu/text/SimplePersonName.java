// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.text;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * A concrete implementation of PersonNameFormatter.PersonName that simply stores the field
 * values in a Map.
 *
 * A caller can store both raw field values (such as "given") and modified field values (such as "given-informal")
 * in a SimplePersonName.  But beyond storing and returning modified field values provided to it by the caller,
 * SimplePersonName relies on the PersonNameFormatter's default handling of field modifiers.
 * @internal ICU 72 technology preview
 * @deprecated This API is for technology preview only.
 */
@Deprecated
public class SimplePersonName implements PersonName {
    /**
     * A utility class for constructing a SimplePersonName.  Use SimplePersonName.builder()
     * to get a new Builder instance.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Builder {
        /**
         * Set the locale for the new name object.
         * @param locale The locale for the new name object.  Can be null, which indicates the
         *               name's locale is unknown.
         * @return This builder.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder setLocale(Locale locale) {
            this.locale = locale;
            return this;
        }

        /**
         * Sets the value for one field (with optional modifiers) in the new name object.
         * @param field A NameField object specifying the field to set.
         * @param modifiers A collection of FieldModifier objects for any modifiers that apply
         *                  to this field value.  May be null, which is the same as the empty set.
         * @param value The value for this field.
         * @return This builder.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder addField(NameField field,
                                Collection<FieldModifier> modifiers,
                                String value) {
            // generate the modifiers' internal names, and sort them alphabetically
            Set<String> modifierNames = new TreeSet<>();
            if (modifiers != null) {
                for (FieldModifier modifier : modifiers) {
                    modifierNames.add(modifier.toString());
                }
            }

            // construct the modified field name, which is the field name, with all the modifier names attached in
            // alphabetical order, delimited by hyphens
            StringBuilder fieldName = new StringBuilder();
            fieldName.append(field.toString());
            for (String modifierName : modifierNames) {
                fieldName.append("-");
                fieldName.append(modifierName);
            }

            fieldValues.put(fieldName.toString(), value);
            return this;
        }

        /**
         * Returns a SimplePersonName with the field values and name locale that were passed to this builder.
         * @return A SimplePersonName with the field values and name locale that were passed to this builder.
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public SimplePersonName build() {
            // special-case code for the "surname" field -- if it isn't specified, but "surname-prefix" and
            // "surname-core" both are, let "surname" be the other two fields joined with a space
            if (fieldValues.get("surname") == null) {
                String surnamePrefix = fieldValues.get("surname-prefix");
                String surnameCore = fieldValues.get("surname-core");
                if (surnamePrefix != null && surnameCore != null) {
                    fieldValues.put("surname", surnamePrefix + " " + surnameCore);
                }
            }

            return new SimplePersonName(locale, fieldValues);
        }

        private Builder() {
            locale = null;
            fieldValues = new HashMap<>();
        }

        private Locale locale;
        private Map<String, String> fieldValues;
    }

    /**
     * Returns a Builder object that can be used to construct a new SimplePersonName object.
     * @return A Builder object that can be used to construct a new SimplePersonName object.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Internal constructor used by the Builder object.
     */
    private SimplePersonName(Locale nameLocale, Map<String, String> fieldValues) {
        this.nameLocale = nameLocale;
        this.fieldValues = new HashMap<>(fieldValues);
    }

    /**
     * Returns the locale of the name-- that is, the language or country of origin for the person being named.
     * @return The name's locale, or null if it's unknown.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Override
    @Deprecated
    public Locale getNameLocale() {
        return nameLocale;
    }

    /**
     * Returns one field of the name, possibly in a modified form.  This class can store modified versions of fields,
     * provided at construction time, and this function will return them.  Otherwise, it ignores modifiers and
     * relies on PersonNameFormat's default modifier handling.
     * @param nameField The identifier of the requested field.
     * @param modifiers An **IN/OUT** parameter that specifies modifiers to apply to the basic field value.
     *                  On return, this list will contain any modifiers that this object didn't handle.  This class
     *                  will always return this set unmodified, unless a modified version of the requested field
     *                  was provided at construction time.
     * @return The value of the requested field, optionally modified by some or all of the requested modifiers, or
     * null if the requested field isn't present in the name.
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Override
    @Deprecated
    public String getFieldValue(NameField nameField, Set<FieldModifier> modifiers) {
        // first look for the fully modified name in the internal table
        String fieldName = nameField.toString();
        String result = fieldValues.get(makeModifiedFieldName(nameField, modifiers));
        if (result != null) {
            modifiers.clear();
            return result;
        }

        // if we don't find it, check the fully unmodified name.  If it's not there, nothing else will be
        result = fieldValues.get(fieldName);
        if (result == null) {
            return null;
        } else if (modifiers.size() == 1) {
            // and if it IS there and there's only one modifier, we're done
            return result;
        }

        // but if there are two or more modifiers, then we have to go through the whole list of fields and look for the best match
        String winningKey = fieldName;
        int winningScore = 0;
        for (String key : fieldValues.keySet()) {
            if (key.startsWith(fieldName)) {
                Set<FieldModifier> keyModifiers = makeModifiersFromName(key);
                if (modifiers.containsAll(keyModifiers)) {
                    if (keyModifiers.size() > winningScore || (keyModifiers.size() == winningScore && key.compareTo(winningKey) < 0)) {
                        winningKey = key;
                        winningScore = keyModifiers.size();
                    }
                }
            }
        }
        result = fieldValues.get(winningKey);
        modifiers.removeAll(makeModifiersFromName(winningKey));
        return result;
    }

    private static String makeModifiedFieldName(NameField fieldName,
                                                Collection<FieldModifier> modifiers) {
        StringBuilder result = new StringBuilder();
        result.append(fieldName);

        TreeSet<String> sortedModifierNames = new TreeSet<>();
        for (FieldModifier modifier : modifiers) {
            sortedModifierNames.add(modifier.toString());
        }
        for (String modifierName : sortedModifierNames) {
            result.append("-");
            result.append(modifierName);
        }
        return result.toString();
    }

    private static Set<FieldModifier> makeModifiersFromName(String modifiedName) {
        StringTokenizer tok = new StringTokenizer(modifiedName, "-");
        Set<FieldModifier> result = new HashSet<>();
        String fieldName = tok.nextToken(); // throw away the field name
        while (tok.hasMoreTokens()) {
            result.add(PersonName.FieldModifier.forString(tok.nextToken()));
        }
        return result;
    }

    private final Locale nameLocale;
    private final Map<String, String> fieldValues;
}