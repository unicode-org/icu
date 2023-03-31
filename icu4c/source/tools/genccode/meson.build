fs = import('fs')
sources = fs.read('sources.txt').split()

genccode_exe = executable(
  'genccode',
  sources,
  dependencies: toolutil_dep,
  install: true,
)

if meson.can_run_host_binaries()
  genccode_native_exe = genccode_exe
else
  genccode_native_exe = executable(
    'genccode-native',
    'genccode.c',
    dependencies: toolutil_native_dep,
    native: true,
  )
endif

meson.override_find_program('genccode', genccode_exe)
