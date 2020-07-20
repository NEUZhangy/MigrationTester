package edu.vt.cs.changes.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ibm.wala.types.FieldReference;
import com.ibm.wala.classLoader.IField;
import partial.code.grapa.dependency.graph.DataFlowAnalysisEngine;

public class SubgraphComparator {

	DataFlowAnalysisEngine e1;
	DataFlowAnalysisEngine e2;
	
	public void compare(Subgraph sg1, Subgraph sg2, GraphConvertor2 c1, GraphConvertor2 c2, 
			DataFlowAnalysisEngine e1, DataFlowAnalysisEngine e2) {	
		this.e1 = e1;
		this.e2 = e2;
		Set<SNode> sources1 = sg1.getMarkedSources();
		Set<SNode> sources2 = sg2.getMarkedSources();
		Set<SNode> dests1 = sg1.getMarkedDests();
		Set<SNode> dests2 = sg2.getMarkedDests();
		
		Map<String, SNode> sMap1 = convertToMap(sources1);
		Map<String, SNode> sMap2 = convertToMap(sources2);					
		Map<String, SNode> addedSources = new HashMap<String, SNode>();
		getSubstraction(sMap2, sMap1, addedSources);
		String key = null;
		SNode n = null;
		if (!addedSources.isEmpty()) {
			System.out.println("Added sources:");
			for (Entry<String, SNode> entry : addedSources.entrySet()) {
				key = entry.getKey();
				n = entry.getValue();
				if (n.getType() == InsnNode.DATA_NODE) {
					if (c1.hasVariable(key)) {
						System.out.println("reuse existing variable");
					}
				} else {
					InsnNode iNode = (InsnNode)n;
					if (iNode.getInstType() == InstType.GET_STATIC) {
						FieldReference fRef = (FieldReference)iNode.getExtraData().get(0);
						IField f = e1.getClassHierarchy().resolveField(fRef);
						if (f == null) {
							System.out.println("added field");
						} else {
							System.out.println("reuse existing field");
						}
					}
				}				
			}		
		}		
				
		Map<String, SNode> deletedSources = new HashMap<String, SNode>();
		getSubstraction(sMap1, sMap2, deletedSources);		
		if (!deletedSources.isEmpty()) {
			System.out.println("Deleted sources:");
			for (Entry<String, SNode> entry : deletedSources.entrySet()) {
				System.out.print(entry.getKey() + "\t");
			}	
		}		
		
		Map<String, SNode> dMap1 = convertToMap(dests1);
		Map<String, SNode> dMap2 = convertToMap(dests2);
		Map<String, SNode> addedDests = new HashMap<String, SNode>();
		getSubstraction(dMap2, dMap1, addedDests);
		if (!addedDests.isEmpty()) {
			System.out.println("Added destinations:");
			for (Entry<String, SNode> entry : addedDests.entrySet()) {
				System.out.print(entry.getKey() + "\t");
			}				
		}				
		Map<String, SNode> deletedDests = new HashMap<String, SNode>();
		getSubstraction(dMap1, dMap2, deletedDests);	
		if (!deletedDests.isEmpty()) {
			System.out.println("Deleted destinations:");
			for (Entry<String, SNode> entry : deletedDests.entrySet()) {
				System.out.print(entry.getKey() + "\t");
			}				
		}
	}
	
	/**
	 * result = a - b
	 * @param a
	 * @param b
	 * @param result
	 */
	private void getSubstraction(Map<String, SNode> a, Map<String, SNode> b, Map<String, SNode> result) {
		String key = null;
		for (Entry<String, SNode> entry : a.entrySet()) {
			key = entry.getKey();
			if (!b.containsKey(key)) {
				result.put(key, entry.getValue());
			}
		}		
	}

	private Map<String, SNode> convertToMap(Set<SNode> sources) {
		Map<String, SNode> map = new HashMap<String, SNode>();
		for (SNode s : sources) {
			map.put(s.label, s);
		}
		return map;
	}
}
