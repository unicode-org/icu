window.BENCHMARK_DATA = {
  "lastUpdate": 1646781940281,
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
      }
    ]
  }
}