fs = import('fs')
sources = fs.read('sources.txt').split()

if host_machine.system() == 'windows'
  sources += windows.compile_resources('common.rc', include_directories: incdir)
endif

icuuc_name = '@0@icuuc@1@'.format(library_prefix, library_suffix)

common_lib = library(
  icuuc_name,
  sources,
  include_directories: incdir,
  c_args: '-DU_COMMON_IMPLEMENTATION',
  cpp_args: '-DU_COMMON_IMPLEMENTATION',
  link_with: stubdata_lib,
  dependencies: dl_dep,
  version: U_ICU_VERSION,
  install: true,
)

if meson.can_run_host_binaries()
  common_native_lib = common_lib
else
  common_native_lib = library(
    '@0@-native'.format(icuuc_name),
    sources,
    include_directories: incdir,
    c_args: '-DU_COMMON_IMPLEMENTATION',
    cpp_args: '-DU_COMMON_IMPLEMENTATION',
    link_with: stubdata_native_lib,
    dependencies: dl_native_dep,
    version: U_ICU_VERSION,
    native: true,
  )
endif

icuuc_dep = declare_dependency(
  link_with: common_lib,
  compile_args: usage_args,
  include_directories: incdir,
  dependencies: dl_dep,
)

icuuc_native_dep = declare_dependency(
  link_with: common_native_lib,
  compile_args: usage_args,
  include_directories: incdir,
  dependencies: dl_native_dep,
)

if meson.version().version_compare('>=0.54.0')
  meson.override_dependency('icu-uc', icuuc_dep)
endif

headers = files(
  'unicode/appendable.h',
  'unicode/brkiter.h',
  'unicode/bytestream.h',
  'unicode/bytestrie.h',
  'unicode/bytestriebuilder.h',
  'unicode/caniter.h',
  'unicode/casemap.h',
  'unicode/char16ptr.h',
  'unicode/chariter.h',
  'unicode/dbbi.h',
  'unicode/docmain.h',
  'unicode/dtintrv.h',
  'unicode/edits.h',
  'unicode/enumset.h',
  'unicode/errorcode.h',
  'unicode/filteredbrk.h',
  'unicode/icudataver.h',
  'unicode/icuplug.h',
  'unicode/idna.h',
  'unicode/localebuilder.h',
  'unicode/localematcher.h',
  'unicode/localpointer.h',
  'unicode/locdspnm.h',
  'unicode/locid.h',
  'unicode/messagepattern.h',
  'unicode/normalizer2.h',
  'unicode/normlzr.h',
  'unicode/parseerr.h',
  'unicode/parsepos.h',
  'unicode/platform.h',
  'unicode/ptypes.h',
  'unicode/putil.h',
  'unicode/rbbi.h',
  'unicode/rep.h',
  'unicode/resbund.h',
  'unicode/schriter.h',
  'unicode/simpleformatter.h',
  'unicode/std_string.h',
  'unicode/strenum.h',
  'unicode/stringoptions.h',
  'unicode/stringpiece.h',
  'unicode/stringtriebuilder.h',
  'unicode/symtable.h',
  'unicode/ubidi.h',
  'unicode/ubiditransform.h',
  'unicode/ubrk.h',
  'unicode/ucasemap.h',
  'unicode/ucat.h',
  'unicode/uchar.h',
  'unicode/ucharstrie.h',
  'unicode/ucharstriebuilder.h',
  'unicode/uchriter.h',
  'unicode/uclean.h',
  'unicode/ucnv.h',
  'unicode/ucnv_cb.h',
  'unicode/ucnv_err.h',
  'unicode/ucnvsel.h',
  'unicode/uconfig.h',
  'unicode/ucpmap.h',
  'unicode/ucptrie.h',
  'unicode/ucurr.h',
  'unicode/udata.h',
  'unicode/udisplaycontext.h',
  'unicode/uenum.h',
  'unicode/uidna.h',
  'unicode/uiter.h',
  'unicode/uldnames.h',
  'unicode/uloc.h',
  'unicode/umachine.h',
  'unicode/umisc.h',
  'unicode/umutablecptrie.h',
  'unicode/unifilt.h',
  'unicode/unifunct.h',
  'unicode/unimatch.h',
  'unicode/uniset.h',
  'unicode/unistr.h',
  'unicode/unorm.h',
  'unicode/unorm2.h',
  'unicode/uobject.h',
  'unicode/urename.h',
  'unicode/urep.h',
  'unicode/ures.h',
  'unicode/uscript.h',
  'unicode/uset.h',
  'unicode/usetiter.h',
  'unicode/ushape.h',
  'unicode/usprep.h',
  'unicode/ustring.h',
  'unicode/ustringtrie.h',
  'unicode/utext.h',
  'unicode/utf.h',
  'unicode/utf16.h',
  'unicode/utf32.h',
  'unicode/utf8.h',
  'unicode/utf_old.h',
  'unicode/utrace.h',
  'unicode/utypes.h',
  'unicode/uvernum.h',
  'unicode/uversion.h',
)

install_headers(
  headers,
  subdir: 'unicode',
)

pkg.generate(
  common_lib,
  name: 'icu-uc',
  description: '@0@: Common and Data libraries'.format(PACKAGE_ICU_DESCRIPTION),
  url: PACKAGE_ICU_URL,
)
