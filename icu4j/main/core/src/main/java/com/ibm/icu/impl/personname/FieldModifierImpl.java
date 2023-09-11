// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.personname;

import java.util.Locale;
import java.util.StringTokenizer;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.CaseMap;
import com.ibm.icu.text.PersonName;
import com.ibm.icu.text.SimpleFormatter;

/**
 * Parent class for classes that implement field-modifier behavior.
 */
abstract class FieldModifierImpl {
    public abstract String modifyField(String fieldValue);

    public static FieldModifierImpl forName(PersonName.FieldModifier modifierID, PersonNameFormatterImpl formatterImpl) {
        switch (modifierID) {
            case INFORMAL:
                return NOOP_MODIFIER;
            case PREFIX:
                return NULL_MODIFIER;
            case CORE:
                return NOOP_MODIFIER;
            case ALL_CAPS:
                return new AllCapsModifier(formatterImpl.getLocale());
            case INITIAL_CAP:
                return new InitialCapModifier(formatterImpl.getLocale());
            case INITIAL:
                return new InitialModifier(formatterImpl.getLocale(), formatterImpl.getInitialPattern(), formatterImpl.getInitialSequencePattern());
            case RETAIN:
                // "retain" is handled by InitialMofidier and PersonNamePattern.NameFieldImpl
                return NOOP_MODIFIER;
            case MONOGRAM:
                return MONOGRAM_MODIFIER;
            case GENITIVE:
                // no built-in implementation for deriving genitive from nominative; PersonName object must supply
                return NOOP_MODIFIER;
            case VOCATIVE:
                // no built-in implementation for deriving vocative from nominative; PersonName object must supply
                return NOOP_MODIFIER;
            default:
                throw new IllegalArgumentException("Invalid modifier ID " + modifierID);
        }
    }

    /**
     * A field modifier that just returns the field value unmodified.  This is used to implement the default
     * behavior of the "informal" and "core" modifiers ("real" informal or core variants have to be supplied or
     * calculated by the PersonName object).
     */
    private static final FieldModifierImpl NOOP_MODIFIER = new FieldModifierImpl() {
        @Override
        public String modifyField(String fieldValue) {
            return fieldValue;
        }
    };

    /**
     * A field modifier that just returns the empty string.  This is used to implement the default behavior of the
     * "prefix" modifier ("real" prefix variants have to be supplied to calculated by the PersonName object).
     */
    private static final FieldModifierImpl NULL_MODIFIER = new FieldModifierImpl() {
        @Override
        public String modifyField(String fieldValue) {
            return "";
        }
    };

    /**
     * A field modifier that returns the field value converted to ALL CAPS.  This is the default behavior
     * for the "allCaps" modifier.
     */
    private static class AllCapsModifier extends FieldModifierImpl {
        private final Locale locale;

        public AllCapsModifier(Locale locale) {
            this.locale = locale;
        }

        @Override
        public String modifyField(String fieldValue) {
            return UCharacter.toUpperCase(locale, fieldValue);
        }
    }

    /**
     * A field modifier that returns the field value with the first letter of each word capitalized.  This is
     * the default behavior of the "initialCap" modifier.
     */
    private static class InitialCapModifier extends FieldModifierImpl {
        private final Locale locale;
        private static final CaseMap.Title TO_TITLE_WHOLE_STRING_NO_LOWERCASE = CaseMap.toTitle().wholeString().noLowercase();

        public InitialCapModifier(Locale locale) {
            this.locale = locale;
        }

        @Override
        public String modifyField(String fieldValue) {
            return TO_TITLE_WHOLE_STRING_NO_LOWERCASE.apply(locale, null, fieldValue);
        }
    }

    /**
     * A field modifier that returns the field value converted into one or more initials.  This is the first grapheme
     * cluster of each word in the field value, modified using the initialPattern/initial resource value from the
     * locale data, and strung together using the initialPattern/initialSequence resource value from the locale data.
     * (In English, these patterns put periods after each initial and connect them with spaces.)
     * This is default behavior of the "initial" modifier.
     */
    static class InitialModifier extends FieldModifierImpl {
        private final Locale locale;
        private final SimpleFormatter initialFormatter;
        private final SimpleFormatter initialSequenceFormatter;
        private boolean retainPunctuation;

        public InitialModifier(Locale locale, String initialPattern, String initialSequencePattern) {
            this.locale = locale;
            this.initialFormatter = SimpleFormatter.compile(initialPattern);
            this.initialSequenceFormatter = SimpleFormatter.compile(initialSequencePattern);
            this.retainPunctuation = false;
        }

        public void setRetainPunctuation(boolean retain) {
            this.retainPunctuation = retain;
        }

        @Override
        public String modifyField(String fieldValue) {
            String separator = "";
            String result = null;
            BreakIterator bi = BreakIterator.getWordInstance(locale);
            bi.setText(fieldValue);
            int wordStart = bi.first();
            for (int wordEnd = bi.next(); wordEnd != BreakIterator.DONE; wordStart = wordEnd, wordEnd = bi.next()) {
                String word = fieldValue.substring(wordStart, wordEnd);
                if (Character.isLetter(word.charAt(0))) {
                    String curInitial = getFirstGrapheme(word);
                    if (result == null) {
                        result = initialFormatter.format(curInitial);
                    } else if (retainPunctuation) {
                        result = result + separator + initialFormatter.format(curInitial);
                        separator = "";
                    } else {
                        result = initialSequenceFormatter.format(result, initialFormatter.format(curInitial));
                    }
                } else if (Character.isWhitespace(word.charAt(0))) {
                    // coalesce a sequence of whitespace characters down to a single space
                    separator = separator + word.charAt(0);
                } else {
                    separator = separator + word;
                }
            }
            return result;
        }
    }

    /**
     * A field modifier that simply returns the first grapheme cluster in the field value.
     * This is the default implementation of the "monogram" modifier.
     */
    private static final FieldModifierImpl MONOGRAM_MODIFIER = new FieldModifierImpl() {
        @Override
        public String modifyField(String fieldValue) {
            return getFirstGrapheme(fieldValue);
        }
    };

    /**
     * A utility function that just returns the first grapheme cluster in the string.
     */
    private static String getFirstGrapheme(String s) {
        // early out if the string is empty to avoid StringIndexOutOfBoundsException
        if (s.isEmpty()) {
            return "";
        }

        // (currently, no locale overrides the grapheme-break rules, so we just use "root" instead of passing in the locale)
        BreakIterator bi = BreakIterator.getCharacterInstance(Locale.ROOT);
        bi.setText(s);
        return s.substring(0, bi.next());
    }
}
