/*
 *******************************************************************************
 *
 *   Copyright (C) 2003, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *   file name:  strprep.h
 *   encoding:   US-ASCII
 *   tab size:   8 (not used)
 *   indentation:4
 *
 *   created on: 2003feb1
 *   created by: Ram Viswanadha
 */

#ifndef STRPREP_H
#define STRPREP_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_IDNA

#include "unicode/uobject.h"
#include "unicode/uniset.h"
#include "unicode/parseerr.h"

U_NAMESPACE_BEGIN

/**\file
 *
 * This API implements RF 3454 StringPrep standard.
 *
 * The steps for preparing strings are:
 *
 *  1) Map -- For each character in the input, check if it has a mapping
 *      and, if so, replace it with its mapping.
 *      <ul>
 *      <li>Delete certain codepoints from the input because their 
 *          presence or absence in the  protocol identifies should not 
 *          make two strings different</li>
 *      <li>Case Mapings
 *          <br>If Normalization is turned off
 *          <br>    Get mappings from case map tables
 *          <br>else
 *          <br>    Get mappings from case map tables for normalization
 *          <br>    Use u_getFC_NFKC_Closure for obtaining extra mappings
 *      </li>
 *      </ul>
 *  2) Normalize -- Possibly normalize the result of step 1 using Unicode
 *     normalization NFKC.  
 *
 *  3) Prohibit -- Check for any characters that are not allowed in the
 *     output.  If any are found, return an error.  
 *
 *  4) Check bidi -- Possibly check for right-to-left characters, and if
 *     any are found, make sure that the whole string satisfies the
 *     requirements for bidirectional strings.  If the string does not
 *     satisfy the requirements for bidirectional strings, return an
 *     error.  
 *
 * Some StringPrep profiles:
 * IDN: "Nameprep" http://www.ietf.org/rfc/rfc3491.txt
 * XMPP Node Identifiers: "Nodeprep" http://www.ietf.org/internet-drafts/draft-ietf-xmpp-nodeprep-01.txt
 * XMPP Resource Identifiers: "Resourceprep" http://www.ietf.org/internet-drafts/draft-ietf-xmpp-resourceprep-01.txt
 * ANONYMOUS SASL tokens: "plain" http://www.ietf.org/internet-drafts/draft-ietf-sasl-anon-00.txt
 * iSCSI    http://www.ietf.org/internet-drafts/draft-ietf-ips-iscsi-string-prep-03.txt
 */ 
class StringPrep : public UObject{

protected:
    UVersionInfo unicodeVersion;    /** The Character repertoire version of this profile */ 
    UBool bidiCheck;                /** Option to turn BiDi checking on     */
    UBool doNFKC;                   /** Option to turn NFKC on              */
    
    /**
     * Protected default constructor sub classes
     */
    StringPrep(){};

public:
    /**
     * Destructor
     */
    virtual inline ~StringPrep(){};

    /**
     * Map every character in input stream with mapping character 
     * in the mapping table and populate the output stream.
     * For any individual character the mapping table may specify 
     * that that a character be mapped to nothing, mapped to one 
     * other character or to a string of other characters.
     *
     * @param src           Pointer to UChar buffer containing a single label
     * @param srcLength     Number of characters in the source label
     * @param dest          Pointer to the destination buffer to receive the output
     * @param destCapacity  The capacity of destination array
     * @param allowUnassigned   Unassigned values can be converted to ASCII for query operations
     *                          If TRUE unassigned values are treated as normal Unicode code point.
     *                          If FALSE the operation fails with U_UNASSIGNED_CODE_POINT_FOUND error code.
     * @param status        ICU error code in/out parameter.
     *                      Must fulfill U_SUCCESS before the function call.
     * @return The number of UChars in the destination buffer
     *
     */
    virtual int32_t map(const UChar* src, int32_t srcLength, 
                        UChar* dest, int32_t destCapacity, 
                        UBool allowUnassigned,
                        UParseError* parseError,
                        UErrorCode& status );

    /**
     * Normalize the input stream using Normalization Form KC (NFKC)
     *
     * @param src           Pointer to UChar buffer containing a single label
     * @param srcLength     Number of characters in the source label
     * @param dest          Pointer to the destination buffer to receive the output
     * @param destCapacity  The capacity of destination array
     * @param status        ICU error code in/out parameter.
     *                      Must fulfill U_SUCCESS before the function call.
     * @return The number of UChars in the destination buffer
     *
     *
     */
    virtual int32_t normalize(  const UChar* src, int32_t srcLength, 
                                    UChar* dest, int32_t destCapacity, 
                                    UErrorCode& status );


    /**
     * Prepare the input stream with for use. This operation maps, normalizes(NFKC),
     * checks for prohited and BiDi characters in the order defined by RFC 3454
     * 
     * @param src           Pointer to UChar buffer containing a single label
     * @param srcLength     Number of characters in the source label
     * @param dest          Pointer to the destination buffer to receive the output
     * @param destCapacity  The capacity of destination array
     * @param allowUnassigned   Unassigned values can be converted to ASCII for query operations
     *                          If TRUE unassigned values are treated as normal Unicode code point.
     *                          If FALSE the operation fails with U_UNASSIGNED_CODE_POINT error code.
     * @param status        ICU error code in/out parameter.
     *                      Must fulfill U_SUCCESS before the function call.
     * @return The number of UChars in the destination buffer
     *
     *
     */
    virtual int32_t process(const UChar* src, int32_t srcLength, 
                            UChar* dest, int32_t destCapacity, 
                            UBool allowUnassigned,
                            UParseError* parseError,
                            UErrorCode& status );

    /**
     * Create a profile from prebuilt default Nameprep profile conforming to 
     * nameprep internet draft (http://www.ietf.org/html.charters/idn-charter.html).
     * This is a built-in/unmodifiable profile. 
     *
     * @param status        ICU error code in/out parameter.
     *                      Must fulfill U_SUCCESS before the function call.
     * @return Pointer to StringPrep object that is created. Should be deleted by
     * by caller
     *
     *
     */
    static StringPrep* createNameprepInstance(UErrorCode& status);

    /**
     * Create a profile from prebuilt default StringPrep profile conforming to 
     * RFC 3454 (ftp://ftp.rfc-editor.org/in-notes/rfc3454.txt).
     * User defined profiles can be created by getting the default profile and
     * adding mappings, removing mappings, turning options ON/OFF and prohibiting 
     * characters from the output.
     *
     * @param status        ICU error code in/out parameter.
     *                      Must fulfill U_SUCCESS before the function call.
     * @return Pointer to StringPrep object that is created. Should be deleted by
     * the caller.
     *
     *
     */
    static StringPrep* createDefaultInstance(UErrorCode& status);

    /**
     * Ascertain if the given code point is a Letter/Digit/Hyphen in the ASCII range
     *
     * @return TRUE is the code point is a Letter/Digit/Hyphen
     *
     *
     */
    static inline UBool isLDHChar(UChar32 ch);

    /**
     * Ascertain if the given code point is a label separator as specified by IDNA
     *
     * @return TRUE is the code point is a label separator
     *
     *
     */
    virtual UBool isLabelSeparator(UChar32 ch, UErrorCode& status);

    /**
     * Get the BiDi option of this profile
     *
     *
     */
     inline UBool getCheckBiDi();

    /**
     * Get the normalization (NFKC) option of this profile
     *
     * @return The normalization option
     *
     *
     */
     inline UBool getNormalization();
    
     /**
      * Get the Unicode version which this profile
      * conforms to
      *
      *
      */
     inline void getUnicodeVersion(UVersionInfo& info);

private:
     // Boiler plate
    
    /**
     * Copy constructor.
     *
     */
    StringPrep(const StringPrep&);

    /**
     * Assignment operator.
     *
     */
    StringPrep& operator=(const StringPrep&);

    /**
     * Return true if another object is semantically equal to this one.
     *
     * @param other    the object to be compared with.
     * @return         true if another object is semantically equal to this one.
     *
     */
    UBool operator==(const StringPrep& other) const {return FALSE;};

    /**
     * Return true if another object is semantically unequal to this one.
     *
     * @param other    the object to be compared with.
     * @return         true if another object is semantically unequal to this one.
     *
     */
    UBool operator!=(const StringPrep& other) const { return !operator==(other); }

public:

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     *
     */
    static inline UClassID getStaticClassID();

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     *
     */
    virtual inline UClassID getDynamicClassID() const;

protected:
    
    /**
     * Sub classes that slightly modify the default profile
     * implement this method to remove characters to 
     * the prohibited list. The default implementation does not
     * check if the data is loaded or not. The caller is responsible
     * for checking for data.
     *
     */
    virtual UBool isNotProhibited(UChar32 ch);
    
    /**
     * Sub classes that slightly modify the default profile
     * implement this method to remove characters to 
     * the unassigned list. The default implementation does not
     * check if the data is loaded or not. The caller is responsible
     * for checking for data.
     */
    virtual UBool isUnassigned(UChar32 ch);
    
    /**
     * Ascertains if uidna.icu data file is loaded.
     * If data is not loaded, loads the data file.
     *
     *
     */
    static UBool isDataLoaded(UErrorCode& status);

private:

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;

};

inline UBool StringPrep::getCheckBiDi(){
    return bidiCheck;
}


inline UBool StringPrep::getNormalization(){
    return doNFKC;
}

inline void StringPrep::getUnicodeVersion(UVersionInfo& info){
    for(int32_t i=0; i< (int32_t)(sizeof(info)/sizeof(info[0])); i++){
        info[i] = unicodeVersion[i];
    }
}

inline UClassID StringPrep::getStaticClassID() { 
    return (UClassID)&fgClassID; 
}

inline UClassID StringPrep::getDynamicClassID() const { 
    return getStaticClassID(); 
}

inline UBool StringPrep::isLDHChar(UChar32 ch){
    // high runner case
    if(ch>0x007A){
        return FALSE;
    }
    //[\\u002D \\u0030-\\u0039 \\u0041-\\u005A \\u0061-\\u007A]
    if( (ch==0x002D) || 
        (0x0030 <= ch && ch <= 0x0039) ||
        (0x0041 <= ch && ch <= 0x005A) ||
        (0x0061 <= ch && ch <= 0x007A)
      ){
        return TRUE;
    }
    return FALSE;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_IDNA */

#endif

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
