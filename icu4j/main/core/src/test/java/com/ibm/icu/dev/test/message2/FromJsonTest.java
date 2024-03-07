// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;

/**
 * These tests come from the test suite created for the JavaScript implementation of MessageFormat v2.
 *
 * <p>Original JSON file
 * <a href="https://github.com/messageformat/messageformat/blob/master/packages/mf2-messageformat/src/__fixtures/test-messages.json">here</a>.</p>
 */
@RunWith(JUnit4.class)
@SuppressWarnings("javadoc")
public class FromJsonTest extends CoreTestFmwk {

    static final TestCase[] TEST_CASES = {
            new TestCase.Builder()
                .pattern("{hello}")
                .expected("hello")
                .build(),
            new TestCase.Builder()
                .pattern("{hello {(world)}}")
                .expected("hello world")
                .build(),
            new TestCase.Builder()
                .pattern("{hello {()}}")
                .expected("hello ")
                .build(),
            new TestCase.Builder()
                .pattern("{hello {$place}}")
                .arguments(Args.of("place", "world"))
                .expected("hello world")
                .build(),
            new TestCase.Builder()
                .pattern("{hello {$place}}")
                .expected("hello {$place}")
                // errorsJs: ["missing-var"]
                .build(),
            new TestCase.Builder()
                .pattern("{{$one} and {$two}}")
                .arguments(Args.of("one", 1.3, "two", 4.2))
                .expected("1.3 and 4.2")
                .build(),
            new TestCase.Builder()
                .pattern("{{$one} et {$two}}")
                .locale("fr")
                .arguments(Args.of("one", 1.3, "two", 4.2))
                .expected("1,3 et 4,2")
                .build(),
            new TestCase.Builder()
                .pattern("{hello {(4.2) :number}}")
                .expected("hello 4.2")
                .build(),
            new TestCase.Builder() // not in the original JSON
                .locale("ar-EG")
                .pattern("{hello {(4.2) :number}}")
                .expected("hello \u0664\u066B\u0662")
                .build(),
            new TestCase.Builder()
                .pattern("{hello {(foo) :number}}")
                .expected("hello NaN")
                .build(),
            new TestCase.Builder()
                .pattern("{hello {:number}}")
                .expected("hello NaN")
                // This is different from JS, should be an error.
                .errors("ICU4J: exception.")
                .build(),
            new TestCase.Builder()
                .pattern("{hello {(4.2) :number minimumFractionDigits=2}}")
                .expected("hello 4.20")
                .build(),
            new TestCase.Builder()
                .pattern("{hello {(4.2) :number minimumFractionDigits=(2)}}")
                .expected("hello 4.20")
                .build(),
            new TestCase.Builder()
                .pattern("{hello {(4.2) :number minimumFractionDigits=$foo}}")
                .arguments(Args.of("foo", 2f))
                .expected("hello 4.20")
                .build(),
            new TestCase.Builder()
                .pattern("{hello {(4.2) :number minimumFractionDigits=$foo}}")
                .arguments(Args.of("foo", "2"))
                .expected("hello 4.20")
                // errorsJs: ["invalid-type"]
                .build(),
            new TestCase.Builder()
                .pattern("let $foo = {(bar)} {bar {$foo}}")
                .expected("bar bar")
                .build(),
            new TestCase.Builder()
                .pattern("let $foo = {(bar)} {bar {$foo}}")
                .arguments(Args.of("foo", "foo"))
                // expectedJs: "bar foo"
                // It is undefined if we allow arguments to override local variables, or it is an error.
                // And undefined who wins if that happens, the local variable of the argument.
                .expected("bar bar")
                .build(),
            new TestCase.Builder()
                .pattern("let $foo = {$bar} {bar {$foo}}")
                .arguments(Args.of("bar", "foo"))
                .expected("bar foo")
                .build(),
            new TestCase.Builder()
                .pattern("let $foo = {$bar :number} {bar {$foo}}")
                .arguments(Args.of("bar", 4.2))
                .expected("bar 4.2")
                .build(),
            new TestCase.Builder()
                .pattern("let $foo = {$bar :number minimumFractionDigits=2} {bar {$foo}}")
                .arguments(Args.of("bar", 4.2))
                .expected("bar 4.20")
                .build(),
            new TestCase.Builder().ignore("Maybe") // Because minimumFractionDigits=foo
                .pattern("let $foo = {$bar :number minimumFractionDigits=foo} {bar {$foo}}")
                .arguments(Args.of("bar", 4.2))
                .expected("bar 4.2")
                .errors("invalid-type")
                .build(),
            new TestCase.Builder().ignore("Maybe. Function specific behavior.")
                .pattern("let $foo = {$bar :number} {bar {$foo}}")
                .arguments(Args.of("bar", "foo"))
                .expected("bar NaN")
                .build(),
            new TestCase.Builder()
                .pattern("let $foo = {$bar} let $bar = {$baz} {bar {$foo}}")
                .arguments(Args.of("baz", "foo"))
                // expectedJs: "bar foo"
                // It is currently undefined if a local variable (like $foo)
                // can reference a local variable that was not yet defined (like $bar).
                // That is called hoisting and it is valid in JavaScript or Python.
                // Not allowing that would prevent circular references.
                // https://github.com/unicode-org/message-format-wg/issues/292
                .expected("bar {$bar}")
                .build(),
            new TestCase.Builder()
                .patternJs("match {$foo} when (1) {one} when * {other}")
                .pattern("match {$foo :select} when (1) {one} when * {other}")
                .arguments(Args.of("foo", "1"))
                .expected("one")
                .build(),
            new TestCase.Builder()
                .pattern("match {$foo :plural} when 1 {one} when * {other}")
                .arguments(Args.of("foo", "1")) // Should this be error? Plural on string?
                // expectedJs: "one"
                .expected("other")
                .build(),
            new TestCase.Builder()
                .pattern("match {$foo :select} when (1) {one} when * {other}")
                .arguments(Args.of("foo", "1"))
                .expected("one")
                .build(),
            new TestCase.Builder()
                .patternJs("match {$foo} when 1 {one} when * {other}")
                .pattern("match {$foo :plural} when 1 {one} when * {other}")
                .arguments(Args.of("foo", 1))
                .expected("one")
                .build(),
            new TestCase.Builder()
                .pattern("match {$foo :plural} when 1 {one} when * {other}")
                .arguments(Args.of("foo", 1))
                .expected("one")
                .build(),
            new TestCase.Builder().ignore("not possible to put a null in a map")
                .pattern("match {$foo} when 1 {one} when * {other}")
                .arguments(Args.of("foo", null))
                .expected("other")
                .build(),
            new TestCase.Builder()
                .patternJs("match {$foo} when 1 {one} when * {other}")
                .pattern("match {$foo :plural} when 1 {one} when * {other}")
                .expected("other")
                .errors("missing-var")
                .build(),
            new TestCase.Builder()
                .patternJs("match {$foo} when one {one} when * {other}")
                .pattern("match {$foo :plural} when one {one} when * {other}")
                .arguments(Args.of("foo", 1))
                .expected("one")
                .build(),
            new TestCase.Builder()
                .patternJs("match {$foo} when 1 {=1} when one {one} when * {other}")
                .pattern("match {$foo :plural} when 1 {=1} when one {one} when * {other}")
                .arguments(Args.of("foo", 1))
                .expected("=1")
                .build(),
            new TestCase.Builder()
                .patternJs("match {$foo} when one {one} when 1 {=1} when * {other}")
                .pattern("match {$foo :plural} when one {one} when 1 {=1} when * {other}")
                .arguments(Args.of("foo", 1))
                .expected("one")
                .build(),
            new TestCase.Builder()
                .patternJs("match {$foo} {$bar} when one one {one one} when one * {one other} when * * {other}")
                .pattern("match {$foo :plural} {$bar :plural} when one one {one one} when one * {one other} when * * {other}")
                .arguments(Args.of("foo", 1, "bar", 1))
                .expected("one one")
                .build(),
            new TestCase.Builder()
                .patternJs("match {$foo} {$bar} when one one {one one} when one * {one other} when * * {other}")
                .pattern("match {$foo :plural} {$bar :plural} when one one {one one} when one * {one other} when * * {other}")
                .arguments(Args.of("foo", 1, "bar", 2))
                .expected("one other")
                .build(),
            new TestCase.Builder()
                .patternJs("match {$foo} {$bar} when one one {one one} when one * {one other} when * * {other}")
                .pattern("match {$foo :plural} {$bar :plural} when one one {one one} when one * {one other} when * * {other}")
                .arguments(Args.of("foo", 2, "bar", 2))
                .expected("other")
                .build(),
            new TestCase.Builder()
                .patternJs("let $foo = {$bar} match {$foo} when one {one} when * {other}")
                .pattern("let $foo = {$bar} match {$foo :plural} when one {one} when * {other}")
                .arguments(Args.of("bar", 1))
                .expected("one")
                .build(),
            new TestCase.Builder()
                .patternJs("let $foo = {$bar} match {$foo} when one {one} when * {other}")
                .pattern("let $foo = {$bar} match {$foo :plural} when one {one} when * {other}")
                .arguments(Args.of("bar", 2))
                .expected("other")
                .build(),
            new TestCase.Builder()
                .patternJs("let $bar = {$none} match {$foo} when one {one} when * {{$bar}}")
                .pattern("let $bar = {$none} match {$foo :plural} when one {one} when * {{$bar}}")
                .arguments(Args.of("foo", 1))
                .expected("one")
                .build(),
            new TestCase.Builder()
                .patternJs("let $bar = {$none} match {$foo} when one {one} when * {{$bar}}")
                .pattern("let $bar = {$none :plural} match {$foo} when one {one} when * {{$bar}}")
                .arguments(Args.of("foo", 2))
                .expected("{$bar}")
                .errors("missing-var")
                .build(),
            new TestCase.Builder()
                .pattern("let bar = {(foo)} {{$bar}}")
                .expected("{$bar}")
                .errors("missing-char", "missing-var")
                .build(),
            new TestCase.Builder()
                .pattern("let $bar {(foo)} {{$bar}}")
                .expected("foo")
                .errors("missing-char")
                .build(),
            new TestCase.Builder()
                .pattern("let $bar = (foo) {{$bar}}")
                .expected("{$bar}")
                .errors("missing-char", "junk-element")
                .build(),
            new TestCase.Builder().ignore("no markup support")
                .pattern("{{+tag}}")
                .expected("{+tag}")
                .build(),
            new TestCase.Builder().ignore("no markup support")
                .pattern("{{+tag}content}")
                .expected("{+tag}content")
                .build(),
            new TestCase.Builder().ignore("no markup support")
                .pattern("{{+tag}content{-tag}}")
                .expected("{+tag}content{-tag}")
                .build(),
            new TestCase.Builder().ignore("no markup support")
                .pattern("{{-tag}content}")
                .expected("{-tag}content")
                .build(),
            new TestCase.Builder().ignore("no markup support")
                .pattern("{{+tag foo=bar}}")
                .expected("{+tag foo=bar}")
                .build(),
            new TestCase.Builder().ignore("no markup support")
                .pattern("{{+tag foo=(foo) bar=$bar}}")
                .arguments(Args.of("bar", "b a r"))
                .expected("{+tag foo=foo bar=(b a r)}")
                .build(),
            new TestCase.Builder()
                .pattern("{bad {(foo) +markup}}")
                .expected("bad {+markup}")
                .errors("extra-content")
                .build(),
            new TestCase.Builder()
                .pattern("{{-tag foo=bar}}")
                .expected("{-tag}")
                .errors("extra-content")
                .build(),
            new TestCase.Builder()
                .pattern("no braces")
                .expected("{no braces}")
                .errors("parse-error", "junk-element")
                .build(),
            new TestCase.Builder()
                .pattern("no braces {$foo}")
                .arguments(Args.of("foo", 2))
                .expected("{no braces {$foo}}")
                .errors("parse-error", "junk-element")
                .build(),
            new TestCase.Builder().ignore("infinite loop!")
                .pattern("{missing end brace")
                .expected("missing end brace")
                .errors("missing-char")
                .build(),
            new TestCase.Builder()
                .pattern("{missing end {$brace")
                .expected("missing end {$brace}")
                .errors("missing-char", "missing-char", "missing-var")
                .build(),
            new TestCase.Builder()
                .pattern("{extra} content")
                .expected("extra")
                .errors("extra-content")
                .build(),
            new TestCase.Builder()
                .pattern("{empty { }}")
                .expected("empty ")
                // errorsJs: ["parse-error", "junk-element"]
                .build(),
            new TestCase.Builder()
                .pattern("{bad {:}}")
                .expected("bad {:}")
                .errors("empty-token", "missing-func")
                .build(),
            new TestCase.Builder()
                .pattern("{bad {placeholder}}")
                .expected("bad {placeholder}")
                .errors("parse-error", "extra-content", "junk-element")
                .build(),
            new TestCase.Builder()
                .pattern("{no-equal {(42) :number minimumFractionDigits 2}}")
                .expected( "no-equal 42.00")
                .errors("missing-char")
                .build(),
            new TestCase.Builder()
                .pattern("{bad {:placeholder option=}}")
                .expected("bad {:placeholder}")
                .errors("empty-token", "missing-func")
                .build(),
            new TestCase.Builder()
                .pattern("{bad {:placeholder option value}}")
                .expected("bad {:placeholder}")
                .errors("missing-char", "missing-func")
                .build(),
            new TestCase.Builder()
                .pattern("{bad {:placeholder option}}")
                .expected("bad {:placeholder}")
                .errors("missing-char", "empty-token", "missing-func")
                .build(),
            new TestCase.Builder()
                .pattern("{bad {$placeholder option}}")
                .expected("bad {$placeholder}")
                .errors("extra-content", "extra-content", "missing-var")
                .build(),
            new TestCase.Builder()
                .pattern("{no {$placeholder end}")
                .expected("no {$placeholder}")
                .errors("extra-content", "missing-var")
                .build(),
            new TestCase.Builder()
                .pattern("match {} when * {foo}")
                .expected("foo")
                .errors("parse-error", "bad-selector", "junk-element")
                .build(),
            new TestCase.Builder()
                .pattern("match {+foo} when * {foo}")
                .expected("foo")
                .errors("bad-selector")
                .build(),
            new TestCase.Builder()
                .pattern("match {(foo)} when*{foo}")
                .expected("foo")
                .errors("missing-char")
                .build(),
            new TestCase.Builder()
                .pattern("match when * {foo}")
                .expected("foo")
                .errors("empty-token")
                .build(),
            new TestCase.Builder()
                .pattern("match {(x)} when * foo")
                .expected("")
                .errors("key-mismatch", "missing-char")
                .build(),
            new TestCase.Builder()
                .pattern("match {(x)} when * {foo} extra")
                .expected("foo")
                .errors("extra-content")
                .build(),
            new TestCase.Builder()
                .pattern("match (x) when * {foo}")
                .expected("")
                .errors("empty-token", "extra-content")
                .build(),
            new TestCase.Builder()
                .pattern("match {$foo} when * * {foo}")
                .expected("foo")
                .errors("key-mismatch", "missing-var")
                .build(),
            new TestCase.Builder()
                .pattern("match {$foo} {$bar} when * {foo}")
                .expected("foo")
                .errors("key-mismatch", "missing-var", "missing-var")
                .build()
    };

    @Test
    public void test() {
        int ignoreCount = 0;
        for (TestCase testCase : TEST_CASES) {
            if (testCase.ignore)
                ignoreCount++;
            TestUtils.runTestCase(testCase);
        }
        System.out.printf("Executed %d test cases out of %d, skipped %d%n",
                TEST_CASES.length - ignoreCount, TEST_CASES.length, ignoreCount);
    }
}
