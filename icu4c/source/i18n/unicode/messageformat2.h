// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#ifndef U_HIDE_DEPRECATED_API

#ifndef MESSAGEFORMAT2_H
#define MESSAGEFORMAT2_H

#if U_SHOW_CPLUSPLUS_API

/**
 * \file
 * \brief C++ API: Formats messages in a language-neutral way using the draft MessageFormat 2.0.
 */

#if !UCONFIG_NO_FORMATTING

#include "unicode/format.h"
#include "unicode/messageformat2_data_model.h"
#include "unicode/parseerr.h"
#include "unicode/unistr.h"
#include "unicode/utypes.h"

U_NAMESPACE_BEGIN namespace message2 {

/**
 * <p>MessageFormat2 is a Technical Preview API implementing MessageFormat 2.0.
 * Since it is not final, documentation has not yet been added everywhere.
 *
 * <p>See <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/spec/syntax.md">the
 * description of the syntax with examples and use cases</a> and the corresponding
 * <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/spec/message.abnf">ABNF</a> grammar.</p>
 *
 * @internal ICU 74.0 technology preview
 * @deprecated This API is for technology preview only.
 */

class U_I18N_API MessageFormat2 : public Format {
  public:
    /**
     * Constructs a new MessageFormat2 using the given pattern and the
     * default locale.
     *
     * @param pattern   Pattern used to construct object.
     * @param parseError Struct to receive information on the position
     *                   of an error within the pattern.
     * @param status    Input/output error code.  If the
     *                  pattern cannot be parsed, set to failure code.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    MessageFormat2(const UnicodeString &pattern, UParseError &parseError, UErrorCode &status);

    /**
     * Destructor.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual ~MessageFormat2();

    // Not yet implemented
    virtual bool operator==(const Format &other) const override;
    // Not yet implemented
    virtual bool operator!=(const Format &other) const;

    // Not yet implemented
    virtual UnicodeString &format(const Formattable &obj, UnicodeString &appendTo, FieldPosition &pos,
                                  UErrorCode &status) const override;

    // Not yet implemented
    virtual void parseObject(const UnicodeString &source, Formattable &result,
                             ParsePosition &pos) const override;

    /**
     * Clones this Format object polymorphically.  The caller owns the
     * result and should delete it when done.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */

    virtual MessageFormat2 *clone() const override;

    /**
     * Constructs a new MessageFormat2 from an existing one.
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    MessageFormat2(const MessageFormat2 &);

    /**
     * Returns a unique class ID POLYMORPHICALLY.  Pure virtual override.
     * This method is to implement a simple version of RTTI, since not all
     * C++ compilers support genuine RTTI.  Polymorphic operator==() and
     * clone() methods call this method.
     *
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    virtual UClassID getDynamicClassID(void) const override;

    /**
     * Return the class ID for this class.  This is useful only for
     * comparing to a return value from getDynamicClassID().  For example:
     * <pre>
     * .   Base* polymorphic_pointer = createPolymorphicObject();
     * .   if (polymorphic_pointer->getDynamicClassID() ==
     * .      Derived::getStaticClassID()) ...
     * </pre>
     *
     * @internal ICU 74.0 technology preview
     * @deprecated This API is for technology preview only.
     */
    static UClassID U_EXPORT2 getStaticClassID(void);

  private:
    MessageFormat2() = delete; // default constructor not implemented

    // Do not define default assignment operator
    const MessageFormat2 &operator=(const MessageFormat2 &) = delete;

    // The parser validates the message and builds the data model
    // from it.
    void parse(const UnicodeString &, UParseError &, UErrorCode &);

    // Data model, representing the parsed message
    MessageFormatDataModel dataModel;
}; // class MessageFormat2

} // namespace message2
U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif /* U_SHOW_CPLUSPLUS_API */

#endif // MESSAGEFORMAT2_H

#endif // U_HIDE_DEPRECATED_API
// eof
