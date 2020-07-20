package edu.vt.cs.changes.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;

import com.ibm.wala.cast.java.ipa.modref.AstJavaModRef;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl.ConcreteJavaMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.PartialCallGraph;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAInstruction;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import edu.vt.cs.append.CommonValue;
import edu.vt.cs.changes.ChangeFact;
import edu.vt.cs.changes.api.refchanges.FieldChangeFact;
import edu.vt.cs.diffparser.util.PatternMatcher;
import edu.vt.cs.diffparser.util.SourceCodeRange;
import edu.vt.cs.graph.GraphConvertor;

public class PatternInferer {

	Map<SourceCodeEntity, SourceCodeChange> entityChanges = null;
	CompilationUnit oldCu = null;
	CompilationUnit newCu = null;
	String oldClassName = null;
	String newClassName = null;
	ChangeFact callbackCF = null;
	SDG old_sdg = null;
	SDG new_sdg = null;
	
	public PatternInferer(ChangeFact cf,
			Map<SourceCodeEntity, SourceCodeChange> map, CompilationUnit oldCu, CompilationUnit newCu, 
			String oldClassName, String newClassName, SDG ldg, SDG rdg) {
		this.callbackCF = cf;
		this.entityChanges = map;
		this.oldCu = oldCu;
		this.newCu = newCu;
		this.oldClassName = oldClassName;
		this.newClassName = newClassName;
		//addded by shengzhe
		this.old_sdg = ldg;
		this.new_sdg = rdg;
	}
	
	private List<Set<Integer>> correlateBasedOnDefUse(Map<Integer, Set<String>> defSets,
			List<List<DefUseChangeFact>> factLists, boolean isAdded) {
		List<Set<Integer>> groups = new ArrayList<Set<Integer>>();
		Set<Integer> group = null;
		DefUseChangeFact fact = null;
		DefUseChangeFact fact2 = null;	
		Map<String,Integer> f1_dec_vid = null; // a map from local variable names to their binding IDs
		Map<String,Integer> f2_dec_vid = null;
		Set<Integer> processed = new HashSet<Integer>();
		Map<String, Set<Integer>> fieldAccessMap = new HashMap<String, Set<Integer>>();
		//added by shengzhe July, 2017, commented it by shengzhe Apr 4, 2018
//		Map<String, Set<Integer>> parameterAccessMap = new HashMap<String, Set<Integer>>();
		//end
		for (int i = 0; i < factLists.size(); i++) {
			List<DefUseChangeFact> facts = factLists.get(i);
			if (facts.isEmpty()) {				
				continue;
			}
			fact = facts.get(0);
			if (!fact.fieldChangeFactMap.isEmpty()) {
				for (Entry<String, FieldChangeFact> entry : fact.fieldChangeFactMap.entrySet()) {
					String fieldKey = entry.getKey();
					Set<Integer> group2 = fieldAccessMap.get(fieldKey);
					if (group2 == null) {
						group2 = new HashSet<Integer>();
						fieldAccessMap.put(fieldKey, group2);
					}
					group2.add(i);
				}				
			}
			Set<String> defParams = null;	
			Set<String> useParamsofi = null;
			if (isAdded) {
				if (fact instanceof InsertFact) {
					defParams = new HashSet<String>(((InsertFact)fact).addedDefs.keySet());
					f1_dec_vid = ((InsertFact)fact).stpostoid;
				} else if (fact instanceof UpgradeFact) {
					defParams = new HashSet<String>(((UpgradeFact)fact).addedDefs.keySet());	
					f1_dec_vid = ((UpgradeFact)fact).new_stpostoid;
				} 
				else {
					continue;
				}					
			} else {
				if (fact instanceof DeleteFact) {
					defParams = new HashSet<String>(((DeleteFact)fact).removedDefs.keySet());	
					f1_dec_vid = ((DeleteFact)fact).stpostoid;
				} else if (fact instanceof UpgradeFact) {
					defParams = new HashSet<String>(((UpgradeFact)fact).removedDefs.keySet());	
					f1_dec_vid = ((UpgradeFact)fact).old_stpostoid;
				} 
				else {
					continue;
				}
			}				
			//updated by shengzhe July, 2017
			// to fix: split it to a individual func
			if (fact instanceof DeleteFact) {
				useParamsofi = new HashSet<String>(((DeleteFact)fact).removedUses.keySet());
			} else if (fact instanceof UpgradeFact) {
				useParamsofi = new HashSet<String>(((UpgradeFact)fact).removedUses.keySet());
			}
			if ((defParams == null || defParams.isEmpty()) && useParamsofi == null)
				continue;
			group = new HashSet<Integer>();
			group.add(i);
			Set<String> useParams = null;
			Set<String> correspondingAdded = new HashSet<String>();
			

			for (int j = 0; j < factLists.size(); j++) {
				if (j == i)
					continue;
				List<DefUseChangeFact> facts2 = factLists.get(j);
				if (facts2.isEmpty())
					continue;
				fact2 = facts2.get(0);
				if (isAdded) {
					if (fact2 instanceof InsertFact) {
						useParams = new HashSet<String>(((InsertFact)fact2).addedUses.keySet());
						f2_dec_vid = ((InsertFact)fact2).stpostoid;
					} else if (fact2 instanceof UpgradeFact) {
						useParams = new HashSet<String>(((UpgradeFact)fact2).addedUses.keySet());
						f2_dec_vid = ((UpgradeFact)fact2).new_stpostoid;
					} else{
						continue;
					}
				} else {
					if (fact2 instanceof DeleteFact) {
						useParams = new HashSet<String>(((DeleteFact)fact2).removedUses.keySet());
						f2_dec_vid = ((DeleteFact)fact2).stpostoid;
					} else if (fact2 instanceof UpgradeFact) {
						useParams = new HashSet<String>(((UpgradeFact)fact2).removedUses.keySet());
						f2_dec_vid = ((UpgradeFact)fact2).old_stpostoid;
					} else {
						continue;
					}
				}		
				boolean label = true;
				// to fix: check if you have 2 variables named in the same way but in different scopes.
				// If so, continue with another iteration of the loop, because the 2 variables have no relation with the other.
				for (String x1 : f1_dec_vid.keySet()) {
					for (String x2 : f2_dec_vid.keySet()) {
						if (x1.equals(x2)) {
							if (f1_dec_vid.get(x1)!=f2_dec_vid.get(x2)) {
								label = false;
								break;
							}
						}
					}
					if (label == false) break;
				}
				if (label == false) {
					continue;
				}			
				Set<String> useMove = new HashSet<String>(useParams);
				// useParamsofi is the removed use of fact1, when isAdded is True
				if (useParamsofi != null && isAdded) {
					useMove.retainAll(useParamsofi);
					if (!useMove.isEmpty()) {
						correspondingAdded.addAll(useMove);
						group.add(j);
					}
				}
				// useParams: either the added use or the removed use of fact2.
				// defParams: either the added def or the removed def of fact1.			
				useParams.retainAll(defParams);
				if (!useParams.isEmpty()) {
					correspondingAdded.addAll(useParams);
					group.add(j);
				}
			}
			defSets.put(i, correspondingAdded);
			groups.add(group);
			processed.addAll(group);
		}
		// group facts based on the access to common field
		if (!fieldAccessMap.isEmpty()) {
			for (int i = 0; i < groups.size(); i++) {
				group = groups.get(i);
				for (Entry<String, Set<Integer>> entry : fieldAccessMap.entrySet()) {
					Set<Integer> group2 = new HashSet<Integer>(entry.getValue());
					group2.retainAll(group);
					if (!group2.isEmpty()) {
						group.addAll(entry.getValue());
					}
				}
			}				
		}		
		//added by shengzhe July, 2017
		// group facts based on the access to common field
//		if (!parameterAccessMap.isEmpty()) {
//			for (int i = 0; i < groups.size(); i++) {
//				group = groups.get(i);
//				for (Entry<String, Set<Integer>> entry : parameterAccessMap.entrySet()) {
//					Set<Integer> group2 = new HashSet<Integer>(entry.getValue());
//					group2.retainAll(group);
//					if (!group2.isEmpty()) {
//						group.addAll(entry.getValue());
//					}
//				}
//			}				
//		}		
		//add single-element groups
		for (int i = 0; i < factLists.size(); i++) {
			if (processed.contains(i))
				continue;
			group = new HashSet<Integer>();
			group.add(i);
			groups.add(group);
		}
		return groups;
	}
	
	private List<Set<Integer>> correlateBasedOnControlDep(Map<Integer, Set<String>> defSets,
			List<List<DefUseChangeFact>> factLists, List<SourceCodeChange> allchanges, boolean isAdded) {
		DefUseChangeFact fact = null;
		DefUseChangeFact fact2 = null;
		List<Set<Integer>> groups = new ArrayList<Set<Integer>>();
		Set<Integer> group = null;
				
		for (int i = 0; i < factLists.size(); i++) {
			group = new HashSet<Integer>();
			group.add(i);
			List<DefUseChangeFact> facts = factLists.get(i);
			if (facts.isEmpty()) {				
				continue;
			}
			fact = facts.get(0);
			Map<Integer, List<Integer>> to_control_lines = null;
			Map<Integer, List<Integer>> control_lines = null;
			if (isAdded) {
				if (fact instanceof InsertFact) {
					to_control_lines = ((InsertFact)fact).getControlLtoL(isAdded);
				} else if (fact instanceof UpgradeFact) {
					to_control_lines = ((UpgradeFact)fact).getControlLtoL(isAdded);	
				} else {
					continue;
				}					
			} else {
				if (fact instanceof DeleteFact) {
					to_control_lines = ((DeleteFact)fact).getControlLtoL(isAdded);	
				} else if (fact instanceof UpgradeFact) {
					to_control_lines = ((UpgradeFact)fact).getControlLtoL(isAdded);	
				} else {
					continue;
				}
			}				
			for (int j = 0; j < factLists.size(); j++) {
				if (j == i)
					continue;
				List<DefUseChangeFact> facts2 = factLists.get(j);
				if (facts2.isEmpty())
					continue;
				fact2 = facts2.get(0);
				if (isAdded) {
					if (fact2 instanceof InsertFact) {
						control_lines = ((InsertFact)fact2).getControlLtoL(isAdded);
					} else if (fact2 instanceof UpgradeFact) {
						control_lines = ((UpgradeFact)fact2).getControlLtoL(isAdded);	
					} else {
						continue;
					}					
				} else {
					if (fact2 instanceof DeleteFact) {
						control_lines = ((DeleteFact)fact2).getControlLtoL(isAdded);	
					} else if (fact2 instanceof UpgradeFact) {
						control_lines = ((UpgradeFact)fact2).getControlLtoL(isAdded);	
					} else {
						continue;
					}
				}				
				Set<Integer> top = new HashSet<Integer>();
				for (Integer x : to_control_lines.keySet()) {
					top.addAll(to_control_lines.get(x));					
				}
				top.retainAll(control_lines.keySet());
				if (!top.isEmpty()) {
					group.add(j);
				}				
			}
			groups.add(group);
		}
		return groups;
	}
	
	private List<Set<Integer>> correlateBasedOnControlDep1(Map<Integer, Set<String>> defSets,
			List<List<DefUseChangeFact>> factLists, List<SourceCodeChange> allchanges, boolean isAdded) {
	   
	   List<SourceCodeChange> inter_changes = allchanges;
	   List<Set<String>> def_paras = new ArrayList<Set<String>>();
	   List<Set<String>> use_paras = new ArrayList<Set<String>>();
	   int ori_label[] = new int[100];
	   for (int i=0;i<inter_changes.size();i++) {
		   ori_label[i] = i;
	   }
	   for(int i=0;i<inter_changes.size() -1;i++){  
	       for(int j=0;j<inter_changes.size()-1-i;j++){  
	    	   	boolean judge = false;
//	    	   	if (inter_changes.get(j) instanceof UpgradeFact) {
//	    	   		if (isAdded) 
//	    	   			judge = (UpgradeFact)factLists.get(j).get(0).getgetStartPosition()
//	    		        		>inter_changes.get(j+1).getNewEntity().getStartPosition()
//	    	   		else 
//	    	   			
//	    	   				
//	    	   	}
		        if(inter_changes.get(j).getChangedEntity().getStartPosition()
		        		>inter_changes.get(j+1).getChangedEntity().getStartPosition()){  
		        	Collections.swap(inter_changes, j, j+1);
		        	Collections.swap(factLists, j, j+1);
		        	int mid = ori_label[j]; ori_label[j] = ori_label[j+1]; ori_label[j+1] = mid;
		        }  
	        }  
	    }  
		
		List<Set<Integer>> groups = new ArrayList<Set<Integer>>();
		Set<Integer> group = null;
		DefUseChangeFact fact = null;
		DefUseChangeFact fact2 = null;		
		Set<Integer> processed = new HashSet<Integer>();
		Map<String, Set<Integer>> fieldAccessMap = new HashMap<String, Set<Integer>>();
		
//		CallGraph pcg = PartialCallGraph.make(CG, new LinkedHashSet<CGNode>(partialRoots));
//		SDG sdg = new SDG(pcg, pa, new AstJavaModRef(), DataDependenceOptions.FULL, ControlDependenceOptions.FULL);
//		g.reConstruct(DataDependenceOptions.NONE, ControlDependenceOptions.FULL);
		
		//Aug Week 1, 2017
		
		
		
		
		this.old_sdg.reConstruct(DataDependenceOptions.NONE, ControlDependenceOptions.FULL);
		this.new_sdg.reConstruct(DataDependenceOptions.NONE, ControlDependenceOptions.FULL);
		
		SDG g = this.old_sdg;
		Iterator<Statement> it = g.iterator();
		int level = 1;
//		System.out.println("---------------------");
		SSAInstruction inst = null;
		int index = -1;
//		g.getPDG();
		
		while(it.hasNext()){
			Statement s1 = it.next();
//			JavaSourceLoaderImpl.ConcreteJavaMethod cjm = (JavaSourceLoaderImpl.ConcreteJavaMethod)it.getMethod();
			if (s1 instanceof StatementWithInstructionIndex) {
				StatementWithInstructionIndex s = (StatementWithInstructionIndex)s1;				
				index = s.getInstructionIndex();
				inst = s.getInstruction();				
			}
			else {
				continue;
			}
//			System.out.println("");
			if (s1.getNode().getMethod() instanceof ConcreteJavaMethod) {
				ConcreteJavaMethod method = (ConcreteJavaMethod)s1.getNode().getMethod();
				System.out.println(method);
			}
			Iterator<Statement> nodes = g.getSuccNodes(s1);
//			System.out.println("level 1:" + s1.toString());
			while(nodes.hasNext()){
				Statement s2 = nodes.next();
//				System.out.println("level 2:" + s2.toString());
			}
		}
		System.out.println();
		
		
		for (int i = 0; i < factLists.size(); i++) {
			List<DefUseChangeFact> facts = factLists.get(i);
			if (facts.isEmpty()) {				
				continue;
			}
			fact = facts.get(0);
			Set<String> defParams = null;
			Set<String> useParams = null;
			Set<String> useParamsofi = null;
			if (isAdded) {
				if (fact instanceof InsertFact) {
					defParams = new HashSet<String>(((InsertFact)fact).addedDefs.keySet());
					def_paras.add(defParams);
					useParams = new HashSet<String>(((InsertFact)fact).addedUses.keySet());
					use_paras.add(useParams);
				} else if (fact instanceof UpgradeFact) {
					defParams = new HashSet<String>(((UpgradeFact)fact).addedDefs.keySet());	
					def_paras.add(defParams);
					useParams = new HashSet<String>(((UpgradeFact)fact).addedUses.keySet());
					use_paras.add(useParams);
				} else {
					continue;
				}					
			} else {
				if (fact instanceof DeleteFact) {
					defParams = new HashSet<String>(((DeleteFact)fact).removedDefs.keySet());	
					def_paras.add(defParams);
					useParams = new HashSet<String>(((DeleteFact)fact).removedUses.keySet());
					use_paras.add(useParams);
				} else if (fact instanceof UpgradeFact) {
					defParams = new HashSet<String>(((UpgradeFact)fact).removedDefs.keySet());	
					def_paras.add(defParams);
					useParams = new HashSet<String>(((UpgradeFact)fact).removedUses.keySet());
					use_paras.add(useParams);
				} else {
					continue;
				}
			}				
		}
//		
//		String left_top_bound, left_bot_bound, right_top_bound="", right_bot_bound="";
//		boolean input_lib_label = false;
//		int[] ingroup = new int[100];
//		for (int i = 0; i < inter_changes.size(); i++)
//			ingroup[i] = 0;
//		for (int pos = 0; pos < inter_changes.size(); pos++) {
//			SourceCodeChange x = inter_changes.get(pos);
//			if (x.getChangedEntity().getType() == JavaEntityType.IF_STATEMENT) {
//				Set<String> focus = use_paras.get(pos); 
//				ingroup[pos] = 1;
////				if (isLib(focus)) {
////					input_lib_label = true;
////				}
//				for (int j = pos - 1; j >= 0; j--) {
//					if (def_paras.get(j).contains(focus)) {
//						focus = use_paras.get(j);
//						ingroup[j] = 1;
////						if (isLib(focus)) {
////							input_lib_label = true;
////						}
//					}
//				}
////				left_top_bound = focus.toString();
////				focus = def_paras.get(pos);
////				for (int j = pos + 1; j < inter_changes.size(); j++) {
////					if (use_paras.get(j).contains(focus)) {
////						ingroup[j] = 1;
////						focus = def_paras.get(j);
////					}
////				}
////				left_bot_bound = focus;
//				System.out.println();
//				// ----
////				if (left_top_bound.equals(right_top_bound)
////						&& left_bot_bound.equals(right_bot_bound)) {
////					// groups.add(ingroup);
////				}
//			}
////			pos++;
//		}

		//add single-element groups
		for (int i = 0; i < factLists.size(); i++) {
			if (processed.contains(i))
				continue;
			group = new HashSet<Integer>();
			group.add(i);
			groups.add(group);
		}
		return groups;
	}
	
	public List<DefUseChangeFactGroup> infer(List<List<DefUseChangeFact>> factLists, List<SourceCodeChange> changes, 
			Map<String, String> renamedVars) {		
		Map<Integer, Set<String>> defSets = new HashMap<Integer, Set<String>>();	
		System.out.print("");
		List<Set<Integer>> groups = correlateBasedOnDefUse(defSets, factLists, true);
		List<Set<Integer>> groups2 = correlateBasedOnDefUse(defSets, factLists, false);
		List<Set<Integer>> groups3 = correlateBasedOnControlDep(defSets, factLists, changes, false);
		List<Set<Integer>> groups4 = correlateBasedOnControlDep(defSets, factLists, changes, true);
		groups.addAll(groups2);
//		groups.addAll(groups3);
		groups3.addAll(groups4);
		boolean lv3map[] = new boolean[1000];
		for (Integer inx = 0; inx<changes.size(); inx++) {
			lv3map[inx] = false;
		}
		for (Set<Integer> in : groups3) {
			if (in.size()>1) {
				for (Integer inx : in) {
					lv3map[inx] = true;
				}
			}
		}
		groups.addAll(groups3);
		List<DefUseChangeFactGroup> groupResults = merge(groups, factLists, changes, renamedVars, lv3map);		
		return groupResults;
	}
	
	private void extend(List<DefUseChangeFactGroup> groups) { 
		
	}
	
	private List<DefUseChangeFactGroup> merge(List<Set<Integer>> groups, List<List<DefUseChangeFact>> factLists, 
			List<SourceCodeChange> changes, Map<String, String> renamedVars, boolean[] lv3map) {
		boolean isChanged = true;
		// commented it by shengzhe Apr 4, 2018
		int relationship[][] = new int[5000][5000];
		CommonValue.dependencymap = relationship;
		CommonValue.dep_output = new ArrayList<String>();
		List<DefUseChangeFactGroup> groupResults = new ArrayList<DefUseChangeFactGroup>();
		do {
				isChanged = false;
				Set<Integer> groupsToRemove = new HashSet<Integer>();
				for (int i = 0; i < groups.size() - 1; i++) {
					Set<Integer> group1 = groups.get(i);				
					for (int j = i + 1; j < groups.size(); j++) {
						List<Integer> group2 = new ArrayList<Integer>(groups.get(j));
					    group2.retainAll(group1);
					    if (!group2.isEmpty()) {				    	
					    	group1.addAll(groups.get(j));					    	  	
					    	isChanged = true;				  
					    	groupsToRemove.add(j);
					    }
					}
					if(isChanged) {					
						break;
					}					
				}
				if (isChanged) {
					int actualIndex = 0;
					for (int i = 0; i < groups.size(); i++) {
						if (groupsToRemove.contains(i)) {
							continue;
						} else {
							if (i == actualIndex) {
								//do nothing
							} else {
								groups.set(actualIndex, groups.get(i));														
							}
							actualIndex++;
						}
	 				}
					if(actualIndex < groups.size()) {
						for (int i = groups.size() -1; i>=actualIndex; i--) {
							groups.remove(i);							
						}
					}
				}
			}while (isChanged);
			List<Integer> pureInsertGroups = new ArrayList<Integer>();
			List<Integer> pureDeleteGroups = new ArrayList<Integer>();
			Set<Integer> g = null;
			System.out.print("");
			for (int i = 0; i < groups.size(); i++) {
				g = groups.get(i);
				boolean isPureInsert = true;
				boolean isPureDelete = true;
				for (Integer idx : g) {
					List<DefUseChangeFact> facts = factLists.get(idx);
					if (facts.isEmpty())
						break;
					DefUseChangeFact f = facts.get(0);
					if (f instanceof DeleteFact) {
						isPureInsert = false;
					} else if (f instanceof InsertFact) {
						isPureDelete = false;
					} else if (f instanceof UpgradeFact) {
						isPureInsert = false;
						isPureDelete = false;
					}
				}
				if (isPureInsert && !isPureDelete) {
					pureInsertGroups.add(i);
				} else if (isPureDelete && !isPureInsert) {
					pureDeleteGroups.add(i);
				}				
			}
			
			// this step works when there is 1 purely inserted group and 1 purely deleted group
			// to merge them together
			if (pureInsertGroups.size() == 1 && pureDeleteGroups.size() == 1) {
				int g1 = pureInsertGroups.get(0);
				int g2 = pureDeleteGroups.get(0);
				Set<Integer> ng = new HashSet<Integer>();
				// checking g1 > g2 because when remove elements from a list 
				// the larger indexed element should be removed before the smaller indexed one				
				if (g1 > g2) {
					ng.addAll(groups.remove(g1));
					ng.addAll(groups.remove(g2));
				} else {
					ng.addAll(groups.remove(g2));
					ng.addAll(groups.remove(g1));
				}
				groups.add(ng);
			}			
			
			for (int i=0;i<changes.size();i++)
				for (int j=0;j<changes.size();j++) {
					if (i==j) continue;
					if (changes.get(i).getChangeType() == ChangeType.STATEMENT_DELETE
							&& changes.get(j).getChangeType() == ChangeType.STATEMENT_INSERT) {
						DeleteFact x = (DeleteFact)factLists.get(i).get(0);
						InsertFact y = (InsertFact)factLists.get(j).get(0);
						Map<String, IBinding> xx = x.removedVarBindings;
						Map<String, IBinding> yy = y.addedVarBindings;
						for (String kx : xx.keySet())
							for (String ky : yy.keySet()) {
								if (kx.equals(ky)) {
									relationship[i][j]=1;
									String rel = i+":"+j;
									CommonValue.dep_output.add(rel);
								}
							}
					}
				}

			DefUseChangeFactGroup duGroup = null;
			CommonValue.de_add_relation = new int[5000][5000];
			for (int i = 0; i < groups.size(); i++) {
				Set<Integer> group = groups.get(i);		
				Iterator it = group.iterator();
				if (CurrentResolver.isinsertversion()) {
					List<Integer> another= new ArrayList<Integer>();
					while (it.hasNext()) {					
						int j = (int) it.next();
						for (int tok = 0; tok<4999; tok++) {
							if (relationship[j][tok]==1 || relationship[tok][j] == 1) {
								another.add(tok);
								CommonValue.de_add_relation[j][tok] = relationship[j][tok];
								CommonValue.de_add_relation[tok][j] = relationship[tok][j];
								System.out.println("Special relationship: " + j + "<->" + tok);
							}
						}
					}
//					 two-side relationship
					for (int p=0;p<another.size();p++)
						group.add(another.get(p));
					another.clear();
				}
				duGroup = new DefUseChangeFactGroup(callbackCF, group, factLists, changes, renamedVars, 
						entityChanges, oldCu, newCu, oldClassName, newClassName, lv3map);
				if (duGroup.concreteToUnified != null)
					groupResults.add(duGroup);
			}
		 return groupResults;
	}
}
