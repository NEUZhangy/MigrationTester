# to get all client projects of a given library, e.g. Lucene
# run with python getClientProjects projectsMined2 clientProjects.txt lucene org.apache.lucene
from sys import argv
import git 
import os
import os.path
import re
import unicodedata
import shutil


script,filename,filename2,lib,groupId=argv

vpat = re.compile("[\s]+<[a-zA-Z\.\d\-]+.version>")
apat = re.compile("[-\+\s]*<artifactId>[a-zA-Z\.\d\-]+</artifactId>")
v2pat = re.compile("[\s]+<version>")
gpat = re.compile("[-\+\s]*<groupId>[a-zA-Z\.\d\-]+</groupId>")
tag = ''
tag2 = ''

def checkpom(lib) :
     tmp = ''
     flag = False
     libnum = ''
     isDollarVersion = False
     with open ('pom.xml') as f2:
         for line2 in f2:
             #line2 = unicodedata.normalize('NFKD', line2).encode('ascii', 'ignore')
             m = re.search(vpat, line2)
             if m: # e.g. <***.version>2.3<***.version>
                 tmp = m.group(0).strip() #<***.version>
                 tmp = tmp[1:-9]
                 if tmp == lib:                     
                     flag = True
                     libnum = line2.strip().split('<')[1]
                     libnum = libnum.split('>')[1]
                     print (libnum)
                     if libnum.startswith('$'):
                         isDollarVersion = True
                     break
             m = re.search(apat, line2)
             if m: # e.g. <artifactId>***<artifactId>
                 tmp = line2.strip()[12:-13]
                 if tmp == lib:
                     flag = True
                 continue
             m = re.search(gpat, line2)
             if m:
                 tmp = line2.strip()[9:-10]                 
                 if tmp == groupId:
                     flag = True
                 continue
             if flag:#<version>...</version>
                 m = re.search(v2pat, line2)
                 if m:
                     libnum = line2.strip().split('<')[1]
                     libnum = libnum.split('>')[1]
                     if libnum.startswith('$'):
                         isDollarVersion = True
                     else:
                         print (libnum)
                     break             
     if flag:
         #print tmp 
         return libnum
     else :
         return ''

target = open(filename2, 'w')
with open(filename) as f:
    for line in f:
        line=line.strip()
        
        url=line+".git"
        group = url.split('://')
        firstPart = group[0]
        #print firstPart
        secondPart = group[1]
        #print secondPart
        url = firstPart + '://NiseGroup:nisevt2015@' + secondPart
        print (url)
        g = git.Git()
        try :
            g.clone(url)
        except:
            pass
        folder=line.rsplit("/", 1)[-1]
        try :
            os.chdir(folder)
        except:
            continue
        if os.path.isfile('pom.xml'):
            tag = checkpom(lib)
            if tag != '':
                hexshas = g.log('--pretty=%H', '--follow', '--', 'pom.xml').split('\n')
                hexsha = hexshas[len(hexshas) - 1]
                print hexsha
                try :
                    g.checkout(hexsha)        
                except:
                    pass
                if not os.path.isfile('pom.xml'):
                    target.write(url)
                    target.write('\n')
                    #break
                else :
                    tag2 = checkpom(lib)                                      
                    if tag2 == '':
                        print ('no lib in initial version')
                        target.write(url)
                        target.write('\n')
                        #break
                    else :
                        if tag.startswith('$') and tag2.startswith('$'):
                            print ('there are only dollar versions')
                            continue                        
                        target.write(url)
                        target.write('\n')
                        #break
        os.chdir('..') 
        shutil.rmtree(folder)  


