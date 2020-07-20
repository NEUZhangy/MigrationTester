#include <cstdio>
#include <cstring>
#include <string>
#include <iostream>
#include <vector>

using namespace std;

vector<std::string> split(const  std::string& s, const std::string& delim)
{
    std::vector<std::string> elems;
    size_t pos = 0;
    size_t len = s.length();
    size_t delim_len = delim.length();
    if (delim_len == 0) return elems;
    while (pos < len)
    {
        int find_pos = s.find(delim, pos);
        if (find_pos < 0)
        {
            elems.push_back(s.substr(pos, len - pos));
            break;
        }
        elems.push_back(s.substr(pos, find_pos - pos));
        pos = find_pos + delim_len;
    }
    return elems;
}

// 0 means a >= b, 1 means a<b
int compare(string a, string b) {
  vector<string> a_s = split(a, ".");
  vector<string> b_s = split(b, ".");
  //cout<<a<<":"<<b<<endl;
  int la = a_s.size();
  int lb = b_s.size();
  int i;
  //cout<<la<<"+"<<lb<<endl;
  for (i = 0; i<la && i<lb; i++) {
    //cout<<"i:"<<i<<endl;
    //cout<<a_s[i]<<"<->"<<b_s[i]<<endl;
    if (a_s[i].compare(b_s[i]) == 0) {
      continue;
    }

    if (stoi(a_s[i]) > stoi(b_s[i])) {
      return 0;
    }
    else return 1;
  }
  if (i<lb) return 1;
  else return 0;
}

string mapp[] = {
  "0.0",
  "1.0", "1.1", "1.5", "1.6", "2.0",
  "2.0.1", "2.1", "2.2", "2.3", "2.3.3",
  "3.0", "3.1", "3.2", "4.0", "4.0.3",
  "4.1", "4.2", "4.3", "4.4", "4.4.9.9.9",
  "5.0", "5.1", "6.0", "7.0", "7.1",
  "8.0", "8.1"
};

int main(int argc, char *argv[]) {
  string str1;
  if (argc == 2) {
    str1 = argv[1];
  }
  else cin >> str1;
  //cout<< str1<<endl;
  int ans = 0;
  for (int i=1;i<=27;i++) {
    if (compare(str1, mapp[i])==0) {
      //cout<<"up:"<<i<<endl;
      ans = i;
    }
  }
  cout<<ans<<endl;
  return 0;
}
