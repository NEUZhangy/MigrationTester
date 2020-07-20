import os
import sys


if __name__ == "__main__":
  currentdir = os.getcwd();
  for prj in os.listdir(currentdir):
    inside_prj = os.path.join(currentdir, prj)
    if (not os.path.isdir(inside_prj)):
      continue
    for cmt in os.listdir(inside_prj):
      if ('only' in cmt):
        continue
      cmt_do = os.path.join(inside_prj, cmt)
      print prj + ',' + cmt
