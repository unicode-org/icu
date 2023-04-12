fs = import('fs')
sources = fs.read('sources.txt').split()

genbrk_exe = executable(
  'genbrk',
  sources,
  dependencies: toolutil_dep,
  install: true,
)

meson.override_find_program('genbrk', genbrk_exe)
