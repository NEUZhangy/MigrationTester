# run "python getCommits.py tmpProjectsMined"

from sys import argv 
import git
import os
import re
import unicodedata

script, filename = argv

jpatminus = re.compile("---[A-Za-z\d/\s]+.java")
jpatplus = re.compile("\+\+\+[A-Za-z\d/\s]+.java")
ppatminus = re.compile("---[A-Za-z\d/\s]+pom.xml")
ppatplus = re.compile("\+\+\+[A-Za-z\d/\s]+pom.xml")
#use re.search to find substring
vpatminus = re.compile("-[\s]+<[a-zA-Z\.\d\-]+.version>")
vpatplus = re.compile("\+[\s]+<[A-Za-z\d\.\-]+.version>")
#use re.search to find substring
v2patminus = re.compile("-[\s]+<version>")
v2patplus = re.compile("\+[\s]+<version>")
#use re.search to find artifactId
apat = re.compile("[-\+\s]*<artifactId>[a-zA-Z\.\d\-]+</artifactId>")
knownLib = 'client'

#tmpStr = '3${version}$'
#if tmpStr.endswith('$'):
#    print 'matched'
#else :
#    print 'unmatched'


def getLibs(commits, foundCommits, knownLib):
    readPomChange = False
    artifactId = False

    for hexsha in commits: #for each commit, collect the modified libs
          try :
              output = repo.git.show("%s" %hexsha).split('\n') 	          
          except :
              continue
          old_lib_ver_map = {}
          new_lib_ver_map = {}
          for line2 in output: #indention 10
              line = unicodedata.normalize('NFKD', line2).encode('ascii', 'ignore')
              if line.startswith('---'):
                  readPomChange = False
                  artifactId = False
              if (ppatminus.match(line) or ppatplus.match(line)):
                  readPomChange = True
              elif readPomChange:
                  m = re.search(vpatminus, line) #oldLib
                  if m: # e.g. - <erwe.version>2.3</erwe.version>
                      tag = m.group(0)[1:].strip() #<erwe.version>
                      tag = tag[1:-9]
                      if tag == knownLib:
                          libver = line.split(".version>", 1)[1]
                          libver = libver.split("<")[0]
                          #print libver
                          old_lib_ver_map[tag] = [libver]         #missing $ check                 
                      continue       
                  m = re.search(vpatplus, line) #newLib
                  if m: # e.g. + <erwe.version>2.3</erwe.version>
                      tag = m.group(0)[1:].strip() #<erwe.version>
                      tag = tag[1:-9]
                      if tag == knownLib:
                          libver = line.split(".version>", 1)[1]
                          libver = libver.split("<")[0]
                          #print libver
                          new_lib_ver_map[tag] = [libver]       #missing $ check
                      continue
                  m = re.search(apat, line) # <artifactId>...</artifactId>
                  if m: 
                      lib = line[12:-13]  #<artifactId>...</artifactId>      
                      if lib == knownLib:
                          artifactId = True	
                          oldNumVer = False
                          newNumVer = False              
                      continue
                  if artifactId:
                      m = re.search(v2patminus, line) #- <version>...</version>
                      if m:
                          old_version=line[1:].strip() #<version>...</version>
                          old_version = old_version[9:-10]
                          if not old_version.startswith('$'):
                              #print old_version                              
                              oldNumVer = True
                          continue
                      m = re.search(v2patplus, line) #+ <version>...</version>
                      if m:
                          new_version=line[1:].strip() #<version>...</version>
                          new_version = new_version[9:-10]
                          if not new_version.startswith('$'):
                              newNumVer = True                             
                          if oldNumVer:
                              foundCommits.append(hexsha)
                              break
                          continue   
          if len(old_lib_ver_map) > 0 and len(new_lib_ver_map) > 0:  
              print "old" 
              print old_lib_ver_map
              print "new"
              print new_lib_ver_map
              if (old_lib_ver_map[knownLib] != new_lib_ver_map[knownLib]) :
                  foundCommits.append(hexsha)


with open(filename) as f:
    for line in f:
        commitsWithJavaChange = []
        foundCommits = []
        os.chdir(line.strip())
        g = git.Git('.')
        repo = git.Repo('.')
        hexshas = g.log('--pretty=%H', '--follow', '--', 'pom.xml').split('\n')
        for hexsha in hexshas:	
          containJavaChange = False
          output = repo.git.show("%s" %hexsha).split('\n')
          for line2 in output:          
              line3 = unicodedata.normalize('NFKD', line2).encode('ascii', 'ignore')
              #print line
              if (jpatminus.match(line3) or jpatplus.match(line3)):
                  containJavaChange = True     
                  break
          if containJavaChange:
              commitsWithJavaChange.append(hexsha)
        getLibs(commitsWithJavaChange, foundCommits, 'lucene')
        print "FOLDER: %s" % line
        for c in foundCommits:
            print c 
        os.chdir('..')
