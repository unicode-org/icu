---
layout: default
title: Glossary
nav_order: 9000
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Glossary

## ICU-specific Words and Acronyms

For additional Unicode terms, please see the official 
[Unicode Standard Glossary](https://www.unicode.org/glossary/).

 Term       | Definition
------------|------------
**- A -**   | 
**accent**  | A modifying mark on a character to indicate a change in vocal tone for pronunciation. For example, the accent marks in Latin script (acute, tilde, and ogonek) and the tone marks in Thai. Synonymous with diacritic.
**accent character** | A character that has a diacritic attached to it.
**alphabetic language** | A written language in which symbols represent vowels and consonants, and in which syllables and words are formed by a phonetic combination of symbols. Examples of alphabetic languages are English, Greek, and Russian. Contrast with ideographic language.
**Arabic numerals** | Forms of decimal numerals used in most parts of the Arabic world (for instance, U+0660, U+0661, U+0662, U+0663). Although European digits (1, 2, 3...) derive historically from these forms, they are visually distinct and are coded separately. (Arabic digits are sometimes called Indic numerals; however, this nomenclature leads to confusion with the digits currently used with the scripts of India.) Arabic digits are referred to as Arabic-Indic digits in the Unicode Standard. Variant forms of Arabic digits used chiefly in Iran and Pakistan are referred to as Eastern Arabic-Indic digits.
**Arabic script** | A cursive script used in Arabic countries. Other writing systems such as Latin and Japanese also have a cursive handwritten form, but usually are typeset or printed in discrete letter form. Arabic script has only the cursive form. Arabic script is also used for Urdu, (spoken in Pakistan, Bangladesh, and India), Farsi and Persian (spoken in Iran, Iraq, and Afghanistan).
**ASCII** | "American Standard Code for Information Interchange." A standard 7-bit character set used for information interchange. ASCII encodes the basic Latin alphabet and punctuation used in American English, but does not encode the accented characters used in many European languages.
**- B -** |  
**base character** | A base character is a Unicode character that does not graphically combine with any preceding character. This does not include control or formatting characters. This is a characteristic of most Unicode characters.
**baseline** | A conceptual line with respect to which successive characters are aligned.
**Basic Multilingual Plane** | As defined by International Standard [ISO/IEC 10646](http://std.dkuug.dk/jtc1/sc2/wg2/), Unicode values `0000` through `FFFF`. This range covers all of the major living languages around the world.
**bidi** | See bidirectional.
**bidirectional** | Text which has a mixture of languages that read and write either left-to-right or right-to-left. Languages such as Arabic, Hebrew, and Yiddish have a general flow of text that proceeds horizontally from right to left, but numbers and Latin based languages like English are written from left to right.
**big-endian** | A computer architecture that stores multiple-byte numerical values with the most significant byte (MSB or big end) values first in a computer's addressable memory. This is the opposite from little-endian.
**BMP** | See Basic Multilingual Plane.
**boundary** | A boundary is a location between user characters, words, or at the start or end of a string. Boundaries break the string into logical groups of characters.
**boundary position** | Each boundary has a boundary position in a string. The boundary position is an integer that is the index of the character that follows it.
**- C -** |  
**canonical decomposition** | The decomposition of a character which results from recursively applying the canonical mappings until no characters can be further decomposed and then re-ordering non-spacing marks according to the canonical behavior rules. For instance, an acute accented A will decompose into an A character followed by an acute accent combining character. Canonical mappings do not remove formatting information, which is the opposite of what happens during a compatibility decomposition.
**canonical equivalent** | Two character sequences are said to be canonical equivalents if their full canonical decomposition are identical.
**CCSID** | Coded Character Set IDentifier. A number which IBM® uses to refer to the combination of particular code page(s), character set(s), and other information. This is defined formally in the CDRA (Coded Character Representation Architecture) documents from IBM.
**character boundary** | A location between characters.
**character properties** | The given properties of a character. These properties include, but are not limited to, case, numeric meaning, and direction to layout successive characters of the same type.
**character set** | The set of characters represented with reference to the binary codes used for the characters. One character set can be encoded into more than one code page.
**Chinese numerals** | Chinese characters that represent numbers. For example, the Chinese characters for 1, 2, and 3 are written with one, two, and three horizontal brush strokes, respectively. Contrast with Arabic numerals, Hindi numerals, and Roman numerals.
**CJK** | Acronym for Chinese/Japanese/Korean characters.
**code page** | An ordered set or characters in which a numeric index (code point value) is associated with each character. This term can also be called a "character set" or "charset."
**code point value** | The encoding value for a character in the specified character set. For example the code point value of "A" in Unicode 3.0 is `0x0041`.
**code set** | UNIX term equivalent to code page.
**collation** | Text comparison using language-sensitive rules as opposed to bitwise comparison of numeric character codes. This is usually done to sort a list of strings.
**collation element** | A collation element consists of the primary, secondary and tertiary weights of a user character.
**combining character** | A combining character is a Unicode character that graphically combines with any preceding base character. A combining character does not stand alone unless it is being described. Accents are examples of combining characters.
**combining character sequence** | A combining character sequence consists of a Unicode base character and zero or more Unicode combining characters. The base and combining characters are dynamically composed at printout time to a user character.
**compatibility character** | A character that has a compatibility decomposition.
**compatibility decomposition** | The decomposition of a character which results from recursively applying both compatibility mappings and canonical mappings until no characters can be further decomposed then re-ordering non-spacing marks according to the canonical behavior rules. Compatibility decomposition may remove formatting information, which is the opposite of what happens during a canonical decomposition.
**compatibility equivalent** | Two characters sequences are said to be compatibility equivalent if their full compatibility decompositions are equivalent.
**core product** | The language independent portion of a software product (as distinct from any particular localized version of that product - including the English language version). Sometimes, however, this term is used to refer to the English product as opposed to other localizations.
**cursive script** | A script whose adjacent characters touch or are connected to each other. For example, Arabic script is cursive.
**- D -** |  
**DBCS (double-byte character set)** | A set of characters in which each character is represented by 2 bytes. Scripts such as Japanese, Chinese, and Korean contain more characters than can be represented by 256 code points, thus requiring two bytes to uniquely represent each character. The term DBCS is often used to mean MBCS (multi-byte character set). See multi-byte character set.
**decomposable character** | A character that is comparable to a sequence of one or more other characters.
**decomposition** | A sequence of one or more characters that is equivalent to a decomposable character.
**diacritic** | A modifying mark on a character. For example, the accent marks in Latin script (acute, tilde, and ogonek) and the tone marks in Thai. Synonymous with accent.
**digit** | A general term for a number character. A digit may or may not be base ten.
**display string** | A display string is a string that may be shown to a user. Normally a display string is visible in GUI. These strings need to be translated for different countries.
**- E -** |  
**EBCDIC** | Extended Binary-Coded Decimal Interchange Code. A group of coded character sets that consists of eight-bit coded characters. EBCDIC-coded character sets map specified graphic and control characters onto code points, each consisting of 8 bits. EBCDIC is an extension of BCD (Binary-Coded Decimal), which uses only 7 bits for each character.
**ECMA** | European Computer Manufacturers Association. A nonprofit organization formed by European computer vendors to announce standards applicable to the functional design and use of data processing equipment.
**encoding scheme** | A set of specific definitions that describe the philosophy used to represent character data. Examples of specifications in such a definition are: the number of bits, the number of bytes, the allowable ranges of bytes, maximum number of characters, and meanings assigned to some generic and specific bit patterns.
**European numerals** | A number comprised of the digits 0, 1, 2, 3, 4, 5, 6, 7, 8, and/or 9.
**expansion** | The process of sorting a character as if it were expanded to two characters.
**- F -** |  
**font** | A set of graphic characters that have a characteristic design, or a font designer's concept of how the graphic characters should appear. The characteristic design specifies the characteristics of its graphic characters. Examples of characteristics are shape, graphic pattern, style, size, weight, and increment.
**- G -** |  
**globalization** | The process of developing, manufacturing, and marketing software products that are intended for worldwide distribution. This term combines two aspects of the work: internationalization (enabling the product to be used without language or culture barriers) and localization (translating and enabling the product for a specific locale).
**glyph** | The actual shape (bit pattern, outline) of a character image. For example, an italic "A" and a roman "A" are two different glyphs representing the same underlying character. Strictly speaking, any two images that differ in shape constitute different glyphs. In this usage, glyph is a synonym for character image, or simply image.
**graphic character** | A character, other than a control function, that has a visual representation normally handwritten, printed, or displayed.
**Graphical User Interface** | Graphical User Interface is normally written as the acronym GUI. It is the display the end-user sees when running a program. Strings that are visible in the GUI need to localized to the end-user's language.
**global application** | An application that can be completely translated for use in different locales. All text shown to the user is in the native language, and user expectations are met for dates, times, and other locale conventions.
**GMT** | Greenwich mean time. In the 1840s the standard time kept by the Royal Greenwich Observatory located at Greenwich, England was established for all of England, Scotland, and Wales, replacing many local times in use in those days. Subsequently GMT became the official time reference for the world until 1972 when it was subsumed by the atomic clock-based coordinated universal time (UTC). GMT is also known as universal time.
**GUI** | Acronym for "Graphical User Interface".
**- H -** |  
**Han Characters** | Ideographic characters of Chinese origin.
**Hangul** | The Korean alphabet that consists of fourteen consonants and ten vowels. Hangul was created by a team of scholars in the 15th century at the behest of King Sejong. See jamo.
**Hanja** | The Korean term for characters derived from Chinese.
**Hiragana** | A Japanese phonetic syllabary. The symbols are cursive or curvilinear in style. See Kanji and Katakana.
**- I -** |  
**i18n** | Synonym for internationalization ("i" + 18 letters + "n"; lower case i is used to distinguish it from the numeral 1 (one)).
**ideographic language** | A written language in which each character (ideogram) represents a thing or an idea (but not necessarily a particular word or phrase). An example of such a language is written Chinese (Zhongen). Contrast with alphabetic language.
**Indic numerals** | A set of numerals used in India and many Arabic countries instead of, or in addition to, the Arabic numerals. Indic numeral shapes correspond to the Arabic numeral shapes. Contrast with Arabic numerals, Chinese numerals, and Roman numerals. See numbers.
**internationalization** | Designing and developing a software product to function in multiple locales. This process involves identifying the locales that must be supported, designing features which support those locales, and writing code that functions equally well in any of the supported locales. Internationalized applications store their text in external resources, and use locale-sensitive utilities for formatting and collation.
**ISO** | International Organization for Standardization. Contrary to popular belief, ISO does NOT stand for International Standards Organization because it is not an acronym. The ISO name is derived from the Greek word isos, which means "equal." ISO is a non-governmental international organization, and it promotes the development of standards on goods and services.
**- J -** | 
**jamo** | A set of consonants and vowels used in Korean Hangul. The word jamo is derived from ja, which means consonant, and mo, which means vowel.
**- K -** | 
**Kanji** | Chinese characters or ideograms used in Japanese writing. The characters may have different meanings from their Chinese counterparts. See Hiragana and Katakana.
**Katakana** | A Japanese phonetic syllabary used primarily for foreign names and place names and words of foreign origin. The symbols are angular, while those of Hiragana are cursive. Katakana is written left to right, or top to bottom. See Kanji.
**- L -** |  
**L10n** | Synonym for "localization" ("L" + 10 letters + "n"; upper case L is used to distinguish it from the numeral 1 (one)).
**L12y** | Acronym for "localizability" ("L" + 12 letters + "y"; upper case L is used to distinguish it from the numeral 1 (one)).
**language** | A set of characters, phonemes, conventions, and rules used for conveying information. The aspects of a language are pragmatics, semantics, syntax, phonology, and morphology.
**legacy** | An inherited obligation. For example, a legacy database might contain strategic data that must be maintained for a long time after the database has become technologically obsolete.
**locale** | A set of conventions affected or determined by human language and customs, as defined within a particular geo-political region. These conventions include (but are not necessarily limited to) the written language, formats for dates, numbers and currency, sorting orders, etc.
**locale-sensitive** | Exhibiting different behavior or returning different data, depending on the locale.
**localizability** | The degree to which a software product can be localized. Localizable products separate data from code, correctly display the target language and function properly after being localized.
**localization** | Modifying or adapting a software product to fit the requirements of a particular locale. This process includes (but may not be limited to) translating the user interface, documentation and packaging, changing dialog box geometries, customizing features (if necessary), and testing the translated product to ensure that it still works (at least as well as the original).
**lowercase** | The small alphabetic characters, whether accented or not, as distinguished from the capital alphabetic characters. The concept of case applies to alphabets such as Latin, Cyrillic, and Greek, but not to Arabic, Hebrew, Thai, Japanese, Chinese, Korean, and many other scripts. Examples of lowercase letters are a, b, and c. Contrast with uppercase.
**- M -** | 
**MBCS** | Multi-byte Character Set. A set of characters in which each character is represented by 1 or more bytes. Contrast with DBCS and SBCS.
**modifier characters** | '`@`' (French secondary collation rule)
**multilingual** | An application that can simultaneously display and manipulate text in multiple languages. For example, a word processor that allows Japanese and English in the same document is multilingual.
**- N -** | 
**National Standard** | A linguistic rule, measurement, educational guideline, or technology-related convention as defined by a government or an industry standards organization. Examples include character sets, keyboard layouts, and some cultural conventions, such as punctuations.
**NLS** | National Language Support. The features of a product that accommodate a specific region, its language, script, local conventions, and culture. See internationalization and localization.
**non-display string** | A non-display string is a string such as a URL that is used programmatically and is not visible to an end-user. A non-display string does not need to be translated.
**normalization** | The process of converting Unicode text into one of several standardized forms in which precomposed and combining characters are used consistently.
**numbers** | Numbers express either quantity (cardinal) or order (ordinal). Many cultures have different forms for cardinal and ordinal numbers. For example, in French the cardinal number five is cinq, but the ordinal fifth is cinquième or 5eme or 5e. Numbers are written with symbols that are usually referred to as numerals. See Arabic numerals, Chinese numerals, Indic numerals, European numerals, and Roman numerals.
**- P -** | 
**pinyin** | A system to phonetically render Chinese ideograms in a Latin alphabet.
**- R -** | 
**relation characters** | '`<`' (primary difference collation rule) <br>'`;`' (secondary difference collation rule) <br>'`,`' (tertiary difference collation rule) <br>'`=`' (identical difference collation rule)
**reset character** | '`&`' (reset the collation rules)
**resource** | 1. Any part of a program which can appear to the user or be changed or configured by the user. <br>2. Any piece of the program's data, as opposed to its code.
**resource bundle** | A set of culturally dependent data used by locale-sensitive classes in an internationalized software program to provide Locale specific responses to the end-user.
**Roman numerals** | A system of writing numbers in which the characters I, V, X, L, C, D, and M have the value of 1, 5, 10, 50, 100, 500, and 1000, respectively. Lesser numbers in prefix positions indicate subtraction. For example MCMLXIV is 1964 in decimal because CM is 900, LX is 60, and IV is 4. Contrast with Arabic numerals, European numerals, Chinese numerals, and Indic numerals.
**- S -** | 
**SBCS (Single-byte character set)** | A set of characters in which each character is represented by 1 byte.
**script** | A set of characters used to write a particular set of languages. For example, the Latin (or Roman) script is used to write English, French, Spanish, and most other European languages; the Cyrillic script is used to write Russian and Serbian.
**separator** | The thousands separator (or digit grouping separator) is the local symbol used to separate every third digit in large numbers or lengthy decimal fractions. The decimal separator is the local symbol used to indicate the decimal position in a number. It may be a comma, period or some other language specific symbol.
**string** | A set of consecutive characters treated by a computer as a single item.
**- T -** | 
**titlecase** | A set of words that usually have the first character of each word in uppercase characters. The rules for titlecase are specific to each locale. Titlecase words usually go on titles of literature and other publications.
**transcoding** | Conversion of character data from one character set to another.
**translation** | The conversion of text from one human language to another. This includes properly converting the grammar, spelling and meaning of the text into the target language.
**transliteration** | Transformation of text from one script to another, usually based on phonetic equivalences and not word meanings. For example, Greek text might be transliterated into the Latin script so that it can be pronounced by English speakers.
**- U -** | 
**UCS** | Universal Multiple-Octet Coded Character Set. The Unicode standard is based upon this ISO/IEC 10646 standard. UCS characters look the same Unicode characters, but they do not have any character properties. Synonymous with UTF.
**Unicode** | A character set that encompasses all of the world's living scripts. Unicode is the basis of most modern software internationalization.
**Unicode character** | A Unicode character enables a computer to store, manipulate, and transfer to other computers multilingual text. A Unicode character has the binary range of 0..10FFFF.
**uppercase** | The larger alphabetic characters, whether accented or not, as distinguished from the lowercase alphabetic characters. The concept of case applies to alphabets such as Latin, Cyrillic, and Greek, but not to Arabic, Hebrew, Thai, Japanese, Chinese, Korean, and many other scripts. Examples of uppercase letters are A, B, and C. Contrast with lowercase.
**user character** | A character made up of two or more Unicode characters that are combined to form a more complex character that has its own semantic value. A user character is the smallest component of written language that has a semantic value to a native language user.
**UTC time** | UTC stands for Coordinated Universal Time. This was formerly known as Greenwich Mean Time (GMT). It is used as a time constant that can be transformed to display an accurate date and time in any world calendar and time zone. This is a time scale based on a cesium atomic clocks.
**UTF** | Unicode Transformation Format. A binary format of representing a Unicode character. There are several encoding forms for a Unicode character, which include UTF-8, UTF-16BE, UTF-16LE, UTF-32BE and UTF-32LE. The numbers in these encoding form names refer to the bit size of each number, and the BE and LE stands for big endian or little endian respectively. The UTF-8 and UTF-16 formats can take multiple units of binary numbers to represent a Unicode character.
