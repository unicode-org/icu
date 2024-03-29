// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.message2.MFDataModel.Message;
import com.ibm.icu.message2.MFParser;
import com.ibm.icu.message2.MFSerializer;

@RunWith(JUnit4.class)
@SuppressWarnings({"static-method", "javadoc"})
public class SerializationTest extends CoreTestFmwk {

    @Test
    public void test() throws Exception {
        String[] testStrings = {
            "Hello {$count &something}",
            "Hello world!",
            "{{.Hello world!}}",
            "Hello {userName}",
            "Hello {$userName}",
            "Hello {|-12345.12+e10|}",
            "Hello {$count :something max=10 min=1.1416 opt1=someString opt2=|a b \\| c| @a1 @a2=|| @a3=|str|}",
            "Hello {$count &something}",
            ".input {$a :number} {{Hello world!}}",
            ".local $b = {$a :number} {{Hello world!}}",
            ".local $c = {1 :number} {{Hello {userName}}}",
            ".match {$count :number}\n"
                    + "one {{You deleted {$count} file}}\n"
                    + "*   {{You deleted {$count} files}}",
            ".match {$count :number}\n"
                    + "one {{You deleted {$count} file}}\n"
                    + "*   {{You deleted {$count} files}}",
            ".match {$place :number select=ordinal}\n"
                    + "*   {{You fininshed in the {$place}th place}}\n"
                    + "two {{You fininshed in the {$place}nd place}}\n"
                    + "one {{You fininshed in the {$place}st place}}\n"
                    + "1   {{You got the gold medal}}\n"
                    + "2   {{You got the silver medal}}\n"
                    + "3   {{You got the bronze medal}}\n"
                    + "few {{You fininshed in the {$place}rd place}}\n",
            ".match {$fileCount :number} {$folderCount :number}\n"
                    + "*   *   {{You deleted {$fileCount} files in {$folderCount} folders}}\n"
                    + "one one {{You deleted {$fileCount} file in {$folderCount} folder}}\n"
                    + "one *   {{You deleted {$fileCount} file in {$folderCount} folders}}\n"
                    + "*   one {{You deleted {$fileCount} files in {$folderCount} folder}}\n",
            "{$count :number minimumFractionDigits=2} dollars",
            "{$count :number minimumFractionDigits=3} dollars",
            "{|3.1415| :number minimumFractionDigits=5} dollars",
            "{|3.1415| :number maximumFractionDigits=2} dollars",
            ".local $c = {$count :number minimumFractionDigits=2}\n"
                    + ".match {$c}\n"
                    + "one {{{$c} dollar}}\n"
                    + "*   {{{$c} dollars}}",
            ".local $c = {$count} .foobar |asd asd asd asd| {$bar1} {$bar2} {$bar3} .local $b = {$bar} {{Foo bar}}\n",
            ".local $c = {1 :number minimumFractionDigits=2}\n"
                    + ".match {$c}\n"
                    + "one {{{$c} dollar}}\n"
                    + "*   {{{$c} dollars}}",
            ".local $c = {1 :number}\n"
                    + ".match {$c}\n"
                    + "one {{{$c} dollar}}\n"
                    + "*   {{{$c} dollars}}",
            ".local $c = {1.25 :number}\n"
                    + ".match {$c}\n"
                    + "one {{{$c} dollar}}\n"
                    + "*   {{{$c} dollars}}",
            ".local $c = {1.25 :number maximumFractionDigits=0}\n"
                    + ".match {$c}\n"
                    + "one {{{$c} dollar}}\n"
                    + "*   {{{$c} dollars}}",
            ".match {$count :number} 1 {{one}} * {{other}}",
        };
        for (String test : testStrings) {
            checkOneString(test);
        }
    }

    void checkOneString(String pattern) throws Exception {
        Message dm = MFParser.parse(pattern);
        String parsed = MFSerializer.dataModelToString(dm);

        /* This is a quick test.
         * A better idea would be to parse the original string,
         * serialize the data model, then parse again in a new data model.
         * That would give us two data models to compare, where
         * small differences in spacing or quoting does not matter.
         * But we don't have (yet) an implementation of `equals` on of the data model classes.
         */
        pattern =
                pattern.replace('\n', ' ')
                        .replaceAll("  +", " ")
                        // Naive normalization for `|1234.56|` to `1234.56`
                        .replaceAll("\\|([\\d\\.]+)\\|", "$1")
                        // Naive normalization for `|asBaC12|` to `asBaC12`
                        .replaceAll("\\|([a-zA-Z\\d]+)\\|", "$1")
                        .replaceAll(" }", "}")
                        .trim();
        assertEquals("Serialization different from to the initial source", pattern, parsed);
    }
}
