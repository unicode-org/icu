// © 2016 and later: Unicode, Inc. and others.

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2.h"
#include "messageformat2test.h"

using namespace icu::message2;

/*
Tests reflect the syntax specified in

  https://github.com/unicode-org/message-format-wg/commits/main/spec/message.abnf

as of the following commit from 2023-05-09:
  https://github.com/unicode-org/message-format-wg/commit/194f6efcec5bf396df36a19bd6fa78d1fa2e0867

   Since the MessageFormat2 "parser" is currently only a validator,
   the following tests only verify that valid messages are validated by the parser
   and that invalid messages are rejected with an error reflecting the correct line number
   and offset for the unexpected character.
*/

// These tests are from the icu4j test suite under icu4j/main/tests/core/src/com/ibm/icu/dev/test/message2/
UnicodeString validTestCases[] = {
    // From Mf2IcuTest.java
    "{There are {$count} files on {$where}}",
    "{At {$when :datetime timestyle=default} on {$when :datetime datestyle=default}, \
      there was {$what} on planet {$planet :number kind=integer}.}",
    "{The disk \"{$diskName}\" contains {$fileCount} file(s).}",
    "match {$userGender :select}\n\
     when female {{$userName} est all\u00E9e \u00E0 Paris.} \
     when  *     {{$userName} est all\u00E9 \u00E0 Paris.}",
    "{{$when :datetime skeleton=MMMMd}}",
    // Edited this from testMessageFormatDateTimeSkeleton() -- unquoted literals can't contain spaces
    "{{$when :datetime skeleton=|(   yMMMMd   )|}}",
    "{Expiration: {$when :datetime skeleton=yMMM}!}",
    "{Hello {$user}, today is {$today :datetime datestyle=long}.}",
    // Edited this from testMessageFormatDateTimeSkeleton() -- unquoted literals can't contain parentheses or single quotation marks
    "{{$when :datetime pattern=|('::'yMMMMd)|}}",
    // From CustomFormatterMessageRefTest.java
    "match {$gcase :select} when genitive {Firefoxin} when * {Firefox}",
    // From CustomFormatterPersonTest.java
    "{Hello {$name :person formality=formal length=medium}}",
    0
};

/*
  These tests are mostly from the test suite created for the JavaScript implementation of MessageFormat v2:
  <p>Original JSON file
  <a href="https://github.com/messageformat/messageformat/blob/master/packages/mf2-messageformat/src/__fixtures/test-messages.json">here</a>.</p>
  Some have been modified or added to reflect syntax changes that post-date the JSON file.

 */
UnicodeString jsonTestCasesValid[] = {
    "{hello}",
    "{hello {|world|}}",
    "{hello {||}}",
    "{hello {$place}}",
    "{{$one} and {$two}}",
    "{{$one} et {$two}}",
    "{hello {|4.2| :number}}",
    "{hello {|foo| :number}}",
    "{hello {|foo| :number   }}",
    "{hello {:number}}",
    "{hello {|4.2| :number minimumFractionDigits=2}}",
    "{hello {|4.2| :number minimumFractionDigits = 2}}",
    "{hello {|4.2| :number minimumFractionDigits= 2}}",
    "{hello {|4.2| :number minimumFractionDigits =2}}",
    "{hello {|4.2| :number minimumFractionDigits=2  }}",
    "{hello {|4.2| :number minimumFractionDigits=2 bar=3}}",
    "{hello {|4.2| :number minimumFractionDigits=2 bar=3  }}",
    "{hello {|4.2| :number minimumFractionDigits=|2|}}",
    "{hello {|4.2| :number minimumFractionDigits=$foo}}",
    "let $foo = {|bar|} {bar {$foo}}",
    "let $foo = {$bar} {bar {$foo}}",
    "let $foo = {$bar :number} {bar {$foo}}",
    "let $foo = {$bar :number minimumFractionDigits=2} {bar {$foo}}",
    "let $foo = {$bar :number minimumFractionDigits=foo} {bar {$foo}}",
    "let $foo = {$bar :number} {bar {$foo}}",
    "let $foo = {$bar} let $bar = {$baz} {bar {$foo}}",
    "match {$foo} when |1| {one} when * {other}",
    "match {$foo :select} when |1| {one} when * {other}",
    "match {$foo :plural} when 1 {one} when * {other}",
    "match {$foo} when 1 {one} when * {other}",
    "match {$foo :plural} when 1 {one} when * {other}",
    "match {$foo} when one {one} when * {other}",
    "match {$foo :plural} when one {one} when * {other}",
    "match {$foo} when 1 {=1} when one {one} when * {other}",
    "match {$foo :plural} when 1 {=1} when one {one} when * {other}",
    "match {$foo} when one {one} when 1 {=1} when * {other}",
    "match {$foo :plural} when one {one} when 1 {=1} when * {other}",
    "match {$foo} {$bar} when one one {one one} when one * {one other} when * * {other}",
    "match {$foo :plural} {$bar :plural} when one one {one one} when one * {one other} when * * {other}",
    "let $foo = {$bar} match {$foo} when one {one} when * {other}",
    "let $foo = {$bar} match {$foo :plural} when one {one} when * {other}",
    "let $foo = {$bar} match {$foo} when one {one} when * {other}",
    "let $foo = {$bar} match {$foo :plural} when one {one} when * {other}",
    "let $bar = {$none} match {$foo} when one {one} when * {{$bar}}",
    "let $bar = {$none} match {$foo :plural} when one {one} when * {{$bar}}",
    "let $bar = {$none} match {$foo} when one {one} when * {{$bar}}",
    "let $bar = {$none :plural} match {$foo} when one {one} when * {{$bar}}",
    "{{+tag}}", // Modified next few patterns to reflect lack of special markup syntax
    "{{-tag}}",
    // Modified next few patterns to reflect lack of special markup syntax
    "match {+foo} when * {foo}",
    "{{|content| +tag}}",
    "{{|content| -tag}}",
    "{{|content| +tag} {|content| -tag}}",
    "{content -tag}",
    "{{+tag foo=bar}}",
    "{{+tag foo=|foo| bar=$bar}}",
    "{{-tag foo=bar}}",
    "{content {|foo| +markup}}",
    "{}",
    // tests for reserved syntax
    "{hello {|4.2| @number}}",
    "{hello {|4.2| @n|um|ber}}",
    "{hello {|4.2| &num|be|r}}",
    "{hello {|4.2| ?num|be||r|s}}",
    "{hello {|foo| !number}}",
    "{hello {|foo| *number}}",
    "{hello {#number}}",
    "match {$foo !select} when |1| {one} when * {other}",
    "match {$foo ^select} when |1| {one} when * {other}",
    "{{<tag}}",
    "let $bar = {$none ~plural} match {$foo} when * {{$bar}}",
    // tests for reserved syntax with escaped chars
    "{hello {|4.2| @num\\\\ber}}",
    "{hello {|4.2| @num\\{be\\|r}}",
    "{hello {|4.2| @num\\\\\\}ber}}",
    // tests for reserved syntax
    "{hello {|4.2| @}}",
    "{hello {|4.2| #}}",
    "{hello {|4.2| *}}",
    "{hello {|4.2| ^abc|123||5|\\\\}}",
    "{hello {|4.2| ^ abc|123||5|\\\\}}",
    "{hello {|4.2| ^ abc|123||5|\\\\ \\|def |3.14||2|}}",
    // tests for reserved syntax with trailing whitespace
    "{hello {|4.2| ? }}",
    "{hello {|4.2| @xyzz }}",
    "{hello {|4.2| !xyzz   }}",
    "{hello {$foo ~xyzz }}",
    "{hello {$x   <xyzz   }}",
    "{{@xyzz }}",
    "{{  !xyzz   }}",
    "{{~xyzz }}",
    "{{ <xyzz   }}",
    // tests for reserved syntax with space-separated sequences
    "{hello {|4.2| @xy z z }}",
    "{hello {|4.2| *num \\\\ b er}}",
    "{hello {|4.2| %num \\\\ b |3.14| r    }}",
    "{hello {|4.2|    #num xx \\\\ b |3.14| r  }}",
    "{hello {$foo    #num x \\\\ abcde |3.14| r  }}",
    "{hello {$foo    >num x \\\\ abcde |aaa||3.14||42| r  }}",
    "{hello {$foo    >num x \\\\ abcde |aaa||3.14| |42| r  }}",
    // tests for escape sequences in literals
    "{{|hel\\\\lo|}}",
    "{{|hel\\|lo|}}",
    "{{|hel\\|\\\\lo|}}",
    // tests for text escape sequences
    "{hel\\{lo}",
    "{hel\\}lo}",
    "{hel\\\\lo}",
    "{hel\\{\\\\lo}",
    "{hel\\{\\}lo}",
    // tests for ':' in unquoted literals
    "match {$foo} when o:ne {one} when * {other}",
    "match {$foo} when one: {one} when * {other}",
    "let $foo = {$bar :fun option=a:b} {bar {$foo}}",
    "let $foo = {$bar :fun option=a:b:c} {bar {$foo}}",
    // tests for newlines in literals and text
    "{hello {|wo\nrld|}}",
    "{hello wo\nrld}",
    // multiple scrutinees, with or without whitespace
    "match {$foo} {$bar} when one * {one} when * * {other}",
    "match {$foo} {$bar}when one * {one} when * * {other}",
    "match {$foo}{$bar} when one * {one} when * * {other}",
    "match {$foo}{$bar}when one * {one} when * * {other}",
    "match{$foo} {$bar} when one * {one} when * * {other}",
    "match{$foo} {$bar}when one * {one} when * * {other}",
    "match{$foo}{$bar} when one * {one} when * * {other}",
    "match{$foo}{$bar}when one * {one} when * * {other}",
    // multiple variants, with or without whitespace
    "match {$foo} {$bar} when one * {one} when * * {other}",
    "match {$foo} {$bar} when one * {one}when * * {other}",
    "match {$foo} {$bar}when one * {one} when * * {other}",
    "match {$foo} {$bar}when one * {one}when * * {other}",
    // one or multiple keys, with or without whitespace before pattern
    "match {$foo} {$bar} when one *{one} when * * {foo}",
    "match {$foo} {$bar} when one * {one} when * * {foo}",
    "match {$foo} {$bar} when one *  {one} when * * {foo}",
    // zero, one or multiple options, with or without whitespace before '}'
    "{{:foo}}",
    "{{:foo }}",
    "{{:foo   }}",
    "{{:foo k=v}}",
    "{{:foo k=v   }}",
    "{{:foo k1=v1   k2=v2}}",
    "{{:foo k1=v1   k2=v2   }}",
    // literals or variables followed by space, with or without an annotation following
    "{{|3.14| }}",
    "{{|3.14|    }}",
    "{{|3.14|    :foo}}",
    "{{|3.14|    :foo   }}",
    "{{$bar }}",
    "{{$bar    }}",
    "{{$bar    :foo}}",
    "{{$bar    :foo   }}",
    // Trailing whitespace at end of message should be accepted
    "match {$foo} {$bar} when one * {one} when * * {other}   ",
    "{hi} ",
    // Variable names can contain '-' or ':'
    "{{$bar:foo}}",
    "{{$bar-foo}}",
    // Name shadowing is allowed
    "let $foo = {|hello|} let $foo = {$foo} {{$foo}}",
    // Unquoted literal -- should work
    "{good {placeholder}}",
    0
};

// From CustomFormatterPersonTest.java
UnicodeString complexMessage = "\
    let $hostName = {$host :person length=long}\n\
    let $guestName = {$guest :person length=long}\n\
    let $guestsOther = {$guestCount :number offset=1}\n\
    \n\
    match {$hostGender :gender} {$guestCount :plural}\n\
    when female 0 {{$hostName} does not give a party.}\n\
    when female 1 {{$hostName} invites {$guestName} to her party.}\n\
    when female 2 {{$hostName} invites {$guestName} and one other person to her party.}\n\
    when female * {{$hostName} invites {$guestName} and {$guestsOther} other people to her party.}\n\
    \n\
    when male 0 {{$hostName} does not give a party.}\n\
    when male 1 {{$hostName} invites {$guestName} to his party.}\n\
    when male 2 {{$hostName} invites {$guestName} and one other person to his party.}\n\
    when male * {{$hostName} invites {$guestName} and {$guestsOther} other people to his party.}\n\
    \n\
    when * 0 {{$hostName} does not give a party.}\n\
    when * 1 {{$hostName} invites {$guestName} to their party.}\n\
    when * 2 {{$hostName} invites {$guestName} and one other person to their party.}\n\
    when * * {{$hostName} invites {$guestName} and {$guestsOther} other people to their party.}\n";

void
TestMessageFormat2::runIndexedTest(int32_t index, UBool exec,
                                  const char* &name, char* /*par*/) {
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testDataModelErrors);
    TESTCASE_AUTO(testAPI);
    TESTCASE_AUTO(testAPISimple);
    TESTCASE_AUTO(testValidJsonPatterns);
    TESTCASE_AUTO(testValidPatterns);
    TESTCASE_AUTO(testComplexMessage);
    TESTCASE_AUTO(testInvalidPatterns);
    TESTCASE_AUTO_END;
}

// Example for design doc
void TestMessageFormat2::testAPISimple() {
  IcuTestErrorCode errorCode1(*this, "testAPI");
  UErrorCode errorCode = (UErrorCode) errorCode1;
  UParseError parseError;
  Locale locale = "en";
// Null checks and error checks elided
  MessageFormatter::Builder* builder = MessageFormatter::builder(errorCode);
  /* MessageFormatter* mf = */ builder
        ->setPattern(u"{Hello, {$userName}!}", errorCode)
        .build(parseError, errorCode);

// TODO formatting calls
  delete builder;
  builder = MessageFormatter::builder(errorCode);

  /* mf = */ builder
        ->setPattern("{Today is {$today :date skeleton=yMMMdEEE}.}", errorCode)
        .setLocale(locale)
        .build(parseError, errorCode);

  delete builder;
  builder = MessageFormatter::builder(errorCode);

  /* mf = */ builder
        ->setPattern("match {$photoCount :plural} {$userGender :select}\n\
                     when 1 masculine {{$userName} added a new photo to his album.}\n \
                     when 1 feminine {{$userName} added a new photo to her album.}\n \
                     when 1 * {{$userName} added a new photo to their album.}\n \
                     when * masculine {{$userName} added {$photoCount} photos to his album.}\n \
                     when * feminine {{$userName} added {$photoCount} photos to her album.}\n \
                     when * * {{$userName} added {$photoCount} photos to their album.}",
                errorCode)
        .setLocale(locale)
        .build(parseError, errorCode);
}


void TestMessageFormat2::testAPI() {
  IcuTestErrorCode errorCode1(*this, "testAPI");
  UErrorCode errorCode = (UErrorCode) errorCode1;
  UParseError parseError;
  Locale locale = "en";
  LocalPointer<MessageFormatter::Builder> builder(MessageFormatter::builder(errorCode));
  if (U_FAILURE(errorCode)) {
    return;
  }

  UnicodeString pattern = u"{Hello, {$userName}!}";
  LocalPointer<MessageFormatter> mf(builder
        ->setPattern(pattern, errorCode)
        .build(parseError, errorCode));
  if (U_FAILURE(errorCode)) {
    return;
  }

  LocalPointer<Hashtable> arguments(new Hashtable(uhash_compareUnicodeString, nullptr, errorCode));
  if (U_FAILURE(errorCode)) {
    return;
  }
  UnicodeString value = "John";
  arguments->put("userName", &value, errorCode);
  UnicodeString expected = "Hello, John!";
  UnicodeString result;
  mf->formatToString(*arguments, errorCode, result);
  if (result != expected) {
    logln("Expected output: " + expected + "\nGot output: " + result);
    errorCode = U_MESSAGE_PARSE_ERROR;
    return;
  }
  if (U_FAILURE(errorCode)) {
        dataerrln(result);
        logln(UnicodeString("TestMessageFormat2::testAPI failed test with pattern: " + pattern));
        return;
  }

  builder.adoptInstead(MessageFormatter::builder(errorCode));

  mf.adoptInstead(builder
        ->setPattern("{Today is {$today :date skeleton=yMMMdEEE}.}", errorCode)
        .setLocale(locale)
        .build(parseError, errorCode));
  /*
result = mf.formatToString(Map.of("today", new Date()));
// "Today is Tue, Aug 16, 2022."
System.out.println(result);
  */

  builder.adoptInstead(MessageFormatter::builder(errorCode));

  // Pattern matching
  pattern = "match {$photoCount} {$userGender}\n\
                     when 1 masculine {{$userName} added a new photo to his album.}\n \
                     when 1 feminine {{$userName} added a new photo to her album.}\n \
                     when 1 * {{$userName} added a new photo to their album.}\n \
                     when * masculine {{$userName} added {$photoCount} photos to his album.}\n \
                     when * feminine {{$userName} added {$photoCount} photos to her album.}\n \
                     when * * {{$userName} added {$photoCount} photos to their album.}";

  mf.adoptInstead(builder
        ->setPattern(pattern, errorCode)
        .setLocale(locale)
        .build(parseError, errorCode));

  arguments->removeAll();
  UnicodeString value1 = "12";
  arguments->put("photoCount", &value1, errorCode);
  UnicodeString value2 = "feminine";
  UnicodeString value3 = "Maria";
  arguments->put("userGender", &value2, errorCode);
  arguments->put("userName", &value3, errorCode);

  result.remove();
  mf->formatToString(*arguments, errorCode, result);

  if (U_FAILURE(errorCode)) {
      dataerrln(pattern);
      logln(UnicodeString("TestMessageFormat2::testAPI failed test with error code ") + (int32_t)errorCode);
      return;
  }

  expected = "Maria added 12 photos to her album.";

  if (result != expected) {
      dataerrln(pattern);
      logln("Expected output: " + expected + "\nGot output: " + result);
      errorCode = U_MESSAGE_PARSE_ERROR;
      return;
  }

  arguments->removeAll();
  value1 = "1";
  arguments->put("photoCount", &value1, errorCode);
  value2 = "feminine";
  value3 = "Maria";
  arguments->put("userGender", &value2, errorCode);
  arguments->put("userName", &value3, errorCode);

  // Built-in functions
  pattern = "match {$photoCount :plural} {$userGender :select}\n\
                     when 1 masculine {{$userName} added a new photo to his album.}\n \
                     when 1 feminine {{$userName} added a new photo to her album.}\n \
                     when 1 * {{$userName} added a new photo to their album.}\n \
                     when * masculine {{$userName} added {$photoCount} photos to his album.}\n \
                     when * feminine {{$userName} added {$photoCount} photos to her album.}\n \
                     when * * {{$userName} added {$photoCount} photos to their album.}";

  expected = "Maria added a new photo to her album.";

  builder->setPattern(pattern, errorCode);
  mf.adoptInstead(builder->build(parseError, errorCode));

  result.remove();
  mf->formatToString(*arguments, errorCode, result);

  if (U_FAILURE(errorCode)) {
      dataerrln(pattern);
      logln(UnicodeString("TestMessageFormat2::testAPI failed test with error code ") + (int32_t)errorCode);
      return;
  }

  if (result != expected) {
      dataerrln(pattern);
      logln("Expected output: " + expected + "\nGot output: " + result);
      errorCode = U_MESSAGE_PARSE_ERROR;
      return;
  }

                  /*
​​Using custom functions:
final Mf2FunctionRegistry functionRegistry = Mf2FunctionRegistry.builder()
        .setFormatter("person", new PersonNameFormatterFactory())
        .setDefaultFormatterNameForType(Person.class, "person")
        .build();

Person who = new Person("Mr.", "John", "Doe");

MessageFormatter mf;
String result;
                  */

   builder.adoptInstead(MessageFormatter::builder(errorCode));

// This fails, because we did not provide a function registry:
   mf.adoptInstead(builder
        ->setPattern("{Hello {$name :person formality=informal}}", errorCode)
        .setLocale(locale)
        .build(parseError, errorCode));
   if (U_SUCCESS(errorCode)) {
     // Should have failed
     errorCode = U_MESSAGE_PARSE_ERROR;
   } else {
     errorCode1.reset();
   }
                   //result = mf.formatToString(Map.of("name", who));

                  /*
// The setClassToFormatterMapping tells the formatter that the class Person is to be handled
// by the :person function, so this works. Similar to MessageFormat.
mf = MessageFormatter.builder()
        .setFunctionRegistry(functionRegistry)
        .setPattern("{Hello {$name}}")
        .setLocale(locale)
        .build();
result = mf.formatToString(Map.of("name", who));
// => "Hello John"

mf = MessageFormatter.builder()
        .setFunctionRegistry(functionRegistry)
        .setPattern("{Hello {$name :person formality=informal}}")
        .setLocale(locale)
        .build();
result = mf.formatToString(Map.of("name", who));
// => "Hello John"

mf = MessageFormatter.builder()
        .setFunctionRegistry(functionRegistry)
        .setPattern("{Hello {$name :person formality=formal}}")
        .setLocale(locale)
        .build();
result = mf.formatToString(Map.of("name", who));
// => "Hello Mr. Doe"

mf = MessageFormatter.builder()
        .setFunctionRegistry(functionRegistry)
        .setPattern("{Hello {$name :person formality=formal length=long}}")
        .setLocale(locale)
        .build();
result = mf.formatToString(Map.of("name", who));
// => "Hello Mr. John Doe"
*/
}

void TestMessageFormat2::testMessageFormatter(const UnicodeString& s, UParseError& parseError, UErrorCode& errorCode) {
    LocalPointer<MessageFormatter::Builder> builder(MessageFormatter::builder(errorCode));
    if (U_SUCCESS(errorCode)) {
      builder->setPattern(s, errorCode);
      LocalPointer<MessageFormatter> mf(builder->build(parseError, errorCode));
      if (U_SUCCESS(errorCode)) {
        // Roundtrip test
        const UnicodeString& normalized = mf->getNormalizedPattern();
        UnicodeString serialized;
        mf->getPattern(serialized);
        if (normalized != serialized) {
          logln("Expected output: " + normalized + "\nGot output: " + serialized);
          errorCode = U_MESSAGE_PARSE_ERROR;
        }
      }
    }
}

/*
 Tests a single pattern, which is expected to be valid.

 `s`: The pattern string.
 `i`: Test number (only used for diagnostic output)
 `testName`: String describing the test (only used for diagnostic output)
*/
void
TestMessageFormat2::testPattern(const UnicodeString& s, uint32_t i, const char* testName) {
    UParseError parseError;
    IcuTestErrorCode errorCode(*this, testName);

    testMessageFormatter(s, parseError, errorCode);

    if (U_FAILURE(errorCode)) {
        dataerrln(s);
        dataerrln("TestMessageFormat2::%s #%d - %s", testName, i, u_errorName(errorCode));
        dataerrln("TestMessageFormat2::%s #%d - %d %d", testName, i, parseError.line, parseError.offset);
        logln(UnicodeString("TestMessageFormat2::" + UnicodeString(testName) + " failed test #") + ((int32_t) i) + UnicodeString(" with error code ")+(int32_t)errorCode);
    }
}

/*
 Tests a fixed-size array of patterns, which are expected to be valid.

 `patterns`: The patterns.
 `testName`: String describing the test (only used for diagnostic output)
*/
template<size_t N>
void TestMessageFormat2::testPatterns(const UnicodeString (&patterns)[N], const char* testName) {
    for (uint32_t i = 0; i < N - 1; i++) {
        testPattern(patterns[i], i, testName);
    }
}

void TestMessageFormat2::testValidPatterns() {
    testPatterns(validTestCases, "testValidPatterns");
}

void TestMessageFormat2::testValidJsonPatterns() {
    testPatterns(jsonTestCasesValid, "testValidJsonPatterns");
}

/*
 Tests a single pattern, which is expected to be invalid.

 `testNum`: Test number (only used for diagnostic output)
 `s`: The pattern string.

 The error is assumed to be on line 0, offset `s.length()`.
*/
void TestMessageFormat2::testInvalidPattern(uint32_t testNum, const UnicodeString& s) {
    testInvalidPattern(testNum, s, s.length(), 0);
}

/*
 Tests a single pattern, which is expected to be invalid.

 `testNum`: Test number (only used for diagnostic output)
 `s`: The pattern string.

 The error is assumed to be on line 0, offset `expectedErrorOffset`.
*/
void TestMessageFormat2::testInvalidPattern(uint32_t testNum, const UnicodeString& s, uint32_t expectedErrorOffset) {
    testInvalidPattern(testNum, s, expectedErrorOffset, 0);
}

/*
 Tests a single pattern, which is expected to be invalid.

 `testNum`: Test number (only used for diagnostic output)
 `s`: The pattern string.
 `expectedErrorOffset`: The expected character offset for the parse error.

 The error is assumed to be on line `expectedErrorLine`, offset `expectedErrorOffset`.
*/
void TestMessageFormat2::testInvalidPattern(uint32_t testNum, const UnicodeString& s, uint32_t expectedErrorOffset, uint32_t expectedErrorLine) {
    UParseError parseError;
    IcuTestErrorCode errorCode(*this, "testInvalidPattern");

    testMessageFormatter(s, parseError, errorCode);

    if (!U_FAILURE(errorCode)) {
        dataerrln("TestMessageFormat2::testInvalidPattern #%d - expected test to fail, but it passed", testNum);
        logln(UnicodeString("TestMessageFormat2::testInvalidPattern failed test ") + s + UnicodeString(" with error code ")+(int32_t)errorCode);
        return;
    } else if (errorCode != U_MESSAGE_PARSE_ERROR) {
        dataerrln("TestMessageFormat2::testInvalidPattern #%d - expected test to fail with U_MESSAGE_PARSE_ERROR, but it failed with a different error", testNum);
        logln(UnicodeString("TestMessageFormat2::testInvalidPattern failed test ") + s + UnicodeString(" with error code ")+(int32_t)errorCode);
        return;

    } else {
        // Check the line and character numbers
        if (parseError.line != ((int32_t) expectedErrorLine) || parseError.offset != ((int32_t) expectedErrorOffset)) {
            dataerrln("TestMessageFormat2::testInvalidPattern #%d - wrong line or character offset in parse error; expected (line %d, offset %d), got (line %d, offset %d)",
                      testNum, expectedErrorLine, expectedErrorOffset, parseError.line, parseError.offset);
            logln(UnicodeString("TestMessageFormat2::testInvalidPattern failed #") + ((int32_t) testNum) + UnicodeString(" with error code ")+(int32_t)errorCode+" by returning the wrong line number or offset in the parse error");
        } else {
            errorCode.reset();
        }
    }
}

/*
 Tests a single pattern, which is expected to cause the formatter to
 emit a data model error, resolution error, selection error, or
 formatting error

 `testNum`: Test number (only used for diagnostic output)
 `s`: The pattern string.
 `expectedErrorCode`: the error code expected to be returned by the formatter

 TODO: For now, the line and character numbers are not checked
*/
void TestMessageFormat2::testSemanticallyInvalidPattern(uint32_t testNum, const UnicodeString& s, UErrorCode expectedErrorCode) {
    UParseError parseError;
    IcuTestErrorCode errorCode(*this, "testInvalidPattern");

    testMessageFormatter(s, parseError, errorCode);

    if (!U_FAILURE(errorCode)) {
        dataerrln("TestMessageFormat2::testSemanticallyInvalidPattern #%d - expected test to fail, but it passed", testNum);
        logln(UnicodeString("TestMessageFormat2::testSemanticallyInvalidPattern failed test ") + s + UnicodeString(" with error code ")+(int32_t)errorCode);
        return;
    } else if (errorCode != expectedErrorCode) {
        dataerrln("TestMessageFormat2::testInvalidPattern #%d - expected test to fail with U_MESSAGE_PARSE_ERROR, but it failed with a different error", testNum);
        logln(UnicodeString("TestMessageFormat2::testInvalidPattern failed test ") + s + UnicodeString(" with error code ")+(int32_t)errorCode);
        return;

    } else {
        errorCode.reset();
    }
}

void TestMessageFormat2::testDataModelErrors() {
    uint32_t i = 0;

    // The following tests are syntactically valid but should trigger a data model error
    
    // Examples taken from https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md

    // Variant key mismatch
    testSemanticallyInvalidPattern(++i, "match {$foo} {$bar} when one{one}", U_VARIANT_KEY_MISMATCH);
    testSemanticallyInvalidPattern(++i, "match {$foo} {$bar} when one {one}", U_VARIANT_KEY_MISMATCH);
    testSemanticallyInvalidPattern(++i, "match {$foo} {$bar} when one  {one}", U_VARIANT_KEY_MISMATCH);
 
    testSemanticallyInvalidPattern(++i, "match {$foo} when * * {foo}", U_VARIANT_KEY_MISMATCH);
    testSemanticallyInvalidPattern(++i, "match {$one}\n\
                             when 1 2 {Too many}\n\
                             when * {Otherwise}", U_VARIANT_KEY_MISMATCH);
    testSemanticallyInvalidPattern(++i, "match {$one} {$two}\n\
                             when 1 2 {Two keys}\n\
                             when * {Missing a key}\n\
                             when * * {Otherwise}", U_VARIANT_KEY_MISMATCH);

    /*
      TODO: add tests with
      0 variants and 0 selectors
      0 variants and 1 selector
      1 variant and 0 selectors
     */

    // Non-exhaustive patterns
    testSemanticallyInvalidPattern(++i, "match {$one}\n\
                                         when 1 {Value is one}\n\
                                         when 2 {Value is two}\n", U_NONEXHAUSTIVE_PATTERN);
    testSemanticallyInvalidPattern(++i, "match {$one} {$two}\n\
                                         when 1 * {First is one}\n\
                                         when * 1 {Second is one}\n", U_NONEXHAUSTIVE_PATTERN);

    /* TODO:
       Duplicate option names
       Unresolved variables
       Unknown functions
       Selector errors
       Formatting errors
          e.g. calls to custom functions with constraints on their arguments;
          handling these errors properly
     */
}

void TestMessageFormat2::testInvalidPatterns() {
/*
  These tests are mostly from the test suite created for the JavaScript implementation of MessageFormat v2:
  <p>Original JSON file
  <a href="https://github.com/messageformat/messageformat/blob/master/packages/mf2-messageformat/src/__fixtures/test-messages.json">here</a>.</p>
  Some have been modified or added to reflect syntax changes that post-date the JSON file.

 */
    uint32_t i = 0;

    // Unexpected end of input
    testInvalidPattern(++i, "let    ");
    testInvalidPattern(++i, "le");
    testInvalidPattern(++i, "let $foo");
    testInvalidPattern(++i, "let $foo =    ");
    testInvalidPattern(++i, "{{:fszzz");
    testInvalidPattern(++i, "{{:fszzz   ");
    testInvalidPattern(++i, "match {$foo} when |xyz");
    testInvalidPattern(++i, "{{:f aaa");
    testInvalidPattern(++i, "{missing end brace");
    testInvalidPattern(++i, "{missing end {$brace");

    // Error should be reported at character 0, not end of input
    testInvalidPattern(++i, "}{|xyz|", 0);
    testInvalidPattern(++i, "}", 0);

    // @xyz is a valid annotation (`reserved`) so the error should be at the end of input
    testInvalidPattern(++i, "{{@xyz");
    // Backslash followed by non-backslash followed by a '{' -- this should be an error
    // immediately after the first backslash
    testInvalidPattern(++i, "{{@\\y{}}", 4);

    // Reserved chars followed by a '|' that doesn't begin a valid literal -- this should be
    // an error at the first invalid char in the literal
    testInvalidPattern(++i, "{{@abc|\\z}}", 8);

    // Same pattern, but with a valid reserved-char following the erroneous reserved-escape
    // -- the offset should be the same as with the previous one
    testInvalidPattern(++i, "{{@\\y{p}}", 4);
    // Erroneous literal inside a reserved string -- the error should be at the first
    // erroneous literal char
    testInvalidPattern(++i, "{{@ab|\\z|cd}}", 7);

    // tests for reserved syntax with bad escaped chars
    // Single backslash - not allowed
    testInvalidPattern(++i, "{hello {|4.2| @num\\ber}}", 19);
    // Unescaped '{' -- not allowed
    testInvalidPattern(++i, "{hello {|4.2| @num{be\\|r}}", 18);
    // Unescaped '}' -- will be interpreted as the end of the reserved
    // string, and the error will be reported at the index of '|', which is
    // when the parser determines that "\|" isn't a valid text-escape
    testInvalidPattern(++i, "{hello {|4.2| @num}be\\|r}}", 22);
    // Unescaped '|' -- will be interpreted as the beginning of a literal
    // Error at end of input
    testInvalidPattern(++i, "{hello {|4.2| @num\\{be|r}}", 26);

    // Invalid escape sequence in a `text` -- the error should be at the character
    // following the backslash
    testInvalidPattern(++i, "{a\\qbc", 3);

    // Missing space after `when` -- the error should be immediately after the
    // `when` (not the error in the pattern)
    testInvalidPattern(++i, "match {|y|} when|y| {|||}", 16);

    // Missing spaces betwen keys in `when`-clause
    testInvalidPattern(++i, "match {|y|} when |foo|bar {a}", 22);
    testInvalidPattern(++i, "match {|y|} when |quux| |foo|bar {a}", 29);
    testInvalidPattern(++i, "match {|y|} when |quux| |foo||bar| {a}", 29);

    // Error parsing the first key -- the error should be there, not in the
    // also-erroneous third key
    testInvalidPattern(++i, "match {|y|} when |\\q| * @{! {z}", 19);

    // Error parsing the second key -- the error should be there, not in the
    // also-erroneous third key
    testInvalidPattern(++i, "match {|y|} when * @{! {z} |\\q|", 19);

    // Error parsing the last key -- the error should be there, not in the erroneous
    // pattern
    testInvalidPattern(++i, "match {|y|} when * |\\q| {\\z}", 21);

    // Selectors not starting with `match` -- error should be on character 1,
    // not the later erroneous key
    testInvalidPattern(++i, "m {|y|} when @{! {z}", 1);

    // Non-expression as scrutinee in pattern -- error should be at the first
    // non-expression, not the later non-expression
    testInvalidPattern(++i, "match {|y|} {\\|} {@} when * * * {a}", 13);

    // Non-key in variant -- error should be there, not in the next erroneous
    // variant
    testInvalidPattern(++i, "match {|y|} when $foo * {a} when * :bar {b}", 17);


    // Error should be within the first erroneous `text` or expression
    testInvalidPattern(++i, "{ foo {|bar|} \\q baz  ", 15);

    // ':' has to be followed by a function name -- the error should be at the first
    // whitespace character
    testInvalidPattern(++i, "{{:    }}", 3);

    // Expression not starting with a '{'
    testInvalidPattern(++i, "let $x = }|foo|}", 9);

    // Error should be at the first declaration not starting with a `let`
    testInvalidPattern(++i, "let $x = {|foo|} l $y = {|bar|} let $z {|quux|}", 18);

    // Missing '=' in `let` declaration
    testInvalidPattern(++i, "let $bar {|foo|} {{$bar}}", 9);

    // LHS of declaration doesn't start with a '$'
    testInvalidPattern(++i, "let bar = {|foo|} {{$bar}}", 4);

    // `let` RHS isn't an expression
    testInvalidPattern(++i, "let $bar = |foo| {{$bar}}", 11);

    // Non-expression
    testInvalidPattern(++i, "no braces", 0);
    testInvalidPattern(++i, "no braces {$foo}", 0);

    // Trailing characters that are not whitespace
    testInvalidPattern(++i, "{extra} content", 8);
    testInvalidPattern(++i, "match {|x|} when * {foo} extra", 25);

    // Empty expression
    testInvalidPattern(++i, "{empty { }}", 9);
    testInvalidPattern(++i, "match {} when * {foo}", 7);

    // ':' not preceding a function name
    testInvalidPattern(++i, "{bad {:}}", 7);

    // Missing '=' after option name
    testInvalidPattern(++i, "{no-equal {|42| :number m }}", 26);
    testInvalidPattern(++i, "{no-equal {|42| :number minimumFractionDigits 2}}", 46);
    testInvalidPattern(++i, "{bad {:placeholder option value}}", 26);

    // Extra '=' after option value
    testInvalidPattern(++i, "{hello {|4.2| :number min=2=3}}", 27),
    testInvalidPattern(++i, "{hello {|4.2| :number min=2max=3}}", 30),
    // Missing whitespace between valid options
    testInvalidPattern(++i, "{hello {|4.2| :number min=|a|max=3}}", 29),
    // Ill-formed RHS of option -- the error should be within the RHS,
    // not after parsing options
    testInvalidPattern(++i, "{hello {|4.2| :number min=|\\a|}}", 28),


    // Junk after annotation
    testInvalidPattern(++i, "{no-equal {|42| :number   {}", 26);

    // Missing RHS of option
    testInvalidPattern(++i, "{bad {:placeholder option=}}", 26);
    testInvalidPattern(++i, "{bad {:placeholder option}}", 25);

    // Annotation is not a function or reserved text
    testInvalidPattern(++i, "{bad {$placeholder option}}", 19);
    testInvalidPattern(++i, "{no {$placeholder end}", 18);

    // Missing whitespace before key in variant
    testInvalidPattern(++i, "match {|foo|} when*{foo}", 18);
    // Missing expression in selectors
    testInvalidPattern(++i, "match when * {foo}", 6);
    // Non-expression in selectors
    testInvalidPattern(++i, "match |x| when * {foo}", 6);

    // Missing RHS in variant
    testInvalidPattern(++i, "match {|x|} when * foo");

    // Text may include newlines; check that the missing closing '}' is
    // reported on the correct line
    testInvalidPattern(++i, "{hello wo\nrld", 3, 1);
    testInvalidPattern(++i, "{hello wo\nr\nl\ndddd", 4, 3);
    // Offset for end-of-input should be 0 here because the line begins
    // after the '\n', but there is no character after the '\n'
    testInvalidPattern(++i, "{hello wo\nr\nl\n", 0, 3);

    // Literals may include newlines; check that the missing closing '|' is
    // reported on the correct line
    testInvalidPattern(++i, "{hello {|wo\nrld}", 4, 1);
    testInvalidPattern(++i, "{hello {|wo\nr\nl\ndddd}", 5, 3);
    // Offset for end-of-input should be 0 here because the line begins
    // after the '\n', but there is no character after the '\n'
    testInvalidPattern(++i, "{hello {|wo\nr\nl\n", 0, 3);

    // Variable names can't start with a : or -
    testInvalidPattern(++i, "{{$:abc}}", 3);
    testInvalidPattern(++i, "{{$-abc}}", 3);

    // Missing space before annotation
    // Note that {{$bar:foo}} and {{$bar-foo}} are valid,
    // because variable names can contain a ':' or a '-'
    testInvalidPattern(++i, "{{$bar+foo}}", 6);
    testInvalidPattern(++i, "{{|3.14|:foo}}", 8);
    testInvalidPattern(++i, "{{|3.14|-foo}}", 8);
    testInvalidPattern(++i, "{{|3.14|+foo}}", 8);

    // Unquoted literals can't begin with a ':'
    testInvalidPattern(++i, "let $foo = {$bar} match {$foo} when :one {one} when * {other}", 36);
    testInvalidPattern(++i, "let $foo = {$bar :fun option=:a} {bar {$foo}}", 29);

}

void TestMessageFormat2::testComplexMessage() {
    testPattern(complexMessage, 0, "testComplexMessage");
}

#endif /* #if !UCONFIG_NO_FORMATTING */
