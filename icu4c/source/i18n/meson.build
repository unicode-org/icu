fs = import('fs')
sources = fs.read('sources.txt').split()

if host_machine.system() == 'windows'
  sources += windows.compile_resources('i18n.rc', include_directories: incdir)
endif

if host_machine.system() == 'windows'
  i18n_name = '@0@icuin@1@'.format(library_prefix, library_suffix)
else
  i18n_name = '@0@icui18n@1@'.format(library_prefix, library_suffix)
endif

i18n_lib = library(
  i18n_name,
  sources,
  dependencies: icuuc_dep,
  c_args: '-DU_I18N_IMPLEMENTATION',
  cpp_args: '-DU_I18N_IMPLEMENTATION',
  version: U_ICU_VERSION,
  install: true,
)

if meson.can_run_host_binaries()
  i18n_native_lib = i18n_lib
else
  i18n_native_lib = library(
    '@0@-native'.format(i18n_name),
    sources,
    dependencies: icuuc_native_dep,
    c_args: '-DU_I18N_IMPLEMENTATION',
    cpp_args: '-DU_I18N_IMPLEMENTATION',
    version: U_ICU_VERSION,
    native: true,
  )
endif

i18ninc = include_directories('.')

icui18n_dep = declare_dependency(
  include_directories: i18ninc,
  link_with: i18n_lib,
  dependencies: icuuc_dep,
)

icui18n_native_dep = declare_dependency(
  include_directories: i18ninc,
  link_with: i18n_native_lib,
  dependencies: icuuc_native_dep,
)

if meson.version().version_compare('>=0.54.0')
  meson.override_dependency('icu-i18n', icui18n_dep)
endif

headers = files(
  'unicode/alphaindex.h',
  'unicode/basictz.h',
  'unicode/calendar.h',
  'unicode/choicfmt.h',
  'unicode/coleitr.h',
  'unicode/coll.h',
  'unicode/compactdecimalformat.h',
  'unicode/curramt.h',
  'unicode/currpinf.h',
  'unicode/currunit.h',
  'unicode/datefmt.h',
  'unicode/dcfmtsym.h',
  'unicode/decimfmt.h',
  'unicode/displayoptions.h',
  'unicode/dtfmtsym.h',
  'unicode/dtitvfmt.h',
  'unicode/dtitvinf.h',
  'unicode/dtptngen.h',
  'unicode/dtrule.h',
  'unicode/fieldpos.h',
  'unicode/fmtable.h',
  'unicode/format.h',
  'unicode/formattedvalue.h',
  'unicode/fpositer.h',
  'unicode/gender.h',
  'unicode/gregocal.h',
  'unicode/listformatter.h',
  'unicode/measfmt.h',
  'unicode/measunit.h',
  'unicode/measure.h',
  'unicode/msgfmt.h',
  'unicode/nounit.h',
  'unicode/numberformatter.h',
  'unicode/numberrangeformatter.h',
  'unicode/numfmt.h',
  'unicode/numsys.h',
  'unicode/plurfmt.h',
  'unicode/plurrule.h',
  'unicode/rbnf.h',
  'unicode/rbtz.h',
  'unicode/regex.h',
  'unicode/region.h',
  'unicode/reldatefmt.h',
  'unicode/scientificnumberformatter.h',
  'unicode/search.h',
  'unicode/selfmt.h',
  'unicode/simpletz.h',
  'unicode/smpdtfmt.h',
  'unicode/sortkey.h',
  'unicode/stsearch.h',
  'unicode/tblcoll.h',
  'unicode/timezone.h',
  'unicode/tmunit.h',
  'unicode/tmutamt.h',
  'unicode/tmutfmt.h',
  'unicode/translit.h',
  'unicode/tzfmt.h',
  'unicode/tznames.h',
  'unicode/tzrule.h',
  'unicode/tztrans.h',
  'unicode/ucal.h',
  'unicode/ucol.h',
  'unicode/ucoleitr.h',
  'unicode/ucsdet.h',
  'unicode/udat.h',
  'unicode/udateintervalformat.h',
  'unicode/udatpg.h',
  'unicode/udisplayoptions.h',
  'unicode/ufieldpositer.h',
  'unicode/uformattable.h',
  'unicode/uformattedvalue.h',
  'unicode/ugender.h',
  'unicode/ulistformatter.h',
  'unicode/ulocdata.h',
  'unicode/umsg.h',
  'unicode/unirepl.h',
  'unicode/unum.h',
  'unicode/unumberformatter.h',
  'unicode/unumberrangeformatter.h',
  'unicode/unumsys.h',
  'unicode/upluralrules.h',
  'unicode/uregex.h',
  'unicode/uregion.h',
  'unicode/ureldatefmt.h',
  'unicode/usearch.h',
  'unicode/uspoof.h',
  'unicode/utmscale.h',
  'unicode/utrans.h',
  'unicode/vtzone.h',
)

install_headers(
  headers,
  subdir: 'unicode',
)

pkg.generate(
  i18n_lib,
  name: 'icu-i18n',
  description: '@0@: Internationalization library'.format(PACKAGE_ICU_DESCRIPTION),
  url: PACKAGE_ICU_URL,
)
