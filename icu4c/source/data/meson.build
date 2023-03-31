U_ICUDATA_NAME = 'icudt@0@'.format(U_ICU_VERSION.split('.')[0])

icudata_command = [
  genccode_native_exe,
  '-d',
  '@OUTDIR@',
  '-e',
  U_ICUDATA_NAME,
  '-f',
  U_ICUDATA_NAME,
  '@CURRENT_SOURCE_DIR@/in/@0@l.dat'.format(U_ICUDATA_NAME),
]

if host_machine.system() == 'windows' and get_option('default_library') == 'static'
  icudata_command += ['--skip-dll-export']
endif

if cpp.get_argument_syntax() == 'msvc'
  ## Uncomment if you want to use MASM (>= 0.64), but be aware
  ## it is Very Slow to build.
  # if add_languages('masm', required: false)
  #   icudata_asm_output = '@0@.masm'.format(U_ICUDATA_NAME)
  #   icudata_command += ['-a', 'masm']
  # else
  icudata_asm_output = '@0@.c'.format(U_ICUDATA_NAME)
  # endif
elif host_machine.system() == 'cygwin'
  icudata_asm_output = '@0@.S'.format(U_ICUDATA_NAME)
  icudata_command += ['-a', 'gcc-cygwin']
elif host_machine.system() == 'darwin'
  icudata_asm_output = '@0@.S'.format(U_ICUDATA_NAME)
  icudata_command += ['-a', 'gcc-darwin']
elif cpp.get_id() == 'gcc' or cpp.get_id() == 'clang'
  icudata_asm_output = '@0@.S'.format(U_ICUDATA_NAME)
  if host_machine.system() == 'windows'
    icudata_command += ['-a', 'gcc-mingw64']
  else
    icudata_command += ['-a', 'gcc']
  endif
else
  icudata_asm_output = '@0@.c'.format(U_ICUDATA_NAME)
endif

icudata_asm = custom_target(
  'icudata_asm',
  command: icudata_command,
  output: icudata_asm_output,
)

sources = [icudata_asm]

python_exe = import('python').find_installation('python3')

icudata_exports = custom_target(
  'icudata_exports',
  command: [python_exe, '@INPUT@', '@0@_dat'.format(U_ICUDATA_NAME), '@OUTDIR@/module.def'],
  input: 'export_module.py',
  output: 'module.def',
)

if host_machine.system() == 'windows'
  sources += windows.compile_resources('misc/icudata.rc', include_directories: incdir)
endif

if cpp.get_id() != 'msvc'
  icudata = library(
    icudata_name,
    sources,
    include_directories: incdir,
    version: U_ICU_VERSION,
    vs_module_defs: icudata_exports,
    install: true,
  )

  icudata_dep = declare_dependency(
    link_with: icudata,
    include_directories: incdir,
    compile_args: usage_args,
  )

  if meson.version().version_compare('>=0.54.0')
    meson.override_dependency('icu-data', icudata_dep)
  endif
endif
