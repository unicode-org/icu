window.BENCHMARK_DATA = {
  "lastUpdate": 1657821829824,
  "repoUrl": "https://github.com/unicode-org/icu",
  "entries": {
    "Benchmark": [
      {
        "commit": {
          "author": {
            "email": "nrunge@google.com",
            "name": "gnrunge",
            "username": "gnrunge"
          },
          "committer": {
            "email": "41129501+gnrunge@users.noreply.github.com",
            "name": "Norbert Runge",
            "username": "gnrunge"
          },
          "distinct": true,
          "id": "6df8bb7307ae9f9ebc961f0887c106f64ea05aa9",
          "message": "ICU-22036 Adds ICU4J performance tests to post-merge continuous integration\nworkflow. Also fixes a tiny oversight in the ICU4J performance framework.",
          "timestamp": "2022-06-27T08:47:48-07:00",
          "tree_id": "f40e02e24289a5397ac51ef5272fedc20596f358",
          "url": "https://github.com/unicode-org/icu/commit/6df8bb7307ae9f9ebc961f0887c106f64ea05aa9"
        },
        "date": 1656346489114,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.914427231082097,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.5060087011200292,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.8323716336555393,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.5020495988879627,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.839806573525998,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 4.009214406648371,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.8445564772300043,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.4881195419988817,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.40378562555405023,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 7.694406915726817,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.8038775404897414,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.4376905919157594,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.7645161053388603,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.754332438456954,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.6910739799402643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.9580939061571148,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 4.635470845897114,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.4016137582246968,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.4100678648903453,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.4118810146046297,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.4124925561839998,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.716840007335399,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.7920925547601008,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.4719175023525035,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.4903102096425522,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.4984333379261463,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.8200940188438404,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.826102133894544,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.843072644192281,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 4.940584187301998,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.4843754726369665,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.8193030609366185,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "allenwtsu@google.com",
            "name": "allenwtsu",
            "username": "allensu05"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "929cf40ecbf464bb133113995185c7353f2e106d",
          "message": "ICU-22059 Add one Thai word into the Thai dictionary\n\nSee #2112",
          "timestamp": "2022-06-27T09:27:56-07:00",
          "tree_id": "06e8f81c97ab94be546a5187c8e251db670d5e2f",
          "url": "https://github.com/unicode-org/icu/commit/929cf40ecbf464bb133113995185c7353f2e106d"
        },
        "date": 1656348894879,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.886905624982513,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.5070896510758334,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.8374028921395087,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.5102617525494437,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.872219192849777,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.8185230576876066,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.8266230401085184,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.5100016277651531,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.40548272695023074,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 7.801351884460061,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.8448783717321104,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.4188463109734977,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.7830223927940816,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.7363072017107566,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.672514046439756,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 1.0051927523329234,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 4.960850110450331,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.4992909394842735,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.5053964919437397,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.467738288840338,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.4579467355928641,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.6842369901690906,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.8158262127315357,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.4509671894696532,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.4529270714166547,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.450306539148194,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.7540754717732394,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.8309547994948643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.774558760747447,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 4.7333425517307575,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.4793064574258388,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.8400338038347672,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "d7c424b00f89ad2cb6734b106964ed72a3390415",
          "message": "ICU-22073 Do not throw away CompactDecimalFormat's affixes",
          "timestamp": "2022-06-27T12:53:22-07:00",
          "tree_id": "ec0d98e1ce9116d2d5820db9421d646147cb6f92",
          "url": "https://github.com/unicode-org/icu/commit/d7c424b00f89ad2cb6734b106964ed72a3390415"
        },
        "date": 1656361197925,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 3.020385848985674,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.8456144528557956,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.6852099975757258,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 2.3848262007293,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 3.0168230021352733,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.940701186048914,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 3.016226662416731,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.8709143509506754,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.33525370986651243,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 6.377168987260781,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 3.0113535289975535,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 2.380937540890999,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 3.0131875630451552,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 3.0133713007792444,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 3.0132637000582325,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.8263165699345905,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 5.698385767437136,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.880795939879669,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 2.396684730471262,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 2.4047690139587465,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 2.3957545617928977,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 3.0131623179836478,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 2.4173058992260836,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 2.3958474469888027,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.7529322132378298,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 2.3865913888617727,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 3.0118568867638054,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 3.015004164610069,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 3.01236008239528,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 6.341232564921232,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 2.3757143303011907,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 3.011483932144306,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "hsivonen@hsivonen.fi",
            "name": "Henri Sivonen",
            "username": "hsivonen"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango",
            "username": "echeran"
          },
          "distinct": true,
          "id": "3cefbd55c7aafbb29bc67aa9a27df9bf13293a5a",
          "message": "ICU-22028 Export collation and normalization data for ICU4X",
          "timestamp": "2022-06-28T08:37:32-07:00",
          "tree_id": "dc6bb456103fdf93ff19ad327327005cd0d65e6f",
          "url": "https://github.com/unicode-org/icu/commit/3cefbd55c7aafbb29bc67aa9a27df9bf13293a5a"
        },
        "date": 1656432273734,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.5113681484860773,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.7191416682847038,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.6793857884979575,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.5760474304972696,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.6860102655572127,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 2.9336021061785735,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.7083356599967883,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.5656718899430813,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.40199138686179464,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 5.2790645581273585,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.6058438949595457,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.643370693188928,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.7816012821213305,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.6664135088440672,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.727449421932952,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.950886194811231,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 4.646476872637503,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.5425957780165855,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.5765614730027853,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.5742433616096738,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.6488207808484858,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.6275288348155397,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.743712056378995,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.5725213547229164,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.5705982004893124,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.6809494659896924,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.673255554311249,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.780387474075034,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.688900267479554,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 5.08287443787475,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.575763865052329,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.6847348773558757,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nrunge@google.com",
            "name": "gnrunge",
            "username": "gnrunge"
          },
          "committer": {
            "email": "41129501+gnrunge@users.noreply.github.com",
            "name": "Norbert Runge",
            "username": "gnrunge"
          },
          "distinct": true,
          "id": "58060eafdc69ac7a060e8b80d942246adc471f0f",
          "message": "ICU-22036 Adds ICU4J date formatting performance test, which is last of the\nICU4J performance tests.",
          "timestamp": "2022-06-29T12:02:29-07:00",
          "tree_id": "c282c6da7f45a320f8d00b465f596b5b55770b50",
          "url": "https://github.com/unicode-org/icu/commit/58060eafdc69ac7a060e8b80d942246adc471f0f"
        },
        "date": 1656531058566,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.557096822630657,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.7165838221512404,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.6790779812601786,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.5797216088958321,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.5722010231819947,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 2.936658236471496,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.677318660133735,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.5695165297417237,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.4019711811499967,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 5.275600269327648,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.620356111174193,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.576481427574868,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.7161290388689556,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.5964586476619904,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.546053563451514,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.952697801160061,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 4.457427877484766,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.600119325112281,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.6499096571117464,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.6750828593245097,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.5990413092320261,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.671910210926426,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.7437171421690034,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.58361023919701,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.6571112084086899,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.6059692783680544,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.62096544313383,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.7056844437886025,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.7326327496569105,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 4.980401312170778,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.5798133518831186,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.7033090981811485,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "robertbastian@users.noreply.github.com",
            "name": "Robert Bastian",
            "username": "robertbastian"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "783b4f7b6a7f0996563c802bcec296e2a81d882f",
          "message": "ICU-22061 rename",
          "timestamp": "2022-06-29T13:16:26-07:00",
          "tree_id": "acffa8df9f2a19ac3aa86209887641a5cf2fcb49",
          "url": "https://github.com/unicode-org/icu/commit/783b4f7b6a7f0996563c802bcec296e2a81d882f"
        },
        "date": 1656535279489,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.5466021927688858,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.7108536951236835,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.6790779812601786,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.5724345609182115,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.6151061004426444,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 2.9522003513751645,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.614883225874037,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.5569174325040886,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.4018305903070499,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 5.3074741400025225,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.618259584214864,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.6522570477743497,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.5752386971846715,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.605875253955914,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.6488944173883673,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.9493160109641487,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 4.440482565523165,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.5382932022683056,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.581670996115991,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.5806592472810848,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.5919414197796027,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.7762049426855397,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.7382506629057517,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.5921160384299724,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.6609557258045766,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.5934856319730364,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.696854008649219,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.79046521066884,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.701677510529694,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 4.700153245286995,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.5833561438827966,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.716275840720669,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nrunge@google.com",
            "name": "gnrunge",
            "username": "gnrunge"
          },
          "committer": {
            "email": "41129501+gnrunge@users.noreply.github.com",
            "name": "Norbert Runge",
            "username": "gnrunge"
          },
          "distinct": true,
          "id": "58060eafdc69ac7a060e8b80d942246adc471f0f",
          "message": "ICU-22036 Adds ICU4J date formatting performance test, which is last of the\nICU4J performance tests.",
          "timestamp": "2022-06-29T12:02:29-07:00",
          "tree_id": "c282c6da7f45a320f8d00b465f596b5b55770b50",
          "url": "https://github.com/unicode-org/icu/commit/58060eafdc69ac7a060e8b80d942246adc471f0f"
        },
        "date": 1656542642713,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.5865889866712237,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.491558153146108,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.6793857884979575,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.5027902554835249,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.7039369269162887,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.3591634093091134,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.652191094179318,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.5128522415001142,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.4020359205163043,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 5.381061743756383,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.708370281358924,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.5009883721290125,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.8025938966243045,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.731246317238603,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.790888571316092,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.9543064812335845,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 4.584199439996896,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.5105106028998474,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.5024533164869915,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.4904876511604335,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.502535588916863,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.583737446390565,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.8030843438782682,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.503858391078289,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.5816854546292225,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.5027652251624912,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.628693420034403,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.587012859803293,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.6478383763943643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 4.6284097857622655,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.5004631130053798,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.4800997585841253,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "cyndy_ishida@apple.com",
            "name": "Cyndy Ishida",
            "username": "cyndyishida"
          },
          "committer": {
            "email": "cyndy_ishida@apple.com",
            "name": "Cyndy Ishida",
            "username": "cyndyishida"
          },
          "distinct": true,
          "id": "e2ae4f5324e863a92bf79892ef1a2e0d6b183af7",
          "message": "ICU-22054 Remove declarations for unimplemented APIs\n\nThis patch also includes marking `=delete` on specific `normal` member functions, as opposed to compiler generated functions,\nbased on the description of such functions' surrounding comments.",
          "timestamp": "2022-07-01T08:57:10-07:00",
          "tree_id": "fda6935be0dbf84c2fec7832622127bc05ca0b48",
          "url": "https://github.com/unicode-org/icu/commit/e2ae4f5324e863a92bf79892ef1a2e0d6b183af7"
        },
        "date": 1656692515914,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 3.0381672093070935,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.842476273029451,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.6853629961320276,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 2.398150361018323,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 3.016479118293647,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.7997860874673224,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 3.0143758142040404,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.8753734959121142,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.33562468369714776,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 6.3835577821253136,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 3.014078068667276,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 2.3875952345155675,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 3.014264116263308,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 3.0143758142040404,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 3.0138770008017413,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.8267832919077376,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 6.353693085569385,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.8692917273879721,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 2.3694662812851575,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 2.3871904077371155,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 2.36575403619991,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 3.0124673797811106,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 2.456307024034196,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 2.3963577046802906,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 2.9284574248740403,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 2.38301298958317,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 3.01236008239528,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 3.010952787238061,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 3.0185372920274616,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 6.339798757224832,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 2.3021398114679625,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 3.011452486631115,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "daschuer@mixxx.org",
            "name": "Daniel Schürmann",
            "username": "daschuer"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "rp9-next",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "9f358ccb2472866a849aa4f393ef4528448c3e9d",
          "message": "ICU-22010 Add early check for AX_CHECK_COMPILE_FLAG\n\nThis helps to avoid missleading error message:\n\n```\n./source/configure: line 7981: syntax error near unexpected token 'newline'\n./source/configure: line 7981: 'AX_CHECK_COMPILE_FLAG('\n```",
          "timestamp": "2022-07-12T13:30:01+05:30",
          "tree_id": "cb81138f619f83e5a8667ed95e034c4d81ae9b8b",
          "url": "https://github.com/unicode-org/icu/commit/9f358ccb2472866a849aa4f393ef4528448c3e9d"
        },
        "date": 1657614296514,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 3.0263638711361063,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.810942369877974,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.684079271502572,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 2.07674071667404,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 3.014978749660427,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.5258959963757652,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 3.0153721412717878,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.7839144969695906,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.3352872452981286,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 6.375902871224394,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 3.012393535811967,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 2.077156147903621,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 3.0136055660340095,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 3.0127284633545828,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 3.0144685236088353,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.825118401593594,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 5.703649788259346,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.7838958634921331,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 2.0769559659864756,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 2.077456489631572,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 2.077034366324052,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 3.012158253272387,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 2.333404359249625,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 2.077156147903621,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.7967467907577352,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 2.077664318412047,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 3.012054957869813,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 3.06470385721861,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 3.0118647657823545,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 6.342344433484166,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 2.077456489631572,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 3.0118568867638054,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "721d41153ec4f80cd8556ac57dc023d169ef9dd2",
          "message": "ICU-22071 Fixed DateTimePatternGenerator to respect the locale's \"rg\" subtag (when it has one) when determining the hour cycle.",
          "timestamp": "2022-07-12T10:55:03-07:00",
          "tree_id": "c8298bfaaccafa158a689ec1bfd5c56a0cc8f44d",
          "url": "https://github.com/unicode-org/icu/commit/721d41153ec4f80cd8556ac57dc023d169ef9dd2"
        },
        "date": 1657649994987,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.581733418800758,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.4916714589515119,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.6797525790673018,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.4991229459969864,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.596149750655774,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.3611371169077136,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.6203807348710106,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.5551048846829627,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.40202195881155095,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 5.372585925941012,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.5555868934137083,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.4862495459794312,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.591623609589878,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.842653478598144,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.720755693040305,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.9500873778609703,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 4.4939531162653,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.5130144242988743,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.5005078555182594,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.4998114583174067,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.5038309352130468,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.6186964140532365,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.8236037962874223,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.4973795222708561,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.5615086608888589,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.4915749193492247,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.862139284369926,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.637344287785814,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.711430322588759,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 4.932243421876515,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.5018966446702726,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.599988906713998,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "committer": {
            "email": "62772518+richgillam@users.noreply.github.com",
            "name": "Rich Gillam",
            "username": "richgillam"
          },
          "distinct": true,
          "id": "4f6f087f2eaa4011237ee0c4e4cf2b1e47ea70fc",
          "message": "ICU-22085 Fix old typo on calculating path size in loadTestData() and add a few small changes to support running ICU unit tests in Xcode.",
          "timestamp": "2022-07-14T08:53:16-07:00",
          "tree_id": "35a3d64dc578f2aac68d1255b7f91072d95f1e29",
          "url": "https://github.com/unicode-org/icu/commit/4f6f087f2eaa4011237ee0c4e4cf2b1e47ea70fc"
        },
        "date": 1657815488787,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.6553081550787483,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 2.213137207951547,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.6805618289660933,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.657769990958833,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.625483044537061,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 4.742282866839388,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.624145270699055,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.7068818108324193,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.40201137023029565,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 5.3888416835086215,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.645190168360587,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.6435194241870172,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.668665960948574,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.6377142209813353,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.721829134966542,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.9535193540149834,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 4.96722175827474,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.5896525301997302,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.5799571264453824,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.5780333091542074,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.6466807579087308,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.7157773566615817,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.7442303135250117,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.573005975961091,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.6325122455568435,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.577975115394374,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.640959521539395,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.7471095371890426,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.7341567683893717,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 4.6860022783359945,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.5773123160755178,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.674267891346442,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "william.marlow@ibm.com",
            "name": "William Marlow",
            "username": "lux01"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "9a7b0e08d0bb8ed11775c76f935eabc155fdc795",
          "message": "ICU-22086 Add ibm-clang_r/ibm-clang++_r to runConfigureICU",
          "timestamp": "2022-07-14T17:38:59Z",
          "tree_id": "0fedd85fc633e091452763491c5688581ca32177",
          "url": "https://github.com/unicode-org/icu/commit/9a7b0e08d0bb8ed11775c76f935eabc155fdc795"
        },
        "date": 1657821825353,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 4.146852158589238,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 2.317012826037247,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.866906554483704,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 2.7175519153850924,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 4.268747079365905,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 4.557029110472314,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 4.222214965052399,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 2.1243049196019044,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.3875095647274525,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 8.542317245288052,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 4.087213734621837,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 2.7768479429343422,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 3.7390997925163654,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 4.1473820682333615,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 4.247501567014391,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.962399419279458,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 7.726217243535811,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 2.1458406886285477,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 2.7576632061486004,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 2.738958098389976,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 2.777815065047672,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 4.2699262976040995,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 2.821323311663848,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 2.6428240287176514,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 3.1804690712168098,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 2.749040109684162,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 4.23197367303008,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 4.21091777231988,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 4.228834999789279,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 7.240024274166547,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 2.7289925232631935,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 4.282242103306997,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      }
    ]
  }
}