fs = import('fs')
sources = fs.read('sources.txt').split()

if host_machine.system() == 'windows'
  sources += windows.compile_resources('io.rc', include_directories: incdir)
endif

io_lib = library(
  'icuio',
  sources,
  c_args: '-DU_IO_IMPLEMENTATION',
  cpp_args: '-DU_IO_IMPLEMENTATION',
  dependencies: icui18n_dep,
  version: U_ICU_VERSION,
  install: true,
)

icuio_dep = declare_dependency(
  link_with: io_lib,
  dependencies: icui18n_dep,
)

if meson.version().version_compare('>=0.54.0')
  meson.override_dependency('icu-io', icuio_dep)
endif

pkg.generate(
  io_lib,
  name: 'icu-io',
  description: '@0@: Stream and I/O Library'.format(PACKAGE_ICU_DESCRIPTION),
  url: PACKAGE_ICU_URL,
)
