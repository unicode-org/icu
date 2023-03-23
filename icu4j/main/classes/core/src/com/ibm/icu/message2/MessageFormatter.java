// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

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
 * It will also provide extension points via interfaces to allow users to supply new formatters and selectors without having to modify the specification.
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
 * public void testMf2() {
 *     final Locale enGb = Locale.forLanguageTag("en-GB");
 *     Map<String, Object> arguments = new HashMap<>();
 *     arguments.put("name", "John");
 *     arguments.put("exp", new Date(1679971371000L));  // March 27, 2023, 7:42:51 PM
 *
 *     MessageFormatter mf2 = MessageFormatter.builder()
 *         .setPattern("{Hello {$name}, your card expires on {$exp :datetime skeleton=yMMMdE}!}")
 *         .setLocale(enGb)
 *         .build();
 *
 *     assertEquals(
 *         "Hello John, your card expires on Mon, 27 Mar 2023!",
 *         mf2.formatToString(arguments));
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
 *     <td>{@code arguments.put("name", "John")}</td>
 *     <td>{@code &#125;$name&#126;}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code arguments.put("exp", new Date(…))}</td>
 *     <td>{@code &#125;$exp :datetime skeleton=yMMMdE&#126;} <br> {@code &#125;$exp :datetime datestyle=full&#126;}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code arguments.put("val", 3.141592653)}</td>
 *     <td>{@code &#125;$val&#126;} <br> {@code &#125;$val :number skeleton=(.####)&#126;}</td>
 *   </tr>
 *   <tr>
 *     <td>No argument for fixed values known at build time</td>
 *     <td>{@code &#125;(123456789.531) :number&#126;}</td>
 *   </tr>
 * </table>
 * 
 * <h4>Plural selection message</h4>
 * 
 * <blockquote><pre>
 * &#064;Test
 * public void testMf2Selection() {
 *    final String message = "match {$count :plural}\n"
 *            + " when one {You have one notification.}\n"
 *            + " when * {You have {$count} notifications.}\n";
 *    final Locale enGb = Locale.forLanguageTag("en-GB");
 *    Map<String, Object> arguments = new HashMap<>();
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
 * <p>The tech preview implementation comes with formatters for numbers ({@code number}), 
 * date / time ({@code datetime}), 
 * plural selectors ({@code plural} and {@code selectordinal}),
 * and general selector ({@code select}), 
 * very similar to what MessageFormat offers.</p>
 * 
 * <p>The <a target="github" href="https://github.com/unicode-org/icu/tree/main/icu4j/main/tests/core/src/com/ibm/icu/dev/test/message2">ICU test code</a>
 * covers most features, and has examples of how to make custom placeholder formatters;
 * you can look for classes that implement {@code com.ibm.icu.message2.FormatterFactory}
 * (they are named {@code Custom*Test.java}).</p>
 * 
 * <h3>Functions currently implemented</h3>
 * 
 * <p>These are the functions interpreted right now:</p>
 *
 * <table border="1">
 * <tr>
 *   <td rowspan="4">{@code datetime}</td>
 *   <td>Similar to MessageFormat's {@code date} and {@code time}.</td>
 * </tr>
 *
 *   <tr><td>{@code datestyle} and {@code timestyle}<br>
 *   Similar to {@code argStyle : short | medium | long | full}.<br>
 *   Same values are accepted, but we can use both in one placeholder,
 *   for example <code>{$due :datetime datestyle=full timestyle=long}</code>.
 *   </td></tr>
 *
 *   <tr><td>{@code pattern}<br>
 *   Similar to {@code argStyle = argStyleText}.<br>
 *   This is bad i18n practice, and will probably be dropped.<br>
 *   This is included just to support migration to MessageFormat 2.
 *   </td></tr>
 *
 *   <tr><td>{@code skeleton}<br>
 *   Same as {@code argStyle = argSkeletonText}.<br>
 *   These are the date/time skeletons as supported by {@link com.ibm.icu.text.SimpleDateFormat}.
 *   </td></tr>
 *
 * <tr>
 *   <td rowspan="4">{@code number}</td>
 *   <td>Similar to MessageFormat's {@code number}.</td>
 * </tr>
 *
 *   <tr><td>{@code skeleton}<br>
 *   These are the number skeletons as supported by {@link com.ibm.icu.number.NumberFormatter}.</td></tr>
 *
 *   <tr><td>{@code minimumFractionDigits}<br>
 *   Only implemented to be able to pass the unit tests from the ECMA tech preview implementation,
 *   which prefers options bags to skeletons.<br>
 *   TBD if the final {@number} function will support skeletons, option backs, or both.</td></tr>
 *
 *   <tr><td>{@code offset}<br>
 *   Used to support plural with an offset.</td></tr>
 *
 * <tr><td >{@code identity}</td><td>Returns the direct string value of the argument (calling {@code toString()}).</td></tr>
 *
 * <tr>
 *   <td rowspan="3">{@code plural}</td>
 *   <td>Similar to MessageFormat's {@code plural}.</td>
 * </tr>
 *
 *   <tr><td>{@code skeleton}<br>
 *   These are the number skeletons as supported by {@link com.ibm.icu.number.NumberFormatter}.<br>
 *   Can also be indirect, from a local variable of type {@code number} (recommended).</td></tr>
 *
 *   <tr><td>{@code offset}<br>
 *   Used to support plural with an offset.<br>
 *   Can also be indirect, from a local variable of type {@code number} (recommended).</td></tr>
 *
 * <tr>
 *   <td>{@code selectordinal}</td>
 *   <td>Similar to MessageFormat's {@code selectordinal}.<br>
 * For now it accepts the same parameters as {@code plural}, although there is no use case for them.<br>
 * TBD if this will be merged into {@code plural} (with some {@code kind} option) or not.</td></tr>
 *
 * <tr><td>{@code select}</td><td>Literal match, same as MessageFormat's {@code select}.</td></tr>
 * </table>
 *
 * @internal ICU 72 technology preview
 * @deprecated This API is for technology preview only.
 */
@Deprecated
public class MessageFormatter {
    private final Locale locale;
    private final String pattern;
    private final Mf2FunctionRegistry functionRegistry;
    private final Mf2DataModel dataModel;
    private final Mf2DataModelFormatter modelFormatter;

    private MessageFormatter(Builder builder) {
        this.locale = builder.locale;
        this.functionRegistry = builder.functionRegistry;
        if ((builder.pattern == null && builder.dataModel == null)
                || (builder.pattern != null && builder.dataModel != null)) {
            throw new IllegalArgumentException("You need to set either a pattern, or a dataModel, but not both.");
        }

        if (builder.dataModel != null) {
            this.dataModel = builder.dataModel;
            this.pattern = Mf2Serializer.dataModelToString(this.dataModel);
        } else {
            this.pattern = builder.pattern;
            Mf2Serializer tree = new Mf2Serializer();
            Mf2Parser parser = new Mf2Parser(pattern, tree);
            try {
                parser.parse_Message();
                dataModel = tree.build();
            } catch (Mf2Parser.ParseException pe) {
                throw new IllegalArgumentException(
                        "Parse error:\n"
                        + "Message: <<" + pattern + ">>\n"
                        + "Error:" + parser.getErrorMessage(pe) + "\n");
            }
        }
        modelFormatter = new Mf2DataModelFormatter(dataModel, locale, functionRegistry);
    }

    /**
     * Creates a builder.
     *
     * @return the Builder.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the locale to use for all the formatting and selections in
     * the current {@code MessageFormatter}.
     *
     * @return the locale.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public Locale getLocale() {
        return locale;
    }

    /**
     * Get the pattern (the serialized message in MessageFormat 2 syntax) of
     * the current {@code MessageFormatter}.
     *
     * <p>If the {@code MessageFormatter} was created from an {@link Mf2DataModel}
     * the this string is generated from that model.</p>
     *
     * @return the pattern.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
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
    public Mf2DataModel getDataModel() {
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
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
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
    public FormattedMessage format(Map<String, Object> arguments) {
        throw new RuntimeException("Not yet implemented.");
    }

    /**
     * A {@code Builder} used to build instances of {@link MessageFormatter}.
     *
     * @internal ICU 72 technology preview
     * @deprecated This API is for technology preview only.
     */
    @Deprecated
    public static class Builder {
        private Locale locale = Locale.getDefault(Locale.Category.FORMAT);
        private String pattern = null;
        private Mf2FunctionRegistry functionRegistry = Mf2FunctionRegistry.builder().build();
        private Mf2DataModel dataModel = null;

        // Prevent direct creation
        private Builder() {
        }

        /**
         * Sets the locale to use for all formatting and selection operations.
         *
         * @param locale the locale to set.
         * @return the builder, for fluent use.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
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
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public Builder setPattern(String pattern) {
            this.pattern = pattern;
            this.dataModel = null;
            return this;
        }

        /**
         * Sets an instance of {@link Mf2FunctionRegistry} that should register any
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
        public Builder setFunctionRegistry(Mf2FunctionRegistry functionRegistry) {
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
        public Builder setDataModel(Mf2DataModel dataModel) {
            this.dataModel = dataModel;
            this.pattern = null;
            return this;
        }

        /**
         * Builds an instance of {@link MessageFormatter}.
         *
         * @return the {@link MessageFormatter} created.
         *
         * @internal ICU 72 technology preview
         * @deprecated This API is for technology preview only.
         */
        @Deprecated
        public MessageFormatter build() {
            return new MessageFormatter(this);
        }
    }
}
