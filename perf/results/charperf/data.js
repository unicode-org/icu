window.BENCHMARK_DATA = {
  "lastUpdate": 1653503947516,
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
          "id": "80ee559205dd165c2d647610376d6f9a06822ae4",
          "message": "ICU-21843 Add ICU4C performance tests to continuous integration\n\nSee #1987",
          "timestamp": "2022-03-07T12:53:44-08:00",
          "tree_id": "7da686bd1662079612215dc8b0f27437626720c3",
          "url": "https://github.com/unicode-org/icu/commit/80ee559205dd165c2d647610376d6f9a06822ae4"
        },
        "date": 1646687414486,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.774,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.1677,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 5.4876,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.8268,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 6.2419,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.1841,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.7462,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.7579,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.4169,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.4316,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.332,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "avetlov@riverlogic.com",
            "name": "Alexey Vetlov",
            "username": "avetlov"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "1393face12f7205a133a63f13bb592f561b4abfb",
          "message": "ICU-21768 Fixed (u_snprintf improperly counts the required buffer size). Modified TestSnprintf to test the null buffer case.",
          "timestamp": "2022-03-08T23:09:54Z",
          "tree_id": "586d92fe70bb8985ec51cb46c5c3b4f26dadd05c",
          "url": "https://github.com/unicode-org/icu/commit/1393face12f7205a133a63f13bb592f561b4abfb"
        },
        "date": 1646781938707,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.608,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.7222,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 5.0349,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.6758,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.7267,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.64,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6966,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.6123,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.2011,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.9877,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.608,
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
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "31871cc14f5b55a53511884d9787494905a80a00",
          "message": "ICU-21801 Fix array comparison warning in uspoof_impl.cpp\n\nSee #2015",
          "timestamp": "2022-03-08T15:21:53-08:00",
          "tree_id": "bd6da903ad6a15330b5714ac76b55b1000680a13",
          "url": "https://github.com/unicode-org/icu/commit/31871cc14f5b55a53511884d9787494905a80a00"
        },
        "date": 1646782592029,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 3.9885,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.3228,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.6143,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.0461,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.1585,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.3457,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 3.983,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.0291,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.364,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3869,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.2989,
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
          "id": "f57ef9ebf72d889e9b93be90fbc020fae0e374ae",
          "message": "ICU-21527 Assert pattern equality instead of object equality",
          "timestamp": "2022-03-08T15:31:52-08:00",
          "tree_id": "ecf2895c54243a8853b544b889a8c7677e6381b5",
          "url": "https://github.com/unicode-org/icu/commit/f57ef9ebf72d889e9b93be90fbc020fae0e374ae"
        },
        "date": 1646783260465,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 3.9862,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.3234,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.6162,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.0175,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.2565,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.3497,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 3.9833,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 3.9946,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3524,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3851,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.2887,
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
          "id": "f79f03dad5ffcd0e7ac9e1bcbd05fc38bff8e3a2",
          "message": "ICU-21322 Add parse and format methods for DecimalQuantity with exponent\n\nSee #2012",
          "timestamp": "2022-03-08T15:56:50-08:00",
          "tree_id": "e99f29d061b511c3a728814891002ca64d0008cd",
          "url": "https://github.com/unicode-org/icu/commit/f79f03dad5ffcd0e7ac9e1bcbd05fc38bff8e3a2"
        },
        "date": 1646784693706,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 3.9873,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.324,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.616,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.0288,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.2182,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.3501,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 3.9846,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.0008,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.351,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3895,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3031,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "daniel.bevenius@gmail.com",
            "name": "Daniel Bevenius",
            "username": "danbev"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "55a51fa9bde41598fb70c5931d313e908f21ca65",
          "message": "ICU-21784 suppress -Wunused-function warning in unistr.cpp\n\nThis commit adds an ignore of the unused function\nuprv_UnicodeStringDummy.",
          "timestamp": "2022-03-09T03:31:37Z",
          "tree_id": "6f7b710d4a93a3e7597bccfba1321df26f0084ee",
          "url": "https://github.com/unicode-org/icu/commit/55a51fa9bde41598fb70c5931d313e908f21ca65"
        },
        "date": 1646797615456,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.4631,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.3934,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.7538,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.8955,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.4906,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.4803,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.4607,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.3642,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.7482,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.6961,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.7482,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "alexandermv@gmail.com",
            "name": "Alexander Morozov",
            "username": "alxrmorozov"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "234cbe2c17034c68d6dd6df09fda79a052f0a238",
          "message": "ICU-21805 Remove useless check",
          "timestamp": "2022-03-09T03:37:37Z",
          "tree_id": "9e1ab2725eb42216e690fc018fce2b085fddb70f",
          "url": "https://github.com/unicode-org/icu/commit/234cbe2c17034c68d6dd6df09fda79a052f0a238"
        },
        "date": 1646798180344,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 3.9179,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.1883,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.1548,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.4694,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.0283,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.1992,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 3.874,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.1831,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.1018,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.2261,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 4.7389,
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
          "id": "77e0c9e371994b739e4049ab4dc476ab8b8c5719",
          "message": "ICU-21900 Adjusts performance alert threshold to 150%, i.e. the alert only is\ntriggered if the current measured execution time is 50% higher than the previous\ntime.\nThe current setting of 100% means that even a 1% increase from previous time\ntriggers the alert already.",
          "timestamp": "2022-03-09T11:10:43-08:00",
          "tree_id": "430a4aa2ed3e70bf913386f73e673d6391f87c4b",
          "url": "https://github.com/unicode-org/icu/commit/77e0c9e371994b739e4049ab4dc476ab8b8c5719"
        },
        "date": 1646853926259,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.1285,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.5337,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 5.5136,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.89,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 6.6923,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.5319,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.0036,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.3718,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.4979,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.7165,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 4.7687,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "younies.mahmoud@gmail.com",
            "name": "Younies",
            "username": "younies"
          },
          "committer": {
            "email": "younies@chromium.org",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "distinct": true,
          "id": "bb822ba38a8b8e0ebd33d8311579043bd7978794",
          "message": "ICU-21936 Make the internal units converters public\n\nSee #2021",
          "timestamp": "2022-03-09T22:42:04+01:00",
          "tree_id": "1e1a52e65f91a8ea8454578ce854063b1be82290",
          "url": "https://github.com/unicode-org/icu/commit/bb822ba38a8b8e0ebd33d8311579043bd7978794"
        },
        "date": 1646863064371,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 3.9868,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.3263,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.6175,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.0217,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.2619,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.3428,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 3.985,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 3.9955,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3588,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3941,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3093,
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
          "id": "571d12abfbe73a974fbe9d260b85975cbfd5f43c",
          "message": "ICU-21409 add word for bell to laodict",
          "timestamp": "2022-03-09T15:14:42-08:00",
          "tree_id": "360a095baf4597f47ff422696cb50d2e95e1e59d",
          "url": "https://github.com/unicode-org/icu/commit/571d12abfbe73a974fbe9d260b85975cbfd5f43c"
        },
        "date": 1646868650829,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.4858,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.5029,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.7548,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.0122,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.4619,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.463,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.4463,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4423,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.7949,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.7117,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.4962,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "mspector@fastmail.com",
            "name": "Michael Spector",
            "username": "spektom"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "e730bce02f15fecc290fe042cdf6f74cc9a78a4c",
          "message": "ICU-21815 Remove unused line",
          "timestamp": "2022-03-10T01:00:26Z",
          "tree_id": "d9fdd09810b4069b89b80efbb1e108c11f85f1bb",
          "url": "https://github.com/unicode-org/icu/commit/e730bce02f15fecc290fe042cdf6f74cc9a78a4c"
        },
        "date": 1646874929725,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.2739,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.5757,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.0477,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.5836,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.9648,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6003,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.2952,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.6032,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.2186,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3577,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3424,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "8a5f045992eab8898eff5f6628650e75b8f13bc3",
          "message": "ICU-21900 Fix issue filtering in commit checker",
          "timestamp": "2022-03-09T19:20:02-08:00",
          "tree_id": "40c23771b9eb09cead8bd04f2a099e81767191cb",
          "url": "https://github.com/unicode-org/icu/commit/8a5f045992eab8898eff5f6628650e75b8f13bc3"
        },
        "date": 1646883255588,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.1629,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.0958,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.4762,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.4755,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.353,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.0842,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.0748,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.9076,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.996,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3578,
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
          "id": "996378821c4ee89e68dc74183359e00f7130de0d",
          "message": "ICU-21839 Add ICU4J test that ISO8601 inherits patterns/symbols grom Gregorian",
          "timestamp": "2022-03-10T09:37:21-08:00",
          "tree_id": "0106e37264406db1343e6cef2ec0b5afbf40768c",
          "url": "https://github.com/unicode-org/icu/commit/996378821c4ee89e68dc74183359e00f7130de0d"
        },
        "date": 1646934754715,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.0769,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.1165,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.61,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.0171,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.37,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.7279,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.8122,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.9292,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.1386,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.0191,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.2742,
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
          "id": "f374427f6018056a6643c6519bbfadf869755ae0",
          "message": "ICU-21900 integrate CLDR release-41-beta1 to ICU main for 71rc",
          "timestamp": "2022-03-10T11:17:09-08:00",
          "tree_id": "52377cd9d7b956ff5192e4fa0fe3d53b630e9a63",
          "url": "https://github.com/unicode-org/icu/commit/f374427f6018056a6643c6519bbfadf869755ae0"
        },
        "date": 1646940769949,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.3531,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.3735,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.3734,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.3535,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.822,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.3541,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.3741,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.3543,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.4917,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.4853,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.2051,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "7fe330119e6b5503232cfd780869f5ff685ecdea",
          "message": "ICU-21900 ICU4C 71 change report\n\nSee #2024. Includes update to generator that removes the minor version indication from the reports.",
          "timestamp": "2022-03-10T15:50:45-08:00",
          "tree_id": "006f83812ce403ec53e3769323919eb155941a7e",
          "url": "https://github.com/unicode-org/icu/commit/7fe330119e6b5503232cfd780869f5ff685ecdea"
        },
        "date": 1646957203032,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.4774,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.8585,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 5.1308,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.8873,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 6.3387,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.8819,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.4607,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.8692,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.687,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.9241,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.8294,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "19c13048d24f0dc010d48249331913bf436fedb0",
          "message": "ICU-21900 Update ICU4J Change report (BRS#20)\n\nUpdate to show only major release numbers.",
          "timestamp": "2022-03-10T17:58:16-08:00",
          "tree_id": "eceff86f8dff9a267d5dfed594393cb554113fef",
          "url": "https://github.com/unicode-org/icu/commit/19c13048d24f0dc010d48249331913bf436fedb0"
        },
        "date": 1646964778965,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.6134,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6903,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.019,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.8489,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.8725,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.664,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.714,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.5909,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.4325,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.4704,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.4019,
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
          "id": "b5f10c4a1c19a0fe50bc4faa73062df4ed7d41fc",
          "message": "ICU-21332 Add tests from ICU4C TestDelimiters() into related ICU4J test",
          "timestamp": "2022-03-10T21:20:11-08:00",
          "tree_id": "3155b8800258c99573d9556c3e7be8ede960b1ba",
          "url": "https://github.com/unicode-org/icu/commit/b5f10c4a1c19a0fe50bc4faa73062df4ed7d41fc"
        },
        "date": 1646976890677,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.7491,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 6.1326,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 5.3185,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 6.1105,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 6.6019,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 6.1325,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.7427,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 6.1115,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.9343,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 7.2143,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 7.113,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "zhin@google.com",
            "name": "Ng Zhi An",
            "username": "ngzhian"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "e0bb2ccdeb2d14000c2af63fc530b019f9361200",
          "message": "ICU-21844 Fix variable shadowing",
          "timestamp": "2022-03-11T19:10:53Z",
          "tree_id": "e2752ea8ab56cc092a5a06e9079d324bc5af7500",
          "url": "https://github.com/unicode-org/icu/commit/e0bb2ccdeb2d14000c2af63fc530b019f9361200"
        },
        "date": 1647026740790,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.2881,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.276,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.9543,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.2854,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.6976,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.2747,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.2744,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.2797,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.4361,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.3056,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.5951,
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
          "id": "df6a8c877c76a120846323e7662016d534d0690f",
          "message": "ICU-13619 Now that UDATPG_NARROW is @stable, can reference it in header",
          "timestamp": "2022-03-11T13:06:50-08:00",
          "tree_id": "3b6dbaef199f1c19fb5e03bbf08bd8e40c6d29a6",
          "url": "https://github.com/unicode-org/icu/commit/df6a8c877c76a120846323e7662016d534d0690f"
        },
        "date": 1647033730196,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 3.968,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.088,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.5147,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.141,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.1584,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.0849,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.11,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 3.9443,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 4.7378,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 4.7515,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 4.7221,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "29111fd19582bb995f6a3a8052d21e4fd756b3b4",
          "message": "ICU-21824 make the Unicode license match exactly\n\nThe ICU copy of the Unicode license was missing some lines that are\nconsidered part of the license text.\n\nSee https://github.com/unicode-org/template-repo/blob/main/LICENSE\nand https://www.unicode.org/license.txt",
          "timestamp": "2022-03-14T19:50:44Z",
          "tree_id": "fc1f892d2ef84e0169155887767528a32ca956a5",
          "url": "https://github.com/unicode-org/icu/commit/29111fd19582bb995f6a3a8052d21e4fd756b3b4"
        },
        "date": 1647288387302,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.3605,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6576,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.064,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.6357,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.0424,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6878,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.3519,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.6854,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3046,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.5568,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.4415,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "bfca3ccaac707f5168f5888b2446a4518426bb04",
          "message": "ICU-21875 improve C API coverage",
          "timestamp": "2022-03-15T05:07:59Z",
          "tree_id": "8225a14dcf2fd2abc50cc44b8cbc4b0d4b5643be",
          "url": "https://github.com/unicode-org/icu/commit/bfca3ccaac707f5168f5888b2446a4518426bb04"
        },
        "date": 1647321794542,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.7496,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.8264,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.1871,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.064,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.04,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.8375,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.8476,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.7336,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.705,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.6069,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.6021,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "811351f67cca8291f0404d9ee30f0acf191b926b",
          "message": "ICU-21900 BRS71 Updated serialization test data for 71.1",
          "timestamp": "2022-03-15T09:47:57-04:00",
          "tree_id": "6fec003664bd37e4fd2da820024913a5620c6207",
          "url": "https://github.com/unicode-org/icu/commit/811351f67cca8291f0404d9ee30f0acf191b926b"
        },
        "date": 1647352958923,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.515,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6328,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9849,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.7088,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7275,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6282,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6484,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4792,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3508,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3869,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3566,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "5b4559df24b8b610998b9c3596534dbcf6744bc4",
          "message": "ICU-21900 BRS71 Cleanup import statements in ICU4J",
          "timestamp": "2022-03-15T09:47:25-04:00",
          "tree_id": "710673993a1d12aa96e3404892847295c45f2e07",
          "url": "https://github.com/unicode-org/icu/commit/5b4559df24b8b610998b9c3596534dbcf6744bc4"
        },
        "date": 1647353196145,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.0197,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6314,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9855,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.7177,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7284,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6289,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6405,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4778,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3686,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3801,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.332,
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
          "id": "dbf7c20be650d9425ba8f25784c3f70b4c07f838",
          "message": "ICU-21942 Fix Kosovo 3-letter code to be XKK for uloc_getISO3Country etc.",
          "timestamp": "2022-03-15T10:59:13-07:00",
          "tree_id": "33cac0a5222e84bdd8a8acd8d8f0092e4f2a2765",
          "url": "https://github.com/unicode-org/icu/commit/dbf7c20be650d9425ba8f25784c3f70b4c07f838"
        },
        "date": 1647368228094,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.3558,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.3756,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.3753,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.3554,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.9701,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.3555,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.3756,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.3556,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.4869,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.489,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.2072,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shaobero@adobe.com",
            "name": "shaobero",
            "username": "shaobero"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "b1269c91211deab8f94c3cf85accb51e5a71ca5c",
          "message": "ICU-21823 Adding changes to fix charset detection incase of no match",
          "timestamp": "2022-03-15T21:47:40Z",
          "tree_id": "4dccceca277645512aea849db22a064750395d60",
          "url": "https://github.com/unicode-org/icu/commit/b1269c91211deab8f94c3cf85accb51e5a71ca5c"
        },
        "date": 1647381815415,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.5406,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6383,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.982,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.6738,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7247,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6278,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6502,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4904,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.384,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3836,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3526,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "younies.mahmoud@gmail.com",
            "name": "Younies",
            "username": "younies"
          },
          "committer": {
            "email": "younies@chromium.org",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "distinct": true,
          "id": "f30956fc9c9fd9e83189ac1c8b372dea0832eb90",
          "message": "ICU-21840 Fix formatting Aliases\n\nSee #2016",
          "timestamp": "2022-03-15T23:27:43+01:00",
          "tree_id": "effd861fbdf9feda8b88ac7df6e6337ffc160e42",
          "url": "https://github.com/unicode-org/icu/commit/f30956fc9c9fd9e83189ac1c8b372dea0832eb90"
        },
        "date": 1647384231074,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.4778,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6651,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.2317,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.6958,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.1392,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.7921,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.3981,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.7258,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.477,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.6428,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.4348,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "daju@microsoft.com",
            "name": "Daniel Ju",
            "username": "daniel-ju"
          },
          "committer": {
            "email": "41210545+daniel-ju@users.noreply.github.com",
            "name": "Daniel Ju",
            "username": "daniel-ju"
          },
          "distinct": true,
          "id": "741bbddcf9279f41385862be8913ed13b9e342c0",
          "message": "ICU-21900 BRS71RC Update version number and regenerate configure",
          "timestamp": "2022-03-15T18:26:10-05:00",
          "tree_id": "1b2b5e930f41b85f81e6baf2dc1b46dda2462bd9",
          "url": "https://github.com/unicode-org/icu/commit/741bbddcf9279f41385862be8913ed13b9e342c0"
        },
        "date": 1647387654169,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.6059,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.9609,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 5.1531,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.9282,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 6.3022,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.907,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.5293,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.9093,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.7554,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 7.0014,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.7868,
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
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "33c9b61b26250e9fe39e098f9f131549d9aa516f",
          "message": "ICU-21871 Make behavior consistent on list format of empty strings",
          "timestamp": "2022-03-15T18:11:57-07:00",
          "tree_id": "fecddd3c2aad1ff68252adb1d70169a10776b5b8",
          "url": "https://github.com/unicode-org/icu/commit/33c9b61b26250e9fe39e098f9f131549d9aa516f"
        },
        "date": 1647393991976,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.2414,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.3358,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.6026,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.5846,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.5368,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.3573,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.3771,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.2538,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.2663,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.2085,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.1396,
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
          "id": "633070497465bea1d19d66ad2162a1ec5f4bdda3",
          "message": "ICU-21944 Sync recent uloc_getLanguage/Countries updates to ICU4J; add \"mo\" mapping for C",
          "timestamp": "2022-03-16T09:01:59-07:00",
          "tree_id": "3ddb1ecd0ec14e15c35b4d94a5ec8352fd487ac8",
          "url": "https://github.com/unicode-org/icu/commit/633070497465bea1d19d66ad2162a1ec5f4bdda3"
        },
        "date": 1647447554670,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.8674,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.1681,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.6736,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.3966,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.7948,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.3571,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.0241,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.4096,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.0614,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.3681,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.1737,
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
          "id": "8575c0dacef68253329608650b25412e6db7ca78",
          "message": "ICU-21900 check non-stable API macros, move class boilerplate out of conditionals",
          "timestamp": "2022-03-16T09:03:05-07:00",
          "tree_id": "a400718b943fe30bfd9da7c0ac9a99ed55899aa7",
          "url": "https://github.com/unicode-org/icu/commit/8575c0dacef68253329608650b25412e6db7ca78"
        },
        "date": 1647448087060,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.228,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.4942,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.864,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.556,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.9271,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.5011,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.1741,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.5162,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.2886,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.5232,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.4267,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "89c5d03023d5e96945188bc365a3d15e53441c86",
          "message": "ICU-21900 BRS71 ICU4J API signature file",
          "timestamp": "2022-03-16T12:36:40-04:00",
          "tree_id": "45a430aee8792898a7dc2a9bc6970172d466a6b5",
          "url": "https://github.com/unicode-org/icu/commit/89c5d03023d5e96945188bc365a3d15e53441c86"
        },
        "date": 1647449536269,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.3535,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.3733,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.3733,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.3535,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.8032,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.3536,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.3734,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.3535,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.4912,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.4935,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.2041,
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
          "id": "0f49e5086850b0a5171000964a07b570749dc1c3",
          "message": "ICU-21843 Increase performance alert threshold to 200% to avoid false alerts\nfrom occasional spikes.",
          "timestamp": "2022-03-17T09:31:53-07:00",
          "tree_id": "ba17dca7ac476bc685815bcd81bb86df6a6f1d95",
          "url": "https://github.com/unicode-org/icu/commit/0f49e5086850b0a5171000964a07b570749dc1c3"
        },
        "date": 1647535598336,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.3424,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.4487,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.7115,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.6457,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.6264,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.4397,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.4537,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.3192,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.4065,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.3283,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.291,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "ee6433c297477453bf12cd1cdb83a58ef74aea4e",
          "message": "ICU-21949 tzdata2022a update in ICU",
          "timestamp": "2022-03-17T13:56:20-04:00",
          "tree_id": "e98ba87a01a2366cff5ef67b84a910d40deb71cf",
          "url": "https://github.com/unicode-org/icu/commit/ee6433c297477453bf12cd1cdb83a58ef74aea4e"
        },
        "date": 1647540653462,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.4862,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6346,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9827,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.6901,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7257,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6293,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6387,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4845,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3261,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3849,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3463,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "yumaoka@users.noreply.github.com",
            "name": "Yoshito Umaoka",
            "username": "yumaoka"
          },
          "distinct": true,
          "id": "ee6433c297477453bf12cd1cdb83a58ef74aea4e",
          "message": "ICU-21949 tzdata2022a update in ICU",
          "timestamp": "2022-03-17T13:56:20-04:00",
          "tree_id": "e98ba87a01a2366cff5ef67b84a910d40deb71cf",
          "url": "https://github.com/unicode-org/icu/commit/ee6433c297477453bf12cd1cdb83a58ef74aea4e"
        },
        "date": 1647543068371,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.4809,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6343,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9841,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.6868,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.733,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.629,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6467,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4779,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3532,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3898,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3479,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "daju@microsoft.com",
            "name": "Daniel Ju",
            "username": "daniel-ju"
          },
          "committer": {
            "email": "41210545+daniel-ju@users.noreply.github.com",
            "name": "Daniel Ju",
            "username": "daniel-ju"
          },
          "distinct": true,
          "id": "09331b75bf4eba38e9e4eb3c06109c54ad940dd4",
          "message": "ICU-21901 BRS71GA Update version number",
          "timestamp": "2022-03-21T15:01:26-05:00",
          "tree_id": "5a68777dbd795812fe73f8d7d51314dfe6a1d6dd",
          "url": "https://github.com/unicode-org/icu/commit/09331b75bf4eba38e9e4eb3c06109c54ad940dd4"
        },
        "date": 1647893854719,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.3548,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.3747,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.3735,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.3534,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.8044,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.3539,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.3738,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.3534,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.4856,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.4928,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.2038,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "29107334+jefgen@users.noreply.github.com",
            "name": "Jeff Genovy",
            "username": "jefgen"
          },
          "committer": {
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "1d9cc717e2b93b8b27b84b2969c3bfc9a4d0a593",
          "message": "ICU-21956 Merge maint-71 to main\n\nMerging the maint/maint-71 branch into the main branch.",
          "timestamp": "2022-03-23T12:50:13-07:00",
          "tree_id": "5a68777dbd795812fe73f8d7d51314dfe6a1d6dd",
          "url": "https://github.com/unicode-org/icu/commit/1d9cc717e2b93b8b27b84b2969c3bfc9a4d0a593"
        },
        "date": 1648065916113,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.3541,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.3737,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.3735,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.3535,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.803,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.354,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.3742,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.3541,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.4923,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.4944,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.206,
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
          "id": "9b3c0c328c78d5ea74bfdff9026f31e91b173c0b",
          "message": "ICU-21901 integrate CLDR release-41-beta2 to ICU main for 71ga",
          "timestamp": "2022-03-23T16:43:02-07:00",
          "tree_id": "aea6b6784ef5f219025dc77f8d8bf48d19777bc9",
          "url": "https://github.com/unicode-org/icu/commit/9b3c0c328c78d5ea74bfdff9026f31e91b173c0b"
        },
        "date": 1648079929338,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.4991,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6359,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9849,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.6873,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7279,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6321,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6462,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4775,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3662,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3935,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3445,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "2aa2a7221c2d28c331ffbe0209b74d3156bbe312",
          "message": "ICU-21953 remove outdated ucdterms.txt\n\nThis is an old version of the Unicode license from 2004,\nwhen the ICU license was different from the Unicode license.\n\nWe should have removed this file in 2016,\nwhen the Unicode license was revised,\nand when the ICU project became a project of the Unicode Consortium,\nadopting the Unicode license for all of ICU.\n\nSince 2016, the Unicode data files are covered by the same license as the\ntop license text in the ICU LICENSE file.",
          "timestamp": "2022-03-24T00:14:02Z",
          "tree_id": "1ceb57fa15c77488db9f7db1a5f553c0d902a004",
          "url": "https://github.com/unicode-org/icu/commit/2aa2a7221c2d28c331ffbe0209b74d3156bbe312"
        },
        "date": 1648081711127,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.0054,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.0107,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.4267,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.11,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.0924,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 3.9594,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 3.9709,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 3.9113,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 4.6174,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 4.5961,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 4.7165,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "ed1d9efc153bce41d61dd10b0ee4641b8513c431",
          "message": "ICU-21954 LICENSE: add more license texts from icu4c files",
          "timestamp": "2022-03-24T21:22:05Z",
          "tree_id": "95b674daae808a0ca19eb48d26139fbd81742c43",
          "url": "https://github.com/unicode-org/icu/commit/ed1d9efc153bce41d61dd10b0ee4641b8513c431"
        },
        "date": 1648157865019,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.4389,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 6.1415,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 5.2529,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.898,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 6.4244,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.8618,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.4392,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.989,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.4979,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.9496,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.5904,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "committer": {
            "email": "srl295@gmail.com",
            "name": "Steven R. Loomis",
            "username": "srl295"
          },
          "distinct": true,
          "id": "2612b8b55af90106c11422a3558006bdc8016bc4",
          "message": "ICU-21965 fix utilities.jar module name\n\ncom.ibm.icu.utilities - otherwise the automatic module name is unusable",
          "timestamp": "2022-03-30T11:09:53-05:00",
          "tree_id": "c25c55651b6b9eedb525e2eafeff337ca86dedcb",
          "url": "https://github.com/unicode-org/icu/commit/2612b8b55af90106c11422a3558006bdc8016bc4"
        },
        "date": 1648657605018,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.5413,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6313,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9835,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.7224,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7248,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6273,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6413,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.5244,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3875,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3832,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3469,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "shane@unicode.org",
            "name": "Shane Carr",
            "username": "sffc"
          },
          "committer": {
            "email": "shane@unicode.org",
            "name": "Shane F. Carr",
            "username": "sffc"
          },
          "distinct": true,
          "id": "b144aac447e4ddbc20cbec90ed625ef983a64fd9",
          "message": "ICU-21762 Export Script_Extensions with --all flag in icuexportdata\n\nSee #2054",
          "timestamp": "2022-03-30T11:44:02-07:00",
          "tree_id": "9f2a01db8caee468c31e2289b4569774b0bebc48",
          "url": "https://github.com/unicode-org/icu/commit/b144aac447e4ddbc20cbec90ed625ef983a64fd9"
        },
        "date": 1648666800343,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.5206,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6326,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9837,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.6805,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7271,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6289,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6383,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.5013,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3719,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3886,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3507,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "y.umaoka@gmail.com",
            "name": "yumaoka",
            "username": "yumaoka"
          },
          "committer": {
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "c205e7ee49a7086a28b9c275fcfdac9ca3dc815d",
          "message": "ICU-21971 Added a new numeric currecny code SLE/695 for Sierra Leone Leone.",
          "timestamp": "2022-03-30T13:49:51-07:00",
          "tree_id": "f4b713638cc22c33d7948a0c67f139597d39895e",
          "url": "https://github.com/unicode-org/icu/commit/c205e7ee49a7086a28b9c275fcfdac9ca3dc815d"
        },
        "date": 1648674386454,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.0527,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.0296,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.6847,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.0064,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.5356,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.1762,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.1457,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.1302,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.2641,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.1878,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.3427,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "29107334+jefgen@users.noreply.github.com",
            "name": "Jeff Genovy",
            "username": "jefgen"
          },
          "committer": {
            "email": "29107334+jefgen@users.noreply.github.com",
            "name": "Jeff Genovy",
            "username": "jefgen"
          },
          "distinct": true,
          "id": "861e55c246bf56867699a11d274c1875e0690fcc",
          "message": "ICU-21972 Use a shared props file for the ICU Major Version number in the Windows Visual Studio project files.",
          "timestamp": "2022-03-30T20:55:36-07:00",
          "tree_id": "69b9145357b51f281cc1709e50a968a3b0498b49",
          "url": "https://github.com/unicode-org/icu/commit/861e55c246bf56867699a11d274c1875e0690fcc"
        },
        "date": 1648699872357,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.5451,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6311,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9826,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.7205,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7355,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6268,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.651,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.5013,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3581,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.385,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3436,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "jubrianc@microsoft.com",
            "name": "Julien Brianceau",
            "username": "jbrianceau"
          },
          "committer": {
            "email": "29107334+jefgen@users.noreply.github.com",
            "name": "Jeff Genovy",
            "username": "jefgen"
          },
          "distinct": true,
          "id": "a2c90781f14869f6fbd9267d03e155efa192b245",
          "message": "ICU-21967 Remove obsolete references in makedata.vcxproj and makedata_uwp.vcxproj\n\nThese references should have been removed in ICU-20489 and ICU-21420.\nTake the opportunity to update the doc as well.",
          "timestamp": "2022-03-31T11:23:29-07:00",
          "tree_id": "f609c16843cff6482f6dd06be11b759537e87d3b",
          "url": "https://github.com/unicode-org/icu/commit/a2c90781f14869f6fbd9267d03e155efa192b245"
        },
        "date": 1648751888087,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.3988,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.7987,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 5.0415,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.8241,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 6.2783,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.8392,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.3998,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.8103,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.6155,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.8375,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.7587,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "daju@microsoft.com",
            "name": "Daniel Ju",
            "username": "daniel-ju"
          },
          "committer": {
            "email": "41210545+daniel-ju@users.noreply.github.com",
            "name": "Daniel Ju",
            "username": "daniel-ju"
          },
          "distinct": true,
          "id": "f4f74c1f2ee380c20434f224d7fea05fdb4e91d6",
          "message": "ICU-21957 BRS72RC Update version number to 72.0.1",
          "timestamp": "2022-03-31T16:39:13-07:00",
          "tree_id": "490d3bcef2c5aed9fa04a64e006f6eea14f5098b",
          "url": "https://github.com/unicode-org/icu/commit/f4f74c1f2ee380c20434f224d7fea05fdb4e91d6"
        },
        "date": 1648770828136,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.4932,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6319,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9849,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.692,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7107,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6294,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.642,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4675,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3703,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.5118,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3372,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "4833cc89b2fae2e8863b46bf1dc785964847e882",
          "message": "ICU-20715 CollationDataBuilder reset outdated prefix+contraction values\n\nSee #2052",
          "timestamp": "2022-04-04T16:10:13Z",
          "tree_id": "32748a15222ff1509cad57b04d3294c0f402e8df",
          "url": "https://github.com/unicode-org/icu/commit/4833cc89b2fae2e8863b46bf1dc785964847e882"
        },
        "date": 1649089562498,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.4937,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6331,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9815,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.72,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7475,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6247,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6287,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4944,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3682,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3912,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3339,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "4747484273dc65a18a0469aca4d36840d64c10c6",
          "message": "ICU-21966 Cleanup -Wunused-but-set-variable\n\nSee #2055",
          "timestamp": "2022-04-06T12:51:46-07:00",
          "tree_id": "61c6000acd9127af32ccdebbe58a5458d0f63962",
          "url": "https://github.com/unicode-org/icu/commit/4747484273dc65a18a0469aca4d36840d64c10c6"
        },
        "date": 1649275650476,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.4775,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.009,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.6418,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7362,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6529,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6571,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4761,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3229,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.4233,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3681,
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
          "id": "87cee860e035e93cee30cc0dab1be8a697f42646",
          "message": "ICU-21984 Fix DateIntervalFormat.normalizeHourMetacharacters() so that it doesn't require the hour and day-period\nfields to appear in any particular order or position in the skeleton string.",
          "timestamp": "2022-04-11T14:37:15-07:00",
          "tree_id": "9d15834932f32762c8a0a350e9263a8dd2a38900",
          "url": "https://github.com/unicode-org/icu/commit/87cee860e035e93cee30cc0dab1be8a697f42646"
        },
        "date": 1649713956561,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.6148,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6285,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.3198,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.6143,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.0154,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6109,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6243,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.6226,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.6491,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.5333,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.7724,
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
            "email": "42151464+pedberg-icu@users.noreply.github.com",
            "name": "Peter Edberg",
            "username": "pedberg-icu"
          },
          "distinct": true,
          "id": "131146a5f43955eee68693e1e627df13da1ae384",
          "message": "ICU-21984 Fix DateIntervalFormat.normalizeHourMetacharacters() so that it doesn't require the hour and day-period\nfields to appear in any particular order or position in the skeleton string.",
          "timestamp": "2022-04-14T14:21:02-07:00",
          "tree_id": "fb805e6034ebad2b48accba6256dbdf1297371ac",
          "url": "https://github.com/unicode-org/icu/commit/131146a5f43955eee68693e1e627df13da1ae384"
        },
        "date": 1649972189778,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.5155,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6311,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9823,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.7104,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.716,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6273,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6383,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4907,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3739,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3858,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3479,
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
            "email": "noreply@github.com",
            "name": "GitHub",
            "username": "web-flow"
          },
          "distinct": true,
          "id": "47824c5568f0a1528c44e5264cb65e9e18705c18",
          "message": "ICU-21956 Merge maint-71 to main",
          "timestamp": "2022-04-14T17:45:38-07:00",
          "tree_id": "b53a205780b474ed928dcf2732513ac8de8743a6",
          "url": "https://github.com/unicode-org/icu/commit/47824c5568f0a1528c44e5264cb65e9e18705c18"
        },
        "date": 1649984425121,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.9989,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.9779,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.6375,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.9721,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.3655,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.9579,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.9592,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.0313,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.1093,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.9415,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.3076,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "fca6b342c03d89998eba227d284e232dabbfab47",
          "message": "ICU-21994 Fix heap-buffer-overflow",
          "timestamp": "2022-04-20T12:03:35-07:00",
          "tree_id": "09972e260fbe59552e69bb75887d064410144398",
          "url": "https://github.com/unicode-org/icu/commit/fca6b342c03d89998eba227d284e232dabbfab47"
        },
        "date": 1650482322254,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.2911,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.6272,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.8895,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.6484,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 6.1966,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.6908,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.3243,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.7869,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.5821,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.7313,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.5246,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "17109322+hugovdm@users.noreply.github.com",
            "name": "Hugo van der Merwe",
            "username": "hugovdm"
          },
          "committer": {
            "email": "17109322+hugovdm@users.noreply.github.com",
            "name": "Hugo van der Merwe",
            "username": "hugovdm"
          },
          "distinct": true,
          "id": "50e14fe15bcde3311f3e76bb196977a0724d7e8b",
          "message": "ICU-21959 Update stale Jira tickets in ICU4J TODOs\n\nSee #2062",
          "timestamp": "2022-04-27T23:31:46+02:00",
          "tree_id": "320adf839470614e0950cfe770d84936a3fe9df2",
          "url": "https://github.com/unicode-org/icu/commit/50e14fe15bcde3311f3e76bb196977a0724d7e8b"
        },
        "date": 1651095997129,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.2831,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.7404,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.9032,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.5945,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 6.0311,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.5551,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.3541,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.7039,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.5021,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.834,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.704,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "committer": {
            "email": "cwcornelius@gmail.com",
            "name": "Craig Cornelius",
            "username": "sven-oly"
          },
          "distinct": true,
          "id": "cb6b778e1b5ef6a1bb82e085aaf427fc086331c9",
          "message": "ICU-21959 Online demos how to update\n\nSee #2069",
          "timestamp": "2022-04-29T09:36:51-07:00",
          "tree_id": "0e8fc014a5ef382a0bcb4e65b3f1013839bed3db",
          "url": "https://github.com/unicode-org/icu/commit/cb6b778e1b5ef6a1bb82e085aaf427fc086331c9"
        },
        "date": 1651251152145,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.8692,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.9873,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.6,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.9566,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.5643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.0159,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.8971,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.9402,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.996,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.8454,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.0883,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "43d082665e72d853ef66626e0b17470722776497",
          "message": "ICU-22006 icupkg: %%ALIAS & %%Parent do not need truncation parent",
          "timestamp": "2022-04-29T17:50:11Z",
          "tree_id": "cc8b7de3a086a733ec693be2cd739669cd6d57c9",
          "url": "https://github.com/unicode-org/icu/commit/43d082665e72d853ef66626e0b17470722776497"
        },
        "date": 1651255469284,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.6105,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.9264,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.3086,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.9198,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.2772,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.9472,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6877,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.8949,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.601,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.8094,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.6913,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "505535813fd539800de0930883cdb0e0ba678842",
          "message": "ICU-22007 remove outdated terms of use from Unicode conversion files",
          "timestamp": "2022-04-29T19:51:01Z",
          "tree_id": "0648d877b9f7113b462aa8be66a35844db09208a",
          "url": "https://github.com/unicode-org/icu/commit/505535813fd539800de0930883cdb0e0ba678842"
        },
        "date": 1651262837941,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.4741,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6321,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9851,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.6996,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7451,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6247,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6361,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4923,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3721,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.5143,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3418,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "07a50207b7a0ae162a5b51856f43576f39751377",
          "message": "ICU-21679 avoid escape of U+FFFF in a character literal",
          "timestamp": "2022-04-29T20:43:53Z",
          "tree_id": "0eab5a9224a4cf8dcff516967a14f248572b25be",
          "url": "https://github.com/unicode-org/icu/commit/07a50207b7a0ae162a5b51856f43576f39751377"
        },
        "date": 1651265923968,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.8877,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.906,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.6392,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.9131,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.3746,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.8773,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.9159,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.9358,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.0038,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.905,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.0301,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "e96e9410bde06962c211fa6f21c3d91263a90f86",
          "message": "ICU-22005 Fix int32 overflow in FormattedStringBuilder\n\nSee #2070",
          "timestamp": "2022-04-29T18:25:01-07:00",
          "tree_id": "02cea832ea6354cb413ef3ac4167fba0fde781bd",
          "url": "https://github.com/unicode-org/icu/commit/e96e9410bde06962c211fa6f21c3d91263a90f86"
        },
        "date": 1651282764242,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.5119,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.631,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9845,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.6959,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7304,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6253,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6341,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.5204,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3834,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3858,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3437,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "eea7985e5a7108d00f1224ed36f0220fa9441cdc",
          "message": "ICU-22016 utrie2.h need not include mutex.h",
          "timestamp": "2022-05-06T20:50:58Z",
          "tree_id": "f2382974fa83f3747ccc3c7bf9e390d0f9d141dc",
          "url": "https://github.com/unicode-org/icu/commit/eea7985e5a7108d00f1224ed36f0220fa9441cdc"
        },
        "date": 1651871211402,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.112,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.1357,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.79,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.0777,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.4641,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.1229,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.0924,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.1228,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.2254,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.1051,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.3527,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "fda4a82bba405579a280b71922056c801c2b3ca2",
          "message": "ICU-21960 fix clang 13 C++20 warnings",
          "timestamp": "2022-05-07T15:21:08Z",
          "tree_id": "d4b6c9e51f35a2735c3c07de3993e596cb42e051",
          "url": "https://github.com/unicode-org/icu/commit/fda4a82bba405579a280b71922056c801c2b3ca2"
        },
        "date": 1651937808559,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.4535,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6325,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9824,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.6748,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7372,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6249,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6363,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4872,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3467,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3944,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3413,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "chetanladdha@microsoft.com",
            "name": "Chetan Laddha",
            "username": "chetanladdha"
          },
          "committer": {
            "email": "chetan.laddha@gmail.com",
            "name": "Chetan Laddha",
            "username": "chetanladdha"
          },
          "distinct": true,
          "id": "5961aacd3c66f3bb8f51d9b4833c03613c57dc01",
          "message": "ICU-21945 Add support for Visual Studio 2022\n\nSee #2059",
          "timestamp": "2022-05-10T22:57:18+05:30",
          "tree_id": "0a38273102e79b4fb0c96aae8fd320b79fae8c57",
          "url": "https://github.com/unicode-org/icu/commit/5961aacd3c66f3bb8f51d9b4833c03613c57dc01"
        },
        "date": 1652204615377,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.5292,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.8961,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 5.1482,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.9091,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 6.3449,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.8912,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.4912,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.9059,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.5988,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.9398,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.8731,
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
          "id": "bdcec144b9f627c8f96261a6afeebb52e4d81ab0",
          "message": "ICU-22012 Add four Japanese word into the dictionary\n\nSee #2072",
          "timestamp": "2022-05-11T08:19:53-07:00",
          "tree_id": "85e2beca4e203db5c621d2700bc85b0a8c9b1b52",
          "url": "https://github.com/unicode-org/icu/commit/bdcec144b9f627c8f96261a6afeebb52e4d81ab0"
        },
        "date": 1652283355728,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.493,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6325,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9856,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.7024,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7357,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6276,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.645,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.5178,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3881,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3879,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3334,
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
          "id": "f8a0810a5a9a8811d13e1e4d9ef266ef5a102729",
          "message": "ICU-22030 Modifies GHA CI performance testing so that existing files in the\nperformance results publishing repository are no longer deleted when the test\nresults are forwarded. This concretely affects the README file in the icu-perf\nrepository, which got deleted with the first data transfer.\n\nRestoring the README file in icu-perf will be a complementary PR.",
          "timestamp": "2022-05-13T17:00:05-07:00",
          "tree_id": "9ba8576b904411dc587d6603ba71189588114912",
          "url": "https://github.com/unicode-org/icu/commit/f8a0810a5a9a8811d13e1e4d9ef266ef5a102729"
        },
        "date": 1652487345848,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 5.1736,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 5.3288,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.6626,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 5.5231,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.52,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.4296,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.3205,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.2143,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.2882,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.1995,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.1411,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "andy.heninger@gmail.com",
            "name": "Andy Heninger",
            "username": "aheninger"
          },
          "committer": {
            "email": "andy.heninger@gmail.com",
            "name": "Andy Heninger",
            "username": "aheninger"
          },
          "distinct": true,
          "id": "85705f04e03f9cb41d4406bcd38c3e419eb7889d",
          "message": "ICU-21960 C++20 Warnings from ATOMIC_VAR_INIT\n\nRemove the ICU macros ATOMIC_INT32_T_INITIALIZER and U_INITONCE_INITIALIZER,\nwhich made use of C++ ATOMIC_VAR_INIT, which has been removed from C++20.\n\nWith modern C++ features being available, these macros no longer served\nany real need.",
          "timestamp": "2022-05-17T15:45:06-07:00",
          "tree_id": "4f972e8ea8588a819020277d8c518b5102e52695",
          "url": "https://github.com/unicode-org/icu/commit/85705f04e03f9cb41d4406bcd38c3e419eb7889d"
        },
        "date": 1652828384461,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 3.9653,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.0855,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.5157,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.131,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.1738,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.0792,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.0863,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 3.9786,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 4.7232,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 4.7493,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 4.7157,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "3d89af0f72d78d0825fbf17fd7808a4d80b6b4ad",
          "message": "ICU-22023 Fix Calendar::get() return out of bound value and SimpleDateTime::format assert while TimeZone is \"UTC\" and value is -1e-9\n\nSee #2086",
          "timestamp": "2022-05-19T13:45:59-07:00",
          "tree_id": "b9cf8e61607c058b52fb71efef44dedd843e1ae7",
          "url": "https://github.com/unicode-org/icu/commit/3d89af0f72d78d0825fbf17fd7808a4d80b6b4ad"
        },
        "date": 1652994040057,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.4615,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6386,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9834,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.6773,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7286,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6256,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6372,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.5104,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.368,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3929,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3338,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "29107334+jefgen@users.noreply.github.com",
            "name": "Jeff Genovy",
            "username": "jefgen"
          },
          "committer": {
            "email": "29107334+jefgen@users.noreply.github.com",
            "name": "Jeff Genovy",
            "username": "jefgen"
          },
          "distinct": true,
          "id": "f6300c97cd4b8f1224776e43226d3a8bbb695c2c",
          "message": "ICU-22040 Update clang-13 build bots to clang-14. Add libc++ build bot.",
          "timestamp": "2022-05-19T14:05:14-07:00",
          "tree_id": "4a40a3af51ba2ebe33f2fbb66613585625f0a00c",
          "url": "https://github.com/unicode-org/icu/commit/f6300c97cd4b8f1224776e43226d3a8bbb695c2c"
        },
        "date": 1652995186357,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.6563,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.7127,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.0562,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.9433,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.8782,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.7731,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6936,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.6365,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.5148,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.5471,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.4961,
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
          "id": "8fcb22f88e7100cd714861b672a3137c7d431a01",
          "message": "ICU-22037 Adds performance tests for ICU forward and backward\nstring search.\n\nICU-22037 Removes a left-over escaped doublequote; removes a\ntest print-out.",
          "timestamp": "2022-05-20T11:56:11-07:00",
          "tree_id": "52eaabe6b81058bf46f32476bad6441fe6d1175c",
          "url": "https://github.com/unicode-org/icu/commit/8fcb22f88e7100cd714861b672a3137c7d431a01"
        },
        "date": 1653073897373,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.3993,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.4234,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.4307,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.3985,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.9827,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.4027,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.4256,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4046,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.5563,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.5545,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3033,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "younies@chromium.org",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "committer": {
            "email": "younies@chromium.org",
            "name": "Younies Mahmoud",
            "username": "younies"
          },
          "distinct": true,
          "id": "fcc981a5b7a11628187ee6a1f97203449b48bd36",
          "message": "ICU-21935 Add DisplayOptions\n\nSee #2061",
          "timestamp": "2022-05-21T03:20:37+02:00",
          "tree_id": "11b85ea79fcb3796ba2871ae8602fce888bb0309",
          "url": "https://github.com/unicode-org/icu/commit/fcc981a5b7a11628187ee6a1f97203449b48bd36"
        },
        "date": 1653096959531,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.3531,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.373,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.3729,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.353,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.8045,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.353,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.373,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.3531,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.4879,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.4928,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.204,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "roubert@google.com",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "committer": {
            "email": "fredrik@roubert.name",
            "name": "Fredrik Roubert",
            "username": "roubert"
          },
          "distinct": true,
          "id": "74a723f22fe2c53445087215d0d1acee69efdda6",
          "message": "ICU-21959 Fix the URL to the icu-le-hb repository.\n\nhttps://sourceforge.net/p/icu/mailman/message/37657918/",
          "timestamp": "2022-05-24T20:55:00+02:00",
          "tree_id": "9acb2514b5ff0f3877d1f8808f75c88511903b33",
          "url": "https://github.com/unicode-org/icu/commit/74a723f22fe2c53445087215d0d1acee69efdda6"
        },
        "date": 1653419425300,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.6926,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6907,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 4.418,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.7157,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 5.0733,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 5.0422,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 5.0479,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 5.0505,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 6.1643,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 6.033,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 6.2845,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "ftang@chromium.org",
            "name": "Frank Tang",
            "username": "FrankYFTang"
          },
          "committer": {
            "email": "ftang@google.com",
            "name": "Frank Yung-Fong Tang",
            "username": "FrankYFTang"
          },
          "distinct": true,
          "id": "f5f4813d161d685a807aa5ea25efa685b0f6eaeb",
          "message": "ICU-22043 Change minimum ZONE_OFFSET value from -12h to -16h\n\nSee #2100",
          "timestamp": "2022-05-24T16:11:39-07:00",
          "tree_id": "affebcf816f9f90df71ea765d812209d8e6490d9",
          "url": "https://github.com/unicode-org/icu/commit/f5f4813d161d685a807aa5ea25efa685b0f6eaeb"
        },
        "date": 1653434881739,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.4788,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6311,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9809,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.7113,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7511,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6255,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6434,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.4622,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3628,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3999,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3454,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      },
      {
        "commit": {
          "author": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "committer": {
            "email": "markus.icu@gmail.com",
            "name": "Markus Scherer",
            "username": "markusicu"
          },
          "distinct": true,
          "id": "e17219582ed0396d993927066e95af0c9199f8db",
          "message": "ICU-21980 add missing comments, finish change log",
          "timestamp": "2022-05-25T18:23:11Z",
          "tree_id": "f3a73d2dbd02b2945b6b3253c0c5e0532259f4fd",
          "url": "https://github.com/unicode-org/icu/commit/e17219582ed0396d993927066e95af0c9199f8db"
        },
        "date": 1653503944400,
        "tool": "ndjson",
        "benches": [
          {
            "name": "TestIsAlpha",
            "value": 4.4769,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsUpper",
            "value": 4.6331,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsLower",
            "value": 3.9841,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsDigit",
            "value": 4.7191,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsSpace",
            "value": 4.7304,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsAlphaNumeric",
            "value": 4.6242,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsPrint",
            "value": 4.6281,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsControl",
            "value": 4.5209,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToLower",
            "value": 5.3722,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestToUpper",
            "value": 5.3958,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "TestIsWhiteSpace",
            "value": 5.3453,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      }
    ]
  }
}