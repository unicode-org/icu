// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.personname;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.PersonName;
import com.ibm.icu.text.PersonNameFormatter;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static com.ibm.icu.util.UResourceBundle.ARRAY;
import static com.ibm.icu.util.UResourceBundle.STRING;

/**
 * Actual implementation class for PersonNameFormatter.
 */
public class PersonNameFormatterImpl {
    private final Locale locale;
    private final PersonNamePattern[] gnFirstPatterns;
    private final PersonNamePattern[] snFirstPatterns;
    private final Set<String> gnFirstLocales;
    private final Set<String> snFirstLocales;
    private final String initialPattern;
    private final String initialSequencePattern;
    private final boolean capitalizeSurname;
    private final String foreignSpaceReplacement;
    private final boolean formatterLocaleUsesSpaces;
    private final PersonNameFormatter.Length length;
    private final PersonNameFormatter.Usage usage;
    private final PersonNameFormatter.Formality formality;
    private final Set<PersonNameFormatter.Options> options;

    public PersonNameFormatterImpl(Locale locale,
                                   PersonNameFormatter.Length length,
                                   PersonNameFormatter.Usage usage,
                                   PersonNameFormatter.Formality formality,
                                   Set<PersonNameFormatter.Options> options) {
        // null for `options` is the same as the empty set
        if (options == null) {
            options = new HashSet<>();
        }

        // save off our creation parameters (these are only used if we have to create a second formatter)
        this.length = length;
        this.usage = usage;
        this.formality = formality;
        this.options = options;

        // load simple property values from the resource bundle (or the options set)
        ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, locale);
        this.locale = locale;
        this.initialPattern = rb.getStringWithFallback("personNames/initialPattern/initial");
        this.initialSequencePattern = rb.getStringWithFallback("personNames/initialPattern/initialSequence");
        this.capitalizeSurname = options.contains(PersonNameFormatter.Options.SURNAME_ALLCAPS);
        this.foreignSpaceReplacement = rb.getStringWithFallback("personNames/foreignSpaceReplacement");
        this.formatterLocaleUsesSpaces = !LOCALES_THAT_DONT_USE_SPACES.contains(locale.getLanguage());

        // asjust for combinations of parameters that don't make sense in practice
        if (usage == PersonNameFormatter.Usage.MONOGRAM) {
            // we don't support SORTING in conjunction with MONOGRAM; if the caller passes in SORTING, remove it from
            // the options list
            options.remove(PersonNameFormatter.Options.SORTING);
        } else if (options.contains(PersonNameFormatter.Options.SORTING)) {
            // we only support SORTING in conjunction with REFERRING; if the caller passes in ADDRESSING, treat it
            // the same as REFERRING
            usage = PersonNameFormatter.Usage.REFERRING;
        }

        // load the actual formatting patterns-- since we don't know the name order until formatting time (it can be
        // different for different names), load patterns for both given-first and surname-first names.  (If the user has
        // specified SORTING, we don't need to do this-- we just load the "sorting" patterns and ignore the name's order.)
        final String RESOURCE_PATH_PREFIX = "personNames/namePattern/";
        String resourceNameBody = length.toString().toLowerCase() + "-" + usage.toString().toLowerCase() + "-"
                + formality.toString().toLowerCase();
        if (!options.contains(PersonNameFormatter.Options.SORTING)) {
            ICUResourceBundle gnFirstResource = rb.getWithFallback(RESOURCE_PATH_PREFIX + "givenFirst-" + resourceNameBody);
            ICUResourceBundle snFirstResource = rb.getWithFallback(RESOURCE_PATH_PREFIX + "surnameFirst-" + resourceNameBody);

            gnFirstPatterns = PersonNamePattern.makePatterns(asStringArray(gnFirstResource), this);
            snFirstPatterns = PersonNamePattern.makePatterns(asStringArray(snFirstResource), this);

            gnFirstLocales = new HashSet<>();
            Collections.addAll(gnFirstLocales, asStringArray(rb.getWithFallback("personNames/nameOrderLocales/givenFirst")));
            snFirstLocales = new HashSet<>();
            Collections.addAll(snFirstLocales, asStringArray(rb.getWithFallback("personNames/nameOrderLocales/surnameFirst")));
        } else {
            ICUResourceBundle patternResource = rb.getWithFallback(RESOURCE_PATH_PREFIX + "sorting-" + resourceNameBody);

            gnFirstPatterns = PersonNamePattern.makePatterns(asStringArray(patternResource), this);
            snFirstPatterns = null;
            gnFirstLocales = null;
            snFirstLocales = null;
        }
    }

    /**
     * THIS IS A DUMMY CONSTRUCTOR JUST FOR THE USE OF THE UNIT TESTS TO CHECK SOME OF THE INTERNAL IMPLEMENTATION!
     */
    public PersonNameFormatterImpl(Locale locale, String[] patterns) {
        // first, set dummy values for the other fields
        snFirstPatterns = null;
        gnFirstLocales = null;
        snFirstLocales = null;
        length = PersonNameFormatter.Length.MEDIUM;
        usage = PersonNameFormatter.Usage.REFERRING;
        formality = PersonNameFormatter.Formality.FORMAL;
        options = Collections.emptySet();
        initialPattern = "{0}.";
        initialSequencePattern = "{0} {1}";
        capitalizeSurname = false;
        foreignSpaceReplacement = " ";
        formatterLocaleUsesSpaces = true;

        // then, set values for the fields we actually care about
        this.locale = locale;
        gnFirstPatterns = PersonNamePattern.makePatterns(patterns, this);

    }

    public String formatToString(PersonName name) {
        // TODO: Should probably return a FormattedPersonName object

        // if the formatter is for a language that doesn't use spaces between words and the name is from a language
        // that does, create a formatter for the NAME'S locale and use THAT to format the name
        Locale nameLocale = getNameLocale(name);
        boolean nameLocaleUsesSpaces = !LOCALES_THAT_DONT_USE_SPACES.contains(nameLocale.getLanguage());
        if (!formatterLocaleUsesSpaces && nameLocaleUsesSpaces) {
            PersonNameFormatterImpl nativeFormatter = new PersonNameFormatterImpl(nameLocale, this.length,
                    this.usage, this.formality, this.options);
            String result = nativeFormatter.formatToString(name);

            // BUT, if the name is actually written in the formatter locale's script, replace any spaces in the name
            // with the foreignSpaceReplacement character
            if (!foreignSpaceReplacement.equals(" ") && scriptMatchesLocale(result, this.locale)) {
                result = result.replace(" ", this.foreignSpaceReplacement);
            }
            return result;
        }

        // if we get down to here, we're just doing normal formatting-- if we have both given-first and surname-first
        // rules, choose which one to use based on the name's locale and preferred field order
        if (snFirstPatterns == null || nameIsGnFirst(name)) {
            return getBestPattern(gnFirstPatterns, name).format(name);
        } else {
            return getBestPattern(snFirstPatterns, name).format(name);
        }
    }

    public Locale getLocale() {
        return locale;
    }

    public PersonNameFormatter.Length getLength() { return length; }

    public PersonNameFormatter.Usage getUsage() { return usage; }

    public PersonNameFormatter.Formality getFormality() { return formality; }

    public Set<PersonNameFormatter.Options> getOptions() { return options; }

    public String getInitialPattern() {
        return initialPattern;
    }

    public String getInitialSequencePattern() {
        return initialSequencePattern;
    }

    public boolean shouldCapitalizeSurname() {
        return capitalizeSurname;
    }

    private final Set<String> LOCALES_THAT_DONT_USE_SPACES = new HashSet<>(Arrays.asList("ja", "zh", "th", "yue", "km", "lo"));

    /**
     * Returns the value of the resource, as a string array.
     * @param resource An ICUResourceBundle of type STRING or ARRAY.  If ARRAY, this function just returns it
     *                 as a string array.  If STRING, it returns a one-element array containing that string.
     * @return The resource's value, as an array of Strings.
     */
    private String[] asStringArray(ICUResourceBundle resource) {
        if (resource.getType() == STRING) {
            return new String[] { resource.getString() };
        } else if (resource.getType() == ARRAY){
            return resource.getStringArray();
        } else {
            throw new IllegalStateException("Unsupported resource type " + resource.getType());
        }
    }

    /**
     * Returns the field order to use when formatting this name, taking into account the name's preferredOrder
     * field, as well as the name and formatter's respective locales.
     * @param name The name to be formatted.
     * @return If true, use given-first order to format the name; if false, use surname-first order.
     */
    private boolean nameIsGnFirst(PersonName name) {
        // the name can declare its order-- check that first (it overrides any locale-based calculation)
        Set<PersonName.FieldModifier> modifiers = new HashSet<>();
        String preferredOrder = name.getFieldValue(PersonName.NameField.PREFERRED_ORDER, modifiers);
        if (preferredOrder != null) {
            if (preferredOrder.equals("givenFirst")) {
                return true;
            } else if (preferredOrder.equals("surnameFirst")) {
                return false;
            } else {
                throw new IllegalArgumentException("Illegal preferredOrder value " + preferredOrder);
            }
        }

        String localeStr = getNameLocale(name).toString();
        do {
            if (gnFirstLocales.contains(localeStr)) {
                return true;
            } else if (snFirstLocales.contains(localeStr)) {
                return false;
            }

            int lastUnderbarPos = localeStr.lastIndexOf("_");
            if (lastUnderbarPos >= 0) {
                localeStr = localeStr.substring(0, lastUnderbarPos);
            } else {
                localeStr = "root";
            }
        } while (!localeStr.equals("root"));

        // should never get here-- "root" should always be in one of the locales
        return true;
    }

    private PersonNamePattern getBestPattern(PersonNamePattern[] patterns, PersonName name) {
        // early out if there's only one pattern
        if (patterns.length == 1) {
            return patterns[0];
        } else {
            // if there's more than one pattern, return the one that contains the greatest number of fields that
            // actually have values in `name`.  If there's a tie, return the pattern that contains the lowest number
            // of fields that DON'T have values in `name`.
            int maxPopulatedFields = 0;
            int minEmptyFields = Integer.MAX_VALUE;
            PersonNamePattern bestPattern = null;

            for (PersonNamePattern pattern : patterns) {
                int populatedFields = pattern.numPopulatedFields(name);
                int emptyFields = pattern.numEmptyFields(name);
                if (populatedFields > maxPopulatedFields) {
                    maxPopulatedFields = populatedFields;
                    minEmptyFields = emptyFields;
                    bestPattern = pattern;
                } else if (populatedFields == maxPopulatedFields && emptyFields < minEmptyFields) {
                    minEmptyFields = emptyFields;
                    bestPattern = pattern;
                }
            }
            return bestPattern;
        }
    }

    /**
     * Internal function to figure out the name's locale when the name doesn't specify it.
     * (Note that this code assumes that if the locale is specified, it includes a language
     * code.)
     * @param name The name for which we need the locale
     * @return The name's (real or guessed) locale.
     */
    private Locale getNameLocale(PersonName name) {
        // if the name specifies its locale, we can just return it
        Locale nameLocale = name.getNameLocale();
        if (nameLocale == null) {
            // if not, we look at the characters in the name.  If their script matches the default script for the formatter's
            // locale, we use the formatter's locale as the name's locale
            int formatterScript = UScript.getCodeFromName(ULocale.addLikelySubtags(ULocale.forLocale(locale)).getScript());
            String givenName = name.getFieldValue(PersonName.NameField.GIVEN, new HashSet<PersonName.FieldModifier>());
            int nameScript = UScript.INVALID_CODE;
            for (int i = 0; nameScript == UScript.INVALID_CODE && i < givenName.length(); i++) {
                // the script of the name is the script of the first character in the name whose script isn't
                // COMMON or INHERITED
                int script = UScript.getScript(givenName.charAt(i));
                if (script != UScript.COMMON && script != UScript.INHERITED) {
                    nameScript = script;
                }
            }
            if (formatterScript == nameScript) {
                nameLocale = this.locale;
            } else {
                // if the name's script is different from the formatter's script, we use addLikelySubtags() to find the
                // default language for the name's script and use THAT as the name's locale
                nameLocale = new Locale(ULocale.addLikelySubtags(new ULocale("und_" + UScript.getShortName(nameScript))).getLanguage());
            }
            // TODO: This algorithm has a few deficiencies: First, it assumes the script of the string is the script of the first
            // character in the string that's not COMMON or INHERITED.  This won't work well for some languages, such as Japanese,
            // that use multiple scripts.  Doing better would require adding a new getScript(String) method on UScript, which
            // might be something we want.  Second, we only look at the given-name field.  This field should always be populated,
            // but if it isn't, we're stuck.  Looking at all the fields requires API on PersonName that we don't need anywhere
            // else.
        }
        return nameLocale;
    }

    /**
     * Returns true if the script of `s` is one of the default scripts for `locale`.
     * This function only checks the script of the first character whose script isn't "common,"
     * so it probably won't work right on mixed-script strings.
     */
    private boolean scriptMatchesLocale(String s, Locale locale) {
        int[] localeScripts = UScript.getCode(locale);
        int stringScript = UScript.COMMON;
        for (int i = 0; stringScript == UScript.COMMON && i < s.length(); i++) {
            char c = s.charAt(i);
            stringScript = UScript.getScript(c);
        }

        for (int localeScript : localeScripts) {
            if (localeScript == stringScript) {
                return true;
            }
        }
        return false;
    }
}
