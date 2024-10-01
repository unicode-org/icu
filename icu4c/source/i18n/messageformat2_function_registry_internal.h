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
static constexpr std::u16string_view AUTO = u"auto";
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
static constexpr std::u16string_view INHERIT = u"inherit";
static constexpr std::u16string_view LONG = u"long";
static constexpr std::u16string_view LONG_UPPER = u"LONG";
static constexpr std::u16string_view LTR = u"ltr";
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
static constexpr std::u16string_view RTL = u"rtl";
static constexpr std::u16string_view SCIENTIFIC = u"scientific";
static constexpr std::u16string_view SECOND = u"second";
static constexpr std::u16string_view SELECT = u"select";
static constexpr std::u16string_view SHORT = u"short";
static constexpr std::u16string_view SHORT_UPPER = u"SHORT";
static constexpr std::u16string_view SIGN_DISPLAY = u"signDisplay";
static constexpr std::u16string_view STYLE = u"style";
static constexpr std::u16string_view TIME_STYLE = u"timeStyle";
static constexpr std::u16string_view TWO_DIGIT = u"2-digit";
static constexpr std::u16string_view U_DIR = u"u:dir";
static constexpr std::u16string_view U_ID = u"u:id";
static constexpr std::u16string_view U_LOCALE = u"u:locale";
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

        class DateTime : public Function {
        public:
            static DateTime* date(UErrorCode&);
            static DateTime* time(UErrorCode&);
            static DateTime* dateTime(UErrorCode&);

            LocalPointer<FunctionValue> call(const FunctionContext& context,
                                             const FunctionValue& operand,
                                             const FunctionOptions& options,
                                             UErrorCode& errorCode) override;
            virtual ~DateTime();

        private:
            friend class DateTimeFactory;
            friend class DateTimeValue;

            // Methods for parsing date literals
            static UDate tryPatterns(const UnicodeString&, UErrorCode&);
            static UDate tryTimeZonePatterns(const UnicodeString&, UErrorCode&);
            static DateInfo createDateInfoFromString(const UnicodeString&, UErrorCode&);

            typedef enum DateTimeType {
                kDate,
                kTime,
                kDateTime
            } DateTimeType;

            const DateTimeType type;
            static DateTime* create(DateTimeType, UErrorCode&);
            DateTime(DateTimeType t) : type(t) {}
            const LocalPointer<icu::DateFormat> icuFormatter;
        };

        class NumberValue;

        class Number : public Function {
        public:
            static Number* integer(UErrorCode& success);
            static Number* number( UErrorCode& success);

            LocalPointer<FunctionValue> call(const FunctionContext& context,
                                const FunctionValue& operand,
                                const FunctionOptions& options,
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

            static Number* create(bool, UErrorCode&);
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
                            int32_t* prefs,
                            int32_t& prefsLen,
                            UErrorCode& status) const override;
            UBool isSelectable() const override { return true; }
            NumberValue();
            const UnicodeString& getFunctionName() const override { return functionName; }
            virtual ~NumberValue();
        private:
            friend class Number;

            number::FormattedNumber formattedNumber;
            NumberValue(const Number&,
                        const FunctionContext&,
                        const FunctionValue&,
                        const FunctionOptions&,
                        UErrorCode&);
        }; // class NumberValue

        class DateTimeValue : public FunctionValue {
        public:
            UnicodeString formatToString(UErrorCode&) const override;
            DateTimeValue();
            const UnicodeString& getFunctionName() const override { return functionName; }
            virtual ~DateTimeValue();
        private:
            friend class DateTime;

            UnicodeString formattedDate;
            DateTimeValue(DateTime::DateTimeType type, const FunctionContext& context,
                          const FunctionValue&, const FunctionOptions&, UErrorCode&);
        }; // class DateTimeValue

        class String : public Function {
        public:
            LocalPointer<FunctionValue> call(const FunctionContext& context,
                                const FunctionValue& val,
                                const FunctionOptions& opts,
                                UErrorCode& errorCode) override;
            static String* string(UErrorCode& status);
            virtual ~String();

        private:
            friend class StringFactory;

            String() {}
        };

        // See https://github.com/unicode-org/message-format-wg/blob/main/test/README.md
        class TestFunctionValue;

        class TestFunction : public Function {
        public:
            static TestFunction* testFunction(UErrorCode&);
            static TestFunction* testFormat(UErrorCode&);
            static TestFunction* testSelect(UErrorCode&);

            LocalPointer<FunctionValue> call(const FunctionContext& context,
                                             const FunctionValue& operand,
                                             const FunctionOptions& options,
                                             UErrorCode& errorCode) override;
            virtual ~TestFunction();
        private:
            friend class TestFunctionValue;

            TestFunction(bool, bool);
            void testFunctionParameters(const FunctionValue&,
                                        const FunctionOptions&,
                                        int32_t&,
                                        bool&,
                                        bool&,
                                        double&,
                                        UErrorCode&) const;
            bool canFormat; // True iff this was invoked as test:function or test:format
            bool canSelect; // True iff this was involved as test:function or test:select
        };

        class TestFunctionValue : public FunctionValue {
        public:
            UnicodeString formatToString(UErrorCode&) const override;
            void selectKeys(const UnicodeString*,
                            int32_t,
                            int32_t*,
                            int32_t&,
                            UErrorCode&) const override;
            UBool isSelectable() const override { return canSelect; }
            TestFunctionValue();
            const UnicodeString& getFunctionName() const override { return functionName; }
            virtual ~TestFunctionValue();
        private:
            friend class TestFunction;

            TestFunctionValue(const TestFunction&,
                              const FunctionContext&,
                              const FunctionValue&,
                              const FunctionOptions&,
                              UErrorCode&);

            UnicodeString formattedString;
            bool canFormat;
            bool canSelect;
            int32_t decimalPlaces;
            bool failsFormat; // Different from "canFormat" -- derived from "fails" option
            bool failsSelect; // Different from "canSelect" -- derived from "fails" option
            double input;
        };

        class StringValue : public FunctionValue {
        public:
            UnicodeString formatToString(UErrorCode&) const override;
            void selectKeys(const UnicodeString* keys,
                            int32_t keysLen,
                            int32_t* prefs,
                            int32_t& prefsLen,
                            UErrorCode& status) const override;
            UBool isSelectable() const override { return true; }
            virtual ~StringValue();
        private:
            friend class String;

            UnicodeString formattedString;
            StringValue(const FunctionContext&, const FunctionValue&, const FunctionOptions&, UErrorCode&);
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
