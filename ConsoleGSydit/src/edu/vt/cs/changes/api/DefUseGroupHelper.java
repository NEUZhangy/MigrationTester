package edu.vt.cs.changes.api;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ITypeBinding;

import com.ibm.wala.types.FieldReference;

import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import edu.vt.cs.append.terms.MethodNameTerm;
import edu.vt.cs.append.terms.TypeNameTerm;
import edu.vt.cs.changes.MigrationChangeDetector;

public class DefUseGroupHelper {
	
	public static void parameterizeFieldUsage(List<Integer> refinedFacts, Map<Integer, DefUseChangeFact> facts, Map<String, Set<FieldReference>> fieldMap) { 
		for (Integer idx : refinedFacts) {
			DefUseChangeFact f = facts.get(idx);
			Map<String, FieldReference> fMap = f.fieldMap;
			if (fMap == null)
				continue;
			for (Entry<String, FieldReference> entry : fMap.entrySet()) {
				String key = entry.getKey();
				FieldReference val = entry.getValue();
				Set<FieldReference> refs = fieldMap.get(key);
				if (refs == null) {
					refs = new HashSet<FieldReference>();
					fieldMap.put(key, refs);
				}
				refs.add(val);
			}
		}
		
	}
	
	
	public static void parameterizeMethodUsage(List<Node> templateList, Map<String, Set<MethodNameTerm>> methodMap) {
		Enumeration<Node> nEnum = null;
		MethodNameTerm mTerm = null;
		Node temp = null;
		Set<MethodNameTerm> mTerms = null;
		for (Node n : templateList) {
			nEnum = n.breadthFirstEnumeration();
			while (nEnum.hasMoreElements()) {
				temp = nEnum.nextElement();
				if (!temp.isLeaf())
					continue;
				Object obj = temp.getUserObject();
				if (obj instanceof MethodNameTerm) {
					mTerm = (MethodNameTerm)obj;
					String mName = mTerm.getMethodName();
					mTerms = methodMap.get(mName);
					if (mTerms == null) {
						mTerms = new HashSet<MethodNameTerm>();
						methodMap.put(mName, mTerms);
					}
					mTerms.add(mTerm);
				}
			}
		}
	}
	
	public static void parameterizeTypeUsage(List<Node> templateList, Map<String, Set<TypeNameTerm>> typeMap, 
			Map<String, String> concreteToAbstract,
			Set<String> packageNames, DefUseChangeFactGroup g) {
		Enumeration<Node> nEnum = null;
		Node temp = null;
		TypeNameTerm tTerm = null;
		Set<TypeNameTerm> tTerms = null; 
		
		for (Node n : templateList) {
			nEnum = n.breadthFirstEnumeration();
			while (nEnum.hasMoreElements()) {
				temp = nEnum.nextElement();
				if (!temp.isLeaf())
					continue;
				Object obj = temp.getUserObject();
				if (obj instanceof TypeNameTerm) {
					tTerm = (TypeNameTerm)obj;
					String tName = tTerm.getTypeName();
					String typeParam = null;
					tTerms = typeMap.get(tName);
					if (tTerms == null) {
						tTerms = new HashSet<TypeNameTerm>();
						typeMap.put(tName, tTerms);
					}					
					tTerms.add(tTerm);						
					if (TypeAPIResolver.isInPackage(tTerm.getQualifiedName(), packageNames)) {
						String typeQualifier = null;						
						ITypeBinding tBinding = tTerm.getTypeBinding();
						Set<ITypeBinding> sBindings = new HashSet<ITypeBinding>();
						sBindings.add(tBinding.getSuperclass());
						for (ITypeBinding b : tBinding.getInterfaces()) {
							sBindings.add(b);
						}
						for (ITypeBinding b : sBindings) {
							if (b == null) {
								continue;
							}
							String qName = b.getQualifiedName();
							if (!TypeAPIResolver.isInPackage(qName, packageNames)) {
								typeQualifier = b.getName();
								tTerms = typeMap.get(typeQualifier);
								if (tTerms == null) {
									tTerms = new HashSet<TypeNameTerm>();
									typeMap.put(typeQualifier, tTerms);
								}
								tTerm = new TypeNameTerm(-1, typeQualifier, b.getQualifiedName());
								tTerm.setTypeBinding(b);
								tTerms.add(tTerm);
								
								break;
							}
						}
						if (typeQualifier != null) {							
							typeParam = g.getNextUnifiedTIdentifier() + "_" + typeQualifier;							
						}  else {
							typeParam = g.getNextUnifiedTIdentifier();
						}
						concreteToAbstract.put(tName, typeParam);
						tTerm.setAbstractName(typeParam);
					}
				}
			}
		}
	}
}
