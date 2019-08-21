# Packaging ICU4J

### Note: The description in this page is not applicable to ICU4J 58 or later releases. ICU4J module build targets explained in this page are no longer available in the latest ICU4J release.

## Overview

This chapter describes, for the advanced user, how to package ICU4J for
distribution.

## Making ICU4J Smaller

The ICU project is intended to provide everything an application might need in
order to process Unicode. However, in doing so, the results may become quite
large on disk. A default build of ICU4J normally results in nearly 16 MB of
data, and a substantial amount of binary code. To reduce the amount of data
used, see the [ICU Data](icudata.md) chapter.

## Modularization of ICU4J

Some clients may not wish to ship all of ICU4J with their application, since the
application might only use a small part of ICU4J. ICU4J release 2.6 and later
provide build options to build individual ICU4J 'modules' for a more compact
distribution. The modules are based on a service and the APIs that define it,
e.g., the normalizer module supports all the APIs of the Normalizer class (and
some others). Tests can be run to verify that the APIs supported by the module
function correctly. Because of internal code dependencies, a module contains
extra classes that are not part of the module's core service API. Some or most
of the APIs of these extra classes will not work. **Only the module's core
service API is guaranteed.** Other APIs may work partially or not at all, so
client code should avoid them.

Individual modules are not built directly into their own separate jar files.
Since their dependencies often overlap, using separate modules to 'add on' ICU4J
functionality would result in unwanted duplication of class files. Instead,
building a module causes a subset of ICU4J's classes to be built and put into
ICU4J's standard build directory. After one or more module targets are built,
the 'moduleJar' target can then be built, which packages the class files into a
'module jar.' Other than the fact that it contains fewer class files, little
distinguishes this jar file from a full ICU4J jar file, and in fact they share
the same name.

Currently ICU4J can be divided into the following modules:

#### Key:

Module NameAnt TargetsTest Package SupportedSize‡Package\* Main Classes†
*\*com.ibm. should be prepended to the package name listed.*
*†Class in bold indicates core service API. Only APIs in this column are fully
supported.*
*‡Sizes are of the compressed jar file containing only this module. These sizes
are approximate for release3.6, they may change in future releases. *

#### Modules:

Normalizernormalizer,
normalizerTestscom.ibm.icu.dev.test.normalizer465KBicu.lang:UCharacter,
UCharacterCategory, UCharacterDirection, UCharacterEnums, UProperty,
Uscripticu.text:BreakIterator, CanonicalIterator, **Normalizer**, Replaceable,
ReplaceableString, SymbolTable, UCharacterIterator, UForwardCharacterIterator,
UnicodeFilter, UnicodeMatcher, UnicodeSet, UnicodeSetIterator,
UTF16icu.util:Freezable, RangeValueIterator, StringTokenizer, ULocale,
UResourceBundle, UResourceBundleIterator, UResourceTypeMismatchException,
ValueIterator, VersionInfoCollatorcollator,
collatorTestscom.ibm.icu.dev.test.collator1,911KBicu.lang:UCharacter,
UCharacterCategory, UCharacterDirection, UCharacterEnums, UProperty,
Uscripticu.text:BreakDictionary, BreakIterator, CanonicalIterator,
**CollationElementIterator**, **CollationKey**, **Collator**,
DictionaryBasedBreakIterator, Normalizer, RawCollationKey, Replaceable,
ReplaceableString, RuleBasedBreakIterator, **RuleBasedCollator**, SymbolTable,
UCharacterIterator, UForwardCharacterIterator, UnicodeFilter, UnicodeMatcher,
UnicodeSet, UnicodeSetIterator, UTF16icu.util:ByteArrayWrapper,
CompactByteArray, Freezable, RangeValueIterator, StringTokenizer, ULocale,
UResourceBundle, UResourceBundleIterator, UResourceTypeMismatchException,
ValueIterator, VersionInfoCalendarcalendar,
calendarTestscom.ibm.icu.dev.test.calendar2,176KBicu.lang:UCharacter,
UCharacterCategory, UCharacterDirection, UCharacterEnums, UProperty,
UScripticu.math:BigDecimal, MathContexticu.text:BreakIterator,
CanonicalIterator, **ChineseDateFormat**, **ChineseDateFormatSymbols**,
CollationElementIterator, CollationKey, Collator, **DateFormat**,
**DateFormatSymbols**, DecimalFormat, DecimalFormatSymbols, MessageFormat,
Normalizer, NumberFormat, PluralFormat, PluralRules, RawCollationKey,
Replaceable, ReplaceableString, RuleBasedCollator, RuleBasedNumberFormat,
RuleBasedTransliterator, **SimpleDateFormat**, SymbolTable, UCharacterIterator,
UFormat, UForwardCharacterIterator, UnicodeFilter, UnicodeMatcher, UnicodeSet,
UnicodeSetIterator, UTF16icu.util:AnnualTimeZoneRule, **BasicTimeZone**,
**BuddhistCalendar**, ByteArrayWrapper, **Calendar**, **ChineseCalendar**,
**CopticCalendar**, Currency, CurrencyAmount, **DateRule**, DateTimeRule,
**EasterHoliday**, **EthiopicCalendar**, Freezable, **GregorianCalendar**,
**HebrewCalendar**, **HebrewHoliday**, **Holiday**, **IndianCalendar**,
InitialTimeZoneRule, **IslamicCalendar**, **JapaneseCalendar**, Measure,
MeasureUnit, **RangeDateRule**, RangeValueIterator, **SimpleDateRule**,
**SimpleHoliday**, **SimpleTimeZone**, StringTokenizer, **TaiwanCalendar**,
**TimeZone**, TimeZoneRule, TimeZoneTransition, ULocale, UResourceBundle,
UResourceBundleIterator, UResourceTypeMismatchException, ValueIterator,
VersionInfoBreakIteratorbreakIterator,
breakIteratorTestscom.ibm.icu.dev.test.breakiterator1,889KBicu.lang:UCharacter,
UCharacterCategory, UCharacterDirection, UCharacterEnums, UProperty,
UScripticu.text:**BreakDictionary**, **BreakIterator**, CanonicalIterator,
**DictionaryBasedBreakIterator**, Normalizer, Replaceable, ReplaceableString,
**RuleBasedBreakIterator**, SymbolTable, Transliterator, UCharacterIterator,
UForwardCharacterIterator, UnicodeFilter, UnicodeMatcher, UnicodeSet,
UnicodeSetIterator, UTF16icu.util:CompactByteArray, Freezable,
RangeValueIterator, StringTokenizer, ULocale, UResourceBundle,
UResourceBundleIterator, UResourceTypeMismatchException, ValueIterator,
VersionInfoFormattingformat,
formatTestscom.ibm.icu.dev.test.format3,443KBicu.lang:UCharacter,
UCharacterCategory, UCharacterDirection, UCharacterEnums, UProperty,
UScripticu.math:**BigDecimal**, MathContexticu.text:BreakIterator,
CanonicalIterator, **ChineseDateFormat**, **ChineseDateFormatSymbols**,
CollationElementIterator, CollationKey, Collator, **DateFormat**,
**DateFormatSymbols**, **DecimalFormat**, **DecimalFormatSymbols**,
**DurationFormat**, MeasureFormat, **MessageFormat**, Normalizer,
**NumberFormat**, **PluralFormat**, **PluralRules**, RawCollationKey,
Replaceable, ReplaceableString, RuleBasedCollator, **RuleBasedNumberFormat**,
**SimpleDateFormat**, SymbolTable, **UCharacterIterator**, UFormat,
UForwardCharacterIterator, UnicodeFilter, UnicodeMatcher, UnicodeSet,
UnicodeSetIterator, UTF16icu.util:AnnualTimeZoneRule, BasicTimeZone,
**BuddhistCalendar**, ByteArrayWrapper, **Calendar**, **ChineseCalendar**,
**CopticCalendar**, **Currency**, CurrencyAmount, DateTimeRule,
**EthiopicCalendar**, Freezable, **GregorianCalendar**, **HebrewCalendar**,
**IndianCalendar**, InitialTimeZoneRule, **IslamicCalendar**,
**JapaneseCalendar**, Measure, MeasureUnit, RangeValueIterator,
**SimpleTimeZone**, StringTokenizer, **TaiwanCalendar**, TimeArrayTimeZoneRule,
**TimeZone**, TimeZoneRule, TimeZoneTransition, ULocale, UResourceBundle,
UResourceBundleIterator, UResourceTypeMismatchException, ValueIterator,
VersionInfoBasic PropertiespropertiesBasic,
propertiesBasicTestscom.ibm.icu.dev.test.lang554KBicu.lang:**UCharacter**,
**UCharacterCategory**, **UCharacterDirection**, **UCharacterEnums**,
**UProperty**, **UScript**, **UScriptRun**icu.text:BreakDictionary,
BreakIterator, DictionaryBasedBreakIterator, Normalizer, Replaceable,
ReplaceableString, RuleBasedBreakIterator, SymbolTable, UCharacterIterator,
UForwardCharacterIterator, UnicodeFilter, UnicodeMatcher, UnicodeSet,
UnicodeSetIterator, **UTF16**icu.util:CompactByteArray, Freezable,
RangeValueIterator, StringTokenizer, ULocale, UResourceBundle,
UResourceBundleIterator, UResourceTypeMismatchException, ValueIterator,
VersionInfo

Full PropertiespropertiesFull,
propertiesFullTestscom.ibm.icu.dev.test.lang1,829KBicu.lang:**UCharacter**,
**UCharacterCategory**, **UCharacterDirection**, **UCharacterEnums**,
**UProperty**, **UScript**, **UScriptRun**icu.text:BreakDictionary,
BreakIterator, DictionaryBasedBreakIterator, **Normalizer**, **Replaceable**,
**ReplaceableString**, RuleBasedBreakIterator, SymbolTable,
**UCharacterIterator**, **UForwardCharacterIterator**, **UnicodeFilter**,
**UnicodeMatcher**, **UnicodeSet**, **UnicodeSetIterator**,
**UTF16**icu.util:CompactByteArray, Freezable, **RangeValueIterator**,
StringTokenizer, ULocale, UResourceBundle, UResourceBundleIterator,
UResourceTypeMismatchException, **ValueIterator**, **VersionInfo**StringPrep,
IDNAstringPrep,
stringPrepTestscom.ibm.icu.dev.test.stringprep488KBicu.lang:UCharacter,
UCharacterCategory, UCharacterDirection, UCharacterEnums, UProperty,
UScripticu.text:**StringPrep**, **StringParseException**, SymbolTable,
UCharacterIterator, UForwardCharacterIterator, UnicodeFilter, UnicodeMatcher,
UnicodeSet, UnicodeSetIterator, UTF16icu.util:Freezable, RangeValueIterator,
StringTokenizer, ULocale, UResourceBundle, UResourceBundleIterator,
UResourceTypeMismatchException, ValueIterator,
VersionInfoTransformstransliterator,
transliteratorTestscom.ibm.icu.dev.test.translit890KBicu.lang:UCharacter,
UCharacterCategory, UCharacterDirection, UCharacterEnums, UProperty,
UScripticu.text:BreakDictionary, BreakIterator, DictionaryBasedBreakIterator,
Normalizer, **Replaceable**, **ReplaceableString**, RuleBasedBreakIterator,
RuleBasedCollator, **RuleBasedTransliterator**, StringTransform, SymbolTable,
**Transliterator**, UCharacterIterator, UForwardCharacterIterator,
UnicodeFilter, UnicodeMatcher, **UnicodeSet**, **UnicodeSetIterator**,
**UTF16**icu.util:CaseInsensitiveString, CompactByteArray, Freezable,
**RangeValueIterator**, StringTokenizer, ULocale, UResourceBundle,
UResourceBundleIterator, UResourceTypeMismatchException, **ValueIterator**,
VersionInfo

Building any of these modules is as easy as specifying a build target to the Ant
build system, e.g:
To build a module that contains only the Normalizer API:

1.  Build the module.
    ant normalizer

2.  Build the jar containing the module.
    ant moduleJar

3.  Build the tests for the module.
    ant normalizerTests

4.  Run the tests and verify that the self tests pass.
    java -classpath classes com.ibm.icu.dev.test.TestAll -nothrow -w

If more than one module is required, the module build targets can be
concatenated, e.g:

1.  Build the modules.
    ant normalizer collator

2.  Build the jar containing the modules.
    ant moduleJar

3.  Build the tests for the module.
    ant normalizerTests collatorTests

4.  Run the tests and verify that they pass.
    java -classpath classes com.ibm.icu.dev.test.TestAll -nothrow -w

The jar should be built before the tests, since for some targets building the
tests will cause additional classes to be compiled that are not strictly
necessary for the module itself.

*Regardless of whether ICU4J is built as a whole or as a modules, the jar file
produced is named icu4j.jar.*
*To ascertain if an icu4j.jar contains all of ICU4J or not, please see the
manifest file in the jar*
*The target moduleJar does not depend on any other target. It just creates a jar
of all class files under $icu4j_root/classes/com/ibm/icu/, excluding the classes
files in $icu4j_root/classes/com/ibm/icu/dev folder*
*The list of module build targets can be obtained by running the command: ant
-projecthelp*
