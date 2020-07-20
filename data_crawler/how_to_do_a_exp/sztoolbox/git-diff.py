import os
import sys

def process(x):
  if x == '\n':
    print 'empty line'
    return

  if x[len(x)-1] == '\n':
    x = x[0:len(x)-1]
  comits = x
  ss = "sh git-diff-coppy.sh "
  ss += comits
  ss += "^ "
  ss += comits
  ss += " "
  ss += comits
  print ss
  os.system(ss)

  count = 0
  for dirpath, dirnames, filenames in os.walk(comits):
    for filename in filenames:
      if filename.endswith('java'):
        count+=1
  if count == 0:
    smv = "mv "
    smv += comits
    smv += " backup/onlypom/"
    smv += comits
    os.system(smv)
  else:
    smv = "mv "
    smv += comits
    smv += " backup/"
    smv += comits
    os.system(smv)


if __name__ == "__main__":
  file_object = open('cmlist.txt')
  try:
    for line in file_object:
      process(line)
  finally:
    file_object.close()

