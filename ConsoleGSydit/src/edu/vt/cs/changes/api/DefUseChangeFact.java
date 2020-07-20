package edu.vt.cs.changes.api;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.EntityType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;

import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;

import edu.vt.cs.append.CommonValue;
import edu.vt.cs.append.TopDownTreeMatcher;
import edu.vt.cs.append.terms.MethodNameTerm;
import edu.vt.cs.append.terms.TypeNameTerm;
import edu.vt.cs.append.terms.VariableTypeBindingTerm;
import edu.vt.cs.changes.api.refchanges.ClassChangeFact;
import edu.vt.cs.changes.api.refchanges.ClassRefChangeFact;
import edu.vt.cs.changes.api.refchanges.FieldChangeFact;
import edu.vt.cs.changes.api.refchanges.FieldRefChangeFact;
import edu.vt.cs.changes.api.refchanges.MethodRefChangeFact;
import edu.vt.cs.diffparser.util.PatternMatcher;

public class DefUseChangeFact {
	
	protected Map<String, FieldReference> fieldMap;	
	protected Map<String, Set<TypeNameTerm>> typeMap = null;
	protected Map<String, Set<MethodNameTerm>> methodMap = null;
	protected Map<String, String> qNameCToA = null;
	protected Map<String, String> concreteToAbstract = null;	
	protected Map<String, String> abstractToConcrete = null;
	
	protected Map<String, String> concreteToAbstractMethod = null;
	protected Map<String, String> abstractToConcreteMethod = null;
	
	protected Map<String, String> concreteToAbstractType = null;
	protected Map<String, String> abstractToConcreteType = null;
	
	public static final String ABSTRACT_V = "VAR_";
	public static final String ABSTRACT_Q = "QNAME_";
	public static final String ABSTRACT_T = "TYPE_";
	public static final String ABSTRACT_M = "M_";	
	public static final String ABSTRACT_C = "C_";
	
	Map<String, ClassRefChangeFact> cRefChangeFactMap = null;
	Map<String, FieldRefChangeFact> fRefChangeFactMap = null;
	Map<String, MethodRefChangeFact> mRefChangeFactMap = null;
	Map<String, FieldChangeFact> fieldChangeFactMap = null;
	Map<String, ClassChangeFact> classChangeFactMap = null;
	Map<Integer, List<Integer>> old_control_line_to_lines = null;
	Map<Integer, List<Integer>> new_control_line_to_lines = null;
	Set<String> exceptions = new HashSet<String>();
	Set<Integer> old_decs = null;
	Set<Integer> new_decs = null;
	
	int index_q = 0;
	int index_v = 0;
	int index_m = 0;
	int index_t = 0;
	int index_c = 0;
	
	public enum DATA_CHANGE_TYPE{
		DEF,
		USE
	};
	
	public DefUseChangeFact() {
		concreteToAbstract = new HashMap<String, String>();
		abstractToConcrete = new HashMap<String, String>();
		
		methodMap = new HashMap<String, Set<MethodNameTerm>>();
		concreteToAbstractMethod = new HashMap<String, String>();
		abstractToConcreteMethod = new HashMap<String, String>();
		
		qNameCToA = new HashMap<String, String>();
		cRefChangeFactMap = new HashMap<String, ClassRefChangeFact>();
		fRefChangeFactMap = new HashMap<String, FieldRefChangeFact>();
		mRefChangeFactMap = new HashMap<String, MethodRefChangeFact>();
		fieldChangeFactMap = new HashMap<String, FieldChangeFact>();
		classChangeFactMap = new HashMap<String, ClassChangeFact>();
		old_control_line_to_lines = new HashMap<Integer, List<Integer>>();
		new_control_line_to_lines = new HashMap<Integer, List<Integer>>();
		
		
		typeMap = new HashMap<String, Set<TypeNameTerm>>();		
		concreteToAbstractType = new HashMap<String, String>();
		abstractToConcreteType = new HashMap<String, String>();
	}
	
	public void addField(String s, FieldReference fRef) {
		if (fieldMap == null) {
			fieldMap = new HashMap<String, FieldReference>();
		}
		fieldMap.put(s, fRef);
	}
	
	public void addFieldChangeFact(String fieldName, FieldChangeFact f) {
		fieldChangeFactMap.put(fieldName, f);
	}
	
	public void addFieldRefChangeFact(String fieldName, FieldRefChangeFact f) {
		fRefChangeFactMap.put(fieldName, f);
	}
	
	public void addClassChangeFact(String className, ClassChangeFact f) {
		classChangeFactMap.put(className, f);
	}
	
	public void addClassRefChangeFact(String typeName, ClassRefChangeFact f) {
		cRefChangeFactMap.put(typeName, f);
	}
	
	public void addMethodRefChangeFact(String methodName, MethodRefChangeFact f) {
		mRefChangeFactMap.put(methodName, f);
	}
	
	public void addControlLtoL(Integer master_line, List<Integer> slave_lines, boolean direction) {
		if (direction) {
			new_control_line_to_lines.put(master_line, slave_lines);
		}
		else {
			old_control_line_to_lines.put(master_line, slave_lines);
		}
	}
	
	public Map<Integer, List<Integer>> getControlLtoL(boolean direction) {
		if (direction) {
			return new_control_line_to_lines;
		}
		else {
			return old_control_line_to_lines;
		}
	}
	
	protected Node getTemplate(Node n, Map<String, DATA_CHANGE_TYPE> parameters, 
			Map<String, IBinding> varBindings) {
		Node copy = NodeManipulator.getCopy(n);		
		Enumeration<Node> nEnum = copy.preorderEnumeration();
		System.out.print("");
//		StringBuffer buffer = new StringBuffer();		
		Node tmp = null;
		Object obj = null;
		String name = null;
		Set<Node> visited = new HashSet<Node>();
		int count = 0;
		while (nEnum.hasMoreElements()) {
			tmp = nEnum.nextElement();
			if (count>0) {
				count--; continue;
			}
			if (tmp.getLabel().equals(JavaEntityType.QUALIFIED_NAME)){
					// ADDED BY SHENGZHE APR-9, GET TYPE_PARAMETER
//					tmp.getLabel().equals(JavaEntityType.TYPE_PARAMETER)) {
				if (visited.contains(tmp)) 
					continue;
				StringBuffer buf = new StringBuffer();
				Node tmp2 = null;			
				Node inter_temp = NodeManipulator.getCopy(tmp);
				Enumeration<Node> nEnum2 = inter_temp.preorderEnumeration();
				while (nEnum2.hasMoreElements()) {					
					tmp2 = nEnum2.nextElement();
					count++;
					if (!visited.add(tmp2)) {
						continue;
					}
					if (!tmp2.isLeaf()) 
						continue;					
					name = tmp2.getValue();				
					buf.append(name);
				}							
				String tmpStr = buf.toString();
				if (!qNameCToA.containsKey(tmpStr)) {
					name = generateNextQName();
					qNameCToA.put(tmpStr, name);
				} else {
					name = qNameCToA.get(tmpStr);					
				}
//				buffer.append(name);
				inter_temp.setValue(name);			
				inter_temp.removeAllChildren();				
				parameters.put(tmpStr, null);
				if (inter_temp.getLabel().equals(JavaEntityType.QUALIFIED_NAME)) {
					IBinding binding = (IBinding) inter_temp.getUserObject();
					varBindings.put(tmpStr, binding);
				}
			} else if (tmp.isLeaf()) {	
				if (visited.contains(tmp)) {
					continue;
				}
				name = tmp.getValue();
				obj = tmp.getUserObject();				
				if (obj instanceof VariableTypeBindingTerm) {
//					String pattern = ((VariableTypeBindingTerm) obj).binding.toString();
//	                String dec_id = "";
//	                boolean kaiguan = false;
//	                for (int i=2;i<pattern.length();i++) {
//	                	if (pattern.charAt(i-2) == 'i'
//	                		&& pattern.charAt(i-1) == 'd'
//	                		&& pattern.charAt(i) == ':') {
//	                		kaiguan = true;
//	                		continue;
//	                	}
//	                	if (kaiguan == true && pattern.charAt(i) == ']') {
//	                		kaiguan = false;
//	                		break;
//	                	}
//	                	if (kaiguan == true) {
//	                		dec_id += pattern.charAt(i);
//	                	}
//	                }
	                parameters.put(name, null);
//					
					//added by shengzhe july, 2017
					if (exceptions.isEmpty()) {
						exceptions.add(name);
					}
					
					VariableTypeBindingTerm vTerm = (VariableTypeBindingTerm)obj;
//				    vTerm.get
					varBindings.put(name, vTerm.binding);
					if (concreteToAbstract.containsKey(name)) {		
						name = concreteToAbstract.get(name);						
					} else {
						String aName = generateNextVar();						
						concreteToAbstract.put(name, aName);
						abstractToConcrete.put(aName, name);
						name = aName;
					}
				} 
				else if (obj instanceof TypeNameTerm) {
//					parameters.put(name, null);
					TypeNameTerm tTerm = (TypeNameTerm)obj;					
					Set<TypeNameTerm> tTerms = typeMap.get(name);
					if (tTerms == null) {
						tTerms = new HashSet<TypeNameTerm>();
						typeMap.put(name, tTerms);
					}
					tTerms.add(tTerm);
					if (concreteToAbstractType.containsKey(name)) {
						name = concreteToAbstractType.get(name);
					} else {
						String aName = generateNextType();
						concreteToAbstractType.put(name, aName);
						abstractToConcreteType.put(aName, name);
						name = aName;
					}
				} else if (obj instanceof MethodNameTerm) {
					MethodNameTerm mTerm = (MethodNameTerm)obj;					
					Set<MethodNameTerm> mTerms = methodMap.get(name);
					if (mTerms == null) {
						mTerms = new HashSet<MethodNameTerm>();
						methodMap.put(name, mTerms);
					}
					mTerms.add(mTerm);
					if (concreteToAbstractMethod.containsKey(name)) {
						name = concreteToAbstractMethod.get(name);
					} else {
						String aName;
						if (mTerm.getMethodBinding().getDeclaringClass().toString().contains(CommonValue.possible_lib_name1)) {
							aName = new String(name);
						}
						else aName = generateNextMethod();
						concreteToAbstractMethod.put(name, aName);
						abstractToConcreteMethod.put(aName, name);
						name = aName;
					}
				}
				tmp.setValue(name);
//				buffer.append(name);
			}			
		}
		return copy;
	}
	
	protected Node getTemplatewithRange(Node n, Map<String, DATA_CHANGE_TYPE> parameters, 
			Map<String, IBinding> varBindings, List<Integer> lines, GraphConvertor2 cc) {
		Node copy = NodeManipulator.getCopy(n);		
		Enumeration<Node> nEnum = copy.preorderEnumeration();
		System.out.print("");
//		StringBuffer buffer = new StringBuffer();		
		Node tmp = null;
		Object obj = null;
		String name = null;
		for (Integer lineNum : lines) {
			List<Integer> indexes = cc.getInstIndexes(lineNum);
//			System.out.println("line:"+ lineNum + "to indexs:" + indexes);
			for (Integer index : indexes) {
				List<String> potential_use_paras = cc.getUses(index);
//				System.out.println("potential_paras:+getuses: " + potential_use_paras);
				for (String pt_para : potential_use_paras) {
					parameters.put(pt_para, null);
				}
				List<String> potential_def_paras = cc.getDefs(index);
//				System.out.println("potential_paras:+getdefs: " + potential_def_paras);
				for (String pt_para : potential_def_paras) {
					parameters.put(pt_para, null);
				}
//				List<String> potential_paras = cc.getParas(index);
//				System.out.println("potential_paras:+getparas: " + potential_paras);
//				for (String pt_para : potential_paras) {
//					parameters.put(pt_para, null);
//				}
			}
		}
		
		Set<Node> visited = new HashSet<Node>();
		int count = 0;
		while (nEnum.hasMoreElements()) {
			tmp = nEnum.nextElement();
			if (count>0) {
				count--;
				continue;
			}
			if (tmp.getLabel().equals(JavaEntityType.QUALIFIED_NAME)){
					// ADDED BY SHENGZHE APR-9, GET TYPE_PARAMETER
//					tmp.getLabel().equals(JavaEntityType.TYPE_PARAMETER)) {
				if (visited.contains(tmp)) 
					continue;
				StringBuffer buf = new StringBuffer();
				Node tmp2 = null;			
				Node inter_temp = NodeManipulator.getCopy(tmp);
				Enumeration<Node> nEnum2 = inter_temp.preorderEnumeration();
				while (nEnum2.hasMoreElements()) {					
					tmp2 = nEnum2.nextElement();
					count++;
					if (!visited.add(tmp2)) {
						continue;
					}
					if (!tmp2.isLeaf()) 
						continue;					
					name = tmp2.getValue();				
					buf.append(name);
				}							
				String tmpStr = buf.toString();
				if (!qNameCToA.containsKey(tmpStr)) {
					name = generateNextQName();
					qNameCToA.put(tmpStr, name);
				} else {
					name = qNameCToA.get(tmpStr);					
				}
//				buffer.append(name);
				inter_temp.setValue(name);
				inter_temp.removeAllChildren();				
				parameters.put(tmpStr, null);
				if (tmp.getLabel().equals(JavaEntityType.QUALIFIED_NAME)) {
					IBinding binding = (IBinding) inter_temp.getUserObject();
					varBindings.put(tmpStr, binding);
				}
			} else if (tmp.isLeaf()) {	
				if (visited.contains(tmp)) {
					continue;
				}
				name = tmp.getValue();
				obj = tmp.getUserObject();
				if (obj instanceof VariableTypeBindingTerm) {
					parameters.put(name, null);
					for (Integer line : lines) {
						parameters.put(name, null);
					}
					//added by shengzhe july, 2017
					if (exceptions.isEmpty()) {
						exceptions.add(name);
					}
					
					VariableTypeBindingTerm vTerm = (VariableTypeBindingTerm)obj;
					varBindings.put(name, vTerm.binding);
					if (concreteToAbstract.containsKey(name)) {		
						name = concreteToAbstract.get(name);						
					} else {
						String aName = generateNextVar();						
						concreteToAbstract.put(name, aName);
						abstractToConcrete.put(aName, name);
						name = aName;
					}
				} 
				else if (obj instanceof TypeNameTerm) {
//					parameters.put(name, null);
					TypeNameTerm tTerm = (TypeNameTerm)obj;					
					Set<TypeNameTerm> tTerms = typeMap.get(name);
					if (tTerms == null) {
						tTerms = new HashSet<TypeNameTerm>();
						typeMap.put(name, tTerms);
					}
					tTerms.add(tTerm);
					if (concreteToAbstractType.containsKey(name)) {
						name = concreteToAbstractType.get(name);
					} else {
						String aName = generateNextType();
						concreteToAbstractType.put(name, aName);
						abstractToConcreteType.put(aName, name);
						name = aName;
					}
				} else if (obj instanceof MethodNameTerm) {
					MethodNameTerm mTerm = (MethodNameTerm)obj;					
					Set<MethodNameTerm> mTerms = methodMap.get(name);
					if (mTerms == null) {
						mTerms = new HashSet<MethodNameTerm>();
						methodMap.put(name, mTerms);
					}
					mTerms.add(mTerm);
					if (concreteToAbstractMethod.containsKey(name)) {
						name = concreteToAbstractMethod.get(name);
					} else {
						String aName = generateNextMethod();
						concreteToAbstractMethod.put(name, aName);
						abstractToConcreteMethod.put(aName, name);
						name = aName;
					}
				}
				tmp.setValue(name);
//				buffer.append(name);
			}			
		}
		return copy;
	}

	
	private String generateNextMethod() {
		return ABSTRACT_M + (index_m++);
	}
	
	private String generateNextQName() {
		return ABSTRACT_Q + (index_q++);
	}
	
	private String generateNextType() {
		return ABSTRACT_T + (index_t++);
	}
	
	private String generateNextVar() {
		return ABSTRACT_V + (index_v++);
	}
	
	private String generateNextConst() {
		return ABSTRACT_C + (index_c++);
	}
	
	public String render() {return null;}
		
	public static String render(Node n) {
		Enumeration<Node> nEnum = n.preorderEnumeration();
		StringBuffer buffer = new StringBuffer();		
		Node tmp = null;
		if (n.subStmtStarts == null) {			
			while (nEnum.hasMoreElements()) {
				tmp = nEnum.nextElement();
				if (CommonValue.same_prefix>0) {
					CommonValue.same_prefix--;
					continue;
				}
				if (tmp.isLeaf()) {					
					buffer.append(tmp.getValue());
				}			
			}			
		} else {
			Enumeration<Node> cEnum = n.children();
			int index = 0;
			Node child = null;
			int start = n.subStmtStarts.get(0);
			Node childToStop = null;
			while (cEnum.hasMoreElements()) {
				child = cEnum.nextElement();
				if (CommonValue.same_prefix>0) {
					CommonValue.same_prefix--;
					continue;
				}
				if (index == start) {
					childToStop = child;
					break;
				}
				index++;
			}
			int block_label = 0;
			while (nEnum.hasMoreElements()) {
				tmp = nEnum.nextElement();
				
				if (tmp.equals(childToStop)) {
					break;
				}
				if (tmp.isLeaf()) {
					buffer.append(tmp.getValue());
				}
			}
		}	
		return buffer.toString();
	}
	
	public static String renderWithTypeInfo(Node n, Map<String, String>api_import) {
		Map<Integer, String> nodeTypeMap = new HashMap<Integer, String>();
		Set<Integer> nodeInsts = new HashSet<Integer>();
		Set<Integer> nodeStaticMethod = new HashSet<Integer>();
		Queue<Node> queue = new LinkedList<Node>();		
		Node node = NodeManipulator.getCopy(n);
		queue.add(node);			
		Node copy = null;
		List<Node> childList = null;
		while(!queue.isEmpty()) {
			copy = queue.remove();			
			childList = new ArrayList<Node>();
			Enumeration<Node> cEnum = copy.children();		
			while(cEnum.hasMoreElements()) {				
				childList.add(cEnum.nextElement());				
			}						
			if (childList.isEmpty()) {
				String val = copy.getValue();
				if (PatternMatcher.vPattern.matcher(val).matches() ||
						PatternMatcher.qPattern.matcher(val).matches() ||
						PatternMatcher.uPattern.matcher(val).matches()) {
					if (nodeTypeMap.containsKey(copy.id)) {
						if (nodeInsts.contains(copy.id))
//							copy.setValue(val + "_" + nodeTypeMap.get(copy.id) + "_inst");
							copy.setValue(val + "_" + nodeTypeMap.get(copy.id));
						else if (nodeStaticMethod.contains(copy.id))
//							copy.setValue(val + "_" + nodeTypeMap.get(copy.id) + "_static");
							copy.setValue(val + "_" + nodeTypeMap.get(copy.id));
						else
							copy.setValue(val + "_" + nodeTypeMap.get(copy.id));
					}
				}
			} else {
				EntityType et = copy.getLabel();				
				if (et.equals(JavaEntityType.METHOD_INVOCATION) ||
						et.equals(JavaEntityType.CONSTRUCTOR_INVOCATION) ||
						et.equals(JavaEntityType.CLASS_INSTANCE_CREATION) ||
						et.equals(JavaEntityType.SUPER_CONSTRUCTOR_INVOCATION) ||
						et.equals(JavaEntityType.SUPER_METHOD_INVOCATION)) {					
					List<Object> info = (List<Object>)copy.getUserObject();
					if (info == null) continue;
					IMethodBinding binding = (IMethodBinding)info.get(0);
					List<Integer> paramIndexes = (List<Integer>)info.get(1);
					ITypeBinding[] tBindings = binding.getParameterTypes();		
					int base = 0;
					int nodeId = 0;
					if (!binding.isConstructor()) {
						if (binding.getModifiers() == 9 &&
								!paramIndexes.isEmpty()) {
							base++;							
							nodeId = childList.get(paramIndexes.get(0)).id;
							nodeTypeMap.put(nodeId, binding.getDeclaringClass().getName());
							nodeStaticMethod.add(nodeId);
//							System.out.println("9alarm");
						}
						if ((binding.getModifiers() & Modifier.STATIC) == 0 &&
								!paramIndexes.isEmpty()) {				        
								base++;							
								nodeId = childList.get(paramIndexes.get(0)).id;
								// added by shengzhe to make the enclosing type show correctly.
								if (binding.getDeclaringClass().getKey().contains(CommonValue.possible_lib_name2)
										&& binding.getDeclaringClass().getKey().contains("$")) {
									String type_name = binding.getDeclaringClass().getKey();
									String dealt_type_name = type_name.split("/")[type_name.split("/").length-1];
									dealt_type_name = dealt_type_name.substring(0, dealt_type_name.length()-1);
									dealt_type_name = dealt_type_name.replace('$', '_');
									if (api_import != null) {
										api_import.put(dealt_type_name, binding.getDeclaringClass().getKey());
									}
									nodeTypeMap.put(nodeId, dealt_type_name);
								}
								else {
									if (api_import != null && binding.getDeclaringClass().getKey().contains(CommonValue.possible_lib_name2)) {
										api_import.put(binding.getDeclaringClass().getName(), binding.getDeclaringClass().getKey());
									}
									nodeTypeMap.put(nodeId, binding.getDeclaringClass().getName());
								}
								nodeInsts.add(nodeId);
										
						}					
					}

//					System.out.println("Attention:" + tBindings.length);
					for (int i = 0; i < tBindings.length; i++) {
//						System.out.println(i+base);
//						System.out.println(paramIndexes);
//						if (i+base >= paramIndexes.size()) {
//							System.out.println("paramIndexes size:" + paramIndexes.size() + " but " + (i+base) + "=" + i+"+"+ base +"wanted");
//							continue;
//						}
						
//						System.out.println("|"+i+"+"+ base + "=" + (i+base)+"however:" + paramIndexes.size());
//						System.out.println(paramIndexes.get(i + base));
//						System.out.println(childList.get(paramIndexes.get(i + base)));
						if (i+base==paramIndexes.size())
							nodeId = childList.get(paramIndexes.get(i + base - 1)).id;
//						else if (tBindings.length + base < paramIndexes.size())
//							nodeId = childList.get(paramIndexes.get(i + base + 1)).id;
						else nodeId = childList.get(paramIndexes.get(i + base)).id;
//						System.out.println(nodeId);
//						System.out.println("1");
//						System.out.println(tBindings[i].getName());
						nodeTypeMap.put(nodeId, tBindings[i].getName());
//						System.out.println("Finish");
					}
				}
				queue.addAll(childList);
			}			
		}
		return render(node);
		
		
//		StringBuffer buffer = new StringBuffer();		
//		Node tmp = null;
//		while (nEnum.hasMoreElements()) {
//			tmp = nEnum.nextElement();
//			if (tmp.isLeaf()) {
//				
//				buffer.append(tmp.getValue());
//			}			
//		}
//		return buffer.toString();
	}
	

	
	public static void replaceIdentifier(Node n, String oldStr, String newStr) {
		Enumeration<Node> nEnum = n.preorderEnumeration();
		Node tmp = null;
		String val = null;
		while (nEnum.hasMoreElements()) {
			tmp = nEnum.nextElement();
			if (tmp.isLeaf()) {
				val = tmp.getValue();
				if (val.equals(oldStr))
					tmp.setValue(newStr);
				}
		}
	}
	
	public static void replaceIdentifiers(Node n, Map<String, String> oldToNew) {
		Enumeration<Node> nEnum = n.preorderEnumeration();
		Node tmp = null;
		String val = null;
		while (nEnum.hasMoreElements()) {
			tmp = nEnum.nextElement();
//			System.out.print("|->" + tmp.getValue());
			if (tmp.isLeaf()) {
				val = tmp.getValue();
				if (oldToNew.containsKey(val)) {
					tmp.setValue(oldToNew.get(val));
				}
			}
//			System.out.println("->" + tmp.getValue());
		}		
	}
	
	// added by shengzhe for the same pre_fix problem
	public static void compare_two_patterns(Node oldnode, Node newnode) {
		TopDownTreeMatcher matcher = new TopDownTreeMatcher();
		matcher.match(oldnode, newnode);
		
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("Relevant changed class refs:");
		for (Entry<String, ClassRefChangeFact> entry : cRefChangeFactMap.entrySet()) {
			buf.append("<").append(entry.getKey()).append(",").append(entry.getValue()).append(">");
		}
		buf.append("\nRelevant changed fields:");
		for (Entry<String, FieldChangeFact> entry : fieldChangeFactMap.entrySet()) {
			buf.append("<").append(entry.getKey()).append(",").append(entry.getValue()).append(">");
		}
		return buf.toString();
	}
	
	public void removeParameter(String key){
		if (concreteToAbstract.containsKey(key)) {
			String val = concreteToAbstract.remove(key);
			replaceIdentifier(val, key);			
			abstractToConcrete.remove(val);			
		} else if (qNameCToA.containsKey(key)) {
			replaceIdentifier(qNameCToA.get(key), key);
			qNameCToA.remove(key);			
		}		
	}
	
	public void removeMethodParameter(String key) {
		if (concreteToAbstractMethod.containsKey(key)) {
			String val = concreteToAbstractMethod.remove(key);
			replaceIdentifier(val, key);
			abstractToConcreteMethod.remove(val);
		}
	}
	
	public void removeTypeParameter(String key) {
		if (concreteToAbstractType.containsKey(key)) {
			String val = concreteToAbstractType.remove(key);
			replaceIdentifier(val, key);			
			abstractToConcreteType.remove(val);			
		}		
	}
	
	public void replaceIdentifier(String oldStr, String newStr) {}
	
	public void replaceMethodIdentifier(String oldStr, String newStr) {
		if (abstractToConcreteMethod.containsKey(oldStr)) {
			replaceIdentifier(oldStr, newStr);
			String key = abstractToConcreteMethod.get(oldStr);
			abstractToConcreteMethod.remove(oldStr);
			concreteToAbstractMethod.put(key, newStr);
			abstractToConcreteMethod.put(newStr, key);
		}
	}
	
	public void replaceTypeIdentifier(String oldStr, String newStr){
		if (abstractToConcreteType.containsKey(oldStr)) {
			replaceIdentifier(oldStr, newStr);
			String key = abstractToConcreteType.get(oldStr);
			abstractToConcreteType.remove(oldStr);
			concreteToAbstractType.put(key, newStr);
			abstractToConcreteType.put(newStr, key);
		}
	}
	
	protected Set getexceptions() {
		return exceptions;
	}
}
