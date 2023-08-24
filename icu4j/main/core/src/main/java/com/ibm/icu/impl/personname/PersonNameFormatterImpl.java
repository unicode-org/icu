// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.personname;

import static com.ibm.icu.util.UResourceBundle.ARRAY;
import static com.ibm.icu.util.UResourceBundle.STRING;

import java.util.*;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.PersonName;
import com.ibm.icu.text.PersonNameFormatter;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

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
    private final String nativeSpaceReplacement;
    private final PersonNameFormatter.Length length;
    private final PersonNameFormatter.Usage usage;
    private final PersonNameFormatter.Formality formality;
    private final PersonNameFormatter.DisplayOrder displayOrder;

    public PersonNameFormatterImpl(Locale locale,
                                   PersonNameFormatter.Length length,
                                   PersonNameFormatter.Usage usage,
                                   PersonNameFormatter.Formality formality,
                                   PersonNameFormatter.DisplayOrder displayOrder,
                                   boolean surnameAllCaps) {
        // save off our creation parameters (these are only used if we have to create a second formatter)
        this.length = length;
        this.usage = usage;
        this.formality = formality;
        this.displayOrder = displayOrder;
        this.capitalizeSurname = surnameAllCaps;

        // load simple property values from the resource bundle (or the options set)
        ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, locale);
        this.locale = locale;
        this.initialPattern = rb.getStringWithFallback("personNames/initialPattern/initial");
        this.initialSequencePattern = rb.getStringWithFallback("personNames/initialPattern/initialSequence");
        this.foreignSpaceReplacement = rb.getStringWithFallback("personNames/foreignSpaceReplacement");
        this.nativeSpaceReplacement = rb.getStringWithFallback("personNames/nativeSpaceReplacement");

        // asjust for combinations of parameters that don't make sense in practice
        if (usage == PersonNameFormatter.Usage.MONOGRAM) {
            // we don't support SORTING in conjunction with MONOGRAM; if the caller passes in SORTING, remove it from
            // the options list
            displayOrder = PersonNameFormatter.DisplayOrder.DEFAULT;
        } else if (displayOrder == PersonNameFormatter.DisplayOrder.SORTING) {
            // we only support SORTING in conjunction with REFERRING; if the caller passes in ADDRESSING, treat it
            // the same as REFERRING
            usage = PersonNameFormatter.Usage.REFERRING;
        }

        // load the actual formatting patterns-- since we don't know the name order until formatting time (it can be
        // different for different names), load patterns for both given-first and surname-first names.  (If the user has
        // specified SORTING, we don't need to do this-- we just load the "sorting" patterns and ignore the name's order.)
        final String RESOURCE_PATH_PREFIX = "personNames/namePattern/";
        String lengthStr = (length != PersonNameFormatter.Length.DEFAULT) ? length.toString().toLowerCase()
                : rb.getStringWithFallback("personNames/parameterDefault/length");
        String formalityStr = (formality != PersonNameFormatter.Formality.DEFAULT) ? formality.toString().toLowerCase()
                : rb.getStringWithFallback("personNames/parameterDefault/formality");
        String resourceNameBody = lengthStr + "-" + usage.toString().toLowerCase() + "-" + formalityStr;
        if (displayOrder != PersonNameFormatter.DisplayOrder.SORTING) {
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
    public PersonNameFormatterImpl(Locale locale, String[] gnFirstPatterns, String[] snFirstPatterns, String[] gnFirstLocales, String[] snFirstLocales) {
        // first, set dummy values for the other fields
        length = PersonNameFormatter.Length.MEDIUM;
        usage = PersonNameFormatter.Usage.REFERRING;
        formality = PersonNameFormatter.Formality.FORMAL;
        displayOrder = PersonNameFormatter.DisplayOrder.DEFAULT;
        initialPattern = "{0}.";
        initialSequencePattern = "{0} {1}";
        capitalizeSurname = false;
        foreignSpaceReplacement = " ";
        nativeSpaceReplacement = " ";

        // then, set values for the fields we actually care about (all but gnFirstPatterns are optional)
        this.locale = locale;
        this.gnFirstPatterns = PersonNamePattern.makePatterns(gnFirstPatterns, this);
        this.snFirstPatterns = (snFirstPatterns != null) ? PersonNamePattern.makePatterns(snFirstPatterns, this) : null;
        if (gnFirstLocales != null) {
            this.gnFirstLocales = new HashSet<>();
            Collections.addAll(this.gnFirstLocales, gnFirstLocales);
        } else {
            this.gnFirstLocales = null;
        }
        if (snFirstLocales != null) {
            this.snFirstLocales = new HashSet<>();
            Collections.addAll(this.snFirstLocales, snFirstLocales);
        } else {
            this.snFirstLocales = null;
        }
    }

    @Override
    public String toString() {
        return "PersonNameFormatter: " + displayOrder + "-" + length + "-" + usage + "-" + formality + ", " + locale;
    }

    public String formatToString(PersonName name) {
        // TODO: Should probably return a FormattedPersonName object

        Locale nameLocale = getNameLocale(name);
        String nameScript = getNameScript(name);

        if (!nameScriptMatchesLocale(nameScript, this.locale)) {
            Locale newFormattingLocale;
            if (formattingLocaleExists(nameLocale)) {
                newFormattingLocale = nameLocale;
            } else {
                newFormattingLocale = newLocaleWithScript(null, nameScript, nameLocale.getCountry());
            }
            PersonNameFormatterImpl nameLocaleFormatter = new PersonNameFormatterImpl(newFormattingLocale, this.length,
                    this.usage, this.formality, this.displayOrder, this.capitalizeSurname);
            return nameLocaleFormatter.formatToString(name);
        }

        String result = null;

        // choose the GN-first or SN-first pattern based on the name itself and use that to format it
        if (snFirstPatterns == null || nameIsGnFirst(name)) {
            result = getBestPattern(gnFirstPatterns, name).format(name);
        } else {
            result = getBestPattern(snFirstPatterns, name).format(name);
        }

        // if either of the space-replacement characters is something other than a space,
        // check to see if the name locale's language matches the formatter locale's language.
        // If they match, replace all spaces with the native space-replacement character,
        // and if they don't, replace all spaces with the foreign space-replacement character
        if (!nativeSpaceReplacement.equals(" ") || !foreignSpaceReplacement.equals(" ")) {
            if (localesMatch(nameLocale, this.locale)) {
                result = result.replace(" ", nativeSpaceReplacement);
            } else {
                result = result.replace(" ", foreignSpaceReplacement);
            }
        }
        return result;
    }

    public Locale getLocale() {
        return locale;
    }

    public PersonNameFormatter.Length getLength() { return length; }

    public PersonNameFormatter.Usage getUsage() { return usage; }

    public PersonNameFormatter.Formality getFormality() { return formality; }

    public PersonNameFormatter.DisplayOrder getDisplayOrder() { return displayOrder; }
    public boolean getSurnameAllCaps() { return capitalizeSurname; }

    public String getInitialPattern() {
        return initialPattern;
    }

    public String getInitialSequencePattern() {
        return initialSequencePattern;
    }

    public boolean shouldCapitalizeSurname() {
        return capitalizeSurname;
    }

    static final Set<String> NON_DEFAULT_SCRIPTS = new HashSet<>(Arrays.asList("Hani", "Hira", "Kana"));

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
        // if the formatter has its display order set to one of the "force" values, that overrides
        // all this logic and the name's preferred-order property
        if (this.displayOrder == PersonNameFormatter.DisplayOrder.FORCE_GIVEN_FIRST) {
            return true;
        } else if (this.displayOrder == PersonNameFormatter.DisplayOrder.FORCE_SURNAME_FIRST) {
            return false;
        }

        // the name can declare its order-- check that first (it overrides any locale-based calculation)
        if (name.getPreferredOrder() == PersonName.PreferredOrder.GIVEN_FIRST) {
            return true;
        } else if (name.getPreferredOrder() == PersonName.PreferredOrder.SURNAME_FIRST) {
            return false;
        }

        // Otherwise, search the gnFirstLocales and snFirstLocales for the locale's name.
        // For our purposes, the "locale's name" is the locale the name itself gives us (if it
        // has one), or the locale we guess for the name (if it doesn't).
        Locale nameLocale = name.getNameLocale();
        if (nameLocale == null) {
            nameLocale = getNameLocale(name);
        }

        // this is a hack to deal with certain script codes that are valid, but not the default, for their locales--
        // to make the parent-chain lookup work right, we need to replace any of those script codes (in the name's locale)
        // with the appropriate default script for whatever language and region we have
        ULocale nameULocale = ULocale.forLocale(nameLocale);
        if (NON_DEFAULT_SCRIPTS.contains(nameULocale.getScript())) {
            ULocale.Builder builder = new ULocale.Builder();
            builder.setLocale(nameULocale);
            builder.setScript(null);
            nameULocale = ULocale.addLikelySubtags(builder.build());
        }

        // now search for the locale in the gnFirstLocales and snFirstLocales lists...
        String localeStr = nameULocale.getName();
        String origLocaleStr = localeStr;
        String languageCode = nameULocale.getLanguage();

        do {
            // first check if the locale is in one of those lists
            if (gnFirstLocales.contains(localeStr)) {
                return true;
            } else if (snFirstLocales.contains(localeStr)) {
                return false;
            }

            // if not, try again with "und" in place of the language code (this lets us use "und_CN" to match
            // all locales with a region code of "CN" and makes sure the last thing we try is always "und", which
            // is required to be in gnFirstLocales or snFirstLocales)
            String undStr = localeStr.replaceAll("^" + languageCode, "und");
            if (gnFirstLocales.contains(undStr)) {
                return true;
            } else if (snFirstLocales.contains(undStr)) {
                return false;
            }

            // if we haven't found the locale ID yet, look up its parent locale ID and try again-- if getParentLocaleID()
            // returns null (i.e., we have a locale ID, such as "zh_Hant", that inherits directly from "root"), try again
            // with just the locale ID's language code (this fixes it so that "zh_Hant" matches "zh", even though "zh" isn't,
            // strictly speaking, its parent locale)
            String parentLocaleStr = ICUResourceBundle.getParentLocaleID(localeStr, origLocaleStr, ICUResourceBundle.OpenType.LOCALE_DEFAULT_ROOT);
            localeStr = (parentLocaleStr != null) ? parentLocaleStr : languageCode;
        } while (localeStr != null);

        // should never get here ("und" should always be in gnFirstLocales or snFirstLocales), but if we do...
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
     * Internal function to figure out the name's script by examining its characters.
     * @param name The name for which we need the script
     * @return The four-letter script code for the name.
     */
    private String getNameScript(PersonName name) {
        // Rather than exhaustively checking all the fields in the name, we just check the given-name
        // and surname fields, giving preference to the script of the surname if they're different
        // (we concatenate them into one string for simplicity).  The "name script" is the script
        // of the first character we find whose script isn't "common".  If that script is one
        // of the scripts used by the specified locale, we have a match.
        String givenName = name.getFieldValue(PersonName.NameField.SURNAME, Collections.emptySet());
        String surname = name.getFieldValue(PersonName.NameField.GIVEN, Collections.emptySet());
        String nameText = ((surname != null) ? surname : "") + ((givenName != null) ? givenName : "");
        int stringScript = UScript.UNKNOWN;
        for (int i = 0; stringScript == UScript.UNKNOWN && i < nameText.length(); i++) {
            int c = nameText.codePointAt(i);
            int charScript = UScript.getScript(c);
            if (charScript != UScript.COMMON && charScript != UScript.INHERITED && charScript != UScript.UNKNOWN) {
                stringScript = charScript;
            }
        }
        return UScript.getShortName(stringScript);
    }

    private Locale newLocaleWithScript(Locale oldLocale, String scriptCode, String regionCode) {
        Locale workingLocale;
        String localeScript;

        // if we got the "unknown" script code, don't do anything with it-- just return the original locale
        if (scriptCode.equals("Zzzz")) {
            return oldLocale;
        }

        Locale.Builder builder = new Locale.Builder();
        if (oldLocale != null) {
            workingLocale = oldLocale;
            builder.setLocale(oldLocale);
            localeScript = ULocale.addLikelySubtags(ULocale.forLocale(oldLocale)).getScript();
        } else {
            ULocale tmpLocale = ULocale.addLikelySubtags(new ULocale("und_" + scriptCode));
            builder.setLanguage(tmpLocale.getLanguage());
            workingLocale = ULocale.addLikelySubtags(new ULocale(tmpLocale.getLanguage())).toLocale();
            localeScript = workingLocale.getScript();

            if (regionCode != null) {
                builder.setRegion(regionCode);
            }
        }

        // if the detected character script matches one of the default scripts for the name's locale,
        // use the name locale's default script code in the locale ID we return (this converts a detected
        // script of "Hani" to "Hans" for "zh", "Hant" for "zh_Hant", and "Jpan" for "ja")
        if (!scriptCode.equals(localeScript) && nameScriptMatchesLocale(scriptCode, workingLocale)) {
            scriptCode = localeScript;
        }

        builder.setScript(scriptCode);
        return builder.build();
    }

    /**
     * Internal function to figure out the name's locale when the name doesn't specify it.
     * (Note that this code assumes that if the locale is specified, it includes a language
     * code.)
     * @param name The name for which we need the locale
     * @return The name's (real or guessed) locale.
     */
    private Locale getNameLocale(PersonName name) {
        return newLocaleWithScript(name.getNameLocale(), getNameScript(name), null);
    }

    /**
     * Returns true if the characters in the name match one of the scripts for the specified locale.
     */
    private boolean nameScriptMatchesLocale(String nameScriptID, Locale formatterLocale) {
        // if the script code is the "unknown" script, pretend it matches everything
        if (nameScriptID.equals("Zzzz")) {
            return true;
        }

        int[] localeScripts = UScript.getCode(formatterLocale);
        int nameScript = UScript.getCodeFromName(nameScriptID);

        for (int localeScript : localeScripts) {
            if (localeScript == nameScript || (localeScript == UScript.SIMPLIFIED_HAN && nameScript == UScript.HAN) || (localeScript == UScript.TRADITIONAL_HAN && nameScript == UScript.HAN)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if there's actual name formatting data for the specified locale (i.e., when
     * we fetch the resource data, we don't fall back to root).
     */
    private boolean formattingLocaleExists(Locale formattingLocale) {
        // NOTE: What we really want to test for here is whether we're falling back to root for either the resource bundle itself
        // or for the personNames/nameOrderLocales/givenFirst and personNames/nameOrderLocales/surnameFirst resources.
        // The problem is that getBundleInstance() doesn't return root when it can't find what it's looking for; it returns
        // ULocale.getDefault().  We could theoretically get around this by passing OpenType.LOCALE_ROOT, but this
        // bypasses the parent-locale table, so fallback across script can happen (ja_Latn falls back to ja instead of root).
        // So I'm checking to see if the language code got changed and using that as a surrogate for falling back to root.
        String formattingLanguage = formattingLocale.getLanguage();
        ICUResourceBundle mainRB = ICUResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, ULocale.forLocale(formattingLocale), ICUResourceBundle.OpenType.LOCALE_DEFAULT_ROOT);
        if (!mainRB.getULocale().getLanguage().equals(formattingLanguage)) {
            return false;
        }

        ICUResourceBundle gnFirstResource = mainRB.getWithFallback("personNames/nameOrderLocales/givenFirst");
        ICUResourceBundle snFirstResource = mainRB.getWithFallback("personNames/nameOrderLocales/surnameFirst");

        return gnFirstResource.getULocale().getLanguage().equals(formattingLanguage) || snFirstResource.getULocale().getLanguage().equals(formattingLanguage);
    }

    /**
     * Returns true if the two locales should be considered equivalent for space-replacement purposes.
     */
    private boolean localesMatch(Locale nameLocale, Locale formatterLocale) {
        String nameLanguage = nameLocale.getLanguage();
        String formatterLanguage = formatterLocale.getLanguage();

        if (nameLanguage.equals(formatterLanguage)) {
            return true;
        }

        // HACK to make Japanese and Chinese names use the native format and native space replacement
        // (do we want to do something more general here?)
        if ((nameLanguage.equals("ja") || nameLanguage.equals("zh")) && (formatterLanguage.equals("ja") || formatterLanguage.equals("zh"))) {
            return true;
        }

        return false;
    }
}
