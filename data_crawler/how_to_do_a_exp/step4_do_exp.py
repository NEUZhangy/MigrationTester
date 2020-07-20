import os
import sys
from sys import argv

if __name__ == "__main__":
  dic = {'myfile':1}
  filename=argv[1]
  file_object = open(filename)
  try:
    for line in file_object:
      if line == '\n':
        continue
      oneline = line.split(',')
      # print oneline
      folder = oneline[0].split('/')[1]
      if folder in dic:
        continue
      dic[folder] = 1      
      print 'the folder:' + folder
      s1 = 'cp sztoolbox/* ' + folder + '/'
      os.system(s1) 
      os.system('mkdir ' + folder+'/backup')
      os.system('mkdir ' + folder+'/backup/onlypom')
      towrite = '\n'.join(oneline[2:])    
      os.chdir(folder)
      fi = open('cmlist.txt', 'w')
      fi.write(towrite)
      fi.close( )
      os.system('python git-diff.py')     
      os.chdir('..')
      # os.system('mkdir allinall/' + folder)
      # print os.system('pwd')
      s3 = 'mv '
      s3 += folder
      s3 += '/backup allinall/'
      os.system(s3)
      os.system('mv allinall/backup allinall/' + str(oneline[1]) + folder)
  finally:
    file_object.close()
