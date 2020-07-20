step1. get the project list with the API library
  python step1_getClientProjects javacodelist.txt commonio.txt lucene org.apache.lucene 

step2. get the project list with a API version change of the library
  python step2_parseVersions.py commonio.txt library groupId > commonio_withversion.txt
  merge and store all output of step2 and prepare to be used later as commonio_lib.txt

---------------

step3. parse commits with version content.
  g++ step3_processfirsttoordered.cpp -o step3_processfirsttoordered
  ./step3_processfirsttoordered < commonio_withversion.txt > commonio_withversion_content.csv


step4. manage each commit folder and reshape them into [from to] form.
  move the sztoolbox into the candidates folder
  python step4_do_exp commonio_withversion_content.csv

step5. 
  chdir to the allinall
  modify the step5.py result record address
  python step5_detect_change_with_lib

step6.
  python step6_make_index.py > commonio_list.csv

step7.
  python step7_manage_package.py commons-io-


folder structure
|- branch_n:
|---step1, step2, step3, sztoolbox/, step4
|---allinall
|-----step5

|- result_folder
|---step6

|- lib_jar_folder
|---step7
