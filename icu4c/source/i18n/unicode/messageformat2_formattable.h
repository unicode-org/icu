// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#ifndef MESSAGEFORMAT2_FORMATTABLE_H
#define MESSAGEFORMAT2_FORMATTABLE_H

#if U_SHOW_CPLUSPLUS_API

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "unicode/chariter.h"
#include "unicode/numberformatter.h"
#include "unicode/messageformat2_data_model_names.h"

#ifndef U_HIDE_DEPRECATED_API

#include <map>
#include <variant>

U_NAMESPACE_BEGIN

class Hashtable;
class UVector;

namespace message2 {

    // Formattable
    // ----------

    /**
     * `FormattableObject` is an abstract class that can be implemented in order to define
     * an arbitrary class that can be passed to a custom formatter or selector function.
     * To be passed in such a way, it must be wrapped in a `Formattable` object.
     *
     * @internal ICU 75 technology preview
     * @deprecated This API is for technology preview only.
     */
    class U_I18N_API FormattableObject : public UObject {
    public:
        /**
         * Returns an arbitrary string representing the type of this object.
         * It's up to the implementor of this class, as well as the implementors
         * of any custom functions that rely on particular values of this tag
         * corresponding to particular classes that the object contents can be
         * downcast to, to ensure that the type tags are used soundly.
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        virtual const UnicodeString& tag() const = 0;
        /**
         * Destructor.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        virtual ~FormattableObject();
    }; // class FormattableObject

    class Formattable;
} // namespace message2

U_NAMESPACE_END

/// @cond DOXYGEN_IGNORE
// Export an explicit template instantiation of the std::variant that is used
// to represent the message2::Formattable class.
// (When building DLLs for Windows this is required.)
// (See measunit_impl.h, datefmt.h, collationiterator.h, erarules.h and others
// for similar examples.)
#if U_PF_WINDOWS <= U_PLATFORM && U_PLATFORM <= U_PF_CYGWIN
#if defined(U_REAL_MSVC) && defined(_MSVC_STL_VERSION)
template class U_I18N_API std::_Variant_storage_<false,
  double,
  int64_t,
  icu::UnicodeString,
  icu::Formattable,
  const icu::message2::FormattableObject *,
  std::pair<const icu::message2::Formattable *,int32_t>>;
#endif
typedef std::pair<const icu::message2::Formattable*, int32_t> P;
template class U_I18N_API std::variant<double,
				       int64_t,
				       icu::UnicodeString,
				       icu::Formattable,
				       const icu::message2::FormattableObject*,
                                       P>;
#endif
/// @endcond

U_NAMESPACE_BEGIN

namespace message2 {
    /**
     * The `Formattable` class represents a typed value that can be formatted,
     * originating either from a message argument or a literal in the code.
     * ICU's Formattable class is not used in MessageFormat 2 because it's unsafe to copy an
     * icu::Formattable value that contains an object. (See ICU-20275).
     *
     * `Formattable` is immutable (not deeply immutable) and
     * is movable and copyable.
     * (Copying does not do a deep copy when the wrapped value is an array or
     * object. Likewise, while a pointer to a wrapped array or object is `const`,
     * the referents of the pointers may be mutated by other code.)
     *
     * @internal ICU 75 technology preview
     * @deprecated This API is for technology preview only.
     */
    class U_I18N_API Formattable : public UObject {
    public:

        /**
         * Gets the data type of this Formattable object.
         * @return    the data type of this Formattable object.
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        UFormattableType getType() const;

        /**
         * Gets the double value of this object. If this object is not of type
         * UFMT_DOUBLE, then the result is undefined and the error code is set.
         *
         * @param status Input/output error code.
         * @return    the double value of this object.
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        double getDouble(UErrorCode& status) const {
            if (U_SUCCESS(status)) {
                if (isDecimal() && getType() == UFMT_DOUBLE) {
                    return (std::get_if<icu::Formattable>(&contents))->getDouble();
                }
                if (std::holds_alternative<double>(contents)) {
                    return *(std::get_if<double>(&contents));
                }
                status = U_ILLEGAL_ARGUMENT_ERROR;
            }
            return 0;
        }

        /**
         * Gets the long value of this object. If this object is not of type
         * UFMT_LONG then the result is undefined and the error code is set.
         *
         * @param status Input/output error code.
         * @return    the long value of this object.
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        int32_t getLong(UErrorCode& status) const {
            if (U_SUCCESS(status)) {
                if (isDecimal() && getType() == UFMT_LONG) {
                    return std::get_if<icu::Formattable>(&contents)->getLong();
                }
                if (std::holds_alternative<int64_t>(contents)) {
                    return static_cast<int32_t>(*(std::get_if<int64_t>(&contents)));
                }
                status = U_ILLEGAL_ARGUMENT_ERROR;
            }
            return 0;
        }

        /**
         * Gets the int64 value of this object. If this object is not of type
         * kInt64 then the result is undefined and the error code is set.
         * If conversion to int64 is desired, call getInt64()
         *
         * @param status Input/output error code.
         * @return    the int64 value of this object.
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        int64_t getInt64Value(UErrorCode& status) const {
            if (U_SUCCESS(status)) {
                if (isDecimal() && getType() == UFMT_INT64) {
                    return std::get_if<icu::Formattable>(&contents)->getInt64();
                }
                if (std::holds_alternative<int64_t>(contents)) {
                    return *(std::get_if<int64_t>(&contents));
                }
                status = U_ILLEGAL_ARGUMENT_ERROR;
            }
            return 0;
        }

        /**
         * Gets the int64 value of this object. If this object is of a numeric
         * type and the magnitude is too large to fit in an int64, then
         * the maximum or minimum int64 value, as appropriate, is returned
         * and the status is set to U_INVALID_FORMAT_ERROR.  If the
         * magnitude fits in an int64, then a casting conversion is
         * performed, with truncation of any fractional part. If this object is
         * not a numeric type, then 0 is returned and
         * the status is set to U_INVALID_FORMAT_ERROR.
         * @param status the error code
         * @return    the int64 value of this object.
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        int64_t         getInt64(UErrorCode& status) const;
        /**
         * Gets the string value of this object. If this object is not of type
         * kString then the result is undefined and the error code is set.
         *
         * @param status Input/output error code.
         * @return          A reference to the string value of this object.
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        const UnicodeString& getString(UErrorCode& status) const {
            if (U_SUCCESS(status)) {
                if (std::holds_alternative<UnicodeString>(contents)) {
                    return *std::get_if<UnicodeString>(&contents);
                }
                status = U_ILLEGAL_ARGUMENT_ERROR;
            }
            return bogusString;
        }

        /**
         * Gets the Date value of this object. If this object is not of type
         * kDate then the result is undefined and the error code is set.
         *
         * @param status Input/output error code.
         * @return    the Date value of this object.
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        UDate getDate(UErrorCode& status) const {
            if (U_SUCCESS(status)) {
                if (isDate()) {
                    return *std::get_if<double>(&contents);
                }
                status = U_ILLEGAL_ARGUMENT_ERROR;
            }
            return 0;
        }

        /**
         * Returns true if the data type of this Formattable object
         * is kDouble
         * @return true if this is a pure numeric object
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        UBool isNumeric() const { return (getType() == UFMT_DOUBLE || getType() == UFMT_LONG || getType() == UFMT_INT64); }

        /**
         * Gets the array value and count of this object. If this object
         * is not of type kArray then the result is undefined and the error code is set.
         *
         * @param count    fill-in with the count of this object.
         * @param status Input/output error code.
         * @return         the array value of this object.
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        const Formattable* getArray(int32_t& count, UErrorCode& status) const;

        /**
         * Returns a pointer to the FormattableObject contained within this
         * formattable, or if this object does not contain a FormattableObject,
         * returns nullptr and sets the error code.
         *
         * @param status Input/output error code.
         * @return a FormattableObject pointer, or nullptr
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        const FormattableObject* getObject(UErrorCode& status) const {
            if (U_SUCCESS(status)) {
                // Can't return a reference since FormattableObject
                // is an abstract class
                if (getType() == UFMT_OBJECT) {
                    return *std::get_if<const FormattableObject*>(&contents);
                    // TODO: should assert that if type is object, object is non-null
                }
                status = U_ILLEGAL_ARGUMENT_ERROR;
            }
            return nullptr;
        }
        /**
         * Non-member swap function.
         * @param f1 will get f2's contents
         * @param f2 will get f1's contents
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        friend inline void swap(Formattable& f1, Formattable& f2) noexcept {
            using std::swap;

            swap(f1.contents, f2.contents);
            swap(f1.holdsDate, f2.holdsDate);
        }
        /**
         * Copy constructor.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        Formattable(const Formattable&);
        /**
         * Assignment operator
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        Formattable& operator=(Formattable) noexcept;
        /**
         * Default constructor. Leaves the Formattable in a
         * valid but undefined state.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        Formattable() : contents(0.0) {}
        /**
         * String constructor.
         *
         * @param s A string to wrap as a Formattable.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        Formattable(const UnicodeString& s) : contents(s) {}
        /**
         * Double constructor.
         *
         * @param d A double value to wrap as a Formattable.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        Formattable(double d) : contents(d) {}
        /**
         * Int64 constructor.
         *
         * @param i An int64 value to wrap as a Formattable.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        Formattable(int64_t i) : contents(i) {}
        /**
         * Date factory method.
         *
         * @param d A UDate value to wrap as a Formattable.
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        static Formattable forDate(UDate d) {
            Formattable f;
            f.contents = d;
            f.holdsDate = true;
            return f;
        }
        /**
         * Creates a Formattable object of an appropriate numeric type from a
         * a decimal number in string form.  The Formattable will retain the
         * full precision of the input in decimal format, even when it exceeds
         * what can be represented by a double or int64_t.
         *
         * @param number  the unformatted (not localized) string representation
         *                     of the Decimal number.
         * @param status  the error code.  Possible errors include U_INVALID_FORMAT_ERROR
         *                if the format of the string does not conform to that of a
         *                decimal number.
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        static Formattable forDecimal(std::string_view number, UErrorCode& status);
        /**
         * Array constructor.
         *
         * @param arr An array of Formattables, which is adopted.
         * @param len The length of the array.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        Formattable(const Formattable* arr, int32_t len) : contents(std::pair(arr, len)) {}
        /**
         * Object constructor.
         *
         * @param obj A FormattableObject (not adopted).
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        Formattable(const FormattableObject* obj) : contents(obj) {}
        /**
         * Destructor.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        virtual ~Formattable();
        /**
         * Converts the Formattable object to an ICU Formattable object.
         * If this has type UFMT_OBJECT or kArray, then `status` is set to
         * U_ILLEGAL_ARGUMENT_ERROR.
         *
         * @param status Input/output error code.
         * @return An icu::Formattable value with the same value as this.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for technology preview only.
         */
        icu::Formattable asICUFormattable(UErrorCode& status) const;
    private:

        std::variant<double,
                     int64_t,
                     UnicodeString,
                     icu::Formattable, // represents a Decimal
                     const FormattableObject*,
                     std::pair<const Formattable*, int32_t>> contents;
        bool holdsDate = false; // otherwise, we get type errors about UDate being a duplicate type
        UnicodeString bogusString; // :((((

        UBool isDecimal() const {
            return std::holds_alternative<icu::Formattable>(contents);
        }
        UBool isDate() const {
            return std::holds_alternative<double>(contents) && holdsDate;
        }
    }; // class Formattable

/**
 * Internal use only, but has to be included here as part of the implementation
 * of the header-only `FunctionOptions::getOptions()` method
 *
 *  A `ResolvedFunctionOption` represents the result of evaluating
 * a single named function option. It pairs the given name with the `Formattable`
 * value resulting from evaluating the option's value.
 *
 * `ResolvedFunctionOption` is immutable and movable. It is not copyable.
 *
 * @internal ICU 75 technology preview
 * @deprecated This API is for technology preview only.
 */
#ifndef U_IN_DOXYGEN
class FunctionValue;
class U_I18N_API ResolvedFunctionOption : public UObject {
  private:

    /* const */ UnicodeString name;
    // This is a pointer because FunctionValue is an abstract class,
    // and is a raw pointer because FunctionValue is forward-declared
    /* const */ FunctionValue* value;

  public:
      const UnicodeString& getName() const { return name; }
      FunctionValue* getValue() const { return value; }
      // Adopts `f`
      ResolvedFunctionOption(const UnicodeString& n, FunctionValue* f);
      ResolvedFunctionOption() {}
      ResolvedFunctionOption(ResolvedFunctionOption&&);
      ResolvedFunctionOption& operator=(ResolvedFunctionOption&& other) noexcept {
          name = std::move(other.name);
          value = std::move(other.value);
          other.value = nullptr;
          return *this;
    }
    ResolvedFunctionOption& operator=(const ResolvedFunctionOption& other) = delete;
    ResolvedFunctionOption(const ResolvedFunctionOption&) = delete;
    virtual ~ResolvedFunctionOption();
}; // class ResolvedFunctionOption
#endif

/**
 * Mapping from option names to `message2::Formattable` objects, obtained
 * by calling `getOptions()` on a `FunctionOptions` object.
 *
 * @internal ICU 75 technology preview
 * @deprecated This API is for technology preview only.
 */
using FunctionOptionsMap = std::map<UnicodeString, const message2::FunctionValue*>;

/**
 * Structure encapsulating named options passed to a custom selector or formatter.
 *
 * This class is immutable and movable but not copyable.
 *
 * @internal ICU 75 technology preview
 * @deprecated This API is for technology preview only.
 */
class U_I18N_API FunctionOptions : public UObject {
 public:
    /**
     * Returns a map of all name-value pairs provided as options to this function.
     * The syntactic order of options is not guaranteed to
     * be preserved.
     *
     * @return           A map from strings to FormattedPlaceholder objects representing
     *                   the results of resolving each option value.
     *
     * @internal ICU 75 technology preview
     * @deprecated This API is for technology preview only.
     */
    FunctionOptionsMap getOptions() const {
        FunctionOptionsMap result;
        for (int32_t i = 0; i < functionOptionsLen; i++) {
            ResolvedFunctionOption& opt = options[i];
            result[opt.getName()] = opt.getValue();
        }
        return result;
    }
    /**
     * Returns a new FunctionOptions object containing all the key-value
     * pairs from `this` and `other`. When `this` and `other` define options with
     * the same name, `this` takes preference.
     * `this` cannot be used after calling this method.
     *
     * @return The result of merging `this` and `other`.
     *
     * @internal ICU 77 technology preview
     * @deprecated This API is for technology preview only.
     */
    FunctionOptions mergeOptions(FunctionOptions&& other, UErrorCode&);
    /**
     * Default constructor.
     * Returns an empty mapping.
     *
     * @internal ICU 75 technology preview
     * @deprecated This API is for technology preview only.
     */
    FunctionOptions() { options = nullptr; }
    /**
     * Destructor.
     *
     * @internal ICU 75 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual ~FunctionOptions();
    /**
     * Move assignment operator:
     * The source FunctionOptions will be left in a valid but undefined state.
     *
     * @internal ICU 75 technology preview
     * @deprecated This API is for technology preview only.
     */
    FunctionOptions& operator=(FunctionOptions&&) noexcept;
    /**
     * Move constructor:
     * The source FunctionOptions will be left in a valid but undefined state.
     *
     * @internal ICU 75 technology preview
     * @deprecated This API is for technology preview only.
     */
    FunctionOptions(FunctionOptions&&);
    /**
     * Copy constructor.
     *
     * @internal ICU 75 technology preview
     * @deprecated This API is for technology preview only.
     */
    FunctionOptions& operator=(const FunctionOptions&) = delete;

 private:
    friend class MessageFormatter;
    friend class StandardFunctions;

    explicit FunctionOptions(UVector&&, UErrorCode&);

    const ResolvedFunctionOption* getResolvedFunctionOptions(int32_t& len) const;
    const FunctionValue* getFunctionOption(const UnicodeString&, UErrorCode&) const;
    // Returns empty string if option doesn't exist
    UnicodeString getStringFunctionOption(const UnicodeString&) const;
    // Sets error code if option doesn't exist
    UnicodeString getStringFunctionOption(const UnicodeString&, UErrorCode&) const;
    int32_t optionsCount() const { return functionOptionsLen; }

    // Named options passed to functions
    // This is not a Hashtable in order to make it possible for code in a public header file
    // to construct a std::map from it, on-the-fly. Otherwise, it would be impossible to put
    // that code in the header because it would have to call internal Hashtable methods.
    ResolvedFunctionOption* options;
    int32_t functionOptionsLen = 0;
}; // class FunctionOptions

    /**
     * Not yet implemented: The result of a message formatting operation. Based on
     * ICU4J's FormattedMessage.java.
     *
     * The class will contain information allowing the result to be viewed as a string,
     * iterator, etc. (TBD)
     *
     * @internal ICU 75 technology preview
     * @deprecated This API is for technology preview only.
     */
    class U_I18N_API FormattedMessage : public icu::FormattedValue {
    public:
        /**
         * Not yet implemented.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        FormattedMessage(UErrorCode& status) {
            if (U_SUCCESS(status)) {
                status = U_UNSUPPORTED_ERROR;
            }
        }
        /**
         * Not yet implemented.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        int32_t length(UErrorCode& status) const {
            if (U_SUCCESS(status)) {
                status = U_UNSUPPORTED_ERROR;
            }
            return -1;
        }
        /**
         * Not yet implemented.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        char16_t charAt(int32_t index, UErrorCode& status) const {
            (void) index;
            if (U_SUCCESS(status)) {
                status = U_UNSUPPORTED_ERROR;
            }
            return 0;
        }
        /**
         * Not yet implemented.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        StringPiece subSequence(int32_t start, int32_t end, UErrorCode& status) const {
            (void) start;
            (void) end;
            if (U_SUCCESS(status)) {
                status = U_UNSUPPORTED_ERROR;
            }
            return "";
        }
        /**
         * Not yet implemented.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        UnicodeString toString(UErrorCode& status) const override {
            if (U_SUCCESS(status)) {
                status = U_UNSUPPORTED_ERROR;
            }
            return {};
        }
        /**
         * Not yet implemented.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        UnicodeString toTempString(UErrorCode& status) const override {
            if (U_SUCCESS(status)) {
                status = U_UNSUPPORTED_ERROR;
            }
            return {};
        }
        /**
         * Not yet implemented.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        Appendable& appendTo(Appendable& appendable, UErrorCode& status) const override {
            if (U_SUCCESS(status)) {
                status = U_UNSUPPORTED_ERROR;
            }
            return appendable;
        }
        /**
         * Not yet implemented.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        UBool nextPosition(ConstrainedFieldPosition& cfpos, UErrorCode& status) const override {
            (void) cfpos;
            if (U_SUCCESS(status)) {
                status = U_UNSUPPORTED_ERROR;
            }
            return false;
        }
        /**
         * Not yet implemented.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        CharacterIterator* toCharacterIterator(UErrorCode& status) {
            if (U_SUCCESS(status)) {
                status = U_UNSUPPORTED_ERROR;
            }
            return nullptr;
        }
        /**
         * Destructor.
         *
         * @internal ICU 75 technology preview
         * @deprecated This API is for ICU internal use only.
         */
        virtual ~FormattedMessage();
    }; // class FormattedMessage

} // namespace message2

U_NAMESPACE_END

#endif // U_HIDE_DEPRECATED_API

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_FORMATTABLE_H

// eof
