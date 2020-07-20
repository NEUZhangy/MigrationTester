import os
import sys

if __name__ == "__main__":
  file_object = open('txttoorder.csv')
  try:
    for line in file_object:
      oneline = line.split()
      folder = oneline[0].split('/')[1]
      s1 = 'cp toolbox/* ' + folder + '/'
      os.system(s1) 
      towrite = '\n'.join(oneline[2:])    
      ss = 'cd ' + folder
      os.system(ss)
      fi = open('cmlist.txt', 'w')
      fi.write(towrite)
      fi.close( )
      os.system('python git-diff.py')
      os.system('cd ..')
      os.system('mkdir allinall/' + folder)

      s3 = 'mv '
      s3 += folder
      s3 += '/backup allinall/' + folder
      os.system(s3)
  finally:
    file_object.close()
