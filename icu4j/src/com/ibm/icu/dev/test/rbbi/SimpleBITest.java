/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.rbbi;

import java.io.IOException;
import java.io.InputStream;
import java.util.ListResourceBundle;
import java.util.MissingResourceException;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.DictionaryBasedBreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;

// TODO: {dlf} this test currently doesn't test anything!
// You'll notice that the resource that uses the dictionary isn't even on the resource path,
// so the dictionary never gets used.  Good thing, too, because it would throw a security
// exception if run with a security manager.  Not that it would matter, the dictionary 
// resource isn't even in the icu source tree!
// In order to fix this:
// 1) make sure english.dict matches the current dictionary format required by dbbi
// 2) make sure english.dict gets included in icu4jtests.jar
// 3) have this test use getResourceAsStream to get a stream on the dictionary, and
//    directly instantiate a DictionaryBasedBreakIterator.  It can use the rules from
//    the appropriate section of ResourceBundle_en_US_TEST.  I'd suggest just copying
//    the rules into this file.
// 4) change the test text by inserting '|' at word breaks, and '||' at line breaks.  
// 5) process this text to a) create tables of break indices, and b) clean up the test
//    for the break iterator to work on
// 
// This would NOT test the ability to load dictionary-based break iterators through our
// normal resource mechanism.  One could install such a break iterator and its
// resources into the icu4j jar, and it would work, but there's no way to register entire
// resources from outside yet.  Even if there were, the access restrictions are a bit
// difficult to manage, if one wanted to register a break iterator whose code and data
// resides outside the icu4j jar.  Since the code to instantiate would be going through 
// two protection domains, each domain would have to allow access to the data-- but 
// icu4j's domain wouldn't know about ours.  So we could instantiate before registering
// the break iterator, but this would mean we'd have to fully initialize the dictionary(s)
// at instantiation time, rather than let this be deferred until they are actually needed.
//
// I've done items 2 and 3 above.  Unfortunately, since I haven't done item 1, the
// dictionary builder crashes.  So for now I'm disabling this test.  This is not
// that important, since we have a thai dictionary that we do test thoroughly.
//

public class SimpleBITest extends TestFmwk{
    public static final String testText =
//        "The rain in Spain stays mainly on the plain.  The plains in Spain are mainly pained with rain.";
//"one-two now--  Hah!  You owe me exactly $1,345.67...  Pay up, huh?  By the way, why don't I send you my re\u0301sume\u0301?  This is a line\r\nbreak.";
//"nowisthetimeforallgoodmen...  tocometothehelpoftheircountry";
"When, in the course of human events, it becomes necessary for one people to dissolve the political bonds which have "
//"When,inthecourseofhumanevents,itbecomesnecessaryforonepeopletodissolvethepoliticalbondswhichhave"
+ "connectedthemwithanother,andtoassumeamongthepowersoftheearth,theseparateandequalstationtowhichthelaws"
+ "ofnatureandofnature'sGodentitlethem,adecentrespecttotheopinionsofmankindrequiresthattheyshoulddeclarethe"
+ "causeswhichimpelthemtotheseparation\n"
+ "Weholdthesetruthstobeself-evident,thatallmenarecreatedequal,thattheyareendowedbytheirCreatorwithcertain"
+ "unalienablerights,thatamongthesearelife,libertyandthepursuitofhappiness.Thattosecuretheserights,governmentsare"
+ "institutedamongmen,derivingtheirjustpowersfromtheconsentofthegoverned.Thatwheneveranyformofgovernment"
+ "becomesdestructivetotheseends,itistherightofthepeopletoalterortoabolishit,andtoinstitutenewgovernment,laying"
+ "itsfoundationonsuchprinciplesandorganizingitspowersinsuchform,astothemshallseemmostlikelytoeffecttheirsafety"
+ "andhappiness.Prudence,indeed,willdictatethatgovernmentslongestablishedshouldnotbechangedforlightandtransient"
+ "causes;andaccordinglyallexperiencehathshownthatmankindaremoredisposedtosuffer,whileevilsaresufferable,than"
+ "torightthemselvesbyabolishingtheformstowhichtheyareaccustomed.Butwhenalongtrainofabusesandusurpations,"
+ "pursuinginvariablythesameobjectevincesadesigntoreducethemunderabsolutedespotism,itistheirright,itistheirduty,"
+ "tothrowoffsuchgovernment,andtoprovidenewguardsfortheirfuturesecurity.--Suchhasbeenthepatientsufferanceof"
+ "thesecolonies;andsuchisnowthenecessitywhichconstrainsthemtoaltertheirformersystemsofgovernment.Thehistory"
+ "ofthepresentKingofGreatBritainisahistoryofrepeatedinjuriesandusurpations,allhavingindirectobjectthe"
+ "establishmentofanabsolutetyrannyoverthesestates.Toprovethis,letfactsbesubmittedtoacandidworld.\n"
+ "Hehasrefusedhisassenttolaws,themostwholesomeandnecessaryforthepublicgood.\n"
+ "Hehasforbiddenhisgovernorstopasslawsofimmediateandpressingimportance,unlesssuspendedintheiroperationtill"
+ "hisassentshouldbeobtained;andwhensosuspended,hehasutterlyneglectedtoattendtothem.\n"
+ "Hehasrefusedtopassotherlawsfortheaccommodationoflargedistrictsofpeople,unlessthosepeoplewouldrelinquish"
+ "therightofrepresentationinthelegislature,arightinestimabletothemandformidabletotyrantsonly.\n"
+ "Hehascalledtogetherlegislativebodiesatplacesunusual,uncomfortable,anddistantfromthedepositoryoftheirpublic"
+ "records,forthesolepurposeoffatiguingthemintocompliancewithhismeasures.\n"
+ "Hehasdissolvedrepresentativehousesrepeatedly,foropposingwithmanlyfirmnesshisinvasionsontherightsofthepeople.\n"
+ "Hehasrefusedforalongtime,aftersuchdissolutions,tocauseotherstobeelected;wherebythelegislativepowers,"
+ "incapableofannihilation,havereturnedtothepeopleatlargefortheirexercise;thestateremaininginthemeantimeexposed"
+ "toallthedangersofinvasionfromwithout,andconvulsionswithin.\n"
+ "Hehasendeavoredtopreventthepopulationofthesestates;forthatpurposeobstructingthelawsfornaturalizationof"
+ "foreigners;refusingtopassotherstoencouragetheirmigrationhither,andraisingtheconditionsofnewappropriationsof"
+ "lands.\n"
+ "Hehasobstructedtheadministrationofjustice,byrefusinghisassenttolawsforestablishingjudiciarypowers.\n"
+ "Hehasmadejudgesdependentonhiswillalone,forthetenureoftheiroffices,andtheamountandpaymentoftheirsalaries.\n"
+ "Hehaserectedamultitudeofnewoffices,andsenthitherswarmsofofficerstoharassourpeople,andeatouttheir"
+ "substance.\n"
+ "Hehaskeptamongus,intimesofpeace,standingarmieswithouttheconsentofourlegislature.\n"
+ "Hehasaffectedtorenderthemilitaryindependentofandsuperiortocivilpower.\n"
+ "Hehascombinedwithotherstosubjectustoajurisdictionforeigntoourconstitution,andunacknowledgedbyourlaws;"
+ "givinghisassenttotheiractsofpretendedlegislation:\n"
+ "Forquarteringlargebodiesofarmedtroopsamongus:\n"
+ "Forprotectingthem,bymocktrial,frompunishmentforanymurderswhichtheyshouldcommitontheinhabitantsofthese"
+ "states:\n"
+ "Forcuttingoffourtradewithallpartsoftheworld:\n"
+ "Forimposingtaxesonuswithoutourconsent:\n"
+ "Fordeprivingusinmanycases,ofthebenefitsoftrialbyjury:\n"
+ "Fortransportingusbeyondseastobetriedforpretendedoffenses:\n"
+ "ForabolishingthefreesystemofEnglishlawsinaneighboringprovince,establishingthereinanarbitrarygovernment,and"
+ "enlargingitsboundariessoastorenderitatonceanexampleandfitinstrumentforintroducingthesameabsoluteruleinthese"
+ "colonies:\n"
+ "Fortakingawayourcharters,abolishingourmostvaluablelaws,andalteringfundamentallytheformsofourgovernments:\n"
+ "Forsuspendingourownlegislatures,anddeclaringthemselvesinvestedwithpowertolegislateforusinallcaseswhatsoever.\n"
+ "Hehasabdicatedgovernmenthere,bydeclaringusoutofhisprotectionandwagingwaragainstus.\n"
+ "Hehasplunderedourseas,ravagedourcoasts,burnedourtowns,anddestroyedthelivesofourpeople.\n"
+ "Heisatthistimetransportinglargearmiesofforeignmercenariestocompletetheworksofdeath,desolationandtyranny,"
+ "alreadybegunwithcircumstancesofcrueltyandperfidyscarcelyparalleledinthemostbarbarousages,andtotalyunworth"
+ "theheadofacivilizednation.\n"
+ "Hehasconstrainedourfellowcitizenstakencaptiveonthehighseastobeararmsagainsttheircountry,tobecomethe"
+ "executionersoftheirfriendsandbrethren,ortofallthemselvesbytheirhands.\n"
+ "Hehasexciteddomesticinsurrectionsamongstus,andhasendeavoredtobringontheinhabitantsofourfrontiers,the"
+ "mercilessIndiansavages,whoseknownruleofwarfare,isundistinguisheddestructionofallages,sexesandconditions.\n"
+ "Ineverystageoftheseoppressionswehavepetitionedforredressinthemosthumbleterms:ourrepeatedpetitionshave"
+ "beenansweredonlybyrepeatedinjury.Aprince,whosecharacteristhusmarkedbyeveryactwhichmaydefineatyrant,is"
+ "unfittobetherulerofafreepeople.\n"
+ "NorhavewebeenwantinginattentiontoourBritishbrethren.Wehavewarnedthemfromtimetotimeofattemptsbytheir"
+ "legislaturetoextendanunwarrantablejurisdictionoverus.Wehaveremindedthemofthecircumstancesofouremigration"
+ "andsettlementhere.Wehaveappealedtotheirnativejusticeandmagnanimity,andwehaveconjuredthembythetiesofour"
+ "commonkindredtodisavowtheseusurpations,which,wouldinevitablyinterruptourconnectionsandcorrespondence.We"
+ "must,therefore,acquiesceinthenecessity,whichdenouncesourseparation,andholdthem,asweholdtherestofmankind,"
+ "enemiesinwar,inpeacefriends.\n"
+ "We,therefore,therepresentativesoftheUnitedStatesofAmerica,inGeneralCongress,assembled,appealingtothe"
+ "SupremeJudgeoftheworldfortherectitudeofourintentions,do,inthename,andbytheauthorityofthegoodpeopleof"
+ "thesecolonies,solemnlypublishanddeclare,thattheseunitedcoloniesare,andofrightoughttobefreeandindependent"
+ "states;thattheyareabsolvedfromallallegiancetotheBritishCrown,andthatallpoliticalconnectionbetweenthemandthe"
+ "stateofGreatBritain,isandoughttobetotallydissolved;andthatasfreeandindependentstates,theyhavefullpowerto"
+ "leveywar,concludepeace,contractalliances,establishcommerce,andtodoallotheractsandthingswhichindependent"
+ "statesmayofrightdo.Andforthesupportofthisdeclaration,withafirmrelianceontheprotectionofDivineProvidence,we"
+ "mutuallypledgetoeachotherourlives,ourfortunesandoursacredhonor.\n";

    public static void main(String[] args) throws Exception {
        new SimpleBITest().run(args);
    }
    
    protected boolean validate() {
        // TODO: remove when english.dict gets fixed
        return false;
    }

    private BreakIterator createTestIterator(int kind) {
        final String bname = "com.ibm.icu.dev.test.rbbi.BreakIteratorRules_en_US_TEST";

        BreakIterator iter = null;

        ListResourceBundle bundle = null;
        try {
            Class cls = Class.forName(bname);
            bundle = (ListResourceBundle)cls.newInstance();
        }
        catch (Exception e) {
            ///CLOVER:OFF
            errln("could not create bundle: " + bname + "exception: " + e.getMessage());
            ///CLOVER:ON
            return null;
        }
        
        final String[] kindNames = {
            "Character", "Word", "Line", "Sentence"
        };
        String rulesName = kindNames[kind] + "BreakRules";
        String dictionaryName = kindNames[kind] + "BreakDictionary";
        
        String[] classNames = bundle.getStringArray("BreakIteratorClasses");
        String rules = bundle.getString(rulesName);
        if (classNames[kind].equals("RuleBasedBreakIterator")) {
            iter = new RuleBasedBreakIterator(rules);
        }
        else if (classNames[kind].equals("DictionaryBasedBreakIterator")) {
            try {
                String dictionaryPath = bundle.getString(dictionaryName);
                InputStream dictionary = bundle.getClass().getResourceAsStream(dictionaryPath);
                System.out.println("looking for " + dictionaryPath + " from " + bundle.getClass() + " returned " + dictionary);
                iter = new DictionaryBasedBreakIterator(rules, dictionary);
            }
            catch(IOException e) {
                e.printStackTrace();
                errln(e.getMessage());
                System.out.println(e); // debug
            }
            catch(MissingResourceException e) {
                errln(e.getMessage());
                System.out.println(e); // debug
            }
        }
        if (iter == null) {
            errln("could not create iterator");
        }
        
        return iter;
    }
    
    public void testWordBreak() throws Exception {
        BreakIterator wordBreak = createTestIterator(BreakIterator.KIND_WORD);
        int breaks = doTest(wordBreak);
        logln(String.valueOf(breaks));
    }

    public void testLineBreak() throws Exception {
        BreakIterator lineBreak = createTestIterator(BreakIterator.KIND_LINE);
        int breaks = doTest(lineBreak);
        logln(String.valueOf(breaks));
    }

    public void testSentenceBreak() throws Exception {
        BreakIterator sentenceBreak = createTestIterator(BreakIterator.KIND_SENTENCE);
        int breaks = doTest(sentenceBreak);
        logln(String.valueOf(breaks));
    }

    private int doTest(BreakIterator bi) {
        // forward
        bi.setText(testText);
        int p = bi.first();
        int lastP = p;
        String fragment;
        int breaks = 0;
        logln("Forward...");
        while (p != BreakIterator.DONE) {
            p = bi.next();
            if (p != BreakIterator.DONE) {
                fragment = testText.substring(lastP, p);
            } else {
                fragment = testText.substring(lastP);
            }
            debugPrintln(": >" + fragment + "<");
            ++breaks;
            lastP = p;
        }
        return breaks;
    }

    private void debugPrintln(String s) {
        final String zeros = "0000";
        String temp;
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= ' ' && c < '\u007f')
                out.append(c);
            else {
                out.append("\\u");
                temp = Integer.toHexString((int)c);
                out.append(zeros.substring(0, 4 - temp.length()));
                out.append(temp);
            }
        }
        logln(out.toString());
    }

    private void debugPrintln2(String s) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '\u0100')
                out.append("<" + ((int)c - 0x100) + ">");
            else
                out.append(c);
        }
        logln(out.toString());
    }
}

