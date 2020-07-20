package edu.vt.cs.changes.api;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PPADefaultBindingResolver;

import partial.code.grapa.dependency.graph.DataFlowAnalysisEngine;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.FieldReference;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.SourceRange;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.Delete;
import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import edu.vt.cs.append.FineChangesInMethod;
import edu.vt.cs.append.JavaExpressionConverter;
import edu.vt.cs.append.TopDownTreeMatcher;
import edu.vt.cs.append.terms.MethodNameTerm;
import edu.vt.cs.append.terms.TypeNameTerm;
import edu.vt.cs.changes.MigrationChangeDetector;
import edu.vt.cs.changes.api.DefUseChangeFact.DATA_CHANGE_TYPE;
import edu.vt.cs.changes.api.refchanges.SubChangeFact;
import edu.vt.cs.changes.api.refchanges.ClassChangeFact;
import edu.vt.cs.changes.api.refchanges.ClassRefChangeFact;
import edu.vt.cs.changes.api.refchanges.FieldChangeFact;
import edu.vt.cs.changes.api.refchanges.FieldRefChangeFact;
import edu.vt.cs.changes.api.refchanges.MethodRefChangeFact;
import edu.vt.cs.changes.api.refchanges.SubChangeFact.CHANGE_TYPE;
import edu.vt.cs.diffparser.util.ASTNodeFinder;
import edu.vt.cs.diffparser.util.SourceCodeRange;

import java.lang.reflect.*;


public class DefUseChangeDetector {

	JavaExpressionConverter lConverter;
	JavaExpressionConverter rConverter;
	DataFlowAnalysisEngine lEngine;
	DataFlowAnalysisEngine rEngine;
	Map<String, String> renamedVars = null;
	Set<String> packageNames = null;
	
	public DefUseChangeDetector() {
		this.lConverter = new JavaExpressionConverter();
		this.rConverter = new JavaExpressionConverter();
	}	
	
	public List<List<DefUseChangeFact>> detect(CompilationUnit oldCu, CompilationUnit newCu, GraphConvertor2 oc, 
			GraphConvertor2 nc, SourceCodeChange change, APIResolver callback, CDGraphConvertor CD_oc, CDGraphConvertor CD_nc) {	
		oc.g.reConstruct(DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
		nc.g.reConstruct(DataDependenceOptions.FULL, ControlDependenceOptions.NONE);
		lEngine = callback.leftEngine;
		rEngine = callback.rightEngine;
		List<List<DefUseChangeFact>> result = new ArrayList<List<DefUseChangeFact>>();
		List<DefUseChangeFact> temp = null;
		FineChangesInMethod allChanges = (FineChangesInMethod)change;	
		renamedVars = new HashMap<String, String>();
		SourceCodeRange oldR = null, newR = null;
		String qName = TypeAPIResolver.convertToQName(oc.getClassName());
		packageNames = TypeAPIResolver.getPackageNames(qName);
		Node tree1 = null, tree2 = null;
		int countnum = -1;
		for (SourceCodeChange c : allChanges.getChanges()) {
			countnum++;
			temp = new ArrayList<DefUseChangeFact>();
			result.add(temp);
			if (c instanceof Update) {
				Update u = (Update)c;
				oldR = SourceCodeRange.convert(u.getChangedEntity());
				newR = SourceCodeRange.convert(u.getNewEntity());
//				if (u.getChangedEntity().getType() == JavaEntityType.VARIABLE_DECLARATION_STATEMENT) {
//					oldR
//				}

				tree1 = getExpressionTree(oldCu, oldR, lConverter);
				tree2 = getExpressionTree(newCu, newR, rConverter);
				TopDownTreeMatcher matcher = new TopDownTreeMatcher();
//				System.out.print("");
				matcher.match(tree1, tree2);				
				Map<Node, Node> unmatchedLeftToRight = matcher.getUnmatchedLeftToRight();	
				if (unmatchedLeftToRight.isEmpty()) {//this is rename change
					UpgradeFact tmpFact = new UpgradeFact(tree1, tree2);
					Set<String> added = tmpFact.addedParameters.keySet();
					Set<String> removed = tmpFact.removedParameters.keySet();
					if (added.size() == 1 && removed.size() == 1) {
						renamedVars.put(removed.iterator().next(), added.iterator().next());
					}
				}
				for (Entry<Node, Node> entry2 : unmatchedLeftToRight.entrySet()) {
					Node lNode = entry2.getKey();
					Node rNode = entry2.getValue();
					System.out.print("");
					//added by shengzhe July, 2017
					if (lNode==null || rNode == null) {
						continue;
					}
					while (lNode.getParent() != null 
							&& (((Node)lNode.getParent()).getLabel() == JavaEntityType.VARIABLE_DECLARATION_STATEMENT
							|| ((Node)lNode.getParent()).getLabel() == JavaEntityType.VARIABLE_DECLARATION_FRAGMENT)) {
						lNode = (Node)lNode.getParent();
					}
					while (rNode.getParent() != null
							&& (((Node)rNode.getParent()).getLabel() == JavaEntityType.VARIABLE_DECLARATION_STATEMENT
							|| ((Node)rNode.getParent()).getLabel() == JavaEntityType.VARIABLE_DECLARATION_FRAGMENT)) {
						rNode = (Node)rNode.getParent();
					}
					
					UpgradeFact f = new UpgradeFact(lNode, rNode);
					temp.add(f);
					//2. infer insert def/use						
					SourceRange sr = rNode.getEntity().getSourceRange();
					List<Integer> lines = this.getLines(newCu, new SourceCodeRange(sr.getStart(), sr.getEnd()));
					f.setTemplateRightwithlines(rNode, lines, nc);
//					System.out.println(countnum + ":update:" + lines);
					if (!f.addedParameters.isEmpty()) {										   
						inferDefUse(f.addedParameters, f.addedDefs, f.addedUses, lines, nc,
								lEngine, f.knownNewInsns, f);
					}
					
					//added by shengzhe
					//3. infer control dep
					inferConDep(lines, CD_nc, f, true);
					
					sr = lNode.getEntity().getSourceRange();
					lines = this.getLines(oldCu, new SourceCodeRange(sr.getStart(), sr.getEnd()));	
					f.setTemplateLeftwithlines(lNode, lines, oc);
					if (!f.removedParameters.isEmpty()) {
					   inferDefUse(f.removedParameters, f.removedDefs, f.removedUses, lines, oc,
							   rEngine, f.knownOldInsns, f);
					}	
					
					//added by shengzhe
					//3. infer control dep
					inferConDep(lines, CD_oc, f, false);
					
					if (!f.typeMap.isEmpty()) {
						processTypes(f, lEngine);
						processTypes(f, rEngine);						
					} 						
					if (!f.methodMap.isEmpty()) {
						processMethods(f, lEngine);
						processMethods(f, rEngine);						
					}						
				}				
			} else if (c instanceof Insert) {
				Insert ins = (Insert)c;
				newR = SourceCodeRange.convert(ins.getChangedEntity());
				tree2 = getExpressionTree(newCu, newR, rConverter);				
				List<Integer> lines = this.getLines(newCu, newR);
//				System.out.println(countnum + ":insert:" + lines);
//				newCu.getAST().
//				System.out.println(newCu.getAST().resolveWellKnownType("t"));
//				try {
//					Field fi = AST.class.getDeclaredField("resolver");
//					fi.setAccessible(true);
//					PPADefaultBindingResolver value = (PPADefaultBindingResolver) fi.get(newCu.getAST());
//					System.out.println();
//				} catch (NoSuchFieldException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (SecurityException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IllegalArgumentException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IllegalAccessException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				InsertFact f = new InsertFact(tree2);
				f.setTemplatewithlines(tree2, lines, nc);
				
				inferDefUse(f.addedParameters, f.addedDefs, f.addedUses, lines, nc, 
						lEngine, f.knownNewInsns, f);
				inferConDep(lines, CD_nc, f, true);
				if (!f.typeMap.isEmpty()) {
					processTypes(f, lEngine);					
				}
				if (!f.methodMap.isEmpty()) {
					processMethods(f, lEngine);
				}
				temp.add(f);
			} else if (c instanceof Delete) {
				Delete del = (Delete)c;
				oldR = SourceCodeRange.convert(del.getChangedEntity());
				tree1 = getExpressionTree(oldCu, oldR, lConverter);
				List<Integer> lines = this.getLines(oldCu, oldR);
//				System.out.println(countnum + ":delete:" + lines);
				DeleteFact f = new DeleteFact(tree1);
				f.setTemplatewithlines(tree1, lines, oc);
				inferDefUse(f.removedParameters, f.removedDefs, f.removedUses, lines, oc,
						rEngine, f.knownOldInsns, f);
				inferConDep(lines, CD_oc, f, false);
				if (!f.typeMap.isEmpty()) {
					processTypes(f, rEngine);
				}
				if (!f.methodMap.isEmpty()) {
					processMethods(f, rEngine);
				}
				temp.add(f);
			}			
		}				
		return result;
	}
	
//	private Node getAPI(Node n) {
//		Enumeration<Node> nEnum = n.breadthFirstEnumeration();
//		Node tmp = null;
//		while (nEnum.hasMoreElements()) {
//			tmp = nEnum.nextElement();
//			if (tmp.getLabel().equals(JavaEntityType.METHOD)) {
//				return tmp;
//			}
//		}
//		return null;
//	}
	
	private Node getExpressionTree(CompilationUnit cu, SourceCodeRange r, JavaExpressionConverter converter) {
		ASTNodeFinder finder = new ASTNodeFinder();
		ASTNode n = finder.lookforASTNode(cu, r);
		converter.clear();
		n.accept(converter);
		Node result = converter.getRoot();
//		Map<Integer, Integer> interm = new HashMap();
//		for (Integer x : converter.local_variable_decid.keySet()) {
//			Integer y = cu.getLineNumber(x);
//			interm.put(y, converter.local_variable_decid.get(x));
//		}
		result.setpostid(converter.local_variable_decid);
//		System.out.println("mapmap:" + interm);
		JavaExpressionConverter.markSubStmts(result, n);
		return result;
	}
	
	private List<Integer> getLines(CompilationUnit cu, SourceCodeRange r) {
		List<Integer> lines = new ArrayList<Integer>();
		int startLine = cu.getLineNumber(r.startPosition);
		int endLine = cu.getLineNumber(r.startPosition + r.length - 1);
		for (int i = startLine; i <= endLine; i++) {
			lines.add(i);
		}
		return lines;
	}	
	
	//added by shengzhe Aug Week 1, 2017
	private void inferConDep(List<Integer>lines, CDGraphConvertor c, DefUseChangeFact fact, boolean direction) {
		SSAInstruction[] insns = c.getInstructions();
		SSAInstruction insn = null;
		for (Integer lineNum : lines) {
			List<Integer> indexes = c.getInstIndexes(lineNum);
			for (Integer idx : indexes) {
				List<Integer> slave_lines = c.getControls(idx);
				fact.addControlLtoL(idx, slave_lines, direction);
			}
		}
//		System.out.println(fact.getControlLtoL(direction));
	}
	
	private void inferDefUse(Map<String, DATA_CHANGE_TYPE> parameters, Map<String, Integer> defParam, 
			Map<String, Integer> useParam, List<Integer> lines,
			GraphConvertor2 c, DataFlowAnalysisEngine otherEngine, 
			Set<Integer> knownInsns, DefUseChangeFact fact) {
		  SSAInstruction[] insns = c.getInstructions();
		  SSAInstruction insn = null;		  
		  for (Integer lineNum : lines) {
			  List<Integer> indexes = c.getInstIndexes(lineNum);
			  knownInsns.addAll(indexes);
			  for (Integer idx : indexes) {
				  // for local variables
				 List<String> defs = c.getDefs(idx);
				 for (String def : defs) {													
					 if (parameters.containsKey(def)) {
						 parameters.put(def, DATA_CHANGE_TYPE.DEF);		
						 defParam.put(def, idx);
					 }
				 }
				 List<String> uses = c.getUses(idx);												
				 for (String use : uses) {
					 ASTNode x = c.cu.findDeclaringNode(use);
					 if (parameters.containsKey(use)) {
						 if (parameters.get(use) == null) {
							 parameters.put(use, DATA_CHANGE_TYPE.USE);
							 useParam.put(use, idx);
						 }
					 }
				 }				 
			  }
		  }	
		  int totalNum = parameters.size();
		  int count = 0;
		  for (Entry<String, DATA_CHANGE_TYPE> pEntry : parameters.entrySet()) {
			  if (pEntry.getValue() != null) {
				  count++;
			  }
		  }
//		  System.out.print("");
		  FieldReference fRef = null;
		  DATA_CHANGE_TYPE type = null;
		  String name = null;
		  Map<String, Integer> paramMap = null;
		  if (totalNum > count) {
			  for (Integer lineNum : lines) {		
				  List<Integer> indexes = c.getInstIndexes(lineNum);											 
				  for (Integer idx : indexes) {  										  
					  // for fields
					  //dec 11 bug fix
					 if (idx >= insns.length) continue;
					 insn = insns[idx];		
					 if (insn instanceof SSAGetInstruction || insn instanceof SSAPutInstruction) {						 
						 if (insn instanceof SSAGetInstruction) {
							 SSAGetInstruction get = (SSAGetInstruction)insn;
							 fRef = get.getDeclaredField();
							 type = DATA_CHANGE_TYPE.USE;
							 paramMap = useParam;
						 } else {
							 SSAPutInstruction put = (SSAPutInstruction)insn;
							 fRef = put.getDeclaredField();
							 type = DATA_CHANGE_TYPE.DEF;
							 paramMap = defParam;
						 }						 
						 String fieldName = fRef.getName().toString();
						 String className = getClassName(fRef);	
						 name = getFieldName(parameters.keySet(), fieldName, className);
						 if (name != null) {
							 String qClassName = fRef.getDeclaringClass().getName().toString();//class name
							 IClassHierarchy cha = otherEngine.getClassHierarchy();								
							 IField ifield = cha.resolveField(fRef);
							 if (qClassName.equals(c.getClassName())) { //this is a field defined in the owner class								 
								 if (ifield == null) {
									 FieldChangeFact fcf = new FieldChangeFact();									 
									 fcf.inSameClass = true;
									 fcf.fRef = fRef;
									 if (otherEngine.equals(lEngine)) {
										 fcf.ct = CHANGE_TYPE.ADD;
									 } else {
										 fcf.ct = CHANGE_TYPE.DELETE;
									 }
									 fact.addFieldChangeFact(name, fcf);
									 fact.removeParameter(name);
								 }
							 } else {
								 String packageName = fRef.getDeclaringClass().getName().getPackage().toString();
								 packageName = packageName.replaceAll("/", ".");
								 if (packageName.startsWith(MigrationChangeDetector.libStr)) {
									 FieldRefChangeFact frcf = new FieldRefChangeFact();
									 frcf.fRef = fRef;
									 if (otherEngine.equals(lEngine)) {
										 frcf.ct = CHANGE_TYPE.ADD;
									 } else {
										 frcf.ct = CHANGE_TYPE.DELETE;
									 }
									 fact.addFieldRefChangeFact(name, frcf);
									 fact.removeParameter(name); 
								 }								 
							 }
//							 parameters.put(name, type);
							 count++;
//							 paramMap.put(name, idx);
						 }						 
						 fact.addField(name, fRef);
					 }
				  }
			  }			 
		  }		  		  
	}
	
	private String getFieldName(Set<String> parameters, String fieldName, String className) {
		Set<String> names = FieldAPIResolver.getPossibleFieldNames(fieldName, className);
		names.retainAll(parameters);
		if (names.isEmpty()) {
			return null;
		} else {
			return names.iterator().next();
		}
	}

	private String getClassName(FieldReference fRef) {
		String className = fRef.getDeclaringClass().getName().getClassName().toString();
		className = className.replaceAll("\\$", ".");
		return className;
	}
	
	public Map<String, String> getRenamedVars() {
		return renamedVars;
	}

	public void processMethods(DefUseChangeFact fact, DataFlowAnalysisEngine otherEngine) {
		Set<MethodNameTerm> mTerms = null;
		String qName = null;
		String typeQualifier = null;
		Set<String> keysToRemove = new HashSet<String>();
		Map<String, Set<MethodNameTerm>> methodMap = fact.methodMap;		
		for (Entry<String, Set<MethodNameTerm>> entry : methodMap.entrySet()) {
			String key = entry.getKey();
			mTerms = entry.getValue();
			for (MethodNameTerm mTerm : mTerms) {
				IMethodBinding binding = mTerm.getMethodBinding();				
				ITypeBinding tBinding = binding.getDeclaringClass();
				qName = tBinding.getQualifiedName();
				if (!TypeAPIResolver.isInPackage(qName, packageNames)) {					
					checkMRefChange(fact, otherEngine, tBinding, binding, binding.getName());
					keysToRemove.add(key);
				}
			}
		}
		for (String key : keysToRemove) {
			fact.removeMethodParameter(key);
		}
	}
	
	public void processTypes(DefUseChangeFact fact, DataFlowAnalysisEngine otherEngine) {
		Set<TypeNameTerm> tTerms = null;
		String qName = null;
		String typeQualifier = null;
		Set<String> keysToRemove = new HashSet<String>();
		Map<String, Set<TypeNameTerm>> typeMap = fact.typeMap;		
		for(Entry<String, Set<TypeNameTerm>> entry : typeMap.entrySet()) {
			String key = entry.getKey();
			tTerms = entry.getValue();
			for (TypeNameTerm tTerm : tTerms) {
				ITypeBinding binding = tTerm.getTypeBinding();
				qName = binding.getQualifiedName();
				if (TypeAPIResolver.isInPackage(qName, packageNames)) {
					Set<ITypeBinding> sBindings = new HashSet<ITypeBinding>();
					sBindings.add(binding.getSuperclass());
					for (ITypeBinding b : binding.getInterfaces()) {
						sBindings.add(b);
					}
					for (ITypeBinding b : sBindings) {
						if (b == null) continue;
						qName = b.getQualifiedName();
						if (!TypeAPIResolver.isInPackage(qName, packageNames)) {
							checkCRefChange(fact, otherEngine, b, b.getName());							
							typeQualifier = b.getName();
							break;							
						}
					}
					if (typeQualifier != null) {		
						String typeParam = fact.concreteToAbstractType.get(key);
						fact.replaceTypeIdentifier(typeParam, typeParam + "_" + typeQualifier);
					}
					ClassChangeFact cFact = new ClassChangeFact();					
					if (otherEngine.equals(lEngine)) {
						cFact.ct = CHANGE_TYPE.ADD;
					} else {
						cFact.ct = CHANGE_TYPE.DELETE;
					}
					cFact.tBinding = binding;
					fact.addClassChangeFact(key, cFact);
					keysToRemove.add(key);
				} else {
					checkCRefChange(fact, otherEngine, binding, binding.getName());
					keysToRemove.add(key);
				}
			}
		}
		for (String key : keysToRemove) {
			fact.removeTypeParameter(key);
		}		
	}
	private void checkCRefChange(DefUseChangeFact fact, DataFlowAnalysisEngine otherEngine, ITypeBinding b, String className) {
		ClassRefChangeFact cFact = new ClassRefChangeFact();
		cFact.isLib = true;
		cFact.binding = b;
		IClass iclass = otherEngine.getClass(b);
		if (iclass == null) {
			if (otherEngine.equals(lEngine)) {
				cFact.ct = CHANGE_TYPE.ADD;
			} else{
				cFact.ct = CHANGE_TYPE.DELETE;
			}
			fact.addClassRefChangeFact(className, cFact);
			Set<TypeNameTerm> tTerms = fact.typeMap.get(className);
			if (tTerms == null) {
				tTerms = new HashSet<TypeNameTerm>();			
				fact.typeMap.put(className, tTerms);
			}
			TypeNameTerm tTerm = new TypeNameTerm(-1, className, b.getQualifiedName());
			tTerm.setTypeBinding(b);
			tTerms.add(tTerm);
		} 
	}

	private void checkMRefChange(DefUseChangeFact fact, DataFlowAnalysisEngine otherEngine, ITypeBinding tb, IMethodBinding b, String methodName) {
		String className = tb.getName();
		IClass iclass = otherEngine.getClass(tb);
		if (iclass == null) {			
			checkCRefChange(fact, otherEngine, tb, className);
			return;
		}
		IMethod imethod = otherEngine.getMethod(iclass, b);
		if (imethod == null) {
			MethodRefChangeFact mFact = new MethodRefChangeFact();
			mFact.isLib = true;
			mFact.binding = b;
			if (otherEngine.equals(lEngine)) {
				mFact.ct = CHANGE_TYPE.ADD;
			} else{
				mFact.ct = CHANGE_TYPE.DELETE;
			}
			fact.addMethodRefChangeFact(methodName, mFact);			
		}
	}
}
