// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 1997-2012, International Business Machines Corporation and
 * others. All Rights Reserved.
 * Copyright (C) 2010 , Yahoo! Inc.
 ********************************************************************
 *
 * File SELFMT.CPP
 *
 * Modification History:
 *
 *   Date        Name        Description
 *   11/11/09    kirtig      Finished first cut of implementation.
 *   11/16/09    kirtig      Improved version
 ********************************************************************/

#include "utypeinfo.h"  // for 'typeid' to work

#include "unicode/messagepattern.h"
#include "unicode/rbnf.h"
#include "unicode/selfmt.h"
#include "unicode/uchar.h"
#include "unicode/ucnv_err.h"
#include "unicode/umsg.h"
#include "unicode/ustring.h"
#include "unicode/utypes.h"
#include "cmemory.h"
#include "messageimpl.h"
#include "patternprops.h"
#include "selfmtimpl.h"
#include "uassert.h"
#include "ustrfmt.h"
#include "util.h"
#include "uvector.h"

#if !UCONFIG_NO_FORMATTING

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(SelectFormat)

static const char16_t SELECT_KEYWORD_OTHER[] = {LOW_O, LOW_T, LOW_H, LOW_E, LOW_R, 0};

SelectFormat::SelectFormat(const UnicodeString& pat,
                           UErrorCode& status) : msgPattern(status) {
   applyPattern(pat, status);
}

SelectFormat::SelectFormat(const SelectFormat& other) : Format(other),
                                                        msgPattern(other.msgPattern) {
}

SelectFormat::~SelectFormat() {
}

void
SelectFormat::applyPattern(const UnicodeString& newPattern, UErrorCode& status) {
    if (U_FAILURE(status)) {
      return;
    }

    msgPattern.parseSelectStyle(newPattern, nullptr, status);
    if (U_FAILURE(status)) {
        msgPattern.clear();
    }
}

UnicodeString&
SelectFormat::format(const Formattable& obj,
                   UnicodeString& appendTo,
                   FieldPosition& pos,
                   UErrorCode& status) const
{
    if (U_FAILURE(status)) {
        return appendTo;
    }
    if (obj.getType() == Formattable::kString) {
        return format(obj.getString(status), appendTo, pos, status);
    } else {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return appendTo;
    }
}

UnicodeString&
SelectFormat::format(const UnicodeString& keyword,
                     UnicodeString& appendTo,
                     FieldPosition& /*pos */,
                     UErrorCode& status) const {
    if (U_FAILURE(status)) {
        return appendTo;
    }
    // Check for the validity of the keyword
    if (!PatternProps::isIdentifier(keyword.getBuffer(), keyword.length())) {
        status = U_ILLEGAL_ARGUMENT_ERROR;  // Invalid formatting argument.
    }
    if (msgPattern.countParts() == 0) {
        status = U_INVALID_STATE_ERROR;
        return appendTo;
    }
    int32_t msgStart = findSubMessage(msgPattern, 0, keyword, status);
    if (!MessageImpl::jdkAposMode(msgPattern)) {
        int32_t patternStart = msgPattern.getPart(msgStart).getLimit();
        int32_t msgLimit = msgPattern.getLimitPartIndex(msgStart);
        appendTo.append(msgPattern.getPatternString(),
                        patternStart,
                        msgPattern.getPatternIndex(msgLimit) - patternStart);
        return appendTo;
    }
    // JDK compatibility mode: Remove SKIP_SYNTAX.
    return MessageImpl::appendSubMessageWithoutSkipSyntax(msgPattern, msgStart, appendTo);
}

UnicodeString&
SelectFormat::toPattern(UnicodeString& appendTo) {
    if (0 == msgPattern.countParts()) {
        appendTo.setToBogus();
    } else {
        appendTo.append(msgPattern.getPatternString());
    }
    return appendTo;
}


int32_t SelectFormat::findSubMessage(const MessagePattern& pattern, int32_t partIndex,
                                     const UnicodeString& keyword, UErrorCode& ec) {
    if (U_FAILURE(ec)) {
        return 0;
    }
    UnicodeString other(false, SELECT_KEYWORD_OTHER, 5);
    int32_t count = pattern.countParts();
    int32_t msgStart=0;
    // Iterate over (ARG_SELECTOR, message) pairs until ARG_LIMIT or end of select-only pattern.
    do {
        const MessagePattern::Part& part=pattern.getPart(partIndex++);
        const UMessagePatternPartType type=part.getType();
        if(type==UMSGPAT_PART_TYPE_ARG_LIMIT) {
            break;
        }
        // part is an ARG_SELECTOR followed by a message
        if(pattern.partSubstringMatches(part, keyword)) {
            // keyword matches
            return partIndex;
        } else if(msgStart==0 && pattern.partSubstringMatches(part, other)) {
            msgStart=partIndex;
        }
        partIndex=pattern.getLimitPartIndex(partIndex);
    } while(++partIndex<count);
    return msgStart;
}

SelectFormat* SelectFormat::clone() const
{
    return new SelectFormat(*this);
}

SelectFormat&
SelectFormat::operator=(const SelectFormat& other) {
    if (this != &other) {
        msgPattern = other.msgPattern;
    }
    return *this;
}

bool
SelectFormat::operator==(const Format& other) const {
    if (this == &other) {
        return true;
    }
    if (!Format::operator==(other)) {
        return false;
    }
    const SelectFormat& o = static_cast<const SelectFormat&>(other);
    return msgPattern == o.msgPattern;
}

bool
SelectFormat::operator!=(const Format& other) const {
    return  !operator==(other);
}

void
SelectFormat::parseObject(const UnicodeString& /*source*/,
                        Formattable& /*result*/,
                        ParsePosition& pos) const
{
    // Parsing not supported.
    pos.setErrorIndex(pos.getIndex());
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_FORMATTING */

//eof

// Keep file-local macros from leaking into other unity sources.
#undef DOT
#undef SINGLE_QUOTE
#undef SLASH
#undef BACKSLASH
#undef SPACE
#undef TAB
#undef QUOTATION_MARK
#undef ASTERISK
#undef COMMA
#undef HYPHEN
#undef U_ZERO
#undef U_ONE
#undef U_TWO
#undef U_THREE
#undef U_FOUR
#undef U_FIVE
#undef U_SIX
#undef U_SEVEN
#undef U_EIGHT
#undef U_NINE
#undef COLON
#undef SEMI_COLON
#undef CAP_A
#undef CAP_B
#undef CAP_R
#undef CAP_Z
#undef LOWLINE
#undef LEFTBRACE
#undef RIGHTBRACE
#undef LOW_A
#undef LOW_B
#undef LOW_C
#undef LOW_D
#undef LOW_E
#undef LOW_F
#undef LOW_G
#undef LOW_H
#undef LOW_I
#undef LOW_J
#undef LOW_K
#undef LOW_L
#undef LOW_M
#undef LOW_N
#undef LOW_O
#undef LOW_P
#undef LOW_Q
#undef LOW_R
#undef LOW_S
#undef LOW_T
#undef LOW_U
#undef LOW_V
#undef LOW_W
#undef LOW_X
#undef LOW_Y
#undef LOW_Z
