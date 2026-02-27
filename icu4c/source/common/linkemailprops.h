// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// linkemailprops.h
// created: 2025 for UTS #58 / Unicode 17.0

#ifndef LINKEMAILPROPS_H
#define LINKEMAILPROPS_H

#include "unicode/utypes.h"
#include "unicode/ucptrie.h"
#include "unicode/uobject.h"

U_NAMESPACE_BEGIN

/**
 * Link_Email binary property constants and data-file identifiers.
 * A code point has Link_Email=Yes (1) if it is allowed in an email local part.
 * All other code points default to No (0).
 */
class LinkEmailProps : public UMemory {
public:
    /**
     * Indexes into the binary data indexes[] array.
     * Values are byte offsets from the start of the indexes[] array.
     */
    enum {
        IX_COUNT,       // 0: length of indexes[] (== IX_LINK_EMAIL_COUNT)
        IX_CPTRIE_TOP,  // 1: limit offset of the Link_Email UCPTrie
        IX_TRIE2_TOP,   // 2: reserved for a second future trie (= IX_CPTRIE_TOP until used)
        IX_TRIE3_TOP,   // 3: reserved for a third future trie  (= IX_TRIE2_TOP  until used)
        IX_TOTAL_SIZE,  // 4: total data size (= limit of last trie)
        // reserved
        IX_LINK_EMAIL_COUNT = 8
    };

    static constexpr char DATA_TYPE[] = "icu";
    static constexpr char DATA_NAME[] = "ulinkemail";
    static constexpr uint8_t DATA_FORMAT[4] = { 'L', 'n', 'k', 'E' };
    static constexpr uint8_t FORMAT_VERSION[4] = { 1, 0, 0, 0 };
};

U_NAMESPACE_END

#endif  // LINKEMAILPROPS_H
