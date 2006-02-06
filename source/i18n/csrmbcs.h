/*
 **********************************************************************
 *   Copyright (C) 2005-2006, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 */

#ifndef __CSRMBCS_H
#define __CSRMBCS_H

#include "unicode/utypes.h"

#include "csrecog.h"

U_NAMESPACE_BEGIN

// "Character"  iterated character class.
//    Recognizers for specific mbcs encodings make their "characters" available
//    by providing a nextChar() function that fills in an instance of iteratedChar
//    with the next char from the input.
//    The returned characters are not converted to Unicode, but remain as the raw
//    bytes (concatenated into an int) from the codepage data.
//
//  For Asian charsets, use the raw input rather than the input that has been
//   stripped of markup.  Detection only considers multi-byte chars, effectively
//   stripping markup anyway, and double byte chars do occur in markup too.
//
class iteratedChar
{
public:
    int32_t charValue;             // 1-4 bytes from the raw input data
    int32_t index;
    int32_t nextIndex;
    UBool   error;
    UBool   done;

public:
    iteratedChar();
    void reset();         
    int32_t nextByte(InputText* det); 
};


class CharsetRecog_mbcs : public CharsetRecognizer {

protected:    
    /**
     * Test the match of this charset with the input text data
     *      which is obtained via the CharsetDetector object.
     * 
     * @param det  The CharsetDetector, which contains the input text
     *             to be checked for being in this charset.
     * @return     Two values packed into one int  (Damn java, anyhow)
     *             <br/>
     *             bits 0-7:  the match confidence, ranging from 0-100
     *             <br/>
     *             bits 8-15: The match reason, an enum-like value.
     */
    int32_t match_mbcs(InputText* det, const int32_t commonChars[], int32_t commonCharsLen);

public:     

    virtual ~CharsetRecog_mbcs();	 

    /**
     * Get the IANA name of this charset.
     * @return the charset name.
     */

    const char *getName() const = 0 ;
    int32_t match(InputText* det) = 0;

    /**
     * Get the next character (however many bytes it is) from the input data
     *    Subclasses for specific charset encodings must implement this function
     *    to get characters according to the rules of their encoding scheme.
     * 
     *  This function is not a method of class iteratedChar only because
     *   that would require a lot of extra derived classes, which is awkward.
     * @param it  The iteratedChar "struct" into which the returned char is placed.
     * @param det The charset detector, which is needed to get at the input byte data
     *            being iterated over.
     * @return    True if a character was returned, false at end of input.
     */
    virtual UBool nextChar(iteratedChar *it, InputText *textIn) = 0;

};     


/**
 *   Shift-JIS charset recognizer.   
 *
 */
class CharsetRecog_sjis : public CharsetRecog_mbcs {
private:
    static const int32_t commonChars [];
    static const int32_t commonCharsLen;

public:
    virtual ~CharsetRecog_sjis();

    UBool nextChar(iteratedChar *it, InputText *det);

    int32_t match(InputText *det);

    const char *getName() const;

};


/**
 *   EUC charset recognizers.  One abstract class that provides the common function
 *             for getting the next character according to the EUC encoding scheme,
 *             and nested derived classes for EUC_KR, EUC_JP, EUC_CN.   
 *
 */
class CharsetRecog_euc : public CharsetRecog_mbcs 
{
public:
    virtual ~CharsetRecog_euc();

    const char* getName() const = 0;

    int32_t match(InputText* det) = 0;
    /*
     *  (non-Javadoc)
     *  Get the next character value for EUC based encodings.
     *  Character "value" is simply the raw bytes that make up the character
     *     packed into an int.
     */
    UBool nextChar(iteratedChar *it, InputText *det);
};         

/**
 * The charset recognize for EUC-JP.  A singleton instance of this class
 *    is created and kept by the public CharsetDetector class
 */
class CharsetRecog_euc_jp : public CharsetRecog_euc
{
private:
    static const int32_t commonChars[];
    static const int32_t commonCharsLen;

public:             
    virtual ~CharsetRecog_euc_jp();

    const char *getName() const;

    int32_t match(InputText *det);
};

/**
 * The charset recognize for EUC-KR.  A singleton instance of this class
 *    is created and kept by the public CharsetDetector class
 */
class CharsetRecog_euc_kr : public CharsetRecog_euc
{
private:
    static const int32_t commonChars[];
    static const int32_t commonCharsLen;

public:
    virtual ~CharsetRecog_euc_kr();

    const char *getName() const;

    int32_t match(InputText *det);
};


/**
 * 
 *   GB-18030 recognizer. Uses simplified Chinese statistics.   
 *
 */
class CharsetRecog_gb_18030 : public CharsetRecog_mbcs 
{
private:

    static const int32_t commonChars[];                 
    static const int32_t commonCharsLen;
    /*
     *  (non-Javadoc)
     *  Get the next character value for EUC based encodings.
     *  Character "value" is simply the raw bytes that make up the character
     *     packed into an int.
     */

public:
    virtual ~CharsetRecog_gb_18030();

    UBool nextChar(iteratedChar* it, InputText* det);

    const char *getName() const;

    int32_t match(InputText *det);
};

U_NAMESPACE_END

#endif /* __CSRMBCS_H */
