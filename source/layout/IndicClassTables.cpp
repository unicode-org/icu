/*
 *
 * (C) Copyright IBM Corp. 1998-2004 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEScripts.h"
#include "OpenTypeTables.h"
#include "OpenTypeUtilities.h"
#include "IndicReordering.h"

U_NAMESPACE_BEGIN

#define ARRAY_SIZE(array) (sizeof array  / sizeof array[0])

enum
{
    // Split matra table indices
    _x1 = 1 << IndicClassTable::CF_INDEX_SHIFT,
    _x2 = 2 << IndicClassTable::CF_INDEX_SHIFT,
    _x3 = 3 << IndicClassTable::CF_INDEX_SHIFT,
    _x4 = 4 << IndicClassTable::CF_INDEX_SHIFT,
    _x5 = 5 << IndicClassTable::CF_INDEX_SHIFT,
    _x6 = 6 << IndicClassTable::CF_INDEX_SHIFT,
    _x7 = 7 << IndicClassTable::CF_INDEX_SHIFT,
    _x8 = 8 << IndicClassTable::CF_INDEX_SHIFT,
    _x9 = 9 << IndicClassTable::CF_INDEX_SHIFT,

    // simple classes
    _xx = IndicClassTable::CC_RESERVED,
    _ma = IndicClassTable::CC_VOWEL_MODIFIER | IndicClassTable::CF_POS_ABOVE,
    _mp = IndicClassTable::CC_VOWEL_MODIFIER | IndicClassTable::CF_POS_AFTER,
    _sa = IndicClassTable::CC_STRESS_MARK | IndicClassTable::CF_POS_ABOVE,
    _sb = IndicClassTable::CC_STRESS_MARK | IndicClassTable::CF_POS_BELOW,
    _iv = IndicClassTable::CC_INDEPENDENT_VOWEL,
    _ct = IndicClassTable::CC_CONSONANT | IndicClassTable::CF_CONSONANT,
    _cn = IndicClassTable::CC_CONSONANT_WITH_NUKTA | IndicClassTable::CF_CONSONANT,
    _nu = IndicClassTable::CC_NUKTA,
    _dv = IndicClassTable::CC_DEPENDENT_VOWEL,
    _dl = _dv | IndicClassTable::CF_POS_BEFORE,
    _db = _dv | IndicClassTable::CF_POS_BELOW,
    _da = _dv | IndicClassTable::CF_POS_ABOVE,
    _dr = _dv | IndicClassTable::CF_POS_AFTER,
    _lm = _dv | IndicClassTable::CF_LENGTH_MARK,
    _vr = IndicClassTable::CC_VIRAMA,

    // split matras
    _s1 = _dv | _x1,
    _s2 = _dv | _x2,
    _s3 = _dv | _x3,
    _s4 = _dv | _x4,
    _s5 = _dv | _x5,
    _s6 = _dv | _x6,
    _s7 = _dv | _x7,
    _s8 = _dv | _x8,
    _s9 = _dv | _x9,

    // consonants with special forms
    // NOTE: this assumes that no consonants with nukta have
    // special forms... (Bengali RA?)
    _bb = _ct | IndicClassTable::CF_BELOW_BASE,
    _pb = _ct | IndicClassTable::CF_POST_BASE,
    _vt = _bb | IndicClassTable::CF_VATTU,
    _rv = _vt | IndicClassTable::CF_REPH,
    _rp = _pb | IndicClassTable::CF_REPH,
    _rb = _bb | IndicClassTable::CF_REPH
};

//
// Character class tables
//
static const IndicClassTable::CharClass devaCharClasses[] =
{
    _xx, _ma, _ma, _mp, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _iv, // 0900 - 090F
    _iv, _iv, _iv, _iv, _iv, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, // 0910 - 091F
    _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _cn, _ct, _ct, _ct, _ct, _ct, _ct, // 0920 - 092F
    _rv, _cn, _ct, _ct, _cn, _ct, _ct, _ct, _ct, _ct, _xx, _xx, _nu, _xx, _dr, _dl, // 0930 - 093F
    _dr, _db, _db, _db, _db, _da, _da, _da, _da, _dr, _dr, _dr, _dr, _vr, _xx, _xx, // 0940 - 094F
    _xx, _sa, _sb, _sa, _sa, _xx, _xx, _xx, _cn, _cn, _cn, _cn, _cn, _cn, _cn, _cn, // 0950 - 095F
    _iv, _iv, _db, _db, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, // 0960 - 096F
    _xx                                                                             // 0970
};

static const IndicClassTable::CharClass bengCharClasses[] =
{
    _xx, _ma, _mp, _mp, _xx, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _xx, _xx, _iv, // 0980 - 098F
    _iv, _xx, _xx, _iv, _iv, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, // 0990 - 099F
    _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _xx, _ct, _ct, _bb, _ct, _ct, _pb, // 09A0 - 09AF
    _rv, _xx, _ct, _xx, _xx, _xx, _ct, _ct, _ct, _ct, _xx, _xx, _nu, _xx, _dr, _dl, // 09B0 - 09BF
    _dr, _db, _db, _db, _db, _xx, _xx, _dl, _dl, _xx, _xx, _s1, _s2, _vr, _xx, _xx, // 09C0 - 09CF
    _xx, _xx, _xx, _xx, _xx, _xx, _xx, _dr, _xx, _xx, _xx, _xx, _cn, _cn, _xx, _cn, // 09D0 - 09DF
    _iv, _iv, _dv, _dv, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, // 09E0 - 09EF
    _ct, _ct, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx                           // 09F0 - 09FA
};

static const IndicClassTable::CharClass punjCharClasses[] =
{
    _xx, _xx, _ma, _xx, _xx, _iv, _iv, _iv, _iv, _iv, _iv, _xx, _xx, _xx, _xx, _iv, // 0A00 - 0A0F
    _iv, _xx, _xx, _iv, _iv, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, // 0A10 - 0A1F
    _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _xx, _ct, _ct, _ct, _ct, _ct, _bb, // 0A20 - 0A2F
    _vt, _xx, _ct, _ct, _xx, _bb, _ct, _xx, _ct, _bb, _xx, _xx, _nu, _xx, _dr, _dl, // 0A30 - 0A3F
    _dr, _db, _db, _xx, _xx, _xx, _xx, _da, _da, _xx, _xx, _da, _da, _vr, _xx, _xx, // 0A40 - 0A4F
    _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _cn, _cn, _cn, _cn, _xx, _cn, _xx, // 0A50 - 0A5F
    _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, // 0A60 - 0A6F
    _ma, _ma, _ct, _ct, _xx                                                         // 0A70 - 0A74
};

static const IndicClassTable::CharClass gujrCharClasses[] =
{
    _xx, _ma, _ma, _mp, _xx, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _xx, _iv, _xx, _iv, // 0A80 - 0A8F
    _iv, _iv, _xx, _iv, _iv, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, // 0A90 - 0A9F
    _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _xx, _ct, _ct, _ct, _ct, _ct, _ct, // 0AA0 - 0AAF
    _rv, _xx, _ct, _ct, _xx, _ct, _ct, _ct, _ct, _ct, _xx, _xx, _nu, _xx, _dr, _dl, // 0AB0 - 0ABF
    _dr, _db, _db, _db, _db, _da, _xx, _da, _da, _dr, _xx, _dr, _dr, _vr, _xx, _xx, // 0AC0 - 0ACF
    _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, // 0AD0 - 0ADF
    _iv, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx  // 0AE0 - 0AEF
};

static const IndicClassTable::CharClass oryaCharClasses[] =
{
    _xx, _ma, _ma, _mp, _xx, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _xx, _xx, _iv, // 0B00 - 0B0F
    _iv, _xx, _xx, _iv, _iv, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, // 0B10 - 0B1F
    _ct, _ct, _ct, _ct, _bb, _ct, _ct, _ct, _bb, _xx, _ct, _ct, _bb, _bb, _bb, _pb, // 0B20 - 0B2F
    _rv, _xx, _bb, _bb, _xx, _xx, _ct, _ct, _ct, _ct, _xx, _xx, _nu, _xx, _dr, _da, // 0B30 - 0B3F
    _dr, _db, _db, _db, _xx, _xx, _xx, _dl, _s1, _xx, _xx, _s2, _s3, _vr, _xx, _xx, // 0B40 - 0B4F
    _xx, _xx, _xx, _xx, _xx, _xx, _da, _dr, _xx, _xx, _xx, _xx, _cn, _cn, _xx, _cn, // 0B50 - 0B5F
    _iv, _iv, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, // 0B60 - 0B6F
    _xx                                                                             // 0B70
};

static const IndicClassTable::CharClass tamlCharClasses[] =
{
    _xx, _xx, _ma, _mp, _xx, _iv, _iv, _iv, _iv, _iv, _iv, _xx, _xx, _xx, _iv, _iv, // 0B80 - 0B8F
    _iv, _xx, _iv, _iv, _iv, _ct, _xx, _xx, _xx, _ct, _ct, _xx, _ct, _xx, _ct, _ct, // 0B90 - 0B9F
    _xx, _xx, _xx, _ct, _ct, _xx, _xx, _xx, _ct, _ct, _ct, _xx, _xx, _xx, _ct, _ct, // 0BA0 - 0BAF
    _ct, _ct, _ct, _ct, _ct, _ct, _xx, _ct, _ct, _ct, _xx, _xx, _xx, _xx, _dr, _dr, // 0BB0 - 0BBF
    _da, _dr, _dr, _xx, _xx, _xx, _dl, _dl, _dl, _xx, _s1, _s2, _s3, _vr, _xx, _xx, // 0BC0 - 0BCF
    _xx, _xx, _xx, _xx, _xx, _xx, _xx, _dr, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, // 0BD0 - 0BDF
    _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, // 0BE0 - 0BEF
    _xx, _xx, _xx                                                                   // 0BF0 - 0BF2
};

// FIXME: Should some of the bb's be pb's? (KA, NA, MA, YA, VA, etc. (approx 13))
static const IndicClassTable::CharClass teluCharClasses[] =
{
    _xx, _mp, _mp, _mp, _xx, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _xx, _iv, _iv, // 0C00 - 0C0F
    _iv, _xx, _iv, _iv, _iv, _bb, _bb, _bb, _bb, _bb, _bb, _bb, _bb, _bb, _bb, _bb, // 0C10 - 0C1F
    _bb, _bb, _bb, _bb, _bb, _bb, _bb, _bb, _bb, _xx, _bb, _bb, _bb, _bb, _bb, _bb, // 0C20 - 0C2F
    _bb, _ct, _bb, _bb, _xx, _bb, _bb, _bb, _bb, _bb, _xx, _xx, _xx, _xx, _da, _da, // 0C30 - 0C3F
    _da, _dr, _dr, _dr, _dr, _xx, _da, _da, _s1, _xx, _da, _da, _da, _vr, _xx, _xx, // 0C40 - 0C4F
    _xx, _xx, _xx, _xx, _xx, _da, _db, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, // 0C50 - 0C5F
    _iv, _iv, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx  // 0C60 - 0C6F
};

// FIXME: is 0CD5 a dr or an lm??
static const IndicClassTable::CharClass kndaCharClasses[] =
{
    _xx, _xx, _mp, _mp, _xx, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _xx, _iv, // 0C80 - 0C8F
    _iv, _xx, _iv, _iv, _iv, _bb, _bb, _bb, _bb, _bb, _bb, _bb, _bb, _bb, _bb, _bb, // 0C90 - 0C9F
    _bb, _bb, _bb, _bb, _bb, _bb, _bb, _bb, _bb, _xx, _bb, _bb, _bb, _bb, _bb, _bb, // 0CA0 - 0CAF
    _rb, _ct, _bb, _bb, _xx, _bb, _bb, _bb, _bb, _bb, _xx, _xx, _xx, _xx, _dr, _da, // 0CB0 - 0CBF
    _s1, _dr, _dr, _dr, _dr, _xx, _da, _s2, _s3, _xx, _s4, _s5, _da, _vr, _xx, _xx, // 0CC0 - 0CCF
    _xx, _xx, _xx, _xx, _xx, _lm, _dr, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _ct, _xx, // 0CD0 - 0CDF
    _iv, _iv, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx  // 0CE0 - 0CEF
};

// FIXME: this is correct for old-style Malayalam (MAL) but not for reformed Malayalam (MLR)
// FIXME: should there be a REPH for old-style Malayalam?
static const IndicClassTable::CharClass mlymCharClasses[] =
{
    _xx, _xx, _mp, _mp, _xx, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _iv, _xx, _iv, _iv, // 0D00 - 0D0F
    _iv, _xx, _iv, _iv, _iv, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _ct, _bb, // 0D10 - 0D1F
    _ct, _ct, _ct, _bb, _ct, _bb, _bb, _ct, _ct, _xx, _ct, _ct, _ct, _ct, _ct, _ct, // 0D20 - 0D2F
    _pb, _cn, _bb, _ct, _ct, _pb, _ct, _ct, _ct, _ct, _xx, _xx, _xx, _xx, _dr, _dr, // 0D30 - 0D3F
    _dr, _db, _db, _db, _xx, _xx, _dl, _dl, _dl, _xx, _s1, _s2, _s3, _vr, _xx, _xx, // 0D40 - 0D4F
    _xx, _xx, _xx, _xx, _xx, _xx, _xx, _dr, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, // 0D50 - 0D5F
    _iv, _iv, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx, _xx  // 0D60 - 0D6F
};

//
// Split matra tables
//
static const SplitMatra bengSplitTable[] = {{0x09C7, 0x09BE}, {0x09C7, 0x09D7}};

static const SplitMatra oryaSplitTable[] = {{0x0B47, 0x0B56}, {0x0B47, 0x0B3E}, {0x0B47, 0x0B57}};

static const SplitMatra tamlSplitTable[] = {{0x0BC6, 0x0BBE}, {0x0BC7, 0x0BBE}, {0x0BC6, 0x0BD7}};

static const SplitMatra teluSplitTable[] = {{0x0C46, 0x0C56}};

static const SplitMatra kndaSplitTable[] = {{0x0CBF, 0x0CD5}, {0x0CC6, 0x0CD5}, {0x0CC6, 0x0CD6}, {0x0CC6, 0x0CC2},
                                   {0x0CC6, 0x0CC2, 0x0CD5}};

static const SplitMatra mlymSplitTable[] = {{0x0D46, 0x0D3E}, {0x0D47, 0x0D3E}, {0x0D46, 0x0D57}};

//
// Script Flags
//

// FIXME: post 'GSUB' reordering of MATRA_PRE's for Malayalam and Tamil
// FIXME: reformed Malayalam needs to reorder VATTU to before base glyph...
// FIXME: eyelash RA only for Devanagari??
#define DEVA_SCRIPT_FLAGS (IndicClassTable::SF_EYELASH_RA | IndicClassTable::SF_NO_POST_BASE_LIMIT)
#define BENG_SCRIPT_FLAGS (IndicClassTable::SF_REPH_AFTER_BELOW | IndicClassTable::SF_NO_POST_BASE_LIMIT)
#define PUNJ_SCRIPT_FLAGS (IndicClassTable::SF_NO_POST_BASE_LIMIT)
#define GUJR_SCRIPT_FLAGS (IndicClassTable::SF_NO_POST_BASE_LIMIT)
#define ORYA_SCRIPT_FLAGS (IndicClassTable::SF_REPH_AFTER_BELOW | IndicClassTable::SF_NO_POST_BASE_LIMIT)
#define TAML_SCRIPT_FLAGS (IndicClassTable::SF_MPRE_FIXUP | IndicClassTable::SF_NO_POST_BASE_LIMIT)
#define TELU_SCRIPT_FLAGS (IndicClassTable::SF_MATRAS_AFTER_BASE | 3)
#define KNDA_SCRIPT_FLAGS (IndicClassTable::SF_MATRAS_AFTER_BASE | 3)
#define MLYM_SCRIPT_FLAGS (IndicClassTable::SF_MPRE_FIXUP | IndicClassTable::SF_NO_POST_BASE_LIMIT)

//
// Indic Class Tables
//
static const IndicClassTable devaClassTable = {0x0900, 0x0970, 2, DEVA_SCRIPT_FLAGS, devaCharClasses, NULL};

static const IndicClassTable bengClassTable = {0x0980, 0x09FA, 3, BENG_SCRIPT_FLAGS, bengCharClasses, bengSplitTable};

static const IndicClassTable punjClassTable = {0x0A00, 0x0A74, 2, PUNJ_SCRIPT_FLAGS, punjCharClasses, NULL};

static const IndicClassTable gujrClassTable = {0x0A80, 0x0AEF, 2, GUJR_SCRIPT_FLAGS, gujrCharClasses, NULL};

static const IndicClassTable oryaClassTable = {0x0B00, 0x0B70, 3, ORYA_SCRIPT_FLAGS, oryaCharClasses, oryaSplitTable};

static const IndicClassTable tamlClassTable = {0x0B80, 0x0BF2, 3, TAML_SCRIPT_FLAGS, tamlCharClasses, tamlSplitTable};

static const IndicClassTable teluClassTable = {0x0C00, 0x0C6F, 3, TELU_SCRIPT_FLAGS, teluCharClasses, teluSplitTable};

static const IndicClassTable kndaClassTable = {0x0C80, 0x0CEF, 4, KNDA_SCRIPT_FLAGS, kndaCharClasses, kndaSplitTable};

static const IndicClassTable mlymClassTable = {0x0D00, 0x0D6F, 3, MLYM_SCRIPT_FLAGS, mlymCharClasses, mlymSplitTable};

//
// IndicClassTable addresses
//
static const IndicClassTable *indicClassTables[] = {
    NULL,            /* 'zyyy' (COMMON) */
    NULL,            /* 'qaai' (INHERITED) */
    NULL,            /* 'arab' (ARABIC) */
    NULL,            /* 'armn' (ARMENIAN) */
    &bengClassTable, /* 'beng' (BENGALI) */
    NULL,            /* 'bopo' (BOPOMOFO) */
    NULL,            /* 'cher' (CHEROKEE) */
    NULL,            /* 'qaac' (COPTIC) */
    NULL,            /* 'cyrl' (CYRILLIC) */
    NULL,            /* 'dsrt' (DESERET) */
    &devaClassTable, /* 'deva' (DEVANAGARI) */
    NULL,            /* 'ethi' (ETHIOPIC) */
    NULL,            /* 'geor' (GEORGIAN) */
    NULL,            /* 'goth' (GOTHIC) */
    NULL,            /* 'grek' (GREEK) */
    &gujrClassTable, /* 'gujr' (GUJARATI) */
    &punjClassTable, /* 'guru' (GURMUKHI) */
    NULL,            /* 'hani' (HAN) */
    NULL,            /* 'hang' (HANGUL) */
    NULL,            /* 'hebr' (HEBREW) */
    NULL,            /* 'hira' (HIRAGANA) */
    &kndaClassTable, /* 'knda' (KANNADA) */
    NULL,            /* 'kata' (KATAKANA) */
    NULL,            /* 'khmr' (KHMER) */
    NULL,            /* 'laoo' (LAO) */
    NULL,            /* 'latn' (LATIN) */
    &mlymClassTable, /* 'mlym' (MALAYALAM) */
    NULL,            /* 'mong' (MONGOLIAN) */
    NULL,            /* 'mymr' (MYANMAR) */
    NULL,            /* 'ogam' (OGHAM) */
    NULL,            /* 'ital' (OLD-ITALIC) */
    &oryaClassTable, /* 'orya' (ORIYA) */
    NULL,            /* 'runr' (RUNIC) */
    NULL,            /* 'sinh' (SINHALA) */
    NULL,            /* 'syrc' (SYRIAC) */
    &tamlClassTable, /* 'taml' (TAMIL) */
    &teluClassTable, /* 'telu' (TELUGU) */
    NULL,            /* 'thaa' (THAANA) */
    NULL,            /* 'thai' (THAI) */
    NULL,            /* 'tibt' (TIBETAN) */
    NULL,            /* 'cans' (CANADIAN-ABORIGINAL) */
    NULL,            /* 'yiii' (YI) */
    NULL,            /* 'tglg' (TAGALOG) */
    NULL,            /* 'hano' (HANUNOO) */
    NULL,            /* 'buhd' (BUHID) */
    NULL,            /* 'tagb' (TAGBANWA) */
    NULL,            /* 'brai' (BRAILLE) */
    NULL,            /* 'cprt' (CYPRIOT) */
    NULL,            /* 'limb' (LIMBU) */
    NULL,            /* 'linb' (LINEAR_B) */
    NULL,            /* 'osma' (OSMANYA) */
    NULL,            /* 'shaw' (SHAVIAN) */
    NULL,            /* 'tale' (TAI_LE) */
    NULL,            /* 'ugar' (UGARITIC) */
    NULL             /* 'hrkt' (KATAKANA_OR_HIRAGANA) */
};

IndicClassTable::CharClass IndicClassTable::getCharClass(LEUnicode ch) const
{
    if (ch == C_SIGN_ZWJ) {
        return CF_CONSONANT | CC_ZERO_WIDTH_MARK;
    }

    if (ch == C_SIGN_ZWNJ) {
        return CC_ZERO_WIDTH_MARK;
    }

    if (ch < firstChar || ch > lastChar) {
        return CC_RESERVED;
    }

    return classTable[ch - firstChar];
}

const IndicClassTable *IndicClassTable::getScriptClassTable(le_int32 scriptCode)
{
    if (scriptCode < 0 || scriptCode >= scriptCodeCount) {
        return NULL;
    }

    return indicClassTables[scriptCode];
}

le_int32 IndicReordering::getWorstCaseExpansion(le_int32 scriptCode)
{
    const IndicClassTable *classTable = IndicClassTable::getScriptClassTable(scriptCode);

    if (classTable == NULL) {
        return 1;
    }

    return classTable->getWorstCaseExpansion();
}

U_NAMESPACE_END
