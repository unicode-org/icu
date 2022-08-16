window.BENCHMARK_DATA = {
  "lastUpdate": 1660672225503,
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
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "6394a48d067134a99faca4c7329accee0c580301",
          "message": "ICU-21957 integrate CLDR release-42-m2 (mid milestone) to ICU main for 72",
          "timestamp": "2022-07-14T10:56:39-07:00",
          "tree_id": "313c19455be6773aa335e6e10e0b9518d142b996",
          "url": "https://github.com/unicode-org/icu/commit/6394a48d067134a99faca4c7329accee0c580301"
        },
        "date": 1657822898636,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 3.2603777537676124,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 2.209271604964246,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.8734320814579797,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 2.6113667533582743,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 3.2640629266614853,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 4.206809900804879,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 3.4173851843762035,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 2.0876314667455733,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.3617904777337523,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 6.880670451612146,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 3.2538157537514807,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 2.57255956610771,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 3.2731715594357533,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 3.2622602074745326,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 3.3439581463970183,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.9092988264918562,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 6.924051731466788,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 2.0236282067222016,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 2.5866310481417742,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 2.5979243543062567,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 2.5745845803984397,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 3.2500058444980957,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 2.616821339532585,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 2.6076509843169693,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.9090984939435207,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 2.505919049339389,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 3.2779387667863022,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 3.271041436649558,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 3.2266705126337536,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 6.859537723707655,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 2.600249117765431,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 3.267670560438451,
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
          "id": "a8594a401f3241b19f9b8b61bf786fde917b4c72",
          "message": "ICU-22056 Add a new unum_hasAttribute() method.",
          "timestamp": "2022-07-15T16:03:56-07:00",
          "tree_id": "1db5acef8d7251a4829b61c61c686f5822486667",
          "url": "https://github.com/unicode-org/icu/commit/a8594a401f3241b19f9b8b61bf786fde917b4c72"
        },
        "date": 1657927734511,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.543003618462409,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.3540834209843369,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.6420112100203241,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.3274901988897547,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.543263861783329,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.3599931558631497,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.4599540842654983,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.2942728649880058,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.35258626578934316,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 6.736397570964882,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.4580827886574257,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.342149566962059,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.3463165156403067,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.1601629452204114,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.2018550894418474,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.7849060198342633,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 3.755063134036126,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.1404958217065815,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.1432494814245913,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.1331139338805536,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.1407362346492778,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.176395427621082,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.4207335967890387,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.1281691522949227,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.226736014049493,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.3217427875421763,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.5139564991804146,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.4893446956297787,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.4940803280363926,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 4.325203966352938,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.2962256692352234,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.4797431427012735,
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
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "9c72bf975890a1edacce046a2e55e88023a86389",
          "message": "ICU-22087 Export a non-recursive canonical decomposition supplement for ICU4X",
          "timestamp": "2022-07-18T10:05:10-07:00",
          "tree_id": "cf9c0091bce7abf7054b71884fd644acc42cae16",
          "url": "https://github.com/unicode-org/icu/commit/9c72bf975890a1edacce046a2e55e88023a86389"
        },
        "date": 1658165581634,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.5496036957098824,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.4909974984790215,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.67880783359092,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.5028465375448816,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.8498961506457,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.357403818374422,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.6126548322298593,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.5138505132993378,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.4018994123255998,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 5.3965256028209465,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.6976834415111504,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.4986492225056631,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.7713866462535535,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.5779147825516784,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.855420541826559,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.954641471188698,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 4.575080042945141,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.5141467259272883,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.486444773912215,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.4919406502816883,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.5022384019048862,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.663804556072234,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.8234086109550731,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.502770926047685,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.5640528777175586,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.502304786478206,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.7020570820525416,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.6082928793908784,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.507571183327886,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 4.851802970334858,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.5032536438094386,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.6160837715112844,
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
          "id": "9d230f923c51bb72af9420e8cf53a019894a8e52",
          "message": "ICU-21939 Fix bogus \"conflicting fields\" error in DateIntervalFormat.",
          "timestamp": "2022-07-18T15:16:40-07:00",
          "tree_id": "c0319f3181cebd91a769eaebd21495a02b16a9cb",
          "url": "https://github.com/unicode-org/icu/commit/9d230f923c51bb72af9420e8cf53a019894a8e52"
        },
        "date": 1658184203323,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.7098107873285056,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.6542451234834048,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.6794058664752398,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.6647701331590288,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.8034253398866773,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 2.936021547687263,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.6841019392536167,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.5451427823357355,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.40198922452468916,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 5.405887115762264,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.603954377781867,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.5849511639007245,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.662504073827826,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.6001310630975683,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.6039640371447583,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.951693604092732,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 4.445259823894019,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.5677725088194694,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.5883825641309055,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.6328515093861748,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.5755107261842336,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.8583514921462276,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.7438018196518976,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.6006427196249502,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.6694967408054209,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.5756743780410327,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.5382961758126523,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.712407653236301,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.588462924302452,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 4.62125187799335,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.658368588263895,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.637032021076912,
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
          "id": "86166e0a2dd1119e086d04b8973dc486b4af8dcc",
          "message": "ICU-22075 Adds a random waiting period (0 - 179 secs) to each test that runs as\npart of a high concurrency performance test setup. This will disperse commits\nof performance test results into the data branch over a wider time frame and\ndrastically reduces the chance of data uploads failing after ten unsuccesful\ncommit attempts.\n\nTest runs showed a huge drop in failed commits/retry, from a high of 113\nwithout wait down to only 4 with the extra wait.\n\nICU-22075 Add comment explaining the rationale of the random\nsleep period prior to test execution.",
          "timestamp": "2022-07-22T08:14:58-07:00",
          "tree_id": "e00a8df1e80c75ce21faf8a38134eaaa9b3b31d3",
          "url": "https://github.com/unicode-org/icu/commit/86166e0a2dd1119e086d04b8973dc486b4af8dcc"
        },
        "date": 1658504397894,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.5352490681859114,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.491324908128659,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.6799452965859667,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.5156004230045064,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.735868749972071,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.3605966440920603,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.725232735094208,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.5715356213565312,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.4018992360228664,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 5.411641975912617,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.7845889242251585,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.489906391570637,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.5810411711783643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.6989044903628248,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.638845013608087,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.9542593853487186,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 4.477601851962288,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.5132633242283766,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.4898131917567772,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.500322656509105,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.5646713651721653,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.751981453337676,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.8547489799830617,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.487954732535053,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 2.136136431728493,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.5094500427292372,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.681135628328345,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.7392137785412753,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.5454436817060646,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 4.785963391972753,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.5167605290276185,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.7336762869616997,
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
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "c258f3d6f81a2514b76d72c5deae8cbc295aecd6",
          "message": "ICU-22095 Export ICU4X normalization data with tries only without Unicode sets",
          "timestamp": "2022-07-25T15:54:29-07:00",
          "tree_id": "1e4ac369923311e8ac0922960bbb1cc63650003f",
          "url": "https://github.com/unicode-org/icu/commit/c258f3d6f81a2514b76d72c5deae8cbc295aecd6"
        },
        "date": 1658791453949,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 3.4526482546895467,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 2.321014191508329,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.9080990523626782,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 2.6802015473659604,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 3.4715781584706273,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 6.580957650634249,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 3.627050546750553,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 2.2540752634977697,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.4040375920652433,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 7.292476293726738,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 3.383593008172816,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 2.7214901248814716,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 3.405002181190727,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 3.401010138801862,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 3.4143226180571093,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.9393770150304024,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 7.173034484697465,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 2.103750836598391,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 2.68491822168748,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 2.693423142014629,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 2.705535500626943,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 3.6113918266073037,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 2.862259737233368,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 2.6534680527708097,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 2.0844885748828714,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 2.875270891840652,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 3.634567615006251,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 3.440000483806737,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 3.553235339443969,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 7.2893515467746575,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 2.6692419178618243,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 3.4105943435546084,
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
          "id": "58a51495dd1c94d29cda93ffc29a904d77e50d31",
          "message": "ICU-22088 Various fixes to make dealing with NUMBERING_SYSTEM formatters easier.",
          "timestamp": "2022-07-28T16:18:01-07:00",
          "tree_id": "69c47d1745ae982673f69de39b18f9c44cdc75b7",
          "url": "https://github.com/unicode-org/icu/commit/58a51495dd1c94d29cda93ffc29a904d77e50d31"
        },
        "date": 1659051772144,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.5991761394559245,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.4970344843133503,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.6790698845073544,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.48371212271052,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.717947194968148,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.3532138452743054,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.567454858082667,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.5162401785159165,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.40205997964291357,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 5.29605749751443,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.6813991150804637,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.4886585962699073,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.7379312567660383,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.730356195492559,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.692727481617647,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.9528883407202929,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 4.520049549014954,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.5616520894519828,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.4903773795685025,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.4865687418184959,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.5032409122974444,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.5051175864941455,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.8281737737239887,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.4847401598781997,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.562314623099814,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.4928178982300344,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.7304218872296544,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.681952566855601,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.7480205257490384,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 4.917383193969727,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.4830461036043567,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.726028424545904,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "dcd19ae9bc0914f8be439a2de1a5fbc17e7b5447",
          "message": "ICU-21957 integrate CLDR release-42-alpha0 (first with Survey Tool data) to ICU main for 72 (#2142)",
          "timestamp": "2022-07-29T15:32:45-07:00",
          "tree_id": "6e9b3f20e3d80a16de21313da72a5cb5b6ac2361",
          "url": "https://github.com/unicode-org/icu/commit/dcd19ae9bc0914f8be439a2de1a5fbc17e7b5447"
        },
        "date": 1659135527221,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.592218823240144,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.397646493810216,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.6660343680148624,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.3752040181970846,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.5824897054008593,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.416261316822462,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.558090346789026,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.3619070692355544,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.3686454862034493,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 7.113205543233039,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.5527095159707276,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.3665583989557404,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.587294210929766,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.6063256897929254,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.598422517105417,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.9713245649761303,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 4.562152326223689,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.388183252457878,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.3575584065883197,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.3790694677923858,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.3827139446836034,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.5852899626261414,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.6889819905706462,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.383222604749734,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.3717741499783893,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.3639437847739873,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.578187853961839,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.5611740120314006,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.5947460562306857,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 4.541550665745433,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.3730163286982675,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.563961910829622,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "rp9-next",
            "username": "rp9-next"
          },
          "committer": {
            "email": "103115900+rp9-next@users.noreply.github.com",
            "name": "rp9-next",
            "username": "rp9-next"
          },
          "distinct": true,
          "id": "4cf4116dadd2f34c20d9df42b1272cac7dcb30c4",
          "message": "ICU-22074 Increase the valgrind CI timeout",
          "timestamp": "2022-08-01T12:51:55+05:30",
          "tree_id": "5219374afb583705529f150783e07b0ac009b0c1",
          "url": "https://github.com/unicode-org/icu/commit/4cf4116dadd2f34c20d9df42b1272cac7dcb30c4"
        },
        "date": 1659340343820,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 2.7655465697080075,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.4562323673717397,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.7718684374455953,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 1.4460504440786357,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 2.7134746459317647,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.6546810149695736,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 2.750683138870175,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.4398335892128509,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.38878631855848406,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 7.453386664526009,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 2.7549062082822195,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 1.4404055872137755,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 2.6935881618688033,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 2.686844296163474,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 2.7182548968035656,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.969574040754168,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 4.689489515208902,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.4383941619491574,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 1.456219310471682,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 1.4306642926259543,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 1.4432687140582938,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 2.7186172377164204,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 1.8037368596729846,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 1.452279170790426,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.4248583611070578,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 1.42431333094837,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 2.7424256868248182,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 2.708998970966598,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 2.734536198348878,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 4.743766611267203,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 1.4433600539429905,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 2.7098934893280457,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "0266970e977b9e2488dfbf788cc280be3a0338ca",
          "message": "ICU-21957 integrate CLDR release-42-alpha1 to ICU main for 72",
          "timestamp": "2022-08-05T09:39:58-07:00",
          "tree_id": "ae5c1572c4ed62ac177b5a843ecc6451b53749b2",
          "url": "https://github.com/unicode-org/icu/commit/0266970e977b9e2488dfbf788cc280be3a0338ca"
        },
        "date": 1659719473256,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 3.015651965461988,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.8454348910846476,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.684043221624273,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 2.2912854699052287,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 3.0107743956440705,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.8619521601884106,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 3.0110910384312164,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.8931073889715722,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.33515911574450324,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 6.373335675495427,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 3.0105528470693756,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 2.2935865869888064,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 3.010921549042295,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 3.0108922540399945,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 3.01212615858382,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.8254485479981528,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 6.353317230806758,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.8522101046521153,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 2.2685534523056643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 2.263713937255453,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 2.3120924331450166,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 3.0104503035839367,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 2.446240717821782,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 2.309448379566262,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.7521174087430667,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 2.2816371393353343,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 3.0103192442715323,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 3.0112267011031517,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 3.0159397653861326,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 6.337028439433177,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 2.2996888325137057,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 3.0105528470693756,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "pedberg@unicode.org",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "d99abb6d65fd48db3bbf6aad523e7aaa793781ec",
          "message": "ICU-21957 integrate CLDR release-42-alpha1b to ICU main for 72",
          "timestamp": "2022-08-09T16:05:20-07:00",
          "tree_id": "114745d9b1092b441127d501c28853c12f8182a3",
          "url": "https://github.com/unicode-org/icu/commit/d99abb6d65fd48db3bbf6aad523e7aaa793781ec"
        },
        "date": 1660087808248,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 3.0159533498279147,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.811205856199187,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.684876947968562,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 2.0775715791332017,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 3.012599449932284,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.528561883818087,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 3.01236008239528,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.7842909995190168,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.3352655177901413,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 6.3782452859095375,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 3.0129011322324977,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 2.0774496901325548,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 3.012832022765865,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 3.0119399661475295,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 3.0125307395635885,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.825679173383931,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 5.69945052803496,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.7846046556938955,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 2.0780031326223667,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 2.0777952075540633,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 2.077664318412047,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 3.0122655401066876,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 2.3585129473699964,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 2.0775715791332017,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.7951410894758293,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 2.077447716305236,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 3.0124273109692017,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 3.0129011322324977,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 3.0124673797811106,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 6.349729631104034,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 2.0773638635184115,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 3.014769729496486,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "nmihai_2000@yahoo.com",
            "name": "Mihai Nita",
            "username": "mihnita"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "0eecb25011de051c57f68c75d767ad3586de7859",
          "message": "ICU-22101 Error prone reports \"StringSplitter\" error in PluralRules.java\n\nString.split(String) and Pattern.split(CharSequence) have surprising behaviour.\n\"a::b:\".split(\":\") produces [\"a\", \"b\"], when one would expect [\"a\", \"\", \"b\", \"\"]\n\nThe recommended fix is to use the Guava Splitter, or setting an explicit limit:\nString.split(String,int limit) and Pattern.split(CharSequence,int limit)",
          "timestamp": "2022-08-11T08:27:19-07:00",
          "tree_id": "724c7700b37e1fcbaf574d25414c4370ec250537",
          "url": "https://github.com/unicode-org/icu/commit/0eecb25011de051c57f68c75d767ad3586de7859"
        },
        "date": 1660233434843,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 3.7547301591557116,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 2.1642059980089043,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 2.019596116267658,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 2.470743370149543,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 3.5748202467656505,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 4.0024631806767985,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 3.5807856116056733,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 2.12127258605313,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.39661924174874597,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 7.566222887305394,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 3.5738857789196605,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 2.483862392527795,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 3.6140376932799194,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 3.634459270154129,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 3.616282410217314,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.9915247342005155,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 7.604857785018845,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 2.1346670023992913,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 2.48782707384507,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 2.488691640512656,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 2.4661096295671823,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 3.5702180485171575,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 2.6647952414586786,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 2.4621667081206455,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 2.1471369705638517,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 2.493085098497489,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 3.617034540168336,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 3.618896711580106,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 3.616674963831363,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 7.616067745374183,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 2.494697641268943,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 3.6198148110711825,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "elango@unicode.org",
            "name": "Elango Cheran",
            "username": "echeran"
          },
          "committer": {
            "email": "elango@unicode.org",
            "name": "Elango",
            "username": "echeran"
          },
          "distinct": true,
          "id": "3ef03a408714cf0be1f6be62e3fad57757403da3",
          "message": "ICU-21947 Replace FixedDecimal with DecimalQuantity in PluralRule sample parsing\n\nSee #2007",
          "timestamp": "2022-08-11T15:10:37-07:00",
          "tree_id": "2216414cc87171c10282454fcf0a35d917a80fbd",
          "url": "https://github.com/unicode-org/icu/commit/3ef03a408714cf0be1f6be62e3fad57757403da3"
        },
        "date": 1660257440856,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 3.0145205112725924,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 1.842630613153807,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.6845463245603383,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 2.306080679412008,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 3.0127284633545828,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.796746866441554,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 3.0103192442715323,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 1.8901831064920975,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.33514517151197903,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 6.376588449698562,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 3.010448572275467,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 2.3158254770418267,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 3.0117734563070995,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 3.0122811389108324,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 3.011551549815316,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.8252075928170128,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 6.3883939827322225,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.8843355073218355,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 2.3948577675668425,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 2.389701019614009,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 2.301669443146197,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 3.0109255784330413,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 2.415192166655457,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 2.300875398747637,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 1.7521886819321848,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 2.3180774011932206,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 3.0120588765076293,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 3.020450293697889,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 3.010921549042295,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 6.364509506804582,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 2.303728512278833,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 3.010850015392417,
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
          "id": "8492a8291670aa0af0f3fa6c088ddaf4ff373833",
          "message": "ICU-22105 Fixed the unit-conversion logic to work correctly with negative temperature values.",
          "timestamp": "2022-08-16T10:18:24-07:00",
          "tree_id": "ade7eb2c651708551e62d475d3987927dec81353",
          "url": "https://github.com/unicode-org/icu/commit/8492a8291670aa0af0f3fa6c088ddaf4ff373833"
        },
        "date": 1660672220399,
        "tool": "ndjson",
        "benches": [
          {
            "name": "testJDKIsDefined",
            "value": 3.478931005015964,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetterOrDigit",
            "value": 2.7283976021894523,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUpperCase",
            "value": 1.9366523514618506,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDigit",
            "value": 2.378813670587098,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetter",
            "value": 3.42191859048334,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierPart",
            "value": 3.8402966575448017,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsTitleCase",
            "value": 3.4469553277954055,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsWhiteSpace",
            "value": 2.0489164837677087,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsISOControl",
            "value": 0.38462682361317874,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetNumericValue",
            "value": 7.296377310263468,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierStart",
            "value": 3.384961195375789,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsSpaceChar",
            "value": 2.3488650880680906,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKDigit",
            "value": 3.3396185596425556,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKGetType",
            "value": 3.357294518527679,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsWhiteSpace",
            "value": 3.3496407397555252,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsISOControl",
            "value": 0.9125139172997166,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUpperCase",
            "value": 6.418622032423341,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testDigit",
            "value": 1.9880724465226924,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLowerCase",
            "value": 2.2719478049473456,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetType",
            "value": 2.2438517809147367,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsUnicodeIdentifierStart",
            "value": 2.3585720763419222,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsUnicodeIdentifierPart",
            "value": 3.4204694142407335,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsIdentifierIgnorable",
            "value": 2.572242037009606,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsLetter",
            "value": 2.2289189985087616,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testGetNumericValue",
            "value": 2.1072419101735416,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsTitleCase",
            "value": 2.3362796944164494,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsDigit",
            "value": 3.4632942473262642,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsSpaceChar",
            "value": 3.57275610571509,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLetterOrDigit",
            "value": 3.5113324050468413,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsLowerCase",
            "value": 7.313793354734243,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testIsDefined",
            "value": 2.412460633962189,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "testJDKIsIdentifierIgnorable",
            "value": 3.4714553986341268,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      }
    ]
  }
}