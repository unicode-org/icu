
/*
 * @(#)IndicLayoutEngine.h	1.4 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __INDICLAYOUTENGINE_H
#define __INDICLAYOUTENGINE_H

#include "LETypes.h"
#include "LEFontInstance.h"
#include "LEGlyphFilter.h"
#include "LayoutEngine.h"
#include "OpenTypeLayoutEngine.h"

#include "GlyphSubstitutionTables.h"
#include "GlyphDefinitionTables.h"
#include "GlyphPositioningTables.h"

#include <string.h>

/**
 * This class implements OpenType layout for Indic OpenType fonts, as
 * specified by Microsoft in "Creating and Supporting OpenType Fonts for
 * Indic Scripts" (http://www.microsoft.com/typography/otspec/indicot/default.htm)
 *
 * This class overrides the characterProcessing method to do Indic character processing
 * and reordering, and the glyphProcessing method to implement post-GSUB processing for
 * left matras. (See the MS spec. for more details)
 */
class IndicOpenTypeLayoutEngine : public OpenTypeLayoutEngine
{
public:
	/**
	 * This is the main constructor. It constructs an instance of IndicOpenTypeLayoutEngine for
	 * a particular font, script and language. It takes the GSUB table as a parameter since
	 * LayoutEngine::layoutEngineFactory has to read the GSUB table to know that it has an
	 * Indic OpenType font.
	 *
	 * @param fontInstance - the font
	 * @param scriptCode - the script
	 * @param langaugeCode - the language
	 * @param gsubTable - the GSUB table
	 *
	 * @see LayoutEngine::layoutEngineFactory
	 * @see OpenTypeLayoutEngine
	 * @see ScriptAndLangaugeTags.h for script and language codes
	 */
    IndicOpenTypeLayoutEngine(LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
                            GlyphSubstitutionTableHeader *gsubTable);

	/**
	 * This constructor is used when the font requires a "canned" GSUB table which can't be known
	 * until after this constructor has been invoked.
	 *
	 * @param fontInstance - the font
	 * @param scriptCode - the script
	 * @param langaugeCode - the language
	 *
	 * @see OpenTypeLayoutEngine
	 * @see ScriptAndLangaugeTags.h for script and language codes
	 */
    IndicOpenTypeLayoutEngine(LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode);

 	/**
	 * The destructor, virtual for correct polymorphic invocation.
	 */
   virtual ~IndicOpenTypeLayoutEngine();

protected:

	/**
	 * This method does Indic OpenType character processing. It assigns the OpenType feature
	 * tags to the characters, and may generate output characters which have been reordered. For
	 * some Indic scripts, it may also split some vowels, resulting in more output characters
	 * than input characters.
	 *
	 * Input parameters:
	 * @param chars - the input character context
	 * @param offset - the index of the first character to process
	 * @param count - the number of characters to process
	 * @param max - the number of characters in the input context
	 * @param rightToLeft - true if the characters are in a right to left directional run
	 *
	 * Output parameters:
	 * @param outChars - the output character arrayt
	 * @param charIndices - the output character index array
	 * @param featureTags - the output feature tag array
	 *
	 * @return the output character count
	 */
    virtual le_int32 characterProcessing(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft,
            LEUnicode *&outChars, le_int32 *&charIndices, const LETag **&featureTags);

	/**
	 * This method does character to glyph mapping, applies the GSUB table and applies
	 * any post GSUB fixups for left matras. It calls OpenTypeLayoutEngine::glyphProcessing
	 * to do the character to glyph mapping, and apply the GSUB table.
	 *
	 * Note that in the case of "canned" GSUB tables, the output glyph indices may be
	 * "fake" glyph indices that need to be converted to "real" glyph indices by the
	 * glyphPostProcessing method.
	 *
	 * Input parameters:
	 * @param chars - the input character context
	 * @param offset - the index of the first character to process
	 * @param count - the number of characters to process
	 * @param max - the number of characters in the input context
	 * @param rightToLeft - true if the characters are in a right to left directional run
	 * @param featureTags - the feature tag array
	 *
	 * Output parameters:
	 * @param glyphs - the output glyph index array
	 * @param charIndices - the output character index array
	 *
	 * @return the number of glyphs in the output glyph index array
	 *
	 * Note: if the character index array was already set by the characterProcessing
	 * method, this method won't change it.
	 */
    // Input: characters, tags
    // Output: glyphs, char indices
    virtual le_int32 glyphProcessing(const LEUnicode chars[], le_int32 offset, le_int32 count, le_int32 max, le_bool rightToLeft,
            const LETag **featureTags, LEGlyphID *&glyphs, le_int32 *&charIndices);
};

#if 0
/**
 * This class implements Indic OpenType layout for CDAC fonts. Since CDAC fonts don't contain
 * a GSUB table, it uses a canned GSUB table, using logical glyph indices. Each logical glyph
 * may be rendered with several physical glyphs in the CDAC font. It uses the CDACLayout class
 * to do the layout.
 *
 * @see CDACLayout
 * @see IndicOpenTypeLayout
 */
class CDACOpenTypeLayoutEngine : public IndicOpenTypeLayoutEngine
{
public:
	/**
	 * This constructs an instance of CDACOpenTypeLayoutEngine for a specific font, script and
	 * language. The scriptInfo parameter contains the information that CDACLayout needs to
	 * layout using the font, including the character to logical glyph mapping information,
	 * the canned GSUB table, and the map from logical to physical glyphs. This will be obtained
	 * by LayoutEngine::layoutEngineFactory to determine if the font is a CDAC font.
	 *
	 * @param fontInstance - the font
	 * @param scriptCode - the script
	 * @param languageCode - the language
	 * @param scriptInfo - the CDAC script information
	 *
	 * @see LEFontInstance
	 * @see CDACLayout
	 * @see ScriptAndLanguageTags.h for script and language codes
	 */
    CDACOpenTypeLayoutEngine(LEFontInstance *fontInstance, le_int32 scriptCode, le_int32 languageCode,
        const CDACLayout::ScriptInfo *scriptInfo);

 	/**
	 * The destructor, virtual for correct polymorphic invocation.
	 */
    virtual ~CDACOpenTypeLayoutEngine();

protected:
    const CDACLayout::ScriptInfo *fScriptInfo;

	/**
	 * This method converts logical glyph indices to physical glyph indices.
	 *
	 * Input paramters:
	 * @param tempGlyphs - the input "fake" glyph index array
	 * @param tempCharIndices - the input "fake" character index array
	 * @param tempGlyphCount - the number of "fake" glyph indices
	 *
	 * Output parameters:
	 * @param glyphs - the output glyph index array
	 * @param charIndices - the output character index array
	 *
	 * @return the number of glyph indices in the output glyph index array
	 */
    virtual le_int32 glyphPostProcessing(LEGlyphID tempGlyphs[], le_int32 tempCharIndices[], le_int32 tempGlyphCount,
                    LEGlyphID *&glyphs, le_int32 *&charIndices);

	/**
	 * This method maps charcters to logical glyph indices.
	 *
	 * Input parameters:
	 * @param chars - the input character context
	 * @param offset - the offset of the first character to be mapped
	 * @param count - the number of characters to be mapped
	 * @param reverse - if true, the output should be in reverse order
	 * @param mirror - if true, map characters like parenthesis to their mirror image
	 *
	 * Output parameters:
	 * @param glyphs - the glyph array
	 * @param charIndices - the character index array
	 */
    virtual void mapCharsToGlyphs(const LEUnicode chars[], le_int32 offset, le_int32 count, le_bool reverse, le_bool mirror,
        LEGlyphID *&glyphs, le_int32 *&charIndices);
};
#endif

#endif

