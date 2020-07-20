#coding:utf-8

import os

result = list()

with open('adr_lib2.txt','r') as f:
  line = f.readline()
  while line:
    print line,
    if '0' <= line[0] and line[0] <= '9' and line[1] == '.' \
      and '0' <= line[2] and line[2] <= '9':
      x = line[0] + line[1] + line[2]
      i = 3
      lenth = len(line)
      while (i+1< lenth and line[i] == '.' and '0' <= line[i+1] and line[i+1] <= '9'):
        x += line[i] + line[i+1]
        i+=2
      # print "|->true" + x
      command = './convertversiontolevel ' + x
      r = os.popen(command)
      y = r.readlines()[0].strip('\r\n')
      result.append(y)
    else:
      result.append(line.strip('\r\n'))
    line = f.readline()

f.close()                
open('result-adr_lib2.txt', 'w').write('%s' % '\n'.join(result))