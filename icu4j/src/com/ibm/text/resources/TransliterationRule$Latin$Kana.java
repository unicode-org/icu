package com.ibm.text.resources;

import java.util.ListResourceBundle;

/**
 * Rewritten April 1999 to implement Hepburn (kebon shiki)
 * transliteration.  Reference: CJKV Information Processing, Lunde,
 * 1999, pp. 30-35.
 * @author Alan Liu
 */
public class TransliterationRule$Latin$Kana extends ListResourceBundle {
    /**
     * Overrides ListResourceBundle
     */
    public Object[][] getContents() {
        return new Object[][] {
            { "Description",
                "Lowercase Latin to Hiragana; Uppercase Latin to Katakana" },

            {   "Rule",

                //------------------------------------------------------------
                // Variables
                //------------------------------------------------------------

                // Hiragana.  These are named according to the
                // regularized Nippon romanization (the naming system
                // used by Unicode).  Thus \u3062 is called "di", not
                // "ji".  "x_" is the small form of "_", e.g. "xa" is
                // small "a".

                "xa=\u3041;"
                + "a=\u3042;"
                + "xi=\u3043;"
                + "i=\u3044;"
                + "xu=\u3045;"
                + "u=\u3046;"
                + "xe=\u3047;"
                + "e=\u3048;"
                + "xo=\u3049;"
                + "o=\u304A;"

                + "ka=\u304B;"
                + "ga=\u304C;"
                + "ki=\u304D;"
                + "gi=\u304E;"
                + "ku=\u304F;"
                + "gu=\u3050;"
                + "ke=\u3051;"
                + "ge=\u3052;"
                + "ko=\u3053;"
                + "go=\u3054;"

                + "sa=\u3055;"
                + "za=\u3056;"
                + "si=\u3057;"
                + "zi=\u3058;"
                + "su=\u3059;"
                + "zu=\u305A;"
                + "se=\u305B;"
                + "ze=\u305C;"
                + "so=\u305D;"
                + "zo=\u305E;"

                + "ta=\u305F;"
                + "da=\u3060;"
                + "ti=\u3061;"
                + "di=\u3062;"
                + "xtu=\u3063;"
                + "tu=\u3064;"
                + "du=\u3065;"
                + "te=\u3066;"
                + "de=\u3067;"
                + "to=\u3068;"
                + "do=\u3069;"

                + "na=\u306A;"
                + "ni=\u306B;"
                + "nu=\u306C;"
                + "ne=\u306D;"
                + "no=\u306E;"

                + "ha=\u306F;"
                + "ba=\u3070;"
                + "pa=\u3071;"
                + "hi=\u3072;"
                + "bi=\u3073;"
                + "pi=\u3074;"
                + "hu=\u3075;"
                + "bu=\u3076;"
                + "pu=\u3077;"
                + "he=\u3078;"
                + "be=\u3079;"
                + "pe=\u307A;"
                + "ho=\u307B;"
                + "bo=\u307C;"
                + "po=\u307D;"

                + "ma=\u307E;"
                + "mi=\u307F;"
                + "mu=\u3080;"
                + "me=\u3081;"
                + "mo=\u3082;"

                + "xya=\u3083;"
                + "ya=\u3084;"
                + "xyu=\u3085;"
                + "yu=\u3086;"
                + "xyo=\u3087;"
                + "yo=\u3088;"

                + "ra=\u3089;"
                + "ri=\u308A;"
                + "ru=\u308B;"
                + "re=\u308C;"
                + "ro=\u308D;"

                + "xwa=\u308E;"
                + "wa=\u308F;"
                + "wi=\u3090;"
                + "we=\u3091;"
                + "wo=\u3092;"

                + "n=\u3093;"
                + "vu=\u3094;"

                // Katakana.  "X_" is the small form of "_", e.g. "XA"
                // is small "A".

                + "XA=\u30A1;"
                + "A=\u30A2;"
                + "XI=\u30A3;"
                + "I=\u30A4;"
                + "XU=\u30A5;"
                + "U=\u30A6;"
                + "XE=\u30A7;"
                + "E=\u30A8;"
                + "XO=\u30A9;"
                + "O=\u30AA;"

                + "KA=\u30AB;"
                + "GA=\u30AC;"
                + "KI=\u30AD;"
                + "GI=\u30AE;"
                + "KU=\u30AF;"
                + "GU=\u30B0;"
                + "KE=\u30B1;"
                + "GE=\u30B2;"
                + "KO=\u30B3;"
                + "GO=\u30B4;"

                + "SA=\u30B5;"
                + "ZA=\u30B6;"
                + "SI=\u30B7;"
                + "ZI=\u30B8;"
                + "SU=\u30B9;"
                + "ZU=\u30BA;"
                + "SE=\u30BB;"
                + "ZE=\u30BC;"
                + "SO=\u30BD;"
                + "ZO=\u30BE;"

                + "TA=\u30BF;"
                + "DA=\u30C0;"
                + "TI=\u30C1;"
                + "DI=\u30C2;"
                + "XTU=\u30C3;"
                + "TU=\u30C4;"
                + "DU=\u30C5;"
                + "TE=\u30C6;"
                + "DE=\u30C7;"
                + "TO=\u30C8;"
                + "DO=\u30C9;"

                + "NA=\u30CA;"
                + "NI=\u30CB;"
                + "NU=\u30CC;"
                + "NE=\u30CD;"
                + "NO=\u30CE;"

                + "HA=\u30CF;"
                + "BA=\u30D0;"
                + "PA=\u30D1;"
                + "HI=\u30D2;"
                + "BI=\u30D3;"
                + "PI=\u30D4;"
                + "HU=\u30D5;"
                + "BU=\u30D6;"
                + "PU=\u30D7;"
                + "HE=\u30D8;"
                + "BE=\u30D9;"
                + "PE=\u30DA;"
                + "HO=\u30DB;"
                + "BO=\u30DC;"
                + "PO=\u30DD;"

                + "MA=\u30DE;"
                + "MI=\u30DF;"
                + "MU=\u30E0;"
                + "ME=\u30E1;"
                + "MO=\u30E2;"

                + "XYA=\u30E3;"
                + "YA=\u30E4;"
                + "XYU=\u30E5;"
                + "YU=\u30E6;"
                + "XYO=\u30E7;"
                + "YO=\u30E8;"

                + "RA=\u30E9;"
                + "RI=\u30EA;"
                + "RU=\u30EB;"
                + "RE=\u30EC;"
                + "RO=\u30ED;"

                + "XWA=\u30EE;"
                + "WA=\u30EF;"
                + "WI=\u30F0;"
                + "WE=\u30F1;"
                + "WO=\u30F2;"

                + "N=\u30F3;"
                + "VU=\u30F4;"

                + "XKA=\u30F5;"
                + "XKE=\u30F6;"

                + "VA=\u30F7;"
                + "VI=\u30F8;"
                + "VE=\u30F9;"
                + "VO=\u30FA;"

                + "DOT=\u30FB;"  // Middle dot
                + "LONG=\u30FC;" // Prolonged sound mark
 
                // Categories and programmatic variables
                
                + "vowel=[aiueo];"
                + "small=\uE000;"
                + "hvr=\uE001;"
                + "hv=[{xya}{xi}{xyu}{xe}{xyo}];"

                //------------------------------------------------------------
                // Rules
                //------------------------------------------------------------
                /*
// Hepburn equivalents

shi>|si
ji>|zi
chi>|ti
// ji>|di // By default we use the ji-zi mapping
tsu>|tu
fu>|hu

sh[{vowel}>|sy
ja>|zya
// ji = zi
ju>|zyu
je>|zye
jo>|zyo
cha>|tya
// chi = ti
chu>|tyu
che>|tye
cho>|tyo
// j[{vowel} = dy{vowel}, but we use zy{vowel} by default

// Historically, m preceded b, p, or m; now n is used
// in all cases
m[b>n
m[p>n
m[m>n

// Compatibility

// 'f' group
fa>{fu}{xa}
fi>{fu}{xi}
// fu = hu
fe>{fu}{xe}
fo>{fu}{xo}

// 'jy' group; these will not round-trip, except for "jyi"
// See also the 'j' group.
jya>|zya
jyi>{zi}{xyi}
jyu>|zyu
jye>|zye
jyo>|zyo

// Nippon romanized forms

a>{a}
i>{i}
u>{u}
e>{e}
o>{o}
ka>{ka}
ki>{ki}
ku>{ku}
ke>{ke}
ko>{ko}
ga>{ga}
gi>{gi}
gu>{gu}
ge>{ge}
go>{go}
sa>{sa}
si>{si}
su>{su}
se>{se}
so>{so}
za>{za}
zi>{zi}
zu>{zu}
ze>{ze}
zo>{zo}
ta>{ta}
ti>{ti}
tu>{tu}
te>{te}
to>{to}
da>{da}
di>{di}
du>{du}
de>{de}
do>{do}
na>{na}
ni>{ni}
nu>{nu}
ne>{ne}
no>{no}
ha>{ha}
hi>{hi}
hu>{hu}
he>{he}
ho>{ho}
ba>{ba}
bi>{bi}
bu>{bu}
be>{be}
bo>{bo}
pa>{pa}
pi>{pi}
pu>{pu}
pe>{pe}
po>{po}
ma>{ma}
mi>{mi}
mu>{mu}
me>{me}
mo>{mo}
ya>{ya}
yu>{yu}
yo>{yo}
ra>{ra}
ri>{ri}
ru>{ru}
re>{re}
ro>{ro}
wa>{wa}
wi>{wi}
// No "wu"
we>{we}
wo>{wo} // Reverse {wo} to "o", not "wo"
n''>{n}
n>{n}

// Palatized Nippon romanized syllables

ky[{vowel}>{ki}|{small}
gy[{vowel}>{gi}|{small}
sy[{vowel}>{si}|{small}
zy[{vowel}>{zi}|{small}
ty[{vowel}>{ti}|{small}
dy[{vowel}>{di}|{small}
ny[{vowel}>{ni}|{small}
my[{vowel}>{mi}|{small}
hy[{vowel}>{hi}|{small}
by[{vowel}>{bi}|{small}
py[{vowel}>{pi}|{small}
ry[{vowel}>{ri}|{small}

// Doubled consonants

c[c>{xtu}
k[k>{xtu}
g[g>{xtu}
s[s>{xtu}
z[z>{xtu}
j[j>{xtu}
t[t>{xtu}
d[d>{xtu}
h[h>{xtu}
f[f>{xtu}
p[p>{xtu}
b[b>{xtu}
m[m>{xtu}
y[y>{xtu}
r[r>{xtu}
w[w>{xtu}
                */

                + "a>{a};"

                + "ba>{ba};"
                + "bi>{bi};"
                + "bu>{bu};"
                + "be>{be};"
                + "bo>{bo};"
                + "by[{vowel}>{bi}|{small};"
                + "b[b>{xtu};"

                + "da>{da};"
                + "di>{di};"
                + "du>{du};"
                + "de>{de};"
                + "do>{do};"
                + "dy[{vowel}>{di}|{small};"
                + "dh[{vowel}>{de}|{small};"
                + "d[d>{xtu};"

                + "e>{e};"

                + "fa>{hu}{xa};"
                + "fi>{hu}{xi};"
                + "fe>{hu}{xe};"
                + "fo>{hu}{xo};"
                + "fya>{hu}{xya};"
                + "fyu>{hu}{xyu};"
                + "fyo>{hu}{xyo};"
                + "f[f>{xtu};"

                + "ga>{ga};"
                + "gi>{gi};"
                + "gu>{gu};"
                + "ge>{ge};"
                + "go>{go};"
                + "gy[{vowel}>{gi}|{small};"
                + "gwa>{gu}{xwa};"
                + "gwi>{gu}{xi};"
                + "gwu>{gu}{xu};"
                + "gwe>{gu}{xe};"
                + "gwo>{gu}{xo};"
                + "g[g>{xtu};"

                + "ha>{ha};"
                + "hi>{hi};"
                + "hu>{hu};"
                + "he>{he};"
                + "ho>{ho};"
                + "hy[{vowel}>{hi}|{small};"
                + "h[h>{xtu};"

                + "i>{i};"

                + "ka>{ka};"
                + "ki>{ki};"
                + "ku>{ku};"
                + "ke>{ke};"
                + "ko>{ko};"
                + "kwa>{ku}{xwa};"
                + "kwi>{ku}{xi};"
                + "kwu>{ku}{xu};"
                + "kwe>{ku}{xe};"
                + "kwo>{ku}{xo};"
                + "ky[{vowel}>{ki}|{small};"
                + "k[k>{xtu};"

                + "ma>{ma};"
                + "mi>{mi};"
                + "mu>{mu};"
                + "me>{me};"
                + "mo>{mo};"
                + "my[{vowel}>{mi}|{small};"
                + "m[b>{n};"
                + "m[f>{n};"
                + "m[m>{n};"
                + "m[p>{n};"
                + "m[v>{n};"
                + "m''>{n};"

                + "na>{na};"
                + "ni>{ni};"
                + "nu>{nu};"
                + "ne>{ne};"
                + "no>{no};"
                + "ny[{vowel}>{ni}|{small};"
                + "nn>{n};"
                + "n''>{n};"
                + "n>{n};"

                + "o>{o};"

                + "pa>{pa};"
                + "pi>{pi};"
                + "pu>{pu};"
                + "pe>{pe};"
                + "po>{po};"
                + "py[{vowel}>{pi}|{small};"
                + "p[p>{xtu};"

                + "qa>{ku}{xa};"
                + "qi>{ku}{xi};"
                + "qu>{ku}{xu};"
                + "qe>{ku}{xe};"
                + "qo>{ku}{xo};"
                + "qy[{vowel}>{ku}|{small};"
                + "q[q>{xtu};"

                + "ra>{ra};"
                + "ri>{ri};"
                + "ru>{ru};"
                + "re>{re};"
                + "ro>{ro};"
                + "ry[{vowel}>{ri}|{small};"
                + "r[r>{xtu};"

                + "sa>{sa};"
                + "si>{si};"
                + "su>{su};"
                + "se>{se};"
                + "so>{so};"
                + "sy[{vowel}>{si}|{small};"
                + "s[sh>{xtu};"
                + "s[s>{xtu};"

                + "ta>{ta};"
                + "ti>{ti};"
                + "tu>{tu};"
                + "te>{te};"
                + "to>{to};"
                + "th[{vowel}>{te}|{small};"
                + "tsa>{tu}{xa};"
                + "tsi>{tu}{xi};"
                + "tse>{tu}{xe};"
                + "tso>{tu}{xo};"
                + "ty[{vowel}>{ti}|{small};"
                + "t[ts>{xtu};"
                + "t[ch>{xtu};"
                + "t[t>{xtu};"

                + "u>{u};"

                + "va>{VA};"
                + "vi>{VI};"
                + "vu>{vu};"
                + "ve>{VE};"
                + "vo>{VO};"
                + "vy[{vowel}>{VI}|{small};"
                + "v[v>{xtu};"

                + "wa>{wa};"
                + "wi>{wi};"
                + "we>{we};"
                + "wo>{wo};"
                + "w[w>{xtu};"

                + "ya>{ya};"
                + "yu>{yu};"
                + "ye>{i}{xe};"
                + "yo>{yo};"
                + "y[y>{xtu};"

                + "za>{za};"
                + "zi>{zi};"
                + "zu>{zu};"
                + "ze>{ze};"
                + "zo>{zo};"
                + "zy[{vowel}>{zi}|{small};"
                + "z[z>{xtu};"

                + "xa>{xa};"
                + "xi>{xi};"
                + "xu>{xu};"
                + "xe>{xe};"
                + "xo>{xo};"
                + "xka>{XKA};"
                + "xke>{XKE};"
                + "xtu>{xtu};"
                + "xwa>{xwa};"
                + "xya>{xya};"
                + "xyu>{xyu};"
                + "xyo>{xyo};"

                // optional mappings
                + "wu>{u};"

                + "ca>{ka};"
                + "ci>{si};"
                + "cu>{ku};"
                + "ce>{se};"
                + "co>{ko};"
                + "cha>{ti}{xya};"
                + "chi>{ti};"
                + "chu>{ti}{xyu};"
                + "che>{ti}{xe};"
                + "cho>{ti}{xyo};"
                + "cy[{vowel}>{ti}|{small};"
                + "c[k>{xtu};"
                + "c[c>{xtu};"

                + "fu>{hu};"

                + "ja>{zi}{xya};"
                + "ji>{zi};"
                + "ju>{zi}{xyu};"
                + "je>{zi}{xe};"
                + "jo>{zi}{xyo};"
                + "jy[{vowel}>{zi}|{small};"
                + "j[j>{xtu};"

                + "la>{ra};"
                + "li>{ri};"
                + "lu>{ru};"
                + "le>{re};"
                + "lo>{ro};"
                + "ly[{vowel}>{ri}|{small};"
                + "l[l>{xtu};"

                + "sha>{si}{xya};"
                + "shi>{si};"
                + "shu>{si}{xyu};"
                + "she>{si}{xe};"
                + "sho>{si}{xyo};"

                + "tsu>{tu};"

                + "yi>{i};"

                + "xtsu>{xtu};"
                + "xyi>{xi};"
                + "xye>{xe};"







                // Convert vowels to small form
                + "{small}a>{xya};"
                + "{small}i>{xi};"
                + "{small}u>{xyu};"
                + "{small}e>{xe};"
                + "{small}o>{xyo};"




                + "gy|{hvr}<{gi}[{hv};"
                + "gwa<{gu}{xwa};"
                + "gwi<{gu}{xi};"
                + "gwu<{gu}{xu};"
                + "gwe<{gu}{xe};"
                + "gwo<{gu}{xo};"
                + "ga<{ga};"
                + "gi<{gi};"
                + "gu<{gu};"
                + "ge<{ge};"
                + "go<{go};"

                + "ky|{hvr}<{ki}[{hv};"
                + "kwa<{ku}{xwa};"
                + "kwi<{ku}{xi};"
                + "kwu<{ku}{xu};"
                + "kwe<{ku}{xe};"
                + "kwo<{ku}{xo};"
                + "qa<{ku}{xa};"
                + "qya<{ku}{xya};"
                + "qyu<{ku}{xyu};"
                + "qyo<{ku}{xyo};"
                + "ka<{ka};"
                + "ki<{ki};"
                + "ku<{ku};"
                + "ke<{ke};"
                + "ko<{ko};"

                + "j|{hvr}<{zi}[{hv};" // Hepburn
                + "za<{za};"
                + "ji<{zi};" // Hepburn
                + "zu<{zu};"
                + "ze<{ze};"
                + "zo<{zo};"

                + "sh|{hvr}<{si}[{hv};" // Hepburn
                + "sa<{sa};"
                + "shi<{si};"
                + "su<{su};"
                + "se<{se};"
                + "so<{so};"

                + "j|{hvr}<{di}[{hv};" // Hepburn
                + "dh|{hvr}<{de}[{hv};" 
                + "da<{da};"
                + "ji<{di};" // Hepburn
                + "de<{de};"
                + "do<{do};"
                + "zu<{du};" // Hepburn

                + "ch|{hvr}<{ti}[{hv};" // Hepburn
                + "tsa<{tu}{xa};"
                + "tsi<{tu}{xi};"
                + "tse<{tu}{xe};"
                + "tso<{tu}{xo};"
                + "th|{hvr}<{te}[{hv};"
                + "ta<{ta};"
                + "chi<{ti};" // Hepburn
                + "tsu<{tu};" // Hepburn
                + "te<{te};"
                + "to<{to};"

                + "ny|{hvr}<{ni}[{hv};"
                + "na<{na};"
                + "ni<{ni};"
                + "nu<{nu};"
                + "ne<{ne};"
                + "no<{no};"

                + "by|{hvr}<{bi}[{hv};"
                + "ba<{ba};"
                + "bi<{bi};"
                + "bu<{bu};"
                + "be<{be};"
                + "bo<{bo};"

                + "py|{hvr}<{pi}[{hv};"
                + "pa<{pa};"
                + "pi<{pi};"
                + "pu<{pu};"
                + "pe<{pe};"
                + "po<{po};"

                + "hy|{hvr}<{hi}[{hv};"
                + "fa<{hu}{xa};"
                + "fi<{hu}{xi};"
                + "fe<{hu}{xe};"
                + "fo<{hu}{xo};"
                + "fya<{hu}{xya};"
                + "fyu<{hu}{xyu};"
                + "fyo<{hu}{xyo};"
                + "ha<{ha};"
                + "hi<{hi};"
                + "fu<{hu};" // Hepburn
                + "he<{he};"
                + "ho<{ho};"

                + "my|{hvr}<{mi}[{hv};"
                + "ma<{ma};"
                + "mi<{mi};"
                + "mu<{mu};"
                + "me<{me};"
                + "mo<{mo};"

                + "ya<{ya};"
                + "yu<{yu};"
                + "ye<{i}{xe};"
                + "yo<{yo};"
                + "xya<{xya};"
                + "xyu<{xyu};"
                + "xyo<{xyo};"

                + "ry|{hvr}<{ri}[{hv};"
                + "ra<{ra};"
                + "ri<{ri};"
                + "ru<{ru};"
                + "re<{re};"
                + "ro<{ro};"

                + "wa<{wa};"
                + "wi<{wi};"
                + "we<{we};"
                + "wo<{wo};"

                + "vu<{vu};"
                + "vy|{hvr}<{VI}[{hv};"
                + "v<{xtu}[{vu};"

                + "xa<{xa};"
                + "xi<{xi};"
                + "xu<{xu};"
                + "xe<{xe};"
                + "xo<{xo};"

                + "n''<{n}[{a};"
                + "n''<{n}[{i};"
                + "n''<{n}[{u};"
                + "n''<{n}[{e};"
                + "n''<{n}[{o};"
                + "n''<{n}[{na};"
                + "n''<{n}[{ni};"
                + "n''<{n}[{nu};"
                + "n''<{n}[{ne};"
                + "n''<{n}[{no};"
                + "n''<{n}[{ya};"
                + "n''<{n}[{yu};"
                + "n''<{n}[{yo};"
                + "n''<{n}[{n};"
                + "n<{n};"


                + "g<{xtu}[{ga};"
                + "g<{xtu}[{gi};"
                + "g<{xtu}[{gu};"
                + "g<{xtu}[{ge};"
                + "g<{xtu}[{go};"
                + "k<{xtu}[{ka};"
                + "k<{xtu}[{ki};"
                + "k<{xtu}[{ku};"
                + "k<{xtu}[{ke};"
                + "k<{xtu}[{ko};"

                + "z<{xtu}[{za};"
                + "z<{xtu}[{zi};"
                + "z<{xtu}[{zu};"
                + "z<{xtu}[{ze};"
                + "z<{xtu}[{zo};"
                + "s<{xtu}[{sa};"
                + "s<{xtu}[{si};"
                + "s<{xtu}[{su};"
                + "s<{xtu}[{se};"
                + "s<{xtu}[{so};"

                + "d<{xtu}[{da};"
                + "d<{xtu}[{di};"
                + "d<{xtu}[{du};"
                + "d<{xtu}[{de};"
                + "d<{xtu}[{do};"
                + "t<{xtu}[{ta};"
                + "t<{xtu}[{ti};"
                + "t<{xtu}[{tu};"
                + "t<{xtu}[{te};"
                + "t<{xtu}[{to};"


                + "b<{xtu}[{ba};"
                + "b<{xtu}[{bi};"
                + "b<{xtu}[{bu};"
                + "b<{xtu}[{be};"
                + "b<{xtu}[{bo};"
                + "p<{xtu}[{pa};"
                + "p<{xtu}[{pi};"
                + "p<{xtu}[{pu};"
                + "p<{xtu}[{pe};"
                + "p<{xtu}[{po};"
                + "h<{xtu}[{ha};"
                + "h<{xtu}[{hi};"
                + "h<{xtu}[{hu};"
                + "h<{xtu}[{he};"
                + "h<{xtu}[{ho};"


                + "r<{xtu}[{ra};"
                + "r<{xtu}[{ri};"
                + "r<{xtu}[{ru};"
                + "r<{xtu}[{re};"
                + "r<{xtu}[{ro};"

                + "w<{xtu}[{wa};"
                + "xtu<{xtu};"

                + "a<{a};"
                + "i<{i};"
                + "u<{u};"
                + "e<{e};"
                + "o<{o};"



                // Convert small forms to vowels
                + "a<{hvr}{xya};"
                + "i<{hvr}{xi};"
                + "u<{hvr}{xyu};"
                + "e<{hvr}{xe};"
                + "o<{hvr}{xyo};"              
            }
        };
    }
}



