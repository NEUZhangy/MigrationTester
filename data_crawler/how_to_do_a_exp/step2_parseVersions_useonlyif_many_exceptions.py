# executed after getClientProjects.py to parse all versions of a library once used by a project in history
# run with "python parseVersions.py allClientProjects.txt library groupId"

from sys import argv
import git
import os
import re
import unicodedata
import shutil
import subprocess
import json

script, filename, lib, groupId = argv

vpat = re.compile("[\s]+<[a-zA-Z\.\d\-]+.version>")
apat = re.compile("[-\+\s]*<artifactId>[a-zA-Z\.\d\-]+</artifactId>")
v2pat = re.compile("[\s]+<version>")
gpat = re.compile("[\s]*<groupId>[a-zA-Z\.\d\-]+</groupId>")

jpatminus = re.compile("---[A-Za-z\d/\s]+.java")
jpatplus = re.compile("\+\+\+[A-Za-z\d/\s]+.java")
ppatminus = re.compile("---[A-Za-z\d/\s\-]+pom.xml")
ppatplus = re.compile("\+\+\+[A-Za-z\d/\s\-]+pom.xml")
# use re.search to find substring
vpatminus = re.compile("-[\s]+<[a-zA-Z\.\d\-]+.version>")
vpatplus = re.compile("\+[\s]+<[A-Za-z\d\.\-]+.version>")
# use re.search to find substring
v2patminus = re.compile("-[\s\t]*<version>")
v2patplus = re.compile("\+[\s\t]*<version>")

data = {}
dictionary = {}


# if ppatminus.match('--- insight/insight-activemq/pom.xml'):
#    print 'matched'
# else:
#    print 'unmatched'

# if re.search(gpat, '<groupId>org.apache.lucene</groupId>'):
#    print 'match'
# else:
#    print 'unmatched'

def parseMigrateCommits(url):
    readPomChange = False
    artifactId = False
    tmp = ''

    folder = url.rsplit("/", 1)[-1]
    folder = folder.split('.git')[0]
    print(url)
    g = git.Git()
    try:
        g.clone(url)
    except:
        pass

    if os.path.exists(folder):
        os.chdir(folder)
        repo = git.Repo('.')
        hexshas = g.log('--pretty=%H', '--follow', '--', '.').split('\n')
        data[folder] = []
        dictionary[folder] = []
        i = 0
        length = len(hexshas)

        for hexsha in hexshas:
            #        if not '9f581f' in hexsha:
            #            continue
            output = subprocess.check_output(["git", "diff-tree", "--no-commit-id", "--name-only", "-r", hexsha])
            if not 'pom.xml' in output:
                continue
            lines = output.split('\n')
            containsJava = False
            for line in lines:
                line = line.lower()
                if '.java' in line and 'test' not in line:
                    containsJava = True
                    break
            output = repo.git.show("%s" % hexsha).split('\n')
            old_lib_ver = ''
            new_lib_ver = ''
            for line2 in output:
                line = unicodedata.normalize('NFKD', line2).encode('ascii', 'ignore')
                line = line.strip()
                if line.startswith('---'):
                    readPomChange = False
                    flag = False

                if (ppatminus.match(line) or ppatplus.match(line)):
                    readPomChange = True

                elif readPomChange:
                    m = re.search(vpatminus, line)  # oldLib
                    if m:  # e.g. - <erwe.version>2.3</erwe.version>
                        tag = m.group(0)[1:].strip()  # <erwe.version>
                        tag = tag[1:-9]
                        if tag == lib or lib in tag:
                            # print tag
                            if old_lib_ver == '' or old_lib_ver.startswith('$'):
                                libver = line.split("version>", 1)[1]
                                libver = libver.split("<")[0]
                                old_lib_ver = libver
                                # print "old_lib_ver: %s" % old_lib_ver
                        continue
                    m = re.search(vpatplus, line)  # newLib
                    if m:  # e.g. + <erwe.version>2.3</erwe.version>
                        tag = m.group(0)[1:].strip()  # <erwe.version>
                        tag = tag[1:-9]
                        if tag == lib or lib in tag:
                            # print 'lucene_2'
                            if new_lib_ver == '' or new_lib_ver.startswith('$'):
                                libver = line.split("version>", 1)[1]
                                libver = libver.split("<")[0]
                                new_lib_ver = libver
                                # print "new_lib_ver: %s" % new_lib_ver
                        continue
                    m = re.search(apat, line)  # <artifactId>...</artifactId>
                    if m:
                        if not line.startswith('<'):
                            continue
                        tag = line[12:-13]

                        if tag == lib or lib in tag:
                            # print 'lucene_3'
                            flag = True
                            old_lib_ver = ''
                            new_lib_ver = ''
                        continue
                    m = re.search(gpat, line)  # <groupId>...</groupId>
                    if m:
                        if line.startswith('+') or line.startswith('-'):
                            continue
                        tag = line[9:-10]
                        if tag == lib or lib in tag:
                            flag = True
                        else:
                            flag = False
                        continue
                    if flag:
                        m = re.search(v2patminus, line)  # - <version>...</version>
                        if m:
                            if old_lib_ver == '' or old_lib_ver.startswith('$'):
                                old_lib_ver = line[1:].strip()
                                old_lib_ver = old_lib_ver[9:-10]
                                # print "old_lib_ver: %s" %old_lib_ver
                                # print hexsha
                            continue
                        m = re.search(v2patplus, line)  # + <version>...</version>
                        if m:
                            if new_lib_ver == '' or new_lib_ver.startswith('$'):
                                new_lib_ver = line[1:].strip()
                                new_lib_ver = new_lib_ver[9:-10]
                                # print "new_lib_ver: %s" % new_lib_ver
                                # print hexsha
                            continue
            if old_lib_ver == '' or new_lib_ver == '' or old_lib_ver.startswith('$') or new_lib_ver.startswith(
                    '$') or old_lib_ver == new_lib_ver:
                continue

            latestVersion = old_lib_ver
            pair = (old_lib_ver, new_lib_ver)
            pairs = dictionary[folder]
            if pair in pairs:
                continue
            output = subprocess.check_output(["git", "show", hexsha + "^", "--pretty=%H", "--name-only"]).split('\n')
            # print output[0]
            data[folder].append(output[0])
            pairs.append(pair)
            if not containsJava:
                print('Version Marker')
            print(old_lib_ver)
            print(new_lib_ver)
            print(hexsha)
            i += 1
        if len(data[folder]) < 2:
            del data[folder]
            del dictionary[folder]
        os.chdir('..')


with open(filename) as f:
    for line in f:
        parseMigrateCommits(line.strip())

with open('migrateCommits_data.json', 'w') as f:
    json.dump(data, f)

with open('migrateCommits_dictionary.json', 'w') as f:
    json.dump(dictionary, f)
'''
        #hexshas = subprocess.check_output(["git", "log", "--pretty=%H"])    
        hexshas = g.log('--pretty=%H', '--follow', '--', '.').split('\n')
        #hexshas = subprocess.check_output(["git", "log", "--pretty=%H"])        	       
        latestPomVersion = ''
        flag = False
        for hexsha in hexshas:  	                          
            if flag:
                latestPomVersion = hexsha
                try:        
                    g.checkout(hexsha) 
                except:
                    continue
                if os.path.isfile('pom.xml'):            
                    break
                else:
                    flag = False                        
            output = subprocess.check_output(["git", "diff-tree", "--no-commit-id", "--name-only", "-r", hexsha])        
            #if not 'pom.xml' in output:
            #    continue
            if flag == False:
                flag = True
                continue                          
        if not flag:
            return
'''

#    latestVersion = '0'
