package applytool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class patternSet {
	List<String> pattern_o;
	List<String> pattern_n;
	String pattern_dep = null;
	
	// map from ith |pattern in pattern_o| to several |patterns in pattern_new|
	// showing that the ith pattern should be changed to a list of pattern
	Map<Integer, List<Integer>> map_relationship;

	Map<String, String> realToabs;
	Map<String, String> absToreal;
	
	public patternSet() {
		pattern_o = new ArrayList<>();
		pattern_n = new ArrayList<>();
		map_relationship = new HashMap<>();
		absToreal = new HashMap<>();
	}
	
//	public patternSet(ArrayList<String> old_p, ArrayList<String> new_p, Map<Integer, List<Integer>> x) {
//		pattern_o = new ArrayList<>();
//		pattern_n = new ArrayList<>();
//		map_relationship = new HashMap<>();
//		
//		// copy old_pattern to this instance
//		for(int i=0;i<old_p.size();i++)
//		{
//			pattern_o.add(old_p.get(i));
//		}
//		// copy old_pattern to this instance
//		for(int i=0;i<new_p.size();i++)
//		{
//			pattern_n.add(new_p.get(i));
//		}
//		
//		map_relationship = x;		
//	}
	
	public void reset() {
		this.map_relationship = new HashMap<>();
	}
	
	public void addpattern(List<String> oldp, List<String> newp) {
		this.pattern_o = oldp;
		this.pattern_n = newp;		
	}
	
	public void adddep(String dep) {
		this.pattern_dep = dep;
	}
	
	public String checkwithabs(String father, String son) {
		int son_i = 0, rst_start_i = 0;
		String rst_set = null;
		String subfather = new String(father);
		String subson = new String(son);
		List<String> abs_list = new ArrayList();
		while (subson.indexOf("[-") != -1 && subson.indexOf("-]") != -1) {
			int ab_b = subson.indexOf("[-"), ab_e = subson.indexOf("-]");
			String x = subson.substring(0, ab_b);
			abs_list.add(subson.substring(0, ab_b));
			x = subson.substring(ab_b, ab_e+2);			
			abs_list.add(subson.substring(ab_b, ab_e+2));
			subson = subson.substring(ab_e+2);
		}
		if (!subson.isEmpty())
			abs_list.add(subson);
			
		for (int start_i=0;start_i<subfather.length();start_i++) {
			String sub_seg = subfather.substring(start_i).trim();
			int fail_label = 0;
			for (int i=0;i<abs_list.size();i++) {
				String p_i = abs_list.get(i).trim();
				// is a abs segment
				if (p_i.startsWith("[-")) {
//					if it's the last pattern segment
					if (i+1 == abs_list.size()) {
//						it is an abs var
						if (sub_seg.indexOf(" ") != -1
								|| sub_seg.indexOf(")") != -1) {
							absToreal.put(p_i, sub_seg);
							sub_seg = "";
						}
						else {
							fail_label = 1;
							break;
						}
					}
//					if there is another pattern segment
					{
						int next_pos = sub_seg.indexOf(abs_list.get(i+1).trim());
						if (next_pos < 0) {
							fail_label = 1;
							break;
						}
						String y = sub_seg.substring(0, next_pos);
						absToreal.put(p_i, sub_seg.substring(0, next_pos));
						sub_seg = sub_seg.substring(next_pos);
					}
				}
//				is a normal segment
				else {
					if (sub_seg.startsWith(p_i)) {
						sub_seg = sub_seg.substring(p_i.length());						
					}
					else {
						fail_label = 1;
						break;
					}
				}
			}	
			if (fail_label == 0) {
				rst_set = sub_seg;
				break;
			}		
		}

		return rst_set;
	}
	
	// check if the pattern can be found in order in this code snippet
	public boolean checkWith(String oldc) {
		String inter_p = oldc;		
		for (int i=0;i<pattern_o.size(); i++) {
			String one_sentence = pattern_o.get(i);
			inter_p = checkwithabs(inter_p, one_sentence);
			if (inter_p == null) return false;
//			int next_pos = inter_p.indexOf(one_sentence);			
//			inter_p = inter_p.substring(next_pos + one_sentence.length());
		}
		return true;
	}
	
	public String replacebyrealofabs(String inputs, boolean apply) {
		String outputs = new String(inputs);
		for (String onekey : absToreal.keySet()) {
			outputs = outputs.replace(onekey, absToreal.get(onekey).trim());
		}
		if (apply) {
			outputs = outputs.replace("[-", "");
			outputs = outputs.replace("-]", "");
		}
		return outputs;
	}
	
//	public int countSpaceEnter
	
	// do the replacement in order
	// todo: deal with cross-pattern situation
	public String applyTo(String oldc) {
		String rst = new String();
		String inter_p = oldc;		
		for (int i=0;i<pattern_o.size(); i++) {
			String one_pattern_sentence = pattern_o.get(i);
			String one_real_sentence = replacebyrealofabs(one_pattern_sentence, false);
			int next_pos = inter_p.indexOf(one_real_sentence);
			
//			String str2 = inter_p.replaceAll(" ", "").replaceAll("\n", "");
//			String str_real = one_real_sentence.replaceAll(" ", "");
//			int next_pos = str2.indexOf(str_real);
//			inter_p = checkwithabs(inter_p, one_sentence);
			if (next_pos > 0) {
				rst += inter_p.substring(0, next_pos);
			}
			String new_pattern_sentence = pattern_n.get(i);
			String new_real_sentence = replacebyrealofabs(new_pattern_sentence, true);
			rst += new_real_sentence + '\n';	
			
			
			inter_p = inter_p.substring(next_pos + one_real_sentence.length());
		}
		
		for (int i=pattern_o.size();i<pattern_n.size();i++) {
			String new_pattern_sentence = pattern_n.get(i);
			String new_real_sentence = replacebyrealofabs(new_pattern_sentence, true);
			rst += new_real_sentence + "\r\n";
		}
		
		if (!inter_p.isEmpty())
			rst += inter_p;
		return rst;
	}
}