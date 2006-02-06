/*
 **********************************************************************
 *   Copyright (C) 2005-2006, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 */

#include "unicode/utypes.h"

#include "csdetect.h"
#include "csmatch.h"

#include "cmemory.h"
#include "umutex.h"
#include "ucln_in.h"
#include "inputext.h"
#include "csrsbcs.h"
#include "csrmbcs.h"
#include "csrutf8.h"
#include "csrucode.h"
#include "csr2022.h"

U_NAMESPACE_BEGIN

#define ARRAY_SIZE(array) (sizeof array / sizeof array[0])

#define NEW_ARRAY(type,count) (type *) uprv_malloc((count) * sizeof(type))
#define DELETE_ARRAY(array) uprv_free((void *) (array))

U_CDECL_BEGIN
static UBool U_CALLCONV csdet_cleanup(void)
{
    return UCharsetDetector::cleanup();
}
U_CDECL_END

CharsetRecognizer **UCharsetDetector::fCSRecognizers = NULL;

int32_t UCharsetDetector::fCSRecognizers_size = 0;

void UCharsetDetector::setRecognizers()
{
    UBool needsInit;
    CharsetRecognizer **recognizers;

    umtx_lock(NULL);
    needsInit = (UBool) (fCSRecognizers == NULL);
    umtx_unlock(NULL);

    if (needsInit) {
        CharsetRecognizer *tempArray[] = {
            new CharsetRecog_UTF8(),

            new CharsetRecog_UTF_16_BE(),
            new CharsetRecog_UTF_16_LE(),
            new CharsetRecog_UTF_32_BE(),
            new CharsetRecog_UTF_32_LE(),

            new CharsetRecog_8859_1_en(),
            new CharsetRecog_8859_1_da(),
            new CharsetRecog_8859_1_de(),
            new CharsetRecog_8859_1_es(),
            new CharsetRecog_8859_1_fr(),
            new CharsetRecog_8859_1_it(),
            new CharsetRecog_8859_1_nl(),
            new CharsetRecog_8859_1_no(),
            new CharsetRecog_8859_1_pt(),
            new CharsetRecog_8859_1_sv(),
            new CharsetRecog_8859_2_cs(),
            new CharsetRecog_8859_2_hu(),
            new CharsetRecog_8859_2_pl(),
            new CharsetRecog_8859_2_ro(),
            new CharsetRecog_8859_5_ru(),
            new CharsetRecog_8859_6_ar(),
            new CharsetRecog_8859_7_el(),
            new CharsetRecog_8859_8_I_he(),
            new CharsetRecog_8859_8_he(),
            new CharsetRecog_windows_1251(),
            new CharsetRecog_windows_1256(),
            new CharsetRecog_KOI8_R(),
            new CharsetRecog_8859_9_tr(),
            new CharsetRecog_sjis(),
            new CharsetRecog_gb_18030(),
            new CharsetRecog_euc_jp(),
            new CharsetRecog_euc_kr(),

            new CharsetRecog_2022JP(),
            new CharsetRecog_2022KR(),
            new CharsetRecog_2022CN()
        };
        int32_t rCount = ARRAY_SIZE(tempArray);
        int32_t r;

        recognizers = NEW_ARRAY(CharsetRecognizer *, rCount);
        for (r = 0; r < rCount; r += 1) {
           recognizers[r] = tempArray[r];
        }

        umtx_lock(NULL);
        if (fCSRecognizers == NULL) {
            fCSRecognizers = recognizers;
            fCSRecognizers_size = rCount;
        }
        umtx_unlock(NULL);

        if (fCSRecognizers != recognizers) {
            for (r = 0; r < rCount; r += 1) {
                delete recognizers[r];
                recognizers[r] = NULL;
            }

            DELETE_ARRAY(recognizers);
        }

        recognizers = NULL;
        ucln_i18n_registerCleanup(UCLN_I18N_CSDET, csdet_cleanup);
    }
}

UBool UCharsetDetector::cleanup()
{
    if (fCSRecognizers != NULL) {
        for(int32_t r = 0; r < fCSRecognizers_size; r += 1) {
            delete fCSRecognizers[r];
            fCSRecognizers[r] = NULL;
        }

        DELETE_ARRAY(fCSRecognizers);
        fCSRecognizers = NULL;
        fCSRecognizers_size = 0;
    }

    return TRUE;
}

UCharsetDetector::UCharsetDetector()
  : textIn(new InputText()), fStripTags(FALSE), fFreshTextSet(FALSE)
{
    setRecognizers();

    resultArray = new UCharsetMatch *[fCSRecognizers_size];

    for(int32_t i = 0; i < fCSRecognizers_size; i += 1) {
        resultArray[i] = new UCharsetMatch;
    }
}

UCharsetDetector::~UCharsetDetector()
{
    delete textIn;

    for(int32_t i = 0; i < fCSRecognizers_size; i += 1) {
        delete resultArray[i];
    }

    delete [] resultArray;
}

void UCharsetDetector::setText(const char *in, int32_t len)
{
    textIn->setText(in, len);
    fFreshTextSet = TRUE;
}

UBool UCharsetDetector::setStripTagsFlag(UBool flag)
{
    UBool temp = fStripTags;
    fStripTags = flag;
    fFreshTextSet = TRUE;
    return temp;
}

UBool UCharsetDetector::getStripTagsFlag() const
{
    return fStripTags;
}

void UCharsetDetector::setDeclaredEncoding(const char *encoding, int32_t len) const
{
    textIn->setDeclaredEncoding(encoding,len);
}

int32_t UCharsetDetector::getDetectableCount()
{
    setRecognizers();

    return fCSRecognizers_size; 
}

const UCharsetMatch* UCharsetDetector::detect(UErrorCode &status)
{
    int32_t maxMatchesFound = 0;

    detectAll(maxMatchesFound, status);

    if(maxMatchesFound > 0) {
        return resultArray[0];
    } else {
        return 0;
    }
}

// this sort of conversion is explicitly mentioned in the C++ standard
// in section 4.4:

// [Note: if a program could assign a pointer of type T** to a pointer of type
//  const T** (that is, if line //1 below was allowed), a program could
// 	    inadvertently modify a const object (as it is done on line //2).  For
// 						 example,

// 	 int32_t main() {
// 		const char c = 'c';
// 		char* pc;
// 		const char** pcc = &pc;//1: not allowed
// 		*pcc = &c;
// 		*pc = 'C';//2: modifies a const object
// 	    }
const UCharsetMatch * const *UCharsetDetector::detectAll(int32_t &maxMatchesFound, UErrorCode &status)
{
    int32_t resultCount = 0;

    if(!textIn->isSet()) {
        status = U_MISSING_RESOURCE_ERROR;// TODO:  Need to set proper status code for input text not set

        return 0;
    } else if(fFreshTextSet) {
        CharsetRecognizer *csr;
        int32_t            detectResults;
        int32_t            confidence;

        textIn->MungeInput(fStripTags);

        // Iterate over all possible charsets, remember all that
        // give a match quality > 0.
        for (int32_t i = 0; i < fCSRecognizers_size; i += 1) {
            csr = fCSRecognizers[i];
            detectResults = csr->match(textIn);
            confidence = detectResults;

            if (confidence > 0)  {
                resultArray[resultCount++]->set(textIn,csr,confidence);
            }
        }

        for(int32_t i = resultCount; i < fCSRecognizers_size; i += 1) {
            resultArray[i]->set(textIn,0,0);
        }

        //Bubble sort
        for(int32_t i = resultCount; i > 1; i -= 1) {
            for(int32_t j = 0; j < i-1; j += 1) {
                if(resultArray[j]->getConfidence() < resultArray[j+1]->getConfidence()) {
                    UCharsetMatch *temp = resultArray[j];
                    resultArray[j]= resultArray[j+1];
                    resultArray[j+1]=temp;
                }
            }
        }

        fFreshTextSet = FALSE;
    }

    maxMatchesFound = resultCount;

    return resultArray;
}

const char *UCharsetDetector::getCharsetName(int32_t index, UErrorCode &status) const
{
    if( index > fCSRecognizers_size-1 || index < 0) {
        status = U_INDEX_OUTOFBOUNDS_ERROR;

        return 0;
    } else {
        return fCSRecognizers[index]->getName();
    }
}

U_NAMESPACE_END

