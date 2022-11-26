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
                return new InitialModifier(formatterImpl.getInitialPattern(), formatterImpl.getInitialSequencePattern());
            case MONOGRAM:
                return MONOGRAM_MODIFIER;
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
    private static class InitialModifier extends FieldModifierImpl {
        private final SimpleFormatter initialFormatter;
        private final SimpleFormatter initialSequenceFormatter;

        public InitialModifier(String initialPattern, String initialSequencePattern) {
            this.initialFormatter = SimpleFormatter.compile(initialPattern);
            this.initialSequenceFormatter = SimpleFormatter.compile(initialSequencePattern);
        }

        @Override
        public String modifyField(String fieldValue) {
            String result = null;
            StringTokenizer tok = new StringTokenizer(fieldValue, " ");
            while (tok.hasMoreTokens()) {
                String curInitial = getFirstGrapheme(tok.nextToken());
                if (result == null) {
                    result = initialFormatter.format(curInitial);
                } else {
                    result = initialSequenceFormatter.format(result, initialFormatter.format(curInitial));
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
