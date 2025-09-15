// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.message2;

import java.util.Locale;
import java.util.Map;

/**
 * <h3>Overview of {@code MessageFormatter}</h3>
 *
 * <p>In ICU4J, the {@code MessageFormatter} class is the next iteration of {@link com.ibm.icu.text.MessageFormat}.
 * This new version will build on the lessons learned from using MessageFormat for 25 years
 * in various environments, when used directly or as a base for other public APIs.</p>
 *
 *
 * <p>The effort to design a succesor to {@code MessageFormat} will result in a specification
 * referred to as MessageFormat 2.0.
 * The reasoning for this effort is shared in the
 * <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/docs/why_mf_next.md">“Why
 * MessageFormat needs a successor”</a> document.</p>
 *
 * <p>MessageFormat 2.0 will be more modular and easier to port and backport.
 * It will also provide extension points via interfaces to allow users to supply new functions without having to modify the specification.
 * ICU will eventually include support for new formatters, such as intervals, relative time, lists, measurement units, personal names, and more,
 * as well as the ability for users to supply their own custom implementations.
 * These will potentially support use cases like grammatical gender, inflection, markup regimes (such as those require for text-to-speech),
 * and other complex message management needs.</p>
 *
 * <p>The MessageFormat Working Group, which develops the new data model, semantics, and syntax,
 * is hosted on <a target="github" href="https://github.com/unicode-org/message-format-wg">GitHub</a>.
 * The current specification for the syntax and data model can be found
 * <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/spec/syntax.md">here</a>.</p>
 *
 * <p>This technical preview implements enough functions for {@code MessageFormatter} to be useful in many situations,
 * but the final set of functions and the parameters accepted by those functions is not yet finalized.</p>
 *
 * <h3>Examples</h3>
 *
 * <h4>Basic usage</h4>
 *
 * <blockquote><pre>
 * import static org.junit.Assert.assertEquals;
 * import java.util.Date;
 * import java.util.HashMap;
 * import java.util.Locale;
 * import java.util.Map;
 *
 * import com.ibm.icu.message2.MessageFormatter;
 *
 * &#064;Test
 * public void test() {
 *     final Locale enGb = Locale.forLanguageTag("en-GB");
 *     Map&lt;String, Object&gt; arguments = new HashMap&lt;&gt;();
 *     arguments.put("name", "John");
 *     arguments.put("exp", new Date(2023 - 1900, 2, 27, 19, 42, 51));  // March 27, 2023, 7:42:51 PM
 *
 *     MessageFormatter mf2 = MessageFormatter.builder()
 *             .setPattern("Hello {$name}, your card expires on {$exp :datetime year=numeric month=short day=numeric weekday=short}!")
 *             .setLocale(enGb)
 *             .build();
 *
 *     assertEquals(
 *             "Hello John, your card expires on Mon, 27 Mar 2023!",
 *             mf2.formatToString(arguments));
 * }
 * </pre></blockquote>
 *
 * <h4>Placeholder examples</h4>
 *
 * <table border="1">
 *   <tr>
 *     <th>Code to set runtime value for placeholder</th>
 *     <th>Examples of placeholder in message pattern</th>
 *   </tr>
 *   <tr>
 *     <td><code>arguments.put("name", "John")</code></td>
 *     <td><code>{$name}</code></td>
 *   </tr>
 *   <tr>
 *     <td><code>arguments.put("exp", new Date(…))</code></td>
 *     <td><code>{$exp :datetime skeleton=year=numeric month=short day=numeric weekday=short}</code> <br>
 *         <code>{$exp :datetime dateStyle=full}</code></td>
 *   </tr>
 *   <tr>
 *     <td><code>arguments.put("val", 3.141592653)</code></td>
 *     <td><code>{$val}</code> <br> <code>{$val :number minimumFractionDigits=5}</code></td>
 *   </tr>
 *   <tr>
 *     <td>No argument for fixed values known at build time</td>
 *     <td><code>{|123456789.531| :number}</code></td>
 *   </tr>
 * </table>
 *
 * <h4>Plural selection message</h4>
 *
 * <blockquote><pre>
 * &#064;Test
 * public void testSelection() {
 *    final String message = ".match {$count :number}\n"
 *            + " 1 {{You have one notification.}}\n"
 *            + " * {{You have {$count} notifications.}}\n";
 *    final Locale enGb = Locale.forLanguageTag("en-GB");
 *    Map&lt;String, Object&gt; arguments = new HashMap&lt;&gt;();
 *
 *    MessageFormatter mf2 = MessageFormatter.builder()
 *        .setPattern(message)
 *        .setLocale(enGb)
 *        .build();
 *
 *    arguments.put("count", 1);
 *    assertEquals(
 *        "You have one notification.",
 *        mf2.formatToString(arguments));
 *
 *    arguments.put("count", 42);
 *    assertEquals(
 *        "You have 42 notifications.",
 *        mf2.formatToString(arguments));
 * }
 * </pre></blockquote>
 *
 * <h4>Built-in formatter functions</h4>
 *
 * <p>The tech preview implementation comes with formatter functions for numbers ({@code :number}),
 * date / time ({@code :datetime}, {@code :date}, {@code :time}),
 * plural selectors ({@code :number} with options for {@code plural} and {@code ordinal} selection),
 * and general selector ({@code :string}),
 * very similar to what {@code MessageFormat} offers.</p>
 *
 * <p>The <a target="github" href="https://github.com/unicode-org/icu/tree/main/icu4j/main/core/src/test/java/com/ibm/icu/dev/test/message2">ICU test code</a>
 * covers most features, and has examples of how to make custom placeholder formatters;
 * you can look for classes that implement {@code com.ibm.icu.message2.FunctionFactory}
 * (they are named {@code Custom*Test.java}).</p>
 *
 * <p>The complete list of valid options for each function, and how they infulence the results, can be found at
 * <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/spec/registry.md">here</a>.<p>
 *
 * @draft ICU 78
 */
public class MessageFormatter {
    private final Locale locale;
    private final String pattern;
    private final ErrorHandlingBehavior errorHandlingBehavior;
    private final BidiIsolation bidiIsolation;
    private final MFFunctionRegistry functionRegistry;
    private final MFDataModel.Message dataModel;
    private final MFDataModelFormatter modelFormatter;

    private MessageFormatter(Builder builder) {
        this.locale = builder.locale;
        this.functionRegistry = builder.functionRegistry;
        this.errorHandlingBehavior = builder.errorHandlingBehavior;
        this.bidiIsolation = builder.bidiIsolation;
        if ((builder.pattern == null && builder.dataModel == null)
                || (builder.pattern != null && builder.dataModel != null)) {
            throw new IllegalArgumentException(
                    "You need to set either a pattern, or a dataModel, but not both.");
        }

        if (builder.dataModel != null) {
            this.dataModel = builder.dataModel;
            // this.pattern = MFSerializer.dataModelToString(this.dataModel);
            this.pattern = MFSerializer.dataModelToString(dataModel);
        } else {
            this.pattern = builder.pattern;
            try {
                this.dataModel = MFParser.parse(pattern);
            } catch (MFParseException pe) {
                throw new IllegalArgumentException(""
                        + "Parse error:\n"
                        + "Message: <<" + pattern + ">>\n"
                        + "Error: " + pe.getMessage() + "\n");
            }
        }
        modelFormatter = new MFDataModelFormatter(dataModel, locale, errorHandlingBehavior, bidiIsolation, functionRegistry);
    }

    /**
     * Creates a builder.
     *
     * @return the Builder.
     *
     * @draft ICU 78
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the locale to use for all the formatting and selections in
     * the current {@code MessageFormatter}.
     *
     * @return the locale.
     *
     * @draft ICU 78
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Get the {@link ErrorHandlingBehavior} to use when encountering errors in
     * the current {@code MessageFormatter}.
     *
     * @return the error handling behavior.
     *
     * @draft ICU 78
     */
    public ErrorHandlingBehavior getErrorHandlingBehavior() {
        return errorHandlingBehavior;
    }

    /**
     * Get the {@link BidiIsolation} algorithm to use when formatting mixed
     * message parts with mixed direction.
     *
     * @return the bidi isolation algorithm.
     *
     * @draft ICU 78
     */
    public BidiIsolation getBidiIsolation() {
        return bidiIsolation;
    }

    /**
     * Get the pattern (the serialized message in MessageFormat 2 syntax) of
     * the current {@code MessageFormatter}.
     *
     * <p>If the {@code MessageFormatter} was created from an {@link MFDataModel}
     * the this string is generated from that model.</p>
     *
     * @return the pattern.
     *
     * @draft ICU 78
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Give public access to the message data model.
     *
     * <p>This data model is similar to the functionality we have today
     * in {@link com.ibm.icu.text.MessagePatternUtil} maybe even a bit more higher level.</p>
     *
     * <p>We can also imagine a model where one parses the string syntax, takes the data model,
     * modifies it, and then uses that modified model to create a {@code MessageFormatter}.</p>
     *
     * @return the data model.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public MFDataModel.Message getDataModel() {
        return dataModel;
    }

    /**
     * Formats a map of objects by iterating over the MessageFormat's pattern,
     * with the plain text “as is” and the arguments replaced by the formatted objects.
     *
     * @param arguments a map of objects to be formatted and substituted.
     * @return the string representing the message with parameters replaced.
     *
     * @throws IllegalArgumentException when something goes wrong
     *         (for example wrong argument type, or null arguments, etc.)
     *
     * @draft ICU 78
     */
    public String formatToString(Map<String, Object> arguments) {
        return modelFormatter.format(arguments);
    }

    /**
     * Not yet implemented: formats a map of objects by iterating over the MessageFormat's
     * pattern, with the plain text “as is” and the arguments replaced by the formatted objects.
     *
     * @param arguments a map of objects to be formatted and substituted.
     * @return the {@link FormattedMessage} class representing the message with parameters replaced.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    @SuppressWarnings("static-method")
    public FormattedMessage format(Map<String, Object> arguments) {
        throw new RuntimeException("Not yet implemented.");
    }

    /**
     * Determines how the formatting errors will be handled at runtime.
     *
     * <p>Parsing errors and data model errors always throw and will not be affected by this setting.<br>
     * But resolution errors and formatting errors will either try to fallback (if possible) or throw,
     * depending on this setting.</p>
     *
     * <p>Used in conjunction with the
     * {@link MessageFormatter.Builder#setErrorHandlingBehavior(MessageFormatter.ErrorHandlingBehavior)} method.</p>
     *
     * @draft ICU 78
     */
    public static enum ErrorHandlingBehavior {
        /**
         * Suppress errors and return best-effort output.
         *
         * @draft ICU 78
         */
        BEST_EFFORT,
        /**
         * Signal all {@code MessageFormat} errors by throwing a {@link RuntimeException}.
         *
         * @draft ICU 78
         */
        STRICT
    }

    /**
     * Determines how the mixtures of bidirectional text are converted to string.
     *
     * <p>They can be either ignored, or will implement the default algorithm
     * described in the MessageFormat 2 specification.</p>
     *
     * <p>Used in conjunction with the
     * {@link MessageFormatter.Builder#setBidiIsolation(MessageFormatter.BidiIsolation)} method.</p>
     *
     * @draft ICU 78
     */
    public static enum BidiIsolation {
        /**
         * Ignore any text direction mixture, don't introduce bidi control characters in the formatted result.
         *
         * @draft ICU 78
         */
        NONE,
        /**
         * Wrap direction mixtures in bidi control characters as described in the MessageFormat 2 specification.
         *
         * @draft ICU 78
         */
        DEFAULT
    }

    /**
     * A {@code Builder} used to build instances of {@link MessageFormatter}.
     *
     * @draft ICU 78
     */
    public static class Builder {
        private Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        private String pattern = null;
        private ErrorHandlingBehavior errorHandlingBehavior = ErrorHandlingBehavior.BEST_EFFORT;
        private BidiIsolation bidiIsolation = BidiIsolation.NONE;
        private MFFunctionRegistry functionRegistry = MFFunctionRegistry.builder().build();
        private MFDataModel.Message dataModel = null;

        // Prevent direct creation
        private Builder() {}

        /**
         * Sets the locale to use for all formatting and selection operations.
         *
         * @param locale the locale to set.
         * @return the builder, for fluent use.
         *
         * @draft ICU 78
         */
        public Builder setLocale(Locale locale) {
            this.locale = locale;
            return this;
        }

        /**
         * Sets the pattern (in MessageFormat 2 syntax) used to create the message.<br>
         * It conflicts with the data model, so it will reset it (the last call on setter wins).
         *
         * @param pattern the pattern to set.
         * @return the builder, for fluent use.
         *
         * @draft ICU 78
         */
        public Builder setPattern(String pattern) {
            this.pattern = pattern;
            this.dataModel = null;
            return this;
        }

        /**
         * Sets the {@link ErrorHandlingBehavior} to use when encountering errors at formatting time.
         *
         * <p>The default value is {@code ErrorHandlingBehavior.BEST_EFFORT}, trying to fallback.</p>
         *
         * @param errorHandlingBehavior the error handling behavior to use.
         * @return the builder, for fluent use.
         *
         * @draft ICU 78
         */
        public Builder setErrorHandlingBehavior(ErrorHandlingBehavior errorHandlingBehavior) {
            this.errorHandlingBehavior = errorHandlingBehavior;
            return this;
        }

        /**
         * Sets the {@link BidiIsolation} to introduce bidi control characters / tags
         * as described in the MessageFormat 2 specification.
         *
         * <p>The default value is {@code BidiIsolation.NONE}.</p>
         *
         * @param bidiIsolation the bidi isolation algorithm to use.
         * @return the builder, for fluent use.
         *
         * @draft ICU 78
         */
        public Builder setBidiIsolation(BidiIsolation bidiIsolation) {
            this.bidiIsolation = bidiIsolation;
            return this;
        }

        /**
         * Sets an instance of {@link MFFunctionRegistry} that should register any
         * custom functions used by the message.
         *
         * <p>There is no need to do this in order to use standard functions
         * (for example date / time / number formatting, plural / ordinal / literal selection).<br>
         * The exact set of standard functions, with the types they format and the options
         * they accept is still TBD.</p>
         *
         * @param functionRegistry the function registry to set.
         * @return the builder, for fluent use.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder setFunctionRegistry(MFFunctionRegistry functionRegistry) {
            this.functionRegistry = functionRegistry;
            return this;
        }

        /**
         * Sets the data model used to create the message.<br>
         * It conflicts with the pattern, so it will reset it (the last call on setter wins).
         *
         * @param dataModel the pattern to set.
         * @return the builder, for fluent use.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder setDataModel(MFDataModel.Message dataModel) {
            this.dataModel = dataModel;
            this.pattern = null;
            return this;
        }

        /**
         * Builds an instance of {@link MessageFormatter}.
         *
         * @return the {@link MessageFormatter} created.
         *
         * @draft ICU 78
         */
        public MessageFormatter build() {
            return new MessageFormatter(this);
        }
    }
}
