// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_FUNCTION_REGISTRY_INTERNAL_H
#define MESSAGEFORMAT2_FUNCTION_REGISTRY_INTERNAL_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "unicode/datefmt.h"
#include "unicode/messageformat2_function_registry.h"

U_NAMESPACE_BEGIN

namespace message2 {

    // Built-in functions
    /*
      The standard functions are :datetime, :date, :time,
      :number, :integer, and :string,
      per https://github.com/unicode-org/message-format-wg/blob/main/spec/registry.md
      as of https://github.com/unicode-org/message-format-wg/releases/tag/LDML45-alpha
    */
    class StandardFunctions {
        friend class MessageFormatter;

        class DateTime;
        class DateTimeValue;

        class DateTimeFactory : public FunctionFactory {
        public:
            Function* createFunction(const Locale& locale, UErrorCode& status) override;
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

            DateTimeType type;

            static DateTimeFactory* create(const DateTimeType,
                                           UErrorCode&);

            DateTimeFactory(const DateTimeType t) : type(t) {}
        }; // class DateTimeFactory

        class DateTime : public Function {
        public:
            FunctionValue* call(FunctionValue& operand,
                                FunctionOptions&& options,
                                UErrorCode& errorCode) override;
            virtual ~DateTime();

        private:
            friend class DateTimeFactory;
            friend class DateTimeValue;

            Locale locale;
            const DateTimeFactory::DateTimeType type;
            static DateTime* create(const Locale&,
                                    DateTimeFactory::DateTimeType,
                                    UErrorCode&);
            DateTime(const Locale& l, DateTimeFactory::DateTimeType t)
                : locale(l), type(t) {}
            const LocalPointer<icu::DateFormat> icuFormatter;
        };

        class NumberValue;

        class NumberFactory : public FunctionFactory {
        public:
            Function* createFunction(const Locale& locale, UErrorCode& status) override;
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

            FunctionValue* call(FunctionValue& operand,
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
            Number(const Locale& loc, bool isInt) : locale(loc), isInteger(isInt), icuFormatter(number::NumberFormatter::withLocale(loc)) {}

        // These options have their own accessor methods, since they have different default values.
            int32_t digitSizeOption(const FunctionOptions&, const UnicodeString&) const;
            int32_t maximumFractionDigits(const FunctionOptions& options) const;
            int32_t minimumFractionDigits(const FunctionOptions& options) const;
            int32_t minimumSignificantDigits(const FunctionOptions& options) const;
            int32_t maximumSignificantDigits(const FunctionOptions& options) const;
            int32_t minimumIntegerDigits(const FunctionOptions& options) const;

            bool usePercent(const FunctionOptions& options) const;
            Locale locale;
            const bool isInteger = false;
            const number::LocalizedNumberFormatter icuFormatter;

            static PluralType pluralType(const FunctionOptions& opts);
        };

        static number::LocalizedNumberFormatter formatterForOptions(const Number& number,
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
            NumberValue(const Number&, FunctionValue&, FunctionOptions&&, UErrorCode&);
        }; // class NumberValue

        class DateTimeValue : public FunctionValue {
        public:
            UnicodeString formatToString(UErrorCode&) const;
            DateTimeValue();
            virtual ~DateTimeValue();
        private:
            friend class DateTime;

            UnicodeString formattedDate;
            DateTimeValue(const Locale& locale, DateTimeFactory::DateTimeType type,
                          FunctionValue&, FunctionOptions&&, UErrorCode&);
        }; // class DateTimeValue

        class StringFactory : public FunctionFactory {
        public:
            Function* createFunction(const Locale& locale, UErrorCode& status) override;
            static StringFactory* string(UErrorCode& status);
            virtual ~StringFactory();
        private:
        }; // class StringFactory

        class String : public Function {
        public:
            FunctionValue* call(FunctionValue& val,
                                FunctionOptions&& opts,
                                UErrorCode& errorCode) override;
            static String* string(const Locale& locale, UErrorCode& status);
            virtual ~String();

        private:
            friend class StringFactory;

            // Formatting `value` to a string might require the locale
            Locale locale;

            String(const Locale& l) : locale(l) {}
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
            StringValue(const Locale&, FunctionValue&, FunctionOptions&&, UErrorCode&);
        }; // class StringValue

    };

    extern void formatDateWithDefaults(const Locale& locale, UDate date, UnicodeString&, UErrorCode& errorCode);
    extern number::FormattedNumber formatNumberWithDefaults(const Locale& locale, double toFormat, UErrorCode& errorCode);
    extern number::FormattedNumber formatNumberWithDefaults(const Locale& locale, int32_t toFormat, UErrorCode& errorCode);
    extern number::FormattedNumber formatNumberWithDefaults(const Locale& locale, int64_t toFormat, UErrorCode& errorCode);
    extern number::FormattedNumber formatNumberWithDefaults(const Locale& locale, StringPiece toFormat, UErrorCode& errorCode);
    extern DateFormat* defaultDateTimeInstance(const Locale&, UErrorCode&);

} // namespace message2

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FUNCTION_REGISTRY_INTERNAL_H

#endif // U_HIDE_DEPRECATED_API
// eof
