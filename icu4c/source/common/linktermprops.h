// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

// linktermprops.h
// created: 2025 for UTS #58 / Unicode 17.0

#ifndef LINKTERMPROPS_H
#define LINKTERMPROPS_H

#include "unicode/utypes.h"
#include "unicode/ucptrie.h"
#include "unicode/uobject.h"

/**
 * Values of the Link_Term property (UTS #58 / proposed Unicode 19.0).
 * The default value for unlisted code points is ULINK_TERM_HARD.
 */
typedef enum ULinkTerm {
    ULINK_TERM_HARD    = 0,  /**< Terminates a URL unconditionally. Default. */
    ULINK_TERM_INCLUDE = 1,  /**< May appear in a URL (letters, digits, …). */
    ULINK_TERM_SOFT    = 2,  /**< Terminates only when followed by Hard. */
    ULINK_TERM_CLOSE   = 3,  /**< Closing bracket; terminates if unmatched. */
    ULINK_TERM_OPEN    = 4,  /**< Opening bracket. */
    ULINK_TERM_COUNT
} ULinkTerm;

U_NAMESPACE_BEGIN

class LinkTermProps : public UMemory {
public:
    /**
     * Indexes into the binary data indexes[] array.
     * Values are byte offsets from the start of the indexes[] array.
     */
    enum {
        IX_COUNT,       // 0: length of indexes[] (== IX_LINK_TERM_COUNT)
        IX_CPTRIE_TOP,  // 1: limit offset of the Link_Term UCPTrie
        IX_TRIE2_TOP,   // 2: reserved for a second future trie (= IX_CPTRIE_TOP until used)
        IX_TRIE3_TOP,   // 3: reserved for a third future trie  (= IX_TRIE2_TOP  until used)
        IX_TOTAL_SIZE,  // 4: total data size (= limit of last trie)
        // reserved
        IX_LINK_TERM_COUNT = 8
    };

    static constexpr char DATA_TYPE[] = "icu";
    static constexpr char DATA_NAME[] = "ulinkterm";
    static constexpr uint8_t DATA_FORMAT[4] = { 'L', 'n', 'k', 'T' };
    static constexpr uint8_t FORMAT_VERSION[4] = { 1, 0, 0, 0 };
};

U_NAMESPACE_END

#endif  // LINKTERMPROPS_H
