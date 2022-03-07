window.BENCHMARK_DATA = {
  "lastUpdate": 1646686993281,
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
      }
    ]
  }
}