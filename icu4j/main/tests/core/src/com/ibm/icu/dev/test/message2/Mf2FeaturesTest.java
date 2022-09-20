// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.CurrencyAmount;

/**
 * Trying to show off most of the features in one place.
 *
 * <p>It covers the examples in the
 * <a href="https://github.com/unicode-org/message-format-wg/blob/main/spec/syntax.md">spec document</a>,
 * except for the custom formatters ones, which are too verbose and were moved to separate test classes.</p>
 * </p>
 */
@RunWith(JUnit4.class)
@SuppressWarnings("javadoc")
public class Mf2FeaturesTest extends TestFmwk {

    // November 23, 2022 at 7:42:37.123 PM
    static final Date TEST_DATE = new Date(1669261357123L);

    @Test
    public void testEmptyMessage() {
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{}")
                .arguments(Args.NONE)
                .expected("")
                .build());
    }

    @Test
    public void testPlainText() {
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Hello World!}")
                .arguments(Args.NONE)
                .expected("Hello World!")
                .build());
    }

    @Test
    public void testPlaceholders() {
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Hello, {$userName}!}")
                .arguments(Args.of("userName", "John"))
                .expected("Hello, John!")
                .build());
    }

    @Test
    public void testArgumentMissing() {
        // Test to check what happens if an argument name from the placeholder is not found
        // We do what the old ICU4J MessageFormat does.
        String message = "{Hello {$name}, today is {$today :datetime skeleton=yMMMMdEEEE}.}";

        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(message)
                .arguments(Args.of("name", "John", "today", TEST_DATE))
                .expected("Hello John, today is Wednesday, November 23, 2022.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(message)
                .arguments(Args.of("name", "John"))
                .expected("Hello John, today is {$today}.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(message)
                .arguments(Args.of("today", TEST_DATE))
                .expected("Hello {$name}, today is Wednesday, November 23, 2022.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(message)
                .arguments(Args.NONE)
                .expected("Hello {$name}, today is {$today}.")
                .build());
    }

    @Test
    public void testDefaultLocale() {
        String message = "{Date: {$date :datetime skeleton=yMMMMdEEEE}.}";
        String expectedEn = "Date: Wednesday, November 23, 2022.";
        String expectedRo = "Date: miercuri, 23 noiembrie 2022.";
        Map<String, Object> arguments = Args.of("date", TEST_DATE);

        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(message)
                .arguments(arguments)
                .expected(expectedEn)
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(message)
                .arguments(arguments)
                .locale("ro")
                .expected(expectedRo)
                .build());

        Locale originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("ro"));
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(message)
                .arguments(arguments)
                .locale("en-US")
                .expected(expectedEn)
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(message)
                .arguments(arguments)
                .expected(expectedRo)
                .build());
        Locale.setDefault(originalLocale);
    }

    @Test
    public void testAllKindOfDates() {
        // Default function
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date}.}")
                .locale("ro")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: 23.11.2022, 19:42.")
                .build());
        // Default options
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date :datetime}.}")
                .locale("ro")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: 23.11.2022, 19:42.")
                .build());

        // Skeleton
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date :datetime skeleton=yMMMMd}.}")
                .locale("ro-RO")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: 23 noiembrie 2022.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date :datetime skeleton=jm}.}")
                .locale("ro-RO")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: 19:42.")
                .build());

        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date :datetime skeleton=yMMMd}.}")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: Nov 23, 2022.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date :datetime skeleton=yMMMdjms}.}")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: Nov 23, 2022, 7:42:37\u202FPM.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date :datetime skeleton=jm}.}")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: 7:42\u202FPM.")
                .build());

        // Style
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date :datetime datestyle=long}.}")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: November 23, 2022.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(
                "{Testing date formatting: {$date :datetime datestyle=medium}.}")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: Nov 23, 2022.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date :datetime datestyle=short}.}")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: 11/23/22.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date :datetime timestyle=long}.}")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: 7:42:37\u202FPM PST.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date :datetime timestyle=medium}.}")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: 7:42:37\u202FPM.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(
                "{Testing date formatting: {$date :datetime timestyle=short}.}")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: 7:42\u202FPM.")
                .build());

        // Pattern
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date :datetime pattern=(d 'of' MMMM, y 'at' HH:mm)}.}")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: 23 of November, 2022 at 19:42.")
                .build());
    }

    @Test
    public void testAllKindOfNumbers() {
        double value = 1234567890.97531;

        // From literal values
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{From literal: {(123456789) :number}!}")
                .locale("ro")
                .arguments(Args.of("val", value))
                .expected("From literal: 123.456.789!")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{From literal: {(123456789.531) :number}!}")
                .locale("ro")
                .arguments(Args.of("val", value))
                .expected("From literal: 123.456.789,531!")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{From literal: {(123456789.531) :number}!}")
                .locale("my")
                .arguments(Args.of("val", value))
                .expected("From literal: \u1041\u1042\u1043,\u1044\u1045\u1046,\u1047\u1048\u1049.\u1045\u1043\u1041!")
                .build());

        // Testing that the detection works for various types (without specifying :number)
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Default double: {$val}!}")
                .locale("en-IN")
                .arguments(Args.of("val", value))
                .expected("Default double: 1,23,45,67,890.97531!")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Default double: {$val}!}")
                .locale("ro")
                .arguments(Args.of("val", value))
                .expected("Default double: 1.234.567.890,97531!")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Default float: {$val}!}")
                .locale("ro")
                .arguments(Args.of("val", 3.1415926535))
                .expected("Default float: 3,141593!")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Default long: {$val}!}")
                .locale("ro")
                .arguments(Args.of("val", 1234567890123456789L))
                .expected("Default long: 1.234.567.890.123.456.789!")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Default number: {$val}!}")
                .locale("ro")
                .arguments(Args.of("val", new BigDecimal("1234567890123456789.987654321")))
                .expected("Default number: 1.234.567.890.123.456.789,987654!")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Price: {$val}}")
                .locale("de")
                .arguments(Args.of("val", new CurrencyAmount(1234.56, Currency.getInstance("EUR"))))
                .expected("Price: 1.234,56\u00A0\u20AC")
                .build());

        // Various skeletons
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Skeletons, minFraction: {$val :number skeleton=(.00000000*)}!}")
                .locale("ro")
                .arguments(Args.of("val", value))
                .expected("Skeletons, minFraction: 1.234.567.890,97531000!")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Skeletons, maxFraction: {$val :number skeleton=(.###)}!}")
                .locale("ro")
                .arguments(Args.of("val", value))
                .expected("Skeletons, maxFraction: 1.234.567.890,975!")
                .build());
        // Currency
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Skeletons, currency: {$val :number skeleton=(currency/EUR)}!}")
                .locale("de")
                .arguments(Args.of("val", value))
                .expected("Skeletons, currency: 1.234.567.890,98\u00A0\u20AC!")
                .build());
        // Currency as a parameter
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Skeletons, currency: {$val :number skeleton=$skel}!}")
                .locale("de")
                .arguments(Args.of("val", value, "skel", "currency/EUR"))
                .expected("Skeletons, currency: 1.234.567.890,98\u00A0\u20AC!")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Skeletons, currency: {$val :number skeleton=$skel}!}")
                .locale("de")
                .arguments(Args.of("val", value, "skel", "currency/JPY"))
                .expected("Skeletons, currency: 1.234.567.891\u00A0\u00A5!")
                .build());

        // Various measures
        double celsius = 27;
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(""
                        + "let $intl = {$valC :number skeleton=(unit/celsius)}\n"
                        + "let $us = {$valF :number skeleton=(unit/fahrenheit)}\n"
                        + "{Temperature: {$intl} ({$us})}")
                .locale("ro")
                .arguments(Args.of("valC", celsius, "valF", celsius * 9 / 5 + 32))
                .expected("Temperature: 27 \u00B0C (80,6 \u00B0F)")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Height: {$len :number skeleton=(unit/meter)}}")
                .locale("ro")
                .arguments(Args.of("len", 1.75))
                .expected("Height: 1,75 m")
                .build());

        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Skeletons, currency: {$val :number skeleton=(currency/EUR)}!}")
                .locale("de")
                .arguments(Args.of("val", value))
                .expected("Skeletons, currency: 1.234.567.890,98\u00A0\u20AC!")
                .build());
    }

    @Test
    public void testSpecialPluralWithDecimals() {
        String message;
        message = "let $amount = {$count :number}\n"
                + "match {$amount :plural}\n"
                + "  when 1 {I have {$amount} dollar.}\n"
                + "  when * {I have {$amount} dollars.}\n";
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(message)
                .locale("en-US")
                .arguments(Args.of("count", 1))
                .expected("I have 1 dollar.")
                .build());
        message = "let $amount = {$count :number skeleton=(.00*)}\n"
                + "match {$amount :plural skeleton=(.00*)}\n"
                + "  when 1 {I have {$amount} dollar.}\n"
                + "  when * {I have {$amount} dollars.}\n";
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(message)
                .locale("en-US")
                .arguments(Args.of("count", 1))
                .expected("I have 1.00 dollars.")
                .build());
    }

    @Test
    public void testDefaultFunctionAndOptions() {
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date}.}")
                .locale("ro")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: 23.11.2022, 19:42.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern("{Testing date formatting: {$date :datetime}.}")
                .locale("ro")
                .arguments(Args.of("date", TEST_DATE))
                .expected("Testing date formatting: 23.11.2022, 19:42.")
                .build());
    }

    @Test
    public void testSimpleSelection() {
        String message = "match {$count :plural}\n"
                + " when 1 {You have one notification.}\n"
                + " when * {You have {$count} notifications.}\n";

        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(message)
                .arguments(Args.of("count", 1))
                .expected("You have one notification.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(message)
                .arguments(Args.of("count", 42))
                .expected("You have 42 notifications.")
                .build());
    }

    @Test
    public void testComplexSelection() {
        String message = ""
                + "match {$photoCount :plural} {$userGender :select}\n"
                + " when 1 masculine {{$userName} added a new photo to his album.}\n"
                + " when 1 feminine {{$userName} added a new photo to her album.}\n"
                + " when 1 * {{$userName} added a new photo to their album.}\n"
                + " when * masculine {{$userName} added {$photoCount} photos to his album.}\n"
                + " when * feminine {{$userName} added {$photoCount} photos to her album.}\n"
                + " when * * {{$userName} added {$photoCount} photos to their album.}";

        TestUtils.runTestCase(new TestCase.Builder().pattern(message)
                .arguments(Args.of("photoCount", 1, "userGender", "masculine", "userName", "John"))
                .expected("John added a new photo to his album.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder().pattern(message)
                .arguments(Args.of("photoCount", 1, "userGender", "feminine", "userName", "Anna"))
                .expected("Anna added a new photo to her album.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder().pattern(message)
                .arguments(Args.of("photoCount", 1, "userGender", "unknown", "userName", "Anonymous"))
                .expected("Anonymous added a new photo to their album.")
                .build());

        TestUtils.runTestCase(new TestCase.Builder().pattern(message)
                .arguments(Args.of("photoCount", 13, "userGender", "masculine", "userName", "John"))
                .expected("John added 13 photos to his album.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder().pattern(message)
                .arguments(Args.of("photoCount", 13, "userGender", "feminine", "userName", "Anna"))
                .expected("Anna added 13 photos to her album.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder().pattern(message)
                .arguments(Args.of("photoCount", 13, "userGender", "unknown", "userName", "Anonymous"))
                .expected("Anonymous added 13 photos to their album.")
                .build());
    }

    // Local Variables

    @Test
    public void testSimpleLocaleVariable() {
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(""
                        + "let $expDate = {$expDate :datetime skeleton=yMMMdE}\n"
                        + "{Your tickets expire on {$expDate}.}")
                .arguments(Args.of("count", 1, "expDate", TEST_DATE))
                .expected("Your tickets expire on Wed, Nov 23, 2022.")
                .build());
    }

    @Test
    public void testLocaleVariableWithSelect() {
        String message = ""
                + "let $expDate = {$expDate :datetime skeleton=yMMMdE}\n"
                + "match {$count :plural}\n"
                + " when 1 {Your ticket expires on {$expDate}.}\n"
                + " when * {Your {$count} tickets expire on {$expDate}.}\n";

        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(message)
                .arguments(Args.of("count", 1, "expDate", TEST_DATE))
                .expected("Your ticket expires on Wed, Nov 23, 2022.")
                .build());
        TestUtils.runTestCase(new TestCase.Builder()
                .pattern(message)
                .arguments(Args.of("count", 3, "expDate", TEST_DATE))
                .expected("Your 3 tickets expire on Wed, Nov 23, 2022.")
                .build());
    }

}
