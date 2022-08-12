// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.text;

import com.ibm.icu.util.ULocale;

import java.util.*;

/**
 * A concrete implementation of PersonNameFormatter.PersonName that simply stores the field
 * values in a Map.
 *
 * A caller can store both raw field values (such as "given") and modified field values (such as "given-informal")
 * in a SimplePersonName.  But beyond storing and returning modified field values provided to it by the caller,
 * SimplePersonName relies on the PersonNameFormatter's default handling of field modifiers.
 * @internal
 */
public class SimplePersonName implements PersonNameFormatter.PersonName {
    /**
     * Simple constructor.
     * @param nameLocale The locale of the name (i.e., its ethnic or national origin).
     * @param fieldValues A Map mapping from field names to field values.  The field names
     *                    are the values returned by NameField.toString().
     * @internal
     */
    public SimplePersonName(ULocale nameLocale, Map<String, String> fieldValues) {
        this.nameLocale = nameLocale;
        this.fieldValues = new HashMap<>(fieldValues);
    }

    /**
     * A constructor that takes the locale ID and field values as a single String.  This constructor is really
     * intended only for the use of the PersonNameFormatter unit tests.
     * @param keysAndValues A single string containing the locale ID and field values.  This string is organized
     *                      into key-value pairs separated by commas.  The keys are separated from the values
     *                      by equal signs.  The keys themselves are field names, as returned by
     *                      NameField.toString(), optionally followed by a hyphen-delimited set of modifier names,
     *                      as returned by FieldModifier.toString().
     * @internal
     */
    public SimplePersonName(String keysAndValues) {
        this.fieldValues = new HashMap<>();

        StringTokenizer tok = new StringTokenizer(keysAndValues, ",");
        ULocale tempLocale = null;
        while (tok.hasMoreTokens()) {
            String entry = tok.nextToken();
            int equalPos = entry.indexOf('=');
            if (equalPos < 0) {
                throw new IllegalArgumentException("No = found in name field entry");
            }
            String fieldName = entry.substring(0, equalPos);
            String fieldValue = entry.substring(equalPos + 1);

            if (fieldName.equals("locale")) {
                tempLocale = new ULocale(fieldValue);
            } else {
                this.fieldValues.put(fieldName, fieldValue);
            }
        }
        this.nameLocale = tempLocale;

        // special-case code for the "surname" field-- if it isn't specified, but "surname-prefix" and
        // "surname-core" both are, let "surname" be the other two fields joined with a space
        if (this.fieldValues.get("surname") == null) {
            String surnamePrefix = this.fieldValues.get("surname-prefix");
            String surnameCore = this.fieldValues.get("surname-core");
            if (surnamePrefix != null && surnameCore != null) {
                this.fieldValues.put("surname", surnamePrefix + " " + surnameCore);
            }
        }
    }

    /**
     * Returns the locale of the name-- that is, the language or country of origin for the person being named.
     * @return The name's locale.
     * @internal
     */
    @Override
    public ULocale getNameLocale() {
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
     * @internal
     */
    @Override
    public String getFieldValue(PersonNameFormatter.NameField nameField, Set<PersonNameFormatter.FieldModifier> modifiers) {
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
                Set<PersonNameFormatter.FieldModifier> keyModifiers = makeModifiersFromName(key);
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

    private static String makeModifiedFieldName(PersonNameFormatter.NameField fieldName,
                                                Collection<PersonNameFormatter.FieldModifier> modifiers) {
        StringBuilder result = new StringBuilder();
        result.append(fieldName);

        TreeSet<String> sortedModifierNames = new TreeSet<>();
        for (PersonNameFormatter.FieldModifier modifier : modifiers) {
            sortedModifierNames.add(modifier.toString());
        }
        for (String modifierName : sortedModifierNames) {
            result.append("-");
            result.append(modifierName);
        }
        return result.toString();
    }

    private static Set<PersonNameFormatter.FieldModifier> makeModifiersFromName(String modifiedName) {
        StringTokenizer tok = new StringTokenizer(modifiedName, "-");
        Set<PersonNameFormatter.FieldModifier> result = new HashSet<>();
        String fieldName = tok.nextToken(); // throw away the field name
        while (tok.hasMoreTokens()) {
            result.add(PersonNameFormatter.FieldModifier.forString(tok.nextToken()));
        }
        return result;
    }

    private final ULocale nameLocale;
    private final Map<String, String> fieldValues;
}