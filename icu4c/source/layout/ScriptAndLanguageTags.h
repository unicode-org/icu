/*
 * @(#)ScriptAndLanguageTags.h	1.4 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __SCRIPTANDLANGUAGETAGS_H
#define __SCRIPTANDLANGUAGETAGS_H

#include "LETypes.h"
#include "LEScripts.h"

// Script tags
const LETag arabScriptTag = 0x61726162; // 'arab'
const LETag armnScriptTag = 0x61726D6E; // 'armn' **** Armenian: no MS definition ****
const LETag bengScriptTag = 0x62656E67; // 'beng'
const LETag bpmfScriptTag = 0x62706D66; // 'bpmf' **** Bopomofo: no MS definition ****
const LETag cyrlScriptTag = 0x6379726C; // 'cyrl'
const LETag devaScriptTag = 0x64657661; // 'deva'
const LETag grekScriptTag = 0x6772656B; // 'grek'
const LETag grgnScriptTag = 0x6772676E; // 'grgn' **** Georgian: no MS definition ****
const LETag gujrScriptTag = 0x67756A72; // 'gujr'
const LETag hangScriptTag = 0x68616E67; // 'hang'
const LETag haniScriptTag = 0x68616E69; // 'hani'
const LETag hebrScriptTag = 0x68656272; // 'hebr'
const LETag kanaScriptTag = 0x6B616E61; // 'kana'
const LETag knbnScriptTag = 0x6B6E626E; // 'knbn' **** Kanbun: no MS definition ****
const LETag kndaScriptTag = 0x6B6E6461; // 'knda'
const LETag laoScriptTag  = 0x6C616F20; // 'lao ' **** Lao: no MS definition ****
const LETag latnScriptTag = 0x6C61746E; // 'latn'
const LETag mlymScriptTag = 0x6D6C796D; // 'mlym'
const LETag oryaScriptTag = 0x6F727961; // 'orya'
const LETag punjScriptTag = 0x70756E6A; // 'punj' punjabi == gurmukhi
const LETag tamlScriptTag = 0x74616D6C; // 'taml'
const LETag teluScriptTag = 0x74656C75; // 'telu'
const LETag thaiScriptTag = 0x74686169; // 'thai'
const LETag tibtScriptTag = 0x74696174; // 'tibt'

const LETag neutScriptTag = 0x4E455554; // 'NEUT'
const LETag puseScriptTag = 0x50555345; // 'PUSE'
const LETag spclScriptTag = 0x5350434C; // 'SPCL'
const LETag surrScriptTag = 0x53555252; // 'SURR'

const LETag nullScriptTag = 0x00000000; // ''

    // Langauge tags
const LETag araLangSysTag = 0x41524120; // 'ARA ' Arabic
const LETag asmLangSysTag = 0x41534D20; // 'ASM ' Assamese
const LETag benlangSysTag = 0x42454E20; // 'BEN ' Bengali
const LETag gujLangSysTag = 0x47554A20; // 'GUJ ' Gujarati
const LETag hndLangSysTag = 0x484E4420; // 'HND ' Hindi
const LETag kanLangSysTag = 0x4B414E20; // 'KAN ' Kannada
const LETag kokLangSysTag = 0x4B4F4B20; // 'KOK ' Konkani
const LETag malLangSysTag = 0x4D414C20; // 'MAL ' Malayalam (old style)
const LETag marLangSysTag = 0x4D415320; // 'MAR ' Marathi
const LETag mlrLangSysTag = 0x4D4C5320; // 'MLR ' Malayalam (reformed)
const LETag nepLangSysTag = 0x4E455020; // 'NEP ' Nepali
const LETag oriLangSysTag = 0x4F524920; // 'ORI ' Oriya
const LETag panLangSysTag = 0x50414E20; // 'PAN ' Punjabi
const LETag sanLangSysTag = 0x53414E20; // 'SAN ' Sanskrit
const LETag tamLangSysTag = 0x54414D20; // 'TAM ' Tamil
const LETag telLangSysTag = 0x54454C20; // 'TEL ' Telegu

const LETag noLangSysTag  = 0x00000000; // ''


#endif
