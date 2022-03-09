window.BENCHMARK_DATA = {
  "lastUpdate": 1646853485039,
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
        "date": 1646686991717,
        "tool": "ndjson",
        "benches": [
          {
            "name": "Roundtrip",
            "value": 14.4107,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUnicode",
            "value": 4.7283,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUTF8",
            "value": 2.7559,
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
        "date": 1646781492255,
        "tool": "ndjson",
        "benches": [
          {
            "name": "Roundtrip",
            "value": 14.0187,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUnicode",
            "value": 4.7375,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUTF8",
            "value": 2.7719,
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
        "date": 1646782137311,
        "tool": "ndjson",
        "benches": [
          {
            "name": "Roundtrip",
            "value": 13.4276,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUnicode",
            "value": 5.0191,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUTF8",
            "value": 3.5586,
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
        "date": 1646782820113,
        "tool": "ndjson",
        "benches": [
          {
            "name": "Roundtrip",
            "value": 13.466,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUnicode",
            "value": 5.0381,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUTF8",
            "value": 3.5485,
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
        "date": 1646784267791,
        "tool": "ndjson",
        "benches": [
          {
            "name": "Roundtrip",
            "value": 14.8854,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUnicode",
            "value": 4.9442,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUTF8",
            "value": 2.8724,
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
        "date": 1646797182442,
        "tool": "ndjson",
        "benches": [
          {
            "name": "Roundtrip",
            "value": 11.8632,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUnicode",
            "value": 5.0685,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUTF8",
            "value": 3.5415,
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
        "date": 1646797737172,
        "tool": "ndjson",
        "benches": [
          {
            "name": "Roundtrip",
            "value": 16.3777,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUnicode",
            "value": 6.1133,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUTF8",
            "value": 4.3052,
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
        "date": 1646853482651,
        "tool": "ndjson",
        "benches": [
          {
            "name": "Roundtrip",
            "value": 13.0256,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUnicode",
            "value": 4.4246,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "FromUTF8",
            "value": 2.547,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      }
    ]
  }
}