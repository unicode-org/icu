/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/rbbi/SimpleBITest.java,v $
 * $Date: 2003/06/03 18:49:30 $
 * $Revision: 1.8 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.rbbi;

import java.util.Locale;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.dev.test.TestFmwk;

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

    public void testWordBreak() throws Exception {
        BreakIterator wordBreak =(BreakIterator) BreakIterator.getWordInstance(new Locale("en", "US", "TEST"));
        int breaks = doTest(wordBreak);
        logln(String.valueOf(breaks));
    }

    public void testLineBreak() throws Exception {
        BreakIterator lineBreak = BreakIterator.getLineInstance(new Locale("en", "US", "TEST"));
        int breaks = doTest(lineBreak);
        logln(String.valueOf(breaks));
    }

    public void testSentenceBreak() throws Exception {
        BreakIterator sentenceBreak = BreakIterator.getSentenceInstance(new Locale("en", "US", "TEST"));
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

