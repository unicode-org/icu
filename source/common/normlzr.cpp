/*
 ********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1996-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************
 */

#include "ucmp16.h"
#include "dcmpdata.h"
#include "compdata.h"

#include "unicode/normlzr.h"
#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/chariter.h"
#include "unicode/schriter.h"
#include "unicode/unicode.h"
#include "mutex.h"


#define ARRAY_LENGTH(array) (sizeof (array) / sizeof (*array))

inline static void insert(UnicodeString& dest, 
              UTextOffset pos, 
              UChar ch)
{
  dest.replace(pos, 0, &ch, 1);
}

const UChar     Normalizer::DONE     = 0xFFFF;
const UChar    Normalizer::HANGUL_BASE = 0xac00;
const UChar    Normalizer::HANGUL_LIMIT= 0xd7a4;
const UChar    Normalizer::JAMO_LBASE  = 0x1100;
const UChar    Normalizer::JAMO_VBASE  = 0x1161;
const UChar    Normalizer::JAMO_TBASE  = 0x11a7;
const int16_t    Normalizer::JAMO_LCOUNT = 19;
const int16_t    Normalizer::JAMO_VCOUNT = 21;
const int16_t    Normalizer::JAMO_TCOUNT = 28;
const int16_t    Normalizer::JAMO_NCOUNT = JAMO_VCOUNT * JAMO_TCOUNT;



//-------------------------------------------------------------------------
// Constructors and other boilerplate
//-------------------------------------------------------------------------

Normalizer::Normalizer(const UnicodeString& str, 
               EMode mode)
{
  init(new StringCharacterIterator(str), mode, 0);
}

Normalizer::Normalizer(const UnicodeString& str, 
               EMode mode, 
               int32_t opt)
{
  init(new StringCharacterIterator(str), mode, opt);
}

Normalizer::Normalizer(const UChar* str, int32_t length, EMode mode) 
{
  init(new StringCharacterIterator(UnicodeString(str, length)), mode, 0);
}

Normalizer::Normalizer(const CharacterIterator& iter, 
               EMode mode)
{
  init(iter.clone(), mode, 0);
}

Normalizer::Normalizer(const CharacterIterator& iter, 
               EMode mode, 
               int32_t opt)
{
  init(iter.clone(), mode, opt);
}

void Normalizer::init(CharacterIterator* adoptIter, 
              EMode mode, 
              int32_t options)
{
  bufferPos = 0;
  bufferLimit = 0;
  fOptions = options;
  currentChar = DONE;
  fMode = mode;
  text = adoptIter;
  
  minDecomp = (fMode & COMPAT_BIT) ? 0 : DecompData::MAX_COMPAT;
}

Normalizer::Normalizer(const Normalizer& copy)
{
  init(copy.text->clone(), copy.fMode, copy.fOptions);
  
  buffer      = copy.buffer;
  bufferPos   = copy.bufferPos;
  bufferLimit = copy.bufferLimit;
  explodeBuf  = copy.explodeBuf;
  currentChar = copy.currentChar;
}

Normalizer::~Normalizer()
{
  delete text;
}

Normalizer*    
Normalizer::clone() const
{
  if(this!=0) {
    return new Normalizer(*this);
  } else {
    return 0;
  }
}

/**
 * Generates a hash code for this iterator.
 */
int32_t Normalizer::hashCode() const
{
  return text->hashCode() + fMode + fOptions + bufferPos + bufferLimit;
}
    
bool_t Normalizer::operator==(const Normalizer& that) const
{
  return *text == *(that.text)
  && currentChar == that.currentChar
  && buffer == that.buffer
  && explodeBuf == that.explodeBuf
  && bufferPos == that.bufferPos
  && bufferLimit == that.bufferLimit;
}

//-------------------------------------------------------------------------
// Static utility methods
//-------------------------------------------------------------------------

void 
Normalizer::normalize(const UnicodeString& source, 
              EMode mode, 
              int32_t options,
              UnicodeString& result, 
              UErrorCode &status)
{
  switch (mode) {
  case NO_OP:
    result = source;
    break;
  case COMPOSE:
  case COMPOSE_COMPAT:
    compose(source, mode & COMPAT_BIT, options, result, status);
    break;
  case DECOMP:
  case DECOMP_COMPAT:
    decompose(source, mode & COMPAT_BIT, options, result, status);
    break;
  }
}

//-------------------------------------------------------------------------
// Compose methods
//-------------------------------------------------------------------------

void
Normalizer::compose(const UnicodeString& source, 
            bool_t compat,
            int32_t options,
            UnicodeString& result, 
            UErrorCode &status)
{
  if (U_FAILURE(status)) {
    return;
  }
  result.truncate(0);
  UnicodeString explodeBuf;
  
  UTextOffset  explodePos = EMPTY;         // Position in input buffer
  UTextOffset  basePos = 0;                // Position of last base in output string
  uint16_t    baseIndex = 0;              // Index of last base in "actions" array
  uint32_t    classesSeen = 0;            // Combining classes seen since last base
  uint16_t    action;
    
  // Compatibility explosions have lower indices; skip them if necessary
  uint16_t minExplode = compat ? 0 : ComposeData::MAX_COMPAT;
  uint16_t minDecomp = compat ? 0 : DecompData::MAX_COMPAT;
  
    UTextOffset i = 0;
    while (i < source.length() || explodePos != EMPTY) {
        // Get the next char from either the buffer or the source
      UChar ch;
      if (explodePos == EMPTY) {
    ch = source[i++];
      } else {
    ch = explodeBuf[explodePos++];
    if (explodePos >= explodeBuf.length()) {
      explodePos = EMPTY;
      explodeBuf.truncate(0);
    }
      }
      
      // Get the basic info for the character
      uint16_t charInfo = composeLookup(ch);
      uint16_t type = charInfo & ComposeData::TYPE_MASK;
      uint16_t index = charInfo >> ComposeData::INDEX_SHIFT;
      
      if (type == ComposeData::BASE) {
    classesSeen = 0;
    baseIndex = index;
    basePos = result.length();
    result += ch;
      }
      else if (type == ComposeData::COMBINING || type == ComposeData::NON_COMPOSING_COMBINING)
        {
      uint32_t cclass = ComposeData::typeMask[index];
      
      // We can only combine a character with the base if we haven't
      // already seen a combining character with the same canonical class.
      if (type == ComposeData::COMBINING && (classesSeen & cclass) == 0
          && (action = composeAction(baseIndex, index)) > 0)
            {
          if (action > ComposeData::MAX_COMPOSED) {
        // Pairwise explosion.  Actions above this value are really
        // indices into an array that in turn contains indices
        // into the exploding string table
        // TODO: What if there are unprocessed chars in the explode buffer?
        UChar newBase = pairExplode(explodeBuf, action);
        explodePos = 0;
        result[basePos] = newBase;
        
        baseIndex = composeLookup(newBase) >> ComposeData::INDEX_SHIFT;
          } else {
        // Normal pairwise combination.  Replace the base char
        UChar newBase = (UChar) action;
        result[basePos] = newBase;
        
        baseIndex = composeLookup(newBase) >> ComposeData::INDEX_SHIFT;
          }
          //
          // Since there are Unicode characters that cannot be combined in arbitrary
          // order, we have to re-process any combining marks that go with this
          // base character.  There are only four characters in Unicode that have
          // this problem.  If they are fixed in Unicode 3.0, this code can go away.
          //
          UTextOffset len = result.length();
          if (len - basePos > 1) {
        for (UTextOffset j = basePos+1; j < len; j++) {
          explodeBuf += result[j];
        }
        result.truncate(basePos+1);
        classesSeen = 0;
        if (explodePos == EMPTY) explodePos = 0;
          }
            } else {
          // No combination with this character
          bubbleAppend(result, ch, cclass);
          classesSeen |= cclass;
            }
        }
      else if (index > minExplode) {
    // Single exploding character
    explode(explodeBuf, index);
    explodePos = 0;
      }
      else if (type == ComposeData::HANGUL && minExplode == 0) {
    // If we're in compatibility mode we need to decompose Hangul to Jamo,
    // because some of the Jamo might have compatibility decompositions.
    hangulToJamo(ch, explodeBuf, minDecomp);
    explodePos = 0;
      }
      else if (type == ComposeData::INITIAL_JAMO) {
    classesSeen = 0;
    baseIndex = ComposeData::INITIAL_JAMO_INDEX;
    basePos = result.length();
    result += ch;
      }
      else if (type == ComposeData::MEDIAL_JAMO && classesSeen == 0
           && baseIndex == ComposeData::INITIAL_JAMO_INDEX) {
    // If the last character was an initial jamo, we can combine it with this
    // one to create a Hangul character.
    uint16_t l = result[basePos] - JAMO_LBASE;
    uint16_t v = ch - JAMO_VBASE;
    result[basePos] = (UChar)(HANGUL_BASE + (l*JAMO_VCOUNT + v) * JAMO_TCOUNT);
    
    baseIndex = ComposeData::MEDIAL_JAMO_INDEX;
      }
      else if (type == ComposeData::FINAL_JAMO && classesSeen == 0
           && baseIndex == ComposeData::MEDIAL_JAMO_INDEX) {
    // If the last character was a medial jamo that we turned into Hangul,
    // we can add this character too.
    result[basePos] = (UChar)(result[basePos] + (ch - JAMO_TBASE));
    
    baseIndex = 0;
    basePos = -1;
    classesSeen = 0;
      } else {
    baseIndex = 0;
    basePos = -1;
    classesSeen = 0;
    result += ch;
      }
    }
}

/**
 * Compose starting with current input character and continuing
 * until just before the next base char.
 * <p>
 * <b>Input</b>:
 * <ul>
 *  <li>underlying char iter points to first character to decompose
 * </ul>
 * <p>
 * <b>Output:</b>
 * <ul>
 *  <li>returns first char of decomposition or DONE if at end
 *  <li>Underlying char iter is pointing at next base char or past end
 * </ul>
 */
UChar Normalizer::nextCompose() 
{
    UTextOffset  explodePos = EMPTY;         // Position in input buffer
    UTextOffset  basePos = 0;                // Position of last base in output string
    uint16_t    baseIndex = 0;              // Index of last base in "actions" array
    uint32_t    classesSeen = 0;            // Combining classes seen since last base
    uint16_t    action;
    UChar        lastBase = 0;
    bool_t        chFromText = TRUE;
    
    // Compatibility explosions have lower indices; skip them if necessary
    uint16_t minExplode = (fMode & COMPAT_BIT) ? 0 : ComposeData::MAX_COMPAT;
    uint16_t minDecomp = (fMode & COMPAT_BIT) ? 0 : DecompData::MAX_COMPAT;
    
    initBuffer();
    explodeBuf.truncate(0);
    
    UChar ch = curForward();

    while (ch != DONE) {
        // Get the basic info for the character
        uint16_t charInfo = composeLookup(ch);
        uint16_t type = charInfo & ComposeData::TYPE_MASK;
        uint16_t index = charInfo >> ComposeData::INDEX_SHIFT;
        
        if (type == ComposeData::BASE) {
            if (buffer.length() > 0 && chFromText && explodePos == EMPTY) {
                // When we hit a base char in the source text, we can return the text
                // that's been composed so far.  We'll re-process this char next time through.
                break;
            }
            classesSeen = 0;
            baseIndex = index;
            basePos = buffer.length();
            buffer += ch;
            lastBase = ch;
        }
        else if (type == ComposeData::COMBINING || type == ComposeData::NON_COMPOSING_COMBINING)
        {
            uint32_t cclass = ComposeData::typeMask[index];
            
            // We can only combine a character with the base if we haven't
            // already seen a combining character with the same canonical class.
            if (type == ComposeData::COMBINING && (classesSeen & cclass) == 0
                && (action = composeAction(baseIndex, index)) > 0)
            {
                if (action > ComposeData::MAX_COMPOSED) {
                    // Pairwise explosion.  Actions above this value are really
                    // indices into an array that in turn contains indices
                    // into the exploding string table
                    // TODO: What if there are unprocessed chars in the explode buffer?
                    UChar newBase = pairExplode(explodeBuf, action);
                    explodePos = 0;
                    buffer[basePos] = newBase;

                    baseIndex = composeLookup(newBase) >> ComposeData::INDEX_SHIFT;
                    lastBase = newBase;
                } else {
                    // Normal pairwise combination.  Replace the base char
                    UChar newBase = (UChar) action;
                    buffer[basePos] = newBase;
                                            
                    baseIndex = composeLookup(newBase) >> ComposeData::INDEX_SHIFT;
                    lastBase = newBase;
                }
                //
                // Since there are Unicode characters that cannot be combined in arbitrary
                // order, we have to re-process any combining marks that go with this
                // base character.  There are only four characters in Unicode that have
                // this problem.  If they are fixed in Unicode 3.0, this code can go away.
                //
                UTextOffset len = buffer.length();
                if (len - basePos > 1) {
                    for (UTextOffset j = basePos+1; j < len; j++) {
                        explodeBuf += buffer[j];
                    }
                    buffer.truncate(basePos+1);
                    classesSeen = 0;
                    if (explodePos == EMPTY) explodePos = 0;
                }
            } else {
                // No combination with this character
                bubbleAppend(buffer, ch, cclass);
                classesSeen |= cclass;
            }
        }
        else if (index > minExplode) {
            // Single exploding character
            explode(explodeBuf, index);
            explodePos = 0;
        }
        else if (type == ComposeData::HANGUL && minExplode == 0) {
            // If we're in compatibility mode we need to decompose Hangul to Jamo,
            // because some of the Jamo might have compatibility decompositions.
            hangulToJamo(ch, explodeBuf, minDecomp);
            explodePos = 0;
        }
        else if (type == ComposeData::INITIAL_JAMO) {
            if (buffer.length() > 0 && chFromText && explodePos == EMPTY) {
                // When we hit a base char in the source text, we can return the text
                // that's been composed so far.  We'll re-process this char next time through.
                break;
            }
            classesSeen = 0;
            baseIndex = ComposeData::INITIAL_JAMO_INDEX;
            basePos = buffer.length();
            buffer += ch;
        }
        else if (type == ComposeData::MEDIAL_JAMO && classesSeen == 0
                    && baseIndex == ComposeData::INITIAL_JAMO_INDEX) {
            // If the last character was an initial jamo, we can combine it with this
            // one to create a Hangul character.
            uint16_t l = buffer[basePos] - JAMO_LBASE;
            uint16_t v = ch - JAMO_VBASE;
            UChar newCh = (UChar)(HANGUL_BASE + (l*JAMO_VCOUNT + v) * JAMO_TCOUNT);
            buffer[basePos] = newCh;
            
            baseIndex = ComposeData::MEDIAL_JAMO_INDEX;
        }
        else if (type == ComposeData::FINAL_JAMO && classesSeen == 0
                    && baseIndex == ComposeData::MEDIAL_JAMO_INDEX) {
            // If the last character was a medial jamo that we turned into Hangul,
            // we can add this character too.
            UChar newCh = (UChar)(buffer[basePos] + (ch - JAMO_TBASE));
            buffer[basePos] = newCh;

            baseIndex = 0;
            basePos = -1;
            classesSeen = 0;
        } else {
            // TODO: deal with JAMO character types
            baseIndex = 0;
            basePos = -1;
            classesSeen = 0;
            buffer += ch;
        }
        
        if (explodePos == EMPTY) {
            ch = text->next();
            chFromText = TRUE;
        } else {
            ch = explodeBuf[explodePos++];
            if (explodePos >= explodeBuf.length()) {
                explodePos = EMPTY;
                explodeBuf.truncate(0);
            }
            chFromText = FALSE;
        }
    }
    if (buffer.length() > 0) {
        bufferLimit = buffer.length() - 1;
        ch = buffer[0];
    } else {
        ch = DONE;
        bufferLimit = 0;
    }
    return ch;
}

/**
 * Compose starting with the input UChar just before the current position
 * and continuing backward until (and including) the previous base char.
 * <p>
 * <b>Input</b>:
 * <ul>
 *  <li>underlying char iter points just after last char to decompose
 * </ul>
 * <p>
 * <b>Output:</b>
 * <ul>
 *  <li>returns last char of resulting decomposition sequence
 *  <li>underlying iter points to lowest-index char we decomposed, i.e. the base char
 * </ul>
 */
UChar Normalizer::prevCompose()
{
    UErrorCode status = U_ZERO_ERROR;
    initBuffer();
    
    // Slurp up characters until we hit a base char or an initial Jamo
    UChar ch;
    while ((ch = curBackward()) != DONE) {
        insert(buffer, 0, ch);
        
        // Get the basic info for the character
        uint16_t charInfo = composeLookup(ch);
        uint16_t type = charInfo & ComposeData::TYPE_MASK;
        
        if (type == ComposeData::BASE || type == ComposeData::HANGUL 
            || type == ComposeData::INITIAL_JAMO || type == ComposeData::IGNORE)
        {
            break;
        }
    }
    // If there's more than one character in the buffer, compose it all at once....
    if (buffer.length() > 0) {
        // TODO: The performance of this is awful; add a way to compose
        // a UnicodeString& in place.
      UnicodeString composed;
      compose(buffer, (fMode & COMPAT_BIT), fOptions, composed, status);
      buffer.truncate(0);
      buffer += composed;
        
        if (buffer.length() > 1) {
            bufferLimit = bufferPos = buffer.length() - 1;
            ch = buffer[bufferPos];
        } else {
            ch = buffer[0];
        }
    }
    else {
        ch = DONE;
    }
    
    return ch;
}

void Normalizer::bubbleAppend(UnicodeString& target, UChar ch, uint32_t cclass) {
    UTextOffset i;
    for (i = target.length() - 1; i > 0; --i) {
        uint32_t iClass = getComposeClass(target[i]);

        if (iClass == 1 || iClass <= cclass) {      // 1 means combining class 0
            // We've hit something we can't bubble this character past, so insert here
            break;
        }
    }
    // We need to insert just after character "i"
    insert(target, i+1, ch);
}
    

uint32_t Normalizer::getComposeClass(UChar ch) {
    uint32_t cclass = 0;
    uint16_t charInfo = composeLookup(ch);
    uint16_t type = charInfo & ComposeData::TYPE_MASK;
    if (type == ComposeData::COMBINING || type == ComposeData::NON_COMPOSING_COMBINING) {
        cclass = ComposeData::typeMask[charInfo >> ComposeData::INDEX_SHIFT];
    }
    return cclass;
}

uint16_t Normalizer::composeLookup(UChar ch) {
  return ucmp16_getu(ComposeData::lookup, ch);
}

uint16_t Normalizer::composeAction(uint16_t baseIndex, uint16_t comIndex) 
{
  return ucmp16_getu(ComposeData::actions,
             ((UChar)(baseIndex + ComposeData::MAX_BASES*comIndex)));
}

void Normalizer::explode(UnicodeString& target, uint16_t index) {
    UChar ch;
    while ((ch = ComposeData::replace[index++]) != 0)
    target += ch;
}

UChar Normalizer::pairExplode(UnicodeString& target, uint16_t action) {
    uint16_t index = ComposeData::actionIndex[action - ComposeData::MAX_COMPOSED];
    explode(target, index + 1);
    return ComposeData::replace[index];   // New base char
}

//-------------------------------------------------------------------------
// Decompose methods
//-------------------------------------------------------------------------

void
Normalizer::decompose(const UnicodeString& source, 
              bool_t compat,
              int32_t options,
              UnicodeString& result, 
              UErrorCode &status)
{
  if (U_FAILURE(status)) {
    return;
  }
  bool_t     hangul = (options & IGNORE_HANGUL) == 0;
  uint16_t     limit  = compat ? 0 : DecompData::MAX_COMPAT;
  
  result.truncate(0);
  
  for (UTextOffset i = 0; i < source.length(); ++i) {
    UChar ch = source[i];
    
    uint16_t offset = ucmp16_getu(DecompData::offsets, ch);
    
    
    if (offset > limit) {
      doAppend(DecompData::contents, offset, result);
    } else if (ch >= HANGUL_BASE && ch < HANGUL_LIMIT && hangul) {
      hangulToJamo(ch, result, limit);
    } else {
      result += ch;
    }
  }
  fixCanonical(result);
}

/**
 * Decompose starting with current input character and continuing
 * until just before the next base char.
 * <p>
 * <b>Input</b>:
 * <ul>
 *  <li>underlying char iter points to first character to decompose
 * </ul>
 * <p>
 * <b>Output:</b>
 * <ul>
 *  <li>returns first char of decomposition or DONE if at end
 *  <li>Underlying char iter is pointing at next base char or past end
 * </ul>
 */
UChar Normalizer::nextDecomp()
{
  bool_t hangul = ((fOptions & IGNORE_HANGUL) == 0);
  UChar ch = curForward();
  
  uint16_t offset = ucmp16_getu(DecompData::offsets, ch);
  
  if (offset > minDecomp || ucmp8_get(DecompData::canonClass, ch) != DecompData::BASE)
    {
      initBuffer();
      
      if (offset > minDecomp) {
    doAppend(DecompData::contents, offset, buffer);
      } else {
    buffer += ch;
      }
      bool_t needToReorder = FALSE;
      
      // Any other combining chacters that immediately follow the decomposed
      // character must be included in the buffer too, because they're
      // conceptually part of the same logical character.
      //
      // TODO: Might these need to be decomposed too?
      // (i.e. are there non-BASE characters with decompositions?
      //
      while ((ch = text->next()) != DONE
         && ucmp8_get(DecompData::canonClass, ch) != DecompData::BASE)
        {
      needToReorder = TRUE;
      buffer += ch;
        }
      
      if (buffer.length() > 1 && needToReorder) {
    // If there is more than one combining character in the buffer,
    // put them into the canonical order.
    // But we don't need to sort if only characters are the ones that
    // resulted from decomosing the base character.
    fixCanonical(buffer);
      }
      bufferLimit = buffer.length() - 1;
      ch = buffer[0];
    } else {
      // Just use this character, but first advance to the next one
      text->next();
      
      // Do Hangul -> Jamo decomposition if necessary
      if (hangul && ch >= HANGUL_BASE && ch < HANGUL_LIMIT) {
    initBuffer();
    hangulToJamo(ch, buffer, minDecomp);
    bufferLimit = buffer.length() - 1;
    ch = buffer[0];
      }
    }
  return ch;
}


/**
 * Decompose starting with the input char just before the current position
 * and continuing backward until (and including) the previous base char.
 * <p>
 * <b>Input</b>:
 * <ul>
 *  <li>underlying char iter points just after last char to decompose
 * </ul>
 * <p>
 * <b>Output:</b>
 * <ul>
 *  <li>returns last char of resulting decomposition sequence
 *  <li>underlying iter points to lowest-index char we decomposed, i.e. the base char
 * </ul>
 */
UChar Normalizer::prevDecomp() {
    bool_t hangul = (fOptions & IGNORE_HANGUL) == 0;

    UChar ch = curBackward();

    uint16_t offset = ucmp16_getu(DecompData::offsets, ch);

    if (offset > minDecomp || ucmp8_get(DecompData::canonClass, ch) != DecompData::BASE)
    {
        initBuffer();

        // Slurp up any combining characters till we get to a base char.
        while (ch != DONE && ucmp8_get(DecompData::canonClass, ch) != DecompData::BASE) {
            insert(buffer, 0, ch);
            ch = text->previous();
        }

        // Now decompose this base character
        offset = ucmp16_getu(DecompData::offsets, ch);
        if (offset > minDecomp) {
            doInsert(DecompData::contents, offset, buffer, 0);
        } else {
            // This is a base character that doesn't decompose
            // and isn't involved in reordering, so throw it back
            text->next();
        }

        if (buffer.length() > 1) {
            // If there is more than one combining character in the buffer,
            // put them into the canonical order.
            fixCanonical(buffer);
        }
        bufferLimit = bufferPos = buffer.length() - 1;
        ch = buffer[bufferPos];
    }
    else if (hangul && ch >= HANGUL_BASE && ch < HANGUL_LIMIT) {
        initBuffer();
        hangulToJamo(ch, buffer, minDecomp);
        bufferLimit = bufferPos = buffer.length() - 1;
        ch = buffer[bufferPos];
    }
    return ch;
}

uint8_t Normalizer::getClass(UChar ch) {
    return  ucmp8_get(DecompData::canonClass, ch);
}
 
/**
 * Fixes the sorting sequence of non-spacing characters according to
 * their combining class.  The algorithm is listed on p.3-11 in the
 * Unicode Standard 2.0.  The table of combining classes is on p.4-2
 * in the Unicode Standard 2.0.
 * @param result the string to fix.
 */
void Normalizer::fixCanonical(UnicodeString& result) {
    UTextOffset i = result.length() - 1;
    uint8_t currentType = getClass(result[i]);
    uint8_t lastType;
    
    for (--i; i >= 0; --i) {
        lastType = currentType;
        currentType = getClass(result[i]);
        
        //
        // a swap is presumed to be rare (and a double-swap very rare),
        // so don't worry about efficiency here.
        //
        if (currentType > lastType && lastType != DecompData::BASE) {
            // swap characters
            UChar temp = result[i];
            result[i] = result[i+1];
            result[i+1] = temp;

            // if not at end, backup (one further, to compensate for for-loop)
            if (i < result.length() - 2) {
                i += 2;
            }
            // reset type, since we swapped.
            currentType = getClass(result[i]);
        }
    }
}

    
//-------------------------------------------------------------------------
// CharacterIterator overrides
//-------------------------------------------------------------------------

/**
 * Return the current character in the normalized text.
 */
UChar Normalizer:: current() const
{
  // TODO: make this method const and guarantee that currentChar is always set?
  Normalizer *nonConst = (Normalizer*)this;
  
  if (currentChar == DONE) {
    switch (fMode) {
    case NO_OP:
      nonConst->currentChar = text->current();            
      break;
    case COMPOSE:
    case COMPOSE_COMPAT:
      nonConst->currentChar = nonConst->nextCompose();    
      break;
    case DECOMP:    
    case DECOMP_COMPAT:
      nonConst->currentChar = nonConst->nextDecomp();        
      break;
    }
  }
  return currentChar;
}

/**
 * Return the first character in the normalized text.  This resets
 * the <tt>Normalizer's</tt> position to the beginning of the text.
 */
UChar Normalizer::first() {
    return setIndex(text->startIndex());
}

/**
 * Return the last character in the normalized text.  This resets
 * the <tt>Normalizer's</tt> position to be just before the
 * the input text corresponding to that normalized character.
 */
UChar Normalizer::last() {
  text->setIndex(text->endIndex());
  
  currentChar = DONE;                     // The current char hasn't been processed
  clearBuffer();                          // The buffer is empty too
  return previous();
}

/**
 * Return the next character in the normalized text and advance
 * the iteration position by one.  If the end
 * of the text has already been reached, {@link #DONE} is returned.
 */
UChar Normalizer::next() {
  if (bufferPos < bufferLimit) {
    // There are output characters left in the buffer
    currentChar = buffer[++bufferPos];
  }
  else {
    bufferLimit = bufferPos = 0;    // Buffer is now out of date
    switch (fMode) {
    case NO_OP:
      currentChar = text->next();        
      break;
    case COMPOSE:        
    case COMPOSE_COMPAT:
      currentChar = nextCompose();    
      break;
    case DECOMP:    
    case DECOMP_COMPAT:
      currentChar = nextDecomp();        
      break;
    }
  }
  return currentChar;
}

/**
 * Return the previous character in the normalized text and decrement
 * the iteration position by one.  If the beginning
 * of the text has already been reached, {@link #DONE} is returned.
 */
UChar Normalizer::previous()
{
  if (bufferPos > 0) {
    // There are output characters left in the buffer
    currentChar = buffer[--bufferPos];
  }
  else {
    bufferLimit = bufferPos = 0;    // Buffer is now out of date
    switch (fMode) {
    case NO_OP:        
      currentChar = text->previous();    
      break;
    case COMPOSE:        
    case COMPOSE_COMPAT:
      currentChar = prevCompose();    
      break;
    case DECOMP:    
    case DECOMP_COMPAT:
      currentChar = prevDecomp();        
      break;
    }
  }
  return currentChar;
}

void Normalizer::reset() 
{
    text->setIndex(text->startIndex());
    currentChar = DONE;     // The current char hasn't been processed
    clearBuffer();          // The buffer is empty too
}

/**
 * Set the iteration position in the input text that is being normalized
 * and return the first normalized character at that position.
 * <p>
 * <b>Note:</b> This method sets the position in the <em>input</em> text,
 * while {@link #next} and {@link #previous} iterate through characters
 * in the normalized <em>output</em>.  This means that there is not
 * necessarily a one-to-one correspondence between characters returned
 * by <tt>next</tt> and <tt>previous</tt> and the indices passed to and
 * returned from <tt>setIndex</tt> and {@link #getIndex}.
 * <p>
 * @param index the desired index in the input text.
 *
 * @return      the first normalized character that is the result of iterating
 *              forward starting at the given index.
 *
 * @throws IllegalArgumentException if the given index is less than
 *          {@link #getBeginIndex} or greater than {@link #getEndIndex}.
 */
UChar Normalizer::setIndex(UTextOffset index)
{
    text->setIndex(index);   // Checks range
    currentChar = DONE;     // The current char hasn't been processed
    clearBuffer();          // The buffer is empty too

    return current();
}

/**
 * Retrieve the current iteration position in the input text that is
 * being normalized.  This method is useful in applications such as
 * searching, where you need to be able to determine the position in
 * the input text that corresponds to a given normalized output character.
 * <p>
 * <b>Note:</b> This method sets the position in the <em>input</em>, while
 * {@link #next} and {@link #previous} iterate through characters in the
 * <em>output</em>.  This means that there is not necessarily a one-to-one
 * correspondence between characters returned by <tt>next</tt> and
 * <tt>previous</tt> and the indices passed to and returned from
 * <tt>setIndex</tt> and {@link #getIndex}.
 *
 */
UTextOffset Normalizer::getIndex() const {
    return text->getIndex();
}

/**
 * Retrieve the index of the start of the input text.  This is the begin index
 * of the <tt>CharacterIterator</tt> or the start (i.e. 0) of the <tt>String</tt>
 * over which this <tt>Normalizer</tt> is iterating
 */
UTextOffset Normalizer::startIndex() const {
    return text->startIndex();
}

/**
 * Retrieve the index of the end of the input text.  This is the end index
 * of the <tt>CharacterIterator</tt> or the length of the <tt>String</tt>
 * over which this <tt>Normalizer</tt> is iterating
 */
UTextOffset Normalizer::endIndex() const {
    return text->endIndex();
}

//-------------------------------------------------------------------------
// Property access methods
//-------------------------------------------------------------------------

void
Normalizer::setMode(EMode newMode) 
{
  fMode     = newMode;
  minDecomp     = ((fMode & COMPAT_BIT) != 0) ? 0 : DecompData::MAX_COMPAT;
}

Normalizer::EMode 
Normalizer::getMode() const
{
    return fMode;
}

void
Normalizer::setOption(int32_t option, 
              bool_t value) 
{
  if (value) {
    fOptions |= option;
  } else {
    fOptions &= (~option);
  }
}

bool_t
Normalizer::getOption(int32_t option) const
{
    return (fOptions & option) != 0;
}

/**
 * Set the input text over which this <tt>Normalizer</tt> will iterate.
 * The iteration position is set to the beginning of the input text.
 */
void
Normalizer::setText(const UnicodeString& newText, 
            UErrorCode &status)
{
  if (U_FAILURE(status)) {
    return;
  }
  CharacterIterator *newIter = new StringCharacterIterator(newText);
  if (newIter == NULL) {
    status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }
  delete text;
  text = newIter;
  reset();
}

/**
 * Set the input text over which this <tt>Normalizer</tt> will iterate.
 * The iteration position is set to the beginning of the string.
 */
void
Normalizer::setText(const CharacterIterator& newText, 
            UErrorCode &status) 
{
  if (U_FAILURE(status)) {
    return;
  }
  CharacterIterator *newIter = newText.clone();
  if (newIter == NULL) {
    status = U_MEMORY_ALLOCATION_ERROR;
    return;
  }
  delete text;
  text = newIter;
  reset();
}

void
Normalizer::setText(const UChar* newText,
                    int32_t length,
            UErrorCode &status)
{
    setText(UnicodeString(newText, length), status);
}

/**
 * Copies the text under iteration into the UnicodeString referred to by "result".
 * @param result Receives a copy of the text under iteration.
 */
void
Normalizer::getText(UnicodeString&  result) 
{
    text->getText(result);
}


//-------------------------------------------------------------------------
// Private utility methods
//-------------------------------------------------------------------------


UChar Normalizer::curForward() {
    UChar ch = text->current();
    return ch;
}

UChar Normalizer::curBackward() {
    UChar ch = text->previous();
    return ch;
}

void Normalizer::doAppend(const UChar source[], uint16_t offset, UnicodeString& dest) {
    uint16_t index = offset >> STR_INDEX_SHIFT;
    uint16_t length = offset & STR_LENGTH_MASK;

    if (length == 0) {
        UChar ch;
        while ((ch = source[index++]) != 0x0000) {
            dest += ch;
        }
    } else {
        while (length-- > 0) {
            dest += source[index++];
        }
    }
}

void Normalizer::doInsert(const UChar source[], uint16_t offset, UnicodeString& dest, UTextOffset pos)
{
    uint16_t index = offset >> STR_INDEX_SHIFT;
    uint16_t length = offset & STR_LENGTH_MASK;

    if (length == 0) {
        UChar ch;
        while ((ch = source[index++]) != 0x0000) {
            insert(dest, pos++, ch);
        }
    } else {
        while (length-- > 0) {
            insert(dest, pos++, source[index++]);
        }
    }
}

void Normalizer::initBuffer() {
    buffer.truncate(0);
    clearBuffer();
}

void Normalizer::clearBuffer() {
    bufferLimit = bufferPos = 0;
}

//-----------------------------------------------------------------------------
// Hangul / Jamo conversion utilities for internal use
// See section 3.10 of The Unicode Standard, v 2.0.
//
/**
 * Convert a single Hangul syllable into one or more Jamo characters.
 * 
 * @param conjoin If TRUE, decompose Jamo into conjoining Jamo.
 */
void Normalizer::hangulToJamo(UChar ch, UnicodeString& result, uint16_t decompLimit)
{
    UChar sIndex  = (UChar)(ch - HANGUL_BASE);
    UChar leading = (UChar)(JAMO_LBASE + sIndex / JAMO_NCOUNT);
    UChar vowel   = (UChar)(JAMO_VBASE +
                          (sIndex % JAMO_NCOUNT) / JAMO_TCOUNT);
    UChar trailing= (UChar)(JAMO_TBASE + (sIndex % JAMO_TCOUNT));

    jamoAppend(leading, decompLimit, result);
    jamoAppend(vowel, decompLimit, result);
    if (trailing != JAMO_TBASE) {
        jamoAppend(trailing, decompLimit, result);
    }
}

void Normalizer::jamoAppend(UChar ch, uint16_t decompLimit, UnicodeString& dest) {
  uint16_t offset = ucmp16_getu(DecompData::offsets, ch);
    if (offset > decompLimit) {
        doAppend(DecompData::contents, offset, dest);
    } else {
        dest += ch;
    }
}

void Normalizer::jamoToHangul(UnicodeString& buffer, UTextOffset start) {
    UTextOffset out = start;
    UTextOffset limit = buffer.length() - 1;

    UTextOffset in;
    int16_t l, v, t;

    for (in = start; in < limit; in++) {
        UChar ch = buffer[in];

        if ((l = ch - JAMO_LBASE) >= 0 && l < JAMO_LCOUNT
                && (v = buffer[in+1] - JAMO_VBASE) >= 0 && v < JAMO_VCOUNT) {
            //
            // We've found a pair of Jamo characters to compose.
            // Snarf the Jamo vowel and see if there's also a trailing char
            //
            in++;   // Snarf the Jamo vowel too.

            t = (in < limit) ? buffer.charAt(in+1) : 0;
            t -= JAMO_TBASE;

            if (t >= 0 && t < JAMO_TCOUNT) {
                in++;   // Snarf the trailing consonant too
            } else {
                t = 0;  // No trailing consonant
            }
            buffer[out++] = (UChar)((l*JAMO_VCOUNT + v) * JAMO_TCOUNT + t + HANGUL_BASE);
        } else {
            buffer[out++] = ch;
        }
    }
    while (in < buffer.length()) {
        buffer[out++] = buffer[in++];
    }

    buffer.truncate(out);
}
