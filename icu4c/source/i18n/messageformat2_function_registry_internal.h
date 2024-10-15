// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_FUNCTION_REGISTRY_INTERNAL_H
#define MESSAGEFORMAT2_FUNCTION_REGISTRY_INTERNAL_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_NORMALIZATION

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "unicode/datefmt.h"
#include "unicode/messageformat2_function_registry.h"

U_NAMESPACE_BEGIN

namespace message2 {

// Constants for option names
namespace options {
static constexpr std::u16string_view ALWAYS = u"always";
static constexpr std::u16string_view COMPACT = u"compact";
static constexpr std::u16string_view COMPACT_DISPLAY = u"compactDisplay";
static constexpr std::u16string_view DATE_STYLE = u"dateStyle";
static constexpr std::u16string_view DAY = u"day";
static constexpr std::u16string_view DECIMAL_PLACES = u"decimalPlaces";
static constexpr std::u16string_view DEFAULT_UPPER = u"DEFAULT";
static constexpr std::u16string_view ENGINEERING = u"engineering";
static constexpr std::u16string_view EXACT = u"exact";
static constexpr std::u16string_view EXCEPT_ZERO = u"exceptZero";
static constexpr std::u16string_view FAILS = u"fails";
static constexpr std::u16string_view FULL_UPPER = u"FULL";
static constexpr std::u16string_view HOUR = u"hour";
static constexpr std::u16string_view LONG = u"long";
static constexpr std::u16string_view LONG_UPPER = u"LONG";
static constexpr std::u16string_view MAXIMUM_FRACTION_DIGITS = u"maximumFractionDigits";
static constexpr std::u16string_view MAXIMUM_SIGNIFICANT_DIGITS = u"maximumSignificantDigits";
static constexpr std::u16string_view MEDIUM_UPPER = u"MEDIUM";
static constexpr std::u16string_view MIN2 = u"min2";
static constexpr std::u16string_view MINIMUM_FRACTION_DIGITS = u"minimumFractionDigits";
static constexpr std::u16string_view MINIMUM_INTEGER_DIGITS = u"minimumIntegerDigits";
static constexpr std::u16string_view MINIMUM_SIGNIFICANT_DIGITS = u"minimumSignificantDigits";
static constexpr std::u16string_view MINUTE = u"minute";
static constexpr std::u16string_view MONTH = u"month";
static constexpr std::u16string_view NARROW = u"narrow";
static constexpr std::u16string_view NEGATIVE = u"negative";
static constexpr std::u16string_view NEVER = u"never";
static constexpr std::u16string_view NOTATION = u"notation";
static constexpr std::u16string_view NUMBERING_SYSTEM = u"numberingSystem";
static constexpr std::u16string_view NUMERIC = u"numeric";
static constexpr std::u16string_view ORDINAL = u"ordinal";
static constexpr std::u16string_view PERCENT_STRING = u"percent";
static constexpr std::u16string_view SCIENTIFIC = u"scientific";
static constexpr std::u16string_view SECOND = u"second";
static constexpr std::u16string_view SELECT = u"select";
static constexpr std::u16string_view SHORT = u"short";
static constexpr std::u16string_view SHORT_UPPER = u"SHORT";
static constexpr std::u16string_view SIGN_DISPLAY = u"signDisplay";
static constexpr std::u16string_view STYLE = u"style";
static constexpr std::u16string_view TIME_STYLE = u"timeStyle";
static constexpr std::u16string_view TWO_DIGIT = u"2-digit";
static constexpr std::u16string_view USE_GROUPING = u"useGrouping";
static constexpr std::u16string_view WEEKDAY = u"weekday";
static constexpr std::u16string_view YEAR = u"year";
} // namespace options

    // Built-in functions
    /*
      The standard functions are :datetime, :date, :time,
      :number, :integer, and :string,
      per https://github.com/unicode-org/message-format-wg/blob/main/spec/registry.md
      as of https://github.com/unicode-org/message-format-wg/releases/tag/LDML45-alpha
    */
    class StandardFunctions {
        friend class MessageFormatter;

        public:
        // Used for normalizing variable names and keys for comparison
        static UnicodeString normalizeNFC(const UnicodeString&);

        private:
        static void validateDigitSizeOptions(const FunctionOptions&, UErrorCode&);
        static void checkSelectOption(const FunctionOptions&, UErrorCode&);
        static UnicodeString getStringOption(const FunctionOptions& opts,
                                             std::u16string_view optionName,
                                             UErrorCode& errorCode);

        class DateTime;
        class DateTimeValue;

        class DateTimeFactory : public FunctionFactory {
        public:
            Function* createFunction(UErrorCode& status) override;
            static DateTimeFactory* date(UErrorCode&);
            static DateTimeFactory* time(UErrorCode&);
            static DateTimeFactory* dateTime(UErrorCode&);
            virtual ~DateTimeFactory();
        private:
            friend class DateTime;
            friend class DateTimeValue;

            typedef enum DateTimeType {
                kDate,
                kTime,
                kDateTime
            } DateTimeType;
            const DateTimeFactory::DateTimeType type;
            friend class DateTimeFactory;
            DateTime(const Locale& l, DateTimeFactory::DateTimeType t)
                : locale(l), type(t) {}
            const LocalPointer<icu::DateFormat> icuFormatter;

            // Methods for parsing date literals
            UDate tryPatterns(const UnicodeString&, UErrorCode&) const;
            UDate tryTimeZonePatterns(const UnicodeString&, UErrorCode&) const;
            DateInfo createDateInfoFromString(const UnicodeString&, UErrorCode&) const;

            /*
              Looks up an option by name, first checking `opts`, then the cached options
              in `toFormat` if applicable, and finally using a default

              Ignores any options with non-string values
             */
            UnicodeString getFunctionOption(const FormattedPlaceholder& toFormat,
                                            const FunctionOptions& opts,
                                            std::u16string_view optionName) const;
            // Version for options that don't have defaults; sets the error
            // code instead of returning a default value
            UnicodeString getFunctionOption(const FormattedPlaceholder& toFormat,
                                            const FunctionOptions& opts,
                                            std::u16string_view optionName,
                                            UErrorCode& errorCode) const;

            DateTimeType type;

            static DateTimeFactory* create(const DateTimeType,
                                           UErrorCode&);

            DateTimeFactory(const DateTimeType t) : type(t) {}
        }; // class DateTimeFactory

        class DateTime : public Function {
        public:
            FunctionValue* call(const FunctionContext& context,
                                FunctionValue& operand,
                                FunctionOptions&& options,
                                UErrorCode& errorCode) override;
            virtual ~DateTime();

        private:
            friend class DateTimeFactory;
            friend class DateTimeValue;

            const DateTimeFactory::DateTimeType type;
            static DateTime* create(DateTimeFactory::DateTimeType,
                                    UErrorCode&);
            DateTime(DateTimeFactory::DateTimeType t) : type(t) {}
            const LocalPointer<icu::DateFormat> icuFormatter;
        };

        class NumberValue;

        class NumberFactory : public FunctionFactory {
        public:
            Function* createFunction(UErrorCode& status) override;
            static NumberFactory* integer(UErrorCode& success);
            static NumberFactory* number(UErrorCode& success);
            virtual ~NumberFactory();
        private:
            static NumberFactory* create(bool, UErrorCode&);
            NumberFactory(bool isInt) : isInteger(isInt) {}
            bool isInteger;
        }; // class NumberFactory

        class Number : public Function {
        public:
            static Number* integer(const Locale& loc, UErrorCode& success);
            static Number* number(const Locale& loc, UErrorCode& success);

            FunctionValue* call(const FunctionContext& context,
                                FunctionValue& operand,
                                FunctionOptions&& options,
                                UErrorCode& errorCode) override;
            virtual ~Number();

        private:
            friend class NumberValue;
            friend class StandardFunctions;

            typedef enum PluralType {
                PLURAL_ORDINAL,
                PLURAL_CARDINAL,
                PLURAL_EXACT
            } PluralType;

            static Number* create(const Locale&, bool, UErrorCode&);
            Number(bool isInt) : isInteger(isInt) /*, icuFormatter(number::NumberFormatter::withLocale(loc))*/ {}

        // These options have their own accessor methods, since they have different default values.
            int32_t digitSizeOption(const FunctionOptions&, const UnicodeString&) const;
            int32_t maximumFractionDigits(const FunctionOptions& options) const;
            int32_t minimumFractionDigits(const FunctionOptions& options) const;
            int32_t minimumSignificantDigits(const FunctionOptions& options) const;
            int32_t maximumSignificantDigits(const FunctionOptions& options) const;
            int32_t minimumIntegerDigits(const FunctionOptions& options) const;

            bool usePercent(const FunctionOptions& options) const;
            const bool isInteger = false;
            const number::LocalizedNumberFormatter icuFormatter;

            static PluralType pluralType(const FunctionOptions& opts);
        };

        static number::LocalizedNumberFormatter formatterForOptions(const Number& number,
                                                                    const Locale& locale,
                                                                    const FunctionOptions& opts,
                                                                    UErrorCode& status);


        class NumberValue : public FunctionValue {
        public:
            UnicodeString formatToString(UErrorCode&) const override;
            void selectKeys(const UnicodeString* keys,
                            int32_t keysLen,
                            UnicodeString* prefs,
                            int32_t& prefsLen,
                            UErrorCode& status) override;
            UBool isSelectable() const override { return true; }
            NumberValue();
            virtual ~NumberValue();
        private:
            friend class Number;

            Locale locale;
            number::FormattedNumber formattedNumber;
            NumberValue(const Number&,
                        const FunctionContext&,
                        FunctionValue&,
                        FunctionOptions&&,
                        UErrorCode&);
        }; // class NumberValue

        class DateTimeValue : public FunctionValue {
        public:
            UnicodeString formatToString(UErrorCode&) const;
            DateTimeValue();
            virtual ~DateTimeValue();
        private:
            friend class DateTime;

            UnicodeString formattedDate;
            DateTimeValue(DateTimeFactory::DateTimeType type, const FunctionContext& context,
                          FunctionValue&, FunctionOptions&&, UErrorCode&);
        }; // class DateTimeValue

        class StringFactory : public FunctionFactory {
        public:
            Function* createFunction(UErrorCode& status) override;
            static StringFactory* string(UErrorCode& status);
            virtual ~StringFactory();
        private:
        }; // class StringFactory

        class String : public Function {
        public:
            FunctionValue* call(const FunctionContext& context,
                                FunctionValue& val,
                                FunctionOptions&& opts,
                                UErrorCode& errorCode) override;
            static String* string(UErrorCode& status);
            virtual ~String();

        private:
            friend class StringFactory;

            String() {}
        };

        // See https://github.com/unicode-org/message-format-wg/blob/main/test/README.md
        class TestFormatFactory : public FormatterFactory {
        public:
            Formatter* createFormatter(const Locale& locale, UErrorCode& status) override;
            TestFormatFactory() {}
            virtual ~TestFormatFactory();
        };

        class TestSelect;

        class TestFormat : public Formatter {
        public:
            FormattedPlaceholder format(FormattedPlaceholder&& toFormat, FunctionOptions&& options, UErrorCode& status) const override;
            virtual ~TestFormat();

        private:
            friend class TestFormatFactory;
            friend class TestSelect;
            TestFormat() {}
            static void testFunctionParameters(const FormattedPlaceholder& arg,
                                               const FunctionOptions& options,
                                               int32_t& decimalPlaces,
                                               bool& failsFormat,
                                               bool& failsSelect,
                                               double& input,
                                               UErrorCode& status);

        };

        // See https://github.com/unicode-org/message-format-wg/blob/main/test/README.md
        class TestSelectFactory : public SelectorFactory {
        public:
            Selector* createSelector(const Locale& locale, UErrorCode& status) const override;
            TestSelectFactory() {}
            virtual ~TestSelectFactory();
        };

        class TestSelect : public Selector {
        public:
            void selectKey(FormattedPlaceholder&& val,
                           FunctionOptions&& options,
                           const UnicodeString* keys,
                           int32_t keysLen,
                           UnicodeString* prefs,
                           int32_t& prefsLen,
                           UErrorCode& status) const override;
            virtual ~TestSelect();

        private:
            friend class TestSelectFactory;
            TestSelect() {}
        };

        class StringValue : public FunctionValue {
        public:
            UnicodeString formatToString(UErrorCode&) const override;
            void selectKeys(const UnicodeString* keys,
                            int32_t keysLen,
                            UnicodeString* prefs,
                            int32_t& prefsLen,
                            UErrorCode& status) override;
            UBool isSelectable() const override { return true; }
            virtual ~StringValue();
        private:
            friend class String;

            UnicodeString formattedString;
            StringValue(const FunctionContext&, FunctionValue&, FunctionOptions&&, UErrorCode&);
        }; // class StringValue

    };

    extern void formatDateWithDefaults(const Locale& locale, const DateInfo& date, UnicodeString&, UErrorCode& errorCode);
    extern number::FormattedNumber formatNumberWithDefaults(const Locale& locale, double toFormat, UErrorCode& errorCode);
    extern number::FormattedNumber formatNumberWithDefaults(const Locale& locale, int32_t toFormat, UErrorCode& errorCode);
    extern number::FormattedNumber formatNumberWithDefaults(const Locale& locale, int64_t toFormat, UErrorCode& errorCode);
    extern number::FormattedNumber formatNumberWithDefaults(const Locale& locale, StringPiece toFormat, UErrorCode& errorCode);
    extern DateFormat* defaultDateTimeInstance(const Locale&, UErrorCode&);

} // namespace message2

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* #if !UCONFIG_NO_NORMALIZATION */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FUNCTION_REGISTRY_INTERNAL_H

#endif // U_HIDE_DEPRECATED_API
// eof
