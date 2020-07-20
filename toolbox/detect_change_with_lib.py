import os
import sys


if __name__ == "__main__":
  currentdir = os.getcwd();
  # to_file_name = 'adbg_selected'
  to_file_name = 'large_android'
  for prj in os.listdir(currentdir):
    inside_prj = os.path.join(currentdir, prj)
    print 'prj:' + inside_prj
    if (not os.path.isdir(inside_prj)):
      continue
    for cmt in os.listdir(inside_prj):
      if ('only' in cmt):
        continue
      cmt_do = os.path.join(inside_prj, cmt)
      print cmt_do

      for home, dirs, files in os.walk(cmt_do+'/from'):
        for filename in files:
          if (filename.endswith('.java')):
            print os.path.join(home, filename)
            cs = os.path.join(home, filename)
            tocs = cs.replace('from', 'to', 1)
            if os.path.isfile(tocs):
              # start file checking
              print tocs
              lib_code = {'xsztest':1}
              fh = open(cs)
              for line in fh.readlines():
                line = line.strip()
                if line.startswith('import'):
                  if 'import android' in line or 'com.google.android' in line:
                    one_lib = line.split('.')[len(line.split('.'))-1].strip(';')
                    lib_code[one_lib] = 1
                    print one_lib

              fh = open(tocs)
              for line in fh.readlines():
                line = line.strip()
                if line.startswith('import'):
                  if 'import android' in line or 'com.google.android' in line:
                    one_lib = line.split('.')[len(line.split('.'))-1].strip(';')
                    lib_code[one_lib] = 1
                    print one_lib

              judge_label = 1
              df_msg = os.popen('diff '+ cs + ' ' + tocs)
              print 'recieved:'
              info = df_msg.readlines()
              print info
              for line in info:
                line = line.strip('\r\n')
                if line.startswith('import'):
                  if 'android' in line:
                    one_lib = line.split('.')[len(line.split('.'))-1].strip(';')
                    # lib_code[one_lib] = 1
                else:
                  if 'android' in line:
                    judge_label = 1
                  for lib_item in lib_code.keys():
                    if lib_item in line:
                      judge_label = 1
              if (judge_label == 1):
                if (not os.path.exists('/home/shengzhex/test_lab/'+ to_file_name +'/' + prj +'/' + cmt + '/from')):
                  os.makedirs('/home/shengzhex/test_lab/'+ to_file_name +'/' + prj +'/' + cmt + '/from')
                if (not os.path.exists('/home/shengzhex/test_lab/'+ to_file_name +'/' + prj +'/' + cmt + '/to')):
                  os.makedirs('/home/shengzhex/test_lab/'+ to_file_name +'/' + prj +'/' + cmt + '/to')

                if (not os.path.exists('/home/shengzhex/test_lab/'+ to_file_name +'/' + prj +'/' + cmt + '/from/' + filename)):
                  os.system('cp ' + cs + ' ' + '/home/shengzhex/test_lab/'+ to_file_name +'/' + prj +'/' + cmt + '/from/' + filename)
                if (not os.path.exists('/home/shengzhex/test_lab/'+ to_file_name +'/' + prj +'/' + cmt + '/to/' + filename)):
                  os.system('cp ' + tocs + ' ' + '/home/shengzhex/test_lab/'+ to_file_name +'/' + prj +'/' + cmt + '/to/' + filename)
