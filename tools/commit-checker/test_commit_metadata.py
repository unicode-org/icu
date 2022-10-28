# Copyright (C) 2022 and later: Unicode, Inc. and others.
# License & terms of use: http://www.unicode.org/copyright.html
# Author: srloomis@unicode.org

from commit_metadata import CommitMetadata

def test_hash():
    assert CommitMetadata.match_commit("512a679aba", "512a679aba22e144a4443df5f8f304e4f8b39054")
    assert CommitMetadata.match_commit("512a679aba", "512a679aba")
    assert CommitMetadata.match_commit("512a679aba22e144a4443df5f8f304e4f8b39054", "512a679aba22e144a4443df5f8f304e4f8b39054")
    assert not CommitMetadata.match_commit("512a679aba", "16aae80199")
    assert not CommitMetadata.match_commit("512a679aba22e144a4443df5f8f304e4f8b39054", "16aae80199")

def test_matchcommit():
    assert CommitMetadata.match_list("512a679aba", [("00decafbad", 44), ("512a679aba", 99)]) == ("512a679aba", 99)
    assert CommitMetadata.match_list("512a679aba22e144a4443df5f8f304e4f8b39054", [("00decafbad", 44), ("512a679aba", 99)]) == ("512a679aba", 99)
    assert CommitMetadata.match_list("512a679aba", [("00decafbad", 44), ("512a679aba22e144a4443df5f8f304e4f8b39054", 99)]) == ("512a679aba22e144a4443df5f8f304e4f8b39054", 99)
    assert not CommitMetadata.match_list("16aae80199", [("00decafbad", 44), ("512a679aba", 99)])
    assert not CommitMetadata.match_list("16aae80199", [("00decafbad", 44), ("512a679aba22e144a4443df5f8f304e4f8b39054", 99)])

def test_read():
    m = CommitMetadata(metadata_file="./TEST_COMMIT_METADATA.md")
    assert m

    assert not m.get_commit_info('00000000')  # not in list
    assert m.get_commit_info('00decafbad')
    assert m.get_commit_info('00decafbad')[1].startswith('CLDR-0000')
    assert m.get_commit_info('56ca5d5')
    assert m.get_commit_info('56ca5d5')[1].startswith('CLDR-14877')
    # short or long, same
    assert m.get_commit_info('56ca5d5') == m.get_commit_info('56ca5d563cf57990a7598f570cb9be51956cb9de')

    # skip list
    assert m.get_commit_info('56ca5d5', skip='v41')
    assert not m.get_commit_info('56ca5d5', skip='v42')
    assert m.get_commit_info('56ca5d563cf57990a7598f570cb9be51956cb9de', skip='v41')
    assert not m.get_commit_info('56ca5d563cf57990a7598f570cb9be51956cb9de', skip='v42')
    assert not m.get_commit_info('00decafbad', 'v41')

def test_null_read():
    m = CommitMetadata(metadata_file=None)
    assert m

    # function with no info
    assert not m.get_commit_info('00000000')  # not in list
    assert not m.get_commit_info('00decafbad')
    assert not m.get_commit_info('56ca5d5')
    assert not m.get_commit_info('56ca5d5', skip='v41')
    assert not m.get_commit_info('56ca5d5', skip='v42')

def test_parse_42():
    m = CommitMetadata(metadata_file="./TEST_COMMIT_METADATA.md")
    assert m

    # no skip
    info = m.get_commit_info('02198373a591a15b804127acddd32582ec985b7e')
    assert info
    assert info[0] == '02198373a591a15b804127acddd32582ec985b7e'
    assert info[1] == 'CLDR-15852'
    assert info[2] == 'v42 merge commit'

    # skip
    info = m.get_commit_info('02198373a591a15b804127acddd32582ec985b7e', skip='v42')
    # not found because it isn't in SKIP v42
    assert not info
