"""Extract abnf rules from java files and compare with spec abnf file."""
import os
import re
import sys

_ENTRY_DEF = re.compile(r'^(\S+)\s*=\s*(.+)')
_ENTRY_CONTINUATION = re.compile(r'^\s+/ (.*)')


def _parse_rule(abnf_rules: dict[str, str], context:tuple[str, str], line: str) -> tuple[str, str]:
  if line.startswith(' '):
    desc = context[1] + '\n' + line
    context = (context[0], desc)
  else:
    match = _ENTRY_DEF.match(line)
    if match:
      if context[0] != '':
        abnf_rules[context[0]] = context[1]
        context = ('', '')
      else:
        context = (match.group(1), match.group(2))
  return context

def _save_rule(abnf_rules: dict[str, str], context:tuple[str, str]):
  if context[0] != '':
    if context[0] in abnf_rules:
      if context[1] != abnf_rules[context[0]]:
        print(f'\x1b[91mERROR: DUPLICATE RULE: {context[0]} =>\n    new: {context[1]}\n    old: {abnf_rules[context[0]]}\x1b[m', file=sys.stderr)
    abnf_rules[context[0]] = context[1]
    # print(f'ADDING RULE: {context[0]} = {context[1]}')
  return ('', '')

def _get_abnf_from_java(abnf_rules: dict[str, str], file_name: str):
  with open(file_name, 'r', encoding='utf-8') as file:
    context = ('', '')
    for line in file:
      line = line.rstrip('\n\r')
      if 'abnf:' in line:
        line = line.strip()
        if line.startswith('// abnf: '):
          line = line[9:]
        elif line.startswith('* abnf: '):
          line = line[8:]
        else:
          print(f'\x1b[91mERROR: BAD ABNF: file:{file_name}, line:{line}\x1b[m', file=sys.stderr)
        match_declaration = _ENTRY_DEF.match(line)
        match_continuation = _ENTRY_CONTINUATION.match(line)
        if match_declaration:
          _save_rule(abnf_rules, context)
          context = (match_declaration.group(1), match_declaration.group(2))
          # print(f'NEW RULE: {context[0]} = {context[1]}')
        elif match_continuation:
          context = (context[0], context[1] + '\n        / ' + match_continuation.group(1))
          # print(f'CONTINUTATION:     / {context[1]}')
        else:
          # print(f'DEFAULT:     {context[0]} ::: {context[1]}')
          context = _save_rule(abnf_rules, context)
    context = _save_rule(abnf_rules, context)

def get_abnf_from_abnf(abnf_rules: dict[str, str], file_name: str):
  with open(file_name, 'r', encoding='utf-8') as file:
    for line in file:
      line = line.rstrip('\n\r')
      if line.startswith(';'):
        print(line)
      elif line == '':
        print(line)
      elif _ENTRY_CONTINUATION.match(line):
        pass # do nothing, ignore continuation lines
      else:
        match = _ENTRY_DEF.match(line)
        if match:
          rule_name = match.group(1)
          # print(f'SEARCHING: {rule_name}')
          if rule_name in abnf_rules:
            print(f'{rule_name} = {abnf_rules[rule_name]}')
            del abnf_rules[rule_name]
          else:
            line = re.sub(r' += +', ' = ', line)
            print(f'# TODO: {line}')
        else:
          print(f'\x1b[91mERROR: BAD ABNF: file:{file_name}, line:"{line}"\x1b[m', file=sys.stderr)

def main():
  # define an empty dictionary
  abnf_rules: dict[str, str] = {}
  directory: str = '.'
  for root, _, files in os.walk(directory):
    for file in files:
      if file.endswith('.java'):
        _get_abnf_from_java(abnf_rules, os.path.join(root, file))
  get_abnf_from_abnf(abnf_rules, 'message.abnf')
  if len(abnf_rules):
    print('; ================================================')
    for name, defn in abnf_rules.items():
      print(f'{name} = {defn}')

if __name__ == '__main__':
  main()
