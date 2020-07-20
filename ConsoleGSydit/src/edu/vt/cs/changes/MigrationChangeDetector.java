package edu.vt.cs.changes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.VariableDeclaration;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.ide.util.ASTNodeFinder;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.collections.Pair;

import edu.vt.cs.append.CommonValue;
import edu.vt.cs.append.DatabaseControl;
import edu.vt.cs.changes.api.APIResolver;
import edu.vt.cs.changes.api.FieldAPIResolver;
import edu.vt.cs.changes.api.MethodAPIResolver;
import edu.vt.cs.changes.api.TypeAPIResolver;
import edu.vt.cs.diffparser.util.SourceCodeRange;
import edu.vt.cs.graph.ClientField;
import partial.code.grapa.commit.CommitComparator;
import partial.code.grapa.dependency.graph.DataFlowAnalysisEngine;
import partial.code.grapa.mapping.ClientMethod;
import partial.code.grapa.mapping.ClientMethodVisitor;

public class MigrationChangeDetector {

//	public static final String libStr = "org.apache.lucene";
//	public static final String libBinStr = "Lorg/apache/lucene";
//	public static final String libStr = "org.bukkit";
//	public static final String libBinStr = "Lorg/bukkit";
	public static final String libStr = CommonValue.possible_lib_name1;
	public static final String libBinStr = "L" + CommonValue.join("/", CommonValue.possible_lib_name1.split("[.]"));
	
	private DataFlowAnalysisEngine leftEngine = null;
	private DataFlowAnalysisEngine rightEngine = null;
	private List<ChangeFact> cfList;
	
	private void analyzeChanges(Map<IMethodBinding, Set<ChangeFact>> oldMethodBindings, Map<IMethodBinding, Set<ChangeFact>> newMethodBindings, 
			Map<String, Set<ChangeFact>> oldTypeMap, Map<String, Set<ChangeFact>> newTypeMap, 
			Map<String, ITypeBinding> oldClassNameToBinding, Map<String, ITypeBinding> newClassNameToBinding,
			Map<IVariableBinding, Set<ChangeFact>> oldFieldBindings, Map<IVariableBinding, Set<ChangeFact>> newFieldBindings) {		
		
		// record the methodbindings that exist in one version but not in the other version
		Map<IMethodBinding, Set<ChangeFact>> missingMethods = new HashMap<IMethodBinding, Set<ChangeFact>>();
		Map<IVariableBinding, Set<ChangeFact>> missingFields = new HashMap<IVariableBinding, Set<ChangeFact>>();
		Map<IVariableBinding, Set<ChangeFact>> added_lib_version = new HashMap<IVariableBinding, Set<ChangeFact>>();
		
		Map<String, Set<ChangeFact>> missingTypes = new HashMap<String, Set<ChangeFact>>();
		Map<String, ITypeBinding> typeMap = new HashMap<String, ITypeBinding>();
		Set<ChangeFact> value = null;
		IMethodBinding mb = null;
		ITypeBinding tb = null;
		IVariableBinding vb = null;
		IClass iclass = null;
		String key = null;
		for (Entry<IMethodBinding, Set<ChangeFact>> entry : oldMethodBindings.entrySet()) {
			mb = entry.getKey();
			tb = mb.getDeclaringClass();
			iclass = rightEngine.getClass(tb);
			value = entry.getValue();
			if (iclass != null) {
			  IMethod method = rightEngine.getMethod(iclass, mb);
			  if (method != null) {
			    continue;
			  } else {
				missingMethods.put(mb, value);
			  }
			} else {
			  String tName = ClientMethodVisitor.getTypeNameToRemoveComma(tb.getKey());				 
			  Set<ChangeFact> tmpSet = missingTypes.get(tName);
			  if (tmpSet == null) {
				  tmpSet = new HashSet<ChangeFact>();
				  missingTypes.put(tName, tmpSet);
				  typeMap.put(tName, tb);
			  }
			  tmpSet.addAll(value);
			}	
		}
		for (Entry<String, Set<ChangeFact>> entry : oldTypeMap.entrySet()) {
			key = entry.getKey();
			String tName = ClientMethodVisitor.getTypeNameToRemoveComma(key);
			tb = oldClassNameToBinding.get(key);
			iclass = rightEngine.getClass(tb);
			value = entry.getValue();
			Set<ChangeFact> tmpSet = missingTypes.get(tName);
			if (tmpSet == null) {
				tmpSet = new HashSet<ChangeFact>();
				missingTypes.put(tName, tmpSet);
				typeMap.put(tName, tb);
			}
			tmpSet.addAll(value);
		}
		for (Entry<IVariableBinding, Set<ChangeFact>> entry : oldFieldBindings.entrySet()) {
			vb = entry.getKey();
//			System.out.println(vb +" " + entry.getValue());
			tb = vb.getDeclaringClass();
//			System.out.println(tb);
			// patch after parameter type in method declaration fixed, Apr-10
			if (tb==null) continue;
			
			iclass = rightEngine.getClass(tb);
			value = entry.getValue();
			
			if (iclass != null) {
			  IField field = rightEngine.getVariable(iclass, vb);
			  if (field != null) {
			    continue;
			  } else {
				missingFields.put(vb, value);
			  }
			} else {
			  String tName = ClientMethodVisitor.getTypeNameToRemoveComma(tb.getKey());				 
			  Set<ChangeFact> tmpSet = missingTypes.get(tName);
			  if (tmpSet == null) {
				  tmpSet = new HashSet<ChangeFact>();
				  missingTypes.put(tName, tmpSet);
				  typeMap.put(tName, tb);
			  }
			  tmpSet.addAll(value);
			}	
		}
		
		//added by shengzhe for insert if (version condition)
		for (Entry<IVariableBinding, Set<ChangeFact>> entry : newFieldBindings.entrySet()) {
			vb = entry.getKey();
//			System.out.println(vb +" " + entry.getValue());
			tb = vb.getDeclaringClass();
//			System.out.println(tb);
			// patch after parameter type in method declaration fixed, Apr-10
			if (tb==null) continue;
			
			iclass = leftEngine.getClass(tb);
			value = entry.getValue();
			
			if (iclass != null) {
			  IField field = leftEngine.getVariable(iclass, vb);
			  if (field != null) {
			    continue;
			  } else {
				  if (iclass.getName().toString().contains(CommonValue.possible_lib_name2)
						  && (iclass.getName().toString().contains("version")
							   || iclass.getName().toString().contains("VERSION")))
				  added_lib_version.put(vb, value);
			  }
			} else {
			  String tName = ClientMethodVisitor.getTypeNameToRemoveComma(tb.getKey());				 
//			  Set<ChangeFact> tmpSet = missingTypes.get(tName);
//			  if (tmpSet == null) {
//				  tmpSet = new HashSet<ChangeFact>();
//				  missingTypes.put(tName, tmpSet);
//				  typeMap.put(tName, tb);
//			  }
//			  tmpSet.addAll(value);
			}	
		}
								
//			} else if (b instanceof IVariableBinding) {
//				IVariableBinding vb = (IVariableBinding)b;
//				ITypeBinding tb = vb.getDeclaringClass();
//				String tName = ClientMethodVisitor.getTypeNameToRemoveComma(tb.getKey());
//				IClass iclass = rightEngine.getClass(tb);
//				if (iclass != null) {
//				  IField variable = rightEngine.getVariable(iclass, vb);
//				  if (variable != null) {
//				    continue;
//				  } else {
//				    missingFields.put(vb, value);
//				  }
//				} else {
//				  Set<ChangeFact> tmpSet = missingTypes.get(tName);
//				  if (tmpSet == null) {
//				    tmpSet = new HashSet<ChangeFact>();
//				    missingTypes.put(tName, value);
//				    typeMap.put(tName, tb);
//				  }
//				  tmpSet.addAll(value);
//				}
//			}
		new APIResolver(leftEngine, rightEngine, missingMethods, missingFields, missingTypes, typeMap, added_lib_version);
//		MethodAPIResolver.resolve(missingMethods, leftEngine, rightEngine);
//		FieldAPIResolver.resolve(missingFields, leftEngine, rightEngine);
//		new TypeAPIResolver(leftEngine, rightEngine).resolve(missingTypes, typeMap);
	}
	
	public void detect(List<ChangeFact> cfList, CommitComparator comparator) {
		leftEngine = comparator.getLeftAnalysisEngine();
		rightEngine = comparator.getRightAnalysisEngine();
	
		this.cfList = cfList;
		Map<IMethodBinding, Set<ChangeFact>> allOldMethodMap = new HashMap<IMethodBinding, Set<ChangeFact>>();
		Map<IMethodBinding, Set<ChangeFact>> allNewMethodMap = new HashMap<IMethodBinding, Set<ChangeFact>>();
		
		Map<String, ITypeBinding> allOldClassNameToBinding = new HashMap<String, ITypeBinding>();
		Map<String, Set<ChangeFact>> allOldTypeMap = new HashMap<String, Set<ChangeFact>>();
		Map<String, ITypeBinding> allNewClassNameToBinding = new HashMap<String, ITypeBinding>();
		Map<String, Set<ChangeFact>> allNewTypeMap = new HashMap<String, Set<ChangeFact>>();
		
		Map<IVariableBinding, Set<ChangeFact>> allOldFieldMap = new HashMap<IVariableBinding, Set<ChangeFact>>();
		Map<IVariableBinding, Set<ChangeFact>> allNewFieldMap = new HashMap<IVariableBinding, Set<ChangeFact>>();
		
		
		Set<ChangeFact> cfSet = null;
		for (ChangeFact cf: cfList) {
			for (ChangeMethodData c : cf.changedMethodData) {		
//				System.out.println("oldrange:" + c.oldASTRanges);
//				System.out.println("newrange:" + c.newASTRanges);
				
				Map<IMethodBinding, Set<SourceCodeRange>> oldMethodMap = new HashMap<IMethodBinding, Set<SourceCodeRange>>();
				Map<String, Set<SourceCodeRange>> oldTypeMap = new HashMap<String, Set<SourceCodeRange>>();
				Map<IVariableBinding, Set<SourceCodeRange>> oldFieldMap = new HashMap<IVariableBinding, Set<SourceCodeRange>>();
				
		
				Map<String, ITypeBinding> oldClassNameToBinding = new HashMap<String, ITypeBinding>();				
				parseAPIs(leftEngine, libStr, c.oldMethod, c.oldASTRanges, oldMethodMap, oldTypeMap, oldClassNameToBinding, oldFieldMap);
				
				Map<IMethodBinding, Set<SourceCodeRange>> newMethodMap = new HashMap<IMethodBinding, Set<SourceCodeRange>>();
				Map<String, Set<SourceCodeRange>> newTypeMap = new HashMap<String, Set<SourceCodeRange>>();
				Map<String, ITypeBinding> newClassNameToBinding = new HashMap<String, ITypeBinding>();
				Map<IVariableBinding, Set<SourceCodeRange>> newFieldMap = new HashMap<IVariableBinding, Set<SourceCodeRange>>();
				parseAPIs(rightEngine, libStr, c.newMethod, c.newASTRanges, newMethodMap, newTypeMap, newClassNameToBinding, newFieldMap);					
							
				// process method bindings				
				Set<IMethodBinding> common = new HashSet<IMethodBinding>(oldMethodMap.keySet());
				common.retainAll(newMethodMap.keySet());
				// add by shengzhe [
//				System.out.println(common);
//				System.out.println(oldMethodMap.keySet().toString());
//				System.out.println(newMethodMap.keySet().toString());
				for (IMethodBinding b: oldMethodMap.keySet()) {
//					System.out.println("ha?old ->" + b);
					for (IMethodBinding ab: newMethodMap.keySet()) {
//						System.out.println("ha?new ->" + ab);
						if (ab.isEqualTo(b)) {
//							System.out.println("Yes!\n"+ b +"\n"+ab);
							common.add(b);
						}
//						System.out.println(ab.getReturnType().getName() + ":::" + b.getReturnType().getName());
//						System.out.println(ab.getReturnType().getName().equalsIgnoreCase(b.getReturnType().getName()));
						if (ab.getName().equalsIgnoreCase(b.getName()) && ab.getDeclaringClass().getName().equalsIgnoreCase(b.getDeclaringClass().getName())
								&& !ab.getReturnType().getName().equalsIgnoreCase(b.getReturnType().getName())) {
							System.out.println("API Return Type Changed:\n" + 
									b + "\n->\n" + ab);
							String dealt_type_name;
							if (b.getDeclaringClass().getKey().contains(CommonValue.possible_lib_name2)
									&& b.getDeclaringClass().getKey().contains("$")) {
								String type_name = b.getDeclaringClass().getKey();
								dealt_type_name = type_name.split("/")[type_name.split("/").length-1];
								dealt_type_name = dealt_type_name.substring(0, dealt_type_name.length()-1);
								dealt_type_name = dealt_type_name.replace('$', '_');								
							}
							else dealt_type_name = b.getDeclaringClass().getName();
							String[] inter_p = b.toString().split(" ");
							String pattern_left = inter_p[0] + " " + inter_p[1] + " " + dealt_type_name + "." + inter_p[2];
							inter_p = ab.toString().split(" ");
							String pattern_right = inter_p[0] + " " + inter_p[1] + " " + dealt_type_name + "." + inter_p[2];
							
							DatabaseControl data1 = new DatabaseControl();
							int label = data1.insertpattern(pattern_left, pattern_right, CommonValue.common_old_version, CommonValue.common_new_version, "RT type change", "1");
							data1.insertsnippet(b.toString(), ab.toString(), CommonValue.common_project_name, CommonValue.common_commit_number, String.valueOf(label)
									, CommonValue.common_old_version+"-"+CommonValue.common_new_version);
						
						}
					}
				}
				// ]
				
				if (!common.isEmpty()) {//remove common usage of methods
					for (IMethodBinding key : common) {						
						oldMethodMap.remove(key);
						newMethodMap.remove(key);
					}
				}
				c.oldMethodBindingMap = oldMethodMap;
				c.newMethodBindingMap = newMethodMap;
				for (IMethodBinding key : oldMethodMap.keySet()) {
					cfSet = allOldMethodMap.get(key);
					if (cfSet == null) {
						cfSet = new HashSet<ChangeFact>();
						allOldMethodMap.put(key, cfSet);
					}
					cfSet.add(cf);
				}
				for (IMethodBinding key : newMethodMap.keySet()) {
					cfSet = allNewMethodMap.get(key);
					if (cfSet == null) {
						cfSet = new HashSet<ChangeFact>();
						allNewMethodMap.put(key, cfSet);
					}
					cfSet.add(cf);
				}
				
				//****************Important Next Step
				// process type bindings
				Set<String> common2 = new HashSet<String>(oldClassNameToBinding.keySet());
				common2.retainAll(newClassNameToBinding.keySet());
				if (!common2.isEmpty()) {//remove common usage of classes
					for (String key : common2) {
						oldTypeMap.remove(key);
						newTypeMap.remove(key);
						oldClassNameToBinding.remove(key);
						newClassNameToBinding.remove(key);
					}						
				}
				c.oldTypeBindingMap = oldTypeMap;
				c.newTypeBindingMap = newTypeMap;
				c.oldClassNameToBinding = oldClassNameToBinding;
				c.newClassNameToBinding = newClassNameToBinding;
				for (String key : oldTypeMap.keySet()) {
					cfSet = allOldTypeMap.get(key);
					if (cfSet == null) {
						cfSet = new HashSet<ChangeFact>();
						allOldTypeMap.put(key, cfSet);
						allOldClassNameToBinding.put(key, oldClassNameToBinding.get(key));
					}
					cfSet.add(cf);
				}
				for (String key : newTypeMap.keySet()) {
					cfSet = allNewTypeMap.get(key);
					if (cfSet == null) {
						cfSet = new HashSet<ChangeFact>();
						allNewTypeMap.put(key, cfSet);
						allNewClassNameToBinding.put(key, newClassNameToBinding.get(key));
					}
					cfSet.add(cf);
				}
				// process field bindings
				Set<IVariableBinding> common3 = new HashSet<IVariableBinding>(oldFieldMap.keySet());
				common3.retainAll(newFieldMap.keySet());
				if (!common3.isEmpty()) {//remove common usage of methods
					for (IVariableBinding key : common3) {
						oldFieldMap.remove(key);
						newFieldMap.remove(key);
					}
				}
//				System.out.println("Old Code:");
//				System.out.println(oldFieldMap);
//
//				System.out.println("New Code:");
//				System.out.println(newFieldMap);
				
				c.oldFieldBindingMap = oldFieldMap;
				c.newFieldBindingMap = newFieldMap;
				for (IVariableBinding key : oldFieldMap.keySet()) {
					cfSet = allOldFieldMap.get(key);
					if (cfSet == null) {
						cfSet = new HashSet<ChangeFact>();
						allOldFieldMap.put(key, cfSet);
					}
					cfSet.add(cf);
				}
				for (IVariableBinding key : newFieldMap.keySet()) {
					cfSet = allNewFieldMap.get(key);
					if (cfSet == null) {
						cfSet = new HashSet<ChangeFact>();
						allNewFieldMap.put(key, cfSet);
					}
					cfSet.add(cf);
				}
			}
			// Changed Type Data. add by shengzhe
//			for (Pair<ClientField, ClientField> c : cf.changedFields) {
			for (ChangeFieldData c : cf.changedFieldData) {
//				System.out.println("111: " +c.toString());
				Map<IVariableBinding, Set<SourceCodeRange>> oldFieldMap = new HashMap<IVariableBinding, Set<SourceCodeRange>>();
				parseFieldsAPIs(leftEngine, libStr, c.oldField, c.oldASTRanges, oldFieldMap);

				Map<IVariableBinding, Set<SourceCodeRange>> newFieldMap = new HashMap<IVariableBinding, Set<SourceCodeRange>>();
				parseFieldsAPIs(rightEngine, libStr, c.newField, c.newASTRanges, newFieldMap);

				// process feild bindings
				Set<IVariableBinding> common4 = new HashSet<IVariableBinding>(oldFieldMap.keySet());
				common4.retainAll(newFieldMap.keySet());
				if (!common4.isEmpty()) {//remove common usage of methods
					for (IVariableBinding key : common4) {
						oldFieldMap.remove(key);
						newFieldMap.remove(key);
					}
				}
				c.oldFieldBindingMap = oldFieldMap;
				c.newFieldBindingMap = newFieldMap;
				for (IVariableBinding key : oldFieldMap.keySet()) {
					cfSet = allOldFieldMap.get(key);
					if (cfSet == null) {
						cfSet = new HashSet<ChangeFact>();
						allOldFieldMap.put(key, cfSet);
					}
					cfSet.add(cf);
				}
				for (IVariableBinding key : newFieldMap.keySet()) {
					cfSet = allNewFieldMap.get(key);
					if (cfSet == null) {
						cfSet = new HashSet<ChangeFact>();
						allNewFieldMap.put(key, cfSet);
					}
					cfSet.add(cf);
				}

			}
//			for (Map<SourceCodeEntity, SourceCodeChange> c : cf.entityChanges) {
//				
//			}
		}
//		System.out.println(allOldMethodMap.size());
//		System.out.println(allOldTypeMap.size());
//		System.out.println(allOldClassNameToBinding.size());
//		System.out.println(allOldFieldMap.size());
		
		analyzeChanges(allOldMethodMap, allNewMethodMap, allOldTypeMap, allNewTypeMap, allOldClassNameToBinding, allNewClassNameToBinding, allOldFieldMap, allNewFieldMap);
	}
	
	private void parseAPIs(DataFlowAnalysisEngine engine,
			String libStr, ClientMethod m, List<SourceCodeRange> ranges, 
			Map<IMethodBinding, Set<SourceCodeRange>> methodMap, 
			Map<String, Set<SourceCodeRange>> typeMap, 
			Map<String, ITypeBinding> classNameToBinding,
			Map<IVariableBinding, Set<SourceCodeRange>> fieldMap) {
		MethodDeclaration md = m.methodbody;
		Initializer il = m.initializerbody;
		ASTNode n = null;
		LibAPIParser parser = new LibAPIParser(engine, libStr);		
		List<IBinding> bindingList  = null;
		Set<SourceCodeRange> value = null;		
		ITypeBinding tb = null;
		String key = null;
		for (SourceCodeRange r : ranges) {
			if (md != null) {
				n = NodeFinder.perform(md, r.startPosition, r.length);
			}
			else {
				n = NodeFinder.perform(il, r.startPosition, r.length);
			}
			parser.init();
			// add method extraction -- Apr-10 Shengzhe
			if (n == null) {
				System.out.println();
				continue;
			}
			n.accept(parser);
			bindingList = parser.getBindings();
			for (IBinding b : bindingList) {
				if (b instanceof IMethodBinding) {
					value = methodMap.get(b);
					if (value == null) {
						value = new HashSet<SourceCodeRange>();
//						System.out.println("key?" + b);
//						System.out.println("value" + value);
					
						methodMap.put((IMethodBinding)b, value);
					}
					value.add(r);
//					System.out.println("methodBinding: " + value);
				} else if (b instanceof ITypeBinding) {
					tb = (ITypeBinding)b;
					key = tb.getKey();
					value = typeMap.get(key);
					if (value == null) {
						value = new HashSet<SourceCodeRange>();
						typeMap.put(key, value);
						classNameToBinding.put(key, tb);
					}
					value.add(r);
//					System.out.println("typeBinding: " + key + "+" + value);
				} else if (b instanceof IVariableBinding) { //this is a field API
					value = fieldMap.get(b);
					if (value == null) {
						value = new HashSet<SourceCodeRange>();
						fieldMap.put((IVariableBinding)b, value);
					}
					value.add(r);
//					System.out.println("varBinding: " + value);
				}
			}			
		}		
	}
	
	//add by shengzhe
	private void parseFieldsAPIs(DataFlowAnalysisEngine engine,
			String libStr, ClientField cd, List<SourceCodeRange> ranges,
			Map<IVariableBinding, Set<SourceCodeRange>> fieldMap) {
		VariableDeclaration vd = cd.field;
		LibAPIParser parser = new LibAPIParser(engine, libStr);		
		List<IBinding> bindingList  = null;
		Set<SourceCodeRange> value = null;		
		ITypeBinding tb = null;
		String key = null;
		
		ASTNode n = null;
		for (SourceCodeRange r : ranges) {
			n = NodeFinder.perform(cd.ast, r.startPosition, r.length);
			parser.init();
			n.accept(parser);
			bindingList = parser.getBindings();
			for (IBinding b : bindingList) {
				if (b instanceof IVariableBinding) { //this is a field API
					value = fieldMap.get(b);
					if (value == null) {
						value = new HashSet<SourceCodeRange>();
						fieldMap.put((IVariableBinding)b, value);
					}
					value.add(r);
//					System.out.println("varBinding: " + value);
				}
			}
		}
	}
}
