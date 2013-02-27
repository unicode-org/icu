/*
**********************************************************************
*   Copyright (C) 1997-2013, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File ULOC.CPP
*
* Modification History:
*
*   Date        Name        Description
*   04/01/97    aliu        Creation.
*   08/21/98    stephen     JDK 1.2 sync
*   12/08/98    rtg         New Locale implementation and C API
*   03/15/99    damiba      overhaul.
*   04/06/99    stephen     changed setDefault() to realloc and copy
*   06/14/99    stephen     Changed calls to ures_open for new params
*   07/21/99    stephen     Modified setDefault() to propagate to C++
*   05/14/04    alan        7 years later: refactored, cleaned up, fixed bugs,
*                           brought canonicalization code into line with spec
*****************************************************************************/

/*
   POSIX's locale format, from putil.c: [no spaces]

     ll [ _CC ] [ . MM ] [ @ VV]

     l = lang, C = ctry, M = charmap, V = variant
*/

#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "unicode/uloc.h"

#include "putilimp.h"
#include "ustr_imp.h"
#include "ulocimp.h"
#include "umutex.h"
#include "cstring.h"
#include "cmemory.h"
#include "ucln_cmn.h"
#include "locmap.h"
#include "uarrsort.h"
#include "uenumimp.h"
#include "uassert.h"

#include <stdio.h> /* for sprintf */

/* ### Declarations **************************************************/

/* Locale stuff from locid.cpp */
U_CFUNC void locale_set_default(const char *id);
U_CFUNC const char *locale_get_default(void);
U_CFUNC int32_t
locale_getKeywords(const char *localeID,
            char prev,
            char *keywords, int32_t keywordCapacity,
            char *values, int32_t valuesCapacity, int32_t *valLen,
            UBool valuesToo,
            UErrorCode *status);

/* ### Data tables **************************************************/

/**
 * Table of language codes, both 2- and 3-letter, with preference
 * given to 2-letter codes where possible.  Includes 3-letter codes
 * that lack a 2-letter equivalent.
 *
 * This list must be in sorted order.  This list is returned directly
 * to the user by some API.
 *
 * This list must be kept in sync with LANGUAGES_3, with corresponding
 * entries matched.
 *
 * This table should be terminated with a NULL entry, followed by a
 * second list, and another NULL entry.  The first list is visible to
 * user code when this array is returned by API.  The second list
 * contains codes we support, but do not expose through user API.
 *
 * Notes
 *
 * Tables updated per http://lcweb.loc.gov/standards/iso639-2/ to
 * include the revisions up to 2001/7/27 *CWB*
 *
 * The 3 character codes are the terminology codes like RFC 3066.  This
 * is compatible with prior ICU codes
 *
 * "in" "iw" "ji" "jw" & "sh" have been withdrawn but are still in the
 * table but now at the end of the table because 3 character codes are
 * duplicates.  This avoids bad searches going from 3 to 2 character
 * codes.
 *
 * The range qaa-qtz is reserved for local use
 */
/* Generated using org.unicode.cldr.icu.GenerateISO639LanguageTables */
/* ISO639 table version is 20130123 */
static const char * const LANGUAGES[] = {
    "aa",  "aaa", "aab", "aac", "aad", "aae", "aaf", "aag", 
    "aah", "aai", "aak", "aal", "aam", "aan", "aao", "aap", 
    "aaq", "aas", "aat", "aau", "aaw", "aax", "aay", "aaz", 
    "ab",  "aba", "abb", "abc", "abd", "abe", "abf", "abg", 
    "abh", "abi", "abj", "abl", "abm", "abn", "abo", "abp", 
    "abq", "abr", "abs", "abt", "abu", "abv", "abw", "abx", 
    "aby", "abz", "aca", "acb", "acc", "acd", "ace", "acf", 
    "ach", "aci", "ack", "acl", "acm", "acn", "acp", "acq", 
    "acr", "acs", "act", "acu", "acv", "acw", "acx", "acy", 
    "acz", "ada", "adb", "add", "ade", "adf", "adg", "adh", 
    "adi", "adj", "adl", "adn", "ado", "adp", "adq", "adr", 
    "ads", "adt", "adu", "adw", "adx", "ady", "adz", "ae",  
    "aea", "aeb", "aec", "aed", "aee", "aek", "ael", "aem", 
    "aen", "aeq", "aer", "aes", "aeu", "aew", "aex", "aey", 
    "aez", "af",  "afa", "afb", "afd", "afe", "afg", "afh", 
    "afi", "afk", "afn", "afo", "afp", "afs", "aft", "afu", 
    "afz", "aga", "agb", "agc", "agd", "age", "agf", "agg", 
    "agh", "agi", "agj", "agk", "agl", "agm", "agn", "ago", 
    "agp", "agq", "agr", "ags", "agt", "agu", "agv", "agw", 
    "agx", "agy", "agz", "aha", "ahb", "ahe", "ahg", "ahh", 
    "ahi", "ahk", "ahl", "ahm", "ahn", "aho", "ahp", "ahr", 
    "ahs", "aht", "aia", "aib", "aic", "aid", "aie", "aif", 
    "aig", "aih", "aii", "aij", "aik", "ail", "aim", "ain", 
    "aio", "aip", "aiq", "air", "ais", "ait", "aiw", "aix", 
    "aiy", "aiz", "aja", "ajg", "aji", "ajn", "ajp", "ajt", 
    "aju", "ajw", "ajz", "ak",  "akb", "akc", "akd", "ake", 
    "akf", "akg", "akh", "aki", "akj", "akk", "akl", "akm", 
    "akn", "ako", "akp", "akq", "akr", "aks", "akt", "aku", 
    "akv", "akw", "akx", "aky", "akz", "ala", "alc", "ald", 
    "ale", "alf", "alg", "alh", "ali", "alj", "alk", "all", 
    "alm", "aln", "alo", "alp", "alq", "alr", "als", "alt", 
    "alu", "alw", "alx", "aly", "alz", "am",  "ama", "amb", 
    "amc", "amd", "ame", "amf", "amg", "ami", "amj", "amk", 
    "aml", "amm", "amn", "amo", "amp", "amq", "amr", "ams", 
    "amt", "amu", "amv", "amw", "amx", "amy", "amz", "an",  
    "ana", "anb", "anc", "and", "ane", "anf", "ang", "anh", 
    "ani", "anj", "ank", "anl", "anm", "ann", "ano", "anp", 
    "anq", "anr", "ans", "ant", "anu", "anv", "anw", "anx", 
    "any", "anz", "aoa", "aob", "aoc", "aod", "aoe", "aof", 
    "aog", "aoh", "aoi", "aoj", "aok", "aol", "aom", "aon", 
    "aor", "aos", "aot", "aou", "aox", "aoz", "apa", "apb", 
    "apc", "apd", "ape", "apf", "apg", "aph", "api", "apj", 
    "apk", "apl", "apm", "apn", "apo", "app", "apq", "apr", 
    "aps", "apt", "apu", "apv", "apw", "apx", "apy", "apz", 
    "aqc", "aqd", "aqg", "aqm", "aqn", "aqp", "aqr", "aqz", 
    "ar",  "arb", "arc", "ard", "are", "arf", "arh", "ari", 
    "arj", "ark", "arl", "arn", "aro", "arp", "arq", "arr", 
    "ars", "art", "aru", "arv", "arw", "arx", "ary", "arz", 
    "as",  "asa", "asb", "asc", "asd", "ase", "asf", "asg", 
    "ash", "asi", "asj", "ask", "asl", "asn", "aso", "asp", 
    "asq", "asr", "ass", "ast", "asu", "asv", "asw", "asx", 
    "asy", "asz", "ata", "atb", "atc", "atd", "ate", "atf", 
    "atg", "ath", "ati", "atj", "atk", "atl", "atm", "atn", 
    "ato", "atp", "atq", "atr", "ats", "att", "atu", "atv", 
    "atw", "atx", "aty", "atz", "aua", "aub", "auc", "aud", 
    "aue", "aug", "auh", "aui", "auj", "auk", "aul", "aum", 
    "aun", "auo", "aup", "auq", "aur", "aus", "aut", "auu", 
    "auv", "auw", "aux", "auy", "auz", "av",  "avb", "avd", 
    "avi", "avk", "avl", "avm", "avn", "avo", "avs", "avt", 
    "avu", "avv", "awa", "awb", "awc", "awe", "awg", "awh", 
    "awi", "awk", "awm", "awn", "awo", "awr", "aws", "awt", 
    "awu", "awv", "aww", "awx", "awy", "axb", "axe", "axg", 
    "axk", "axl", "axm", "axx", "ay",  "aya", "ayb", "ayc", 
    "ayd", "aye", "ayg", "ayh", "ayi", "ayk", "ayl", "ayn", 
    "ayo", "ayp", "ayq", "ayr", "ays", "ayt", "ayu", "ayx", 
    "ayy", "ayz", "az",  "aza", "azb", "azd", "azg", "azj", 
    "azm", "azn", "azo", "azr", "azt", "azz", 
    "ba",  "baa", "bab", "bac", "bad", "bae", "baf", "bag", 
    "bah", "bai", "baj", "bal", "ban", "bao", "bap", "bar", 
    "bas", "bat", "bau", "bav", "baw", "bax", "bay", "baz", 
    "bba", "bbb", "bbc", "bbd", "bbe", "bbf", "bbg", "bbh", 
    "bbi", "bbj", "bbk", "bbl", "bbm", "bbn", "bbo", "bbp", 
    "bbq", "bbr", "bbs", "bbt", "bbu", "bbv", "bbw", "bbx", 
    "bby", "bbz", "bca", "bcb", "bcc", "bcd", "bce", "bcf", 
    "bcg", "bch", "bci", "bcj", "bck", "bcl", "bcm", "bcn", 
    "bco", "bcp", "bcq", "bcr", "bcs", "bct", "bcu", "bcv", 
    "bcw", "bcx", "bcy", "bcz", "bda", "bdb", "bdc", "bdd", 
    "bde", "bdf", "bdg", "bdh", "bdi", "bdj", "bdk", "bdl", 
    "bdm", "bdn", "bdo", "bdp", "bdq", "bdr", "bds", "bdt", 
    "bdu", "bdv", "bdw", "bdx", "bdy", "bdz", "be",  "bea", 
    "beb", "bec", "bed", "bee", "bef", "beg", "beh", "bei", 
    "bej", "bek", "bem", "beo", "bep", "beq", "ber", "bes", 
    "bet", "beu", "bev", "bew", "bex", "bey", "bez", "bfa", 
    "bfb", "bfc", "bfd", "bfe", "bff", "bfg", "bfh", "bfi", 
    "bfj", "bfk", "bfl", "bfm", "bfn", "bfo", "bfp", "bfq", 
    "bfr", "bfs", "bft", "bfu", "bfw", "bfx", "bfy", "bfz", 
    "bg",  "bga", "bgb", "bgc", "bgd", "bge", "bgf", "bgg", 
    "bgh", "bgi", "bgj", "bgk", "bgl", "bgm", "bgn", "bgo", 
    "bgp", "bgq", "bgr", "bgs", "bgt", "bgu", "bgv", "bgw", 
    "bgx", "bgy", "bgz", "bh",  "bha", "bhb", "bhc", "bhd", 
    "bhe", "bhf", "bhg", "bhh", "bhi", "bhj", "bhk", "bhl", 
    "bhm", "bhn", "bho", "bhp", "bhq", "bhr", "bhs", "bht", 
    "bhu", "bhv", "bhw", "bhx", "bhy", "bhz", "bi",  "bia", 
    "bib", "bic", "bid", "bie", "bif", "big", "bii", "bij", 
    "bik", "bil", "bim", "bin", "bio", "bip", "biq", "bir", 
    "bit", "biu", "biv", "biw", "bix", "biy", "biz", "bja", 
    "bjb", "bjc", "bjd", "bje", "bjf", "bjg", "bjh", "bji", 
    "bjj", "bjk", "bjl", "bjm", "bjn", "bjo", "bjp", "bjq", 
    "bjr", "bjs", "bjt", "bju", "bjv", "bjw", "bjx", "bjy", 
    "bjz", "bka", "bkb", "bkc", "bkd", "bke", "bkf", "bkg", 
    "bkh", "bki", "bkj", "bkk", "bkl", "bkm", "bkn", "bko", 
    "bkp", "bkq", "bkr", "bks", "bkt", "bku", "bkv", "bkw", 
    "bkx", "bky", "bkz", "bla", "blb", "blc", "bld", "ble", 
    "blf", "blg", "blh", "bli", "blj", "blk", "bll", "blm", 
    "bln", "blo", "blp", "blq", "blr", "bls", "blt", "blu", 
    "blv", "blw", "blx", "bly", "blz", "bm",  "bma", "bmb", 
    "bmc", "bmd", "bme", "bmf", "bmg", "bmh", "bmi", "bmj", 
    "bmk", "bml", "bmm", "bmn", "bmo", "bmp", "bmq", "bmr", 
    "bms", "bmt", "bmu", "bmv", "bmw", "bmx", "bmy", "bmz", 
    "bn",  "bna", "bnb", "bnc", "bnd", "bne", "bnf", "bng", 
    "bnh", "bni", "bnj", "bnk", "bnl", "bnm", "bnn", "bno", 
    "bnp", "bnq", "bnr", "bns", "bnt", "bnu", "bnv", "bnw", 
    "bnx", "bny", "bnz", "bo",  "boa", "bob", "boc", "boe", 
    "bof", "bog", "boh", "boi", "boj", "bok", "bol", "bom", 
    "bon", "boo", "bop", "boq", "bor", "bot", "bou", "bov", 
    "bow", "box", "boy", "boz", "bpa", "bpb", "bpd", "bpg", 
    "bph", "bpi", "bpj", "bpk", "bpl", "bpm", "bpn", "bpo", 
    "bpp", "bpq", "bpr", "bps", "bpt", "bpu", "bpv", "bpw", 
    "bpx", "bpy", "bpz", "bqa", "bqb", "bqc", "bqd", "bqe", 
    "bqf", "bqg", "bqh", "bqi", "bqj", "bqk", "bql", "bqm", 
    "bqn", "bqo", "bqp", "bqq", "bqr", "bqs", "bqt", "bqu", 
    "bqv", "bqw", "bqx", "bqy", "bqz", "br",  "bra", "brb", 
    "brc", "brd", "brf", "brg", "brh", "bri", "brj", "brk", 
    "brl", "brm", "brn", "bro", "brp", "brq", "brr", "brs", 
    "brt", "bru", "brv", "brw", "brx", "bry", "brz", "bs",  
    "bsa", "bsb", "bsc", "bsd", "bse", "bsf", "bsg", "bsh", 
    "bsi", "bsj", "bsk", "bsl", "bsm", "bsn", "bso", "bsp", 
    "bsq", "bsr", "bss", "bst", "bsu", "bsv", "bsw", "bsx", 
    "bsy", "bsz", "bta", "btb", "btc", "btd", "bte", "btf", 
    "btg", "bth", "bti", "btj", "btk", "btl", "btm", "btn", 
    "bto", "btp", "btq", "btr", "bts", "btt", "btu", "btv", 
    "btw", "btx", "bty", "btz", "bua", "bub", "buc", "bud", 
    "bue", "buf", "bug", "buh", "bui", "buj", "buk", "bum", 
    "bun", "buo", "bup", "buq", "bus", "but", "buu", "buv", 
    "buw", "bux", "buy", "buz", "bva", "bvb", "bvc", "bvd", 
    "bve", "bvf", "bvg", "bvh", "bvi", "bvj", "bvk", "bvl", 
    "bvm", "bvn", "bvo", "bvp", "bvq", "bvr", "bvs", "bvt", 
    "bvu", "bvv", "bvw", "bvx", "bvy", "bvz", "bwa", "bwb", 
    "bwc", "bwd", "bwe", "bwf", "bwg", "bwh", "bwi", "bwj", 
    "bwk", "bwl", "bwm", "bwn", "bwo", "bwp", "bwq", "bwr", 
    "bws", "bwt", "bwu", "bwv", "bww", "bwx", "bwy", "bwz", 
    "bxa", "bxb", "bxc", "bxd", "bxe", "bxf", "bxg", "bxh", 
    "bxi", "bxj", "bxk", "bxl", "bxm", "bxn", "bxo", "bxp", 
    "bxq", "bxr", "bxs", "bxt", "bxu", "bxv", "bxw", "bxx", 
    "bxz", "bya", "byb", "byc", "byd", "bye", "byf", "byg", 
    "byh", "byi", "byj", "byk", "byl", "bym", "byn", "byo", 
    "byp", "byq", "byr", "bys", "byt", "byu", "byv", "byw", 
    "byx", "byy", "byz", "bza", "bzb", "bzc", "bzd", "bze", 
    "bzf", "bzg", "bzh", "bzi", "bzj", "bzk", "bzl", "bzm", 
    "bzn", "bzo", "bzp", "bzq", "bzr", "bzs", "bzt", "bzu", 
    "bzv", "bzw", "bzx", "bzy", "bzz", 
    "ca",  "caa", "cab", "cac", "cad", "cae", "caf", "cag", 
    "cah", "cai", "caj", "cak", "cal", "cam", "can", "cao", 
    "cap", "caq", "car", "cas", "cau", "cav", "caw", "cax", 
    "cay", "caz", "cbb", "cbc", "cbd", "cbe", "cbg", "cbh", 
    "cbi", "cbj", "cbk", "cbl", "cbm", "cbn", "cbo", "cbr", 
    "cbs", "cbt", "cbu", "cbv", "cbw", "cby", "cca", "ccc", 
    "ccd", "cce", "ccg", "cch", "ccj", "ccl", "ccm", "cco", 
    "ccp", "ccq", "ccr", "ccx", "ccy", "cda", "cde", "cdf", 
    "cdg", "cdh", "cdi", "cdj", "cdm", "cdn", "cdo", "cdr", 
    "cds", "cdy", "cdz", "ce",  "cea", "ceb", "ceg", "cek", 
    "cel", "cen", "cet", "cfa", "cfd", "cfg", "cfm", "cga", 
    "cgc", "cgg", "cgk", "ch",  "chb", "chc", "chd", "chf", 
    "chg", "chh", "chj", "chk", "chl", "chm", "chn", "cho", 
    "chp", "chq", "chr", "chs", "cht", "chw", "chx", "chy", 
    "chz", "cia", "cib", "cic", "cid", "cie", "cih", "cik", 
    "cim", "cin", "cip", "cir", "cit", "ciw", "ciy", "cja", 
    "cje", "cjh", "cji", "cjk", "cjm", "cjn", "cjo", "cjp", 
    "cjr", "cjs", "cjv", "cjy", "cka", "ckb", "ckc", "ckd", 
    "cke", "ckf", "ckh", "cki", "ckj", "ckk", "ckl", "ckn", 
    "cko", "ckq", "ckr", "cks", "ckt", "cku", "ckv", "ckw", 
    "ckx", "cky", "ckz", "cla", "clc", "cld", "cle", "clh", 
    "cli", "clj", "clk", "cll", "clm", "clo", "clt", "clu", 
    "clw", "cly", "cma", "cmc", "cme", "cmg", "cmi", "cmk", 
    "cml", "cmm", "cmn", "cmo", "cmr", "cms", "cmt", "cna", 
    "cnb", "cnc", "cng", "cnh", "cni", "cnk", "cnl", "cnm", 
    "cno", "cns", "cnt", "cnu", "cnw", "cnx", "co",  "coa", 
    "cob", "coc", "cod", "coe", "cof", "cog", "coh", "coj", 
    "cok", "col", "com", "con", "coo", "cop", "coq", "cot", 
    "cou", "cov", "cow", "cox", "coy", "coz", "cpa", "cpb", 
    "cpc", "cpe", "cpf", "cpg", "cpi", "cpn", "cpo", "cpp", 
    "cps", "cpu", "cpx", "cpy", "cqd", "cqu", "cr",  "cra", 
    "crb", "crc", "crd", "crf", "crg", "crh", "cri", "crj", 
    "crk", "crl", "crm", "crn", "cro", "crp", "crq", "crr", 
    "crs", "crt", "cru", "crv", "crw", "crx", "cry", "crz", 
    "cs",  "csa", "csb", "csc", "csd", "cse", "csf", "csg", 
    "csh", "csi", "csj", "csk", "csl", "csm", "csn", "cso", 
    "csq", "csr", "css", "cst", "csv", "csw", "csy", "csz", 
    "cta", "ctc", "ctd", "cte", "ctg", "cth", "cti", "ctl", 
    "ctm", "ctn", "cto", "ctp", "cts", "ctt", "ctu", "ctz", 
    "cu",  "cua", "cub", "cuc", "cug", "cuh", "cui", "cuj", 
    "cuk", "cul", "cum", "cun", "cuo", "cup", "cuq", "cur", 
    "cus", "cut", "cuu", "cuv", "cuw", "cux", "cv",  "cvg", 
    "cvn", "cwa", "cwb", "cwd", "cwe", "cwg", "cwt", "cy",  
    "cya", "cyb", "cyo", "czh", "czk", "czn", "czo", "czt", 
    "da",  "daa", "dac", "dad", "dae", "daf", "dag", "dah", 
    "dai", "daj", "dak", "dal", "dam", "dao", "dap", "daq", 
    "dar", "das", "dat", "dau", "dav", "daw", "dax", "day", 
    "daz", "dba", "dbb", "dbd", "dbe", "dbf", "dbg", "dbi", 
    "dbj", "dbl", "dbm", "dbn", "dbo", "dbp", "dbq", "dbr", 
    "dbt", "dbu", "dbv", "dbw", "dby", "dcc", "dcr", "dda", 
    "ddd", "dde", "ddg", "ddi", "ddj", "ddn", "ddo", "ddr", 
    "dds", "ddw", "de",  "dec", "ded", "dee", "def", "deg", 
    "deh", "dei", "dek", "del", "dem", "den", "dep", "deq", 
    "der", "des", "dev", "dez", "dga", "dgb", "dgc", "dgd", 
    "dge", "dgg", "dgh", "dgi", "dgk", "dgl", "dgn", "dgo", 
    "dgr", "dgs", "dgt", "dgu", "dgw", "dgx", "dgz", "dha", 
    "dhd", "dhg", "dhi", "dhl", "dhm", "dhn", "dho", "dhr", 
    "dhs", "dhu", "dhv", "dhw", "dhx", "dia", "dib", "dic", 
    "did", "dif", "dig", "dih", "dii", "dij", "dik", "dil", 
    "dim", "din", "dio", "dip", "diq", "dir", "dis", "dit", 
    "diu", "diw", "dix", "diy", "diz", "dja", "djb", "djc", 
    "djd", "dje", "djf", "dji", "djj", "djk", "djl", "djm", 
    "djn", "djo", "djr", "dju", "djw", "dka", "dkk", "dkl", 
    "dkr", "dks", "dkx", "dlg", "dlk", "dlm", "dln", "dma", 
    "dmb", "dmc", "dmd", "dme", "dmg", "dmk", "dml", "dmm", 
    "dmo", "dmr", "dms", "dmu", "dmv", "dmw", "dmx", "dmy", 
    "dna", "dnd", "dne", "dng", "dni", "dnj", "dnk", "dnn", 
    "dnr", "dnt", "dnu", "dnv", "dnw", "dny", "doa", "dob", 
    "doc", "doe", "dof", "doh", "doi", "dok", "dol", "don", 
    "doo", "dop", "doq", "dor", "dos", "dot", "dov", "dow", 
    "dox", "doy", "doz", "dpp", "dra", "drb", "drc", "drd", 
    "dre", "drg", "drh", "dri", "drl", "drn", "dro", "drq", 
    "drr", "drs", "drt", "dru", "drw", "dry", "dsb", "dse", 
    "dsh", "dsi", "dsl", "dsn", "dso", "dsq", "dta", "dtb", 
    "dtd", "dth", "dti", "dtk", "dtm", "dto", "dtp", "dtr", 
    "dts", "dtt", "dtu", "dty", "dua", "dub", "duc", "dud", 
    "due", "duf", "dug", "duh", "dui", "duj", "duk", "dul", 
    "dum", "dun", "duo", "dup", "duq", "dur", "dus", "duu", 
    "duv", "duw", "dux", "duy", "duz", "dv",  "dva", "dwa", 
    "dwl", "dwr", "dws", "dww", "dya", "dyb", "dyd", "dyg", 
    "dyi", "dyk", "dym", "dyn", "dyo", "dyu", "dyy", "dz",  
    "dza", "dzd", "dze", "dzg", "dzl", "dzn", 
    "eaa", "ebg", "ebk", "ebo", "ebr", "ebu", "ecr", "ecs", 
    "ecy", "ee",  "eee", "efa", "efe", "efi", "ega", "egl", 
    "ego", "egy", "ehu", "eip", "eit", "eiv", "eja", "eka", 
    "ekc", "eke", "ekg", "eki", "ekk", "ekl", "ekm", "eko", 
    "ekp", "ekr", "eky", "el",  "ele", "elh", "eli", "elk", 
    "elm", "elo", "elp", "elu", "elx", "ema", "emb", "eme", 
    "emg", "emi", "emk", "eml", "emm", "emn", "emo", "emp", 
    "ems", "emu", "emw", "emx", "emy", "en",  "ena", "enb", 
    "enc", "end", "enf", "enh", "eni", "enm", "enn", "eno", 
    "enq", "enr", "enu", "env", "enw", "eo",  "eot", "epi", 
    "era", "erg", "erh", "eri", "erk", "ero", "err", "ers", 
    "ert", "erw", "es",  "ese", "esh", "esi", "esk", "esl", 
    "esm", "esn", "eso", "esq", "ess", "esu", "et",  "etb", 
    "etc", "eth", "etn", "eto", "etr", "ets", "ett", "etu", 
    "etx", "etz", "eu",  "eur", "eve", "evh", "evn", "ewo", 
    "ext", "eya", "eyo", "eza", "eze", 
    "fa",  "faa", "fab", "fad", "faf", "fag", "fah", "fai", 
    "faj", "fak", "fal", "fam", "fan", "fap", "far", "fat", 
    "fau", "fax", "fay", "faz", "fbl", "fcs", "fer", "ff",  
    "ffi", "ffm", "fgr", "fi",  "fia", "fie", "fil", "fip", 
    "fir", "fit", "fiu", "fiw", "fiz", "fj",  "fkk", "fkv", 
    "fla", "flh", "fli", "fll", "flm", "fln", "flr", "fly", 
    "fmp", "fmu", "fng", "fni", "fo",  "fod", "foi", "fom", 
    "fon", "for", "fos", "fpe", "fqs", "fr",  "frc", "frd", 
    "fri", "frk", "frm", "fro", "frp", "frq", "frr", "frs", 
    "frt", "fse", "fsl", "fss", "fub", "fuc", "fud", "fue", 
    "fuf", "fuh", "fui", "fuj", "fum", "fun", "fuq", "fur", 
    "fut", "fuu", "fuv", "fuy", "fvr", "fwa", "fwe", "fy",  
    "ga",  "gaa", "gab", "gac", "gad", "gae", "gaf", "gag", 
    "gah", "gai", "gaj", "gak", "gal", "gam", "gan", "gao", 
    "gap", "gaq", "gar", "gas", "gat", "gau", "gav", "gaw", 
    "gax", "gay", "gaz", "gba", "gbb", "gbc", "gbd", "gbe", 
    "gbf", "gbg", "gbh", "gbi", "gbj", "gbk", "gbl", "gbm", 
    "gbn", "gbo", "gbp", "gbq", "gbr", "gbs", "gbu", "gbv", 
    "gbw", "gbx", "gby", "gbz", "gcc", "gcd", "gce", "gcf", 
    "gcl", "gcn", "gcr", "gct", "gd",  "gda", "gdb", "gdc", 
    "gdd", "gde", "gdf", "gdg", "gdh", "gdi", "gdj", "gdk", 
    "gdl", "gdm", "gdn", "gdo", "gdq", "gdr", "gds", "gdt", 
    "gdu", "gdx", "gea", "geb", "gec", "ged", "geg", "geh", 
    "gei", "gej", "gek", "gel", "gem", "gen", "geq", "ges", 
    "gew", "gex", "gey", "gez", "gfk", "gft", "gfx", "gga", 
    "ggb", "ggd", "gge", "ggg", "ggh", "ggk", "ggl", "ggm", 
    "ggn", "ggo", "ggr", "ggt", "ggu", "ggw", "gha", "ghc", 
    "ghe", "ghh", "ghk", "ghl", "ghn", "gho", "ghr", "ghs", 
    "ght", "gia", "gib", "gic", "gid", "gig", "gih", "gil", 
    "gim", "gin", "gio", "gip", "giq", "gir", "gis", "git", 
    "giu", "giw", "gix", "giy", "giz", "gji", "gjk", "gjm", 
    "gjn", "gju", "gka", "gke", "gkn", "gko", "gkp", "gl",  
    "glc", "gld", "glh", "gli", "glj", "glk", "gll", "glo", 
    "glr", "glu", "glw", "gly", "gma", "gmb", "gmd", "gmh", 
    "gml", "gmm", "gmn", "gmo", "gmu", "gmv", "gmx", "gmy", 
    "gmz", "gn",  "gna", "gnb", "gnc", "gnd", "gne", "gng", 
    "gnh", "gni", "gnk", "gnl", "gnm", "gnn", "gno", "gnq", 
    "gnr", "gnt", "gnu", "gnw", "gnz", "goa", "gob", "goc", 
    "god", "goe", "gof", "gog", "goh", "goi", "goj", "gok", 
    "gol", "gom", "gon", "goo", "gop", "goq", "gor", "gos", 
    "got", "gou", "gow", "gox", "goy", "goz", "gpa", "gpe", 
    "gpn", "gqa", "gqi", "gqn", "gqr", "gqu", "gra", "grb", 
    "grc", "grd", "grg", "grh", "gri", "grj", "grm", "gro", 
    "grq", "grr", "grs", "grt", "gru", "grv", "grw", "grx", 
    "gry", "grz", "gsc", "gse", "gsg", "gsl", "gsm", "gsn", 
    "gso", "gsp", "gss", "gsw", "gta", "gti", "gtu", "gu",  
    "gua", "gub", "guc", "gud", "gue", "guf", "gug", "guh", 
    "gui", "guk", "gul", "gum", "gun", "guo", "gup", "guq", 
    "gur", "gus", "gut", "guu", "guv", "guw", "gux", "guz", 
    "gv",  "gva", "gvc", "gve", "gvf", "gvj", "gvl", "gvm", 
    "gvn", "gvo", "gvp", "gvr", "gvs", "gvy", "gwa", "gwb", 
    "gwc", "gwd", "gwe", "gwf", "gwg", "gwi", "gwj", "gwm", 
    "gwn", "gwr", "gwt", "gwu", "gww", "gwx", "gxx", "gya", 
    "gyb", "gyd", "gye", "gyf", "gyg", "gyi", "gyl", "gym", 
    "gyn", "gyr", "gyy", "gza", "gzi", "gzn", 
    "ha",  "haa", "hab", "hac", "had", "hae", "haf", "hag", 
    "hah", "hai", "haj", "hak", "hal", "ham", "han", "hao", 
    "hap", "haq", "har", "has", "hav", "haw", "hax", "hay", 
    "haz", "hba", "hbb", "hbn", "hbo", "hbu", "hca", "hch", 
    "hdn", "hds", "hdy", "he",  "hea", "hed", "heg", "heh", 
    "hei", "hem", "hgm", "hgw", "hhi", "hhr", "hhy", "hi",  
    "hia", "hib", "hid", "hif", "hig", "hih", "hii", "hij", 
    "hik", "hil", "him", "hio", "hir", "hit", "hiw", "hix", 
    "hji", "hka", "hke", "hkk", "hks", "hla", "hlb", "hld", 
    "hle", "hlt", "hlu", "hma", "hmb", "hmc", "hmd", "hme", 
    "hmf", "hmg", "hmh", "hmi", "hmj", "hmk", "hml", "hmm", 
    "hmn", "hmp", "hmq", "hmr", "hms", "hmt", "hmu", "hmv", 
    "hmw", "hmy", "hmz", "hna", "hnd", "hne", "hnh", "hni", 
    "hnj", "hnn", "hno", "hns", "hnu", "ho",  "hoa", "hob", 
    "hoc", "hod", "hoe", "hoh", "hoi", "hoj", "hol", "hom", 
    "hoo", "hop", "hor", "hos", "hot", "hov", "how", "hoy", 
    "hoz", "hpo", "hps", "hr",  "hra", "hrc", "hre", "hrk", 
    "hrm", "hro", "hrp", "hrr", "hrt", "hru", "hrw", "hrx", 
    "hrz", "hsb", "hsf", "hsh", "hsl", "hsn", "hss", "ht",  
    "hti", "hto", "hts", "htu", "htx", "hu",  "hub", "huc", 
    "hud", "hue", "huf", "hug", "huh", "hui", "huj", "huk", 
    "hul", "hum", "huo", "hup", "huq", "hur", "hus", "hut", 
    "huu", "huv", "huw", "hux", "huy", "huz", "hva", "hvc", 
    "hve", "hvk", "hvn", "hvv", "hwa", "hwc", "hwo", "hy",  
    "hya", "hz",  
    "ia",  "iai", "ian", "iap", "iar", "iba", "ibb", "ibd", 
    "ibe", "ibg", "ibi", "ibl", "ibm", "ibn", "ibr", "ibu", 
    "iby", "ica", "ich", "icl", "icr", "id",  "ida", "idb", 
    "idc", "idd", "ide", "idi", "idr", "ids", "idt", "idu", 
    "ie",  "ifa", "ifb", "ife", "iff", "ifk", "ifm", "ifu", 
    "ify", "ig",  "igb", "ige", "igg", "igl", "igm", "ign", 
    "igo", "igs", "igw", "ihb", "ihi", "ihp", "ihw", "ii",  
    "iin", "ijc", "ije", "ijj", "ijn", "ijo", "ijs", "ik",  
    "ike", "iki", "ikk", "ikl", "iko", "ikp", "ikr", "ikt", 
    "ikv", "ikw", "ikx", "ikz", "ila", "ilb", "ilg", "ili", 
    "ilk", "ill", "ilo", "ils", "ilu", "ilv", "ilw", "ima", 
    "ime", "imi", "iml", "imn", "imo", "imr", "ims", "imy", 
    "inb", "inc", "ine", "ing", "inh", "inj", "inl", "inm", 
    "inn", "ino", "inp", "ins", "int", "inz", "io",  "ior", 
    "iou", "iow", "ipi", "ipo", "iqu", "iqw", "ira", "ire", 
    "irh", "iri", "irk", "irn", "iro", "irr", "iru", "irx", 
    "iry", "is",  "isa", "isc", "isd", "ise", "isg", "ish", 
    "isi", "isk", "ism", "isn", "iso", "isr", "ist", "isu", 
    "it",  "itb", "ite", "iti", "itk", "itl", "itm", "ito", 
    "itr", "its", "itt", "itu", "itv", "itw", "itx", "ity", 
    "itz", "iu",  "ium", "ivb", "ivv", "iwk", "iwm", "iwo", 
    "iws", "ixc", "ixi", "ixj", "ixl", "iya", "iyo", "iyx", 
    "izh", "izi", "izr", "izz", 
    "ja",  "jaa", "jab", "jac", "jad", "jae", "jaf", "jah", 
    "jai", "jaj", "jak", "jal", "jam", "jan", "jao", "jap", 
    "jaq", "jas", "jat", "jau", "jax", "jay", "jaz", "jbe", 
    "jbi", "jbj", "jbk", "jbn", "jbo", "jbr", "jbt", "jbu", 
    "jbw", "jcs", "jct", "jda", "jdg", "jdt", "jeb", "jee", 
    "jeg", "jeh", "jei", "jek", "jel", "jen", "jer", "jet", 
    "jeu", "jgb", "jge", "jgk", "jgo", "jhi", "jhs", "jia", 
    "jib", "jic", "jid", "jie", "jig", "jih", "jii", "jil", 
    "jim", "jio", "jiq", "jit", "jiu", "jiv", "jiy", "jjr", 
    "jkm", "jko", "jkp", "jkr", "jku", "jle", "jls", "jma", 
    "jmb", "jmc", "jmd", "jmi", "jml", "jmn", "jmr", "jms", 
    "jmw", "jmx", "jna", "jnd", "jng", "jni", "jnj", "jnl", 
    "jns", "job", "jod", "jor", "jos", "jow", "jpa", "jpr", 
    "jqr", "jra", "jrb", "jrr", "jrt", "jru", "jsl", "jua", 
    "jub", "juc", "jud", "juh", "jui", "juk", "jul", "jum", 
    "jun", "juo", "jup", "jur", "jus", "jut", "juu", "juw", 
    "juy", "jv",  "jvd", "jvn", "jwi", "jya", "jye", "jyy", 
    "ka",  "kaa", "kab", "kac", "kad", "kae", "kaf", "kag", 
    "kah", "kai", "kaj", "kak", "kam", "kao", "kap", "kaq", 
    "kar", "kav", "kaw", "kax", "kay", "kba", "kbb", "kbc", 
    "kbd", "kbe", "kbf", "kbg", "kbh", "kbi", "kbj", "kbk", 
    "kbl", "kbm", "kbn", "kbo", "kbp", "kbq", "kbr", "kbs", 
    "kbt", "kbu", "kbv", "kbw", "kbx", "kby", "kbz", "kca", 
    "kcb", "kcc", "kcd", "kce", "kcf", "kcg", "kch", "kci", 
    "kcj", "kck", "kcl", "kcm", "kcn", "kco", "kcp", "kcq", 
    "kcr", "kcs", "kct", "kcu", "kcv", "kcw", "kcx", "kcy", 
    "kcz", "kda", "kdc", "kdd", "kde", "kdf", "kdg", "kdh", 
    "kdi", "kdj", "kdk", "kdl", "kdm", "kdn", "kdp", "kdq", 
    "kdr", "kds", "kdt", "kdu", "kdv", "kdw", "kdx", "kdy", 
    "kdz", "kea", "keb", "kec", "ked", "kee", "kef", "keg", 
    "keh", "kei", "kej", "kek", "kel", "kem", "ken", "keo", 
    "kep", "keq", "ker", "kes", "ket", "keu", "kev", "kew", 
    "kex", "key", "kez", "kfa", "kfb", "kfc", "kfd", "kfe", 
    "kff", "kfg", "kfh", "kfi", "kfj", "kfk", "kfl", "kfm", 
    "kfn", "kfo", "kfp", "kfq", "kfr", "kfs", "kft", "kfu", 
    "kfv", "kfw", "kfx", "kfy", "kfz", "kg",  "kga", "kgb", 
    "kgc", "kgd", "kge", "kgf", "kgg", "kgh", "kgi", "kgj", 
    "kgk", "kgl", "kgm", "kgn", "kgo", "kgp", "kgq", "kgr", 
    "kgs", "kgt", "kgu", "kgv", "kgw", "kgx", "kgy", "kha", 
    "khb", "khc", "khd", "khe", "khf", "khg", "khh", "khi", 
    "khj", "khk", "khl", "khn", "kho", "khp", "khq", "khr", 
    "khs", "kht", "khu", "khv", "khw", "khx", "khy", "khz", 
    "ki",  "kia", "kib", "kic", "kid", "kie", "kif", "kig", 
    "kih", "kii", "kij", "kil", "kim", "kio", "kip", "kiq", 
    "kis", "kit", "kiu", "kiv", "kiw", "kix", "kiy", "kiz", 
    "kj",  "kja", "kjb", "kjc", "kjd", "kje", "kjf", "kjg", 
    "kjh", "kji", "kjj", "kjk", "kjl", "kjm", "kjn", "kjo", 
    "kjp", "kjq", "kjr", "kjs", "kjt", "kju", "kjx", "kjy", 
    "kjz", "kk",  "kka", "kkb", "kkc", "kkd", "kke", "kkf", 
    "kkg", "kkh", "kki", "kkj", "kkk", "kkl", "kkm", "kkn", 
    "kko", "kkp", "kkq", "kkr", "kks", "kkt", "kku", "kkv", 
    "kkw", "kkx", "kky", "kkz", "kl",  "kla", "klb", "klc", 
    "kld", "kle", "klf", "klg", "klh", "kli", "klj", "klk", 
    "kll", "klm", "kln", "klo", "klp", "klq", "klr", "kls", 
    "klt", "klu", "klv", "klw", "klx", "kly", "klz", "km",  
    "kma", "kmb", "kmc", "kmd", "kme", "kmf", "kmg", "kmh", 
    "kmi", "kmj", "kmk", "kml", "kmm", "kmn", "kmo", "kmp", 
    "kmq", "kmr", "kms", "kmt", "kmu", "kmv", "kmw", "kmx", 
    "kmy", "kmz", "kn",  "kna", "knb", "knc", "knd", "kne", 
    "knf", "kng", "knh", "kni", "knj", "knk", "knl", "knm", 
    "knn", "kno", "knp", "knq", "knr", "kns", "knt", "knu", 
    "knv", "knw", "knx", "kny", "knz", "ko",  "koa", "kob", 
    "koc", "kod", "koe", "kof", "kog", "koh", "koi", "koj", 
    "kok", "kol", "koo", "kop", "koq", "kos", "kot", "kou", 
    "kov", "kow", "kox", "koy", "koz", "kpa", "kpb", "kpc", 
    "kpd", "kpe", "kpf", "kpg", "kph", "kpi", "kpj", "kpk", 
    "kpl", "kpm", "kpn", "kpo", "kpp", "kpq", "kpr", "kps", 
    "kpt", "kpu", "kpv", "kpw", "kpx", "kpy", "kpz", "kqa", 
    "kqb", "kqc", "kqd", "kqe", "kqf", "kqg", "kqh", "kqi", 
    "kqj", "kqk", "kql", "kqm", "kqn", "kqo", "kqp", "kqq", 
    "kqr", "kqs", "kqt", "kqu", "kqv", "kqw", "kqx", "kqy", 
    "kqz", "kr",  "kra", "krb", "krc", "krd", "kre", "krf", 
    "krg", "krh", "kri", "krj", "krk", "krl", "krm", "krn", 
    "kro", "krp", "krq", "krr", "krs", "krt", "kru", "krv", 
    "krw", "krx", "kry", "krz", "ks",  "ksa", "ksb", "ksc", 
    "ksd", "kse", "ksf", "ksg", "ksh", "ksi", "ksj", "ksk", 
    "ksl", "ksm", "ksn", "kso", "ksp", "ksq", "ksr", "kss", 
    "kst", "ksu", "ksv", "ksw", "ksx", "ksy", "ksz", "kta", 
    "ktb", "ktc", "ktd", "kte", "ktf", "ktg", "kth", "kti", 
    "ktj", "ktk", "ktl", "ktm", "ktn", "kto", "ktp", "ktq", 
    "ktr", "kts", "ktt", "ktu", "ktv", "ktw", "ktx", "kty", 
    "ktz", "ku",  "kub", "kuc", "kud", "kue", "kuf", "kug", 
    "kuh", "kui", "kuj", "kuk", "kul", "kum", "kun", "kuo", 
    "kup", "kuq", "kus", "kut", "kuu", "kuv", "kuw", "kux", 
    "kuy", "kuz", "kv",  "kva", "kvb", "kvc", "kvd", "kve", 
    "kvf", "kvg", "kvh", "kvi", "kvj", "kvk", "kvl", "kvm", 
    "kvn", "kvo", "kvp", "kvq", "kvr", "kvs", "kvt", "kvu", 
    "kvv", "kvw", "kvx", "kvy", "kvz", "kw",  "kwa", "kwb", 
    "kwc", "kwd", "kwe", "kwf", "kwg", "kwh", "kwi", "kwj", 
    "kwk", "kwl", "kwm", "kwn", "kwo", "kwp", "kwq", "kwr", 
    "kws", "kwt", "kwu", "kwv", "kww", "kwx", "kwy", "kwz", 
    "kxa", "kxb", "kxc", "kxd", "kxe", "kxf", "kxg", "kxh", 
    "kxi", "kxj", "kxk", "kxl", "kxm", "kxn", "kxo", "kxp", 
    "kxq", "kxr", "kxs", "kxt", "kxu", "kxv", "kxw", "kxx", 
    "kxy", "kxz", "ky",  "kya", "kyb", "kyc", "kyd", "kye", 
    "kyf", "kyg", "kyh", "kyi", "kyj", "kyk", "kyl", "kym", 
    "kyn", "kyo", "kyp", "kyq", "kyr", "kys", "kyt", "kyu", 
    "kyv", "kyw", "kyx", "kyy", "kyz", "kza", "kzb", "kzc", 
    "kzd", "kze", "kzf", "kzg", "kzh", "kzi", "kzj", "kzk", 
    "kzl", "kzm", "kzn", "kzo", "kzp", "kzq", "kzr", "kzs", 
    "kzt", "kzu", "kzv", "kzw", "kzx", "kzy", "kzz", 
    "la",  "laa", "lab", "lac", "lad", "lae", "laf", "lag", 
    "lah", "lai", "laj", "lak", "lal", "lam", "lan", "lap", 
    "laq", "lar", "las", "lau", "law", "lax", "lay", "laz", 
    "lb",  "lba", "lbb", "lbc", "lbe", "lbf", "lbg", "lbi", 
    "lbj", "lbk", "lbl", "lbm", "lbn", "lbo", "lbq", "lbr", 
    "lbs", "lbt", "lbu", "lbv", "lbw", "lbx", "lby", "lbz", 
    "lcc", "lcd", "lce", "lcf", "lch", "lcl", "lcm", "lcp", 
    "lcq", "lcs", "lda", "ldb", "ldd", "ldg", "ldh", "ldi", 
    "ldj", "ldk", "ldl", "ldm", "ldn", "ldo", "ldp", "ldq", 
    "lea", "leb", "lec", "led", "lee", "lef", "leg", "leh", 
    "lei", "lej", "lek", "lel", "lem", "len", "leo", "lep", 
    "leq", "ler", "les", "let", "leu", "lev", "lew", "lex", 
    "ley", "lez", "lfa", "lfn", "lg",  "lga", "lgb", "lgg", 
    "lgh", "lgi", "lgk", "lgl", "lgm", "lgn", "lgq", "lgr", 
    "lgt", "lgu", "lgz", "lha", "lhh", "lhi", "lhl", "lhm", 
    "lhn", "lhp", "lhs", "lht", "lhu", "li",  "lia", "lib", 
    "lic", "lid", "lie", "lif", "lig", "lih", "lii", "lij", 
    "lik", "lil", "lio", "lip", "liq", "lir", "lis", "liu", 
    "liv", "liw", "lix", "liy", "liz", "lja", "lje", "lji", 
    "ljl", "ljp", "ljw", "ljx", "lka", "lkb", "lkc", "lkd", 
    "lke", "lkh", "lki", "lkj", "lkl", "lkm", "lkn", "lko", 
    "lkr", "lks", "lkt", "lku", "lky", "lla", "llb", "llc", 
    "lld", "lle", "llf", "llg", "llh", "lli", "llj", "llk", 
    "lll", "llm", "lln", "llo", "llp", "llq", "lls", "llu", 
    "llx", "lma", "lmb", "lmc", "lmd", "lme", "lmf", "lmg", 
    "lmh", "lmi", "lmj", "lmk", "lml", "lmm", "lmn", "lmo", 
    "lmp", "lmq", "lmr", "lms", "lmt", "lmu", "lmv", "lmw", 
    "lmx", "lmy", "lmz", "ln",  "lna", "lnb", "lnc", "lnd", 
    "lng", "lnh", "lni", "lnj", "lnl", "lnm", "lnn", "lno", 
    "lns", "lnt", "lnu", "lnw", "lnz", "lo",  "loa", "lob", 
    "loc", "lod", "loe", "lof", "log", "loh", "loi", "loj", 
    "lok", "lol", "lom", "lon", "loo", "lop", "loq", "lor", 
    "los", "lot", "lou", "lov", "low", "lox", "loy", "loz", 
    "lpa", "lpe", "lpn", "lpo", "lpx", "lra", "lrc", "lre", 
    "lrg", "lri", "lrk", "lrl", "lrm", "lrn", "lro", "lrr", 
    "lrt", "lrv", "lrz", "lsa", "lsd", "lse", "lsg", "lsh", 
    "lsi", "lsl", "lsm", "lso", "lsp", "lsr", "lss", "lst", 
    "lsy", "lt",  "ltc", "ltg", "lti", "ltn", "lto", "lts", 
    "ltu", "lu",  "lua", "luc", "lud", "lue", "luf", "lui", 
    "luj", "luk", "lul", "lum", "lun", "luo", "lup", "luq", 
    "lur", "lus", "lut", "luu", "luv", "luw", "luy", "luz", 
    "lv",  "lva", "lvk", "lvs", "lvu", "lwa", "lwe", "lwg", 
    "lwh", "lwl", "lwm", "lwo", "lwt", "lwu", "lww", "lya", 
    "lyg", "lyn", "lzh", "lzl", "lzn", "lzz", 
    "maa", "mab", "mad", "mae", "maf", "mag", "mai", "maj", 
    "mak", "mam", "man", "map", "maq", "mas", "mat", "mau", 
    "mav", "maw", "max", "maz", "mba", "mbb", "mbc", "mbd", 
    "mbe", "mbf", "mbg", "mbh", "mbi", "mbj", "mbk", "mbl", 
    "mbm", "mbn", "mbo", "mbp", "mbq", "mbr", "mbs", "mbt", 
    "mbu", "mbv", "mbw", "mbx", "mby", "mbz", "mca", "mcb", 
    "mcc", "mcd", "mce", "mcf", "mcg", "mch", "mci", "mcj", 
    "mck", "mcl", "mcm", "mcn", "mco", "mcp", "mcq", "mcr", 
    "mcs", "mct", "mcu", "mcv", "mcw", "mcx", "mcy", "mcz", 
    "mda", "mdb", "mdc", "mdd", "mde", "mdf", "mdg", "mdh", 
    "mdi", "mdj", "mdk", "mdl", "mdm", "mdn", "mdo", "mdp", 
    "mdq", "mdr", "mds", "mdt", "mdu", "mdv", "mdw", "mdx", 
    "mdy", "mdz", "mea", "meb", "mec", "med", "mee", "mef", 
    "meg", "meh", "mei", "mej", "mek", "mel", "mem", "men", 
    "meo", "mep", "meq", "mer", "mes", "met", "meu", "mev", 
    "mew", "mey", "mez", "mfa", "mfb", "mfc", "mfd", "mfe", 
    "mff", "mfg", "mfh", "mfi", "mfj", "mfk", "mfl", "mfm", 
    "mfn", "mfo", "mfp", "mfq", "mfr", "mfs", "mft", "mfu", 
    "mfv", "mfw", "mfx", "mfy", "mfz", "mg",  "mga", "mgb", 
    "mgc", "mgd", "mge", "mgf", "mgg", "mgh", "mgi", "mgj", 
    "mgk", "mgl", "mgm", "mgn", "mgo", "mgp", "mgq", "mgr", 
    "mgs", "mgt", "mgu", "mgv", "mgw", "mgx", "mgy", "mgz", 
    "mh",  "mha", "mhb", "mhc", "mhd", "mhe", "mhf", "mhg", 
    "mhh", "mhi", "mhj", "mhk", "mhl", "mhm", "mhn", "mho", 
    "mhp", "mhq", "mhr", "mhs", "mht", "mhu", "mhv", "mhw", 
    "mhx", "mhy", "mhz", "mi",  "mia", "mib", "mic", "mid", 
    "mie", "mif", "mig", "mih", "mii", "mij", "mik", "mil", 
    "mim", "min", "mio", "mip", "miq", "mir", "mis", "mit", 
    "miu", "miv", "miw", "mix", "miy", "miz", "mja", "mjc", 
    "mjd", "mje", "mjg", "mjh", "mji", "mjj", "mjk", "mjl", 
    "mjm", "mjn", "mjo", "mjp", "mjq", "mjr", "mjs", "mjt", 
    "mju", "mjv", "mjw", "mjx", "mjy", "mjz", "mk",  "mka", 
    "mkb", "mkc", "mke", "mkf", "mkg", "mkh", "mki", "mkj", 
    "mkk", "mkl", "mkm", "mkn", "mko", "mkp", "mkq", "mkr", 
    "mks", "mkt", "mku", "mkv", "mkw", "mkx", "mky", "mkz", 
    "ml",  "mla", "mlb", "mlc", "mld", "mle", "mlf", "mlh", 
    "mli", "mlj", "mlk", "mll", "mlm", "mln", "mlo", "mlp", 
    "mlq", "mlr", "mls", "mlu", "mlv", "mlw", "mlx", "mly", 
    "mlz", "mma", "mmb", "mmc", "mmd", "mme", "mmf", "mmg", 
    "mmh", "mmi", "mmj", "mmk", "mml", "mmm", "mmn", "mmo", 
    "mmp", "mmq", "mmr", "mms", "mmt", "mmu", "mmv", "mmw", 
    "mmx", "mmy", "mmz", "mn",  "mna", "mnb", "mnc", "mnd", 
    "mne", "mnf", "mng", "mnh", "mni", "mnj", "mnk", "mnl", 
    "mnm", "mnn", "mno", "mnp", "mnq", "mnr", "mns", "mnt", 
    "mnu", "mnv", "mnw", "mnx", "mny", "mnz", "mo",  "moa", 
    "mob", "moc", "mod", "moe", "mof", "mog", "moh", "moi", 
    "moj", "mok", "mol", "mom", "moo", "mop", "moq", "mor", 
    "mos", "mot", "mou", "mov", "mow", "mox", "moy", "moz", 
    "mpa", "mpb", "mpc", "mpd", "mpe", "mpf", "mpg", "mph", 
    "mpi", "mpj", "mpk", "mpl", "mpm", "mpn", "mpo", "mpp", 
    "mpq", "mpr", "mps", "mpt", "mpu", "mpv", "mpw", "mpx", 
    "mpy", "mpz", "mqa", "mqb", "mqc", "mqd", "mqe", "mqf", 
    "mqg", "mqh", "mqi", "mqj", "mqk", "mql", "mqm", "mqn", 
    "mqo", "mqp", "mqq", "mqr", "mqs", "mqt", "mqu", "mqv", 
    "mqw", "mqx", "mqy", "mqz", "mr",  "mra", "mrb", "mrc", 
    "mrd", "mre", "mrf", "mrg", "mrh", "mrj", "mrk", "mrl", 
    "mrm", "mrn", "mro", "mrp", "mrq", "mrr", "mrs", "mrt", 
    "mru", "mrv", "mrw", "mrx", "mry", "mrz", "ms",  "msb", 
    "msc", "msd", "mse", "msf", "msg", "msh", "msi", "msj", 
    "msk", "msl", "msm", "msn", "mso", "msp", "msq", "msr", 
    "mss", "mst", "msu", "msv", "msw", "msx", "msy", "msz", 
    "mt",  "mta", "mtb", "mtc", "mtd", "mte", "mtf", "mtg", 
    "mth", "mti", "mtj", "mtk", "mtl", "mtm", "mtn", "mto", 
    "mtp", "mtq", "mtr", "mts", "mtt", "mtu", "mtv", "mtw", 
    "mtx", "mty", "mtz", "mua", "mub", "muc", "mud", "mue", 
    "mug", "muh", "mui", "muj", "muk", "mul", "mum", "mun", 
    "muo", "mup", "muq", "mur", "mus", "mut", "muu", "muv", 
    "muw", "mux", "muy", "muz", "mva", "mvb", "mvc", "mvd", 
    "mve", "mvf", "mvg", "mvh", "mvi", "mvj", "mvk", "mvl", 
    "mvm", "mvn", "mvo", "mvp", "mvq", "mvr", "mvs", "mvt", 
    "mvu", "mvv", "mvw", "mvx", "mvy", "mvz", "mwa", "mwb", 
    "mwc", "mwd", "mwe", "mwf", "mwg", "mwh", "mwi", "mwj", 
    "mwk", "mwl", "mwm", "mwn", "mwo", "mwp", "mwq", "mwr", 
    "mws", "mwt", "mwu", "mwv", "mww", "mwx", "mwy", "mwz", 
    "mxa", "mxb", "mxc", "mxd", "mxe", "mxf", "mxg", "mxh", 
    "mxi", "mxj", "mxk", "mxl", "mxm", "mxn", "mxo", "mxp", 
    "mxq", "mxr", "mxs", "mxt", "mxu", "mxv", "mxw", "mxx", 
    "mxy", "mxz", "my",  "myb", "myc", "myd", "mye", "myf", 
    "myg", "myh", "myi", "myj", "myk", "myl", "mym", "myn", 
    "myo", "myp", "myq", "myr", "mys", "myt", "myu", "myv", 
    "myw", "myx", "myy", "myz", "mza", "mzb", "mzc", "mzd", 
    "mze", "mzf", "mzg", "mzh", "mzi", "mzj", "mzk", "mzl", 
    "mzm", "mzn", "mzo", "mzp", "mzq", "mzr", "mzs", "mzt", 
    "mzu", "mzv", "mzw", "mzx", "mzy", "mzz", 
    "na",  "naa", "nab", "nac", "nad", "nae", "naf", "nag", 
    "nah", "nai", "naj", "nak", "nal", "nam", "nan", "nao", 
    "nap", "naq", "nar", "nas", "nat", "naw", "nax", "nay", 
    "naz", "nb",  "nba", "nbb", "nbc", "nbd", "nbe", "nbf", 
    "nbg", "nbh", "nbi", "nbj", "nbk", "nbm", "nbn", "nbo", 
    "nbp", "nbq", "nbr", "nbs", "nbt", "nbu", "nbv", "nbw", 
    "nbx", "nby", "nca", "ncb", "ncc", "ncd", "nce", "ncf", 
    "ncg", "nch", "nci", "ncj", "nck", "ncl", "ncm", "ncn", 
    "nco", "ncp", "ncr", "ncs", "nct", "ncu", "ncx", "ncz", 
    "nd",  "nda", "ndb", "ndc", "ndd", "ndf", "ndg", "ndh", 
    "ndi", "ndj", "ndk", "ndl", "ndm", "ndn", "ndp", "ndq", 
    "ndr", "nds", "ndt", "ndu", "ndv", "ndw", "ndx", "ndy", 
    "ndz", "ne",  "nea", "neb", "nec", "ned", "nee", "nef", 
    "neg", "neh", "nei", "nej", "nek", "nem", "nen", "neo", 
    "neq", "ner", "nes", "net", "neu", "nev", "new", "nex", 
    "ney", "nez", "nfa", "nfd", "nfg", "nfk", "nfl", "nfr", 
    "nfu", "ng",  "nga", "ngb", "ngc", "ngd", "nge", "ngg", 
    "ngh", "ngi", "ngj", "ngk", "ngl", "ngm", "ngn", "ngo", 
    "ngp", "ngq", "ngr", "ngs", "ngt", "ngu", "ngv", "ngw", 
    "ngx", "ngy", "ngz", "nha", "nhb", "nhc", "nhd", "nhe", 
    "nhf", "nhg", "nhh", "nhi", "nhj", "nhk", "nhm", "nhn", 
    "nho", "nhp", "nhq", "nhr", "nhs", "nht", "nhu", "nhv", 
    "nhw", "nhx", "nhy", "nhz", "nia", "nib", "nic", "nid", 
    "nie", "nif", "nig", "nih", "nii", "nij", "nik", "nil", 
    "nim", "nin", "nio", "niq", "nir", "nis", "nit", "niu", 
    "niv", "niw", "nix", "niy", "niz", "nja", "njb", "njd", 
    "njh", "nji", "njj", "njl", "njm", "njn", "njo", "njr", 
    "njs", "njt", "nju", "njx", "njy", "njz", "nka", "nkb", 
    "nkc", "nkd", "nke", "nkf", "nkg", "nkh", "nki", "nkj", 
    "nkk", "nkm", "nkn", "nko", "nkp", "nkq", "nkr", "nks", 
    "nkt", "nku", "nkv", "nkw", "nkx", "nky", "nkz", "nl",  
    "nla", "nlc", "nle", "nlg", "nli", "nlj", "nlk", "nll", 
    "nln", "nlo", "nlq", "nlr", "nlu", "nlv", "nlw", "nlx", 
    "nly", "nlz", "nma", "nmb", "nmc", "nmd", "nme", "nmf", 
    "nmg", "nmh", "nmi", "nmj", "nmk", "nml", "nmm", "nmn", 
    "nmo", "nmp", "nmq", "nmr", "nms", "nmt", "nmu", "nmv", 
    "nmw", "nmx", "nmy", "nmz", "nn",  "nna", "nnb", "nnc", 
    "nnd", "nne", "nnf", "nng", "nnh", "nni", "nnj", "nnk", 
    "nnl", "nnm", "nnn", "nnp", "nnq", "nnr", "nns", "nnt", 
    "nnu", "nnv", "nnw", "nnx", "nny", "nnz", "no",  "noa", 
    "noc", "nod", "noe", "nof", "nog", "noh", "noi", "noj", 
    "nok", "nol", "nom", "non", "noo", "nop", "noq", "nos", 
    "not", "nou", "nov", "now", "noy", "noz", "npa", "npb", 
    "npg", "nph", "npi", "npl", "npn", "npo", "nps", "npu", 
    "npy", "nqg", "nqk", "nqm", "nqn", "nqo", "nqq", "nqy", 
    "nr",  "nra", "nrb", "nrc", "nre", "nrg", "nri", "nrk", 
    "nrl", "nrm", "nrn", "nrp", "nrr", "nrt", "nru", "nrx", 
    "nrz", "nsa", "nsc", "nsd", "nse", "nsf", "nsg", "nsh", 
    "nsi", "nsk", "nsl", "nsm", "nsn", "nso", "nsp", "nsq", 
    "nsr", "nss", "nst", "nsu", "nsv", "nsw", "nsx", "nsy", 
    "nsz", "nte", "ntg", "nti", "ntj", "ntk", "ntm", "nto", 
    "ntp", "ntr", "nts", "ntu", "ntw", "ntx", "nty", "ntz", 
    "nua", "nub", "nuc", "nud", "nue", "nuf", "nug", "nuh", 
    "nui", "nuj", "nuk", "nul", "num", "nun", "nuo", "nup", 
    "nuq", "nur", "nus", "nut", "nuu", "nuv", "nuw", "nux", 
    "nuy", "nuz", "nv",  "nvh", "nvm", "nvo", "nwa", "nwb", 
    "nwc", "nwe", "nwg", "nwi", "nwm", "nwo", "nwr", "nwx", 
    "nwy", "nxa", "nxd", "nxe", "nxg", "nxi", "nxj", "nxk", 
    "nxl", "nxm", "nxn", "nxq", "nxr", "nxu", "nxx", "ny",  
    "nyb", "nyc", "nyd", "nye", "nyf", "nyg", "nyh", "nyi", 
    "nyj", "nyk", "nyl", "nym", "nyn", "nyo", "nyp", "nyq", 
    "nyr", "nys", "nyt", "nyu", "nyv", "nyw", "nyx", "nyy", 
    "nza", "nzb", "nzi", "nzk", "nzm", "nzs", "nzu", "nzy", 
    "nzz", 
    "oaa", "oac", "oar", "oav", "obi", "obk", "obl", "obm", 
    "obo", "obr", "obt", "obu", "oc",  "oca", "occ", "och", 
    "oco", "ocu", "oda", "odk", "odt", "odu", "ofo", "ofs", 
    "ofu", "ogb", "ogc", "oge", "ogg", "ogn", "ogo", "ogu", 
    "oht", "ohu", "oia", "oin", "oj",  "ojb", "ojc", "ojg", 
    "ojp", "ojs", "ojv", "ojw", "oka", "okb", "okd", "oke", 
    "okg", "okh", "oki", "okj", "okk", "okl", "okm", "okn", 
    "oko", "okr", "oks", "oku", "okv", "okx", "ola", "old", 
    "ole", "olk", "olm", "olo", "olr", "om",  "oma", "omb", 
    "omc", "ome", "omg", "omi", "omk", "oml", "omn", "omo", 
    "omp", "omr", "omt", "omu", "omw", "omx", "ona", "onb", 
    "one", "ong", "oni", "onj", "onk", "onn", "ono", "onp", 
    "onr", "ons", "ont", "onu", "onw", "onx", "ood", "oog", 
    "oon", "oor", "oos", "opa", "ope", "opk", "opm", "opo", 
    "opt", "opy", "or",  "ora", "orc", "ore", "org", "orh", 
    "ork", "orn", "oro", "orr", "ors", "ort", "oru", "orv", 
    "orw", "orx", "ory", "orz", "os",  "osa", "osc", "osi", 
    "oso", "osp", "ost", "osu", "osx", "ota", "otb", "otd", 
    "ote", "oti", "otk", "otl", "otm", "otn", "oto", "otq", 
    "otr", "ots", "ott", "otu", "otw", "otx", "oty", "otz", 
    "oua", "oub", "oue", "oui", "oum", "oun", "owi", "owl", 
    "oyb", "oyd", "oym", "oyy", "ozm", 
    "pa",  "paa", "pab", "pac", "pad", "pae", "paf", "pag", 
    "pah", "pai", "paj", "pak", "pal", "pam", "pao", "pap", 
    "paq", "par", "pas", "pat", "pau", "pav", "paw", "pax", 
    "pay", "paz", "pbb", "pbc", "pbe", "pbf", "pbg", "pbh", 
    "pbi", "pbl", "pbn", "pbo", "pbp", "pbr", "pbs", "pbt", 
    "pbu", "pbv", "pby", "pbz", "pca", "pcb", "pcc", "pcd", 
    "pce", "pcf", "pcg", "pch", "pci", "pcj", "pck", "pcl", 
    "pcm", "pcn", "pcp", "pcr", "pcw", "pda", "pdc", "pdi", 
    "pdn", "pdo", "pdt", "pdu", "pea", "peb", "pec", "ped", 
    "pee", "pef", "peg", "peh", "pei", "pej", "pek", "pel", 
    "pem", "pen", "peo", "pep", "peq", "pes", "pev", "pex", 
    "pey", "pez", "pfa", "pfe", "pfl", "pga", "pgg", "pgi", 
    "pgk", "pgl", "pgn", "pgs", "pgu", "pgy", "pha", "phd", 
    "phg", "phh", "phi", "phk", "phl", "phm", "phn", "pho", 
    "phq", "phr", "pht", "phu", "phv", "phw", "pi",  "pia", 
    "pib", "pic", "pid", "pie", "pif", "pig", "pih", "pii", 
    "pij", "pil", "pim", "pin", "pio", "pip", "pir", "pis", 
    "pit", "piu", "piv", "piw", "pix", "piy", "piz", "pjt", 
    "pka", "pkb", "pkc", "pkg", "pkh", "pkn", "pko", "pkp", 
    "pkr", "pks", "pkt", "pku", "pl",  "pla", "plb", "plc", 
    "pld", "ple", "plg", "plh", "plj", "plk", "pll", "plm", 
    "pln", "plo", "plp", "plq", "plr", "pls", "plt", "plu", 
    "plv", "plw", "ply", "plz", "pma", "pmb", "pmc", "pmd", 
    "pme", "pmf", "pmh", "pmi", "pmj", "pmk", "pml", "pmm", 
    "pmn", "pmo", "pmq", "pmr", "pms", "pmt", "pmu", "pmw", 
    "pmx", "pmy", "pmz", "pna", "pnb", "pnc", "pne", "png", 
    "pnh", "pni", "pnj", "pnk", "pnm", "pnn", "pno", "pnp", 
    "pnq", "pnr", "pns", "pnt", "pnu", "pnv", "pnw", "pnx", 
    "pny", "pnz", "poa", "pob", "poc", "pod", "poe", "pof", 
    "pog", "poh", "poi", "poj", "pok", "pom", "pon", "poo", 
    "pop", "poq", "pos", "pot", "pou", "pov", "pow", "pox", 
    "poy", "ppa", "ppe", "ppi", "ppk", "ppl", "ppm", "ppn", 
    "ppo", "ppp", "ppq", "ppr", "pps", "ppt", "ppu", "ppv", 
    "pqa", "pqm", "pra", "prb", "prc", "prd", "pre", "prf", 
    "prg", "prh", "pri", "prk", "prl", "prm", "prn", "pro", 
    "prp", "prq", "prr", "prs", "prt", "pru", "prv", "prw", 
    "prx", "pry", "prz", "ps",  "psa", "psc", "psd", "pse", 
    "psg", "psh", "psi", "psl", "psm", "psn", "pso", "psp", 
    "psq", "psr", "pss", "pst", "psu", "psw", "psy", "pt",  
    "pta", "pth", "pti", "ptn", "pto", "ptp", "ptr", "ptt", 
    "ptu", "ptv", "ptw", "pty", "pua", "pub", "puc", "pud", 
    "pue", "puf", "pug", "pui", "puj", "puk", "pum", "pun", 
    "puo", "pup", "puq", "pur", "put", "puu", "puw", "pux", 
    "puy", "puz", "pwa", "pwb", "pwg", "pwi", "pwm", "pwn", 
    "pwo", "pwr", "pww", "pxm", "pye", "pym", "pyn", "pys", 
    "pyu", "pyx", "pyy", "pzn", 
    "qaa", "qab", "qac", "qad", "qae", "qaf", "qag", "qah", 
    "qai", "qaj", "qak", "qal", "qam", "qan", "qao", "qap", 
    "qaq", "qar", "qas", "qat", "qau", "qav", "qaw", "qax", 
    "qay", "qaz", "qba", "qbb", "qbc", "qbd", "qbe", "qbf", 
    "qbg", "qbh", "qbi", "qbj", "qbk", "qbl", "qbm", "qbn", 
    "qbo", "qbp", "qbq", "qbr", "qbs", "qbt", "qbu", "qbv", 
    "qbw", "qbx", "qby", "qbz", "qca", "qcb", "qcc", "qcd", 
    "qce", "qcf", "qcg", "qch", "qci", "qcj", "qck", "qcl", 
    "qcm", "qcn", "qco", "qcp", "qcq", "qcr", "qcs", "qct", 
    "qcu", "qcv", "qcw", "qcx", "qcy", "qcz", "qda", "qdb", 
    "qdc", "qdd", "qde", "qdf", "qdg", "qdh", "qdi", "qdj", 
    "qdk", "qdl", "qdm", "qdn", "qdo", "qdp", "qdq", "qdr", 
    "qds", "qdt", "qdu", "qdv", "qdw", "qdx", "qdy", "qdz", 
    "qea", "qeb", "qec", "qed", "qee", "qef", "qeg", "qeh", 
    "qei", "qej", "qek", "qel", "qem", "qen", "qeo", "qep", 
    "qeq", "qer", "qes", "qet", "qeu", "qev", "qew", "qex", 
    "qey", "qez", "qfa", "qfb", "qfc", "qfd", "qfe", "qff", 
    "qfg", "qfh", "qfi", "qfj", "qfk", "qfl", "qfm", "qfn", 
    "qfo", "qfp", "qfq", "qfr", "qfs", "qft", "qfu", "qfv", 
    "qfw", "qfx", "qfy", "qfz", "qga", "qgb", "qgc", "qgd", 
    "qge", "qgf", "qgg", "qgh", "qgi", "qgj", "qgk", "qgl", 
    "qgm", "qgn", "qgo", "qgp", "qgq", "qgr", "qgs", "qgt", 
    "qgu", "qgv", "qgw", "qgx", "qgy", "qgz", "qha", "qhb", 
    "qhc", "qhd", "qhe", "qhf", "qhg", "qhh", "qhi", "qhj", 
    "qhk", "qhl", "qhm", "qhn", "qho", "qhp", "qhq", "qhr", 
    "qhs", "qht", "qhu", "qhv", "qhw", "qhx", "qhy", "qhz", 
    "qia", "qib", "qic", "qid", "qie", "qif", "qig", "qih", 
    "qii", "qij", "qik", "qil", "qim", "qin", "qio", "qip", 
    "qiq", "qir", "qis", "qit", "qiu", "qiv", "qiw", "qix", 
    "qiy", "qiz", "qja", "qjb", "qjc", "qjd", "qje", "qjf", 
    "qjg", "qjh", "qji", "qjj", "qjk", "qjl", "qjm", "qjn", 
    "qjo", "qjp", "qjq", "qjr", "qjs", "qjt", "qju", "qjv", 
    "qjw", "qjx", "qjy", "qjz", "qka", "qkb", "qkc", "qkd", 
    "qke", "qkf", "qkg", "qkh", "qki", "qkj", "qkk", "qkl", 
    "qkm", "qkn", "qko", "qkp", "qkq", "qkr", "qks", "qkt", 
    "qku", "qkv", "qkw", "qkx", "qky", "qkz", "qla", "qlb", 
    "qlc", "qld", "qle", "qlf", "qlg", "qlh", "qli", "qlj", 
    "qlk", "qll", "qlm", "qln", "qlo", "qlp", "qlq", "qlr", 
    "qls", "qlt", "qlu", "qlv", "qlw", "qlx", "qly", "qlz", 
    "qma", "qmb", "qmc", "qmd", "qme", "qmf", "qmg", "qmh", 
    "qmi", "qmj", "qmk", "qml", "qmm", "qmn", "qmo", "qmp", 
    "qmq", "qmr", "qms", "qmt", "qmu", "qmv", "qmw", "qmx", 
    "qmy", "qmz", "qna", "qnb", "qnc", "qnd", "qne", "qnf", 
    "qng", "qnh", "qni", "qnj", "qnk", "qnl", "qnm", "qnn", 
    "qno", "qnp", "qnq", "qnr", "qns", "qnt", "qnu", "qnv", 
    "qnw", "qnx", "qny", "qnz", "qoa", "qob", "qoc", "qod", 
    "qoe", "qof", "qog", "qoh", "qoi", "qoj", "qok", "qol", 
    "qom", "qon", "qoo", "qop", "qoq", "qor", "qos", "qot", 
    "qou", "qov", "qow", "qox", "qoy", "qoz", "qpa", "qpb", 
    "qpc", "qpd", "qpe", "qpf", "qpg", "qph", "qpi", "qpj", 
    "qpk", "qpl", "qpm", "qpn", "qpo", "qpp", "qpq", "qpr", 
    "qps", "qpt", "qpu", "qpv", "qpw", "qpx", "qpy", "qpz", 
    "qqa", "qqb", "qqc", "qqd", "qqe", "qqf", "qqg", "qqh", 
    "qqi", "qqj", "qqk", "qql", "qqm", "qqn", "qqo", "qqp", 
    "qqq", "qqr", "qqs", "qqt", "qqu", "qqv", "qqw", "qqx", 
    "qqy", "qqz", "qra", "qrb", "qrc", "qrd", "qre", "qrf", 
    "qrg", "qrh", "qri", "qrj", "qrk", "qrl", "qrm", "qrn", 
    "qro", "qrp", "qrq", "qrr", "qrs", "qrt", "qru", "qrv", 
    "qrw", "qrx", "qry", "qrz", "qsa", "qsb", "qsc", "qsd", 
    "qse", "qsf", "qsg", "qsh", "qsi", "qsj", "qsk", "qsl", 
    "qsm", "qsn", "qso", "qsp", "qsq", "qsr", "qss", "qst", 
    "qsu", "qsv", "qsw", "qsx", "qsy", "qsz", "qta", "qtb", 
    "qtc", "qtd", "qte", "qtf", "qtg", "qth", "qti", "qtj", 
    "qtk", "qtl", "qtm", "qtn", "qto", "qtp", "qtq", "qtr", 
    "qts", "qtt", "qtu", "qtv", "qtw", "qtx", "qty", "qtz", 
    "qu",  "qua", "qub", "quc", "qud", "quf", "qug", "quh", 
    "qui", "quj", "quk", "qul", "qum", "qun", "qup", "quq", 
    "qur", "qus", "qut", "quu", "quv", "quw", "qux", "quy", 
    "quz", "qva", "qvc", "qve", "qvh", "qvi", "qvj", "qvl", 
    "qvm", "qvn", "qvo", "qvp", "qvs", "qvw", "qvy", "qvz", 
    "qwa", "qwc", "qwh", "qwm", "qws", "qwt", "qxa", "qxc", 
    "qxh", "qxi", "qxl", "qxn", "qxo", "qxp", "qxq", "qxr", 
    "qxs", "qxt", "qxu", "qxw", "qya", "qyp", 
    "raa", "rab", "rac", "rad", "rae", "raf", "rag", "rah", 
    "rai", "raj", "rak", "ral", "ram", "ran", "rao", "rap", 
    "raq", "rar", "ras", "rat", "rau", "rav", "raw", "rax", 
    "ray", "raz", "rbb", "rbk", "rbl", "rbp", "rcf", "rdb", 
    "rea", "reb", "ree", "reg", "rei", "rej", "rel", "rem", 
    "ren", "rer", "res", "ret", "rey", "rga", "rge", "rgk", 
    "rgn", "rgr", "rgs", "rgu", "rhg", "rhp", "ria", "rie", 
    "rif", "ril", "rim", "rin", "rir", "rit", "riu", "rjb", 
    "rjg", "rji", "rjs", "rka", "rkb", "rkh", "rki", "rkm", 
    "rkt", "rkw", "rm",  "rma", "rmb", "rmc", "rmd", "rme", 
    "rmf", "rmg", "rmh", "rmi", "rmk", "rml", "rmm", "rmn", 
    "rmo", "rmp", "rmq", "rmr", "rms", "rmt", "rmu", "rmv", 
    "rmw", "rmx", "rmy", "rmz", "rn",  "rna", "rnd", "rng", 
    "rnl", "rnn", "rnp", "rnr", "rnw", "ro",  "roa", "rob", 
    "roc", "rod", "roe", "rof", "rog", "rol", "rom", "roo", 
    "rop", "ror", "rou", "row", "rpn", "rpt", "rri", "rro", 
    "rrt", "rsb", "rsi", "rsl", "rtc", "rth", "rtm", "rtw", 
    "ru",  "rub", "ruc", "rue", "ruf", "rug", "ruh", "rui", 
    "ruk", "ruo", "rup", "ruq", "rut", "ruu", "ruy", "ruz", 
    "rw",  "rwa", "rwk", "rwm", "rwo", "rwr", "rws", "rxd", 
    "rxw", "ryn", "rys", "ryu", 
    "sa",  "saa", "sab", "sac", "sad", "sae", "saf", "sah", 
    "sai", "saj", "sak", "sal", "sam", "sao", "sap", "saq", 
    "sar", "sas", "sat", "sau", "sav", "saw", "sax", "say", 
    "saz", "sba", "sbb", "sbc", "sbd", "sbe", "sbf", "sbg", 
    "sbh", "sbi", "sbj", "sbk", "sbl", "sbm", "sbn", "sbo", 
    "sbp", "sbq", "sbr", "sbs", "sbt", "sbu", "sbv", "sbw", 
    "sbx", "sby", "sbz", "sc",  "sca", "scb", "scc", "sce", 
    "scf", "scg", "sch", "sci", "sck", "scl", "scn", "sco", 
    "scp", "scq", "scr", "scs", "scu", "scv", "scw", "scx", 
    "sd",  "sda", "sdb", "sdc", "sdd", "sde", "sdf", "sdg", 
    "sdh", "sdi", "sdj", "sdk", "sdl", "sdm", "sdn", "sdo", 
    "sdp", "sdr", "sds", "sdt", "sdu", "sdx", "sdz", "se",  
    "sea", "seb", "sec", "sed", "see", "sef", "seg", "seh", 
    "sei", "sej", "sek", "sel", "sem", "sen", "seo", "sep", 
    "seq", "ser", "ses", "set", "seu", "sev", "sew", "sey", 
    "sez", "sfb", "sfe", "sfm", "sfs", "sfw", "sg",  "sga", 
    "sgb", "sgc", "sgd", "sge", "sgg", "sgh", "sgi", "sgj", 
    "sgk", "sgl", "sgm", "sgn", "sgo", "sgp", "sgr", "sgs", 
    "sgt", "sgu", "sgw", "sgx", "sgy", "sgz", /*sh*/ "sha", /* sh is deprecated */
    "shb", "shc", "shd", "she", "shg", "shh", "shi", "shj", 
    "shk", "shl", "shm", "shn", "sho", "shp", "shq", "shr", 
    "shs", "sht", "shu", "shv", "shw", "shx", "shy", "shz", 
    "si",  "sia", "sib", "sic", "sid", "sie", "sif", "sig", 
    "sih", "sii", "sij", "sik", "sil", "sim", "sio", "sip", 
    "siq", "sir", "sis", "sit", "siu", "siv", "siw", "six", 
    "siy", "siz", "sja", "sjb", "sjd", "sje", "sjg", "sjk", 
    "sjl", "sjm", "sjn", "sjo", "sjp", "sjr", "sjs", "sjt", 
    "sju", "sjw", "sk",  "ska", "skb", "skc", "skd", "ske", 
    "skf", "skg", "skh", "ski", "skj", "skk", "skl", "skm", 
    "skn", "sko", "skp", "skq", "skr", "sks", "skt", "sku", 
    "skv", "skw", "skx", "sky", "skz", "sl",  "sla", "slb", 
    "slc", "sld", "sle", "slf", "slg", "slh", "sli", "slj", 
    "sll", "slm", "sln", "slp", "slq", "slr", "sls", "slt", 
    "slu", "slw", "slx", "sly", "slz", "sm",  "sma", "smb", 
    "smc", "smd", "smf", "smg", "smh", "smi", "smj", "smk", 
    "sml", "smm", "smn", "smp", "smq", "smr", "sms", "smt", 
    "smu", "smv", "smw", "smx", "smy", "smz", "sn",  "snb", 
    "snc", "sne", "snf", "sng", "snh", "sni", "snj", "snk", 
    "snl", "snm", "snn", "sno", "snp", "snq", "snr", "sns", 
    "snu", "snv", "snw", "snx", "sny", "snz", "so",  "soa", 
    "sob", "soc", "sod", "soe", "sog", "soh", "soi", "soj", 
    "sok", "sol", "son", "soo", "sop", "soq", "sor", "sos", 
    "sou", "sov", "sow", "sox", "soy", "soz", "spb", "spc", 
    "spd", "spe", "spg", "spi", "spk", "spl", "spm", "spo", 
    "spp", "spq", "spr", "sps", "spt", "spu", "spv", "spx", 
    "spy", "sq",  "sqa", "sqh", "sqk", "sqm", "sqn", "sqo", 
    "sqq", "sqr", "sqs", "sqt", "squ", "sr",  "sra", "srb", 
    "src", "sre", "srf", "srg", "srh", "sri", "srj", "srk", 
    "srl", "srm", "srn", "sro", "srq", "srr", "srs", "srt", 
    "sru", "srv", "srw", "srx", "sry", "srz", "ss",  "ssa", 
    "ssb", "ssc", "ssd", "sse", "ssf", "ssg", "ssh", "ssi", 
    "ssj", "ssk", "ssl", "ssm", "ssn", "sso", "ssp", "ssq", 
    "ssr", "sss", "sst", "ssu", "ssv", "ssx", "ssy", "ssz", 
    "st",  "sta", "stb", "stc", "std", "ste", "stf", "stg", 
    "sth", "sti", "stj", "stk", "stl", "stm", "stn", "sto", 
    "stp", "stq", "str", "sts", "stt", "stu", "stv", "stw", 
    "su",  "sua", "sub", "suc", "sue", "suf", "sug", "suh", 
    "sui", "suj", "suk", "sul", "sum", "suq", "sur", "sus", 
    "sut", "suu", "suv", "suw", "sux", "suy", "suz", "sv",  
    "sva", "svb", "svc", "sve", "svk", "svm", "svr", "svs", 
    "svx", "sw",  "swb", "swc", "swf", "swg", "swh", "swi", 
    "swj", "swk", "swl", "swm", "swn", "swo", "swp", "swq", 
    "swr", "sws", "swt", "swu", "swv", "sww", "swx", "swy", 
    "sxb", "sxc", "sxe", "sxg", "sxk", "sxl", "sxm", "sxn", 
    "sxo", "sxr", "sxs", "sxu", "sxw", "sya", "syb", "syc", 
    "syi", "syk", "syl", "sym", "syn", "syo", "syr", "sys", 
    "syw", "syy", "sza", "szb", "szc", "szd", "sze", "szg", 
    "szk", "szl", "szn", "szp", "szv", "szw", 
    "ta",  "taa", "tab", "tac", "tad", "tae", "taf", "tag", 
    "tai", "taj", "tak", "tal", "tan", "tao", "tap", "taq", 
    "tar", "tas", "tau", "tav", "taw", "tax", "tay", "taz", 
    "tba", "tbb", "tbc", "tbd", "tbe", "tbf", "tbg", "tbh", 
    "tbi", "tbj", "tbk", "tbl", "tbm", "tbn", "tbo", "tbp", 
    "tbr", "tbs", "tbt", "tbu", "tbv", "tbw", "tbx", "tby", 
    "tbz", "tca", "tcb", "tcc", "tcd", "tce", "tcf", "tcg", 
    "tch", "tci", "tck", "tcl", "tcm", "tcn", "tco", "tcp", 
    "tcq", "tcs", "tct", "tcu", "tcw", "tcx", "tcy", "tcz", 
    "tda", "tdb", "tdc", "tdd", "tde", "tdf", "tdg", "tdh", 
    "tdi", "tdj", "tdk", "tdl", "tdn", "tdo", "tdq", "tdr", 
    "tds", "tdt", "tdu", "tdv", "tdx", "tdy", "te",  "tea", 
    "teb", "tec", "ted", "tee", "tef", "teg", "teh", "tei", 
    "tek", "tem", "ten", "teo", "tep", "teq", "ter", "tes", 
    "tet", "teu", "tev", "tew", "tex", "tey", "tfi", "tfn", 
    "tfo", "tfr", "tft", "tg",  "tga", "tgb", "tgc", "tgd", 
    "tge", "tgf", "tgg", "tgh", "tgi", "tgj", "tgn", "tgo", 
    "tgp", "tgq", "tgr", "tgs", "tgt", "tgu", "tgv", "tgw", 
    "tgx", "tgy", "tgz", "th",  "thc", "thd", "the", "thf", 
    "thh", "thi", "thk", "thl", "thm", "thn", "thp", "thq", 
    "thr", "ths", "tht", "thu", "thv", "thw", "thx", "thy", 
    "thz", "ti",  "tia", "tic", "tid", "tie", "tif", "tig", 
    "tih", "tii", "tij", "tik", "til", "tim", "tin", "tio", 
    "tip", "tiq", "tis", "tit", "tiu", "tiv", "tiw", "tix", 
    "tiy", "tiz", "tja", "tjg", "tji", "tjl", "tjm", "tjn", 
    "tjo", "tjs", "tju", "tjw", "tk",  "tka", "tkb", "tkd", 
    "tke", "tkf", "tkg", "tkk", "tkl", "tkm", "tkn", "tkp", 
    "tkq", "tkr", "tks", "tkt", "tku", "tkw", "tkx", "tkz", 
    "tl",  "tla", "tlb", "tlc", "tld", "tle", "tlf", "tlg", 
    "tlh", "tli", "tlj", "tlk", "tll", "tlm", "tln", "tlo", 
    "tlp", "tlq", "tlr", "tls", "tlt", "tlu", "tlv", "tlw", 
    "tlx", "tly", "tlz", "tma", "tmb", "tmc", "tmd", "tme", 
    "tmf", "tmg", "tmh", "tmi", "tmj", "tmk", "tml", "tmm", 
    "tmn", "tmo", "tmp", "tmq", "tmr", "tms", "tmt", "tmu", 
    "tmv", "tmw", "tmx", "tmy", "tmz", "tn",  "tna", "tnb", 
    "tnc", "tnd", "tne", "tnf", "tng", "tnh", "tni", "tnj", 
    "tnk", "tnl", "tnm", "tnn", "tno", "tnp", "tnq", "tnr", 
    "tns", "tnt", "tnu", "tnv", "tnw", "tnx", "tny", "tnz", 
    "to",  "tob", "toc", "tod", "toe", "tof", "tog", "toh", 
    "toi", "toj", "tol", "tom", "too", "top", "toq", "tor", 
    "tos", "tot", "tou", "tov", "tow", "tox", "toy", "toz", 
    "tpa", "tpc", "tpe", "tpf", "tpg", "tpi", "tpj", "tpk", 
    "tpl", "tpm", "tpn", "tpo", "tpp", "tpq", "tpr", "tpt", 
    "tpu", "tpv", "tpw", "tpx", "tpy", "tpz", "tqb", "tql", 
    "tqm", "tqn", "tqo", "tqp", "tqq", "tqr", "tqt", "tqu", 
    "tqw", "tr",  "tra", "trb", "trc", "trd", "tre", "trf", 
    "trg", "trh", "tri", "trj", "trl", "trm", "trn", "tro", 
    "trp", "trq", "trr", "trs", "trt", "tru", "trv", "trw", 
    "trx", "try", "trz", "ts",  "tsa", "tsb", "tsc", "tsd", 
    "tse", "tsf", "tsg", "tsh", "tsi", "tsj", "tsk", "tsl", 
    "tsm", "tsp", "tsq", "tsr", "tss", "tst", "tsu", "tsv", 
    "tsw", "tsx", "tsy", "tsz", "tt",  "tta", "ttb", "ttc", 
    "ttd", "tte", "ttf", "ttg", "tth", "tti", "ttj", "ttk", 
    "ttl", "ttm", "ttn", "tto", "ttp", "ttq", "ttr", "tts", 
    "ttt", "ttu", "ttv", "ttw", "ttx", "tty", "ttz", "tua", 
    "tub", "tuc", "tud", "tue", "tuf", "tug", "tuh", "tui", 
    "tuj", "tul", "tum", "tun", "tuo", "tup", "tuq", "tus", 
    "tut", "tuu", "tuv", "tux", "tuy", "tuz", "tva", "tvd", 
    "tve", "tvk", "tvl", "tvm", "tvn", "tvo", "tvs", "tvt", 
    "tvu", "tvw", "tvy", "tw",  "twa", "twb", "twc", "twd", 
    "twe", "twf", "twg", "twh", "twl", "twm", "twn", "two", 
    "twp", "twq", "twr", "twt", "twu", "tww", "twx", "twy", 
    "txa", "txb", "txc", "txe", "txg", "txh", "txi", "txm", 
    "txn", "txo", "txq", "txr", "txs", "txt", "txu", "txx", 
    "txy", "ty",  "tya", "tye", "tyh", "tyi", "tyj", "tyl", 
    "tyn", "typ", "tyr", "tys", "tyt", "tyu", "tyv", "tyx", 
    "tyz", "tza", "tzb", "tzc", "tze", "tzh", "tzj", "tzl", 
    "tzm", "tzn", "tzo", "tzs", "tzt", "tzu", "tzx", "tzz", 
    "uam", "uan", "uar", "uba", "ubi", "ubl", "ubm", "ubr", 
    "ubu", "uby", "uda", "ude", "udg", "udi", "udj", "udl", 
    "udm", "udu", "ues", "ufi", "ug",  "uga", "ugb", "uge", 
    "ugn", "ugo", "ugy", "uha", "uhn", "uis", "uiv", "uji", 
    "uk",  "uka", "ukg", "ukh", "ukl", "ukp", "ukq", "uks", 
    "uku", "ukw", "uky", "ula", "ulb", "ulc", "ule", "ulf", 
    "uli", "ulk", "ull", "ulm", "uln", "ulu", "ulw", "uma", 
    "umb", "umc", "umd", "umg", "umi", "umm", "umn", "umo", 
    "ump", "umr", "ums", "umu", "una", "und", "une", "ung", 
    "unk", "unm", "unn", "unp", "unr", "unu", "unx", "unz", 
    "uok", "upi", "upv", "ur",  "ura", "urb", "urc", "ure", 
    "urf", "urg", "urh", "uri", "urk", "url", "urm", "urn", 
    "uro", "urp", "urr", "urt", "uru", "urv", "urw", "urx", 
    "ury", "urz", "usa", "ush", "usi", "usk", "usp", "usu", 
    "uta", "ute", "utp", "utr", "utu", "uum", "uun", "uur", 
    "uuu", "uve", "uvh", "uvl", "uwa", "uya", "uz",  "uzn", 
    "uzs", 
    "vaa", "vae", "vaf", "vag", "vah", "vai", "vaj", "val", 
    "vam", "van", "vao", "vap", "var", "vas", "vau", "vav", 
    "vay", "vbb", "vbk", "ve",  "vec", "ved", "vel", "vem", 
    "veo", "vep", "ver", "vgr", "vgt", "vi",  "vic", "vid", 
    "vif", "vig", "vil", "vin", "vis", "vit", "viv", "vka", 
    "vki", "vkj", "vkk", "vkl", "vkm", "vko", "vkp", "vkt", 
    "vku", "vky", "vlp", "vlr", "vls", "vma", "vmb", "vmc", 
    "vmd", "vme", "vmf", "vmg", "vmh", "vmi", "vmj", "vmk", 
    "vml", "vmm", "vmo", "vmp", "vmq", "vmr", "vms", "vmu", 
    "vmv", "vmw", "vmx", "vmy", "vmz", "vnk", "vnm", "vnp", 
    "vo",  "vor", "vot", "vra", "vro", "vrs", "vrt", "vsi", 
    "vsl", "vsv", "vto", "vum", "vun", "vut", "vwa", 
    "wa",  "waa", "wab", "wac", "wad", "wae", "waf", "wag", 
    "wah", "wai", "waj", "wak", "wal", "wam", "wan", "wao", 
    "wap", "waq", "war", "was", "wat", "wau", "wav", "waw", 
    "wax", "way", "waz", "wba", "wbb", "wbe", "wbf", "wbh", 
    "wbi", "wbj", "wbk", "wbl", "wbm", "wbp", "wbq", "wbr", 
    "wbt", "wbv", "wbw", "wca", "wci", "wdd", "wdg", "wdj", 
    "wdk", "wdu", "wdy", "wea", "wec", "wed", "weg", "weh", 
    "wei", "wem", "wen", "weo", "wep", "wer", "wes", "wet", 
    "weu", "wew", "wfg", "wga", "wgb", "wgg", "wgi", "wgo", 
    "wgu", "wgw", "wgy", "wha", "whg", "whk", "whu", "wib", 
    "wic", "wie", "wif", "wig", "wih", "wii", "wij", "wik", 
    "wil", "wim", "win", "wir", "wit", "wiu", "wiv", "wiw", 
    "wiy", "wja", "wji", "wka", "wkb", "wkd", "wkl", "wku", 
    "wkw", "wky", "wla", "wlc", "wle", "wlg", "wli", "wlk", 
    "wll", "wlm", "wlo", "wlr", "wls", "wlu", "wlv", "wlw", 
    "wlx", "wly", "wma", "wmb", "wmc", "wmd", "wme", "wmh", 
    "wmi", "wmm", "wmn", "wmo", "wms", "wmt", "wmw", "wmx", 
    "wnb", "wnc", "wnd", "wne", "wng", "wni", "wnk", "wnm", 
    "wnn", "wno", "wnp", "wnu", "wnw", "wny", "wo",  "woa", 
    "wob", "woc", "wod", "woe", "wof", "wog", "woi", "wok", 
    "wom", "won", "woo", "wor", "wos", "wow", "woy", "wpc", 
    "wra", "wrb", "wrd", "wre", "wrg", "wrh", "wri", "wrk", 
    "wrl", "wrm", "wrn", "wro", "wrp", "wrr", "wrs", "wru", 
    "wrv", "wrw", "wrx", "wry", "wrz", "wsa", "wsi", "wsk", 
    "wsr", "wss", "wsu", "wsv", "wtf", "wth", "wti", "wtk", 
    "wtm", "wtw", "wua", "wub", "wud", "wuh", "wul", "wum", 
    "wun", "wur", "wut", "wuu", "wuv", "wux", "wuy", "wwa", 
    "wwb", "wwo", "wwr", "www", "wxa", "wxw", "wya", "wyb", 
    "wyi", "wym", "wyr", "wyy", 
    "xaa", "xab", "xac", "xad", "xae", "xag", "xah", "xai", 
    "xal", "xam", "xan", "xao", "xap", "xaq", "xar", "xas", 
    "xat", "xau", "xav", "xaw", "xay", "xba", "xbb", "xbc", 
    "xbd", "xbe", "xbg", "xbi", "xbj", "xbm", "xbn", "xbo", 
    "xbp", "xbr", "xbw", "xbx", "xby", "xcb", "xcc", "xce", 
    "xcg", "xch", "xcl", "xcm", "xcn", "xco", "xcr", "xct", 
    "xcu", "xcv", "xcw", "xcy", "xda", "xdc", "xdk", "xdm", 
    "xdy", "xeb", "xed", "xeg", "xel", "xem", "xep", "xer", 
    "xes", "xet", "xeu", "xfa", "xga", "xgb", "xgd", "xgf", 
    "xgg", "xgi", "xgl", "xgm", "xgr", "xgu", "xgw", "xh",  
    "xha", "xhc", "xhd", "xhe", "xhr", "xht", "xhu", "xhv", 
    "xia", "xib", "xii", "xil", "xin", "xip", "xir", "xiv", 
    "xiy", "xjb", "xjt", "xka", "xkb", "xkc", "xkd", "xke", 
    "xkf", "xkg", "xkh", "xki", "xkj", "xkk", "xkl", "xkm", 
    "xkn", "xko", "xkp", "xkq", "xkr", "xks", "xkt", "xku", 
    "xkv", "xkw", "xkx", "xky", "xkz", "xla", "xlb", "xlc", 
    "xld", "xle", "xlg", "xli", "xln", "xlo", "xlp", "xls", 
    "xlu", "xly", "xma", "xmb", "xmc", "xmd", "xme", "xmf", 
    "xmg", "xmh", "xmi", "xmj", "xmk", "xml", "xmm", "xmn", 
    "xmo", "xmp", "xmq", "xmr", "xms", "xmt", "xmu", "xmv", 
    "xmw", "xmx", "xmy", "xmz", "xna", "xnb", "xng", "xnh", 
    "xni", "xnk", "xnn", "xno", "xnr", "xns", "xnt", "xnu", 
    "xny", "xnz", "xoc", "xod", "xog", "xoi", "xok", "xom", 
    "xon", "xoo", "xop", "xor", "xow", "xpa", "xpc", "xpe", 
    "xpg", "xpi", "xpj", "xpk", "xpm", "xpn", "xpo", "xpp", 
    "xpq", "xpr", "xps", "xpt", "xpu", "xpy", "xqa", "xqt", 
    "xra", "xrb", "xrd", "xre", "xrg", "xri", "xrm", "xrn", 
    "xrq", "xrr", "xrt", "xru", "xrw", "xsa", "xsb", "xsc", 
    "xsd", "xse", "xsh", "xsi", "xsj", "xsk", "xsl", "xsm", 
    "xsn", "xso", "xsp", "xsq", "xsr", "xss", "xst", "xsu", 
    "xsv", "xsy", "xta", "xtb", "xtc", "xtd", "xte", "xtg", 
    "xth", "xti", "xtj", "xtl", "xtm", "xtn", "xto", "xtp", 
    "xtq", "xtr", "xts", "xtt", "xtu", "xtv", "xtw", "xty", 
    "xtz", "xua", "xub", "xud", "xuf", "xug", "xuj", "xul", 
    "xum", "xun", "xuo", "xup", "xur", "xut", "xuu", "xve", 
    "xvi", "xvn", "xvo", "xvs", "xwa", "xwc", "xwd", "xwe", 
    "xwg", "xwj", "xwk", "xwl", "xwo", "xwr", "xwt", "xww", 
    "xxb", "xxk", "xxm", "xxr", "xxt", "xya", "xyb", "xyj", 
    "xyk", "xyl", "xyt", "xyy", "xzh", "xzm", "xzp", 
    "yaa", "yab", "yac", "yad", "yae", "yaf", "yag", "yah", 
    "yai", "yaj", "yak", "yal", "yam", "yan", "yao", "yap", 
    "yaq", "yar", "yas", "yat", "yau", "yav", "yaw", "yax", 
    "yay", "yaz", "yba", "ybb", "ybd", "ybe", "ybh", "ybi", 
    "ybj", "ybk", "ybl", "ybm", "ybn", "ybo", "ybx", "yby", 
    "ych", "ycl", "ycn", "ycp", "yda", "ydd", "yde", "ydg", 
    "ydk", "yds", "yea", "yec", "yee", "yei", "yej", "yel", 
    "yen", "yer", "yes", "yet", "yeu", "yev", "yey", "yga", 
    "ygi", "ygl", "ygm", "ygp", "ygr", "ygu", "ygw", "yha", 
    "yhd", "yhl", "yi",  "yia", "yib", "yif", "yig", "yih", 
    "yii", "yij", "yik", "yil", "yim", "yin", "yio", "yip", 
    "yiq", "yir", "yis", "yit", "yiu", "yiv", "yix", "yiy", 
    "yiz", "yka", "ykg", "yki", "ykk", "ykl", "ykm", "ykn", 
    "yko", "ykr", "ykt", "yku", "yky", "yla", "ylb", "yle", 
    "ylg", "yli", "yll", "ylm", "yln", "ylo", "ylr", "ylu", 
    "yly", "yma", "ymb", "ymc", "ymd", "yme", "ymg", "ymh", 
    "ymi", "ymj", "ymk", "yml", "ymm", "ymn", "ymo", "ymp", 
    "ymq", "ymr", "yms", "ymt", "ymx", "ymz", "yna", "ynd", 
    "yne", "yng", "ynh", "ynk", "ynl", "ynn", "yno", "ynq", 
    "yns", "ynu", "yo",  "yob", "yog", "yoi", "yok", "yol", 
    "yom", "yon", "yos", "yot", "yox", "yoy", "ypa", "ypb", 
    "ypg", "yph", "ypk", "ypl", "ypm", "ypn", "ypo", "ypp", 
    "ypw", "ypz", "yra", "yrb", "yre", "yri", "yrk", "yrl", 
    "yrm", "yrn", "yrs", "yrw", "yry", "ysc", "ysd", "ysg", 
    "ysl", "ysn", "yso", "ysp", "ysr", "yss", "ysy", "yta", 
    "ytl", "ytp", "ytw", "yty", "yua", "yub", "yuc", "yud", 
    "yue", "yuf", "yug", "yui", "yuj", "yuk", "yul", "yum", 
    "yun", "yup", "yuq", "yur", "yus", "yut", "yuu", "yuw", 
    "yux", "yuy", "yuz", "yva", "yvt", "ywa", "ywg", "ywl", 
    "ywm", "ywn", "ywq", "ywr", "ywt", "ywu", "yww", "yxa", 
    "yxg", "yxl", "yxm", "yxu", "yxy", "yym", "yyr", "yyu", 
    "yyz", "yzg", "yzk", 
    "za",  "zaa", "zab", "zac", "zad", "zae", "zaf", "zag", 
    "zah", "zai", "zaj", "zak", "zal", "zam", "zao", "zap", 
    "zaq", "zar", "zas", "zat", "zau", "zav", "zaw", "zax", 
    "zay", "zaz", "zbc", "zbe", "zbl", "zbt", "zbw", "zca", 
    "zch", "zdj", "zea", "zeg", "zeh", "zen", "zga", "zgb", 
    "zgh", "zgm", "zgn", "zgr", "zh",  "zhb", "zhd", "zhi", 
    "zhn", "zhw", "zia", "zib", "zik", "zil", "zim", "zin", 
    "zir", "ziw", "ziz", "zka", "zkb", "zkd", "zkg", "zkh", 
    "zkk", "zkn", "zko", "zkp", "zkr", "zkt", "zku", "zkv", 
    "zkz", "zlj", "zlm", "zln", "zlq", "zma", "zmb", "zmc", 
    "zmd", "zme", "zmf", "zmg", "zmh", "zmi", "zmj", "zmk", 
    "zml", "zmm", "zmn", "zmo", "zmp", "zmq", "zmr", "zms", 
    "zmt", "zmu", "zmv", "zmw", "zmx", "zmy", "zmz", "zna", 
    "znd", "zne", "zng", "znk", "zns", "zoc", "zoh", "zom", 
    "zoo", "zoq", "zor", "zos", "zpa", "zpb", "zpc", "zpd", 
    "zpe", "zpf", "zpg", "zph", "zpi", "zpj", "zpk", "zpl", 
    "zpm", "zpn", "zpo", "zpp", "zpq", "zpr", "zps", "zpt", 
    "zpu", "zpv", "zpw", "zpx", "zpy", "zpz", "zqe", "zra", 
    "zrg", "zrn", "zro", "zrp", "zrs", "zsa", "zsk", "zsl", 
    "zsm", "zsr", "zsu", "ztc", "zte", "ztg", "ztl", "ztm", 
    "ztn", "ztp", "ztq", "zts", "ztt", "ztu", "ztx", "zty", 
    "zu",  "zua", "zuh", "zum", "zun", "zuy", "zwa", "zxx", 
    "zyb", "zyg", "zyj", "zyn", "zyp", "zza", "zzj", 
NULL,
    "in",  "iw",  "ji",  "jw",  "sh",    /* obsolete language codes */
NULL
};
static const char* const DEPRECATED_LANGUAGES[]={
    "in", "iw", "ji", "jw", NULL, NULL
};
static const char* const REPLACEMENT_LANGUAGES[]={
    "id", "he", "yi", "jv", NULL, NULL
};

/**
 * Table of 3-letter language codes.
 *
 * This is a lookup table used to convert 3-letter language codes to
 * their 2-letter equivalent, where possible.  It must be kept in sync
 * with LANGUAGES.  For all valid i, LANGUAGES[i] must refer to the
 * same language as LANGUAGES_3[i].  The commented-out lines are
 * copied from LANGUAGES to make eyeballing this baby easier.
 *
 * Where a 3-letter language code has no 2-letter equivalent, the
 * 3-letter code occupies both LANGUAGES[i] and LANGUAGES_3[i].
 *
 * This table should be terminated with a NULL entry, followed by a
 * second list, and another NULL entry.  The two lists correspond to
 * the two lists in LANGUAGES.
 */
/* Generated using org.unicode.cldr.icu.GenerateISO639LanguageTables */
/* ISO639 table version is 20130123 */
static const char * const LANGUAGES_3[] = {
    "aar", "aaa", "aab", "aac", "aad", "aae", "aaf", "aag", 
    "aah", "aai", "aak", "aal", "aam", "aan", "aao", "aap", 
    "aaq", "aas", "aat", "aau", "aaw", "aax", "aay", "aaz", 
    "abk", "aba", "abb", "abc", "abd", "abe", "abf", "abg", 
    "abh", "abi", "abj", "abl", "abm", "abn", "abo", "abp", 
    "abq", "abr", "abs", "abt", "abu", "abv", "abw", "abx", 
    "aby", "abz", "aca", "acb", "acc", "acd", "ace", "acf", 
    "ach", "aci", "ack", "acl", "acm", "acn", "acp", "acq", 
    "acr", "acs", "act", "acu", "acv", "acw", "acx", "acy", 
    "acz", "ada", "adb", "add", "ade", "adf", "adg", "adh", 
    "adi", "adj", "adl", "adn", "ado", "adp", "adq", "adr", 
    "ads", "adt", "adu", "adw", "adx", "ady", "adz", "ave", 
    "aea", "aeb", "aec", "aed", "aee", "aek", "ael", "aem", 
    "aen", "aeq", "aer", "aes", "aeu", "aew", "aex", "aey", 
    "aez", "afr", "afa", "afb", "afd", "afe", "afg", "afh", 
    "afi", "afk", "afn", "afo", "afp", "afs", "aft", "afu", 
    "afz", "aga", "agb", "agc", "agd", "age", "agf", "agg", 
    "agh", "agi", "agj", "agk", "agl", "agm", "agn", "ago", 
    "agp", "agq", "agr", "ags", "agt", "agu", "agv", "agw", 
    "agx", "agy", "agz", "aha", "ahb", "ahe", "ahg", "ahh", 
    "ahi", "ahk", "ahl", "ahm", "ahn", "aho", "ahp", "ahr", 
    "ahs", "aht", "aia", "aib", "aic", "aid", "aie", "aif", 
    "aig", "aih", "aii", "aij", "aik", "ail", "aim", "ain", 
    "aio", "aip", "aiq", "air", "ais", "ait", "aiw", "aix", 
    "aiy", "aiz", "aja", "ajg", "aji", "ajn", "ajp", "ajt", 
    "aju", "ajw", "ajz", "aka", "akb", "akc", "akd", "ake", 
    "akf", "akg", "akh", "aki", "akj", "akk", "akl", "akm", 
    "akn", "ako", "akp", "akq", "akr", "aks", "akt", "aku", 
    "akv", "akw", "akx", "aky", "akz", "ala", "alc", "ald", 
    "ale", "alf", "alg", "alh", "ali", "alj", "alk", "all", 
    "alm", "aln", "alo", "alp", "alq", "alr", "als", "alt", 
    "alu", "alw", "alx", "aly", "alz", "amh", "ama", "amb", 
    "amc", "amd", "ame", "amf", "amg", "ami", "amj", "amk", 
    "aml", "amm", "amn", "amo", "amp", "amq", "amr", "ams", 
    "amt", "amu", "amv", "amw", "amx", "amy", "amz", "arg", 
    "ana", "anb", "anc", "and", "ane", "anf", "ang", "anh", 
    "ani", "anj", "ank", "anl", "anm", "ann", "ano", "anp", 
    "anq", "anr", "ans", "ant", "anu", "anv", "anw", "anx", 
    "any", "anz", "aoa", "aob", "aoc", "aod", "aoe", "aof", 
    "aog", "aoh", "aoi", "aoj", "aok", "aol", "aom", "aon", 
    "aor", "aos", "aot", "aou", "aox", "aoz", "apa", "apb", 
    "apc", "apd", "ape", "apf", "apg", "aph", "api", "apj", 
    "apk", "apl", "apm", "apn", "apo", "app", "apq", "apr", 
    "aps", "apt", "apu", "apv", "apw", "apx", "apy", "apz", 
    "aqc", "aqd", "aqg", "aqm", "aqn", "aqp", "aqr", "aqz", 
    "ara", "arb", "arc", "ard", "are", "arf", "arh", "ari", 
    "arj", "ark", "arl", "arn", "aro", "arp", "arq", "arr", 
    "ars", "art", "aru", "arv", "arw", "arx", "ary", "arz", 
    "asm", "asa", "asb", "asc", "asd", "ase", "asf", "asg", 
    "ash", "asi", "asj", "ask", "asl", "asn", "aso", "asp", 
    "asq", "asr", "ass", "ast", "asu", "asv", "asw", "asx", 
    "asy", "asz", "ata", "atb", "atc", "atd", "ate", "atf", 
    "atg", "ath", "ati", "atj", "atk", "atl", "atm", "atn", 
    "ato", "atp", "atq", "atr", "ats", "att", "atu", "atv", 
    "atw", "atx", "aty", "atz", "aua", "aub", "auc", "aud", 
    "aue", "aug", "auh", "aui", "auj", "auk", "aul", "aum", 
    "aun", "auo", "aup", "auq", "aur", "aus", "aut", "auu", 
    "auv", "auw", "aux", "auy", "auz", "ava", "avb", "avd", 
    "avi", "avk", "avl", "avm", "avn", "avo", "avs", "avt", 
    "avu", "avv", "awa", "awb", "awc", "awe", "awg", "awh", 
    "awi", "awk", "awm", "awn", "awo", "awr", "aws", "awt", 
    "awu", "awv", "aww", "awx", "awy", "axb", "axe", "axg", 
    "axk", "axl", "axm", "axx", "aym", "aya", "ayb", "ayc", 
    "ayd", "aye", "ayg", "ayh", "ayi", "ayk", "ayl", "ayn", 
    "ayo", "ayp", "ayq", "ayr", "ays", "ayt", "ayu", "ayx", 
    "ayy", "ayz", "aze", "aza", "azb", "azd", "azg", "azj", 
    "azm", "azn", "azo", "azr", "azt", "azz", 
    "bak", "baa", "bab", "bac", "bad", "bae", "baf", "bag", 
    "bah", "bai", "baj", "bal", "ban", "bao", "bap", "bar", 
    "bas", "bat", "bau", "bav", "baw", "bax", "bay", "baz", 
    "bba", "bbb", "bbc", "bbd", "bbe", "bbf", "bbg", "bbh", 
    "bbi", "bbj", "bbk", "bbl", "bbm", "bbn", "bbo", "bbp", 
    "bbq", "bbr", "bbs", "bbt", "bbu", "bbv", "bbw", "bbx", 
    "bby", "bbz", "bca", "bcb", "bcc", "bcd", "bce", "bcf", 
    "bcg", "bch", "bci", "bcj", "bck", "bcl", "bcm", "bcn", 
    "bco", "bcp", "bcq", "bcr", "bcs", "bct", "bcu", "bcv", 
    "bcw", "bcx", "bcy", "bcz", "bda", "bdb", "bdc", "bdd", 
    "bde", "bdf", "bdg", "bdh", "bdi", "bdj", "bdk", "bdl", 
    "bdm", "bdn", "bdo", "bdp", "bdq", "bdr", "bds", "bdt", 
    "bdu", "bdv", "bdw", "bdx", "bdy", "bdz", "bel", "bea", 
    "beb", "bec", "bed", "bee", "bef", "beg", "beh", "bei", 
    "bej", "bek", "bem", "beo", "bep", "beq", "ber", "bes", 
    "bet", "beu", "bev", "bew", "bex", "bey", "bez", "bfa", 
    "bfb", "bfc", "bfd", "bfe", "bff", "bfg", "bfh", "bfi", 
    "bfj", "bfk", "bfl", "bfm", "bfn", "bfo", "bfp", "bfq", 
    "bfr", "bfs", "bft", "bfu", "bfw", "bfx", "bfy", "bfz", 
    "bul", "bga", "bgb", "bgc", "bgd", "bge", "bgf", "bgg", 
    "bgh", "bgi", "bgj", "bgk", "bgl", "bgm", "bgn", "bgo", 
    "bgp", "bgq", "bgr", "bgs", "bgt", "bgu", "bgv", "bgw", 
    "bgx", "bgy", "bgz", "bih", "bha", "bhb", "bhc", "bhd", 
    "bhe", "bhf", "bhg", "bhh", "bhi", "bhj", "bhk", "bhl", 
    "bhm", "bhn", "bho", "bhp", "bhq", "bhr", "bhs", "bht", 
    "bhu", "bhv", "bhw", "bhx", "bhy", "bhz", "bis", "bia", 
    "bib", "bic", "bid", "bie", "bif", "big", "bii", "bij", 
    "bik", "bil", "bim", "bin", "bio", "bip", "biq", "bir", 
    "bit", "biu", "biv", "biw", "bix", "biy", "biz", "bja", 
    "bjb", "bjc", "bjd", "bje", "bjf", "bjg", "bjh", "bji", 
    "bjj", "bjk", "bjl", "bjm", "bjn", "bjo", "bjp", "bjq", 
    "bjr", "bjs", "bjt", "bju", "bjv", "bjw", "bjx", "bjy", 
    "bjz", "bka", "bkb", "bkc", "bkd", "bke", "bkf", "bkg", 
    "bkh", "bki", "bkj", "bkk", "bkl", "bkm", "bkn", "bko", 
    "bkp", "bkq", "bkr", "bks", "bkt", "bku", "bkv", "bkw", 
    "bkx", "bky", "bkz", "bla", "blb", "blc", "bld", "ble", 
    "blf", "blg", "blh", "bli", "blj", "blk", "bll", "blm", 
    "bln", "blo", "blp", "blq", "blr", "bls", "blt", "blu", 
    "blv", "blw", "blx", "bly", "blz", "bam", "bma", "bmb", 
    "bmc", "bmd", "bme", "bmf", "bmg", "bmh", "bmi", "bmj", 
    "bmk", "bml", "bmm", "bmn", "bmo", "bmp", "bmq", "bmr", 
    "bms", "bmt", "bmu", "bmv", "bmw", "bmx", "bmy", "bmz", 
    "ben", "bna", "bnb", "bnc", "bnd", "bne", "bnf", "bng", 
    "bnh", "bni", "bnj", "bnk", "bnl", "bnm", "bnn", "bno", 
    "bnp", "bnq", "bnr", "bns", "bnt", "bnu", "bnv", "bnw", 
    "bnx", "bny", "bnz", "bod", "boa", "bob", "boc", "boe", 
    "bof", "bog", "boh", "boi", "boj", "bok", "bol", "bom", 
    "bon", "boo", "bop", "boq", "bor", "bot", "bou", "bov", 
    "bow", "box", "boy", "boz", "bpa", "bpb", "bpd", "bpg", 
    "bph", "bpi", "bpj", "bpk", "bpl", "bpm", "bpn", "bpo", 
    "bpp", "bpq", "bpr", "bps", "bpt", "bpu", "bpv", "bpw", 
    "bpx", "bpy", "bpz", "bqa", "bqb", "bqc", "bqd", "bqe", 
    "bqf", "bqg", "bqh", "bqi", "bqj", "bqk", "bql", "bqm", 
    "bqn", "bqo", "bqp", "bqq", "bqr", "bqs", "bqt", "bqu", 
    "bqv", "bqw", "bqx", "bqy", "bqz", "bre", "bra", "brb", 
    "brc", "brd", "brf", "brg", "brh", "bri", "brj", "brk", 
    "brl", "brm", "brn", "bro", "brp", "brq", "brr", "brs", 
    "brt", "bru", "brv", "brw", "brx", "bry", "brz", "bos", 
    "bsa", "bsb", "bsc", "bsd", "bse", "bsf", "bsg", "bsh", 
    "bsi", "bsj", "bsk", "bsl", "bsm", "bsn", "bso", "bsp", 
    "bsq", "bsr", "bss", "bst", "bsu", "bsv", "bsw", "bsx", 
    "bsy", "bsz", "bta", "btb", "btc", "btd", "bte", "btf", 
    "btg", "bth", "bti", "btj", "btk", "btl", "btm", "btn", 
    "bto", "btp", "btq", "btr", "bts", "btt", "btu", "btv", 
    "btw", "btx", "bty", "btz", "bua", "bub", "buc", "bud", 
    "bue", "buf", "bug", "buh", "bui", "buj", "buk", "bum", 
    "bun", "buo", "bup", "buq", "bus", "but", "buu", "buv", 
    "buw", "bux", "buy", "buz", "bva", "bvb", "bvc", "bvd", 
    "bve", "bvf", "bvg", "bvh", "bvi", "bvj", "bvk", "bvl", 
    "bvm", "bvn", "bvo", "bvp", "bvq", "bvr", "bvs", "bvt", 
    "bvu", "bvv", "bvw", "bvx", "bvy", "bvz", "bwa", "bwb", 
    "bwc", "bwd", "bwe", "bwf", "bwg", "bwh", "bwi", "bwj", 
    "bwk", "bwl", "bwm", "bwn", "bwo", "bwp", "bwq", "bwr", 
    "bws", "bwt", "bwu", "bwv", "bww", "bwx", "bwy", "bwz", 
    "bxa", "bxb", "bxc", "bxd", "bxe", "bxf", "bxg", "bxh", 
    "bxi", "bxj", "bxk", "bxl", "bxm", "bxn", "bxo", "bxp", 
    "bxq", "bxr", "bxs", "bxt", "bxu", "bxv", "bxw", "bxx", 
    "bxz", "bya", "byb", "byc", "byd", "bye", "byf", "byg", 
    "byh", "byi", "byj", "byk", "byl", "bym", "byn", "byo", 
    "byp", "byq", "byr", "bys", "byt", "byu", "byv", "byw", 
    "byx", "byy", "byz", "bza", "bzb", "bzc", "bzd", "bze", 
    "bzf", "bzg", "bzh", "bzi", "bzj", "bzk", "bzl", "bzm", 
    "bzn", "bzo", "bzp", "bzq", "bzr", "bzs", "bzt", "bzu", 
    "bzv", "bzw", "bzx", "bzy", "bzz", 
    "cat", "caa", "cab", "cac", "cad", "cae", "caf", "cag", 
    "cah", "cai", "caj", "cak", "cal", "cam", "can", "cao", 
    "cap", "caq", "car", "cas", "cau", "cav", "caw", "cax", 
    "cay", "caz", "cbb", "cbc", "cbd", "cbe", "cbg", "cbh", 
    "cbi", "cbj", "cbk", "cbl", "cbm", "cbn", "cbo", "cbr", 
    "cbs", "cbt", "cbu", "cbv", "cbw", "cby", "cca", "ccc", 
    "ccd", "cce", "ccg", "cch", "ccj", "ccl", "ccm", "cco", 
    "ccp", "ccq", "ccr", "ccx", "ccy", "cda", "cde", "cdf", 
    "cdg", "cdh", "cdi", "cdj", "cdm", "cdn", "cdo", "cdr", 
    "cds", "cdy", "cdz", "che", "cea", "ceb", "ceg", "cek", 
    "cel", "cen", "cet", "cfa", "cfd", "cfg", "cfm", "cga", 
    "cgc", "cgg", "cgk", "cha", "chb", "chc", "chd", "chf", 
    "chg", "chh", "chj", "chk", "chl", "chm", "chn", "cho", 
    "chp", "chq", "chr", "chs", "cht", "chw", "chx", "chy", 
    "chz", "cia", "cib", "cic", "cid", "cie", "cih", "cik", 
    "cim", "cin", "cip", "cir", "cit", "ciw", "ciy", "cja", 
    "cje", "cjh", "cji", "cjk", "cjm", "cjn", "cjo", "cjp", 
    "cjr", "cjs", "cjv", "cjy", "cka", "ckb", "ckc", "ckd", 
    "cke", "ckf", "ckh", "cki", "ckj", "ckk", "ckl", "ckn", 
    "cko", "ckq", "ckr", "cks", "ckt", "cku", "ckv", "ckw", 
    "ckx", "cky", "ckz", "cla", "clc", "cld", "cle", "clh", 
    "cli", "clj", "clk", "cll", "clm", "clo", "clt", "clu", 
    "clw", "cly", "cma", "cmc", "cme", "cmg", "cmi", "cmk", 
    "cml", "cmm", "cmn", "cmo", "cmr", "cms", "cmt", "cna", 
    "cnb", "cnc", "cng", "cnh", "cni", "cnk", "cnl", "cnm", 
    "cno", "cns", "cnt", "cnu", "cnw", "cnx", "cos", "coa", 
    "cob", "coc", "cod", "coe", "cof", "cog", "coh", "coj", 
    "cok", "col", "com", "con", "coo", "cop", "coq", "cot", 
    "cou", "cov", "cow", "cox", "coy", "coz", "cpa", "cpb", 
    "cpc", "cpe", "cpf", "cpg", "cpi", "cpn", "cpo", "cpp", 
    "cps", "cpu", "cpx", "cpy", "cqd", "cqu", "cre", "cra", 
    "crb", "crc", "crd", "crf", "crg", "crh", "cri", "crj", 
    "crk", "crl", "crm", "crn", "cro", "crp", "crq", "crr", 
    "crs", "crt", "cru", "crv", "crw", "crx", "cry", "crz", 
    "ces", "csa", "csb", "csc", "csd", "cse", "csf", "csg", 
    "csh", "csi", "csj", "csk", "csl", "csm", "csn", "cso", 
    "csq", "csr", "css", "cst", "csv", "csw", "csy", "csz", 
    "cta", "ctc", "ctd", "cte", "ctg", "cth", "cti", "ctl", 
    "ctm", "ctn", "cto", "ctp", "cts", "ctt", "ctu", "ctz", 
    "chu", "cua", "cub", "cuc", "cug", "cuh", "cui", "cuj", 
    "cuk", "cul", "cum", "cun", "cuo", "cup", "cuq", "cur", 
    "cus", "cut", "cuu", "cuv", "cuw", "cux", "chv", "cvg", 
    "cvn", "cwa", "cwb", "cwd", "cwe", "cwg", "cwt", "cym", 
    "cya", "cyb", "cyo", "czh", "czk", "czn", "czo", "czt", 
    "dan", "daa", "dac", "dad", "dae", "daf", "dag", "dah", 
    "dai", "daj", "dak", "dal", "dam", "dao", "dap", "daq", 
    "dar", "das", "dat", "dau", "dav", "daw", "dax", "day", 
    "daz", "dba", "dbb", "dbd", "dbe", "dbf", "dbg", "dbi", 
    "dbj", "dbl", "dbm", "dbn", "dbo", "dbp", "dbq", "dbr", 
    "dbt", "dbu", "dbv", "dbw", "dby", "dcc", "dcr", "dda", 
    "ddd", "dde", "ddg", "ddi", "ddj", "ddn", "ddo", "ddr", 
    "dds", "ddw", "deu", "dec", "ded", "dee", "def", "deg", 
    "deh", "dei", "dek", "del", "dem", "den", "dep", "deq", 
    "der", "des", "dev", "dez", "dga", "dgb", "dgc", "dgd", 
    "dge", "dgg", "dgh", "dgi", "dgk", "dgl", "dgn", "dgo", 
    "dgr", "dgs", "dgt", "dgu", "dgw", "dgx", "dgz", "dha", 
    "dhd", "dhg", "dhi", "dhl", "dhm", "dhn", "dho", "dhr", 
    "dhs", "dhu", "dhv", "dhw", "dhx", "dia", "dib", "dic", 
    "did", "dif", "dig", "dih", "dii", "dij", "dik", "dil", 
    "dim", "din", "dio", "dip", "diq", "dir", "dis", "dit", 
    "diu", "diw", "dix", "diy", "diz", "dja", "djb", "djc", 
    "djd", "dje", "djf", "dji", "djj", "djk", "djl", "djm", 
    "djn", "djo", "djr", "dju", "djw", "dka", "dkk", "dkl", 
    "dkr", "dks", "dkx", "dlg", "dlk", "dlm", "dln", "dma", 
    "dmb", "dmc", "dmd", "dme", "dmg", "dmk", "dml", "dmm", 
    "dmo", "dmr", "dms", "dmu", "dmv", "dmw", "dmx", "dmy", 
    "dna", "dnd", "dne", "dng", "dni", "dnj", "dnk", "dnn", 
    "dnr", "dnt", "dnu", "dnv", "dnw", "dny", "doa", "dob", 
    "doc", "doe", "dof", "doh", "doi", "dok", "dol", "don", 
    "doo", "dop", "doq", "dor", "dos", "dot", "dov", "dow", 
    "dox", "doy", "doz", "dpp", "dra", "drb", "drc", "drd", 
    "dre", "drg", "drh", "dri", "drl", "drn", "dro", "drq", 
    "drr", "drs", "drt", "dru", "drw", "dry", "dsb", "dse", 
    "dsh", "dsi", "dsl", "dsn", "dso", "dsq", "dta", "dtb", 
    "dtd", "dth", "dti", "dtk", "dtm", "dto", "dtp", "dtr", 
    "dts", "dtt", "dtu", "dty", "dua", "dub", "duc", "dud", 
    "due", "duf", "dug", "duh", "dui", "duj", "duk", "dul", 
    "dum", "dun", "duo", "dup", "duq", "dur", "dus", "duu", 
    "duv", "duw", "dux", "duy", "duz", "div", "dva", "dwa", 
    "dwl", "dwr", "dws", "dww", "dya", "dyb", "dyd", "dyg", 
    "dyi", "dyk", "dym", "dyn", "dyo", "dyu", "dyy", "dzo", 
    "dza", "dzd", "dze", "dzg", "dzl", "dzn", 
    "eaa", "ebg", "ebk", "ebo", "ebr", "ebu", "ecr", "ecs", 
    "ecy", "ewe", "eee", "efa", "efe", "efi", "ega", "egl", 
    "ego", "egy", "ehu", "eip", "eit", "eiv", "eja", "eka", 
    "ekc", "eke", "ekg", "eki", "ekk", "ekl", "ekm", "eko", 
    "ekp", "ekr", "eky", "ell", "ele", "elh", "eli", "elk", 
    "elm", "elo", "elp", "elu", "elx", "ema", "emb", "eme", 
    "emg", "emi", "emk", "eml", "emm", "emn", "emo", "emp", 
    "ems", "emu", "emw", "emx", "emy", "eng", "ena", "enb", 
    "enc", "end", "enf", "enh", "eni", "enm", "enn", "eno", 
    "enq", "enr", "enu", "env", "enw", "epo", "eot", "epi", 
    "era", "erg", "erh", "eri", "erk", "ero", "err", "ers", 
    "ert", "erw", "spa", "ese", "esh", "esi", "esk", "esl", 
    "esm", "esn", "eso", "esq", "ess", "esu", "est", "etb", 
    "etc", "eth", "etn", "eto", "etr", "ets", "ett", "etu", 
    "etx", "etz", "eus", "eur", "eve", "evh", "evn", "ewo", 
    "ext", "eya", "eyo", "eza", "eze", 
    "fas", "faa", "fab", "fad", "faf", "fag", "fah", "fai", 
    "faj", "fak", "fal", "fam", "fan", "fap", "far", "fat", 
    "fau", "fax", "fay", "faz", "fbl", "fcs", "fer", "ful", 
    "ffi", "ffm", "fgr", "fin", "fia", "fie", "fil", "fip", 
    "fir", "fit", "fiu", "fiw", "fiz", "fij", "fkk", "fkv", 
    "fla", "flh", "fli", "fll", "flm", "fln", "flr", "fly", 
    "fmp", "fmu", "fng", "fni", "fao", "fod", "foi", "fom", 
    "fon", "for", "fos", "fpe", "fqs", "fra", "frc", "frd", 
    "fri", "frk", "frm", "fro", "frp", "frq", "frr", "frs", 
    "frt", "fse", "fsl", "fss", "fub", "fuc", "fud", "fue", 
    "fuf", "fuh", "fui", "fuj", "fum", "fun", "fuq", "fur", 
    "fut", "fuu", "fuv", "fuy", "fvr", "fwa", "fwe", "fry", 
    "gle", "gaa", "gab", "gac", "gad", "gae", "gaf", "gag", 
    "gah", "gai", "gaj", "gak", "gal", "gam", "gan", "gao", 
    "gap", "gaq", "gar", "gas", "gat", "gau", "gav", "gaw", 
    "gax", "gay", "gaz", "gba", "gbb", "gbc", "gbd", "gbe", 
    "gbf", "gbg", "gbh", "gbi", "gbj", "gbk", "gbl", "gbm", 
    "gbn", "gbo", "gbp", "gbq", "gbr", "gbs", "gbu", "gbv", 
    "gbw", "gbx", "gby", "gbz", "gcc", "gcd", "gce", "gcf", 
    "gcl", "gcn", "gcr", "gct", "gla", "gda", "gdb", "gdc", 
    "gdd", "gde", "gdf", "gdg", "gdh", "gdi", "gdj", "gdk", 
    "gdl", "gdm", "gdn", "gdo", "gdq", "gdr", "gds", "gdt", 
    "gdu", "gdx", "gea", "geb", "gec", "ged", "geg", "geh", 
    "gei", "gej", "gek", "gel", "gem", "gen", "geq", "ges", 
    "gew", "gex", "gey", "gez", "gfk", "gft", "gfx", "gga", 
    "ggb", "ggd", "gge", "ggg", "ggh", "ggk", "ggl", "ggm", 
    "ggn", "ggo", "ggr", "ggt", "ggu", "ggw", "gha", "ghc", 
    "ghe", "ghh", "ghk", "ghl", "ghn", "gho", "ghr", "ghs", 
    "ght", "gia", "gib", "gic", "gid", "gig", "gih", "gil", 
    "gim", "gin", "gio", "gip", "giq", "gir", "gis", "git", 
    "giu", "giw", "gix", "giy", "giz", "gji", "gjk", "gjm", 
    "gjn", "gju", "gka", "gke", "gkn", "gko", "gkp", "glg", 
    "glc", "gld", "glh", "gli", "glj", "glk", "gll", "glo", 
    "glr", "glu", "glw", "gly", "gma", "gmb", "gmd", "gmh", 
    "gml", "gmm", "gmn", "gmo", "gmu", "gmv", "gmx", "gmy", 
    "gmz", "grn", "gna", "gnb", "gnc", "gnd", "gne", "gng", 
    "gnh", "gni", "gnk", "gnl", "gnm", "gnn", "gno", "gnq", 
    "gnr", "gnt", "gnu", "gnw", "gnz", "goa", "gob", "goc", 
    "god", "goe", "gof", "gog", "goh", "goi", "goj", "gok", 
    "gol", "gom", "gon", "goo", "gop", "goq", "gor", "gos", 
    "got", "gou", "gow", "gox", "goy", "goz", "gpa", "gpe", 
    "gpn", "gqa", "gqi", "gqn", "gqr", "gqu", "gra", "grb", 
    "grc", "grd", "grg", "grh", "gri", "grj", "grm", "gro", 
    "grq", "grr", "grs", "grt", "gru", "grv", "grw", "grx", 
    "gry", "grz", "gsc", "gse", "gsg", "gsl", "gsm", "gsn", 
    "gso", "gsp", "gss", "gsw", "gta", "gti", "gtu", "guj", 
    "gua", "gub", "guc", "gud", "gue", "guf", "gug", "guh", 
    "gui", "guk", "gul", "gum", "gun", "guo", "gup", "guq", 
    "gur", "gus", "gut", "guu", "guv", "guw", "gux", "guz", 
    "glv", "gva", "gvc", "gve", "gvf", "gvj", "gvl", "gvm", 
    "gvn", "gvo", "gvp", "gvr", "gvs", "gvy", "gwa", "gwb", 
    "gwc", "gwd", "gwe", "gwf", "gwg", "gwi", "gwj", "gwm", 
    "gwn", "gwr", "gwt", "gwu", "gww", "gwx", "gxx", "gya", 
    "gyb", "gyd", "gye", "gyf", "gyg", "gyi", "gyl", "gym", 
    "gyn", "gyr", "gyy", "gza", "gzi", "gzn", 
    "hau", "haa", "hab", "hac", "had", "hae", "haf", "hag", 
    "hah", "hai", "haj", "hak", "hal", "ham", "han", "hao", 
    "hap", "haq", "har", "has", "hav", "haw", "hax", "hay", 
    "haz", "hba", "hbb", "hbn", "hbo", "hbu", "hca", "hch", 
    "hdn", "hds", "hdy", "heb", "hea", "hed", "heg", "heh", 
    "hei", "hem", "hgm", "hgw", "hhi", "hhr", "hhy", "hin", 
    "hia", "hib", "hid", "hif", "hig", "hih", "hii", "hij", 
    "hik", "hil", "him", "hio", "hir", "hit", "hiw", "hix", 
    "hji", "hka", "hke", "hkk", "hks", "hla", "hlb", "hld", 
    "hle", "hlt", "hlu", "hma", "hmb", "hmc", "hmd", "hme", 
    "hmf", "hmg", "hmh", "hmi", "hmj", "hmk", "hml", "hmm", 
    "hmn", "hmp", "hmq", "hmr", "hms", "hmt", "hmu", "hmv", 
    "hmw", "hmy", "hmz", "hna", "hnd", "hne", "hnh", "hni", 
    "hnj", "hnn", "hno", "hns", "hnu", "hmo", "hoa", "hob", 
    "hoc", "hod", "hoe", "hoh", "hoi", "hoj", "hol", "hom", 
    "hoo", "hop", "hor", "hos", "hot", "hov", "how", "hoy", 
    "hoz", "hpo", "hps", "hrv", "hra", "hrc", "hre", "hrk", 
    "hrm", "hro", "hrp", "hrr", "hrt", "hru", "hrw", "hrx", 
    "hrz", "hsb", "hsf", "hsh", "hsl", "hsn", "hss", "hat", 
    "hti", "hto", "hts", "htu", "htx", "hun", "hub", "huc", 
    "hud", "hue", "huf", "hug", "huh", "hui", "huj", "huk", 
    "hul", "hum", "huo", "hup", "huq", "hur", "hus", "hut", 
    "huu", "huv", "huw", "hux", "huy", "huz", "hva", "hvc", 
    "hve", "hvk", "hvn", "hvv", "hwa", "hwc", "hwo", "hye", 
    "hya", "her", 
    "ina", "iai", "ian", "iap", "iar", "iba", "ibb", "ibd", 
    "ibe", "ibg", "ibi", "ibl", "ibm", "ibn", "ibr", "ibu", 
    "iby", "ica", "ich", "icl", "icr", "ind", "ida", "idb", 
    "idc", "idd", "ide", "idi", "idr", "ids", "idt", "idu", 
    "ile", "ifa", "ifb", "ife", "iff", "ifk", "ifm", "ifu", 
    "ify", "ibo", "igb", "ige", "igg", "igl", "igm", "ign", 
    "igo", "igs", "igw", "ihb", "ihi", "ihp", "ihw", "iii", 
    "iin", "ijc", "ije", "ijj", "ijn", "ijo", "ijs", "ipk", 
    "ike", "iki", "ikk", "ikl", "iko", "ikp", "ikr", "ikt", 
    "ikv", "ikw", "ikx", "ikz", "ila", "ilb", "ilg", "ili", 
    "ilk", "ill", "ilo", "ils", "ilu", "ilv", "ilw", "ima", 
    "ime", "imi", "iml", "imn", "imo", "imr", "ims", "imy", 
    "inb", "inc", "ine", "ing", "inh", "inj", "inl", "inm", 
    "inn", "ino", "inp", "ins", "int", "inz", "ido", "ior", 
    "iou", "iow", "ipi", "ipo", "iqu", "iqw", "ira", "ire", 
    "irh", "iri", "irk", "irn", "iro", "irr", "iru", "irx", 
    "iry", "isl", "isa", "isc", "isd", "ise", "isg", "ish", 
    "isi", "isk", "ism", "isn", "iso", "isr", "ist", "isu", 
    "ita", "itb", "ite", "iti", "itk", "itl", "itm", "ito", 
    "itr", "its", "itt", "itu", "itv", "itw", "itx", "ity", 
    "itz", "iku", "ium", "ivb", "ivv", "iwk", "iwm", "iwo", 
    "iws", "ixc", "ixi", "ixj", "ixl", "iya", "iyo", "iyx", 
    "izh", "izi", "izr", "izz", 
    "jpn", "jaa", "jab", "jac", "jad", "jae", "jaf", "jah", 
    "jai", "jaj", "jak", "jal", "jam", "jan", "jao", "jap", 
    "jaq", "jas", "jat", "jau", "jax", "jay", "jaz", "jbe", 
    "jbi", "jbj", "jbk", "jbn", "jbo", "jbr", "jbt", "jbu", 
    "jbw", "jcs", "jct", "jda", "jdg", "jdt", "jeb", "jee", 
    "jeg", "jeh", "jei", "jek", "jel", "jen", "jer", "jet", 
    "jeu", "jgb", "jge", "jgk", "jgo", "jhi", "jhs", "jia", 
    "jib", "jic", "jid", "jie", "jig", "jih", "jii", "jil", 
    "jim", "jio", "jiq", "jit", "jiu", "jiv", "jiy", "jjr", 
    "jkm", "jko", "jkp", "jkr", "jku", "jle", "jls", "jma", 
    "jmb", "jmc", "jmd", "jmi", "jml", "jmn", "jmr", "jms", 
    "jmw", "jmx", "jna", "jnd", "jng", "jni", "jnj", "jnl", 
    "jns", "job", "jod", "jor", "jos", "jow", "jpa", "jpr", 
    "jqr", "jra", "jrb", "jrr", "jrt", "jru", "jsl", "jua", 
    "jub", "juc", "jud", "juh", "jui", "juk", "jul", "jum", 
    "jun", "juo", "jup", "jur", "jus", "jut", "juu", "juw", 
    "juy", "jav", "jvd", "jvn", "jwi", "jya", "jye", "jyy", 
    "kat", "kaa", "kab", "kac", "kad", "kae", "kaf", "kag", 
    "kah", "kai", "kaj", "kak", "kam", "kao", "kap", "kaq", 
    "kar", "kav", "kaw", "kax", "kay", "kba", "kbb", "kbc", 
    "kbd", "kbe", "kbf", "kbg", "kbh", "kbi", "kbj", "kbk", 
    "kbl", "kbm", "kbn", "kbo", "kbp", "kbq", "kbr", "kbs", 
    "kbt", "kbu", "kbv", "kbw", "kbx", "kby", "kbz", "kca", 
    "kcb", "kcc", "kcd", "kce", "kcf", "kcg", "kch", "kci", 
    "kcj", "kck", "kcl", "kcm", "kcn", "kco", "kcp", "kcq", 
    "kcr", "kcs", "kct", "kcu", "kcv", "kcw", "kcx", "kcy", 
    "kcz", "kda", "kdc", "kdd", "kde", "kdf", "kdg", "kdh", 
    "kdi", "kdj", "kdk", "kdl", "kdm", "kdn", "kdp", "kdq", 
    "kdr", "kds", "kdt", "kdu", "kdv", "kdw", "kdx", "kdy", 
    "kdz", "kea", "keb", "kec", "ked", "kee", "kef", "keg", 
    "keh", "kei", "kej", "kek", "kel", "kem", "ken", "keo", 
    "kep", "keq", "ker", "kes", "ket", "keu", "kev", "kew", 
    "kex", "key", "kez", "kfa", "kfb", "kfc", "kfd", "kfe", 
    "kff", "kfg", "kfh", "kfi", "kfj", "kfk", "kfl", "kfm", 
    "kfn", "kfo", "kfp", "kfq", "kfr", "kfs", "kft", "kfu", 
    "kfv", "kfw", "kfx", "kfy", "kfz", "kon", "kga", "kgb", 
    "kgc", "kgd", "kge", "kgf", "kgg", "kgh", "kgi", "kgj", 
    "kgk", "kgl", "kgm", "kgn", "kgo", "kgp", "kgq", "kgr", 
    "kgs", "kgt", "kgu", "kgv", "kgw", "kgx", "kgy", "kha", 
    "khb", "khc", "khd", "khe", "khf", "khg", "khh", "khi", 
    "khj", "khk", "khl", "khn", "kho", "khp", "khq", "khr", 
    "khs", "kht", "khu", "khv", "khw", "khx", "khy", "khz", 
    "kik", "kia", "kib", "kic", "kid", "kie", "kif", "kig", 
    "kih", "kii", "kij", "kil", "kim", "kio", "kip", "kiq", 
    "kis", "kit", "kiu", "kiv", "kiw", "kix", "kiy", "kiz", 
    "kua", "kja", "kjb", "kjc", "kjd", "kje", "kjf", "kjg", 
    "kjh", "kji", "kjj", "kjk", "kjl", "kjm", "kjn", "kjo", 
    "kjp", "kjq", "kjr", "kjs", "kjt", "kju", "kjx", "kjy", 
    "kjz", "kaz", "kka", "kkb", "kkc", "kkd", "kke", "kkf", 
    "kkg", "kkh", "kki", "kkj", "kkk", "kkl", "kkm", "kkn", 
    "kko", "kkp", "kkq", "kkr", "kks", "kkt", "kku", "kkv", 
    "kkw", "kkx", "kky", "kkz", "kal", "kla", "klb", "klc", 
    "kld", "kle", "klf", "klg", "klh", "kli", "klj", "klk", 
    "kll", "klm", "kln", "klo", "klp", "klq", "klr", "kls", 
    "klt", "klu", "klv", "klw", "klx", "kly", "klz", "khm", 
    "kma", "kmb", "kmc", "kmd", "kme", "kmf", "kmg", "kmh", 
    "kmi", "kmj", "kmk", "kml", "kmm", "kmn", "kmo", "kmp", 
    "kmq", "kmr", "kms", "kmt", "kmu", "kmv", "kmw", "kmx", 
    "kmy", "kmz", "kan", "kna", "knb", "knc", "knd", "kne", 
    "knf", "kng", "knh", "kni", "knj", "knk", "knl", "knm", 
    "knn", "kno", "knp", "knq", "knr", "kns", "knt", "knu", 
    "knv", "knw", "knx", "kny", "knz", "kor", "koa", "kob", 
    "koc", "kod", "koe", "kof", "kog", "koh", "koi", "koj", 
    "kok", "kol", "koo", "kop", "koq", "kos", "kot", "kou", 
    "kov", "kow", "kox", "koy", "koz", "kpa", "kpb", "kpc", 
    "kpd", "kpe", "kpf", "kpg", "kph", "kpi", "kpj", "kpk", 
    "kpl", "kpm", "kpn", "kpo", "kpp", "kpq", "kpr", "kps", 
    "kpt", "kpu", "kpv", "kpw", "kpx", "kpy", "kpz", "kqa", 
    "kqb", "kqc", "kqd", "kqe", "kqf", "kqg", "kqh", "kqi", 
    "kqj", "kqk", "kql", "kqm", "kqn", "kqo", "kqp", "kqq", 
    "kqr", "kqs", "kqt", "kqu", "kqv", "kqw", "kqx", "kqy", 
    "kqz", "kau", "kra", "krb", "krc", "krd", "kre", "krf", 
    "krg", "krh", "kri", "krj", "krk", "krl", "krm", "krn", 
    "kro", "krp", "krq", "krr", "krs", "krt", "kru", "krv", 
    "krw", "krx", "kry", "krz", "kas", "ksa", "ksb", "ksc", 
    "ksd", "kse", "ksf", "ksg", "ksh", "ksi", "ksj", "ksk", 
    "ksl", "ksm", "ksn", "kso", "ksp", "ksq", "ksr", "kss", 
    "kst", "ksu", "ksv", "ksw", "ksx", "ksy", "ksz", "kta", 
    "ktb", "ktc", "ktd", "kte", "ktf", "ktg", "kth", "kti", 
    "ktj", "ktk", "ktl", "ktm", "ktn", "kto", "ktp", "ktq", 
    "ktr", "kts", "ktt", "ktu", "ktv", "ktw", "ktx", "kty", 
    "ktz", "kur", "kub", "kuc", "kud", "kue", "kuf", "kug", 
    "kuh", "kui", "kuj", "kuk", "kul", "kum", "kun", "kuo", 
    "kup", "kuq", "kus", "kut", "kuu", "kuv", "kuw", "kux", 
    "kuy", "kuz", "kom", "kva", "kvb", "kvc", "kvd", "kve", 
    "kvf", "kvg", "kvh", "kvi", "kvj", "kvk", "kvl", "kvm", 
    "kvn", "kvo", "kvp", "kvq", "kvr", "kvs", "kvt", "kvu", 
    "kvv", "kvw", "kvx", "kvy", "kvz", "cor", "kwa", "kwb", 
    "kwc", "kwd", "kwe", "kwf", "kwg", "kwh", "kwi", "kwj", 
    "kwk", "kwl", "kwm", "kwn", "kwo", "kwp", "kwq", "kwr", 
    "kws", "kwt", "kwu", "kwv", "kww", "kwx", "kwy", "kwz", 
    "kxa", "kxb", "kxc", "kxd", "kxe", "kxf", "kxg", "kxh", 
    "kxi", "kxj", "kxk", "kxl", "kxm", "kxn", "kxo", "kxp", 
    "kxq", "kxr", "kxs", "kxt", "kxu", "kxv", "kxw", "kxx", 
    "kxy", "kxz", "kir", "kya", "kyb", "kyc", "kyd", "kye", 
    "kyf", "kyg", "kyh", "kyi", "kyj", "kyk", "kyl", "kym", 
    "kyn", "kyo", "kyp", "kyq", "kyr", "kys", "kyt", "kyu", 
    "kyv", "kyw", "kyx", "kyy", "kyz", "kza", "kzb", "kzc", 
    "kzd", "kze", "kzf", "kzg", "kzh", "kzi", "kzj", "kzk", 
    "kzl", "kzm", "kzn", "kzo", "kzp", "kzq", "kzr", "kzs", 
    "kzt", "kzu", "kzv", "kzw", "kzx", "kzy", "kzz", 
    "lat", "laa", "lab", "lac", "lad", "lae", "laf", "lag", 
    "lah", "lai", "laj", "lak", "lal", "lam", "lan", "lap", 
    "laq", "lar", "las", "lau", "law", "lax", "lay", "laz", 
    "ltz", "lba", "lbb", "lbc", "lbe", "lbf", "lbg", "lbi", 
    "lbj", "lbk", "lbl", "lbm", "lbn", "lbo", "lbq", "lbr", 
    "lbs", "lbt", "lbu", "lbv", "lbw", "lbx", "lby", "lbz", 
    "lcc", "lcd", "lce", "lcf", "lch", "lcl", "lcm", "lcp", 
    "lcq", "lcs", "lda", "ldb", "ldd", "ldg", "ldh", "ldi", 
    "ldj", "ldk", "ldl", "ldm", "ldn", "ldo", "ldp", "ldq", 
    "lea", "leb", "lec", "led", "lee", "lef", "leg", "leh", 
    "lei", "lej", "lek", "lel", "lem", "len", "leo", "lep", 
    "leq", "ler", "les", "let", "leu", "lev", "lew", "lex", 
    "ley", "lez", "lfa", "lfn", "lug", "lga", "lgb", "lgg", 
    "lgh", "lgi", "lgk", "lgl", "lgm", "lgn", "lgq", "lgr", 
    "lgt", "lgu", "lgz", "lha", "lhh", "lhi", "lhl", "lhm", 
    "lhn", "lhp", "lhs", "lht", "lhu", "lim", "lia", "lib", 
    "lic", "lid", "lie", "lif", "lig", "lih", "lii", "lij", 
    "lik", "lil", "lio", "lip", "liq", "lir", "lis", "liu", 
    "liv", "liw", "lix", "liy", "liz", "lja", "lje", "lji", 
    "ljl", "ljp", "ljw", "ljx", "lka", "lkb", "lkc", "lkd", 
    "lke", "lkh", "lki", "lkj", "lkl", "lkm", "lkn", "lko", 
    "lkr", "lks", "lkt", "lku", "lky", "lla", "llb", "llc", 
    "lld", "lle", "llf", "llg", "llh", "lli", "llj", "llk", 
    "lll", "llm", "lln", "llo", "llp", "llq", "lls", "llu", 
    "llx", "lma", "lmb", "lmc", "lmd", "lme", "lmf", "lmg", 
    "lmh", "lmi", "lmj", "lmk", "lml", "lmm", "lmn", "lmo", 
    "lmp", "lmq", "lmr", "lms", "lmt", "lmu", "lmv", "lmw", 
    "lmx", "lmy", "lmz", "lin", "lna", "lnb", "lnc", "lnd", 
    "lng", "lnh", "lni", "lnj", "lnl", "lnm", "lnn", "lno", 
    "lns", "lnt", "lnu", "lnw", "lnz", "lao", "loa", "lob", 
    "loc", "lod", "loe", "lof", "log", "loh", "loi", "loj", 
    "lok", "lol", "lom", "lon", "loo", "lop", "loq", "lor", 
    "los", "lot", "lou", "lov", "low", "lox", "loy", "loz", 
    "lpa", "lpe", "lpn", "lpo", "lpx", "lra", "lrc", "lre", 
    "lrg", "lri", "lrk", "lrl", "lrm", "lrn", "lro", "lrr", 
    "lrt", "lrv", "lrz", "lsa", "lsd", "lse", "lsg", "lsh", 
    "lsi", "lsl", "lsm", "lso", "lsp", "lsr", "lss", "lst", 
    "lsy", "lit", "ltc", "ltg", "lti", "ltn", "lto", "lts", 
    "ltu", "lub", "lua", "luc", "lud", "lue", "luf", "lui", 
    "luj", "luk", "lul", "lum", "lun", "luo", "lup", "luq", 
    "lur", "lus", "lut", "luu", "luv", "luw", "luy", "luz", 
    "lav", "lva", "lvk", "lvs", "lvu", "lwa", "lwe", "lwg", 
    "lwh", "lwl", "lwm", "lwo", "lwt", "lwu", "lww", "lya", 
    "lyg", "lyn", "lzh", "lzl", "lzn", "lzz", 
    "maa", "mab", "mad", "mae", "maf", "mag", "mai", "maj", 
    "mak", "mam", "man", "map", "maq", "mas", "mat", "mau", 
    "mav", "maw", "max", "maz", "mba", "mbb", "mbc", "mbd", 
    "mbe", "mbf", "mbg", "mbh", "mbi", "mbj", "mbk", "mbl", 
    "mbm", "mbn", "mbo", "mbp", "mbq", "mbr", "mbs", "mbt", 
    "mbu", "mbv", "mbw", "mbx", "mby", "mbz", "mca", "mcb", 
    "mcc", "mcd", "mce", "mcf", "mcg", "mch", "mci", "mcj", 
    "mck", "mcl", "mcm", "mcn", "mco", "mcp", "mcq", "mcr", 
    "mcs", "mct", "mcu", "mcv", "mcw", "mcx", "mcy", "mcz", 
    "mda", "mdb", "mdc", "mdd", "mde", "mdf", "mdg", "mdh", 
    "mdi", "mdj", "mdk", "mdl", "mdm", "mdn", "mdo", "mdp", 
    "mdq", "mdr", "mds", "mdt", "mdu", "mdv", "mdw", "mdx", 
    "mdy", "mdz", "mea", "meb", "mec", "med", "mee", "mef", 
    "meg", "meh", "mei", "mej", "mek", "mel", "mem", "men", 
    "meo", "mep", "meq", "mer", "mes", "met", "meu", "mev", 
    "mew", "mey", "mez", "mfa", "mfb", "mfc", "mfd", "mfe", 
    "mff", "mfg", "mfh", "mfi", "mfj", "mfk", "mfl", "mfm", 
    "mfn", "mfo", "mfp", "mfq", "mfr", "mfs", "mft", "mfu", 
    "mfv", "mfw", "mfx", "mfy", "mfz", "mlg", "mga", "mgb", 
    "mgc", "mgd", "mge", "mgf", "mgg", "mgh", "mgi", "mgj", 
    "mgk", "mgl", "mgm", "mgn", "mgo", "mgp", "mgq", "mgr", 
    "mgs", "mgt", "mgu", "mgv", "mgw", "mgx", "mgy", "mgz", 
    "mah", "mha", "mhb", "mhc", "mhd", "mhe", "mhf", "mhg", 
    "mhh", "mhi", "mhj", "mhk", "mhl", "mhm", "mhn", "mho", 
    "mhp", "mhq", "mhr", "mhs", "mht", "mhu", "mhv", "mhw", 
    "mhx", "mhy", "mhz", "mri", "mia", "mib", "mic", "mid", 
    "mie", "mif", "mig", "mih", "mii", "mij", "mik", "mil", 
    "mim", "min", "mio", "mip", "miq", "mir", "mis", "mit", 
    "miu", "miv", "miw", "mix", "miy", "miz", "mja", "mjc", 
    "mjd", "mje", "mjg", "mjh", "mji", "mjj", "mjk", "mjl", 
    "mjm", "mjn", "mjo", "mjp", "mjq", "mjr", "mjs", "mjt", 
    "mju", "mjv", "mjw", "mjx", "mjy", "mjz", "mkd", "mka", 
    "mkb", "mkc", "mke", "mkf", "mkg", "mkh", "mki", "mkj", 
    "mkk", "mkl", "mkm", "mkn", "mko", "mkp", "mkq", "mkr", 
    "mks", "mkt", "mku", "mkv", "mkw", "mkx", "mky", "mkz", 
    "mal", "mla", "mlb", "mlc", "mld", "mle", "mlf", "mlh", 
    "mli", "mlj", "mlk", "mll", "mlm", "mln", "mlo", "mlp", 
    "mlq", "mlr", "mls", "mlu", "mlv", "mlw", "mlx", "mly", 
    "mlz", "mma", "mmb", "mmc", "mmd", "mme", "mmf", "mmg", 
    "mmh", "mmi", "mmj", "mmk", "mml", "mmm", "mmn", "mmo", 
    "mmp", "mmq", "mmr", "mms", "mmt", "mmu", "mmv", "mmw", 
    "mmx", "mmy", "mmz", "mon", "mna", "mnb", "mnc", "mnd", 
    "mne", "mnf", "mng", "mnh", "mni", "mnj", "mnk", "mnl", 
    "mnm", "mnn", "mno", "mnp", "mnq", "mnr", "mns", "mnt", 
    "mnu", "mnv", "mnw", "mnx", "mny", "mnz", "mol", "moa", 
    "mob", "moc", "mod", "moe", "mof", "mog", "moh", "moi", 
    "moj", "mok", "mol", "mom", "moo", "mop", "moq", "mor", 
    "mos", "mot", "mou", "mov", "mow", "mox", "moy", "moz", 
    "mpa", "mpb", "mpc", "mpd", "mpe", "mpf", "mpg", "mph", 
    "mpi", "mpj", "mpk", "mpl", "mpm", "mpn", "mpo", "mpp", 
    "mpq", "mpr", "mps", "mpt", "mpu", "mpv", "mpw", "mpx", 
    "mpy", "mpz", "mqa", "mqb", "mqc", "mqd", "mqe", "mqf", 
    "mqg", "mqh", "mqi", "mqj", "mqk", "mql", "mqm", "mqn", 
    "mqo", "mqp", "mqq", "mqr", "mqs", "mqt", "mqu", "mqv", 
    "mqw", "mqx", "mqy", "mqz", "mar", "mra", "mrb", "mrc", 
    "mrd", "mre", "mrf", "mrg", "mrh", "mrj", "mrk", "mrl", 
    "mrm", "mrn", "mro", "mrp", "mrq", "mrr", "mrs", "mrt", 
    "mru", "mrv", "mrw", "mrx", "mry", "mrz", "msa", "msb", 
    "msc", "msd", "mse", "msf", "msg", "msh", "msi", "msj", 
    "msk", "msl", "msm", "msn", "mso", "msp", "msq", "msr", 
    "mss", "mst", "msu", "msv", "msw", "msx", "msy", "msz", 
    "mlt", "mta", "mtb", "mtc", "mtd", "mte", "mtf", "mtg", 
    "mth", "mti", "mtj", "mtk", "mtl", "mtm", "mtn", "mto", 
    "mtp", "mtq", "mtr", "mts", "mtt", "mtu", "mtv", "mtw", 
    "mtx", "mty", "mtz", "mua", "mub", "muc", "mud", "mue", 
    "mug", "muh", "mui", "muj", "muk", "mul", "mum", "mun", 
    "muo", "mup", "muq", "mur", "mus", "mut", "muu", "muv", 
    "muw", "mux", "muy", "muz", "mva", "mvb", "mvc", "mvd", 
    "mve", "mvf", "mvg", "mvh", "mvi", "mvj", "mvk", "mvl", 
    "mvm", "mvn", "mvo", "mvp", "mvq", "mvr", "mvs", "mvt", 
    "mvu", "mvv", "mvw", "mvx", "mvy", "mvz", "mwa", "mwb", 
    "mwc", "mwd", "mwe", "mwf", "mwg", "mwh", "mwi", "mwj", 
    "mwk", "mwl", "mwm", "mwn", "mwo", "mwp", "mwq", "mwr", 
    "mws", "mwt", "mwu", "mwv", "mww", "mwx", "mwy", "mwz", 
    "mxa", "mxb", "mxc", "mxd", "mxe", "mxf", "mxg", "mxh", 
    "mxi", "mxj", "mxk", "mxl", "mxm", "mxn", "mxo", "mxp", 
    "mxq", "mxr", "mxs", "mxt", "mxu", "mxv", "mxw", "mxx", 
    "mxy", "mxz", "mya", "myb", "myc", "myd", "mye", "myf", 
    "myg", "myh", "myi", "myj", "myk", "myl", "mym", "myn", 
    "myo", "myp", "myq", "myr", "mys", "myt", "myu", "myv", 
    "myw", "myx", "myy", "myz", "mza", "mzb", "mzc", "mzd", 
    "mze", "mzf", "mzg", "mzh", "mzi", "mzj", "mzk", "mzl", 
    "mzm", "mzn", "mzo", "mzp", "mzq", "mzr", "mzs", "mzt", 
    "mzu", "mzv", "mzw", "mzx", "mzy", "mzz", 
    "nau", "naa", "nab", "nac", "nad", "nae", "naf", "nag", 
    "nah", "nai", "naj", "nak", "nal", "nam", "nan", "nao", 
    "nap", "naq", "nar", "nas", "nat", "naw", "nax", "nay", 
    "naz", "nob", "nba", "nbb", "nbc", "nbd", "nbe", "nbf", 
    "nbg", "nbh", "nbi", "nbj", "nbk", "nbm", "nbn", "nbo", 
    "nbp", "nbq", "nbr", "nbs", "nbt", "nbu", "nbv", "nbw", 
    "nbx", "nby", "nca", "ncb", "ncc", "ncd", "nce", "ncf", 
    "ncg", "nch", "nci", "ncj", "nck", "ncl", "ncm", "ncn", 
    "nco", "ncp", "ncr", "ncs", "nct", "ncu", "ncx", "ncz", 
    "nde", "nda", "ndb", "ndc", "ndd", "ndf", "ndg", "ndh", 
    "ndi", "ndj", "ndk", "ndl", "ndm", "ndn", "ndp", "ndq", 
    "ndr", "nds", "ndt", "ndu", "ndv", "ndw", "ndx", "ndy", 
    "ndz", "nep", "nea", "neb", "nec", "ned", "nee", "nef", 
    "neg", "neh", "nei", "nej", "nek", "nem", "nen", "neo", 
    "neq", "ner", "nes", "net", "neu", "nev", "new", "nex", 
    "ney", "nez", "nfa", "nfd", "nfg", "nfk", "nfl", "nfr", 
    "nfu", "ndo", "nga", "ngb", "ngc", "ngd", "nge", "ngg", 
    "ngh", "ngi", "ngj", "ngk", "ngl", "ngm", "ngn", "ngo", 
    "ngp", "ngq", "ngr", "ngs", "ngt", "ngu", "ngv", "ngw", 
    "ngx", "ngy", "ngz", "nha", "nhb", "nhc", "nhd", "nhe", 
    "nhf", "nhg", "nhh", "nhi", "nhj", "nhk", "nhm", "nhn", 
    "nho", "nhp", "nhq", "nhr", "nhs", "nht", "nhu", "nhv", 
    "nhw", "nhx", "nhy", "nhz", "nia", "nib", "nic", "nid", 
    "nie", "nif", "nig", "nih", "nii", "nij", "nik", "nil", 
    "nim", "nin", "nio", "niq", "nir", "nis", "nit", "niu", 
    "niv", "niw", "nix", "niy", "niz", "nja", "njb", "njd", 
    "njh", "nji", "njj", "njl", "njm", "njn", "njo", "njr", 
    "njs", "njt", "nju", "njx", "njy", "njz", "nka", "nkb", 
    "nkc", "nkd", "nke", "nkf", "nkg", "nkh", "nki", "nkj", 
    "nkk", "nkm", "nkn", "nko", "nkp", "nkq", "nkr", "nks", 
    "nkt", "nku", "nkv", "nkw", "nkx", "nky", "nkz", "nld", 
    "nla", "nlc", "nle", "nlg", "nli", "nlj", "nlk", "nll", 
    "nln", "nlo", "nlq", "nlr", "nlu", "nlv", "nlw", "nlx", 
    "nly", "nlz", "nma", "nmb", "nmc", "nmd", "nme", "nmf", 
    "nmg", "nmh", "nmi", "nmj", "nmk", "nml", "nmm", "nmn", 
    "nmo", "nmp", "nmq", "nmr", "nms", "nmt", "nmu", "nmv", 
    "nmw", "nmx", "nmy", "nmz", "nno", "nna", "nnb", "nnc", 
    "nnd", "nne", "nnf", "nng", "nnh", "nni", "nnj", "nnk", 
    "nnl", "nnm", "nnn", "nnp", "nnq", "nnr", "nns", "nnt", 
    "nnu", "nnv", "nnw", "nnx", "nny", "nnz", "nor", "noa", 
    "noc", "nod", "noe", "nof", "nog", "noh", "noi", "noj", 
    "nok", "nol", "nom", "non", "noo", "nop", "noq", "nos", 
    "not", "nou", "nov", "now", "noy", "noz", "npa", "npb", 
    "npg", "nph", "npi", "npl", "npn", "npo", "nps", "npu", 
    "npy", "nqg", "nqk", "nqm", "nqn", "nqo", "nqq", "nqy", 
    "nbl", "nra", "nrb", "nrc", "nre", "nrg", "nri", "nrk", 
    "nrl", "nrm", "nrn", "nrp", "nrr", "nrt", "nru", "nrx", 
    "nrz", "nsa", "nsc", "nsd", "nse", "nsf", "nsg", "nsh", 
    "nsi", "nsk", "nsl", "nsm", "nsn", "nso", "nsp", "nsq", 
    "nsr", "nss", "nst", "nsu", "nsv", "nsw", "nsx", "nsy", 
    "nsz", "nte", "ntg", "nti", "ntj", "ntk", "ntm", "nto", 
    "ntp", "ntr", "nts", "ntu", "ntw", "ntx", "nty", "ntz", 
    "nua", "nub", "nuc", "nud", "nue", "nuf", "nug", "nuh", 
    "nui", "nuj", "nuk", "nul", "num", "nun", "nuo", "nup", 
    "nuq", "nur", "nus", "nut", "nuu", "nuv", "nuw", "nux", 
    "nuy", "nuz", "nav", "nvh", "nvm", "nvo", "nwa", "nwb", 
    "nwc", "nwe", "nwg", "nwi", "nwm", "nwo", "nwr", "nwx", 
    "nwy", "nxa", "nxd", "nxe", "nxg", "nxi", "nxj", "nxk", 
    "nxl", "nxm", "nxn", "nxq", "nxr", "nxu", "nxx", "nya", 
    "nyb", "nyc", "nyd", "nye", "nyf", "nyg", "nyh", "nyi", 
    "nyj", "nyk", "nyl", "nym", "nyn", "nyo", "nyp", "nyq", 
    "nyr", "nys", "nyt", "nyu", "nyv", "nyw", "nyx", "nyy", 
    "nza", "nzb", "nzi", "nzk", "nzm", "nzs", "nzu", "nzy", 
    "nzz", 
    "oaa", "oac", "oar", "oav", "obi", "obk", "obl", "obm", 
    "obo", "obr", "obt", "obu", "oci", "oca", "occ", "och", 
    "oco", "ocu", "oda", "odk", "odt", "odu", "ofo", "ofs", 
    "ofu", "ogb", "ogc", "oge", "ogg", "ogn", "ogo", "ogu", 
    "oht", "ohu", "oia", "oin", "oji", "ojb", "ojc", "ojg", 
    "ojp", "ojs", "ojv", "ojw", "oka", "okb", "okd", "oke", 
    "okg", "okh", "oki", "okj", "okk", "okl", "okm", "okn", 
    "oko", "okr", "oks", "oku", "okv", "okx", "ola", "old", 
    "ole", "olk", "olm", "olo", "olr", "orm", "oma", "omb", 
    "omc", "ome", "omg", "omi", "omk", "oml", "omn", "omo", 
    "omp", "omr", "omt", "omu", "omw", "omx", "ona", "onb", 
    "one", "ong", "oni", "onj", "onk", "onn", "ono", "onp", 
    "onr", "ons", "ont", "onu", "onw", "onx", "ood", "oog", 
    "oon", "oor", "oos", "opa", "ope", "opk", "opm", "opo", 
    "opt", "opy", "ori", "ora", "orc", "ore", "org", "orh", 
    "ork", "orn", "oro", "orr", "ors", "ort", "oru", "orv", 
    "orw", "orx", "ory", "orz", "oss", "osa", "osc", "osi", 
    "oso", "osp", "ost", "osu", "osx", "ota", "otb", "otd", 
    "ote", "oti", "otk", "otl", "otm", "otn", "oto", "otq", 
    "otr", "ots", "ott", "otu", "otw", "otx", "oty", "otz", 
    "oua", "oub", "oue", "oui", "oum", "oun", "owi", "owl", 
    "oyb", "oyd", "oym", "oyy", "ozm", 
    "pan", "paa", "pab", "pac", "pad", "pae", "paf", "pag", 
    "pah", "pai", "paj", "pak", "pal", "pam", "pao", "pap", 
    "paq", "par", "pas", "pat", "pau", "pav", "paw", "pax", 
    "pay", "paz", "pbb", "pbc", "pbe", "pbf", "pbg", "pbh", 
    "pbi", "pbl", "pbn", "pbo", "pbp", "pbr", "pbs", "pbt", 
    "pbu", "pbv", "pby", "pbz", "pca", "pcb", "pcc", "pcd", 
    "pce", "pcf", "pcg", "pch", "pci", "pcj", "pck", "pcl", 
    "pcm", "pcn", "pcp", "pcr", "pcw", "pda", "pdc", "pdi", 
    "pdn", "pdo", "pdt", "pdu", "pea", "peb", "pec", "ped", 
    "pee", "pef", "peg", "peh", "pei", "pej", "pek", "pel", 
    "pem", "pen", "peo", "pep", "peq", "pes", "pev", "pex", 
    "pey", "pez", "pfa", "pfe", "pfl", "pga", "pgg", "pgi", 
    "pgk", "pgl", "pgn", "pgs", "pgu", "pgy", "pha", "phd", 
    "phg", "phh", "phi", "phk", "phl", "phm", "phn", "pho", 
    "phq", "phr", "pht", "phu", "phv", "phw", "pli", "pia", 
    "pib", "pic", "pid", "pie", "pif", "pig", "pih", "pii", 
    "pij", "pil", "pim", "pin", "pio", "pip", "pir", "pis", 
    "pit", "piu", "piv", "piw", "pix", "piy", "piz", "pjt", 
    "pka", "pkb", "pkc", "pkg", "pkh", "pkn", "pko", "pkp", 
    "pkr", "pks", "pkt", "pku", "pol", "pla", "plb", "plc", 
    "pld", "ple", "plg", "plh", "plj", "plk", "pll", "plm", 
    "pln", "plo", "plp", "plq", "plr", "pls", "plt", "plu", 
    "plv", "plw", "ply", "plz", "pma", "pmb", "pmc", "pmd", 
    "pme", "pmf", "pmh", "pmi", "pmj", "pmk", "pml", "pmm", 
    "pmn", "pmo", "pmq", "pmr", "pms", "pmt", "pmu", "pmw", 
    "pmx", "pmy", "pmz", "pna", "pnb", "pnc", "pne", "png", 
    "pnh", "pni", "pnj", "pnk", "pnm", "pnn", "pno", "pnp", 
    "pnq", "pnr", "pns", "pnt", "pnu", "pnv", "pnw", "pnx", 
    "pny", "pnz", "poa", "pob", "poc", "pod", "poe", "pof", 
    "pog", "poh", "poi", "poj", "pok", "pom", "pon", "poo", 
    "pop", "poq", "pos", "pot", "pou", "pov", "pow", "pox", 
    "poy", "ppa", "ppe", "ppi", "ppk", "ppl", "ppm", "ppn", 
    "ppo", "ppp", "ppq", "ppr", "pps", "ppt", "ppu", "ppv", 
    "pqa", "pqm", "pra", "prb", "prc", "prd", "pre", "prf", 
    "prg", "prh", "pri", "prk", "prl", "prm", "prn", "pro", 
    "prp", "prq", "prr", "prs", "prt", "pru", "prv", "prw", 
    "prx", "pry", "prz", "pus", "psa", "psc", "psd", "pse", 
    "psg", "psh", "psi", "psl", "psm", "psn", "pso", "psp", 
    "psq", "psr", "pss", "pst", "psu", "psw", "psy", "por", 
    "pta", "pth", "pti", "ptn", "pto", "ptp", "ptr", "ptt", 
    "ptu", "ptv", "ptw", "pty", "pua", "pub", "puc", "pud", 
    "pue", "puf", "pug", "pui", "puj", "puk", "pum", "pun", 
    "puo", "pup", "puq", "pur", "put", "puu", "puw", "pux", 
    "puy", "puz", "pwa", "pwb", "pwg", "pwi", "pwm", "pwn", 
    "pwo", "pwr", "pww", "pxm", "pye", "pym", "pyn", "pys", 
    "pyu", "pyx", "pyy", "pzn", 
    "qaa", "qab", "qac", "qad", "qae", "qaf", "qag", "qah", 
    "qai", "qaj", "qak", "qal", "qam", "qan", "qao", "qap", 
    "qaq", "qar", "qas", "qat", "qau", "qav", "qaw", "qax", 
    "qay", "qaz", "qba", "qbb", "qbc", "qbd", "qbe", "qbf", 
    "qbg", "qbh", "qbi", "qbj", "qbk", "qbl", "qbm", "qbn", 
    "qbo", "qbp", "qbq", "qbr", "qbs", "qbt", "qbu", "qbv", 
    "qbw", "qbx", "qby", "qbz", "qca", "qcb", "qcc", "qcd", 
    "qce", "qcf", "qcg", "qch", "qci", "qcj", "qck", "qcl", 
    "qcm", "qcn", "qco", "qcp", "qcq", "qcr", "qcs", "qct", 
    "qcu", "qcv", "qcw", "qcx", "qcy", "qcz", "qda", "qdb", 
    "qdc", "qdd", "qde", "qdf", "qdg", "qdh", "qdi", "qdj", 
    "qdk", "qdl", "qdm", "qdn", "qdo", "qdp", "qdq", "qdr", 
    "qds", "qdt", "qdu", "qdv", "qdw", "qdx", "qdy", "qdz", 
    "qea", "qeb", "qec", "qed", "qee", "qef", "qeg", "qeh", 
    "qei", "qej", "qek", "qel", "qem", "qen", "qeo", "qep", 
    "qeq", "qer", "qes", "qet", "qeu", "qev", "qew", "qex", 
    "qey", "qez", "qfa", "qfb", "qfc", "qfd", "qfe", "qff", 
    "qfg", "qfh", "qfi", "qfj", "qfk", "qfl", "qfm", "qfn", 
    "qfo", "qfp", "qfq", "qfr", "qfs", "qft", "qfu", "qfv", 
    "qfw", "qfx", "qfy", "qfz", "qga", "qgb", "qgc", "qgd", 
    "qge", "qgf", "qgg", "qgh", "qgi", "qgj", "qgk", "qgl", 
    "qgm", "qgn", "qgo", "qgp", "qgq", "qgr", "qgs", "qgt", 
    "qgu", "qgv", "qgw", "qgx", "qgy", "qgz", "qha", "qhb", 
    "qhc", "qhd", "qhe", "qhf", "qhg", "qhh", "qhi", "qhj", 
    "qhk", "qhl", "qhm", "qhn", "qho", "qhp", "qhq", "qhr", 
    "qhs", "qht", "qhu", "qhv", "qhw", "qhx", "qhy", "qhz", 
    "qia", "qib", "qic", "qid", "qie", "qif", "qig", "qih", 
    "qii", "qij", "qik", "qil", "qim", "qin", "qio", "qip", 
    "qiq", "qir", "qis", "qit", "qiu", "qiv", "qiw", "qix", 
    "qiy", "qiz", "qja", "qjb", "qjc", "qjd", "qje", "qjf", 
    "qjg", "qjh", "qji", "qjj", "qjk", "qjl", "qjm", "qjn", 
    "qjo", "qjp", "qjq", "qjr", "qjs", "qjt", "qju", "qjv", 
    "qjw", "qjx", "qjy", "qjz", "qka", "qkb", "qkc", "qkd", 
    "qke", "qkf", "qkg", "qkh", "qki", "qkj", "qkk", "qkl", 
    "qkm", "qkn", "qko", "qkp", "qkq", "qkr", "qks", "qkt", 
    "qku", "qkv", "qkw", "qkx", "qky", "qkz", "qla", "qlb", 
    "qlc", "qld", "qle", "qlf", "qlg", "qlh", "qli", "qlj", 
    "qlk", "qll", "qlm", "qln", "qlo", "qlp", "qlq", "qlr", 
    "qls", "qlt", "qlu", "qlv", "qlw", "qlx", "qly", "qlz", 
    "qma", "qmb", "qmc", "qmd", "qme", "qmf", "qmg", "qmh", 
    "qmi", "qmj", "qmk", "qml", "qmm", "qmn", "qmo", "qmp", 
    "qmq", "qmr", "qms", "qmt", "qmu", "qmv", "qmw", "qmx", 
    "qmy", "qmz", "qna", "qnb", "qnc", "qnd", "qne", "qnf", 
    "qng", "qnh", "qni", "qnj", "qnk", "qnl", "qnm", "qnn", 
    "qno", "qnp", "qnq", "qnr", "qns", "qnt", "qnu", "qnv", 
    "qnw", "qnx", "qny", "qnz", "qoa", "qob", "qoc", "qod", 
    "qoe", "qof", "qog", "qoh", "qoi", "qoj", "qok", "qol", 
    "qom", "qon", "qoo", "qop", "qoq", "qor", "qos", "qot", 
    "qou", "qov", "qow", "qox", "qoy", "qoz", "qpa", "qpb", 
    "qpc", "qpd", "qpe", "qpf", "qpg", "qph", "qpi", "qpj", 
    "qpk", "qpl", "qpm", "qpn", "qpo", "qpp", "qpq", "qpr", 
    "qps", "qpt", "qpu", "qpv", "qpw", "qpx", "qpy", "qpz", 
    "qqa", "qqb", "qqc", "qqd", "qqe", "qqf", "qqg", "qqh", 
    "qqi", "qqj", "qqk", "qql", "qqm", "qqn", "qqo", "qqp", 
    "qqq", "qqr", "qqs", "qqt", "qqu", "qqv", "qqw", "qqx", 
    "qqy", "qqz", "qra", "qrb", "qrc", "qrd", "qre", "qrf", 
    "qrg", "qrh", "qri", "qrj", "qrk", "qrl", "qrm", "qrn", 
    "qro", "qrp", "qrq", "qrr", "qrs", "qrt", "qru", "qrv", 
    "qrw", "qrx", "qry", "qrz", "qsa", "qsb", "qsc", "qsd", 
    "qse", "qsf", "qsg", "qsh", "qsi", "qsj", "qsk", "qsl", 
    "qsm", "qsn", "qso", "qsp", "qsq", "qsr", "qss", "qst", 
    "qsu", "qsv", "qsw", "qsx", "qsy", "qsz", "qta", "qtb", 
    "qtc", "qtd", "qte", "qtf", "qtg", "qth", "qti", "qtj", 
    "qtk", "qtl", "qtm", "qtn", "qto", "qtp", "qtq", "qtr", 
    "qts", "qtt", "qtu", "qtv", "qtw", "qtx", "qty", "qtz", 
    "que", "qua", "qub", "quc", "qud", "quf", "qug", "quh", 
    "qui", "quj", "quk", "qul", "qum", "qun", "qup", "quq", 
    "qur", "qus", "qut", "quu", "quv", "quw", "qux", "quy", 
    "quz", "qva", "qvc", "qve", "qvh", "qvi", "qvj", "qvl", 
    "qvm", "qvn", "qvo", "qvp", "qvs", "qvw", "qvy", "qvz", 
    "qwa", "qwc", "qwh", "qwm", "qws", "qwt", "qxa", "qxc", 
    "qxh", "qxi", "qxl", "qxn", "qxo", "qxp", "qxq", "qxr", 
    "qxs", "qxt", "qxu", "qxw", "qya", "qyp", 
    "raa", "rab", "rac", "rad", "rae", "raf", "rag", "rah", 
    "rai", "raj", "rak", "ral", "ram", "ran", "rao", "rap", 
    "raq", "rar", "ras", "rat", "rau", "rav", "raw", "rax", 
    "ray", "raz", "rbb", "rbk", "rbl", "rbp", "rcf", "rdb", 
    "rea", "reb", "ree", "reg", "rei", "rej", "rel", "rem", 
    "ren", "rer", "res", "ret", "rey", "rga", "rge", "rgk", 
    "rgn", "rgr", "rgs", "rgu", "rhg", "rhp", "ria", "rie", 
    "rif", "ril", "rim", "rin", "rir", "rit", "riu", "rjb", 
    "rjg", "rji", "rjs", "rka", "rkb", "rkh", "rki", "rkm", 
    "rkt", "rkw", "roh", "rma", "rmb", "rmc", "rmd", "rme", 
    "rmf", "rmg", "rmh", "rmi", "rmk", "rml", "rmm", "rmn", 
    "rmo", "rmp", "rmq", "rmr", "rms", "rmt", "rmu", "rmv", 
    "rmw", "rmx", "rmy", "rmz", "run", "rna", "rnd", "rng", 
    "rnl", "rnn", "rnp", "rnr", "rnw", "ron", "roa", "rob", 
    "roc", "rod", "roe", "rof", "rog", "rol", "rom", "roo", 
    "rop", "ror", "rou", "row", "rpn", "rpt", "rri", "rro", 
    "rrt", "rsb", "rsi", "rsl", "rtc", "rth", "rtm", "rtw", 
    "rus", "rub", "ruc", "rue", "ruf", "rug", "ruh", "rui", 
    "ruk", "ruo", "rup", "ruq", "rut", "ruu", "ruy", "ruz", 
    "kin", "rwa", "rwk", "rwm", "rwo", "rwr", "rws", "rxd", 
    "rxw", "ryn", "rys", "ryu", 
    "san", "saa", "sab", "sac", "sad", "sae", "saf", "sah", 
    "sai", "saj", "sak", "sal", "sam", "sao", "sap", "saq", 
    "sar", "sas", "sat", "sau", "sav", "saw", "sax", "say", 
    "saz", "sba", "sbb", "sbc", "sbd", "sbe", "sbf", "sbg", 
    "sbh", "sbi", "sbj", "sbk", "sbl", "sbm", "sbn", "sbo", 
    "sbp", "sbq", "sbr", "sbs", "sbt", "sbu", "sbv", "sbw", 
    "sbx", "sby", "sbz", "srd", "sca", "scb", "scc", "sce", 
    "scf", "scg", "sch", "sci", "sck", "scl", "scn", "sco", 
    "scp", "scq", "scr", "scs", "scu", "scv", "scw", "scx", 
    "snd", "sda", "sdb", "sdc", "sdd", "sde", "sdf", "sdg", 
    "sdh", "sdi", "sdj", "sdk", "sdl", "sdm", "sdn", "sdo", 
    "sdp", "sdr", "sds", "sdt", "sdu", "sdx", "sdz", "sme", 
    "sea", "seb", "sec", "sed", "see", "sef", "seg", "seh", 
    "sei", "sej", "sek", "sel", "sem", "sen", "seo", "sep", 
    "seq", "ser", "ses", "set", "seu", "sev", "sew", "sey", 
    "sez", "sfb", "sfe", "sfm", "sfs", "sfw", "sag", "sga", 
    "sgb", "sgc", "sgd", "sge", "sgg", "sgh", "sgi", "sgj", 
    "sgk", "sgl", "sgm", "sgn", "sgo", "sgp", "sgr", "sgs", 
    "sgt", "sgu", "sgw", "sgx", "sgy", "sgz",/*hsb*/ "sha", /* sh -> hsb is deprecated */
    "shb", "shc", "shd", "she", "shg", "shh", "shi", "shj", 
    "shk", "shl", "shm", "shn", "sho", "shp", "shq", "shr", 
    "shs", "sht", "shu", "shv", "shw", "shx", "shy", "shz", 
    "sin", "sia", "sib", "sic", "sid", "sie", "sif", "sig", 
    "sih", "sii", "sij", "sik", "sil", "sim", "sio", "sip", 
    "siq", "sir", "sis", "sit", "siu", "siv", "siw", "six", 
    "siy", "siz", "sja", "sjb", "sjd", "sje", "sjg", "sjk", 
    "sjl", "sjm", "sjn", "sjo", "sjp", "sjr", "sjs", "sjt", 
    "sju", "sjw", "slk", "ska", "skb", "skc", "skd", "ske", 
    "skf", "skg", "skh", "ski", "skj", "skk", "skl", "skm", 
    "skn", "sko", "skp", "skq", "skr", "sks", "skt", "sku", 
    "skv", "skw", "skx", "sky", "skz", "slv", "sla", "slb", 
    "slc", "sld", "sle", "slf", "slg", "slh", "sli", "slj", 
    "sll", "slm", "sln", "slp", "slq", "slr", "sls", "slt", 
    "slu", "slw", "slx", "sly", "slz", "smo", "sma", "smb", 
    "smc", "smd", "smf", "smg", "smh", "smi", "smj", "smk", 
    "sml", "smm", "smn", "smp", "smq", "smr", "sms", "smt", 
    "smu", "smv", "smw", "smx", "smy", "smz", "sna", "snb", 
    "snc", "sne", "snf", "sng", "snh", "sni", "snj", "snk", 
    "snl", "snm", "snn", "sno", "snp", "snq", "snr", "sns", 
    "snu", "snv", "snw", "snx", "sny", "snz", "som", "soa", 
    "sob", "soc", "sod", "soe", "sog", "soh", "soi", "soj", 
    "sok", "sol", "son", "soo", "sop", "soq", "sor", "sos", 
    "sou", "sov", "sow", "sox", "soy", "soz", "spb", "spc", 
    "spd", "spe", "spg", "spi", "spk", "spl", "spm", "spo", 
    "spp", "spq", "spr", "sps", "spt", "spu", "spv", "spx", 
    "spy", "sqi", "sqa", "sqh", "sqk", "sqm", "sqn", "sqo", 
    "sqq", "sqr", "sqs", "sqt", "squ", "srp", "sra", "srb", 
    "src", "sre", "srf", "srg", "srh", "sri", "srj", "srk", 
    "srl", "srm", "srn", "sro", "srq", "srr", "srs", "srt", 
    "sru", "srv", "srw", "srx", "sry", "srz", "ssw", "ssa", 
    "ssb", "ssc", "ssd", "sse", "ssf", "ssg", "ssh", "ssi", 
    "ssj", "ssk", "ssl", "ssm", "ssn", "sso", "ssp", "ssq", 
    "ssr", "sss", "sst", "ssu", "ssv", "ssx", "ssy", "ssz", 
    "sot", "sta", "stb", "stc", "std", "ste", "stf", "stg", 
    "sth", "sti", "stj", "stk", "stl", "stm", "stn", "sto", 
    "stp", "stq", "str", "sts", "stt", "stu", "stv", "stw", 
    "sun", "sua", "sub", "suc", "sue", "suf", "sug", "suh", 
    "sui", "suj", "suk", "sul", "sum", "suq", "sur", "sus", 
    "sut", "suu", "suv", "suw", "sux", "suy", "suz", "swe", 
    "sva", "svb", "svc", "sve", "svk", "svm", "svr", "svs", 
    "svx", "swa", "swb", "swc", "swf", "swg", "swh", "swi", 
    "swj", "swk", "swl", "swm", "swn", "swo", "swp", "swq", 
    "swr", "sws", "swt", "swu", "swv", "sww", "swx", "swy", 
    "sxb", "sxc", "sxe", "sxg", "sxk", "sxl", "sxm", "sxn", 
    "sxo", "sxr", "sxs", "sxu", "sxw", "sya", "syb", "syc", 
    "syi", "syk", "syl", "sym", "syn", "syo", "syr", "sys", 
    "syw", "syy", "sza", "szb", "szc", "szd", "sze", "szg", 
    "szk", "szl", "szn", "szp", "szv", "szw", 
    "tam", "taa", "tab", "tac", "tad", "tae", "taf", "tag", 
    "tai", "taj", "tak", "tal", "tan", "tao", "tap", "taq", 
    "tar", "tas", "tau", "tav", "taw", "tax", "tay", "taz", 
    "tba", "tbb", "tbc", "tbd", "tbe", "tbf", "tbg", "tbh", 
    "tbi", "tbj", "tbk", "tbl", "tbm", "tbn", "tbo", "tbp", 
    "tbr", "tbs", "tbt", "tbu", "tbv", "tbw", "tbx", "tby", 
    "tbz", "tca", "tcb", "tcc", "tcd", "tce", "tcf", "tcg", 
    "tch", "tci", "tck", "tcl", "tcm", "tcn", "tco", "tcp", 
    "tcq", "tcs", "tct", "tcu", "tcw", "tcx", "tcy", "tcz", 
    "tda", "tdb", "tdc", "tdd", "tde", "tdf", "tdg", "tdh", 
    "tdi", "tdj", "tdk", "tdl", "tdn", "tdo", "tdq", "tdr", 
    "tds", "tdt", "tdu", "tdv", "tdx", "tdy", "tel", "tea", 
    "teb", "tec", "ted", "tee", "tef", "teg", "teh", "tei", 
    "tek", "tem", "ten", "teo", "tep", "teq", "ter", "tes", 
    "tet", "teu", "tev", "tew", "tex", "tey", "tfi", "tfn", 
    "tfo", "tfr", "tft", "tgk", "tga", "tgb", "tgc", "tgd", 
    "tge", "tgf", "tgg", "tgh", "tgi", "tgj", "tgn", "tgo", 
    "tgp", "tgq", "tgr", "tgs", "tgt", "tgu", "tgv", "tgw", 
    "tgx", "tgy", "tgz", "tha", "thc", "thd", "the", "thf", 
    "thh", "thi", "thk", "thl", "thm", "thn", "thp", "thq", 
    "thr", "ths", "tht", "thu", "thv", "thw", "thx", "thy", 
    "thz", "tir", "tia", "tic", "tid", "tie", "tif", "tig", 
    "tih", "tii", "tij", "tik", "til", "tim", "tin", "tio", 
    "tip", "tiq", "tis", "tit", "tiu", "tiv", "tiw", "tix", 
    "tiy", "tiz", "tja", "tjg", "tji", "tjl", "tjm", "tjn", 
    "tjo", "tjs", "tju", "tjw", "tuk", "tka", "tkb", "tkd", 
    "tke", "tkf", "tkg", "tkk", "tkl", "tkm", "tkn", "tkp", 
    "tkq", "tkr", "tks", "tkt", "tku", "tkw", "tkx", "tkz", 
    "tgl", "tla", "tlb", "tlc", "tld", "tle", "tlf", "tlg", 
    "tlh", "tli", "tlj", "tlk", "tll", "tlm", "tln", "tlo", 
    "tlp", "tlq", "tlr", "tls", "tlt", "tlu", "tlv", "tlw", 
    "tlx", "tly", "tlz", "tma", "tmb", "tmc", "tmd", "tme", 
    "tmf", "tmg", "tmh", "tmi", "tmj", "tmk", "tml", "tmm", 
    "tmn", "tmo", "tmp", "tmq", "tmr", "tms", "tmt", "tmu", 
    "tmv", "tmw", "tmx", "tmy", "tmz", "tsn", "tna", "tnb", 
    "tnc", "tnd", "tne", "tnf", "tng", "tnh", "tni", "tnj", 
    "tnk", "tnl", "tnm", "tnn", "tno", "tnp", "tnq", "tnr", 
    "tns", "tnt", "tnu", "tnv", "tnw", "tnx", "tny", "tnz", 
    "ton", "tob", "toc", "tod", "toe", "tof", "tog", "toh", 
    "toi", "toj", "tol", "tom", "too", "top", "toq", "tor", 
    "tos", "tot", "tou", "tov", "tow", "tox", "toy", "toz", 
    "tpa", "tpc", "tpe", "tpf", "tpg", "tpi", "tpj", "tpk", 
    "tpl", "tpm", "tpn", "tpo", "tpp", "tpq", "tpr", "tpt", 
    "tpu", "tpv", "tpw", "tpx", "tpy", "tpz", "tqb", "tql", 
    "tqm", "tqn", "tqo", "tqp", "tqq", "tqr", "tqt", "tqu", 
    "tqw", "tur", "tra", "trb", "trc", "trd", "tre", "trf", 
    "trg", "trh", "tri", "trj", "trl", "trm", "trn", "tro", 
    "trp", "trq", "trr", "trs", "trt", "tru", "trv", "trw", 
    "trx", "try", "trz", "tso", "tsa", "tsb", "tsc", "tsd", 
    "tse", "tsf", "tsg", "tsh", "tsi", "tsj", "tsk", "tsl", 
    "tsm", "tsp", "tsq", "tsr", "tss", "tst", "tsu", "tsv", 
    "tsw", "tsx", "tsy", "tsz", "tat", "tta", "ttb", "ttc", 
    "ttd", "tte", "ttf", "ttg", "tth", "tti", "ttj", "ttk", 
    "ttl", "ttm", "ttn", "tto", "ttp", "ttq", "ttr", "tts", 
    "ttt", "ttu", "ttv", "ttw", "ttx", "tty", "ttz", "tua", 
    "tub", "tuc", "tud", "tue", "tuf", "tug", "tuh", "tui", 
    "tuj", "tul", "tum", "tun", "tuo", "tup", "tuq", "tus", 
    "tut", "tuu", "tuv", "tux", "tuy", "tuz", "tva", "tvd", 
    "tve", "tvk", "tvl", "tvm", "tvn", "tvo", "tvs", "tvt", 
    "tvu", "tvw", "tvy", "twi", "twa", "twb", "twc", "twd", 
    "twe", "twf", "twg", "twh", "twl", "twm", "twn", "two", 
    "twp", "twq", "twr", "twt", "twu", "tww", "twx", "twy", 
    "txa", "txb", "txc", "txe", "txg", "txh", "txi", "txm", 
    "txn", "txo", "txq", "txr", "txs", "txt", "txu", "txx", 
    "txy", "tah", "tya", "tye", "tyh", "tyi", "tyj", "tyl", 
    "tyn", "typ", "tyr", "tys", "tyt", "tyu", "tyv", "tyx", 
    "tyz", "tza", "tzb", "tzc", "tze", "tzh", "tzj", "tzl", 
    "tzm", "tzn", "tzo", "tzs", "tzt", "tzu", "tzx", "tzz", 
    "uam", "uan", "uar", "uba", "ubi", "ubl", "ubm", "ubr", 
    "ubu", "uby", "uda", "ude", "udg", "udi", "udj", "udl", 
    "udm", "udu", "ues", "ufi", "uig", "uga", "ugb", "uge", 
    "ugn", "ugo", "ugy", "uha", "uhn", "uis", "uiv", "uji", 
    "ukr", "uka", "ukg", "ukh", "ukl", "ukp", "ukq", "uks", 
    "uku", "ukw", "uky", "ula", "ulb", "ulc", "ule", "ulf", 
    "uli", "ulk", "ull", "ulm", "uln", "ulu", "ulw", "uma", 
    "umb", "umc", "umd", "umg", "umi", "umm", "umn", "umo", 
    "ump", "umr", "ums", "umu", "una", "und", "une", "ung", 
    "unk", "unm", "unn", "unp", "unr", "unu", "unx", "unz", 
    "uok", "upi", "upv", "urd", "ura", "urb", "urc", "ure", 
    "urf", "urg", "urh", "uri", "urk", "url", "urm", "urn", 
    "uro", "urp", "urr", "urt", "uru", "urv", "urw", "urx", 
    "ury", "urz", "usa", "ush", "usi", "usk", "usp", "usu", 
    "uta", "ute", "utp", "utr", "utu", "uum", "uun", "uur", 
    "uuu", "uve", "uvh", "uvl", "uwa", "uya", "uzb", "uzn", 
    "uzs", 
    "vaa", "vae", "vaf", "vag", "vah", "vai", "vaj", "val", 
    "vam", "van", "vao", "vap", "var", "vas", "vau", "vav", 
    "vay", "vbb", "vbk", "ven", "vec", "ved", "vel", "vem", 
    "veo", "vep", "ver", "vgr", "vgt", "vie", "vic", "vid", 
    "vif", "vig", "vil", "vin", "vis", "vit", "viv", "vka", 
    "vki", "vkj", "vkk", "vkl", "vkm", "vko", "vkp", "vkt", 
    "vku", "vky", "vlp", "vlr", "vls", "vma", "vmb", "vmc", 
    "vmd", "vme", "vmf", "vmg", "vmh", "vmi", "vmj", "vmk", 
    "vml", "vmm", "vmo", "vmp", "vmq", "vmr", "vms", "vmu", 
    "vmv", "vmw", "vmx", "vmy", "vmz", "vnk", "vnm", "vnp", 
    "vol", "vor", "vot", "vra", "vro", "vrs", "vrt", "vsi", 
    "vsl", "vsv", "vto", "vum", "vun", "vut", "vwa", 
    "wln", "waa", "wab", "wac", "wad", "wae", "waf", "wag", 
    "wah", "wai", "waj", "wak", "wal", "wam", "wan", "wao", 
    "wap", "waq", "war", "was", "wat", "wau", "wav", "waw", 
    "wax", "way", "waz", "wba", "wbb", "wbe", "wbf", "wbh", 
    "wbi", "wbj", "wbk", "wbl", "wbm", "wbp", "wbq", "wbr", 
    "wbt", "wbv", "wbw", "wca", "wci", "wdd", "wdg", "wdj", 
    "wdk", "wdu", "wdy", "wea", "wec", "wed", "weg", "weh", 
    "wei", "wem", "wen", "weo", "wep", "wer", "wes", "wet", 
    "weu", "wew", "wfg", "wga", "wgb", "wgg", "wgi", "wgo", 
    "wgu", "wgw", "wgy", "wha", "whg", "whk", "whu", "wib", 
    "wic", "wie", "wif", "wig", "wih", "wii", "wij", "wik", 
    "wil", "wim", "win", "wir", "wit", "wiu", "wiv", "wiw", 
    "wiy", "wja", "wji", "wka", "wkb", "wkd", "wkl", "wku", 
    "wkw", "wky", "wla", "wlc", "wle", "wlg", "wli", "wlk", 
    "wll", "wlm", "wlo", "wlr", "wls", "wlu", "wlv", "wlw", 
    "wlx", "wly", "wma", "wmb", "wmc", "wmd", "wme", "wmh", 
    "wmi", "wmm", "wmn", "wmo", "wms", "wmt", "wmw", "wmx", 
    "wnb", "wnc", "wnd", "wne", "wng", "wni", "wnk", "wnm", 
    "wnn", "wno", "wnp", "wnu", "wnw", "wny", "wol", "woa", 
    "wob", "woc", "wod", "woe", "wof", "wog", "woi", "wok", 
    "wom", "won", "woo", "wor", "wos", "wow", "woy", "wpc", 
    "wra", "wrb", "wrd", "wre", "wrg", "wrh", "wri", "wrk", 
    "wrl", "wrm", "wrn", "wro", "wrp", "wrr", "wrs", "wru", 
    "wrv", "wrw", "wrx", "wry", "wrz", "wsa", "wsi", "wsk", 
    "wsr", "wss", "wsu", "wsv", "wtf", "wth", "wti", "wtk", 
    "wtm", "wtw", "wua", "wub", "wud", "wuh", "wul", "wum", 
    "wun", "wur", "wut", "wuu", "wuv", "wux", "wuy", "wwa", 
    "wwb", "wwo", "wwr", "www", "wxa", "wxw", "wya", "wyb", 
    "wyi", "wym", "wyr", "wyy", 
    "xaa", "xab", "xac", "xad", "xae", "xag", "xah", "xai", 
    "xal", "xam", "xan", "xao", "xap", "xaq", "xar", "xas", 
    "xat", "xau", "xav", "xaw", "xay", "xba", "xbb", "xbc", 
    "xbd", "xbe", "xbg", "xbi", "xbj", "xbm", "xbn", "xbo", 
    "xbp", "xbr", "xbw", "xbx", "xby", "xcb", "xcc", "xce", 
    "xcg", "xch", "xcl", "xcm", "xcn", "xco", "xcr", "xct", 
    "xcu", "xcv", "xcw", "xcy", "xda", "xdc", "xdk", "xdm", 
    "xdy", "xeb", "xed", "xeg", "xel", "xem", "xep", "xer", 
    "xes", "xet", "xeu", "xfa", "xga", "xgb", "xgd", "xgf", 
    "xgg", "xgi", "xgl", "xgm", "xgr", "xgu", "xgw", "xho", 
    "xha", "xhc", "xhd", "xhe", "xhr", "xht", "xhu", "xhv", 
    "xia", "xib", "xii", "xil", "xin", "xip", "xir", "xiv", 
    "xiy", "xjb", "xjt", "xka", "xkb", "xkc", "xkd", "xke", 
    "xkf", "xkg", "xkh", "xki", "xkj", "xkk", "xkl", "xkm", 
    "xkn", "xko", "xkp", "xkq", "xkr", "xks", "xkt", "xku", 
    "xkv", "xkw", "xkx", "xky", "xkz", "xla", "xlb", "xlc", 
    "xld", "xle", "xlg", "xli", "xln", "xlo", "xlp", "xls", 
    "xlu", "xly", "xma", "xmb", "xmc", "xmd", "xme", "xmf", 
    "xmg", "xmh", "xmi", "xmj", "xmk", "xml", "xmm", "xmn", 
    "xmo", "xmp", "xmq", "xmr", "xms", "xmt", "xmu", "xmv", 
    "xmw", "xmx", "xmy", "xmz", "xna", "xnb", "xng", "xnh", 
    "xni", "xnk", "xnn", "xno", "xnr", "xns", "xnt", "xnu", 
    "xny", "xnz", "xoc", "xod", "xog", "xoi", "xok", "xom", 
    "xon", "xoo", "xop", "xor", "xow", "xpa", "xpc", "xpe", 
    "xpg", "xpi", "xpj", "xpk", "xpm", "xpn", "xpo", "xpp", 
    "xpq", "xpr", "xps", "xpt", "xpu", "xpy", "xqa", "xqt", 
    "xra", "xrb", "xrd", "xre", "xrg", "xri", "xrm", "xrn", 
    "xrq", "xrr", "xrt", "xru", "xrw", "xsa", "xsb", "xsc", 
    "xsd", "xse", "xsh", "xsi", "xsj", "xsk", "xsl", "xsm", 
    "xsn", "xso", "xsp", "xsq", "xsr", "xss", "xst", "xsu", 
    "xsv", "xsy", "xta", "xtb", "xtc", "xtd", "xte", "xtg", 
    "xth", "xti", "xtj", "xtl", "xtm", "xtn", "xto", "xtp", 
    "xtq", "xtr", "xts", "xtt", "xtu", "xtv", "xtw", "xty", 
    "xtz", "xua", "xub", "xud", "xuf", "xug", "xuj", "xul", 
    "xum", "xun", "xuo", "xup", "xur", "xut", "xuu", "xve", 
    "xvi", "xvn", "xvo", "xvs", "xwa", "xwc", "xwd", "xwe", 
    "xwg", "xwj", "xwk", "xwl", "xwo", "xwr", "xwt", "xww", 
    "xxb", "xxk", "xxm", "xxr", "xxt", "xya", "xyb", "xyj", 
    "xyk", "xyl", "xyt", "xyy", "xzh", "xzm", "xzp", 
    "yaa", "yab", "yac", "yad", "yae", "yaf", "yag", "yah", 
    "yai", "yaj", "yak", "yal", "yam", "yan", "yao", "yap", 
    "yaq", "yar", "yas", "yat", "yau", "yav", "yaw", "yax", 
    "yay", "yaz", "yba", "ybb", "ybd", "ybe", "ybh", "ybi", 
    "ybj", "ybk", "ybl", "ybm", "ybn", "ybo", "ybx", "yby", 
    "ych", "ycl", "ycn", "ycp", "yda", "ydd", "yde", "ydg", 
    "ydk", "yds", "yea", "yec", "yee", "yei", "yej", "yel", 
    "yen", "yer", "yes", "yet", "yeu", "yev", "yey", "yga", 
    "ygi", "ygl", "ygm", "ygp", "ygr", "ygu", "ygw", "yha", 
    "yhd", "yhl", "yid", "yia", "yib", "yif", "yig", "yih", 
    "yii", "yij", "yik", "yil", "yim", "yin", "yio", "yip", 
    "yiq", "yir", "yis", "yit", "yiu", "yiv", "yix", "yiy", 
    "yiz", "yka", "ykg", "yki", "ykk", "ykl", "ykm", "ykn", 
    "yko", "ykr", "ykt", "yku", "yky", "yla", "ylb", "yle", 
    "ylg", "yli", "yll", "ylm", "yln", "ylo", "ylr", "ylu", 
    "yly", "yma", "ymb", "ymc", "ymd", "yme", "ymg", "ymh", 
    "ymi", "ymj", "ymk", "yml", "ymm", "ymn", "ymo", "ymp", 
    "ymq", "ymr", "yms", "ymt", "ymx", "ymz", "yna", "ynd", 
    "yne", "yng", "ynh", "ynk", "ynl", "ynn", "yno", "ynq", 
    "yns", "ynu", "yor", "yob", "yog", "yoi", "yok", "yol", 
    "yom", "yon", "yos", "yot", "yox", "yoy", "ypa", "ypb", 
    "ypg", "yph", "ypk", "ypl", "ypm", "ypn", "ypo", "ypp", 
    "ypw", "ypz", "yra", "yrb", "yre", "yri", "yrk", "yrl", 
    "yrm", "yrn", "yrs", "yrw", "yry", "ysc", "ysd", "ysg", 
    "ysl", "ysn", "yso", "ysp", "ysr", "yss", "ysy", "yta", 
    "ytl", "ytp", "ytw", "yty", "yua", "yub", "yuc", "yud", 
    "yue", "yuf", "yug", "yui", "yuj", "yuk", "yul", "yum", 
    "yun", "yup", "yuq", "yur", "yus", "yut", "yuu", "yuw", 
    "yux", "yuy", "yuz", "yva", "yvt", "ywa", "ywg", "ywl", 
    "ywm", "ywn", "ywq", "ywr", "ywt", "ywu", "yww", "yxa", 
    "yxg", "yxl", "yxm", "yxu", "yxy", "yym", "yyr", "yyu", 
    "yyz", "yzg", "yzk", 
    "zha", "zaa", "zab", "zac", "zad", "zae", "zaf", "zag", 
    "zah", "zai", "zaj", "zak", "zal", "zam", "zao", "zap", 
    "zaq", "zar", "zas", "zat", "zau", "zav", "zaw", "zax", 
    "zay", "zaz", "zbc", "zbe", "zbl", "zbt", "zbw", "zca", 
    "zch", "zdj", "zea", "zeg", "zeh", "zen", "zga", "zgb", 
    "zgh", "zgm", "zgn", "zgr", "zho", "zhb", "zhd", "zhi", 
    "zhn", "zhw", "zia", "zib", "zik", "zil", "zim", "zin", 
    "zir", "ziw", "ziz", "zka", "zkb", "zkd", "zkg", "zkh", 
    "zkk", "zkn", "zko", "zkp", "zkr", "zkt", "zku", "zkv", 
    "zkz", "zlj", "zlm", "zln", "zlq", "zma", "zmb", "zmc", 
    "zmd", "zme", "zmf", "zmg", "zmh", "zmi", "zmj", "zmk", 
    "zml", "zmm", "zmn", "zmo", "zmp", "zmq", "zmr", "zms", 
    "zmt", "zmu", "zmv", "zmw", "zmx", "zmy", "zmz", "zna", 
    "znd", "zne", "zng", "znk", "zns", "zoc", "zoh", "zom", 
    "zoo", "zoq", "zor", "zos", "zpa", "zpb", "zpc", "zpd", 
    "zpe", "zpf", "zpg", "zph", "zpi", "zpj", "zpk", "zpl", 
    "zpm", "zpn", "zpo", "zpp", "zpq", "zpr", "zps", "zpt", 
    "zpu", "zpv", "zpw", "zpx", "zpy", "zpz", "zqe", "zra", 
    "zrg", "zrn", "zro", "zrp", "zrs", "zsa", "zsk", "zsl", 
    "zsm", "zsr", "zsu", "ztc", "zte", "ztg", "ztl", "ztm", 
    "ztn", "ztp", "ztq", "zts", "ztt", "ztu", "ztx", "zty", 
    "zul", "zua", "zuh", "zum", "zun", "zuy", "zwa", "zxx", 
    "zyb", "zyg", "zyj", "zyn", "zyp", "zza", "zzj", 
NULL,
/*  "in",  "iw",  "ji",  "jw",  "sh",                          */
    "ind", "heb", "yid", "jaw", "srp",
NULL
};

/**
 * Table of 2-letter country codes.
 *
 * This list must be in sorted order.  This list is returned directly
 * to the user by some API.
 *
 * This list must be kept in sync with COUNTRIES_3, with corresponding
 * entries matched.
 *
 * This table should be terminated with a NULL entry, followed by a
 * second list, and another NULL entry.  The first list is visible to
 * user code when this array is returned by API.  The second list
 * contains codes we support, but do not expose through user API.
 *
 * Notes:
 *
 * ZR(ZAR) is now CD(COD) and FX(FXX) is PS(PSE) as per
 * http://www.evertype.com/standards/iso3166/iso3166-1-en.html added
 * new codes keeping the old ones for compatibility updated to include
 * 1999/12/03 revisions *CWB*
 *
 * RO(ROM) is now RO(ROU) according to
 * http://www.iso.org/iso/en/prods-services/iso3166ma/03updates-on-iso-3166/nlv3e-rou.html
 */
static const char * const COUNTRIES[] = {
    "AD",  "AE",  "AF",  "AG",  "AI",  "AL",  "AM",
    "AO",  "AQ",  "AR",  "AS",  "AT",  "AU",  "AW",  "AX",  "AZ",
    "BA",  "BB",  "BD",  "BE",  "BF",  "BG",  "BH",  "BI",
    "BJ",  "BL",  "BM",  "BN",  "BO",  "BQ",  "BR",  "BS",  "BT",  "BV",
    "BW",  "BY",  "BZ",  "CA",  "CC",  "CD",  "CF",  "CG",
    "CH",  "CI",  "CK",  "CL",  "CM",  "CN",  "CO",  "CR",
    "CU",  "CV",  "CW",  "CX",  "CY",  "CZ",  "DE",  "DJ",  "DK",
    "DM",  "DO",  "DZ",  "EC",  "EE",  "EG",  "EH",  "ER",
    "ES",  "ET",  "FI",  "FJ",  "FK",  "FM",  "FO",  "FR",
    "GA",  "GB",  "GD",  "GE",  "GF",  "GG",  "GH",  "GI",  "GL",
    "GM",  "GN",  "GP",  "GQ",  "GR",  "GS",  "GT",  "GU",
    "GW",  "GY",  "HK",  "HM",  "HN",  "HR",  "HT",  "HU",
    "ID",  "IE",  "IL",  "IM",  "IN",  "IO",  "IQ",  "IR",  "IS",
    "IT",  "JE",  "JM",  "JO",  "JP",  "KE",  "KG",  "KH",  "KI",
    "KM",  "KN",  "KP",  "KR",  "KW",  "KY",  "KZ",  "LA",
    "LB",  "LC",  "LI",  "LK",  "LR",  "LS",  "LT",  "LU",
    "LV",  "LY",  "MA",  "MC",  "MD",  "ME",  "MF",  "MG",  "MH",  "MK",
    "ML",  "MM",  "MN",  "MO",  "MP",  "MQ",  "MR",  "MS",
    "MT",  "MU",  "MV",  "MW",  "MX",  "MY",  "MZ",  "NA",
    "NC",  "NE",  "NF",  "NG",  "NI",  "NL",  "NO",  "NP",
    "NR",  "NU",  "NZ",  "OM",  "PA",  "PE",  "PF",  "PG",
    "PH",  "PK",  "PL",  "PM",  "PN",  "PR",  "PS",  "PT",
    "PW",  "PY",  "QA",  "RE",  "RO",  "RS",  "RU",  "RW",  "SA",
    "SB",  "SC",  "SD",  "SE",  "SG",  "SH",  "SI",  "SJ",
    "SK",  "SL",  "SM",  "SN",  "SO",  "SR",  "SS",  "ST",  "SV",
    "SX",  "SY",  "SZ",  "TC",  "TD",  "TF",  "TG",  "TH",  "TJ",
    "TK",  "TL",  "TM",  "TN",  "TO",  "TR",  "TT",  "TV",
    "TW",  "TZ",  "UA",  "UG",  "UM",  "US",  "UY",  "UZ",
    "VA",  "VC",  "VE",  "VG",  "VI",  "VN",  "VU",  "WF",
    "WS",  "YE",  "YT",  "ZA",  "ZM",  "ZW",
NULL,
    "AN",  "BU", "CS", "FX", "RO", "SU", "TP", "YD", "YU", "ZR",   /* obsolete country codes */
NULL
};

static const char* const DEPRECATED_COUNTRIES[] = {
    "AN", "BU", "CS", "DD", "DY", "FX", "HV", "NH", "RH", "SU", "TP", "UK", "VD", "YD", "YU", "ZR", NULL, NULL /* deprecated country list */
};
static const char* const REPLACEMENT_COUNTRIES[] = {
/*  "AN", "BU", "CS", "DD", "DY", "FX", "HV", "NH", "RH", "SU", "TP", "UK", "VD", "YD", "YU", "ZR" */
    "CW", "MM", "RS", "DE", "BJ", "FR", "BF", "VU", "ZW", "RU", "TL", "GB", "VN", "YE", "RS", "CD", NULL, NULL  /* replacement country codes */      
};
    
/**
 * Table of 3-letter country codes.
 *
 * This is a lookup table used to convert 3-letter country codes to
 * their 2-letter equivalent.  It must be kept in sync with COUNTRIES.
 * For all valid i, COUNTRIES[i] must refer to the same country as
 * COUNTRIES_3[i].  The commented-out lines are copied from COUNTRIES
 * to make eyeballing this baby easier.
 *
 * This table should be terminated with a NULL entry, followed by a
 * second list, and another NULL entry.  The two lists correspond to
 * the two lists in COUNTRIES.
 */
static const char * const COUNTRIES_3[] = {
/*  "AD",  "AE",  "AF",  "AG",  "AI",  "AL",  "AM",      */
    "AND", "ARE", "AFG", "ATG", "AIA", "ALB", "ARM",
/*  "AO",  "AQ",  "AR",  "AS",  "AT",  "AU",  "AW",  "AX",  "AZ",     */
    "AGO", "ATA", "ARG", "ASM", "AUT", "AUS", "ABW", "ALA", "AZE",
/*  "BA",  "BB",  "BD",  "BE",  "BF",  "BG",  "BH",  "BI",     */
    "BIH", "BRB", "BGD", "BEL", "BFA", "BGR", "BHR", "BDI",
/*  "BJ",  "BL",  "BM",  "BN",  "BO",  "BQ",  "BR",  "BS",  "BT",  "BV",     */
    "BEN", "BLM", "BMU", "BRN", "BOL", "BES", "BRA", "BHS", "BTN", "BVT",
/*  "BW",  "BY",  "BZ",  "CA",  "CC",  "CD",  "CF",  "CG",     */
    "BWA", "BLR", "BLZ", "CAN", "CCK", "COD", "CAF", "COG",
/*  "CH",  "CI",  "CK",  "CL",  "CM",  "CN",  "CO",  "CR",     */
    "CHE", "CIV", "COK", "CHL", "CMR", "CHN", "COL", "CRI",
/*  "CU",  "CV",  "CW",  "CX",  "CY",  "CZ",  "DE",  "DJ",  "DK",     */
    "CUB", "CPV", "CUW", "CXR", "CYP", "CZE", "DEU", "DJI", "DNK",
/*  "DM",  "DO",  "DZ",  "EC",  "EE",  "EG",  "EH",  "ER",     */
    "DMA", "DOM", "DZA", "ECU", "EST", "EGY", "ESH", "ERI",
/*  "ES",  "ET",  "FI",  "FJ",  "FK",  "FM",  "FO",  "FR",     */
    "ESP", "ETH", "FIN", "FJI", "FLK", "FSM", "FRO", "FRA",
/*  "GA",  "GB",  "GD",  "GE",  "GF",  "GG",  "GH",  "GI",  "GL",     */
    "GAB", "GBR", "GRD", "GEO", "GUF", "GGY", "GHA", "GIB", "GRL",
/*  "GM",  "GN",  "GP",  "GQ",  "GR",  "GS",  "GT",  "GU",     */
    "GMB", "GIN", "GLP", "GNQ", "GRC", "SGS", "GTM", "GUM",
/*  "GW",  "GY",  "HK",  "HM",  "HN",  "HR",  "HT",  "HU",     */
    "GNB", "GUY", "HKG", "HMD", "HND", "HRV", "HTI", "HUN",
/*  "ID",  "IE",  "IL",  "IM",  "IN",  "IO",  "IQ",  "IR",  "IS" */
    "IDN", "IRL", "ISR", "IMN", "IND", "IOT", "IRQ", "IRN", "ISL",
/*  "IT",  "JE",  "JM",  "JO",  "JP",  "KE",  "KG",  "KH",  "KI",     */
    "ITA", "JEY", "JAM", "JOR", "JPN", "KEN", "KGZ", "KHM", "KIR",
/*  "KM",  "KN",  "KP",  "KR",  "KW",  "KY",  "KZ",  "LA",     */
    "COM", "KNA", "PRK", "KOR", "KWT", "CYM", "KAZ", "LAO",
/*  "LB",  "LC",  "LI",  "LK",  "LR",  "LS",  "LT",  "LU",     */
    "LBN", "LCA", "LIE", "LKA", "LBR", "LSO", "LTU", "LUX",
/*  "LV",  "LY",  "MA",  "MC",  "MD",  "ME",  "MF",  "MG",  "MH",  "MK",     */
    "LVA", "LBY", "MAR", "MCO", "MDA", "MNE", "MAF", "MDG", "MHL", "MKD",
/*  "ML",  "MM",  "MN",  "MO",  "MP",  "MQ",  "MR",  "MS",     */
    "MLI", "MMR", "MNG", "MAC", "MNP", "MTQ", "MRT", "MSR",
/*  "MT",  "MU",  "MV",  "MW",  "MX",  "MY",  "MZ",  "NA",     */
    "MLT", "MUS", "MDV", "MWI", "MEX", "MYS", "MOZ", "NAM",
/*  "NC",  "NE",  "NF",  "NG",  "NI",  "NL",  "NO",  "NP",     */
    "NCL", "NER", "NFK", "NGA", "NIC", "NLD", "NOR", "NPL",
/*  "NR",  "NU",  "NZ",  "OM",  "PA",  "PE",  "PF",  "PG",     */
    "NRU", "NIU", "NZL", "OMN", "PAN", "PER", "PYF", "PNG",
/*  "PH",  "PK",  "PL",  "PM",  "PN",  "PR",  "PS",  "PT",     */
    "PHL", "PAK", "POL", "SPM", "PCN", "PRI", "PSE", "PRT",
/*  "PW",  "PY",  "QA",  "RE",  "RO",  "RS",  "RU",  "RW",  "SA",     */
    "PLW", "PRY", "QAT", "REU", "ROU", "SRB", "RUS", "RWA", "SAU",
/*  "SB",  "SC",  "SD",  "SE",  "SG",  "SH",  "SI",  "SJ",     */
    "SLB", "SYC", "SDN", "SWE", "SGP", "SHN", "SVN", "SJM",
/*  "SK",  "SL",  "SM",  "SN",  "SO",  "SR",  "SS",  "ST",  "SV",     */
    "SVK", "SLE", "SMR", "SEN", "SOM", "SUR", "SSD", "STP", "SLV",
/*  "SX",  "SY",  "SZ",  "TC",  "TD",  "TF",  "TG",  "TH",  "TJ",     */
    "SXM", "SYR", "SWZ", "TCA", "TCD", "ATF", "TGO", "THA", "TJK",
/*  "TK",  "TL",  "TM",  "TN",  "TO",  "TR",  "TT",  "TV",     */
    "TKL", "TLS", "TKM", "TUN", "TON", "TUR", "TTO", "TUV",
/*  "TW",  "TZ",  "UA",  "UG",  "UM",  "US",  "UY",  "UZ",     */
    "TWN", "TZA", "UKR", "UGA", "UMI", "USA", "URY", "UZB",
/*  "VA",  "VC",  "VE",  "VG",  "VI",  "VN",  "VU",  "WF",     */
    "VAT", "VCT", "VEN", "VGB", "VIR", "VNM", "VUT", "WLF",
/*  "WS",  "YE",  "YT",  "ZA",  "ZM",  "ZW",          */
    "WSM", "YEM", "MYT", "ZAF", "ZMB", "ZWE",
NULL,
/*  "AN",  "BU",  "CS",  "FX",  "RO", "SU",  "TP",  "YD",  "YU",  "ZR" */
    "ANT", "BUR", "SCG", "FXX", "ROM", "SUN", "TMP", "YMD", "YUG", "ZAR",
NULL
};

typedef struct CanonicalizationMap {
    const char *id;          /* input ID */
    const char *canonicalID; /* canonicalized output ID */
    const char *keyword;     /* keyword, or NULL if none */
    const char *value;       /* keyword value, or NULL if kw==NULL */
} CanonicalizationMap;

/**
 * A map to canonicalize locale IDs.  This handles a variety of
 * different semantic kinds of transformations.
 */
static const CanonicalizationMap CANONICALIZE_MAP[] = {
    { "",               "en_US_POSIX", NULL, NULL }, /* .NET name */
    { "c",              "en_US_POSIX", NULL, NULL }, /* POSIX name */
    { "posix",          "en_US_POSIX", NULL, NULL }, /* POSIX name (alias of C) */
    { "art_LOJBAN",     "jbo", NULL, NULL }, /* registered name */
    { "az_AZ_CYRL",     "az_Cyrl_AZ", NULL, NULL }, /* .NET name */
    { "az_AZ_LATN",     "az_Latn_AZ", NULL, NULL }, /* .NET name */
    { "ca_ES_PREEURO",  "ca_ES", "currency", "ESP" },
    { "de__PHONEBOOK",  "de", "collation", "phonebook" }, /* Old ICU name */
    { "de_AT_PREEURO",  "de_AT", "currency", "ATS" },
    { "de_DE_PREEURO",  "de_DE", "currency", "DEM" },
    { "de_LU_PREEURO",  "de_LU", "currency", "LUF" },
    { "el_GR_PREEURO",  "el_GR", "currency", "GRD" },
    { "en_BE_PREEURO",  "en_BE", "currency", "BEF" },
    { "en_IE_PREEURO",  "en_IE", "currency", "IEP" },
    { "es__TRADITIONAL", "es", "collation", "traditional" }, /* Old ICU name */
    { "es_ES_PREEURO",  "es_ES", "currency", "ESP" },
    { "eu_ES_PREEURO",  "eu_ES", "currency", "ESP" },
    { "fi_FI_PREEURO",  "fi_FI", "currency", "FIM" },
    { "fr_BE_PREEURO",  "fr_BE", "currency", "BEF" },
    { "fr_FR_PREEURO",  "fr_FR", "currency", "FRF" },
    { "fr_LU_PREEURO",  "fr_LU", "currency", "LUF" },
    { "ga_IE_PREEURO",  "ga_IE", "currency", "IEP" },
    { "gl_ES_PREEURO",  "gl_ES", "currency", "ESP" },
    { "hi__DIRECT",     "hi", "collation", "direct" }, /* Old ICU name */
    { "it_IT_PREEURO",  "it_IT", "currency", "ITL" },
    { "ja_JP_TRADITIONAL", "ja_JP", "calendar", "japanese" }, /* Old ICU name */
    { "nb_NO_NY",       "nn_NO", NULL, NULL },  /* "markus said this was ok" :-) */
    { "nl_BE_PREEURO",  "nl_BE", "currency", "BEF" },
    { "nl_NL_PREEURO",  "nl_NL", "currency", "NLG" },
    { "pt_PT_PREEURO",  "pt_PT", "currency", "PTE" },
    { "sr_SP_CYRL",     "sr_Cyrl_RS", NULL, NULL }, /* .NET name */
    { "sr_SP_LATN",     "sr_Latn_RS", NULL, NULL }, /* .NET name */
    { "sr_YU_CYRILLIC", "sr_Cyrl_RS", NULL, NULL }, /* Linux name */
    { "th_TH_TRADITIONAL", "th_TH", "calendar", "buddhist" }, /* Old ICU name */
    { "uz_UZ_CYRILLIC", "uz_Cyrl_UZ", NULL, NULL }, /* Linux name */
    { "uz_UZ_CYRL",     "uz_Cyrl_UZ", NULL, NULL }, /* .NET name */
    { "uz_UZ_LATN",     "uz_Latn_UZ", NULL, NULL }, /* .NET name */
    { "zh_CHS",         "zh_Hans", NULL, NULL }, /* .NET name */
    { "zh_CHT",         "zh_Hant", NULL, NULL }, /* .NET name */
    { "zh_GAN",         "gan", NULL, NULL }, /* registered name */
    { "zh_GUOYU",       "zh", NULL, NULL }, /* registered name */
    { "zh_HAKKA",       "hak", NULL, NULL }, /* registered name */
    { "zh_MIN_NAN",     "nan", NULL, NULL }, /* registered name */
    { "zh_WUU",         "wuu", NULL, NULL }, /* registered name */
    { "zh_XIANG",       "hsn", NULL, NULL }, /* registered name */
    { "zh_YUE",         "yue", NULL, NULL }, /* registered name */
};

typedef struct VariantMap {
    const char *variant;          /* input ID */
    const char *keyword;     /* keyword, or NULL if none */
    const char *value;       /* keyword value, or NULL if kw==NULL */
} VariantMap;

static const VariantMap VARIANT_MAP[] = {
    { "EURO",   "currency", "EUR" },
    { "PINYIN", "collation", "pinyin" }, /* Solaris variant */
    { "STROKE", "collation", "stroke" }  /* Solaris variant */
};

/* ### BCP47 Conversion *******************************************/
/* Test if the locale id has BCP47 u extension and does not have '@' */
#define _hasBCP47Extension(id) (id && uprv_strstr(id, "@") == NULL && getShortestSubtagLength(localeID) == 1)
/* Converts the BCP47 id to Unicode id. Does nothing to id if conversion fails */
#define _ConvertBCP47(finalID, id, buffer, length,err) \
        if (uloc_forLanguageTag(id, buffer, length, NULL, err) <= 0 || U_FAILURE(*err)) { \
            finalID=id; \
        } else { \
            finalID=buffer; \
        }
/* Gets the size of the shortest subtag in the given localeID. */
static int32_t getShortestSubtagLength(const char *localeID) {
    int32_t localeIDLength = uprv_strlen(localeID);
    int32_t length = localeIDLength;
    int32_t tmpLength = 0;
    int32_t i;
    UBool reset = TRUE;

    for (i = 0; i < localeIDLength; i++) {
        if (localeID[i] != '_' && localeID[i] != '-') {
            if (reset) {
                tmpLength = 0;
                reset = FALSE;
            }
            tmpLength++;
        } else {
            if (tmpLength != 0 && tmpLength < length) {
                length = tmpLength;
            }
            reset = TRUE;
        }
    }

    return length;
}

/* ### Keywords **************************************************/

#define ULOC_KEYWORD_BUFFER_LEN 25
#define ULOC_MAX_NO_KEYWORDS 25

U_CAPI const char * U_EXPORT2
locale_getKeywordsStart(const char *localeID) {
    const char *result = NULL;
    if((result = uprv_strchr(localeID, '@')) != NULL) {
        return result;
    }
#if (U_CHARSET_FAMILY == U_EBCDIC_FAMILY)
    else {
        /* We do this because the @ sign is variant, and the @ sign used on one
        EBCDIC machine won't be compiled the same way on other EBCDIC based
        machines. */
        static const uint8_t ebcdicSigns[] = { 0x7C, 0x44, 0x66, 0x80, 0xAC, 0xAE, 0xAF, 0xB5, 0xEC, 0xEF, 0x00 };
        const uint8_t *charToFind = ebcdicSigns;
        while(*charToFind) {
            if((result = uprv_strchr(localeID, *charToFind)) != NULL) {
                return result;
            }
            charToFind++;
        }
    }
#endif
    return NULL;
}

/**
 * @param buf buffer of size [ULOC_KEYWORD_BUFFER_LEN]
 * @param keywordName incoming name to be canonicalized
 * @param status return status (keyword too long)
 * @return length of the keyword name
 */
static int32_t locale_canonKeywordName(char *buf, const char *keywordName, UErrorCode *status)
{
  int32_t i;
  int32_t keywordNameLen = (int32_t)uprv_strlen(keywordName);
  
  if(keywordNameLen >= ULOC_KEYWORD_BUFFER_LEN) {
    /* keyword name too long for internal buffer */
    *status = U_INTERNAL_PROGRAM_ERROR;
          return 0;
  }
  
  /* normalize the keyword name */
  for(i = 0; i < keywordNameLen; i++) {
    buf[i] = uprv_tolower(keywordName[i]);
  }
  buf[i] = 0;
    
  return keywordNameLen;
}

typedef struct {
    char keyword[ULOC_KEYWORD_BUFFER_LEN];
    int32_t keywordLen;
    const char *valueStart;
    int32_t valueLen;
} KeywordStruct;

static int32_t U_CALLCONV
compareKeywordStructs(const void * /*context*/, const void *left, const void *right) {
    const char* leftString = ((const KeywordStruct *)left)->keyword;
    const char* rightString = ((const KeywordStruct *)right)->keyword;
    return uprv_strcmp(leftString, rightString);
}

/**
 * Both addKeyword and addValue must already be in canonical form.
 * Either both addKeyword and addValue are NULL, or neither is NULL.
 * If they are not NULL they must be zero terminated.
 * If addKeyword is not NULL is must have length small enough to fit in KeywordStruct.keyword.
 */
static int32_t
_getKeywords(const char *localeID,
             char prev,
             char *keywords, int32_t keywordCapacity,
             char *values, int32_t valuesCapacity, int32_t *valLen,
             UBool valuesToo,
             const char* addKeyword,
             const char* addValue,
             UErrorCode *status)
{
    KeywordStruct keywordList[ULOC_MAX_NO_KEYWORDS];
    
    int32_t maxKeywords = ULOC_MAX_NO_KEYWORDS;
    int32_t numKeywords = 0;
    const char* pos = localeID;
    const char* equalSign = NULL;
    const char* semicolon = NULL;
    int32_t i = 0, j, n;
    int32_t keywordsLen = 0;
    int32_t valuesLen = 0;

    if(prev == '@') { /* start of keyword definition */
        /* we will grab pairs, trim spaces, lowercase keywords, sort and return */
        do {
            UBool duplicate = FALSE;
            /* skip leading spaces */
            while(*pos == ' ') {
                pos++;
            }
            if (!*pos) { /* handle trailing "; " */
                break;
            }
            if(numKeywords == maxKeywords) {
                *status = U_INTERNAL_PROGRAM_ERROR;
                return 0;
            }
            equalSign = uprv_strchr(pos, '=');
            semicolon = uprv_strchr(pos, ';');
            /* lack of '=' [foo@currency] is illegal */
            /* ';' before '=' [foo@currency;collation=pinyin] is illegal */
            if(!equalSign || (semicolon && semicolon<equalSign)) {
                *status = U_INVALID_FORMAT_ERROR;
                return 0;
            }
            /* need to normalize both keyword and keyword name */
            if(equalSign - pos >= ULOC_KEYWORD_BUFFER_LEN) {
                /* keyword name too long for internal buffer */
                *status = U_INTERNAL_PROGRAM_ERROR;
                return 0;
            }
            for(i = 0, n = 0; i < equalSign - pos; ++i) {
                if (pos[i] != ' ') {
                    keywordList[numKeywords].keyword[n++] = uprv_tolower(pos[i]);
                }
            }
            keywordList[numKeywords].keyword[n] = 0;
            keywordList[numKeywords].keywordLen = n;
            /* now grab the value part. First we skip the '=' */
            equalSign++;
            /* then we leading spaces */
            while(*equalSign == ' ') {
                equalSign++;
            }
            keywordList[numKeywords].valueStart = equalSign;
            
            pos = semicolon;
            i = 0;
            if(pos) {
                while(*(pos - i - 1) == ' ') {
                    i++;
                }
                keywordList[numKeywords].valueLen = (int32_t)(pos - equalSign - i);
                pos++;
            } else {
                i = (int32_t)uprv_strlen(equalSign);
                while(i && equalSign[i-1] == ' ') {
                    i--;
                }
                keywordList[numKeywords].valueLen = i;
            }
            /* If this is a duplicate keyword, then ignore it */
            for (j=0; j<numKeywords; ++j) {
                if (uprv_strcmp(keywordList[j].keyword, keywordList[numKeywords].keyword) == 0) {
                    duplicate = TRUE;
                    break;
                }
            }
            if (!duplicate) {
                ++numKeywords;
            }
        } while(pos);

        /* Handle addKeyword/addValue. */
        if (addKeyword != NULL) {
            UBool duplicate = FALSE;
            U_ASSERT(addValue != NULL);
            /* Search for duplicate; if found, do nothing. Explicit keyword
               overrides addKeyword. */
            for (j=0; j<numKeywords; ++j) {
                if (uprv_strcmp(keywordList[j].keyword, addKeyword) == 0) {
                    duplicate = TRUE;
                    break;
                }
            }
            if (!duplicate) {
                if (numKeywords == maxKeywords) {
                    *status = U_INTERNAL_PROGRAM_ERROR;
                    return 0;
                }
                uprv_strcpy(keywordList[numKeywords].keyword, addKeyword);
                keywordList[numKeywords].keywordLen = (int32_t)uprv_strlen(addKeyword);
                keywordList[numKeywords].valueStart = addValue;
                keywordList[numKeywords].valueLen = (int32_t)uprv_strlen(addValue);
                ++numKeywords;
            }
        } else {
            U_ASSERT(addValue == NULL);
        }

        /* now we have a list of keywords */
        /* we need to sort it */
        uprv_sortArray(keywordList, numKeywords, sizeof(KeywordStruct), compareKeywordStructs, NULL, FALSE, status);
        
        /* Now construct the keyword part */
        for(i = 0; i < numKeywords; i++) {
            if(keywordsLen + keywordList[i].keywordLen + 1< keywordCapacity) {
                uprv_strcpy(keywords+keywordsLen, keywordList[i].keyword);
                if(valuesToo) {
                    keywords[keywordsLen + keywordList[i].keywordLen] = '=';
                } else {
                    keywords[keywordsLen + keywordList[i].keywordLen] = 0;
                }
            }
            keywordsLen += keywordList[i].keywordLen + 1;
            if(valuesToo) {
                if(keywordsLen + keywordList[i].valueLen < keywordCapacity) {
                    uprv_strncpy(keywords+keywordsLen, keywordList[i].valueStart, keywordList[i].valueLen);
                }
                keywordsLen += keywordList[i].valueLen;
                
                if(i < numKeywords - 1) {
                    if(keywordsLen < keywordCapacity) {       
                        keywords[keywordsLen] = ';';
                    }
                    keywordsLen++;
                }
            }
            if(values) {
                if(valuesLen + keywordList[i].valueLen + 1< valuesCapacity) {
                    uprv_strcpy(values+valuesLen, keywordList[i].valueStart);
                    values[valuesLen + keywordList[i].valueLen] = 0;
                }
                valuesLen += keywordList[i].valueLen + 1;
            }
        }
        if(values) {
            values[valuesLen] = 0;
            if(valLen) {
                *valLen = valuesLen;
            }
        }
        return u_terminateChars(keywords, keywordCapacity, keywordsLen, status);   
    } else {
        return 0;
    }
}

U_CFUNC int32_t
locale_getKeywords(const char *localeID,
                   char prev,
                   char *keywords, int32_t keywordCapacity,
                   char *values, int32_t valuesCapacity, int32_t *valLen,
                   UBool valuesToo,
                   UErrorCode *status) {
    return _getKeywords(localeID, prev, keywords, keywordCapacity,
                        values, valuesCapacity, valLen, valuesToo,
                        NULL, NULL, status);
}

U_CAPI int32_t U_EXPORT2
uloc_getKeywordValue(const char* localeID,
                     const char* keywordName,
                     char* buffer, int32_t bufferCapacity,
                     UErrorCode* status)
{ 
    const char* startSearchHere = NULL;
    const char* nextSeparator = NULL;
    char keywordNameBuffer[ULOC_KEYWORD_BUFFER_LEN];
    char localeKeywordNameBuffer[ULOC_KEYWORD_BUFFER_LEN];
    int32_t i = 0;
    int32_t result = 0;

    if(status && U_SUCCESS(*status) && localeID) {
      char tempBuffer[ULOC_FULLNAME_CAPACITY];
      const char* tmpLocaleID;

      if (_hasBCP47Extension(localeID)) {
          _ConvertBCP47(tmpLocaleID, localeID, tempBuffer, sizeof(tempBuffer), status);
      } else {
          tmpLocaleID=localeID;
      }
    
      startSearchHere = uprv_strchr(tmpLocaleID, '@'); /* TODO: REVISIT: shouldn't this be locale_getKeywordsStart ? */
      if(startSearchHere == NULL) {
          /* no keywords, return at once */
          return 0;
      }

      locale_canonKeywordName(keywordNameBuffer, keywordName, status);
      if(U_FAILURE(*status)) {
        return 0;
      }
    
      /* find the first keyword */
      while(startSearchHere) {
          startSearchHere++;
          /* skip leading spaces (allowed?) */
          while(*startSearchHere == ' ') {
              startSearchHere++;
          }
          nextSeparator = uprv_strchr(startSearchHere, '=');
          /* need to normalize both keyword and keyword name */
          if(!nextSeparator) {
              break;
          }
          if(nextSeparator - startSearchHere >= ULOC_KEYWORD_BUFFER_LEN) {
              /* keyword name too long for internal buffer */
              *status = U_INTERNAL_PROGRAM_ERROR;
              return 0;
          }
          for(i = 0; i < nextSeparator - startSearchHere; i++) {
              localeKeywordNameBuffer[i] = uprv_tolower(startSearchHere[i]);
          }
          /* trim trailing spaces */
          while(startSearchHere[i-1] == ' ') {
              i--;
              U_ASSERT(i>=0);
          }
          localeKeywordNameBuffer[i] = 0;
        
          startSearchHere = uprv_strchr(nextSeparator, ';');
        
          if(uprv_strcmp(keywordNameBuffer, localeKeywordNameBuffer) == 0) {
              nextSeparator++;
              while(*nextSeparator == ' ') {
                  nextSeparator++;
              }
              /* we actually found the keyword. Copy the value */
              if(startSearchHere && startSearchHere - nextSeparator < bufferCapacity) {
                  while(*(startSearchHere-1) == ' ') {
                      startSearchHere--;
                  }
                  uprv_strncpy(buffer, nextSeparator, startSearchHere - nextSeparator);
                  result = u_terminateChars(buffer, bufferCapacity, (int32_t)(startSearchHere - nextSeparator), status);
              } else if(!startSearchHere && (int32_t)uprv_strlen(nextSeparator) < bufferCapacity) { /* last item in string */
                  i = (int32_t)uprv_strlen(nextSeparator);
                  while(nextSeparator[i - 1] == ' ') {
                      i--;
                  }
                  uprv_strncpy(buffer, nextSeparator, i);
                  result = u_terminateChars(buffer, bufferCapacity, i, status);
              } else {
                  /* give a bigger buffer, please */
                  *status = U_BUFFER_OVERFLOW_ERROR;
                  if(startSearchHere) {
                      result = (int32_t)(startSearchHere - nextSeparator);
                  } else {
                      result = (int32_t)uprv_strlen(nextSeparator); 
                  }
              }
              return result;
          }
      }
    }
    return 0;
}

U_CAPI int32_t U_EXPORT2
uloc_setKeywordValue(const char* keywordName,
                     const char* keywordValue,
                     char* buffer, int32_t bufferCapacity,
                     UErrorCode* status)
{
    /* TODO: sorting. removal. */
    int32_t keywordNameLen;
    int32_t keywordValueLen;
    int32_t bufLen;
    int32_t needLen = 0;
    int32_t foundValueLen;
    int32_t keywordAtEnd = 0; /* is the keyword at the end of the string? */
    char keywordNameBuffer[ULOC_KEYWORD_BUFFER_LEN];
    char localeKeywordNameBuffer[ULOC_KEYWORD_BUFFER_LEN];
    int32_t i = 0;
    int32_t rc;
    char* nextSeparator = NULL;
    char* nextEqualsign = NULL;
    char* startSearchHere = NULL;
    char* keywordStart = NULL;
    char *insertHere = NULL;
    if(U_FAILURE(*status)) { 
        return -1; 
    }
    if(bufferCapacity>1) {
        bufLen = (int32_t)uprv_strlen(buffer);
    } else {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    if(bufferCapacity<bufLen) {
        /* The capacity is less than the length?! Is this NULL terminated? */
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    if(keywordValue && !*keywordValue) { 
        keywordValue = NULL;
    }
    if(keywordValue) {
        keywordValueLen = (int32_t)uprv_strlen(keywordValue);
    } else { 
        keywordValueLen = 0;
    }
    keywordNameLen = locale_canonKeywordName(keywordNameBuffer, keywordName, status);
    if(U_FAILURE(*status)) {
        return 0;
    }
    startSearchHere = (char*)locale_getKeywordsStart(buffer);
    if(startSearchHere == NULL || (startSearchHere[1]==0)) {
        if(!keywordValue) { /* no keywords = nothing to remove */
            return bufLen; 
        }

        needLen = bufLen+1+keywordNameLen+1+keywordValueLen;
        if(startSearchHere) { /* had a single @ */ 
            needLen--; /* already had the @ */
            /* startSearchHere points at the @ */
        } else {
            startSearchHere=buffer+bufLen;
        }
        if(needLen >= bufferCapacity) {
            *status = U_BUFFER_OVERFLOW_ERROR;
            return needLen; /* no change */
        }
        *startSearchHere = '@';
        startSearchHere++;
        uprv_strcpy(startSearchHere, keywordNameBuffer);
        startSearchHere += keywordNameLen;
        *startSearchHere = '=';
        startSearchHere++;
        uprv_strcpy(startSearchHere, keywordValue);
        startSearchHere+=keywordValueLen;
        return needLen;
    } /* end shortcut - no @ */
    
    keywordStart = startSearchHere;
    /* search for keyword */
    while(keywordStart) {
        keywordStart++;
        /* skip leading spaces (allowed?) */
        while(*keywordStart == ' ') {
            keywordStart++;
        }
        nextEqualsign = uprv_strchr(keywordStart, '=');
        /* need to normalize both keyword and keyword name */
        if(!nextEqualsign) {
            break;
        }
        if(nextEqualsign - keywordStart >= ULOC_KEYWORD_BUFFER_LEN) {
            /* keyword name too long for internal buffer */
            *status = U_INTERNAL_PROGRAM_ERROR;
            return 0;
        }
        for(i = 0; i < nextEqualsign - keywordStart; i++) {
            localeKeywordNameBuffer[i] = uprv_tolower(keywordStart[i]);
        }
        /* trim trailing spaces */
        while(keywordStart[i-1] == ' ') {
            i--;
        }
        U_ASSERT(i>=0 && i<ULOC_KEYWORD_BUFFER_LEN);
        localeKeywordNameBuffer[i] = 0;

        nextSeparator = uprv_strchr(nextEqualsign, ';');
        rc = uprv_strcmp(keywordNameBuffer, localeKeywordNameBuffer);
        if(rc == 0) {
            nextEqualsign++;
            while(*nextEqualsign == ' ') {
                nextEqualsign++;
            }
            /* we actually found the keyword. Change the value */
            if (nextSeparator) {
                keywordAtEnd = 0;
                foundValueLen = (int32_t)(nextSeparator - nextEqualsign);
            } else {
                keywordAtEnd = 1;
                foundValueLen = (int32_t)uprv_strlen(nextEqualsign);
            }
            if(keywordValue) { /* adding a value - not removing */
              if(foundValueLen == keywordValueLen) {
                uprv_strncpy(nextEqualsign, keywordValue, keywordValueLen);
                return bufLen; /* no change in size */
              } else if(foundValueLen > keywordValueLen) {
                int32_t delta = foundValueLen - keywordValueLen;
                if(nextSeparator) { /* RH side */
                  uprv_memmove(nextSeparator - delta, nextSeparator, bufLen-(nextSeparator-buffer));
                }
                uprv_strncpy(nextEqualsign, keywordValue, keywordValueLen);
                bufLen -= delta;
                buffer[bufLen]=0;
                return bufLen;
              } else { /* FVL < KVL */
                int32_t delta = keywordValueLen - foundValueLen;
                if((bufLen+delta) >= bufferCapacity) {
                  *status = U_BUFFER_OVERFLOW_ERROR;
                  return bufLen+delta;
                }
                if(nextSeparator) { /* RH side */
                  uprv_memmove(nextSeparator+delta,nextSeparator, bufLen-(nextSeparator-buffer));
                }
                uprv_strncpy(nextEqualsign, keywordValue, keywordValueLen);
                bufLen += delta;
                buffer[bufLen]=0;
                return bufLen;
              }
            } else { /* removing a keyword */
              if(keywordAtEnd) {
                /* zero out the ';' or '@' just before startSearchhere */
                keywordStart[-1] = 0;
                return (int32_t)((keywordStart-buffer)-1); /* (string length without keyword) minus separator */
              } else {
                uprv_memmove(keywordStart, nextSeparator+1, bufLen-((nextSeparator+1)-buffer));
                keywordStart[bufLen-((nextSeparator+1)-buffer)]=0;
                return (int32_t)(bufLen-((nextSeparator+1)-keywordStart));
              }
            }
        } else if(rc<0){ /* end match keyword */
          /* could insert at this location. */
          insertHere = keywordStart;
        }
        keywordStart = nextSeparator;
    } /* end loop searching */
    
    if(!keywordValue) {
      return bufLen; /* removal of non-extant keyword - no change */
    }

    /* we know there is at least one keyword. */
    needLen = bufLen+1+keywordNameLen+1+keywordValueLen;
    if(needLen >= bufferCapacity) {
        *status = U_BUFFER_OVERFLOW_ERROR;
        return needLen; /* no change */
    }
    
    if(insertHere) {
      uprv_memmove(insertHere+(1+keywordNameLen+1+keywordValueLen), insertHere, bufLen-(insertHere-buffer));
      keywordStart = insertHere;
    } else {
      keywordStart = buffer+bufLen;
      *keywordStart = ';';
      keywordStart++;
    }
    uprv_strncpy(keywordStart, keywordNameBuffer, keywordNameLen);
    keywordStart += keywordNameLen;
    *keywordStart = '=';
    keywordStart++;
    uprv_strncpy(keywordStart, keywordValue, keywordValueLen); /* terminates. */
    keywordStart+=keywordValueLen;
    if(insertHere) {
      *keywordStart = ';';
      keywordStart++;
    }
    buffer[needLen]=0;
    return needLen;
}

/* ### ID parsing implementation **************************************************/

#define _isPrefixLetter(a) ((a=='x')||(a=='X')||(a=='i')||(a=='I'))

/*returns TRUE if one of the special prefixes is here (s=string)
  'x-' or 'i-' */
#define _isIDPrefix(s) (_isPrefixLetter(s[0])&&_isIDSeparator(s[1]))

/* Dot terminates it because of POSIX form  where dot precedes the codepage
 * except for variant
 */
#define _isTerminator(a)  ((a==0)||(a=='.')||(a=='@'))

static char* _strnchr(const char* str, int32_t len, char c) {
    U_ASSERT(str != 0 && len >= 0);
    while (len-- != 0) {
        char d = *str;
        if (d == c) {
            return (char*) str;
        } else if (d == 0) {
            break;
        }
        ++str;
    }
    return NULL;
}

/**
 * Lookup 'key' in the array 'list'.  The array 'list' should contain
 * a NULL entry, followed by more entries, and a second NULL entry.
 *
 * The 'list' param should be LANGUAGES, LANGUAGES_3, COUNTRIES, or
 * COUNTRIES_3.
 */
static int16_t _findIndex(const char* const* list, const char* key)
{
    const char* const* anchor = list;
    int32_t pass = 0;

    /* Make two passes through two NULL-terminated arrays at 'list' */
    while (pass++ < 2) {
        while (*list) {
            if (uprv_strcmp(key, *list) == 0) {
                return (int16_t)(list - anchor);
            }
            list++;
        }
        ++list;     /* skip final NULL *CWB*/
    }
    return -1;
}

/* count the length of src while copying it to dest; return strlen(src) */
static inline int32_t
_copyCount(char *dest, int32_t destCapacity, const char *src) {
    const char *anchor;
    char c;

    anchor=src;
    for(;;) {
        if((c=*src)==0) {
            return (int32_t)(src-anchor);
        }
        if(destCapacity<=0) {
            return (int32_t)((src-anchor)+uprv_strlen(src));
        }
        ++src;
        *dest++=c;
        --destCapacity;
    }
}

U_CFUNC const char* 
uloc_getCurrentCountryID(const char* oldID){
    int32_t offset = _findIndex(DEPRECATED_COUNTRIES, oldID);
    if (offset >= 0) {
        return REPLACEMENT_COUNTRIES[offset];
    }
    return oldID;
}
U_CFUNC const char* 
uloc_getCurrentLanguageID(const char* oldID){
    int32_t offset = _findIndex(DEPRECATED_LANGUAGES, oldID);
    if (offset >= 0) {
        return REPLACEMENT_LANGUAGES[offset];
    }
    return oldID;        
}
/*
 * the internal functions _getLanguage(), _getCountry(), _getVariant()
 * avoid duplicating code to handle the earlier locale ID pieces
 * in the functions for the later ones by
 * setting the *pEnd pointer to where they stopped parsing
 *
 * TODO try to use this in Locale
 */
U_CFUNC int32_t
ulocimp_getLanguage(const char *localeID,
                    char *language, int32_t languageCapacity,
                    const char **pEnd) {
    int32_t i=0;
    int32_t offset;
    char lang[4]={ 0, 0, 0, 0 }; /* temporary buffer to hold language code for searching */

    /* if it starts with i- or x- then copy that prefix */
    if(_isIDPrefix(localeID)) {
        if(i<languageCapacity) {
            language[i]=(char)uprv_tolower(*localeID);
        }
        if(i<languageCapacity) {
            language[i+1]='-';
        }
        i+=2;
        localeID+=2;
    }
    
    /* copy the language as far as possible and count its length */
    while(!_isTerminator(*localeID) && !_isIDSeparator(*localeID)) {
        if(i<languageCapacity) {
            language[i]=(char)uprv_tolower(*localeID);
        }
        if(i<3) {
            U_ASSERT(i>=0);
            lang[i]=(char)uprv_tolower(*localeID);
        }
        i++;
        localeID++;
    }

    if(i==3) {
        /* convert 3 character code to 2 character code if possible *CWB*/
        offset=_findIndex(LANGUAGES_3, lang);
        if(offset>=0) {
            i=_copyCount(language, languageCapacity, LANGUAGES[offset]);
        }
    }

    if(pEnd!=NULL) {
        *pEnd=localeID;
    }
    return i;
}

U_CFUNC int32_t
ulocimp_getScript(const char *localeID,
                  char *script, int32_t scriptCapacity,
                  const char **pEnd)
{
    int32_t idLen = 0;

    if (pEnd != NULL) {
        *pEnd = localeID;
    }

    /* copy the second item as far as possible and count its length */
    while(!_isTerminator(localeID[idLen]) && !_isIDSeparator(localeID[idLen])
            && uprv_isASCIILetter(localeID[idLen])) {
        idLen++;
    }

    /* If it's exactly 4 characters long, then it's a script and not a country. */
    if (idLen == 4) {
        int32_t i;
        if (pEnd != NULL) {
            *pEnd = localeID+idLen;
        }
        if(idLen > scriptCapacity) {
            idLen = scriptCapacity;
        }
        if (idLen >= 1) {
            script[0]=(char)uprv_toupper(*(localeID++));
        }
        for (i = 1; i < idLen; i++) {
            script[i]=(char)uprv_tolower(*(localeID++));
        }
    }
    else {
        idLen = 0;
    }
    return idLen;
}

U_CFUNC int32_t
ulocimp_getCountry(const char *localeID,
                   char *country, int32_t countryCapacity,
                   const char **pEnd)
{
    int32_t idLen=0;
    char cnty[ULOC_COUNTRY_CAPACITY]={ 0, 0, 0, 0 };
    int32_t offset;

    /* copy the country as far as possible and count its length */
    while(!_isTerminator(localeID[idLen]) && !_isIDSeparator(localeID[idLen])) {
        if(idLen<(ULOC_COUNTRY_CAPACITY-1)) {   /*CWB*/
            cnty[idLen]=(char)uprv_toupper(localeID[idLen]);
        }
        idLen++;
    }

    /* the country should be either length 2 or 3 */
    if (idLen == 2 || idLen == 3) {
        UBool gotCountry = FALSE;
        /* convert 3 character code to 2 character code if possible *CWB*/
        if(idLen==3) {
            offset=_findIndex(COUNTRIES_3, cnty);
            if(offset>=0) {
                idLen=_copyCount(country, countryCapacity, COUNTRIES[offset]);
                gotCountry = TRUE;
            }
        }
        if (!gotCountry) {
            int32_t i = 0;
            for (i = 0; i < idLen; i++) {
                if (i < countryCapacity) {
                    country[i]=(char)uprv_toupper(localeID[i]);
                }
            }
        }
        localeID+=idLen;
    } else {
        idLen = 0;
    }

    if(pEnd!=NULL) {
        *pEnd=localeID;
    }

    return idLen;
}

/**
 * @param needSeparator if true, then add leading '_' if any variants
 * are added to 'variant'
 */
static int32_t
_getVariantEx(const char *localeID,
              char prev,
              char *variant, int32_t variantCapacity,
              UBool needSeparator) {
    int32_t i=0;

    /* get one or more variant tags and separate them with '_' */
    if(_isIDSeparator(prev)) {
        /* get a variant string after a '-' or '_' */
        while(!_isTerminator(*localeID)) {
            if (needSeparator) {
                if (i<variantCapacity) {
                    variant[i] = '_';
                }
                ++i;
                needSeparator = FALSE;
            }
            if(i<variantCapacity) {
                variant[i]=(char)uprv_toupper(*localeID);
                if(variant[i]=='-') {
                    variant[i]='_';
                }
            }
            i++;
            localeID++;
        }
    }

    /* if there is no variant tag after a '-' or '_' then look for '@' */
    if(i==0) {
        if(prev=='@') {
            /* keep localeID */
        } else if((localeID=locale_getKeywordsStart(localeID))!=NULL) {
            ++localeID; /* point after the '@' */
        } else {
            return 0;
        }
        while(!_isTerminator(*localeID)) {
            if (needSeparator) {
                if (i<variantCapacity) {
                    variant[i] = '_';
                }
                ++i;
                needSeparator = FALSE;
            }
            if(i<variantCapacity) {
                variant[i]=(char)uprv_toupper(*localeID);
                if(variant[i]=='-' || variant[i]==',') {
                    variant[i]='_';
                }
            }
            i++;
            localeID++;
        }
    }
    
    return i;
}

static int32_t
_getVariant(const char *localeID,
            char prev,
            char *variant, int32_t variantCapacity) {
    return _getVariantEx(localeID, prev, variant, variantCapacity, FALSE);
}

/**
 * Delete ALL instances of a variant from the given list of one or
 * more variants.  Example: "FOO_EURO_BAR_EURO" => "FOO_BAR".
 * @param variants the source string of one or more variants,
 * separated by '_'.  This will be MODIFIED IN PLACE.  Not zero
 * terminated; if it is, trailing zero will NOT be maintained.
 * @param variantsLen length of variants
 * @param toDelete variant to delete, without separators, e.g.  "EURO"
 * or "PREEURO"; not zero terminated
 * @param toDeleteLen length of toDelete
 * @return number of characters deleted from variants
 */
static int32_t
_deleteVariant(char* variants, int32_t variantsLen,
               const char* toDelete, int32_t toDeleteLen)
{
    int32_t delta = 0; /* number of chars deleted */
    for (;;) {
        UBool flag = FALSE;
        if (variantsLen < toDeleteLen) {
            return delta;
        }
        if (uprv_strncmp(variants, toDelete, toDeleteLen) == 0 &&
            (variantsLen == toDeleteLen ||
             (flag=(variants[toDeleteLen] == '_'))))
        {
            int32_t d = toDeleteLen + (flag?1:0);
            variantsLen -= d;
            delta += d;
            if (variantsLen > 0) {
                uprv_memmove(variants, variants+d, variantsLen);
            }
        } else {
            char* p = _strnchr(variants, variantsLen, '_');
            if (p == NULL) {
                return delta;
            }
            ++p;
            variantsLen -= (int32_t)(p - variants);
            variants = p;
        }
    }
}

/* Keyword enumeration */

typedef struct UKeywordsContext {
    char* keywords;
    char* current;
} UKeywordsContext;

static void U_CALLCONV
uloc_kw_closeKeywords(UEnumeration *enumerator) {
    uprv_free(((UKeywordsContext *)enumerator->context)->keywords);
    uprv_free(enumerator->context);
    uprv_free(enumerator);
}

static int32_t U_CALLCONV
uloc_kw_countKeywords(UEnumeration *en, UErrorCode * /*status*/) {
    char *kw = ((UKeywordsContext *)en->context)->keywords;
    int32_t result = 0;
    while(*kw) {
        result++;
        kw += uprv_strlen(kw)+1;
    }
    return result;
}

static const char* U_CALLCONV 
uloc_kw_nextKeyword(UEnumeration* en,
                    int32_t* resultLength,
                    UErrorCode* /*status*/) {
    const char* result = ((UKeywordsContext *)en->context)->current;
    int32_t len = 0;
    if(*result) {
        len = (int32_t)uprv_strlen(((UKeywordsContext *)en->context)->current);
        ((UKeywordsContext *)en->context)->current += len+1;
    } else {
        result = NULL;
    }
    if (resultLength) {
        *resultLength = len;
    }
    return result;
}

static void U_CALLCONV 
uloc_kw_resetKeywords(UEnumeration* en, 
                      UErrorCode* /*status*/) {
    ((UKeywordsContext *)en->context)->current = ((UKeywordsContext *)en->context)->keywords;
}

static const UEnumeration gKeywordsEnum = {
    NULL,
    NULL,
    uloc_kw_closeKeywords,
    uloc_kw_countKeywords,
    uenum_unextDefault,
    uloc_kw_nextKeyword,
    uloc_kw_resetKeywords
};

U_CAPI UEnumeration* U_EXPORT2
uloc_openKeywordList(const char *keywordList, int32_t keywordListSize, UErrorCode* status)
{
    UKeywordsContext *myContext = NULL;
    UEnumeration *result = NULL;

    if(U_FAILURE(*status)) {
        return NULL;
    }
    result = (UEnumeration *)uprv_malloc(sizeof(UEnumeration));
    /* Null pointer test */
    if (result == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    uprv_memcpy(result, &gKeywordsEnum, sizeof(UEnumeration));
    myContext = static_cast<UKeywordsContext *>(uprv_malloc(sizeof(UKeywordsContext)));
    if (myContext == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        uprv_free(result);
        return NULL;
    }
    myContext->keywords = (char *)uprv_malloc(keywordListSize+1);
    uprv_memcpy(myContext->keywords, keywordList, keywordListSize);
    myContext->keywords[keywordListSize] = 0;
    myContext->current = myContext->keywords;
    result->context = myContext;
    return result;
}

U_CAPI UEnumeration* U_EXPORT2
uloc_openKeywords(const char* localeID,
                        UErrorCode* status) 
{
    int32_t i=0;
    char keywords[256];
    int32_t keywordsCapacity = 256;
    char tempBuffer[ULOC_FULLNAME_CAPACITY];
    const char* tmpLocaleID;

    if(status==NULL || U_FAILURE(*status)) {
        return 0;
    }
    
    if (_hasBCP47Extension(localeID)) {
        _ConvertBCP47(tmpLocaleID, localeID, tempBuffer, sizeof(tempBuffer), status);
    } else {
        if (localeID==NULL) {
           localeID=uloc_getDefault();
        }
        tmpLocaleID=localeID;
    }

    /* Skip the language */
    ulocimp_getLanguage(tmpLocaleID, NULL, 0, &tmpLocaleID);
    if(_isIDSeparator(*tmpLocaleID)) {
        const char *scriptID;
        /* Skip the script if available */
        ulocimp_getScript(tmpLocaleID+1, NULL, 0, &scriptID);
        if(scriptID != tmpLocaleID+1) {
            /* Found optional script */
            tmpLocaleID = scriptID;
        }
        /* Skip the Country */
        if (_isIDSeparator(*tmpLocaleID)) {
            ulocimp_getCountry(tmpLocaleID+1, NULL, 0, &tmpLocaleID);
            if(_isIDSeparator(*tmpLocaleID)) {
                _getVariant(tmpLocaleID+1, *tmpLocaleID, NULL, 0);
            }
        }
    }

    /* keywords are located after '@' */
    if((tmpLocaleID = locale_getKeywordsStart(tmpLocaleID)) != NULL) {
        i=locale_getKeywords(tmpLocaleID+1, '@', keywords, keywordsCapacity, NULL, 0, NULL, FALSE, status);
    }

    if(i) {
        return uloc_openKeywordList(keywords, i, status);
    } else {
        return NULL;
    }
}


/* bit-flags for 'options' parameter of _canonicalize */
#define _ULOC_STRIP_KEYWORDS 0x2
#define _ULOC_CANONICALIZE   0x1

#define OPTION_SET(options, mask) ((options & mask) != 0)

static const char i_default[] = {'i', '-', 'd', 'e', 'f', 'a', 'u', 'l', 't'};
#define I_DEFAULT_LENGTH (sizeof i_default / sizeof i_default[0])

/**
 * Canonicalize the given localeID, to level 1 or to level 2,
 * depending on the options.  To specify level 1, pass in options=0.
 * To specify level 2, pass in options=_ULOC_CANONICALIZE.
 *
 * This is the code underlying uloc_getName and uloc_canonicalize.
 */
static int32_t
_canonicalize(const char* localeID,
              char* result,
              int32_t resultCapacity,
              uint32_t options,
              UErrorCode* err) {
    int32_t j, len, fieldCount=0, scriptSize=0, variantSize=0, nameCapacity;
    char localeBuffer[ULOC_FULLNAME_CAPACITY];
    char tempBuffer[ULOC_FULLNAME_CAPACITY];
    const char* origLocaleID;
    const char* tmpLocaleID;
    const char* keywordAssign = NULL;
    const char* separatorIndicator = NULL;
    const char* addKeyword = NULL;
    const char* addValue = NULL;
    char* name;
    char* variant = NULL; /* pointer into name, or NULL */

    if (U_FAILURE(*err)) {
        return 0;
    }
    
    if (_hasBCP47Extension(localeID)) {
        _ConvertBCP47(tmpLocaleID, localeID, tempBuffer, sizeof(tempBuffer), err);
    } else {
        if (localeID==NULL) {
           localeID=uloc_getDefault();
        }
        tmpLocaleID=localeID;
    }

    origLocaleID=tmpLocaleID;

    /* if we are doing a full canonicalization, then put results in
       localeBuffer, if necessary; otherwise send them to result. */
    if (/*OPTION_SET(options, _ULOC_CANONICALIZE) &&*/
        (result == NULL || resultCapacity < (int32_t)sizeof(localeBuffer))) {
        name = localeBuffer;
        nameCapacity = (int32_t)sizeof(localeBuffer);
    } else {
        name = result;
        nameCapacity = resultCapacity;
    }

    /* get all pieces, one after another, and separate with '_' */
    len=ulocimp_getLanguage(tmpLocaleID, name, nameCapacity, &tmpLocaleID);

    if(len == I_DEFAULT_LENGTH && uprv_strncmp(origLocaleID, i_default, len) == 0) {
        const char *d = uloc_getDefault();
        
        len = (int32_t)uprv_strlen(d);

        if (name != NULL) {
            uprv_strncpy(name, d, len);
        }
    } else if(_isIDSeparator(*tmpLocaleID)) {
        const char *scriptID;

        ++fieldCount;
        if(len<nameCapacity) {
            name[len]='_';
        }
        ++len;

        scriptSize=ulocimp_getScript(tmpLocaleID+1,
            (len<nameCapacity ? name+len : NULL), nameCapacity-len, &scriptID);
        if(scriptSize > 0) {
            /* Found optional script */
            tmpLocaleID = scriptID;
            ++fieldCount;
            len+=scriptSize;
            if (_isIDSeparator(*tmpLocaleID)) {
                /* If there is something else, then we add the _ */
                if(len<nameCapacity) {
                    name[len]='_';
                }
                ++len;
            }
        }

        if (_isIDSeparator(*tmpLocaleID)) {
            const char *cntryID;
            int32_t cntrySize = ulocimp_getCountry(tmpLocaleID+1,
                (len<nameCapacity ? name+len : NULL), nameCapacity-len, &cntryID);
            if (cntrySize > 0) {
                /* Found optional country */
                tmpLocaleID = cntryID;
                len+=cntrySize;
            }
            if(_isIDSeparator(*tmpLocaleID)) {
                /* If there is something else, then we add the _  if we found country before. */
                if (cntrySize >= 0 && ! _isIDSeparator(*(tmpLocaleID+1)) ) {
                    ++fieldCount;
                    if(len<nameCapacity) {
                        name[len]='_';
                    }
                    ++len;
                }

                variantSize = _getVariant(tmpLocaleID+1, *tmpLocaleID,
                    (len<nameCapacity ? name+len : NULL), nameCapacity-len);
                if (variantSize > 0) {
                    variant = len<nameCapacity ? name+len : NULL;
                    len += variantSize;
                    tmpLocaleID += variantSize + 1; /* skip '_' and variant */
                }
            }
        }
    }

    /* Copy POSIX-style charset specifier, if any [mr.utf8] */
    if (!OPTION_SET(options, _ULOC_CANONICALIZE) && *tmpLocaleID == '.') {
        UBool done = FALSE;
        do {
            char c = *tmpLocaleID;
            switch (c) {
            case 0:
            case '@':
                done = TRUE;
                break;
            default:
                if (len<nameCapacity) {
                    name[len] = c;
                }
                ++len;
                ++tmpLocaleID;
                break;
            }
        } while (!done);
    }

    /* Scan ahead to next '@' and determine if it is followed by '=' and/or ';'
       After this, tmpLocaleID either points to '@' or is NULL */
    if ((tmpLocaleID=locale_getKeywordsStart(tmpLocaleID))!=NULL) {
        keywordAssign = uprv_strchr(tmpLocaleID, '=');
        separatorIndicator = uprv_strchr(tmpLocaleID, ';');
    }

    /* Copy POSIX-style variant, if any [mr@FOO] */
    if (!OPTION_SET(options, _ULOC_CANONICALIZE) &&
        tmpLocaleID != NULL && keywordAssign == NULL) {
        for (;;) {
            char c = *tmpLocaleID;
            if (c == 0) {
                break;
            }
            if (len<nameCapacity) {
                name[len] = c;
            }
            ++len;
            ++tmpLocaleID;
        }
    }

    if (OPTION_SET(options, _ULOC_CANONICALIZE)) {
        /* Handle @FOO variant if @ is present and not followed by = */
        if (tmpLocaleID!=NULL && keywordAssign==NULL) {
            int32_t posixVariantSize;
            /* Add missing '_' if needed */
            if (fieldCount < 2 || (fieldCount < 3 && scriptSize > 0)) {
                do {
                    if(len<nameCapacity) {
                        name[len]='_';
                    }
                    ++len;
                    ++fieldCount;
                } while(fieldCount<2);
            }
            posixVariantSize = _getVariantEx(tmpLocaleID+1, '@', name+len, nameCapacity-len,
                                             (UBool)(variantSize > 0));
            if (posixVariantSize > 0) {
                if (variant == NULL) {
                    variant = name+len;
                }
                len += posixVariantSize;
                variantSize += posixVariantSize;
            }
        }

        /* Handle generic variants first */
        if (variant) {
            for (j=0; j<(int32_t)(sizeof(VARIANT_MAP)/sizeof(VARIANT_MAP[0])); j++) {
                const char* variantToCompare = VARIANT_MAP[j].variant;
                int32_t n = (int32_t)uprv_strlen(variantToCompare);
                int32_t variantLen = _deleteVariant(variant, uprv_min(variantSize, (nameCapacity-len)), variantToCompare, n);
                len -= variantLen;
                if (variantLen > 0) {
                    if (len > 0 && name[len-1] == '_') { /* delete trailing '_' */
                        --len;
                    }
                    addKeyword = VARIANT_MAP[j].keyword;
                    addValue = VARIANT_MAP[j].value;
                    break;
                }
            }
            if (len > 0 && len <= nameCapacity && name[len-1] == '_') { /* delete trailing '_' */
                --len;
            }
        }

        /* Look up the ID in the canonicalization map */
        for (j=0; j<(int32_t)(sizeof(CANONICALIZE_MAP)/sizeof(CANONICALIZE_MAP[0])); j++) {
            const char* id = CANONICALIZE_MAP[j].id;
            int32_t n = (int32_t)uprv_strlen(id);
            if (len == n && uprv_strncmp(name, id, n) == 0) {
                if (n == 0 && tmpLocaleID != NULL) {
                    break; /* Don't remap "" if keywords present */
                }
                len = _copyCount(name, nameCapacity, CANONICALIZE_MAP[j].canonicalID);
                if (CANONICALIZE_MAP[j].keyword) {
                    addKeyword = CANONICALIZE_MAP[j].keyword;
                    addValue = CANONICALIZE_MAP[j].value;
                }
                break;
            }
        }
    }

    if (!OPTION_SET(options, _ULOC_STRIP_KEYWORDS)) {
        if (tmpLocaleID!=NULL && keywordAssign!=NULL &&
            (!separatorIndicator || separatorIndicator > keywordAssign)) {
            if(len<nameCapacity) {
                name[len]='@';
            }
            ++len;
            ++fieldCount;
            len += _getKeywords(tmpLocaleID+1, '@', (len<nameCapacity ? name+len : NULL), nameCapacity-len,
                                NULL, 0, NULL, TRUE, addKeyword, addValue, err);
        } else if (addKeyword != NULL) {
            U_ASSERT(addValue != NULL && len < nameCapacity);
            /* inelegant but works -- later make _getKeywords do this? */
            len += _copyCount(name+len, nameCapacity-len, "@");
            len += _copyCount(name+len, nameCapacity-len, addKeyword);
            len += _copyCount(name+len, nameCapacity-len, "=");
            len += _copyCount(name+len, nameCapacity-len, addValue);
        }
    }

    if (U_SUCCESS(*err) && result != NULL && name == localeBuffer) {
        uprv_strncpy(result, localeBuffer, (len > resultCapacity) ? resultCapacity : len);
    }

    return u_terminateChars(result, resultCapacity, len, err);
}

/* ### ID parsing API **************************************************/

U_CAPI int32_t  U_EXPORT2
uloc_getParent(const char*    localeID,
               char* parent,
               int32_t parentCapacity,
               UErrorCode* err)
{
    const char *lastUnderscore;
    int32_t i;
    
    if (U_FAILURE(*err))
        return 0;
    
    if (localeID == NULL)
        localeID = uloc_getDefault();

    lastUnderscore=uprv_strrchr(localeID, '_');
    if(lastUnderscore!=NULL) {
        i=(int32_t)(lastUnderscore-localeID);
    } else {
        i=0;
    }

    if(i>0 && parent != localeID) {
        uprv_memcpy(parent, localeID, uprv_min(i, parentCapacity));
    }
    return u_terminateChars(parent, parentCapacity, i, err);
}

U_CAPI int32_t U_EXPORT2
uloc_getLanguage(const char*    localeID,
         char* language,
         int32_t languageCapacity,
         UErrorCode* err)
{
    /* uloc_getLanguage will return a 2 character iso-639 code if one exists. *CWB*/
    int32_t i=0;

    if (err==NULL || U_FAILURE(*err)) {
        return 0;
    }
    
    if(localeID==NULL) {
        localeID=uloc_getDefault();
    }

    i=ulocimp_getLanguage(localeID, language, languageCapacity, NULL);
    return u_terminateChars(language, languageCapacity, i, err);
}

U_CAPI int32_t U_EXPORT2
uloc_getScript(const char*    localeID,
         char* script,
         int32_t scriptCapacity,
         UErrorCode* err)
{
    int32_t i=0;

    if(err==NULL || U_FAILURE(*err)) {
        return 0;
    }

    if(localeID==NULL) {
        localeID=uloc_getDefault();
    }

    /* skip the language */
    ulocimp_getLanguage(localeID, NULL, 0, &localeID);
    if(_isIDSeparator(*localeID)) {
        i=ulocimp_getScript(localeID+1, script, scriptCapacity, NULL);
    }
    return u_terminateChars(script, scriptCapacity, i, err);
}

U_CAPI int32_t  U_EXPORT2
uloc_getCountry(const char* localeID,
            char* country,
            int32_t countryCapacity,
            UErrorCode* err) 
{
    int32_t i=0;

    if(err==NULL || U_FAILURE(*err)) {
        return 0;
    }

    if(localeID==NULL) {
        localeID=uloc_getDefault();
    }

    /* Skip the language */
    ulocimp_getLanguage(localeID, NULL, 0, &localeID);
    if(_isIDSeparator(*localeID)) {
        const char *scriptID;
        /* Skip the script if available */
        ulocimp_getScript(localeID+1, NULL, 0, &scriptID);
        if(scriptID != localeID+1) {
            /* Found optional script */
            localeID = scriptID;
        }
        if(_isIDSeparator(*localeID)) {
            i=ulocimp_getCountry(localeID+1, country, countryCapacity, NULL);
        }
    }
    return u_terminateChars(country, countryCapacity, i, err);
}

U_CAPI int32_t  U_EXPORT2
uloc_getVariant(const char* localeID,
                char* variant,
                int32_t variantCapacity,
                UErrorCode* err) 
{
    char tempBuffer[ULOC_FULLNAME_CAPACITY];
    const char* tmpLocaleID;
    int32_t i=0;
    
    if(err==NULL || U_FAILURE(*err)) {
        return 0;
    }
    
    if (_hasBCP47Extension(localeID)) {
        _ConvertBCP47(tmpLocaleID, localeID, tempBuffer, sizeof(tempBuffer), err);
    } else {
        if (localeID==NULL) {
           localeID=uloc_getDefault();
        }
        tmpLocaleID=localeID;
    }
    
    /* Skip the language */
    ulocimp_getLanguage(tmpLocaleID, NULL, 0, &tmpLocaleID);
    if(_isIDSeparator(*tmpLocaleID)) {
        const char *scriptID;
        /* Skip the script if available */
        ulocimp_getScript(tmpLocaleID+1, NULL, 0, &scriptID);
        if(scriptID != tmpLocaleID+1) {
            /* Found optional script */
            tmpLocaleID = scriptID;
        }
        /* Skip the Country */
        if (_isIDSeparator(*tmpLocaleID)) {
            const char *cntryID;
            ulocimp_getCountry(tmpLocaleID+1, NULL, 0, &cntryID);
            if (cntryID != tmpLocaleID+1) {
                /* Found optional country */
                tmpLocaleID = cntryID;
            }
            if(_isIDSeparator(*tmpLocaleID)) {
                /* If there was no country ID, skip a possible extra IDSeparator */
                if (tmpLocaleID != cntryID && _isIDSeparator(tmpLocaleID[1])) {
                    tmpLocaleID++;
                }
                i=_getVariant(tmpLocaleID+1, *tmpLocaleID, variant, variantCapacity);
            }
        }
    }
    
    /* removed by weiv. We don't want to handle POSIX variants anymore. Use canonicalization function */
    /* if we do not have a variant tag yet then try a POSIX variant after '@' */
/*
    if(!haveVariant && (localeID=uprv_strrchr(localeID, '@'))!=NULL) {
        i=_getVariant(localeID+1, '@', variant, variantCapacity);
    }
*/
    return u_terminateChars(variant, variantCapacity, i, err);
}

U_CAPI int32_t  U_EXPORT2
uloc_getName(const char* localeID,
             char* name,
             int32_t nameCapacity,
             UErrorCode* err)  
{
    return _canonicalize(localeID, name, nameCapacity, 0, err);
}

U_CAPI int32_t  U_EXPORT2
uloc_getBaseName(const char* localeID,
                 char* name,
                 int32_t nameCapacity,
                 UErrorCode* err)  
{
    return _canonicalize(localeID, name, nameCapacity, _ULOC_STRIP_KEYWORDS, err);
}

U_CAPI int32_t  U_EXPORT2
uloc_canonicalize(const char* localeID,
                  char* name,
                  int32_t nameCapacity,
                  UErrorCode* err)  
{
    return _canonicalize(localeID, name, nameCapacity, _ULOC_CANONICALIZE, err);
}
  
U_CAPI const char*  U_EXPORT2
uloc_getISO3Language(const char* localeID) 
{
    int16_t offset;
    char lang[ULOC_LANG_CAPACITY];
    UErrorCode err = U_ZERO_ERROR;
    
    if (localeID == NULL)
    {
        localeID = uloc_getDefault();
    }
    uloc_getLanguage(localeID, lang, ULOC_LANG_CAPACITY, &err);
    if (U_FAILURE(err))
        return "";
    offset = _findIndex(LANGUAGES, lang);
    if (offset < 0)
        return "";
    return LANGUAGES_3[offset];
}

U_CAPI const char*  U_EXPORT2
uloc_getISO3Country(const char* localeID) 
{
    int16_t offset;
    char cntry[ULOC_LANG_CAPACITY];
    UErrorCode err = U_ZERO_ERROR;
    
    if (localeID == NULL)
    {
        localeID = uloc_getDefault();
    }
    uloc_getCountry(localeID, cntry, ULOC_LANG_CAPACITY, &err);
    if (U_FAILURE(err))
        return "";
    offset = _findIndex(COUNTRIES, cntry);
    if (offset < 0)
        return "";
    
    return COUNTRIES_3[offset];
}

U_CAPI uint32_t  U_EXPORT2
uloc_getLCID(const char* localeID) 
{
    UErrorCode status = U_ZERO_ERROR;
    char       langID[ULOC_FULLNAME_CAPACITY];

    uloc_getLanguage(localeID, langID, sizeof(langID), &status);
    if (U_FAILURE(status)) {
        return 0;
    }

    return uprv_convertToLCID(langID, localeID, &status);
}

U_CAPI int32_t U_EXPORT2
uloc_getLocaleForLCID(uint32_t hostid, char *locale, int32_t localeCapacity,
                UErrorCode *status)
{
    int32_t length;
    const char *posix = uprv_convertToPosix(hostid, status);
    if (U_FAILURE(*status) || posix == NULL) {
        return 0;
    }
    length = (int32_t)uprv_strlen(posix);
    if (length+1 > localeCapacity) {
        *status = U_BUFFER_OVERFLOW_ERROR;
    }
    else {
        uprv_strcpy(locale, posix);
    }
    return length;
}

/* ### Default locale **************************************************/

U_CAPI const char*  U_EXPORT2
uloc_getDefault()
{
    return locale_get_default();
}

U_CAPI void  U_EXPORT2
uloc_setDefault(const char*   newDefaultLocale,
             UErrorCode* err) 
{
    if (U_FAILURE(*err))
        return;
    /* the error code isn't currently used for anything by this function*/
    
    /* propagate change to C++ */
    locale_set_default(newDefaultLocale);
}

/**
 * Returns a list of all language codes defined in ISO 639.  This is a pointer
 * to an array of pointers to arrays of char.  All of these pointers are owned
 * by ICU-- do not delete them, and do not write through them.  The array is
 * terminated with a null pointer.
 */
U_CAPI const char* const*  U_EXPORT2
uloc_getISOLanguages() 
{
    return LANGUAGES;
}

/**
 * Returns a list of all 2-letter country codes defined in ISO 639.  This is a
 * pointer to an array of pointers to arrays of char.  All of these pointers are
 * owned by ICU-- do not delete them, and do not write through them.  The array is
 * terminated with a null pointer.
 */
U_CAPI const char* const*  U_EXPORT2
uloc_getISOCountries() 
{
    return COUNTRIES;
}


/* this function to be moved into cstring.c later */
static char gDecimal = 0;

static /* U_CAPI */
double
/* U_EXPORT2 */
_uloc_strtod(const char *start, char **end) {
    char *decimal;
    char *myEnd;
    char buf[30];
    double rv;
    if (!gDecimal) {
        char rep[5];
        /* For machines that decide to change the decimal on you,
        and try to be too smart with localization.
        This normally should be just a '.'. */
        sprintf(rep, "%+1.1f", 1.0);
        gDecimal = rep[2];
    }

    if(gDecimal == '.') {
        return uprv_strtod(start, end); /* fall through to OS */
    } else {
        uprv_strncpy(buf, start, 29);
        buf[29]=0;
        decimal = uprv_strchr(buf, '.');
        if(decimal) {
            *decimal = gDecimal;
        } else {
            return uprv_strtod(start, end); /* no decimal point */
        }
        rv = uprv_strtod(buf, &myEnd);
        if(end) {
            *end = (char*)(start+(myEnd-buf)); /* cast away const (to follow uprv_strtod API.) */
        }
        return rv;
    }
}

typedef struct { 
    float q;
    int32_t dummy;  /* to avoid uninitialized memory copy from qsort */
    char *locale;
} _acceptLangItem;

static int32_t U_CALLCONV
uloc_acceptLanguageCompare(const void * /*context*/, const void *a, const void *b)
{
    const _acceptLangItem *aa = (const _acceptLangItem*)a;
    const _acceptLangItem *bb = (const _acceptLangItem*)b;

    int32_t rc = 0;
    if(bb->q < aa->q) {
        rc = -1;  /* A > B */
    } else if(bb->q > aa->q) {
        rc = 1;   /* A < B */
    } else {
        rc = 0;   /* A = B */
    }

    if(rc==0) {
        rc = uprv_stricmp(aa->locale, bb->locale);
    }

#if defined(ULOC_DEBUG)
    /*  fprintf(stderr, "a:[%s:%g], b:[%s:%g] -> %d\n", 
    aa->locale, aa->q, 
    bb->locale, bb->q,
    rc);*/
#endif

    return rc;
}

/* 
mt-mt, ja;q=0.76, en-us;q=0.95, en;q=0.92, en-gb;q=0.89, fr;q=0.87, iu-ca;q=0.84, iu;q=0.82, ja-jp;q=0.79, mt;q=0.97, de-de;q=0.74, de;q=0.71, es;q=0.68, it-it;q=0.66, it;q=0.63, vi-vn;q=0.61, vi;q=0.58, nl-nl;q=0.55, nl;q=0.53
*/

U_CAPI int32_t U_EXPORT2
uloc_acceptLanguageFromHTTP(char *result, int32_t resultAvailable, UAcceptResult *outResult,
                            const char *httpAcceptLanguage,
                            UEnumeration* availableLocales,
                            UErrorCode *status)
{
    _acceptLangItem *j;
    _acceptLangItem smallBuffer[30];
    char **strs;
    char tmp[ULOC_FULLNAME_CAPACITY +1];
    int32_t n = 0;
    const char *itemEnd;
    const char *paramEnd;
    const char *s;
    const char *t;
    int32_t res;
    int32_t i;
    int32_t l = (int32_t)uprv_strlen(httpAcceptLanguage);
    int32_t jSize;
    char *tempstr; /* Use for null pointer check */

    j = smallBuffer;
    jSize = sizeof(smallBuffer)/sizeof(smallBuffer[0]);
    if(U_FAILURE(*status)) {
        return -1;
    }

    for(s=httpAcceptLanguage;s&&*s;) {
        while(isspace(*s)) /* eat space at the beginning */
            s++;
        itemEnd=uprv_strchr(s,',');
        paramEnd=uprv_strchr(s,';');
        if(!itemEnd) {
            itemEnd = httpAcceptLanguage+l; /* end of string */
        }
        if(paramEnd && paramEnd<itemEnd) { 
            /* semicolon (;) is closer than end (,) */
            t = paramEnd+1;
            if(*t=='q') {
                t++;
            }
            while(isspace(*t)) {
                t++;
            }
            if(*t=='=') {
                t++;
            }
            while(isspace(*t)) {
                t++;
            }
            j[n].q = (float)_uloc_strtod(t,NULL);
        } else {
            /* no semicolon - it's 1.0 */
            j[n].q = 1.0f;
            paramEnd = itemEnd;
        }
        j[n].dummy=0;
        /* eat spaces prior to semi */
        for(t=(paramEnd-1);(paramEnd>s)&&isspace(*t);t--)
            ;
        /* Check for null pointer from uprv_strndup */
        tempstr = uprv_strndup(s,(int32_t)((t+1)-s));
        if (tempstr == NULL) {
            *status = U_MEMORY_ALLOCATION_ERROR;
            return -1;
        }
        j[n].locale = tempstr;
        uloc_canonicalize(j[n].locale,tmp,sizeof(tmp)/sizeof(tmp[0]),status);
        if(strcmp(j[n].locale,tmp)) {
            uprv_free(j[n].locale);
            j[n].locale=uprv_strdup(tmp);
        }
#if defined(ULOC_DEBUG)
        /*fprintf(stderr,"%d: s <%s> q <%g>\n", n, j[n].locale, j[n].q);*/
#endif
        n++;
        s = itemEnd;
        while(*s==',') { /* eat duplicate commas */
            s++;
        }
        if(n>=jSize) {
            if(j==smallBuffer) {  /* overflowed the small buffer. */
                j = static_cast<_acceptLangItem *>(uprv_malloc(sizeof(j[0])*(jSize*2)));
                if(j!=NULL) {
                    uprv_memcpy(j,smallBuffer,sizeof(j[0])*jSize);
                }
#if defined(ULOC_DEBUG)
                fprintf(stderr,"malloced at size %d\n", jSize);
#endif
            } else {
                j = static_cast<_acceptLangItem *>(uprv_realloc(j, sizeof(j[0])*jSize*2));
#if defined(ULOC_DEBUG)
                fprintf(stderr,"re-alloced at size %d\n", jSize);
#endif
            }
            jSize *= 2;
            if(j==NULL) {
                *status = U_MEMORY_ALLOCATION_ERROR;
                return -1;
            }
        }
    }
    uprv_sortArray(j, n, sizeof(j[0]), uloc_acceptLanguageCompare, NULL, TRUE, status);
    if(U_FAILURE(*status)) {
        if(j != smallBuffer) {
#if defined(ULOC_DEBUG)
            fprintf(stderr,"freeing j %p\n", j);
#endif
            uprv_free(j);
        }
        return -1;
    }
    strs = static_cast<char **>(uprv_malloc((size_t)(sizeof(strs[0])*n)));
    /* Check for null pointer */
    if (strs == NULL) {
        uprv_free(j); /* Free to avoid memory leak */
        *status = U_MEMORY_ALLOCATION_ERROR;
        return -1;
    }
    for(i=0;i<n;i++) {
#if defined(ULOC_DEBUG)
        /*fprintf(stderr,"%d: s <%s> q <%g>\n", i, j[i].locale, j[i].q);*/
#endif
        strs[i]=j[i].locale;
    }
    res =  uloc_acceptLanguage(result, resultAvailable, outResult, 
        (const char**)strs, n, availableLocales, status);
    for(i=0;i<n;i++) {
        uprv_free(strs[i]);
    }
    uprv_free(strs);
    if(j != smallBuffer) {
#if defined(ULOC_DEBUG)
        fprintf(stderr,"freeing j %p\n", j);
#endif
        uprv_free(j);
    }
    return res;
}


U_CAPI int32_t U_EXPORT2
uloc_acceptLanguage(char *result, int32_t resultAvailable, 
                    UAcceptResult *outResult, const char **acceptList,
                    int32_t acceptListCount,
                    UEnumeration* availableLocales,
                    UErrorCode *status)
{
    int32_t i,j;
    int32_t len;
    int32_t maxLen=0;
    char tmp[ULOC_FULLNAME_CAPACITY+1];
    const char *l;
    char **fallbackList;
    if(U_FAILURE(*status)) {
        return -1;
    }
    fallbackList = static_cast<char **>(uprv_malloc((size_t)(sizeof(fallbackList[0])*acceptListCount)));
    if(fallbackList==NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return -1;
    }
    for(i=0;i<acceptListCount;i++) {
#if defined(ULOC_DEBUG)
        fprintf(stderr,"%02d: %s\n", i, acceptList[i]);
#endif
        while((l=uenum_next(availableLocales, NULL, status))) {
#if defined(ULOC_DEBUG)
            fprintf(stderr,"  %s\n", l);
#endif
            len = (int32_t)uprv_strlen(l);
            if(!uprv_strcmp(acceptList[i], l)) {
                if(outResult) { 
                    *outResult = ULOC_ACCEPT_VALID;
                }
#if defined(ULOC_DEBUG)
                fprintf(stderr, "MATCH! %s\n", l);
#endif
                if(len>0) {
                    uprv_strncpy(result, l, uprv_min(len, resultAvailable));
                }
                for(j=0;j<i;j++) {
                    uprv_free(fallbackList[j]);
                }
                uprv_free(fallbackList);
                return u_terminateChars(result, resultAvailable, len, status);   
            }
            if(len>maxLen) {
                maxLen = len;
            }
        }
        uenum_reset(availableLocales, status);    
        /* save off parent info */
        if(uloc_getParent(acceptList[i], tmp, sizeof(tmp)/sizeof(tmp[0]), status)!=0) {
            fallbackList[i] = uprv_strdup(tmp);
        } else {
            fallbackList[i]=0;
        }
    }

    for(maxLen--;maxLen>0;maxLen--) {
        for(i=0;i<acceptListCount;i++) {
            if(fallbackList[i] && ((int32_t)uprv_strlen(fallbackList[i])==maxLen)) {
#if defined(ULOC_DEBUG)
                fprintf(stderr,"Try: [%s]", fallbackList[i]);
#endif
                while((l=uenum_next(availableLocales, NULL, status))) {
#if defined(ULOC_DEBUG)
                    fprintf(stderr,"  %s\n", l);
#endif
                    len = (int32_t)uprv_strlen(l);
                    if(!uprv_strcmp(fallbackList[i], l)) {
                        if(outResult) { 
                            *outResult = ULOC_ACCEPT_FALLBACK;
                        }
#if defined(ULOC_DEBUG)
                        fprintf(stderr, "fallback MATCH! %s\n", l);
#endif
                        if(len>0) {
                            uprv_strncpy(result, l, uprv_min(len, resultAvailable));
                        }
                        for(j=0;j<acceptListCount;j++) {
                            uprv_free(fallbackList[j]);
                        }
                        uprv_free(fallbackList);
                        return u_terminateChars(result, resultAvailable, len, status);
                    }
                }
                uenum_reset(availableLocales, status);    

                if(uloc_getParent(fallbackList[i], tmp, sizeof(tmp)/sizeof(tmp[0]), status)!=0) {
                    uprv_free(fallbackList[i]);
                    fallbackList[i] = uprv_strdup(tmp);
                } else {
                    uprv_free(fallbackList[i]);
                    fallbackList[i]=0;
                }
            }
        }
        if(outResult) { 
            *outResult = ULOC_ACCEPT_FAILED;
        }
    }
    for(i=0;i<acceptListCount;i++) {
        uprv_free(fallbackList[i]);
    }
    uprv_free(fallbackList);
    return -1;
}

/*eof*/
