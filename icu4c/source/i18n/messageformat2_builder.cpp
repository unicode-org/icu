// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/messageformat2.h"
#include "unicode/messageformat2_data_model.h"
#include "uvector.h" // U_ASSERT

U_NAMESPACE_BEGIN namespace message2 {

// -------------------------------------
// Creates a MessageFormat instance based on the pattern.

// Returns a new (uninitialized) builder
MessageFormatter::Builder* MessageFormatter::builder(UErrorCode& errorCode) {
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    LocalPointer<MessageFormatter::Builder> tree(new Builder());
    if (!tree.isValid()) {
        errorCode = U_MEMORY_ALLOCATION_ERROR;
        return nullptr;
    }
    return tree.orphan();
}

MessageFormatter::Builder& MessageFormatter::Builder::setPattern(const UnicodeString& pat) {
    hasPattern = true;
    pattern = pat;
    dataModel = nullptr;

    return *this;
}

// Precondition: `reg` is non-null
// Does not adopt `reg`
MessageFormatter::Builder& MessageFormatter::Builder::setFunctionRegistry(const FunctionRegistry* reg) {
    U_ASSERT(reg != nullptr);
    customFunctionRegistry = reg;
    return *this;
}

MessageFormatter::Builder& MessageFormatter::Builder::setLocale(const Locale& loc) {
    locale = loc;
    return *this;
}

// Does not adopt `dataModel`
MessageFormatter::Builder& MessageFormatter::Builder::setDataModel(const MessageFormatDataModel* newDataModel) {
    U_ASSERT(newDataModel != nullptr);
    hasPattern = false;
    dataModel = newDataModel;

    return *this;
}

/*
  This build() method is non-destructive, which entails the risk that
  its borrowed FunctionRegistry and (if the setDataModel() method was called)
  MessageFormatDataModel pointers could become invalidated.
*/
MessageFormatter* MessageFormatter::Builder::build(UParseError& parseError, UErrorCode& errorCode) const {
    NULL_ON_ERROR(errorCode);

    LocalPointer<MessageFormatter> mf(new MessageFormatter(*this, parseError, errorCode));
    if (U_FAILURE(errorCode)) {
        return nullptr;
    }
    return mf.orphan();
}

void MessageFormatter::initErrors(UErrorCode& errorCode) {
    CHECK_ERROR(errorCode);
    errors.adoptInstead(Errors::create(errorCode));
}

MessageFormatter::MessageFormatter(const MessageFormatter::Builder& builder, UParseError &parseError,
                                   UErrorCode &success) : locale(builder.locale), customFunctionRegistry(builder.customFunctionRegistry) {
    CHECK_ERROR(success);

    // Set up the standard function registry
    LocalPointer<FunctionRegistry::Builder> standardFunctionsBuilder(FunctionRegistry::builder(success));
    CHECK_ERROR(success);

    standardFunctionsBuilder->setFormatter(UnicodeString("datetime"), new StandardFunctions::DateTimeFactory(), success)
        .setFormatter(UnicodeString("number"), new StandardFunctions::NumberFactory(), success)
        .setFormatter(UnicodeString("identity"), new StandardFunctions::IdentityFactory(), success)
        .setSelector(UnicodeString("plural"), new StandardFunctions::PluralFactory(UPLURAL_TYPE_CARDINAL), success)
        .setSelector(UnicodeString("selectordinal"), new StandardFunctions::PluralFactory(UPLURAL_TYPE_ORDINAL), success)
        .setSelector(UnicodeString("select"), new StandardFunctions::TextFactory(), success)
        .setSelector(UnicodeString("gender"), new StandardFunctions::TextFactory(), success);
    standardFunctionRegistry.adoptInstead(standardFunctionsBuilder->build(success));
    CHECK_ERROR(success);
    standardFunctionRegistry->checkStandard();

    initErrors(success);
    CHECK_ERROR(success);

    // Validate pattern and build data model
    // First, check that exactly one of the pattern and data model are set, but not both

    bool dataModelSet = builder.dataModel != nullptr;

    if ((!builder.hasPattern && !dataModelSet)
        || (builder.hasPattern && dataModelSet)) {
      success = U_INVALID_STATE_ERROR;
      return;
    }

    // If data model was set, just assign it
    if (dataModelSet) {
        ownedDataModel = false;
        borrowedDataModel = builder.dataModel;
        return;
    }
    borrowedDataModel = nullptr;

    LocalPointer<MessageFormatDataModel::Builder> tree(MessageFormatDataModel::builder(success));
    if (U_FAILURE(success)) {
      return;
    }

    // Initialize formatter cache
    cachedFormatters.adoptInstead(new CachedFormatters(success));

    // Parse the pattern
    LocalPointer<Parser> parser(Parser::create(builder.pattern, *tree, normalizedInput, *errors, success));
    CHECK_ERROR(success);
    parser->parse(parseError, success);

    // Build the data model based on what was parsed
    LocalPointer<MessageFormatDataModel> dataModelPtr(tree->build(success));
    if (U_SUCCESS(success)) {
        ownedDataModel = true;
        dataModel.adoptInstead(dataModelPtr.orphan());
    }
}

const MessageFormatDataModel& MessageFormatter::getDataModel() const {
    U_ASSERT(dataModelOK());
    if (ownedDataModel) {
        return *dataModel;
    }
    return *borrowedDataModel;
}

bool MessageFormatter::dataModelOK() const {
    if (ownedDataModel) {
        return dataModel.isValid() && borrowedDataModel == nullptr;
    }
    return !dataModel.isValid() && borrowedDataModel != nullptr;
}

MessageFormatter::~MessageFormatter() {}

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */
