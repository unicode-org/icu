// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.message2.FormattedPlaceholder;
import com.ibm.icu.message2.Formatter;
import com.ibm.icu.message2.FormatterFactory;
import com.ibm.icu.message2.MessageFormatter;
import com.ibm.icu.message2.Mf2FunctionRegistry;
import com.ibm.icu.number.FormattedNumber;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.util.BuddhistCalendar;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;

/**
 * Tests migrated from {@link com.ibm.icu.text.MessageFormat}, to show what they look like and that they work.
 *
 * <p>It does not include all the tests for edge cases and error handling, only the ones that show real functionality.</p>
 */
@RunWith(JUnit4.class)
@SuppressWarnings("javadoc")
public class MessageFormat2Test extends CoreTestFmwk {

    @Test
    public void test() {
        MessageFormatter mf2 = MessageFormatter.builder()
                .setPattern("{Hello World!}").build();
        assertEquals("simple message",
                "Hello World!",
                mf2.formatToString(Args.NONE));
    }

    @Test
    public void testDateFormat() {
        Date expiration = new Date(2022 - 1900, java.util.Calendar.OCTOBER, 27);
        MessageFormatter mf2 = MessageFormatter.builder()
                .setPattern("{Your card expires on {$exp :datetime skeleton=yMMMdE}!}")
                .build();
        assertEquals("date format",
                "Your card expires on Thu, Oct 27, 2022!",
                mf2.formatToString(Args.of("exp", expiration)));

        mf2 = MessageFormatter.builder()
                .setPattern("{Your card expires on {$exp :datetime datestyle=full}!}")
                .build();
        assertEquals("date format",
                "Your card expires on Thursday, October 27, 2022!",
                mf2.formatToString(Args.of("exp", expiration)));
        mf2 = MessageFormatter.builder()
                .setPattern("{Your card expires on {$exp :datetime datestyle=long}!}")
                .build();
        assertEquals("date format",
                "Your card expires on October 27, 2022!",
                mf2.formatToString(Args.of("exp", expiration)));
        mf2 = MessageFormatter.builder()
                .setPattern("{Your card expires on {$exp :datetime datestyle=medium}!}")
                .build();
        assertEquals("date format",
                "Your card expires on Oct 27, 2022!",
                mf2.formatToString(Args.of("exp", expiration)));
        mf2 = MessageFormatter.builder()
                .setPattern("{Your card expires on {$exp :datetime datestyle=short}!}")
                .build();
        assertEquals("date format",
                "Your card expires on 10/27/22!",
                mf2.formatToString(Args.of("exp", expiration)));

        Calendar cal = new GregorianCalendar(2022, Calendar.OCTOBER, 27);
        mf2 = MessageFormatter.builder()
                .setPattern("{Your card expires on {$exp :datetime skeleton=yMMMdE}!}")
                .build();
        assertEquals("date format",
                "Your card expires on Thu, Oct 27, 2022!",
                mf2.formatToString(Args.of("exp", cal)));

        // Implied function based on type of the object to format
        mf2 = MessageFormatter.builder()
                .setPattern("{Your card expires on {$exp}!}")
                .build();
        assertEquals("date format",
                "Your card expires on 10/27/22, 12:00\u202FAM!",
                mf2.formatToString(Args.of("exp", expiration)));
        assertEquals("date format",
                "Your card expires on 10/27/22, 12:00\u202FAM!",
                mf2.formatToString(Args.of("exp", cal)));

        // Implied function based on type of the object to format
        // This is a calendar that is not explicitly added to the registry.
        // But we test to see if it works because it extends Calendar, which is registered.
        BuddhistCalendar calNotRegistered = new BuddhistCalendar(2022, Calendar.OCTOBER, 27);
        mf2 = MessageFormatter.builder()
                .setPattern("{Your card expires on {$exp :datetime skeleton=yMMMdE}!}")
                .build();
        assertEquals("date format",
                "Your card expires on Wed, Oct 27, 1479!",
                mf2.formatToString(Args.of("exp", calNotRegistered)));

        mf2 = MessageFormatter.builder()
                .setPattern("{Your card expires on {$exp :datetime skeleton=yMMMdE}!}")
                .build();
        assertEquals("date format",
                "Your card expires on Wed, Oct 27, 1479!",
                mf2.formatToString(Args.of("exp", calNotRegistered)));
    }

    @Test
    public void testPlural() {
        String message = ""
                + "match {$count :plural}\n"
                + " when 1 {You have one notification.}\n"
                + " when * {You have {$count} notifications.}\n";

        MessageFormatter mf2 = MessageFormatter.builder()
                .setPattern(message)
                .build();
        assertEquals("plural",
                "You have one notification.",
                mf2.formatToString(Args.of("count", 1)));
        assertEquals("plural",
                "You have 42 notifications.",
                mf2.formatToString(Args.of("count", 42)));
    }

    @Test
    public void testPluralOrdinal() {
        String message = ""
                + "match {$place :selectordinal}\n"
                + " when 1 {You got the gold medal}\n"
                + " when 2 {You got the silver medal}\n"
                + " when 3 {You got the bronze medal}\n"
                + " when one {You got in the {$place}st place}\n"
                + " when two {You got in the {$place}nd place}\n"
                + " when few {You got in the {$place}rd place}\n"
                + " when * {You got in the {$place}th place}\n"
                ;

        MessageFormatter mf2 = MessageFormatter.builder()
                .setPattern(message)
                .build();
        assertEquals("selectordinal",
                "You got the gold medal",
                mf2.formatToString(Args.of("place", 1)));
        assertEquals("selectordinal",
                "You got the silver medal",
                mf2.formatToString(Args.of("place", 2)));
        assertEquals("selectordinal",
                "You got the bronze medal",
                mf2.formatToString(Args.of("place", 3)));
        assertEquals("selectordinal",
                "You got in the 21st place",
                mf2.formatToString(Args.of("place", 21)));
        assertEquals("selectordinal",
                "You got in the 32nd place",
                mf2.formatToString(Args.of("place", 32)));
        assertEquals("selectordinal",
                "You got in the 23rd place",
                mf2.formatToString(Args.of("place", 23)));
        assertEquals("selectordinal",
                "You got in the 15th place",
                mf2.formatToString(Args.of("place", 15)));
    }

    static class TemperatureFormatterFactory implements FormatterFactory {
        int constructCount = 0;
        int formatCount = 0;
        int fFormatterCount = 0;
        int cFormatterCount = 0;

        @Override
        public Formatter createFormatter(Locale locale, Map<String, Object> fixedOptions) {
            // Check that the formatter can only see the fixed options
            Assert.assertTrue(fixedOptions.containsKey("skeleton"));
            Assert.assertFalse(fixedOptions.containsKey("unit"));

            Object valSkeleton = fixedOptions.get("skeleton");
            LocalizedNumberFormatter nf = valSkeleton != null
                    ? NumberFormatter.forSkeleton(valSkeleton.toString()).locale(locale)
                    : NumberFormatter.withLocale(locale);

            return new TemperatureFormatterImpl(nf, this);
        }

        static private class TemperatureFormatterImpl implements Formatter {
            private final TemperatureFormatterFactory formatterFactory;
            private final LocalizedNumberFormatter nf;
            private final Map<String, LocalizedNumberFormatter> cachedFormatters =
                    new HashMap<>();

            TemperatureFormatterImpl(LocalizedNumberFormatter nf, TemperatureFormatterFactory formatterFactory) {
                this.nf = nf;
                this.formatterFactory = formatterFactory;
                this.formatterFactory.constructCount++;
            }

            @Override
            public String formatToString(Object toFormat, Map<String, Object> variableOptions) {
                return this.format(toFormat, variableOptions).toString();
            }

            @Override
            public FormattedPlaceholder format(Object toFormat, Map<String, Object> variableOptions) {
                // Check that the formatter can only see the variable options
                Assert.assertFalse(variableOptions.containsKey("skeleton"));
                Assert.assertTrue(variableOptions.containsKey("unit"));
                this.formatterFactory.formatCount++;

                String unit = variableOptions.get("unit").toString();
                LocalizedNumberFormatter realNf = cachedFormatters.get(unit);
                if (realNf == null) {
                    switch (variableOptions.get("unit").toString()) {
                        case "C":
                            formatterFactory.cFormatterCount++;
                            realNf = nf.unit(MeasureUnit.CELSIUS);
                            break;
                        case "F":
                            formatterFactory.fFormatterCount++;
                            realNf = nf.unit(MeasureUnit.FAHRENHEIT);
                            break;
                        default:
                            realNf = nf;
                            break;
                    }
                    cachedFormatters.put(unit, realNf);
                }

                FormattedNumber result;
                if (toFormat instanceof Double) {
                    result = realNf.format((double) toFormat);
                } else if (toFormat instanceof Long) {
                    result = realNf.format((Long) toFormat);
                } else if (toFormat instanceof Number) {
                    result = realNf.format((Number) toFormat);
                } else if (toFormat instanceof Measure) {
                    result = realNf.format((Measure) toFormat);
                } else {
                    result = null;
                }
                return new FormattedPlaceholder(toFormat, result);
            }
        }
    }

    @Test
    public void testFormatterIsCreatedOnce() {
        TemperatureFormatterFactory counter = new TemperatureFormatterFactory();
        Mf2FunctionRegistry registry = Mf2FunctionRegistry.builder()
                .setFormatter("temp", counter)
                .build();
        String message = "{Testing {$count :temp unit=$unit skeleton=(.00/w)}.}";
        MessageFormatter mf2 = MessageFormatter.builder()
                .setFunctionRegistry(registry)
                .setPattern(message)
                .build();

        final int maxCount = 10;
        for (int count = 0; count < maxCount; count++) {
            assertEquals("cached formatter",
                    "Testing " + count + "°C.",
                    mf2.formatToString(Args.of("count", count, "unit", "C")));
            assertEquals("cached formatter",
                    "Testing " + count + "°F.",
                    mf2.formatToString(Args.of("count", count, "unit", "F")));
        }

        // Check that the constructor was only called once,
        // and the formatter as many times as the public call to format.
        assertEquals("cached formatter", 1, counter.constructCount);
        assertEquals("cached formatter", maxCount * 2, counter.formatCount);
        assertEquals("cached formatter", 1, counter.fFormatterCount);
        assertEquals("cached formatter", 1, counter.cFormatterCount);

        // Check that the skeleton is respected
        assertEquals("cached formatter",
                "Testing 12°C.",
                mf2.formatToString(Args.of("count", 12, "unit", "C")));
        assertEquals("cached formatter",
                "Testing 12.50°F.",
                mf2.formatToString(Args.of("count", 12.5, "unit", "F")));
        assertEquals("cached formatter",
                "Testing 12.54°C.",
                mf2.formatToString(Args.of("count", 12.54, "unit", "C")));
        assertEquals("cached formatter",
                "Testing 12.54°F.",
                mf2.formatToString(Args.of("count", 12.54321, "unit", "F")));

        message = "{Testing {$count :temp unit=$unit skeleton=(.0/w)}.}";
        mf2 = MessageFormatter.builder()
                .setFunctionRegistry(registry)
                .setPattern(message)
                .build();
        // Check that the skeleton is respected
        assertEquals("cached formatter",
                "Testing 12°C.",
                mf2.formatToString(Args.of("count", 12, "unit", "C")));
        assertEquals("cached formatter",
                "Testing 12.5°F.",
                mf2.formatToString(Args.of("count", 12.5, "unit", "F")));
        assertEquals("cached formatter",
                "Testing 12.5°C.",
                mf2.formatToString(Args.of("count", 12.54, "unit", "C")));
        assertEquals("cached formatter",
                "Testing 12.5°F.",
                mf2.formatToString(Args.of("count", 12.54321, "unit", "F")));
    }

    @Test
    public void testPluralWithOffset() {
        String message = ""
                + "match {$count :plural offset=2}\n"
                + " when 1 {Anna}\n"
                + " when 2 {Anna and Bob}\n"
                + " when one {Anna, Bob, and {$count :number offset=2} other guest}\n"
                + " when * {Anna, Bob, and {$count :number offset=2} other guests}\n";
        MessageFormatter mf2 = MessageFormatter.builder()
                .setPattern(message)
                .build();
        assertEquals("plural with offset",
                "Anna",
                mf2.formatToString(Args.of("count", 1)));
        assertEquals("plural with offset",
                "Anna and Bob",
                mf2.formatToString(Args.of("count", 2)));
        assertEquals("plural with offset",
                "Anna, Bob, and 1 other guest",
                mf2.formatToString(Args.of("count", 3)));
        assertEquals("plural with offset",
                "Anna, Bob, and 2 other guests",
                mf2.formatToString(Args.of("count", 4)));
        assertEquals("plural with offset",
                "Anna, Bob, and 10 other guests",
                mf2.formatToString(Args.of("count", 12)));
    }

    @Test
    public void testPluralWithOffsetAndLocalVar() {
        String message = ""
                + "let $foo = {$count :number offset=2}"
                + "match {$foo :plural}\n" // should "inherit" the offset
                + " when 1 {Anna}\n"
                + " when 2 {Anna and Bob}\n"
                + " when one {Anna, Bob, and {$foo} other guest}\n"
                + " when * {Anna, Bob, and {$foo} other guests}\n";
        MessageFormatter mf2 = MessageFormatter.builder()
                .setPattern(message)
                .build();
        assertEquals("plural with offset",
                "Anna",
                mf2.formatToString(Args.of("count", 1)));
        assertEquals("plural with offset",
                "Anna and Bob",
                mf2.formatToString(Args.of("count", 2)));
        assertEquals("plural with offset",
                "Anna, Bob, and 1 other guest",
                mf2.formatToString(Args.of("count", 3)));
        assertEquals("plural with offset",
                "Anna, Bob, and 2 other guests",
                mf2.formatToString(Args.of("count", 4)));
        assertEquals("plural with offset",
                "Anna, Bob, and 10 other guests",
                mf2.formatToString(Args.of("count", 12)));
    }

    @Test
    public void testPluralWithOffsetAndLocalVar2() {
        String message = ""
                + "let $foo = {$amount :number skeleton=(.00/w)}\n"
                + "match {$foo :plural}\n" // should "inherit" the offset
                + " when 1 {Last dollar}\n"
                + " when one {{$foo} dollar}\n"
                + " when * {{$foo} dollars}\n";
        MessageFormatter mf2 = MessageFormatter.builder()
                .setPattern(message)
                .build();
        assertEquals("plural with offset",
                "Last dollar",
                mf2.formatToString(Args.of("amount", 1)));
        assertEquals("plural with offset",
                "2 dollars",
                mf2.formatToString(Args.of("amount", 2)));
        assertEquals("plural with offset",
                "3 dollars",
                mf2.formatToString(Args.of("amount", 3)));
    }

    @Test
    public void testLoopOnLocalVars() {
        String message = ""
                + "let $foo = {$baz :number}\n"
                + "let $bar = {$foo}\n"
                + "let $baz = {$bar}\n"
                + "{The message uses {$baz} and works}\n";
        MessageFormatter mf2 = MessageFormatter.builder()
                .setPattern(message)
                .build();
        assertEquals("test local vars loop",
                "The message uses {$bar} and works",
                mf2.formatToString(Args.of("amount", 1)));
    }

    @Test
    public void testVariableOptionsInSelector() {
        String messageVar = ""
                + "match {$count :plural offset=$delta}\n"
                + " when 1 {A}\n"
                + " when 2 {A and B}\n"
                + " when one {A, B, and {$count :number offset=$delta} more character}\n"
                + " when * {A, B, and {$count :number offset=$delta} more characters}\n";
        MessageFormatter mfVar = MessageFormatter.builder()
                .setPattern(messageVar)
                .build();
        assertEquals("test local vars loop", "A",
                mfVar.formatToString(Args.of("count", 1, "delta", 2)));
        assertEquals("test local vars loop", "A and B",
                mfVar.formatToString(Args.of("count", 2, "delta", 2)));
        assertEquals("test local vars loop", "A, B, and 1 more character",
                mfVar.formatToString(Args.of("count", 3, "delta", 2)));
        assertEquals("test local vars loop", "A, B, and 5 more characters",
                mfVar.formatToString(Args.of("count", 7, "delta", 2)));

        String messageVar2 = ""
                + "match {$count :plural offset=$delta}\n"
                + " when 1 {Exactly 1}\n"
                + " when 2 {Exactly 2}\n"
                + " when * {Count = {$count :number offset=$delta} and delta={$delta}.}\n";
        MessageFormatter mfVar2 = MessageFormatter.builder()
                .setPattern(messageVar2)
                .build();
        assertEquals("test local vars loop", "Exactly 1",
                mfVar2.formatToString(Args.of("count", 1, "delta", 0)));
        assertEquals("test local vars loop", "Exactly 1",
                mfVar2.formatToString(Args.of("count", 1, "delta", 1)));
        assertEquals("test local vars loop", "Exactly 1",
                mfVar2.formatToString(Args.of("count", 1, "delta", 2)));

        assertEquals("test local vars loop", "Exactly 2",
                mfVar2.formatToString(Args.of("count", 2, "delta", 0)));
        assertEquals("test local vars loop", "Exactly 2",
                mfVar2.formatToString(Args.of("count", 2, "delta", 1)));
        assertEquals("test local vars loop", "Exactly 2",
                mfVar2.formatToString(Args.of("count", 2, "delta", 2)));

        assertEquals("test local vars loop", "Count = 3 and delta=0.",
                mfVar2.formatToString(Args.of("count", 3, "delta", 0)));
        assertEquals("test local vars loop", "Count = 2 and delta=1.",
                mfVar2.formatToString(Args.of("count", 3, "delta", 1)));
        assertEquals("test local vars loop", "Count = 1 and delta=2.",
                mfVar2.formatToString(Args.of("count", 3, "delta", 2)));

        assertEquals("test local vars loop", "Count = 23 and delta=0.",
                mfVar2.formatToString(Args.of("count", 23, "delta", 0)));
        assertEquals("test local vars loop", "Count = 22 and delta=1.",
                mfVar2.formatToString(Args.of("count", 23, "delta", 1)));
        assertEquals("test local vars loop", "Count = 21 and delta=2.",
                mfVar2.formatToString(Args.of("count", 23, "delta", 2)));
    }

    @Test
    public void testVariableOptionsInSelectorWithLocalVar() {
        String messageFix = ""
                + "let $offCount = {$count :number offset=2}"
                + "match {$offCount :plural}\n"
                + " when 1 {A}\n"
                + " when 2 {A and B}\n"
                + " when one {A, B, and {$offCount} more character}\n"
                + " when * {A, B, and {$offCount} more characters}\n";
        MessageFormatter mfFix = MessageFormatter.builder()
                .setPattern(messageFix)
                .build();
        assertEquals("test local vars loop", "A", mfFix.formatToString(Args.of("count", 1)));
        assertEquals("test local vars loop", "A and B", mfFix.formatToString(Args.of("count", 2)));
        assertEquals("test local vars loop", "A, B, and 1 more character", mfFix.formatToString(Args.of("count", 3)));
        assertEquals("test local vars loop", "A, B, and 5 more characters", mfFix.formatToString(Args.of("count", 7)));

        String messageVar = ""
                + "let $offCount = {$count :number offset=$delta}"
                + "match {$offCount :plural}\n"
                + " when 1 {A}\n"
                + " when 2 {A and B}\n"
                + " when one {A, B, and {$offCount} more character}\n"
                + " when * {A, B, and {$offCount} more characters}\n";
        MessageFormatter mfVar = MessageFormatter.builder()
                .setPattern(messageVar)
                .build();
        assertEquals("test local vars loop", "A",
                mfVar.formatToString(Args.of("count", 1, "delta", 2)));
        assertEquals("test local vars loop", "A and B",
                mfVar.formatToString(Args.of("count", 2, "delta", 2)));
        assertEquals("test local vars loop", "A, B, and 1 more character",
                mfVar.formatToString(Args.of("count", 3, "delta", 2)));
        assertEquals("test local vars loop", "A, B, and 5 more characters",
                mfVar.formatToString(Args.of("count", 7, "delta", 2)));

        String messageVar2 = ""
                + "let $offCount = {$count :number offset=$delta}"
                + "match {$offCount :plural}\n"
                + " when 1 {Exactly 1}\n"
                + " when 2 {Exactly 2}\n"
                + " when * {Count = {$count}, OffCount = {$offCount}, and delta={$delta}.}\n";
        MessageFormatter mfVar2 = MessageFormatter.builder()
                .setPattern(messageVar2)
                .build();
        assertEquals("test local vars loop", "Exactly 1",
                mfVar2.formatToString(Args.of("count", 1, "delta", 0)));
        assertEquals("test local vars loop", "Exactly 1",
                mfVar2.formatToString(Args.of("count", 1, "delta", 1)));
        assertEquals("test local vars loop", "Exactly 1",
                mfVar2.formatToString(Args.of("count", 1, "delta", 2)));

        assertEquals("test local vars loop", "Exactly 2",
                mfVar2.formatToString(Args.of("count", 2, "delta", 0)));
        assertEquals("test local vars loop", "Exactly 2",
                mfVar2.formatToString(Args.of("count", 2, "delta", 1)));
        assertEquals("test local vars loop", "Exactly 2",
                mfVar2.formatToString(Args.of("count", 2, "delta", 2)));

        assertEquals("test local vars loop", "Count = 3, OffCount = 3, and delta=0.",
                mfVar2.formatToString(Args.of("count", 3, "delta", 0)));
        assertEquals("test local vars loop", "Count = 3, OffCount = 2, and delta=1.",
                mfVar2.formatToString(Args.of("count", 3, "delta", 1)));
        assertEquals("test local vars loop", "Count = 3, OffCount = 1, and delta=2.",
                mfVar2.formatToString(Args.of("count", 3, "delta", 2)));

        assertEquals("test local vars loop", "Count = 23, OffCount = 23, and delta=0.",
                mfVar2.formatToString(Args.of("count", 23, "delta", 0)));
        assertEquals("test local vars loop", "Count = 23, OffCount = 22, and delta=1.",
                mfVar2.formatToString(Args.of("count", 23, "delta", 1)));
        assertEquals("test local vars loop", "Count = 23, OffCount = 21, and delta=2.",
                mfVar2.formatToString(Args.of("count", 23, "delta", 2)));
    }
}
