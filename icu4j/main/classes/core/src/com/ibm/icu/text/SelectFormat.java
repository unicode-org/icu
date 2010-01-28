/*
 *******************************************************************************
 * Copyright (C) 2004-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 * Copyright (C) 2009 , Yahoo! Inc.                                            *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Map;

/**
 * <p><code>SelectFormat</code> supports the creation of  internationalized
 * messages by selecting phrases based on keywords. The pattern  specifies
 * how to map keywords to phrases and provides a default phrase. The
 * object provided to the format method is a string that's matched
 * against the keywords. If there is a match, the corresponding phrase
 * is selected; otherwise, the default phrase is used.</p>
 *
 * <h4>Using <code>SelectFormat</code> for Gender Agreement</h4>
 *
 * <p>The main use case for the select format is gender based  inflection.
 * When names or nouns are inserted into sentences, their gender can  affect pronouns,
 * verb forms, articles, and adjectives. Special care needs to be
 * taken for the case where the gender cannot be determined.
 * The impact varies between languages:</p>
 *
 * <ul>
 * <li>English has three genders, and unknown gender is handled as a  special
 * case. Names use the gender of the named person (if known), nouns  referring
 * to people use natural gender, and inanimate objects are usually  neutral.
 * The gender only affects pronouns: "he", "she", "it", "they".
 *
 * <li>German differs from English in that the gender of nouns is  rather
 * arbitrary, even for nouns referring to people ("M&#u00E4;dchen", girl, is  neutral).
 * The gender affects pronouns ("er", "sie", "es"), articles ("der",  "die",
 * "das"), and adjective forms ("guter Mann", "gute Frau", "gutes  M&#u00E4;dchen").
 *
 * <li>French has only two genders; as in German the gender of nouns
 * is rather arbitrary - for sun and moon, the genders
 * are the opposite of those in German. The gender affects
 * pronouns ("il", "elle"), articles ("le", "la"),
 * adjective forms ("bon", "bonne"), and sometimes
 * verb forms ("all&#u00E9;", "all&#u00E9e;").
 *
 * <li>Polish distinguishes five genders (or noun classes),
 * human masculine, animate non-human masculine, inanimate masculine,
 * feminine, and neuter.
 * </ul>
 *
 * <p>Some other languages have noun classes that are not related to  gender,
 * but similar in grammatical use.
 * Some African languages have around 20 noun classes.</p>
 *
 * <p>To enable localizers to create sentence patterns that take their
 * language's gender dependencies into consideration, software has to  provide
 * information about the gender associated with a noun or name to
 * <code>MessageFormat</code>.
 * Two main cases can be distinguished:</p>
 *
 * <ul>
 * <li>For people, natural gender information should be maintained  for each person.
 * The keywords "male", "female", "mixed" (for groups of people)
 * and "unknown" are used.
 *
 * <li>For nouns, grammatical gender information should be maintained  for
 * each noun and per language, e.g., in resource bundles.
 * The keywords "masculine", "feminine", and "neuter" are commonly  used,
 * but some languages may require other keywords.
 * </ul>
 *
 * <p>The resulting keyword is provided to <code>MessageFormat</code>  as a
 * parameter separate from the name or noun it's associated with. For  example,
 * to generate a message such as "Jean went to Paris", three separate  arguments
 * would be provided: The name of the person as argument 0, the  gender of
 * the person as argument 1, and the name of the city as argument 2.
 * The sentence pattern for English, where the gender of the person has
 * no impact on this simple sentence, would not refer to argument 1  at all:</p>
 *
 * <pre>{0} went to {2}.</pre>
 *
 * <p>The sentence pattern for French, where the gender of the person affects
 * the form of the participle, uses a select format based on argument 1:</p>
 *
 * <pre>{0} est {1, select, female {all&#u00E9;e} other {all&#u00E9;}} &#u00E0; {2}.</pre>
 *
 * <p>Patterns can be nested, so that it's possible to handle  interactions of
 * number and gender where necessary. For example, if the above  sentence should
 * allow for the names of several people to be inserted, the  following sentence
 * pattern can be used (with argument 0 the list of people's names,  
 * argument 1 the number of people, argument 2 their combined gender, and  
 * argument 3 the city name):</p>
 *
 * <pre>{0} {1, plural, 
 * one {est {2, select, female {all&#u00E9;e} other  {all&#u00E9;}}}
 * other {sont {2, select, female {all&#u00E9;es} other {all&#u00E9;s}}}
 * }&#u00E0; {3}.</pre>
 *
 * <h4>Patterns and Their Interpretation</h4>
 *
 * <p>The <code>SelectFormat</code> pattern text defines the phrase  output
 * for each user-defined keyword.
 * The pattern is a sequence of <code><i>keyword</i>{<i>phrase</i>}</code>
 * clauses, separated by white space characters.
 * Each clause assigns the phrase <code><i>phrase</i></code>
 * to the user-defined <code><i>keyword</i></code>.</p>
 *
 * <p>Keywords must match the pattern [a-zA-Z][a-zA-Z0-9_-]*; keywords
 * that don't match this pattern result in the error code
 * <code>U_ILLEGAL_CHARACTER</code>.
 * You always have to define a phrase for the default keyword
 * <code>other</code>; this phrase is returned when the keyword  
 * provided to
 * the <code>format</code> method matches no other keyword.
 * If a pattern does not provide a phrase for <code>other</code>, the  method
 * it's provided to returns the error  <code>U_DEFAULT_KEYWORD_MISSING</code>.
 * If a pattern provides more than one phrase for the same keyword, the
 * error <code>U_DUPLICATE_KEYWORD</code> is returned.
 * <br/>
 * Spaces between <code><i>keyword</i></code> and
 * <code>{<i>phrase</i>}</code>  will be ignored; spaces within
 * <code>{<i>phrase</i>}</code> will be preserved.</p>
 *
 * <p>The phrase for a particular select case may contain other message
 * format patterns. <code>SelectFormat</code> preserves these so that  you
 * can use the strings produced by <code>SelectFormat</code> with other
 * formatters. If you are using <code>SelectFormat</code> inside a
 * <code>MessageFormat</code> pattern, <code>MessageFormat</code> will
 * automatically evaluate the resulting format pattern.
 * Thus, curly braces (<code>{</code>, <code>}</code>) are <i>only</i> allowed
 * in phrases to define a nested format pattern.</p>
 *
 * <pre>Example:
 * MessageFormat msgFmt = new MessageFormat("{0} est " +
 *     "{1, select, female {all&#u00E9;e} other {all&#u00E9;}} &#u00E0; Paris.",
 *     new ULocale("fr"));
 * Object args[] = {"Kirti","female"};
 * System.out.println(msgFmt.format(args));
 * </pre>
 * <p>
 * Produces the output:<br/>
 * <code>Kirti est all&#u00E9;e &#u00E0; Paris.</code>
 * </p>
 *
 * @draft ICU 4.4
 * @provisional This API might change or be removed in a future release.
 */

public class SelectFormat extends Format{
    // Generated by serialver from JDK 1.5
    private static final long serialVersionUID = 2993154333257524984L;

    /*
     * The applied pattern string.
     */
    private String pattern = null;

    /*
     * The format messages for each select case. It is a mapping:
     *  <code>String</code>(select case keyword) --&gt; <code>String</code>
     *  (message for this select case).
     */
    transient private Map<String, String> parsedValues = null;

    /**
     * Common name for the default select form.  This name is returned
     * for values to which no other form in the rule applies.  It 
     * can additionally be assigned rules of its own.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    private static final String KEYWORD_OTHER = "other";

    /*
     * The types of character classifications 
     */
    private enum CharacterClass {
        T_START_KEYWORD, T_CONTINUE_KEYWORD, T_LEFT_BRACE,
        T_RIGHT_BRACE, T_SPACE, T_OTHER
    };

    /*
     * The different states needed in state machine
     * in applyPattern method. 
     */
    private enum State {
       START_STATE, KEYWORD_STATE,
       PAST_KEYWORD_STATE, PHRASE_STATE      
    };

    /**
     * Creates a new <code>SelectFormat</code> for a given pattern string.
     * @param  pattern the pattern for this <code>SelectFormat</code>.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public SelectFormat(String pattern) {
        init();
        applyPattern(pattern);
    }

    /*
     * Initializes the <code>SelectFormat</code> object.
     * Postcondition:<br/>
     *   <code>parsedValues</code>: is <code>null</code><br/>
     *   <code>pattern</code>:      is <code>null</code><br/>
     */
    private void init() {
        parsedValues = null;
        pattern = null;
    }

    /**
     * Classifies the characters 
     */
    private boolean checkValidKeyword(String argKeyword) {
        int len = argKeyword.length();
        if (len < 1) {
            return false;
        };
        if (classifyCharacter(argKeyword.charAt(0)) != CharacterClass.T_START_KEYWORD) {
            return false;
        };
        for (int i = 1; i < len; i++) {
            CharacterClass type = classifyCharacter(argKeyword.charAt(i));
            if (type != CharacterClass.T_START_KEYWORD && 
                type != CharacterClass.T_CONTINUE_KEYWORD) {
                return false;
            };
        };
        return true;
    }

    /**
     * Classifies the characters.
     */
    private CharacterClass classifyCharacter(char ch) {
        if ((ch >= 'A') && (ch <= 'Z')) {
            return CharacterClass.T_START_KEYWORD;
        }
        if ((ch >= 'a') && (ch <= 'z')) {
            return CharacterClass.T_START_KEYWORD;
        }
        if ((ch >= '0') && (ch <= '9')) {
            return CharacterClass.T_CONTINUE_KEYWORD;
        }
        switch (ch) {
            case '{':
                return CharacterClass.T_LEFT_BRACE;
            case '}':
                return CharacterClass.T_RIGHT_BRACE;
            case ' ':
            case '\t':
                return CharacterClass.T_SPACE;
            case '-':
            case '_':
                return CharacterClass.T_CONTINUE_KEYWORD;
            default :
                return CharacterClass.T_OTHER;
        }
    }

    /**
     * Sets the pattern used by this select format.
     * Patterns and their interpretation are specified in the class description.
     *
     * @param pattern the pattern for this select format.
     * @throws IllegalArgumentException when the pattern is not a valid select format pattern.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public void applyPattern(String pattern) {
        parsedValues = null;
        this.pattern = pattern;

        //Initialization
        StringBuilder keyword = new StringBuilder();
        StringBuilder phrase = new StringBuilder();
        int braceCount = 0;

        parsedValues = new HashMap<String, String>();

        //Process the state machine
        State state = State.START_STATE;
        for (int i = 0; i < pattern.length(); i++ ){
            //Get the character and check its type
            char ch = pattern.charAt(i);
            CharacterClass type = classifyCharacter(ch);

            //Process the state machine
            switch (state) {
                //At the start of pattern
                case START_STATE:
                    switch (type) {
                        case T_SPACE:
                            break ;
                        case T_START_KEYWORD:
                            state = State.KEYWORD_STATE;
                            keyword.append(ch);
                            break ;
                        //If anything else is encountered, it's a syntax error
                        default :
                            parsingFailure("Pattern syntax error.");
                }//end of switch(type)
                break ;

                //Handle the keyword state
                case KEYWORD_STATE:
                    switch (type) {
                        case T_SPACE:
                            state = State.PAST_KEYWORD_STATE;
                            break ;
                        case T_START_KEYWORD:
                        case T_CONTINUE_KEYWORD:
                            keyword.append(ch);
                            break ;
                        case T_LEFT_BRACE:
                            state = State.PHRASE_STATE;
                        break ;
                        //If anything else is encountered, it's a syntax error
                        default :
                            parsingFailure("Pattern syntax error.");
                    }//end of switch(type)
                    break ;

                //Handle the pastkeyword state
                case PAST_KEYWORD_STATE:
                    switch (type) {
                        case T_SPACE:
                            break ;
                        case T_LEFT_BRACE:
                            state = State.PHRASE_STATE;
                            break ;
                        //If anything else is encountered, it's a syntax error
                        default :
                            parsingFailure("Pattern syntax error.");
                    }//end of switch(type)
                        break ;

               //Handle the phrase state
               case PHRASE_STATE:
                    switch (type) {
                        case T_LEFT_BRACE:
                            braceCount++;
                            phrase.append(ch);
                            break ;
                        case T_RIGHT_BRACE:
                            //Matching keyword, phrase pair found
                            if (braceCount == 0){
                                //Check validity of keyword
                                if (parsedValues.get(keyword.toString()) != null) {
                                    parsingFailure("Duplicate keyword error.");
                                }
                                if (keyword.length() == 0) {
                                    parsingFailure("Pattern syntax error.");
                                }

                                //Store the keyword, phrase pair in hashTable
                                parsedValues.put( keyword.toString(), phrase.toString());

                               //Reinitialize
                                keyword.setLength(0);
                                phrase.setLength(0);
                                state = State.START_STATE;
                            }

                            if (braceCount > 0){
                                braceCount-- ;
                                phrase.append(ch);
                            }
                            break ;
                        default :
                            phrase.append(ch);
                    }//end of switch(type)
                    break ;

                //Handle the  default case of switch(state)
                default :
                    parsingFailure("Pattern syntax error.");

            }//end of switch(state)
        }

        //Check if the state machine is back to START_STATE
        if ( state != State.START_STATE){
            parsingFailure("Pattern syntax error.");
        }

        //Check if "other" keyword is present 
        if ( !checkSufficientDefinition() ) {
            parsingFailure("Pattern syntax error. " 
                    + "Value for case \"" + KEYWORD_OTHER
                    + "\" was not defined. ");
        }
        return ;
    }

    /**
     * Returns the pattern for this <code>SelectFormat</code>
     *
     * @return the pattern string
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public String toPattern() {
        return pattern;
    }

    /**
     * Selects the phrase for the given keyword.
     *
     * @param keyword a keyword for which the select message should be formatted.
     * @return the string containing the formatted select message.
     * @throws IllegalArgumentException when the given keyword is not available in the select format pattern
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public final String format(String keyword) {
        //Check for the validity of the keyword
        if( !checkValidKeyword(keyword) ){
            throw new IllegalArgumentException("Invalid formatting argument.");
        }

        // If no pattern was applied, throw an exception
        if (parsedValues == null) {
            throw new IllegalStateException("Invalid format error.");
        }

        // Get appropriate format pattern.
        String selectedPattern = parsedValues.get(keyword);
        if (selectedPattern == null) { // Fallback to others.
            selectedPattern = parsedValues.get(KEYWORD_OTHER);
        }
        return selectedPattern;
    }

    /**
     * Selects the phrase for the given keyword.
     * and appends the formatted message to the given <code>StringBuffer</code>.
     * @param keyword a keyword for which the select message should be formatted.
     * @param toAppendTo the formatted message will be appended to this
     *        <code>StringBuffer</code>.
     * @param pos will be ignored by this method.
     * @throws IllegalArgumentException when the given keyword is not available in the select format pattern
     * @return the string buffer passed in as toAppendTo, with formatted text
     *         appended.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public StringBuffer format(Object keyword, StringBuffer toAppendTo,
            FieldPosition pos) {
        if (keyword instanceof String) {
            toAppendTo.append(format( (String)keyword));
        }else{
            throw new IllegalArgumentException("'" + keyword + "' is not a String");
        }
        return toAppendTo;
    }

    /**
     * This method is not supported by <code>SelectFormat</code>.
     * @param source the string to be parsed.
     * @param pos defines the position where parsing is to begin,
     * and upon return, the position where parsing left off.  If the position
     * has not changed upon return, then parsing failed.
     * @return nothing because this method is not supported.
     * @throws UnsupportedOperationException thrown always.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public Object parseObject(String source, ParsePosition pos) {
        throw new UnsupportedOperationException();
    }

    /*
     * Checks if the applied pattern provided enough information,
     * i.e., if the attribute <code>parsedValues</code> stores enough
     * information for select formatting.
     * Will be called at the end of pattern parsing.
     */
    private boolean checkSufficientDefinition() {
        // Check that at least the default rule is defined.
        return parsedValues.get(KEYWORD_OTHER) != null; 
    }

    /*
     * Helper method that resets the <code>SelectFormat</code> object and throws
     * an <code>IllegalArgumentException</code> with a given error text.
     * @param errorText the error text of the exception message.
     * @throws IllegalArgumentException will always be thrown by this method.
     */
    private void parsingFailure(String errorText) {
        // Set SelectFormat to a valid state.
        init();
        throw new IllegalArgumentException(errorText);
    }

    /**
     * {@inheritDoc}
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof SelectFormat)) {
            return false;
        }
        SelectFormat sf = (SelectFormat) obj;
        return pattern == null ? sf.pattern == null : pattern.equals(sf.pattern);
    }

    /**
     * {@inheritDoc}
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public int hashCode() {
        if (pattern != null) {
            return pattern.hashCode();
        }
        return 0;
    }

    /**
     * Returns a string representation of the object
     * @return a text representation of the format object.
     * The result string includes the class name and
     * the pattern string returned by <code>toPattern()</code>.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("pattern='" + pattern + "'");
        return buf.toString();
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (pattern != null) {
            applyPattern(pattern);
        }
    }
}
