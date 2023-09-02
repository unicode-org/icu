// © 2016 and later: Unicode, Inc. and others.

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2.h"
#include "messageformat2test.h"

using namespace icu::message2;

/*
  TODO: Tests need to be unified in a single format that
  both ICU4C and ICU4J can use, rather than being embedded in code.

  Tests are included in their current state to give a sense of
  how much test coverage has been achieved. Most of the testing is
  of the parser/serializer; the formatter needs to be tested more
  thoroughly.
*/

/*
Tests reflect the syntax specified in

  https://github.com/unicode-org/message-format-wg/commits/main/spec/message.abnf

as of the following commit from 2023-05-09:
  https://github.com/unicode-org/message-format-wg/commit/194f6efcec5bf396df36a19bd6fa78d1fa2e0867

*/

/*
  Transcribed from https://github.com/messageformat/messageformat/blob/main/packages/mf2-messageformat/src/__fixtures/test-messages.json
https://github.com/messageformat/messageformat/commit/6656c95d66414da29a332a6f5bbb225371f2b9a3

*/
void TestMessageFormat2::jsonTests(IcuTestErrorCode& errorCode) {
    LocalPointer<TestCase::Builder> testBuilder(TestCase::builder(errorCode));
    testBuilder->setName("jsonTests");

    LocalPointer<TestCase> test(testBuilder->setPattern("{hello}")
                                .setExpected("hello")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{hello {|world|}}")
                                .setExpected("hello world")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{hello {||}}")
                                .setExpected("hello ")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{hello {$place}}")
                                .setExpected("hello world")
                                .setArgument("place", "world", errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{hello {$place:-.}}")
                                .setExpected("hello world")
                                .setArgument("place:-.", "world", errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{hello {$place}}")
                                .setExpected("hello {$place}")
                                .clearArguments(errorCode)
                                .setExpectedError(U_UNRESOLVED_VARIABLE_ERROR)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{{$one} and {$two}}")
                                .setExpected("1.3 and 4.2")
                                .setExpectSuccess()
                                .setArgument("one", 1.3, errorCode)
                                .setArgument("two", 4.2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
    testBuilder->setArgument("one", "1.3", errorCode).setArgument("two", "4.2", errorCode);
    test.adoptInstead(testBuilder->build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{{$one} et {$two}}")
                                .setExpected("1,3 et 4,2")
                                .setLocale(Locale("fr"), errorCode)
                                .setArgument("one", 1.3, errorCode)
                                .setArgument("two", 4.2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{hello {|4.2| :number}}")
                                .setExpected("hello 4.2")
                                .setLocale(Locale("en"), errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{hello {|foo| :number}}")
                                .setExpected("hello NaN")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{hello {:number}}")
                                .setExpected("hello NaN")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);


    test.adoptInstead(testBuilder->setPattern("{hello {|4.2| :number minimumFractionDigits=2}}")
                                .setExpected("hello 4.20")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{hello {|4.2| :number minimumFractionDigits=|2|}}")
                                .setExpected("hello 4.20")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{hello {|4.2| :number minimumFractionDigits=$foo}}")
                                .setExpected("hello 4.20")
                                .setArgument("foo", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {bar} {bar {$foo}}")
                                .setExpected("bar bar")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {|bar|} {bar {$foo}}")
                                .setExpected("bar bar")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {|bar|} {bar {$foo}}")
                                .setExpected("bar bar")
                                .setArgument("foo", "foo", errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {$bar} {bar {$foo}}")
                                .setExpected("bar foo")
                                .setArgument("bar", "foo", errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {$bar :number} {bar {$foo}}")
                                .setExpected("bar 4.2")
                                .setArgument("bar", 4.2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {$bar :number minimumFractionDigits=2} {bar {$foo}}")
                                .setExpected("bar 4.20")
                                .setArgument("bar", 4.2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {$bar :number minimumFractionDigits=foo} {bar {$foo}}")
                                .setExpected("bar 4.2")
                                .setArgument("bar", 4.2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {$bar :number} {bar {$foo}}")
                                .setExpected("bar NaN")
                                .setArgument("bar", "foo", errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {$baz} let $bar = {$foo} {bar {$bar}}")
                                .setExpected("bar foo")
                                .setArgument("baz", "foo", errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {$foo} {bar {$foo}}")
                                .setExpected("bar foo")
                                .setArgument("foo", "foo", errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {$foo} let $foo = {42} {bar {$foo}}")
                                .setExpected("bar 42")
                                .setArgument("foo", "foo", errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {42} let $foo = {$foo} {bar {$foo}}")
                                .setExpected("bar 42")
                                .setArgument("foo", "foo", errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {$foo} let $foo = {42} {bar {$foo}}")
                                .setExpected("bar 42")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {:unknown} let $foo = {42} {bar {$foo}}")
                                .setExpected("bar 42")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $x = {42} let $y = {$x} let $x = {13} {{$x} {$y}}")
                                .setExpected("13 42")
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

/*
  Shouldn't this be "bar {$bar}"?

    test.adoptInstead(testBuilder->setPattern("let $foo = {$bar} let $bar = {$baz} {bar {$foo}}")
                                .setExpected("bar foo")
                                .setArgument("baz", "foo", errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
*/

    test.adoptInstead(testBuilder->setPattern("match {$foo :select} when |1| {one} when * {other}")
                                .setExpected("one")
                                .setArgument("foo", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match {$foo :plural} when 1 {one} when * {other}")
                                .setExpected("one")
                                .setArgument("foo", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

/*
  This case can't be tested without a way to set the "foo" argument to null

    test.adoptInstead(testBuilder->setPattern("match {$foo :plural} when 1 {one} when * {other}")
                                .setExpected("other")
                                .setArgument("foo", "", errorCode)
                                .setExpectedError(U_UNRESOLVED_VARIABLE_ERROR)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);
*/

    test.adoptInstead(testBuilder->setPattern("match {$foo :plural} when one {one} when * {other}")
                                .setExpected("one")
                                .setArgument("foo", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match {$foo :plural} when 1 {=1} when one {one} when * {other}")
                                .setExpected("=1")
                                .setArgument("foo", "1", errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match {$foo :plural} when 1 {=1} when one {one} when * {other}")
                                .setExpected("=1")
                                .setArgument("foo", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match {$foo :plural} when one {one} when 1 {=1} when * {other}")
                                .setExpected("=1")
                                .setArgument("foo", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match {$foo :plural} {$bar :plural} when one one {one one} when one * {one other} when * * {other}")
                                .setExpected("one one")
                                .setArgument("foo", (int64_t) 1, errorCode)
                                .setArgument("bar", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match {$foo :plural} {$bar :plural} when one one {one one} when one * {one other} when * * {other}")
                                .setExpected("one other")
                                .setArgument("foo", (int64_t) 1, errorCode)
                                .setArgument("bar", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match {$foo :plural} {$bar :plural} when one one {one one} when one * {one other} when * * {other}")
                                .setExpected("other")
                                .setArgument("foo", (int64_t) 2, errorCode)
                                .setArgument("bar", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {$bar :plural} match {$foo} when one {one} when * {other}")
                                .setExpected("one")
                                .setArgument("bar", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $foo = {$bar :plural} match {$foo} when one {one} when * {other}")
                                .setExpected("other")
                                .setArgument("bar", (int64_t) 2, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("let $bar = {$none} match {$foo :plural} when one {one} when * {{$bar}}")
                                .setExpected("one")
                                .setArgument("foo", (int64_t) 1, errorCode)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

/*
  Note: this differs from https://github.com/messageformat/messageformat/blob/e0087bff312d759b67a9129eac135d318a1f0ce7/packages/mf2-messageformat/src/__fixtures/test-messages.json#L197

  The expected value in the test as defined there is "{$bar}".
  The value should be "{$none}" per 
https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#fallback-resolution -
"When an error occurs in an expression with a variable operand and the variable refers to a local declaration, the fallback value is formatted based on the expression on the right-hand side of the declaration, rather than the expression in the selector or pattern."
*/
    test.adoptInstead(testBuilder->setPattern("let $bar = {$none} match {$foo :plural} when one {one} when * {{$bar}}")
                                .setExpected("{$none}")
                                .setArgument("foo", (int64_t) 2, errorCode)
                                .setExpectedError(U_UNRESOLVED_VARIABLE_ERROR)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    // Missing '$' before `bar`
    test.adoptInstead(testBuilder->setPattern("let bar = {|foo|} {{$bar}}")
                                .setExpected("{$bar}")
                                .clearArguments(errorCode)
                                .setExpectedError(U_SYNTAX_ERROR)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    // Missing '=' after `bar`
    test.adoptInstead(testBuilder->setPattern("let $bar {|foo|} {{$bar}}")
                                .setExpected("foo")
                                .setExpectedError(U_SYNTAX_ERROR)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    // Missing '{'/'}' around `foo`
    test.adoptInstead(testBuilder->setPattern("let bar = |foo| {{$bar}}")
                                .setExpected("{$bar}")
                                .setExpectedError(U_SYNTAX_ERROR)
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{{+tag}}")
                                .setExpected("{+tag}")
                                .setIgnoreError()
                                .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{{+tag}content}")
                      .setExpected("{+tag}content")
                      .setIgnoreError()
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{{+tag}content{-tag}}")
                      .setExpected("{+tag}content{-tag}")
                      .setIgnoreError()
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{{-tag}content}")
                      .setExpected("{-tag}content")
                      .setIgnoreError()
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{{+tag foo=bar}}")
                      .setExpected("{+tag}")
                      .setIgnoreError()
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{{+tag foo=|foo| bar=$bar}}")
                      .setArgument("bar", "b a r", errorCode)
                      .setExpected("{+tag}")
                      .setIgnoreError()
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{{|foo| +markup}}")
                      .setExpected("{|foo|}")
                      .setIgnoreError()
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{{-tag foo=bar}}")
                      .setExpected("{-tag}")
                      .setIgnoreError()
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("no braces")
                      .clearIgnoreError()
                      .setExpected("{no braces}")
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("no braces {$foo}")
                      .setExpected("{no braces {$foo}}")
                      .setArgument("foo", (int64_t) 2, errorCode)
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{missing end brace")
                      .setExpected("missing end brace")
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{missing end {$brace")
                      .setExpected("missing end {$brace}")
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{extra} content")
                      .setExpected("extra")
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{empty { }}")
                      .setExpectedError(U_SYNTAX_ERROR)
                      .setExpected("empty \uFFFD")
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{bad {:}}")
                      .setExpected("bad {:}")
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{unquoted {literal}}")
                      .setExpected("unquoted literal")
                      .setExpectSuccess()
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{bad {\\u0000placeholder}}")
                      .clearExpected()
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{no-equal {|42| :number minimumFractionDigits 2}}")
                      .setExpected("no-equal 42.00")
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{bad {:placeholder option=}}")
                      .setExpected("bad {:placeholder}")
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{bad {:placeholder option value}}")
                      .setExpected("bad {:placeholder}")
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{bad {:placeholder option}}")
                      .setExpected("bad {:placeholder}")
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{bad {$placeholder option}}")
                      .clearExpected()
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("{no {$placeholder end}")
                      .clearExpected()
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match {} when * {foo}")
                      .clearExpected()
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match {+foo} when * {foo}")
                      .setExpected("foo")
                      .setExpectedError(U_UNKNOWN_FUNCTION_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match {|foo|} when*{foo}")
                      .clearExpected()
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match when * {foo}")
                      .clearExpected()
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match {|x|} when * foo")
                      .clearExpected()
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match {|x|} when * {foo} extra")
                      .clearExpected()
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match |x| when * {foo}")
                      .clearExpected()
                      .setExpectedError(U_SYNTAX_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match {$foo :plural} when * * {foo}")
                      .clearExpected()
                      .setExpectedError(U_VARIANT_KEY_MISMATCH_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);

    test.adoptInstead(testBuilder->setPattern("match {$foo :plural} {$bar :plural} when * {foo}")
                      .clearExpected()
                      .setExpectedError(U_VARIANT_KEY_MISMATCH_ERROR)
                      .build(errorCode));
    TestUtils::runTestCase(*this, *test, errorCode);   
}

#endif /* #if !UCONFIG_NO_FORMATTING */
