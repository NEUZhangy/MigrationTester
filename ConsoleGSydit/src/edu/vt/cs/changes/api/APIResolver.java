package edu.vt.cs.changes.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.FieldReference;

import edu.vt.cs.append.FineChangesInMethod;
import edu.vt.cs.changes.ChangeFact;
import edu.vt.cs.changes.api.refchanges.ClassChangeFact;
import edu.vt.cs.changes.api.refchanges.SubChangeFact.CHANGE_TYPE;
import edu.vt.cs.diffparser.util.SourceCodeRange;
import edu.vt.cs.graph.SourceMapper;
import partial.code.grapa.dependency.graph.DataFlowAnalysisEngine;
import partial.code.grapa.mapping.ClientMethod;

public class APIResolver {

	public SourceMapper oldMapper = null;
	public SourceMapper newMapper = null;
	
	private Map<ClientMethod, SDG> oldSDGs = null;
	private Map<ClientMethod, SDG> newSDGs = null;	
	
	protected DataFlowAnalysisEngine leftEngine;
	protected DataFlowAnalysisEngine rightEngine;
	protected Map<IMethodBinding, Set<ChangeFact>> missingMethods; //missing old bindings for methods
	protected Map<IVariableBinding, Set<ChangeFact>> missingFields;
	protected Map<String, Set<ChangeFact>> missingTypes;
	protected Map<String, ITypeBinding> typeMap;
//	protected Map<ChangeFact, Set<IBinding>> changeToBindings = null; commented by shengzhe Apr-4, 2018
	public CompilationUnit oldCu;
	public CompilationUnit newCu; 
	public GraphConvertor2 oc;
	public GraphConvertor2 nc;
	
	
	public APIResolver(DataFlowAnalysisEngine lEngine, DataFlowAnalysisEngine rEngine, 
			Map<IMethodBinding, Set<ChangeFact>> missingMethods, Map<IVariableBinding, Set<ChangeFact>> missingFields,
			Map<String, Set<ChangeFact>> missingTypes, Map<String, ITypeBinding> typeMap, Map<IVariableBinding, Set<ChangeFact>> added_lib_version) {
		leftEngine = lEngine;
		rightEngine = rEngine;
		this.oldSDGs = new HashMap<ClientMethod, SDG>();
		this.newSDGs = new HashMap<ClientMethod, SDG>();
		
		oldMapper = new SourceMapper();
		newMapper = new SourceMapper();
		
		this.missingFields = missingFields;
		this.missingMethods = missingMethods;
		this.missingTypes = missingTypes;
		

		// commented by shengzhe Apr-4,
//		changeToBindings = new HashMap<ChangeFact, Set<IBinding>>();
//		IBinding binding = null;
//		Set<IBinding> bSet = null;
//		for (Entry<IMethodBinding, Set<ChangeFact>> entry : missingMethods.entrySet()) {
////			if (x==1) {
////				break;
////			}
//			binding = entry.getKey();
//			Set<ChangeFact> facts = entry.getValue();
//			for (ChangeFact cf : facts) {
//				bSet = changeToBindings.get(cf);
//				if (bSet == null) {
//					bSet = new HashSet<IBinding>();
//					changeToBindings.put(cf, bSet);
//				}
//				bSet.add(binding);
//			}
//		}
		MethodAPIResolver mResolver = new MethodAPIResolver(this);
		mResolver.resolve(missingMethods, added_lib_version);	
		System.out.println(missingFields);
		System.out.println("class:" + missingFields.getClass());
		FieldAPIResolver fResolver = new FieldAPIResolver(this);
		fResolver.resolve(missingFields);
		TypeAPIResolver tResolver = new TypeAPIResolver(this);
		tResolver.resolve(missingTypes, typeMap);
	}
	
	// added by shengzhe, 4/5 2018 for checking the input output semantics
	public int resolve_one(DefUseChangeFactGroup target_group, Set<SourceCodeRange> ranges, FineChangesInMethod allChanges) {
		DefUseChangeFactGroup group = null;
		for (SourceCodeRange r : ranges) {			   
			   int idx = allChanges.getIndexOfChangeForOldRange(r.converToSourceRange());
			   if (ChangeFact.checkGroupWithChange(idx, target_group) == true) {
				   group = target_group;
			   }
			   if (CurrentResolver.isinsertversion()) {
				   group = target_group;
			   }
			   if (group == null) {
				   System.out.println("Not an API Proper Group");
				   continue;
			   }				  
			   Set<String> inputs = new HashSet<String>();
			   Set<String> outputs = new HashSet<String>();
			   getInputOutput(group.oldRanges, group.oldOrderedFacts, group.concreteToUnified, oldCu, oc, inputs, outputs);		
			   Set<String> inputs2 = new HashSet<String>();
			   Set<String> outputs2 = new HashSet<String>();
//			   System.out.print("");
			   getInputOutput(group.newRanges, group.newOrderedFacts, group.concreteToUnified, newCu, nc, inputs2, outputs2);
//			   System.out.println("Input1:"+ inputs.toString() +" and Output1:" + outputs.toString());
//			   System.out.println("Input2:"+ inputs2.toString() +" and Output2:" + outputs2.toString());
			   if (inputs.containsAll(inputs2) && inputs2.containsAll(inputs) && outputs.containsAll(outputs2) && outputs2.containsAll(outputs)) {
				   System.out.println("1API replacements");		
				   return 1;
			   } else if (inputs2.isEmpty() && outputs2.isEmpty()){
				   System.out.println("API removal");
				   return 2;
			   } else {		
				   removeRenamedVars(inputs, inputs2, outputs, outputs2, group);				  
				   if (group.changedClasses != null && !group.changedClasses.isEmpty()) {
					   removeVarsInOtherClasses(inputs, inputs2, outputs, outputs2, group);					  
				   }
				   if (inputs.containsAll(inputs2) && inputs2.containsAll(inputs) && outputs.containsAll(outputs2) && outputs2.containsAll(outputs)) {
					   System.out.println("2API replacements");
					   return 3;
				   } else {					  
					   System.out.println("The updated API is not semantically equivalent");						   
				   }							   
			   }
		   }	
		return 0;
	}
	
	public void resolve(List<DefUseChangeFactGroup> groups, Set<SourceCodeRange> ranges, FineChangesInMethod allChanges) {
		DefUseChangeFactGroup group = null;
		for (SourceCodeRange r : ranges) {			   
			   int idx = allChanges.getIndexOfChangeForOldRange(r.converToSourceRange());
			   group = ChangeFact.getGroupWithChange(idx, groups);
			   if (group == null) {
				   System.out.println("No proper Group");
				   continue;
			   }				  
			   Set<String> inputs = new HashSet<String>();
			   Set<String> outputs = new HashSet<String>();
			   getInputOutput(group.oldRanges, group.oldOrderedFacts, group.concreteToUnified, oldCu, oc, inputs, outputs);		
			   Set<String> inputs2 = new HashSet<String>();
			   Set<String> outputs2 = new HashSet<String>();
			   System.out.print("");
			   getInputOutput(group.newRanges, group.newOrderedFacts, group.concreteToUnified, newCu, nc, inputs2, outputs2);
			   if (inputs.containsAll(inputs2) && inputs2.containsAll(inputs) && outputs.containsAll(outputs2) && outputs2.containsAll(outputs)) {
				   System.out.println("1API replacements");								   
			   } else if (inputs2.isEmpty() && outputs2.isEmpty()){
				   System.out.println("API removal");
			   } else {		
				   removeRenamedVars(inputs, inputs2, outputs, outputs2, group);	
				   if (!group.changedClasses.isEmpty()) {
					   removeVarsInOtherClasses(inputs, inputs2, outputs, outputs2, group);					  
				   }
				   if (inputs.containsAll(inputs2) && inputs2.containsAll(inputs) && outputs.containsAll(outputs2) && outputs2.containsAll(outputs)) {
					   System.out.println("2API replacements");
				   } else {					  
					   System.out.println("The updated API is not semantically equivalent");					  
				   }							   
			   }
		   }		
	}
	
	public SDG findOrCreateNewSDG(ClientMethod m) {
		SDG g = newSDGs.get(m);
		if (g == null) {
			g = rightEngine.buildSystemDependencyGraph2(m);
			newSDGs.put(m, g);
		} else {
			rightEngine.getCFGBuilder(m);
		}
		return g;
	}
	
	public SDG findOrCreateOldSDG(ClientMethod m) {
		SDG g = oldSDGs.get(m);
		if (g == null) {
			g = leftEngine.buildSystemDependencyGraph2(m);
			oldSDGs.put(m, g);
		} else {
			leftEngine.getCFGBuilder(m);
		}
		return g;
	}
	
	private List<ISSABasicBlock> getBlocks(Set<Integer> indexes, GraphConvertor2 c) {
		IR ir = c.ir;
		SSAInstruction[] insts = ir.getInstructions();
		ISSABasicBlock bb = null;		
		Set<ISSABasicBlock> result = new HashSet<ISSABasicBlock>();
		for (Integer i : indexes) {
			bb = ir.getBasicBlockForInstruction(insts[i]);
			if (bb == null) {
				continue;
			}
			result.add(bb);
 		} 			
		return new ArrayList<ISSABasicBlock>(result);
	}
	
	public void getInputOutput(Map<SourceCodeRange, Integer> ranges, List<Integer> ordered,
			Map<String, String> concreteToUnified,
			CompilationUnit cu, GraphConvertor2 c, 
			Set<String> inputs, Set<String> outputs) {		
		   Map<Integer, SourceCodeRange> rangeMap = new HashMap<Integer, SourceCodeRange>();
		   for (Entry<SourceCodeRange, Integer> entry2 : ranges.entrySet()) {
			   rangeMap.put(entry2.getValue(), entry2.getKey());
		   }						   		  		 
		   Set<String> intermediate = new HashSet<String>();
		   SourceCodeRange scr = null;
		   for (Integer factIndex : ordered) {
			   scr = rangeMap.get(factIndex);
//			   System.out.print("");
			   Subgraph s1 = getSubgraph(cu, scr, c);			
			   Set<SNode> sources = s1.getMarkedSources();
			   Set<SNode> dests = s1.getMarkedDests();
			   for (SNode s : sources) {
				   if (s instanceof DataNode) {
					   DataNode dNode = (DataNode)s;
					   String unified = concreteToUnified.get(dNode.label);
					   if (unified == null) {
						   boolean isFound = false;
						   if (dNode.fRef != null) {
							   String fieldName = dNode.label;
							   String className = dNode.fRef.getDeclaringClass().getName().getClassName().toString();
							   Set<String> names = FieldAPIResolver.getPossibleFieldNames(fieldName, className);
							   for(String name : names) {
								   if (concreteToUnified.containsKey(name)) {
									   unified = concreteToUnified.get(name);
									   isFound = true;
									   break;
								   }								   
							   }							   
						   }
						   if (!isFound)
							   continue;
					   }						  
					   if (outputs.contains(unified)) {
						   outputs.remove(unified);
						   intermediate.add(unified);
					   } else if (!intermediate.contains(unified)){
						   inputs.add(unified);
					   }									  
				   } 
			   }
			   for (SNode dest : dests) {
				   if (dest instanceof DataNode) {
					   DataNode dNode = (DataNode)dest;
					   String unified = concreteToUnified.get(dNode.label);
					   if (unified == null) {
						   continue;
					   }
					   if (inputs.contains(unified)) {
						   inputs.remove(unified);
						   intermediate.add(unified);
					   } else if (!intermediate.contains(unified)){
						   outputs.add(unified);
					   }
				   }
			   }
		   }
	}
	
	private Subgraph getSubgraph(CompilationUnit cu, SourceCodeRange r, GraphConvertor2 c) {
		//1. convert source code range to line range
		LineRange lineRange = LineRange.get(cu, r);
		//2. convert line range to instructions
		Set<Integer> indexes = new HashSet<Integer>();
		for (int i = lineRange.startLine; i <= lineRange.endLine; i++) {
			indexes.addAll(c.getInstIndexes(i));	   
		}				
		//3. get all blocks enclosing the instructions
		List<ISSABasicBlock> blocks = getBlocks(indexes, c);
		//4. create subgraph with the blocks
//		System.out.println(blocks);
		Subgraph s = c.getSubgraph(blocks);
//		System.out.println(indexes);
		s.mark(indexes);
		return s;
	}
	
	public void removeRenamedVars(Set<String> inputs, Set<String> inputs2, Set<String> outputs, Set<String> outputs2, DefUseChangeFactGroup group) {
		Map<String, String> cTou = new HashMap<String, String>();
		   Map<String, String> cTou2 = new HashMap<String, String>();
		   String key = null;
		   String key2 = null;
		   String uValue = null;
		   String uValue2 = null;
		   for (Entry<String, String> entry2 : group.renamedVars.entrySet()) {
			   key = entry2.getKey();
			   key2 = entry2.getValue();
			   uValue = group.concreteToUnified.get(key);
			   uValue2 = group.concreteToUnified.get(key2);
			   if (uValue != null) {
				   cTou.put(key, uValue);
			   }
			   if (uValue2 != null) {
				   cTou2.put(key2, uValue2);
			   }								   
		   }							   
		   inputs.removeAll(cTou.values());
		   inputs2.removeAll(cTou2.values());
		   outputs.removeAll(cTou.values());
		   outputs2.removeAll(cTou2.values());	
	}

	public void removeVarsInOtherClasses(Set<String> inputs, Set<String> inputs2, Set<String> outputs, Set<String> outputs2, DefUseChangeFactGroup group) {
		Map<String, ClassChangeFact> changedClasses = group.changedClasses;		
		ClassChangeFact ccf = null;
		Set<String> removedInputs = new HashSet<String>(inputs);
		Set<String> addedInputs = new HashSet<String>(inputs2);
		removedInputs.removeAll(inputs2);
		addedInputs.removeAll(inputs);		
		Map<String, Set<FieldReference>> fieldMap = group.fieldMap;
		Map<String, String> removedCTou = new HashMap<String, String>();		
		Map<String, String> addedCTou = new HashMap<String, String>();		
		for (Entry<String, String> entry : group.concreteToUnified.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
			if (fieldMap.containsKey(key)) {
				if (removedInputs.contains(val)) {
					removedCTou.put(key, val);					
				} else if (addedInputs.contains(val)) {
					addedCTou.put(key, val);					
				}
			}			
		}			
		boolean hasDeletedField = !removedCTou.isEmpty();						
		boolean hasInsertedField = !addedCTou.isEmpty();
		FieldAccessVisitor visitor = new FieldAccessVisitor();
		for (Entry<String, ClassChangeFact> entry : changedClasses.entrySet()) {			
			ccf = entry.getValue();
			if (ccf.ct.equals(CHANGE_TYPE.ADD)) {
				if (hasDeletedField) {
					TypeDeclaration td = ccf.linkedEntity.node;				
					Map<String, IVariableBinding> map = visitor.lookforFieldAccess(td);
					Set<String> foundRemovedFields = new HashSet<String>();
					for (Entry<String, String> entry2 : removedCTou.entrySet()) {
						String cf = entry2.getKey();
						String uf = entry2.getValue();
						if (map.containsKey(cf)) {
							foundRemovedFields.add(uf);
						}
					}					
					inputs2.addAll(foundRemovedFields);
				}				
			} else if (ccf.ct.equals(CHANGE_TYPE.DELETE)) {
				if (hasInsertedField) {
					TypeDeclaration td = ccf.linkedEntity.node;
					Map<String, IVariableBinding> map = visitor.lookforFieldAccess(td);
					Set<String> foundAddedFields = new HashSet<String>();
					for (Entry<String, String> entry2 : addedCTou.entrySet()) {
						String cf = entry2.getKey();
						String uf = entry2.getValue();
						if (map.containsKey(cf)) {
							foundAddedFields.add(uf);
						}
					}					
					inputs.addAll(foundAddedFields);
				}
			}
		}
	}
}
