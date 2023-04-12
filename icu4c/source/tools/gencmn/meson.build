fs = import('fs')
sources = fs.read('sources.txt').split()

gencmn_exe = executable(
  'gencmn',
  sources,
  dependencies: toolutil_dep,
  install: true,
)

meson.override_find_program('gencmn', gencmn_exe)
