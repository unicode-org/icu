window.BENCHMARK_DATA = {
  "lastUpdate": 1646784694895,
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
      }
    ]
  }
}