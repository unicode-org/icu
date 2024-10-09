// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#if !UCONFIG_NO_MF2

#include "unicode/messageformat2.h"
#include "messageformat2_allocation.h"
#include "messageformat2_checker.h"
#include "messageformat2_errors.h"
#include "messageformat2_evaluation.h"
#include "messageformat2_function_registry_internal.h"
#include "messageformat2_macros.h"
#include "messageformat2_parser.h"
#include "messageformat2_serializer.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN

namespace message2 {

    // MessageFormatter::Builder

    // -------------------------------------
    // Creates a MessageFormat instance based on the pattern.

    void MessageFormatter::Builder::clearState() {
        normalizedInput.remove();
        delete errors;
        errors = nullptr;
    }

    MessageFormatter::Builder& MessageFormatter::Builder::setPattern(const UnicodeString& pat,
                                                                     UParseError& parseError,
                                                                     UErrorCode& errorCode) {
        clearState();
        // Create errors
        errors = create<StaticErrors>(StaticErrors(errorCode), errorCode);
        THIS_ON_ERROR(errorCode);

        // Parse the pattern
        MFDataModel::Builder tree(errorCode);
        Parser(pat, tree, *errors, normalizedInput).parse(parseError, errorCode);

        // Fail on syntax errors
        if (errors->hasSyntaxError()) {
            errors->checkErrors(errorCode);
            // Check that the checkErrors() method set the error code
            U_ASSERT(U_FAILURE(errorCode));
        }

        // Build the data model based on what was parsed
        dataModel = tree.build(errorCode);
        hasDataModel = true;
        hasPattern = true;
        pattern = pat;

        return *this;
    }

    // Precondition: `reg` is non-null
    // Does not adopt `reg`
    MessageFormatter::Builder& MessageFormatter::Builder::setFunctionRegistry(const MFFunctionRegistry& reg) {
        customMFFunctionRegistry = &reg;
        return *this;
    }

    MessageFormatter::Builder& MessageFormatter::Builder::setLocale(const Locale& loc) {
        locale = loc;
        return *this;
    }

    MessageFormatter::Builder& MessageFormatter::Builder::setDataModel(MFDataModel&& newDataModel) {
        clearState();
        hasPattern = false;
        hasDataModel = true;
        dataModel = std::move(newDataModel);

        return *this;
    }

    MessageFormatter::Builder&
        MessageFormatter::Builder::setErrorHandlingBehavior(
           MessageFormatter::UMFErrorHandlingBehavior type) {
               signalErrors = type == U_MF_STRICT;
               return *this;
    }

    /*
      This build() method is non-destructive, which entails the risk that
      its borrowed MFFunctionRegistry and (if the setDataModel() method was called)
      MFDataModel pointers could become invalidated.
    */
    MessageFormatter MessageFormatter::Builder::build(UErrorCode& errorCode) const {
        return MessageFormatter(*this, errorCode);
    }

    MessageFormatter::Builder::Builder(UErrorCode& errorCode) : locale(Locale::getDefault()), customMFFunctionRegistry(nullptr) {
        // Initialize errors
        errors = new StaticErrors(errorCode);
        CHECK_ERROR(errorCode);
        if (errors == nullptr) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
        }
    }

    MessageFormatter::Builder::~Builder() {
        if (errors != nullptr) {
            delete errors;
            errors = nullptr;
        }
    }

    // MessageFormatter

    MessageFormatter::MessageFormatter(const MessageFormatter::Builder& builder, UErrorCode &success) : locale(builder.locale), customMFFunctionRegistry(builder.customMFFunctionRegistry) {
        CHECK_ERROR(success);

        // Set up the standard function registry
        MFFunctionRegistry::Builder standardFunctionsBuilder(success);

        LocalPointer<FunctionFactory> dateTime(StandardFunctions::DateTimeFactory::dateTime(success));
        LocalPointer<FunctionFactory> date(StandardFunctions::DateTimeFactory::date(success));
        LocalPointer<FunctionFactory> time(StandardFunctions::DateTimeFactory::time(success));
        LocalPointer<FunctionFactory> number(StandardFunctions::NumberFactory::number(success));
        LocalPointer<FunctionFactory> integer(StandardFunctions::NumberFactory::integer(success));
        LocalPointer<FunctionFactory> string(StandardFunctions::StringFactory::string(success));
        CHECK_ERROR(success);
        standardFunctionsBuilder.adoptFunctionFactory(FunctionName(UnicodeString("datetime")),
                                                      dateTime.orphan(), success)
            .adoptFunctionFactory(FunctionName(UnicodeString("date")), date.orphan(), success)
            .adoptFunctionFactory(FunctionName(UnicodeString("time")), time.orphan(), success)
            .adoptFunctionFactory(FunctionName(UnicodeString("number")),
                                  number.orphan(), success)
            .adoptFunctionFactory(FunctionName(UnicodeString("integer")),
                                  integer.orphan(), success)
            .adoptFunctionFactory(FunctionName(UnicodeString("string")),
                                  string.orphan(), success);
        CHECK_ERROR(success);
        standardMFFunctionRegistry = standardFunctionsBuilder.build();
        CHECK_ERROR(success);
        standardMFFunctionRegistry.checkStandard();

        normalizedInput = builder.normalizedInput;
        signalErrors = builder.signalErrors;

        // Build data model
        // First, check that there is a data model
        // (which might have been set by setDataModel(), or to
        // the data model parsed from the pattern by setPattern())

        if (!builder.hasDataModel) {
            success = U_INVALID_STATE_ERROR;
            return;
        }

        dataModel = builder.dataModel;
        if (builder.errors != nullptr) {
            errors = new StaticErrors(*builder.errors, success);
        } else {
            // Initialize errors
            LocalPointer<StaticErrors> errorsNew(new StaticErrors(success));
            CHECK_ERROR(success);
            errors = errorsNew.orphan();
        }

        // Note: we currently evaluate variables lazily,
        // without memoization. This call is still necessary
        // to check out-of-scope uses of local variables in
        // right-hand sides (unresolved variable errors can
        // only be checked when arguments are known)

        // Check for resolution errors
        Checker(dataModel, *errors).check(success);
    }

    void MessageFormatter::cleanup() noexcept {
        if (errors != nullptr) {
            delete errors;
            errors = nullptr;
        }
    }

    MessageFormatter& MessageFormatter::operator=(MessageFormatter&& other) noexcept {
        cleanup();

        locale = std::move(other.locale);
        standardMFFunctionRegistry = std::move(other.standardMFFunctionRegistry);
        customMFFunctionRegistry = other.customMFFunctionRegistry;
        dataModel = std::move(other.dataModel);
        normalizedInput = std::move(other.normalizedInput);
        signalErrors = other.signalErrors;
        errors = other.errors;
        other.errors = nullptr;
        return *this;
    }

    const MFDataModel& MessageFormatter::getDataModel() const { return dataModel; }

    UnicodeString MessageFormatter::getPattern() const {
        // Converts the current data model back to a string
        UnicodeString result;
        Serializer serializer(getDataModel(), result);
        serializer.serialize();
        return result;
    }

    // Precondition: custom function registry exists
    const MFFunctionRegistry& MessageFormatter::getCustomMFFunctionRegistry() const {
        U_ASSERT(hasCustomMFFunctionRegistry());
        return *customMFFunctionRegistry;
    }

    MessageFormatter::~MessageFormatter() {
        cleanup();
    }

    // ---------------------------------------------------
    // Function registry

    bool MessageFormatter::isBuiltInFunction(const FunctionName& functionName) const {
        return standardMFFunctionRegistry.hasFunction(functionName);
    }

    FunctionFactory*
    MessageFormatter::lookupFunctionFactory(const FunctionName& functionName,
                                            UErrorCode& status) const {
        NULL_ON_ERROR(status);

        if (isBuiltInFunction(functionName)) {
            return standardMFFunctionRegistry.getFunction(functionName);
        }
        if (hasCustomMFFunctionRegistry()) {
            const MFFunctionRegistry& customMFFunctionRegistry = getCustomMFFunctionRegistry();
            FunctionFactory* function = customMFFunctionRegistry.getFunction(functionName);
            if (function != nullptr) {
                return function;
            }
        }
        // Either there is no custom function registry and the function
        // isn't built-in, or the function doesn't exist in either the built-in
        // or custom registry.
        // Unknown function error
        status = U_MF_UNKNOWN_FUNCTION_ERROR;
        return nullptr;
    }

    bool MessageFormatter::getDefaultFormatterNameByType(const UnicodeString& tag,
                                                         FunctionName& result) const {
        if (hasCustomMFFunctionRegistry()) {
            const MFFunctionRegistry& customMFFunctionRegistry = getCustomMFFunctionRegistry();
            return customMFFunctionRegistry.getDefaultFormatterNameByType(tag, result);
        }
        return false;
    }

    bool MessageFormatter::isCustomFunction(const FunctionName& fn) const {
        return hasCustomMFFunctionRegistry() && getCustomMFFunctionRegistry().getFunction(fn) != nullptr;
    }

} // namespace message2

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_MF2 */

#endif /* #if !UCONFIG_NO_FORMATTING */
