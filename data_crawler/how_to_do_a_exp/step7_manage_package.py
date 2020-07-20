# -*- coding: utf-8 -*-
import os
import shutil
from sys import argv

#遍历指定目录，显示目录下的所有文件夹名
def traversalDir_FirstDir(path):
    lib_name = argv[1]
    print "需要遍历的路径:", path
    if (os.path.exists(path) != 1):
        print "路径不存在"
    else:
        files = os.listdir(path)   #获取指定路径 下一级 所有文件夹 和 文件 的名称
        for file in files:
            strList1 = file.split(".jar")
            # lib_name = "spring-security-core-"
            strList2 = strList1[0].split(lib_name)

            if (strList2[0]):
                continue

            # print strList2[1]

            strList3 = path + "/" + strList2[1]
            # print strList3

            os.makedirs(strList3)

            shutil.copy(path+"/"+file,strList3)

if __name__ == '__main__':
    sourcepath = os.getcwd()
    traversalDir_FirstDir(sourcepath) # 返回 文件夹名字 列表
