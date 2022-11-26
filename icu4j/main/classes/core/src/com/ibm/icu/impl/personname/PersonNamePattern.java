// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.personname;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.ibm.icu.text.PersonName;

/**
 * A single name formatting pattern, corresponding to a single namePattern element in CLDR.
 */
class PersonNamePattern {
    private String patternText; // for debugging
    private Element[] patternElements;

    public static PersonNamePattern[] makePatterns(String[] patternText, PersonNameFormatterImpl formatterImpl) {
        PersonNamePattern[] result = new PersonNamePattern[patternText.length];
        for (int i = 0; i < patternText.length; i++) {
            result[i] = new PersonNamePattern(patternText[i], formatterImpl);
        }
        return result;
    }

    private PersonNamePattern(String patternText, PersonNameFormatterImpl formatterImpl) {
        this.patternText = patternText;

        List<Element> elements = new ArrayList<>();
        boolean inField = false;
        boolean inEscape = false;
        StringBuilder workingString = new StringBuilder();
        for (int i = 0; i < patternText.length(); i++) {
            char c = patternText.charAt(i);

            if (inEscape) {
                workingString.append(c);
                inEscape = false;
            } else {
                switch (c) {
                    case '\\':
                        inEscape = true;
                        break;
                    case '{':
                        if (!inField) {
                            if (workingString.length() > 0) {
                                elements.add(new LiteralText(workingString.toString()));
                                workingString = new StringBuilder();
                            }
                            inField = true;
                        } else {
                            throw new IllegalArgumentException("Nested braces are not allowed in name patterns");
                        }
                        break;
                    case '}':
                        if (inField) {
                            if (workingString.length() > 0) {
                                elements.add(new NameFieldImpl(workingString.toString(), formatterImpl));
                                workingString = new StringBuilder();
                            } else {
                                throw new IllegalArgumentException("No field name inside braces");
                            }
                            inField = false;
                        } else {
                            throw new IllegalArgumentException("Unmatched closing brace in literal text");
                        }
                        break;
                    default:
                        workingString.append(c);
                }
            }
        }
        if (workingString.length() > 0) {
            elements.add(new LiteralText(workingString.toString()));
        }
        this.patternElements = elements.toArray(new Element[0]);
    }

    public String format(PersonName name) {
        StringBuilder result = new StringBuilder();
        boolean seenLeadingField = false;
        boolean seenEmptyLeadingField = false;
        boolean seenEmptyField = false;
        StringBuilder textBefore = new StringBuilder();
        StringBuilder textAfter = new StringBuilder();

        // the logic below attempts to implement the following algorithm:
        // - If one or more fields at the beginning of the name are empty, also skip all literal text
        //   from the beginning of the name up to the first populated field.
        // - If one or more fields at the end of the name are empty, also skip all literal text from
        //   the last populated field to the end of the name.
        // - If one or more contiguous fields in the middle of the name are empty, skip the literal text
        //   between them, omit characters from the literal text on either side of the empty fields up to
        //   the first space on either side, and make sure that the resulting literal text doesn't end up
        //   with two spaces in a row.
        for (Element element : patternElements) {
            if (element.isLiteral()) {
                if (seenEmptyLeadingField) {
                    // do nothing; throw away the literal text
                } else if (seenEmptyField) {
                    textAfter.append(element.format(name));
                } else {
                    textBefore.append(element.format(name));
                }
            } else {
                String fieldText = element.format(name);
                if (fieldText == null || fieldText.isEmpty()) {
                    if (!seenLeadingField) {
                        seenEmptyLeadingField = true;
                        textBefore.setLength(0);
                    } else {
                        seenEmptyField = true;
                        textAfter.setLength(0);
                    }
                } else {
                    seenLeadingField = true;
                    seenEmptyLeadingField = false;
                    if (seenEmptyField) {
                        result.append(coalesce(textBefore, textAfter));
                        result.append(fieldText);
                        seenEmptyField = false;
                    } else {
                        result.append(textBefore);
                        textBefore.setLength(0);
                        result.append(element.format(name));
                    }
                }
            }
        }
        if (!seenEmptyField) {
            result.append(textBefore);
        }
        return result.toString();
    }

    public int numPopulatedFields(PersonName name) {
        int result = 0;
        for (Element element : patternElements) {
            result += element.isPopulated(name) ? 1 : 0;
        }
        return result;
    }

    public int numEmptyFields(PersonName name) {
        int result = 0;
        for (Element element : patternElements) {
            result += element.isPopulated(name) ? 0 : 1;
        }
        return result;
    }

    /**
     * Stitches together the literal text on either side of an omitted field by deleting any
     * non-whitespace characters immediately neighboring the omitted field and coalescing any
     * adjacent spaces at the join point down to one.
     * @param s1 The literal text before the omitted field.
     * @param s2 The literal text after the omitted field.
     */
    private String coalesce(StringBuilder s1, StringBuilder s2) {
        // get the range of non-whitespace characters at the beginning of s1
        int p1 = 0;
        while (p1 < s1.length() && !Character.isWhitespace(s1.charAt(p1))) {
            ++p1;
        }

        // get the range of non-whitespace characters at the end of s2
        int p2 = s2.length() - 1;
        while (p2 >= 0 && !Character.isWhitespace(s2.charAt(p2))) {
            --p2;
        }

        // also include one whitespace character from s1 or, if there aren't
        // any, one whitespace character from s2
        if (p1 < s1.length()) {
            ++p1;
        } else if (p2 >= 0) {
            --p2;
        }

        // concatenate those two ranges to get the coalesced literal text
        String result = s1.substring(0, p1) + s2.substring(p2 + 1);

        // clear out s1 and s2 (done here to improve readability in format() above))
        s1.setLength(0);
        s2.setLength(0);

        return result;
    }

    /**
     * A single element in a NamePattern.  This is either a name field or a range of literal text.
     */
    private interface Element {
        boolean isLiteral();
        String format(PersonName name);
        boolean isPopulated(PersonName name);
    }

    /**
     * Literal text from a name pattern.
     */
    private static class LiteralText implements Element {
        private String text;

        public LiteralText(String text) {
            this.text = text;
        }

        public boolean isLiteral() {
            return true;
        }

        public String format(PersonName name) {
            return text;
        }

        public boolean isPopulated(PersonName name) {
            return false;
        }
    }

    /**
     * An actual name field in a NamePattern (i.e., the stuff represented in the pattern by text
     * in braces).  This class actually handles fetching the value for the field out of a
     * PersonName object and applying any modifiers to it.
     */
    private static class NameFieldImpl implements Element {
        private PersonName.NameField fieldID;
        private Map<PersonName.FieldModifier, FieldModifierImpl> modifiers;

        public NameFieldImpl(String fieldNameAndModifiers, PersonNameFormatterImpl formatterImpl) {
            List<PersonName.FieldModifier> modifierIDs = new ArrayList<>();
            StringTokenizer tok = new StringTokenizer(fieldNameAndModifiers, "-");

            this.fieldID = PersonName.NameField.forString(tok.nextToken());
            while (tok.hasMoreTokens()) {
                modifierIDs.add(PersonName.FieldModifier.forString(tok.nextToken()));
            }
            if (this.fieldID == PersonName.NameField.SURNAME && formatterImpl.shouldCapitalizeSurname()) {
                modifierIDs.add(PersonName.FieldModifier.ALL_CAPS);
            }

            this.modifiers = new HashMap<>();
            for (PersonName.FieldModifier modifierID : modifierIDs) {
                this.modifiers.put(modifierID, FieldModifierImpl.forName(modifierID, formatterImpl));
            }
        }

        public boolean isLiteral() {
            return false;
        }

        public String format(PersonName name) {
            Set<PersonName.FieldModifier> modifierIDs = new HashSet<>(modifiers.keySet());
            String result = name.getFieldValue(fieldID, modifierIDs);
            if (result != null) {
                for (PersonName.FieldModifier modifierID : modifierIDs) {
                    result = modifiers.get(modifierID).modifyField(result);
                }
            }
            return result;
        }

        public boolean isPopulated(PersonName name) {
            // just check whether the unmodified field contains a value
            Set<PersonName.FieldModifier> modifierIDs = new HashSet<>();
            String fieldValue = name.getFieldValue(fieldID, modifierIDs);
            return fieldValue != null && !fieldValue.isEmpty();
        }
    }
}
