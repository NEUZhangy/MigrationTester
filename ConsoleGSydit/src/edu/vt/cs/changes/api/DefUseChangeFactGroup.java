package edu.vt.cs.changes.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.stringtemplate.v4.ST;

import com.ibm.wala.types.FieldReference;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.EntityType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SourceRange;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import edu.vt.cs.append.CommonValue;
import edu.vt.cs.append.DatabaseControl;
import edu.vt.cs.append.FineChangesInMethod;
import edu.vt.cs.append.JavaExpressionConverter;
import edu.vt.cs.append.TopDownTreeMatcher;
import edu.vt.cs.append.terms.MethodNameTerm;
import edu.vt.cs.append.terms.TypeNameTerm;
import edu.vt.cs.changes.ChangeFact;
import edu.vt.cs.changes.MigrationChangeDetector;
import edu.vt.cs.changes.api.DefUseChangeFact.DATA_CHANGE_TYPE;
import edu.vt.cs.changes.api.refchanges.ClassChangeFact;
import edu.vt.cs.changes.api.refchanges.FieldChangeFact;
import edu.vt.cs.changes.api.refchanges.SubChangeFact.CHANGE_TYPE;
import edu.vt.cs.diffparser.util.ASTNodeFinder;
import edu.vt.cs.diffparser.util.PatternMatcher;
import edu.vt.cs.diffparser.util.SourceCodeRange;
import edu.vt.cs.graph.ClientClass;

public class DefUseChangeFactGroup {

	public CHANGE_TYPE ct;
	public static final String ABSTRACT_V = "V_";	
	public static final String ABSTRACT_U = "U_";
	public static final String ABSTRACT_M = "M_";
	public static final String ABSTRACT_T = "T_";
	public static final String ABSTRACT_F = "F_";
	public static final String ABSTRACT_C = "C_";
	Map<Integer, DefUseChangeFact> facts = null;
	String oldTemplate = null;
	String newTemplate = null;
	String oldOriTemplate = null;
	String newOriTemplate = null;
	List<IMethodBinding> oldAPIs = null;
	List<IMethodBinding> newAPIs = null;
	List<Integer> oldOrderedFacts = null;
	List<Integer> newOrderedFacts = null;
	List<Integer> oldOrderedRefinedFacts = null;
	List<Integer> newOrderedRefinedFacts = null;
	Map<SourceCodeRange, Integer> oldRanges = null;  // map from a source code range to a fact index
	Map<SourceCodeRange, Integer> newRanges = null;
	Map<Integer, SourceCodeRange> oldBackRan = null; // map from a fact index to a source code range
	Map<Integer, SourceCodeRange> newBackRan = null;
	Map<String, String> old_api_import = null;
	Map<String, String> new_api_import = null;
	
	List<Node> templateLeftList = null;
	List<Node> templateRightList = null;
	Map<String, String> concreteToUnified = null;	
	Map<String, FieldChangeFact> changedFields = null;
	Map<String, ClassChangeFact> changedClasses = null;
	Map<String, Set<FieldReference>> fieldMap = null;
	Map<String, Set<TypeNameTerm>> typeMap = null;
	Map<String, Set<MethodNameTerm>> methodMap = null;
	Map<String, String> concreteToUnifiedMethods = null;
	Map<String, String> concreteToUnifiedTypes = null;
	int uIdx = 0;
	int vIdx = 0;
	int tIdx = 0;
	int mIdx = 0;
	int cIdx = 0;
	// added for helping remove the common prefix nodes of top-down tree match
	Node pattern_start_node_left = null;
	Node pattern_start_node_right = null;
	
	Map<String, DATA_CHANGE_TYPE> addedParameters = null;
	Map<String, DATA_CHANGE_TYPE> deletedParameters = null;	
	Map<String, String> renamedVars = null;
	ChangeFact callbackCF = null;
	boolean lv3map[] = null;
	boolean lv3label = false;
	
	public boolean purecheck(String totest) {
		int len = totest.length();
		for (int i=0;i<len;i++) {
			if ('a' <= totest.charAt(i) && totest.charAt(i) <= 'z') return true;
			if ('A' <= totest.charAt(i) && totest.charAt(i) <= 'Z'
				&& totest.charAt(i) != 'U') return true;
		}
		return false;
	}
	
	public DefUseChangeFactGroup(ChangeFact cf,
			Set<Integer> group, List<List<DefUseChangeFact>> factLists, 
			List<SourceCodeChange> changes, Map<String, String> renamedVars, Map<SourceCodeEntity, SourceCodeChange> entityChanges, 
			CompilationUnit oldCu, CompilationUnit newCu, String oldClassName, String newClassName, boolean[] lv3map) {
		callbackCF = cf;
		this.lv3map = lv3map;
		if (group.size() == 1 && factLists.get(group.iterator().next()).isEmpty())
			return;
		concreteToUnified = new HashMap<String, String>();
		concreteToUnifiedMethods = new HashMap<String, String>();
		concreteToUnifiedTypes = new HashMap<String, String>();
		
		facts = new HashMap<Integer, DefUseChangeFact>();
		this.renamedVars = renamedVars;
//		String oldStr = null;
//		String newStr = null;
		Node templateLeft = null;
		Node templateRight = null;
		DefUseChangeFact fact = null;		
		Map<Integer, String> oldAPICalls = new HashMap<Integer, String>();
		Map<Integer, String> newAPICalls = new HashMap<Integer, String>();
		Map<Integer, String> oldOriCodes = new HashMap<Integer, String>();
		Map<Integer, String> newOriCodes = new HashMap<Integer, String>();
		oldRanges = new HashMap<SourceCodeRange, Integer>();
		newRanges = new HashMap<SourceCodeRange, Integer>();
		oldBackRan = new HashMap<Integer, SourceCodeRange>();
		newBackRan = new HashMap<Integer, SourceCodeRange>();
		old_api_import = new HashMap<String, String>();
		new_api_import = new HashMap<String, String>();
		templateLeftList = new ArrayList<Node>();
		templateRightList = new ArrayList<Node>();
		Map<Integer, Node> leftTemplateMap = new HashMap<Integer, Node>();
		Map<Integer, Node> rightTemplateMap = new HashMap<Integer, Node>();
		JavaExpressionConverter lConverter = new JavaExpressionConverter();
		JavaExpressionConverter rConverter = new JavaExpressionConverter();
		
		List<Integer> sortedFacts = new ArrayList<Integer>(group);
		for (int index = 0; index < sortedFacts.size(); index++) {
			int factIndex = sortedFacts.get(index);
			List<DefUseChangeFact> facts2 = factLists.get(factIndex);
			if (facts2.isEmpty())
				continue;
			fact = factLists.get(factIndex).get(0);			
			facts.put(factIndex, fact);
//			oldStr = newStr = null;
			templateLeft = templateRight = null;
			
			SourceCodeChange change = changes.get(factIndex);
			if (fact instanceof InsertFact) {
				InsertFact iFact = (InsertFact)fact;
//				newStr = iFact.template;	
				templateRight = iFact.templateTree;
				rightTemplateMap.put(factIndex, templateRight);
				newRanges.put(SourceCodeRange.convert(change.getChangedEntity()), factIndex);	
				newBackRan.put(factIndex, SourceCodeRange.convert(change.getChangedEntity()));
			} else if (fact instanceof DeleteFact) {				
				DeleteFact dFact = (DeleteFact)fact;
//				oldStr = dFact.template;			
				templateLeft = dFact.templateTree;
				leftTemplateMap.put(factIndex, templateLeft);
				oldRanges.put(SourceCodeRange.convert(change.getChangedEntity()), factIndex);	
				oldBackRan.put(factIndex, SourceCodeRange.convert(change.getChangedEntity()));
			} else {
				UpgradeFact uFact = (UpgradeFact)fact;				
//				oldStr = uFact.oldAPICall;				
				templateLeft = uFact.templateLeft;
				leftTemplateMap.put(factIndex, templateLeft);
				oldRanges.put(SourceCodeRange.convert(change.getChangedEntity()), factIndex);
				oldBackRan.put(factIndex, SourceCodeRange.convert(change.getChangedEntity()));
//				newStr = uFact.newAPICall;		
				templateRight = uFact.templateRight;
				rightTemplateMap.put(factIndex, templateRight);
				newRanges.put(SourceCodeRange.convert(((Update)change).getNewEntity()), factIndex);
				newBackRan.put(factIndex, SourceCodeRange.convert(((Update)change).getNewEntity()));
			}
			Map<String, String> abstractToUnified = new HashMap<String, String>();			
			unifyIdentifiers(fact.concreteToAbstract, concreteToUnified, abstractToUnified, ABSTRACT_V);
			unifyIdentifiers(fact.qNameCToA, concreteToUnified, abstractToUnified, ABSTRACT_U);
			unifyIdentifiers(fact.concreteToAbstractMethod, concreteToUnifiedMethods, abstractToUnified, ABSTRACT_M);
			unifyIdentifiers(fact.concreteToAbstractType, concreteToUnifiedTypes, abstractToUnified, ABSTRACT_T);
//			unifyIdentifiers(fact.concreteToAbstractType, concreteToUnifiedTypes, abstractToUnified, ABSTRACT_C);
			
			TopDownTreeMatcher matcher = new TopDownTreeMatcher();
	        matcher.match_pattern_tree(templateLeft, templateRight);	        
	        if (matcher.left_pattern_original_node != null) {
	        	templateLeft = matcher.left_pattern_original_node;	        
	        }
	        if (matcher.right_pattern_original_node != null) {
	        	templateRight = matcher.right_pattern_original_node;
	        }	        
	        String left_pre_output = null, right_pre_output = null;	    
			if (templateLeft != null) {
				oldOriCodes.put(factIndex, templateLeft.toString());
				DefUseChangeFact.replaceIdentifiers(templateLeft, abstractToUnified);	
//				fact.compare_two_patterns(templateLeft, templateRight);				
				left_pre_output = fact.renderWithTypeInfo(templateLeft, old_api_import);
				//only consider simple suffix like API1.m_1() to API2.m_1()
				//don't simplyfy the complex shape like API1.m_1(V_1) etc.
			}
			if (templateRight != null) {
				newOriCodes.put(factIndex, templateRight.toString());
				DefUseChangeFact.replaceIdentifiers(templateRight, abstractToUnified);
//				fact.compare_two_patterns(templateLeft, templateRight);
				right_pre_output = fact.renderWithTypeInfo(templateRight, new_api_import);			
			}
			if (left_pre_output != null && right_pre_output != null) {
				String[] leftdots = left_pre_output.split("[.]");
				String[] rightdots = right_pre_output.split("[.]");
				int label = 0;
				if (leftdots.length>0 && rightdots.length>0) {
					String cp_left = leftdots[leftdots.length-1];
					String cp_right = rightdots[rightdots.length-1];
					if (cp_left.length() == cp_right.length()) {
						label = 1;
						if (cp_left.endsWith(";") && cp_right.endsWith(";")) {
							cp_left = cp_left.substring(0, cp_left.length()-1);
							cp_right = cp_right.substring(0, cp_right.length()-1);						
						}
						if (cp_left.endsWith("()") && cp_right.endsWith("()")) {
							cp_left = cp_left.substring(0, cp_left.length()-2);
							cp_right = cp_right.substring(0, cp_right.length()-2);		
							label = 2;
						}	
						for (int i = 0; i<cp_left.length();i++) {
							if (cp_left.charAt(i) != cp_right.charAt(i)
									|| cp_left.charAt(i)=='(' || cp_right.charAt(i)=='(') {
								label = 0;
							}
						}
					}					
				}
				if (label>0) {
					String new_left = new String(leftdots[0]);
					for (int i=1;i<leftdots.length-1;i++) new_left = new_left + "." + leftdots[i];
					String new_right = new String(rightdots[0]);
					for (int i=1;i<rightdots.length-1;i++) new_right = new_right + "." + rightdots[i];
					left_pre_output = new_left + ".";
					right_pre_output = new_right + ".";
					if (label == 1) {
						left_pre_output += "u_field";
						right_pre_output += "u_field";
					}
					else {
						left_pre_output += "u_method()";
						right_pre_output += "u_method()";
					}
				}
			}				
			if (templateLeft != null) {
				oldAPICalls.put(factIndex, left_pre_output);
			}
			if (templateRight != null) {
				newAPICalls.put(factIndex, right_pre_output);
			}
//			System.out.println();
		}

		oldOrderedFacts = sortFacts(oldRanges, sortedFacts);
		// oldexp means the indexs that should be removed from the old facts. 
		Set<Integer> oldexp = new HashSet<Integer>();
		Set<Integer> newexp = new HashSet<Integer>();		
		newOrderedFacts = sortFacts(newRanges, sortedFacts);

//		System.out.println("oldRan" + oldBackRan);
//		System.out.println("newRan" + newBackRan);
		
		// waiting for explanation next time ???
		for (Integer indexo : oldOrderedFacts) {
			for (Integer indexn : newOrderedFacts) {
				if (indexo == indexn) {
					continue;
				}
				List<DefUseChangeFact> facts2 = factLists.get(indexo);
				if (facts2.isEmpty())
					continue;
				DefUseChangeFact facto = factLists.get(indexo).get(0);	
				SourceCodeRange leftran = oldBackRan.get(indexo);
				Node tree1 = getExpressionTree(oldCu, leftran, lConverter);
				
				facts2 = factLists.get(indexn);
				if (facts2.isEmpty())
					continue;
				DefUseChangeFact factn = factLists.get(indexn).get(0);	
				SourceCodeRange rightran = newBackRan.get(indexn);
				Node tree2 = getExpressionTree(newCu, rightran, rConverter);
				TopDownTreeMatcher matcher = new TopDownTreeMatcher();
				matcher.match_filter(tree1, tree2);
				Map<Node, Node> unmatchedLeftToRight = matcher
						.getUnmatchedLeftToRight();
				if (unmatchedLeftToRight.isEmpty()) {
					System.out.println("chongfu!");
//					oldexp.add(indexo);
//					newexp.add(indexn);
				}
			}
		}
		
		for (Integer indexo1 : oldOrderedFacts) {
			for (Integer indexo2 : oldOrderedFacts) {
				if (oldexp.contains(indexo1) || oldexp.contains(indexo2) || indexo1 == indexo2) {
					continue;
				}
				List<DefUseChangeFact> facts2 = factLists.get(indexo1);
				if (facts2.isEmpty())
					continue;
				DefUseChangeFact facto1 = factLists.get(indexo1).get(0);	
				SourceCodeRange leftran = oldBackRan.get(indexo1);
				
				facts2 = factLists.get(indexo2);
				if (facts2.isEmpty())
					continue;
				DefUseChangeFact facto2 = factLists.get(indexo2).get(0);	
				SourceCodeRange rightran = oldBackRan.get(indexo2);
								
				if (leftran.startPosition <= rightran.startPosition
					&& rightran.startPosition+rightran.length <= leftran.startPosition+leftran.length
					&& changes.get(indexo1).getChangedEntity().getType()==JavaEntityType.ELSE_STATEMENT) {
//					System.out.println("obaohan!"+ indexo1 + "->" + indexo2);
//					oldexp.add(indexo2);
				}
			}
		}
		for (Integer indexn1 : newOrderedFacts) {
			for (Integer indexn2 : newOrderedFacts) {
				if (newexp.contains(indexn1) || newexp.contains(indexn2) || indexn1 == indexn2) {
					continue;
				}
				List<DefUseChangeFact> facts2 = factLists.get(indexn1);
				if (facts2.isEmpty())
					continue;
				DefUseChangeFact factn1 = factLists.get(indexn1).get(0);	
				SourceCodeRange leftran = newBackRan.get(indexn1);
				
				facts2 = factLists.get(indexn2);
				if (facts2.isEmpty())
					continue;
				DefUseChangeFact factn2 = factLists.get(indexn2).get(0);	
				SourceCodeRange rightran = newBackRan.get(indexn2);
				
				if (leftran.startPosition <= rightran.startPosition
					&& rightran.length <= leftran.length) {
//					System.out.println("nbaohan!" + indexn1 + "->" + indexn2);
//					newexp.add(indexn2);
				}
			}
		}

		System.out.println(oldRanges);
		System.out.println(newRanges);
		lv3label = false;
		if (!oldRanges.isEmpty()) {
			oldOrderedFacts = sortFacts(oldRanges, sortedFacts);
			StringBuffer oldCode = new StringBuffer();
			StringBuffer oldOri = new StringBuffer();
			for (Integer orderIndex : oldOrderedFacts) {
				if (oldexp.contains(orderIndex)) {
					continue;
				}
				if (this.lv3map[orderIndex] == true) {
					lv3label = true;
				}
				if (changes.get(orderIndex).getChangedEntity().getType()==JavaEntityType.ELSE_STATEMENT) {
					oldCode.append("}\nelse ");
				}
				int lack_label = 0;
				if (changes.get(orderIndex).getChangedEntity().getType()==JavaEntityType.IF_STATEMENT
						&& !oldAPICalls.get(orderIndex).startsWith("if") ) {
					oldCode.append("if (");
					lack_label = 1;
				}		
				//main pattern gen
				oldCode.append(oldAPICalls.get(orderIndex));
				//main pattern gen
				
				if (lack_label == 1) {
					oldCode.append(")\n{");
				}
				else oldCode.append("\n");
				
				if (changes.get(orderIndex).getChangedEntity().getType()==JavaEntityType.IF_STATEMENT) {
					oldCode.append("{\n");
				}								
				oldOri.append(oldOriCodes.get(orderIndex)).append("\n");
				templateLeftList.add(leftTemplateMap.get(orderIndex));
			}								
			oldTemplate = oldCode.toString();
			oldOriTemplate = oldOri.toString();
//			System.out.println("? old Temp *******");
//			System.out.println(oldCode.toString());
//			System.out.println("? old Temp #######");
		} else {
			oldOrderedFacts = Collections.EMPTY_LIST;
		}
		if (!newRanges.isEmpty()) {
			newOrderedFacts = sortFacts(newRanges, sortedFacts);
			StringBuffer newCode = new StringBuffer();
			StringBuffer newOri = new StringBuffer();
			for (Integer orderIndex : newOrderedFacts) {
				if (newexp.contains(orderIndex)) {
					continue;
				}
				if (this.lv3map[orderIndex] == true) {
					lv3label = true;
				}
				if (changes.get(orderIndex).getChangedEntity().getType()==JavaEntityType.ELSE_STATEMENT) {
					newCode.append("}\nelse ");
				}
				newCode.append(newAPICalls.get(orderIndex)).append("\n");
				if (changes.get(orderIndex).getChangedEntity().getType()==JavaEntityType.IF_STATEMENT) {
					newCode.append("{\n");
				}	
				newOri.append(newOriCodes.get(orderIndex)).append("\n");
				templateRightList.add(rightTemplateMap.get(orderIndex));
			}
			newTemplate = newCode.toString();
			newOriTemplate = newOri.toString();
//			System.out.println("? new Temp *******");
//			System.out.println(newTemplate);
//			System.out.println("? new Temp #######");
		} else {
			newOrderedFacts = Collections.EMPTY_LIST;
		}
		if (oldTemplate == null) {
			oldTemplate = new String();
			oldOriTemplate = new String();
		}
		if (newTemplate == null) {
			newTemplate = new String();
			newOriTemplate = new String();
		}
		while (oldTemplate.endsWith("\n") || oldTemplate.endsWith(" ")) {
			oldTemplate = oldTemplate.substring(0, oldTemplate.length()-1);
		}
		while (newTemplate.endsWith("\n") || newTemplate.endsWith(" ")) {
			newTemplate = newTemplate.substring(0, newTemplate.length()-1);
		}
//		if (oldTemplate.endsWith(";")) {
//			oldTemplate = oldTemplate.substring(0, oldTemplate.length()-1);
//		}
//		if (newTemplate.endsWith(";")) {
//			newTemplate = newTemplate.substring(0, newTemplate.length()-1);
//		}
		boolean refined_label = true;
		int api_migr_type = CurrentResolver.current_resolver.resolve_one(this, CurrentResolver.current_ranges, CurrentResolver.current_allchanges);
		if (api_migr_type == 0) refined_label = false;
		System.out.println("api_migr_type is:" + api_migr_type);
		if (CurrentResolver.isinsertversion() && oldTemplate.isEmpty()) refined_label = false;
//		if (!oldTemplate.equals("") && !newTemplate.equals("")) refined_label = false;
		String depRec = String.join(",", CommonValue.dep_output);
		CommonValue.all_dep.add(depRec);
		if (refined_label) {
			if (purecheck(oldTemplate) && purecheck(newTemplate) && !CommonValue.common_old_version.equals(CommonValue.common_new_version)) {
				System.out.println("-----------------------------------> Valid Pattern");
				DatabaseControl data1 = new DatabaseControl();
				String old_imports = headGen(old_api_import);
				String new_imports = headGen(new_api_import);
				String full_oldTemplate = old_imports + oldTemplate;
				String full_newTemplate = new_imports + newTemplate;
//				System.out.println("? old Temp *******");
				System.out.println(full_oldTemplate);
				System.out.println("====================Replacedby======================");
				
//				System.out.println("? new Temp *******");
				System.out.println(full_newTemplate);
				if (CommonValue.dep_output.size() > 0) {
					System.out.println("====dep");
					System.out.println(depRec);
				}
				System.out.println("===end");
				
				String lv_label = lv3label?"3":"2";				
				if (!full_oldTemplate.contains("\n") && !full_newTemplate.contains("\n")) lv_label = "1";
				int label = data1.insertpattern(full_oldTemplate, full_newTemplate, CommonValue.common_old_version, CommonValue.common_new_version, "Group Changes", lv_label);
				data1.insertsnippet(oldOriTemplate, newOriTemplate, CommonValue.common_project_name, CommonValue.common_commit_number, String.valueOf(label)
						, CommonValue.common_old_version+"-"+CommonValue.common_new_version);

				refineChanges(entityChanges, oldCu, newCu, oldClassName, newClassName);	
				printChanges();
			}
			else {
				System.out.println("-----------------------------------> Not Pattern");
			}
		}
		else {
			System.out.println("-----------------------------------> Not Pattern");
		}
	}
	
	/**
	 * The return value is immutable
	 * @return
	 */
	public Set<Integer> getFactIndexes() {
		return facts.keySet();
	}	

	public String getNextUnifiedIdentifier() {
		return ABSTRACT_U + (uIdx++);				
	}
	
	public String getNextUnifiedMIdentifier() {
		return ABSTRACT_M + (mIdx++);
	}
	
	public String getNextUnifiedTIdentifier() {
		return ABSTRACT_T + (tIdx++);
	}
	
	//Pretty printer
	public void printChanges() {
		if (!templateLeftList.isEmpty()) {
			System.out.println("This Pattern *******");
			System.out.println("Old code:");
			PrettyPrinter.printTemplates(templateLeftList);
		}
		if (!templateRightList.isEmpty()) {
			System.out.println("New code:");
			PrettyPrinter.printTemplates(templateRightList);
			System.out.println("This Pattern #######");
		}
		
		if (!changedFields.isEmpty()) {
			System.out.println("Relevant entity changes:");
			for (FieldChangeFact fcf : changedFields.values()) {
				System.out.println(fcf.ct + " " + fcf.content);
			}
		}
		if (!changedClasses.isEmpty()) {
			System.out.println("Relevant class changes:");
			for (ClassChangeFact ccf : changedClasses.values()) {
				System.out.println(ccf.ct + " " + ccf.tBinding.getName());
			}
		}
	}
	
	//added by shengzhe 10/10, 2018
	public String headGen(Map<String, String>heads) {
		String rst = new String();
		for (String key : heads.keySet()) {
			rst += "import " + key + " from " + heads.get(key) + "\n";
		}
		if (!rst.isEmpty()) {
			rst += "=======\n";
		}
		return rst;
	}
	
	
	//modified by shengzhe 4/5, 2018
	public void refineChanges(Map<SourceCodeEntity, SourceCodeChange> entityChanges, CompilationUnit oldCu, CompilationUnit newCu, 
			String oldClassName, String newClassName) {
		oldAPIs = new ArrayList<IMethodBinding>();
		newAPIs = new ArrayList<IMethodBinding>();
		oldOrderedRefinedFacts = new ArrayList<Integer>();
		newOrderedRefinedFacts = new ArrayList<Integer>();
		typeMap = new HashMap<String, Set<TypeNameTerm>>();
		methodMap = new HashMap<String, Set<MethodNameTerm>>();
		fieldMap = new HashMap<String, Set<FieldReference>>();
		refineTemplate(templateLeftList, oldAPIs, oldOrderedFacts, oldOrderedRefinedFacts, true, oldClassName);
		refineTemplate(templateRightList, newAPIs, newOrderedFacts, newOrderedRefinedFacts, false, newClassName);
		List<IMethodBinding> temp = new ArrayList<IMethodBinding>(oldAPIs);
		temp.retainAll(newAPIs);//common set
		oldAPIs.removeAll(temp);
		newAPIs.removeAll(temp);
		oldTemplate = null;
		newTemplate = null;
		changedFields = new HashMap<String, FieldChangeFact>();
		changedClasses = new HashMap<String, ClassChangeFact>();
		relateChangeEntities(oldOrderedRefinedFacts, entityChanges, oldCu, newCu);
		relateChangeEntities(newOrderedRefinedFacts, entityChanges, oldCu, newCu);		
	}
	
	private void refineTemplate(List<Node> templateList, List<IMethodBinding> bindings, 
			List<Integer> originalOrderedFacts, 
			List<Integer> refinedOrderedFacts, boolean isOld, String ownerClassName) {
		Node template = null;
		Set<IMethodBinding> allBindings = new HashSet<IMethodBinding>();
		Set<Integer> apiIrrelevants = new HashSet<Integer>();
		for (int i = 0; i < templateList.size(); i++) {
			template = NodeManipulator.getCopy(templateList.get(i));
			Enumeration<Node> bEnum = template.breadthFirstEnumeration();
			Node temp = null;
			Map<Integer, Node> libAPINodes = new HashMap<Integer, Node>();
			Map<Integer, Node> nonLibAPINodes = new HashMap<Integer, Node>();
			while (bEnum.hasMoreElements()) {
				temp = bEnum.nextElement();
				Object obj = temp.getUserObject();
				if (obj != null && obj instanceof List) {
					List<Object> info = (List<Object>)obj;
					IMethodBinding binding = (IMethodBinding) info.get(0);
					String className = binding.getDeclaringClass().getKey();					
					if (className.equals(ownerClassName)) {
						nonLibAPINodes.put(temp.id, temp);
					} else {
						libAPINodes.put(temp.id, temp);
						allBindings.add(binding);
					}								
				}
			}
			if (libAPINodes.isEmpty()) {
				apiIrrelevants.add(i);
			}
			List<Integer> sortedIndexes = new ArrayList<Integer>(nonLibAPINodes.keySet());
			Collections.reverse(sortedIndexes);
			for (Integer idx : sortedIndexes) {
				temp = nonLibAPINodes.get(idx);
				Enumeration<Node> subEnum = temp.breadthFirstEnumeration();
				Set<Node> subNodes = new HashSet<Node>();
				while (subEnum.hasMoreElements()) {
					subNodes.add(subEnum.nextElement());
				}
				subNodes.retainAll(libAPINodes.values());
				if (subNodes.isEmpty()) {
					String tmpStr = DefUseChangeFact.render(temp);
					String uStr = concreteToUnified.get(tmpStr);
					if (uStr == null) {
						if (!isOld && !renamedVars.isEmpty()) {
							for (Entry<String, String> entry : renamedVars.entrySet()) {
								String keyStr = concreteToUnified.get(entry.getKey());
								String valStr = concreteToUnified.get(entry.getValue());									
								if (keyStr != null && tmpStr.contains(valStr)) {
									tmpStr = tmpStr.replaceAll(valStr, keyStr);
								}
							}
							uStr = concreteToUnified.get(tmpStr);								
						}
						if (uStr == null)
							uStr = getNextUnifiedIdentifier();
						concreteToUnified.put(tmpStr, uStr);
					}
					temp.removeAllChildren();
					temp.setValue(uStr);
				}				
			}
			templateList.set(i, template);
//			System.out.println("temp list *******");
//			System.out.println(DefUseChangeFact.render(template));
//			System.out.println("temp list #######");
		}	
		if (!apiIrrelevants.isEmpty()) {
			SimpleDataflowGraph sg = createSimpleDataflow(templateList, apiIrrelevants);
			Map<String, String> varReplaceMap = mergeDataNodes(sg);
			List<Node> newTemplateList = new ArrayList<Node>();
			for (int i = 0 ; i < templateList.size(); i++) {
				if (apiIrrelevants.contains(i)) {
					continue;
				}
				refinedOrderedFacts.add(originalOrderedFacts.get(i));
				Node temp = templateList.get(i);
				DefUseChangeFact.replaceIdentifiers(temp, varReplaceMap);
				newTemplateList.add(temp);
//				System.out.println(DefUseChangeFact.render(temp));
			}
			templateList.clear();
			templateList.addAll(newTemplateList);				
		} else {
 			refinedOrderedFacts.addAll(originalOrderedFacts);
		}		
		Map<String, String> concreteToAbstractTypes = new HashMap<String, String>();
		String qName = TypeAPIResolver.convertToQName(ownerClassName);
		Set<String> pNames = TypeAPIResolver.getPackageNames(qName);
		DefUseGroupHelper.parameterizeTypeUsage(templateList, typeMap, concreteToAbstractTypes, pNames, this);
		DefUseGroupHelper.parameterizeMethodUsage(templateList, methodMap);
		DefUseGroupHelper.parameterizeFieldUsage(refinedOrderedFacts, facts, fieldMap);
		bindings.addAll(allBindings);
	}
	
	
	
	private void relateChangeEntities(List<Integer> refinedFacts, Map<SourceCodeEntity, SourceCodeChange> entityChanges, 
			CompilationUnit oldCu, CompilationUnit newCu) {
		ASTNodeFinder finder = new ASTNodeFinder();
		for (Integer idx : refinedFacts) {			
			DefUseChangeFact f = facts.get(idx);
			if (!f.fieldChangeFactMap.isEmpty()) {
				for(Entry<String, FieldChangeFact> entry : f.fieldChangeFactMap.entrySet()) {
					String fName = entry.getKey();	
					FieldChangeFact fcf = entry.getValue();
					for (Entry<SourceCodeEntity, SourceCodeChange> entry2 : entityChanges.entrySet()) {
						SourceCodeEntity e = entry2.getKey();
						if (e.getType().isField()) {
							String name = e.getUniqueName();
							name = name.substring(0, name.indexOf(' '));
							String fieldName = name.substring(name.lastIndexOf('.') + 1);
							String qName = name.substring(0, name.lastIndexOf('.'));
							Set<String> fNames = FieldAPIResolver.getPossibleFieldNames(fieldName, qName);
							if (fNames.contains(fName)) {
								SourceCodeRange scr = SourceCodeRange.convert(e);
								if (fcf.ct.equals(CHANGE_TYPE.ADD)) {
									ASTNode n = finder.lookforASTNode(newCu, scr);
									fcf.content = n.toString();
									changedFields.put(fName, fcf);
								}
							}							
						}																	
					}
				}				
			}
			if(!f.classChangeFactMap.isEmpty()) {
				for(Entry<String, ClassChangeFact> entry : f.classChangeFactMap.entrySet()) {
					String className = entry.getKey();
					ClassChangeFact ccf = entry.getValue();
					String qName = ccf.tBinding.getQualifiedName();
					if (ccf.ct.equals(CHANGE_TYPE.ADD)) {
						List<ClientClass> insertedClasses = callbackCF.getInsertedClasses();
						for (ClientClass cc : insertedClasses) {
							String cName = cc.className;
							if (qName.equals(cName)) {
								ccf.linkedEntity = cc;
								changedClasses.put(className, ccf);
								break;
							}
						}
					}					
				}
			}
		}
	}
	
	private SimpleDataflowGraph createSimpleDataflow(List<Node> templateList, Set<Integer> apiIrrelevants) {
		Node template = null;
		SimpleDataflowGraph sg = new SimpleDataflowGraph();
		Set<Integer> purelyAssigns = new HashSet<Integer>();
		for (int i = 0; i < templateList.size(); i++) {
			template = templateList.get(i);
			if (apiIrrelevants.contains(i)) {
				EntityType et = template.getLabel();
				if (et.equals(JavaEntityType.VARIABLE_DECLARATION_STATEMENT)) {
					purelyAssigns.add(i);
					Enumeration<Node> nEnum = template.breadthFirstEnumeration();
					Node temp = null;
					while(nEnum.hasMoreElements()) {
						temp = nEnum.nextElement();
						if (temp.getLabel().equals(JavaEntityType.VARIABLE_DECLARATION_FRAGMENT)) {
							Enumeration<Node> cEnum = temp.children();
							List<Node> children = new ArrayList<Node>();
							Node cNode = null;
							int indexOfEqual = 0;
							int index = 0;
							while (cEnum.hasMoreElements()) {
								cNode = cEnum.nextElement();								
								if (cNode.getValue().equals("=")) {
									indexOfEqual = index;
								}
								children.add(cNode);
								index++;
							}
//							System.out.println(children.size());
//							System.out.println(indexOfEqual);
							if (indexOfEqual -1 <0 || indexOfEqual >= children.size()) {
//								System.out.println("ha?");
								continue;
							}
							String dest = DefUseChangeFact.render(children.get(indexOfEqual - 1));
							String src = DefUseChangeFact.render(children.get(indexOfEqual + 1));
							DataNode srcNode = sg.getDataNode(src);
							DataNode destNode = sg.getDataNode(dest);
							sg.addEdge(srcNode, destNode);
						} 
					}					
				}
			} else {
				String str = DefUseChangeFact.render(template);
				StmtNode sNode = sg.getStmtNode(str, i);
				Matcher matcher = PatternMatcher.uPattern.matcher(str);
				Set<String> vars = new HashSet<String>();
				while (matcher.find()) {
					vars.add(matcher.group());
				}
				for (String var : vars) {
					DataNode dNode = sg.getDataNode(var);
					sg.addEdge(dNode, sNode);
				}							
			}
		}		
		return sg;
	}
	
	private Map<String, String> mergeDataNodes(SimpleDataflowGraph sg) {
		Map<Integer, StmtNode> stmtNodes = sg.getAllStmtNodes();
		Map<String, String> varReplaceMap = new HashMap<String, String>();
		for (StmtNode sNode : stmtNodes.values()) {
			Collection<SEdge> edges = sg.getInEdges(sNode);
			for (SEdge e : edges) {
				SNode src = sg.getSource(e);
				if (src instanceof DataNode) {
					String strToBeReplaced = src.label;
					Collection<SEdge> edges2 = sg.getInEdges(src);
					String strToReplace = null;
					while (edges2.size() == 1) {						
						Iterator<SEdge> eIter = edges2.iterator();						
						SNode dst = sg.getSource(eIter.next());
						if (!(dst instanceof DataNode))
							break;
						strToReplace = dst.label;
						src = dst;
						edges2 = sg.getInEdges(src);
					}
					if (strToReplace != null) {
						varReplaceMap.put(strToBeReplaced, strToReplace);
					}
				}
			}
		}
		return varReplaceMap;
	}
	
	
	public void reorganizeChanges() {
		addedParameters = new HashMap<String, DATA_CHANGE_TYPE>();
		deletedParameters = new HashMap<String, DATA_CHANGE_TYPE>();
		for (DefUseChangeFact f : facts.values()) {
			if (f instanceof InsertFact) {
				InsertFact iFact = (InsertFact)f;
				addedParameters.putAll(iFact.addedParameters);
			} else if (f instanceof DeleteFact) {
				DeleteFact dFact = (DeleteFact)f;
				deletedParameters.putAll(dFact.removedParameters);
			} else {
				UpgradeFact uFact = (UpgradeFact)f;
				addedParameters.putAll(uFact.addedParameters);
				deletedParameters.putAll(uFact.removedParameters);
			}			
		}
	}
	
	private List<Integer> sortFacts(Map<SourceCodeRange, Integer> oldRanges, List<Integer> sortedFacts) {
		List<SourceCodeRange> sortedOldRanges = new ArrayList<SourceCodeRange>(oldRanges.keySet());						
		Collections.sort(sortedOldRanges);
		List<Integer> reorderedFacts = new ArrayList<Integer>();		
		for (int i = 0; i < sortedOldRanges.size(); i++) {
			int idx = oldRanges.get(sortedOldRanges.get(i));
			reorderedFacts.add(idx);
			
		}		
		return reorderedFacts;
	}
	
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer("Group ");
		if (oldTemplate != null) {
			buffer.append("Old code:\n").append(oldTemplate);
		}
		if (newTemplate != null) {
			buffer.append("New code:\n").append(newTemplate);
		}
		return buffer.toString();
	}
	
	private void unifyIdentifiers(Map<String, String> tmpConcreteToAbstract, Map<String, String> concreteToUnified, 
			Map<String, String> abstractToUnified, String prefix) {
		String cStr = null;
		String uStr = null;
		for (Entry<String, String> entry : tmpConcreteToAbstract.entrySet()) {
			cStr = entry.getKey();
			if (concreteToUnified.containsKey(cStr)) {
				uStr = concreteToUnified.get(cStr);
			} else {					
				if (prefix.equals(ABSTRACT_U)) {
					if (cStr.equals("String")) uStr = "String";
					else uStr = getNextUnifiedIdentifier();					
				} else if (prefix.equals(ABSTRACT_V)) {
					uStr = "[-" + ABSTRACT_V + (vIdx++) + "-]";
				} else if (prefix.equals(ABSTRACT_M)) {
					uStr = "[-" + ABSTRACT_M + (mIdx++) + "-]";
				} else if (prefix.equals(ABSTRACT_T)) {
					uStr = "[-" + ABSTRACT_T + (tIdx++) + "-]";
				} else if (prefix.equals(ABSTRACT_C)) {
					uStr = "[-" + ABSTRACT_C + (cIdx++) + "-]";
				}
				
				concreteToUnified.put(cStr, uStr);
			}
			abstractToUnified.put(tmpConcreteToAbstract.get(cStr), uStr);
		}
	}
	
	private Node getExpressionTree(CompilationUnit cu, SourceCodeRange r,
			JavaExpressionConverter converter) {
		ASTNodeFinder finder = new ASTNodeFinder();
		ASTNode n = finder.lookforASTNode(cu, r);
		converter.clear();
		n.accept(converter);
		Node result = converter.getRoot();
		JavaExpressionConverter.markSubStmts(result, n);
		return result;
	}
	
	private Node editExpressionTree(CompilationUnit cu, SourceCodeRange r,
			JavaExpressionConverter converter) {
		ASTNodeFinder finder = new ASTNodeFinder();
		ASTNode n = finder.lookforASTNode(cu, r);
		converter.clear();
		n.accept(converter);
		Node result = converter.getRoot();
		JavaExpressionConverter.markSubStmts(result, n);
		return result;
	}
	
}
