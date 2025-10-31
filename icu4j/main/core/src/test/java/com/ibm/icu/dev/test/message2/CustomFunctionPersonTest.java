// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.message2.FormattedPlaceholder;
import com.ibm.icu.message2.Function;
import com.ibm.icu.message2.FunctionFactory;
import com.ibm.icu.message2.MFFunctionRegistry;
import com.ibm.icu.message2.PlainStringFormattedValue;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Showing a custom function for a user defined class. */
@RunWith(JUnit4.class)
@SuppressWarnings({"static-method", "javadoc"})
public class CustomFunctionPersonTest extends CoreTestFmwk {

    public static class Person {
        final String title;
        final String firstName;
        final String lastName;

        public Person(String title, String firstName, String lastName) {
            this.title = title;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        @Override
        public String toString() {
            return "Person {title='"
                    + title
                    + "', firstName='"
                    + firstName
                    + "', lastName='"
                    + lastName
                    + "'}";
        }
    }

    private static class PersonNameFunctionFactory implements FunctionFactory {
        @Override
        public Function create(Locale locale, Map<String, Object> fixedOptions) {
            return new PersonNameFunctionImpl(
                    fixedOptions.get("formality"), fixedOptions.get("length"));
        }

        static class PersonNameFunctionImpl implements Function {
            boolean useFormal = false;
            final String length;

            public PersonNameFunctionImpl(Object level, Object length) {
                this.useFormal = Objects.equals(level, "formal");
                this.length = Objects.toString(length);
            }

            @Override
            public String formatToString(Object toFormat, Map<String, Object> variableOptions) {
                return format(toFormat, variableOptions).toString();
            }

            // Very-very primitive implementation of the "CLDR Person Name Formatting" spec:
            // https://docs.google.com/document/d/1uvv6gdkuFwtbNV26Pk7ddfZult4unYwR6DnnKYbujUo/
            @Override
            public FormattedPlaceholder format(
                    Object toFormat, Map<String, Object> variableOptions) {
                String result;
                if (toFormat instanceof Person) {
                    Person person = (Person) toFormat;
                    switch (length) {
                        case "long":
                            result = person.title + " " + person.firstName + " " + person.lastName;
                            break;
                        case "medium":
                            result =
                                    useFormal
                                            ? person.firstName + " " + person.lastName
                                            : person.title + " " + person.firstName;
                            break;
                        case "short": // intentional fall-through
                        default:
                            result =
                                    useFormal
                                            ? person.title + " " + person.lastName
                                            : person.firstName;
                    }
                } else {
                    result = Objects.toString(toFormat);
                }
                return new FormattedPlaceholder(toFormat, new PlainStringFormattedValue(result));
            }
        }
    }

    private static final MFFunctionRegistry CUSTOM_FUNCTION_REGISTRY =
            MFFunctionRegistry.builder()
                    .setFunction("person", new PersonNameFunctionFactory())
                    .setDefaultFunctionNameForType(Person.class, "person")
                    .build();

    @Test
    public void testCustomFunctions() {
        Person who = new Person("Mr.", "John", "Doe");

        TestUtils.runTestCase(
                new TestCase.Builder()
                        .pattern("Hello {$name :person formality=formal}")
                        .arguments(Args.of("name", who))
                        .expected("Hello {$name}")
                        .build());

        TestUtils.runTestCase(
                new TestCase.Builder()
                        .pattern("Hello {$name :person formality=informal}")
                        .arguments(Args.of("name", who))
                        .expected("Hello {$name}")
                        .build());

        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern("Hello {$name :person formality=formal}")
                        .arguments(Args.of("name", who))
                        .expected("Hello Mr. Doe")
                        .build());
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern("Hello {$name :person formality=informal}")
                        .arguments(Args.of("name", who))
                        .expected("Hello John")
                        .build());
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern("Hello {$name :person formality=formal length=long}")
                        .arguments(Args.of("name", who))
                        .expected("Hello Mr. John Doe")
                        .build());
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern("Hello {$name :person formality=formal length=medium}")
                        .arguments(Args.of("name", who))
                        .expected("Hello John Doe")
                        .build());
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern("Hello {$name :person formality=formal length=short}")
                        .arguments(Args.of("name", who))
                        .expected("Hello Mr. Doe")
                        .build());
    }

    @Test
    public void testCustomFunctionsComplexMessage() {
        Person femalePerson = new Person("Ms.", "Jane", "Doe");
        Person malePerson = new Person("Mr.", "John", "Doe");
        Person unknownPerson = new Person("Mr./Ms.", "Anonymous", "Doe");
        String message =
                ""
                        + ".input {$hostGender :string}\n"
                        + ".input {$guestCount :number}\n"
                        + ".local $hostName = {$host :person length=long}\n"
                        + ".local $guestName = {$guest :person length=long}\n"
                        + ".local $guestsOther = {$guestCount :number icu:offset=1}\n"
                        //  + "\n"
                        + ".match $hostGender $guestCount\n"
                        //  + "\n"
                        + " female 0 {{{$hostName} does not give a party.}}\n"
                        + " female 1 {{{$hostName} invites {$guestName} to her party.}}\n"
                        + " female 2 {{{$hostName} invites {$guestName} and one other person to her party.}}\n"
                        + " female * {{{$hostName} invites {$guestName} and {$guestsOther} other people to her party.}}\n"
                        //  + "\n"
                        + " male 0 {{{$hostName} does not give a party.}}\n"
                        + " male 1 {{{$hostName} invites {$guestName} to his party.}}\n"
                        + " male 2 {{{$hostName} invites {$guestName} and one other person to his party.}}\n"
                        + " male * {{{$hostName} invites {$guestName} and {$guestsOther} other people to his party.}}\n"
                        //  + "\n"
                        + " * 0 {{{$hostName} does not give a party.}}\n"
                        + " * 1 {{{$hostName} invites {$guestName} to their party.}}\n"
                        + " * 2 {{{$hostName} invites {$guestName} and one other person to their party.}}\n"
                        + " * * {{{$hostName} invites {$guestName} and {$guestsOther} other people to their party.}}";

        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern(message)
                        .arguments(
                                Args.of(
                                        "hostGender",
                                        "female",
                                        "host",
                                        femalePerson,
                                        "guest",
                                        malePerson,
                                        "guestCount",
                                        3))
                        .expected(
                                "Ms. Jane Doe invites Mr. John Doe and 2 other people to her party.")
                        .build());
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern(message)
                        .arguments(
                                Args.of(
                                        "hostGender",
                                        "female",
                                        "host",
                                        femalePerson,
                                        "guest",
                                        malePerson,
                                        "guestCount",
                                        2))
                        .expected(
                                "Ms. Jane Doe invites Mr. John Doe and one other person to her party.")
                        .build());
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern(message)
                        .arguments(
                                Args.of(
                                        "hostGender",
                                        "female",
                                        "host",
                                        femalePerson,
                                        "guest",
                                        malePerson,
                                        "guestCount",
                                        1))
                        .expected("Ms. Jane Doe invites Mr. John Doe to her party.")
                        .build());
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern(message)
                        .arguments(
                                Args.of(
                                        "hostGender",
                                        "male",
                                        "host",
                                        malePerson,
                                        "guest",
                                        femalePerson,
                                        "guestCount",
                                        3))
                        .expected(
                                "Mr. John Doe invites Ms. Jane Doe and 2 other people to his party.")
                        .build());
        TestUtils.runTestCase(
                CUSTOM_FUNCTION_REGISTRY,
                new TestCase.Builder()
                        .pattern(message)
                        .arguments(
                                Args.of(
                                        "hostGender",
                                        "unknown",
                                        "host",
                                        unknownPerson,
                                        "guest",
                                        femalePerson,
                                        "guestCount",
                                        2))
                        .expected(
                                "Mr./Ms. Anonymous Doe invites Ms. Jane Doe and one other person to their party.")
                        .build());
    }
}
