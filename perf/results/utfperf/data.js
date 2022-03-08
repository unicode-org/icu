window.BENCHMARK_DATA = {
  "lastUpdate": 1646781493348,
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
      }
    ]
  }
}