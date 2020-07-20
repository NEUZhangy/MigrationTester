#include <cstdio>
#include <iostream>
#include <string>
#include <cstring>
#include <vector>

using namespace std;

int main() {
  string oneline, nows;
  int j;
  vector<string> eachone;
  while (cin>> oneline) {
    if (oneline.find("http") == 0) {
      if (eachone.size()!=0) {
        cout << nows << "," << eachone.size();
        for (int i=0;i<eachone.size();i++) {
          cout<<","<<eachone[i];
        }
        cout<<endl;
      }
      nows = oneline.substr(41);
      nows = nows.substr(0, nows.length()-4);
      eachone.clear();
    }
    if (oneline.length()==40) {
      eachone.push_back(oneline);
    }
  }
  return 0;
}
