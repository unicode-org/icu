window.BENCHMARK_DATA = {
  "lastUpdate": 1646687305074,
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
        "date": 1646687303485,
        "tool": "ndjson",
        "benches": [
          {
            "name": "titlecase_letter_add",
            "value": 57753.3272,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "titlecase_letter_contains",
            "value": 115436.0109,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "titlecase_letter_iterator",
            "value": 0.4404,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "unassigned_add",
            "value": 7.2914,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "unassigned_contains",
            "value": 4.3124,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "unassigned_iterator",
            "value": 0,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "pattern1",
            "value": 11716.2784,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "pattern2",
            "value": 40376.3128,
            "unit": "ns/iter",
            "biggerIsBetter": false
          },
          {
            "name": "pattern3",
            "value": 1997.3709,
            "unit": "ns/iter",
            "biggerIsBetter": false
          }
        ]
      }
    ]
  }
}