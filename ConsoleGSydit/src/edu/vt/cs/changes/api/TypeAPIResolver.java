package edu.vt.cs.changes.api;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;

import com.ibm.wala.ipa.slicer.SDG;

import partial.code.grapa.dependency.graph.DataFlowAnalysisEngine;
import partial.code.grapa.mapping.ClientMethod;
import edu.vt.cs.append.CommonValue;
import edu.vt.cs.append.DatabaseControl;
import edu.vt.cs.append.FineChangesInMethod;
import edu.vt.cs.append.JavaExpressionConverter;
import edu.vt.cs.append.TopDownTreeMatcher;
import edu.vt.cs.changes.ChangeFact;
import edu.vt.cs.changes.ChangeMethodData;
import edu.vt.cs.diffparser.util.ASTNodeFinder;
import edu.vt.cs.diffparser.util.SourceCodeRange;

public class TypeAPIResolver{	
	
	protected DataFlowAnalysisEngine leftEngine;
	protected DataFlowAnalysisEngine rightEngine;
	private APIResolver resolver;
	
	public TypeAPIResolver(APIResolver resolver) {
		this.leftEngine = resolver.leftEngine;
		this.rightEngine = resolver.rightEngine;
		this.resolver = resolver;
	}
	
	public void resolve(Map<String, Set<ChangeFact>> missings, Map<String, ITypeBinding> typeMap) {
		ITypeBinding tb = null;
		GraphConvertor2 oc = new GraphConvertor2();
		GraphConvertor2 nc = new GraphConvertor2();
		Set<SourceCodeRange> ranges = null;
		DefUseChangeFactGroup group = null;
		String key = null;
		for (Entry<String, Set<ChangeFact>> entry : missings.entrySet()) {
			tb = typeMap.get(entry.getKey());
			key = tb.getKey();
			for (ChangeFact cf : entry.getValue()) {
				List<ChangeMethodData> data = cf.changedMethodData;
				for (ChangeMethodData d : data) {
					if (d.oldTypeBindingMap.isEmpty())
						continue;
					if (d.oldTypeBindingMap.containsKey(key)) {
						ClientMethod oldMethod = d.oldMethod;
						ClientMethod newMethod = d.newMethod;
						CompilationUnit oldCu = (CompilationUnit)oldMethod.ast;
						CompilationUnit newCu = (CompilationUnit)newMethod.ast;
						SDG lfg = resolver.findOrCreateOldSDG(oldMethod);						   
						SDG rfg = resolver.findOrCreateNewSDG(newMethod);
						resolver.oldMapper.add(oldMethod.ast);
						oc.init(leftEngine.getCurrentIR(), lfg,
								   oldMethod,
								   resolver.oldMapper.getLineMapper(oldCu), oldCu);   
						nc.init(rightEngine.getCurrentIR(), rfg, newMethod, 
								   resolver.newMapper.getLineMapper(newCu), newCu);					  						   						 
						SourceCodeEntity mEntity = cf.getEntity(SourceCodeRange.convert(oldMethod.methodbody));					   			  
					    FineChangesInMethod allChanges = (FineChangesInMethod)(cf.getChange(mEntity));
					    Node tree1 = null, tree2 = null, treep = null;
						SourceCodeRange oldR = null, newR = null,parentR = null;
						JavaExpressionConverter lConverter = new JavaExpressionConverter();
						JavaExpressionConverter rConverter = new JavaExpressionConverter();
						
					    for (SourceCodeChange oneChange : allChanges.getChanges() ) {
					    	if (oneChange instanceof Update) {
						    	Update u = (Update) oneChange;
						    	oldR = SourceCodeRange.convert(u
										.getChangedEntity());
								newR = SourceCodeRange
										.convert(u.getNewEntity());
								parentR = SourceCodeRange.convert(u
										.getParentEntity());
								tree1 = getExpressionTree(oldCu, oldR,
										lConverter);
								tree2 = getExpressionTree(newCu, newR,
										rConverter);
								if (tree1 == null || tree2 == null) {
									continue;
								}
//								treep = getExpressionTree(oldCu, parentR, lConverter);
//								TopDownTreeMatcher matcher = new TopDownTreeMatcher();
//								matcher.match(tree1, tree2);
//								Map<Node, Node> unmatchedLeftToRight = matcher
//										.getUnmatchedLeftToRight();
//								if (unmatchedLeftToRight.isEmpty()) { // this is
																		// rename
																		// change
								if (!tree1.equals(tree2)) {
									UpgradeFact tmpFact = new UpgradeFact(
											tree1, tree2);
									Set<String> added = tmpFact.addedParameters
											.keySet();
									Set<String> removed = tmpFact.removedParameters
											.keySet();
									if (added.size() == 1
											&& removed.size() == 1) {
										String lx = removed.iterator().next();
										String ry = added.iterator().next();
										System.out
												.println("this pattern*******");
										
										System.out.println("Type Change: "
												+ ""
												+ lx
												+ " -> "
												+ ry);
										System.out
												.println("this pattern#######");
										if (lx.equals(ry)) {
											continue;
										}
										if (lx.length() <= 5 || ry.length()<=5) {
											continue;
										}
										DatabaseControl data1 = new DatabaseControl();
										int label = data1.insertpattern(lx, ry, CommonValue.common_old_version, CommonValue.common_new_version, "Type Change", "1");
										data1.insertsnippet(u.getChangedEntity().toString(), u.getNewEntity().toString(), CommonValue.common_project_name, CommonValue.common_commit_number, String.valueOf(label)
												, CommonValue.common_old_version+"-"+CommonValue.common_new_version);
										
									}
								}
					    	}
					    }
//					    List<DefUseChangeFactGroup> groups = cf.getDefUseChangeFactGroups(mEntity, oldCu, newCu, oc, nc, resolver);
						   
						ranges = d.oldTypeBindingMap.get(key);
						resolver.oc = oc;
						resolver.oldCu = oldCu;
					    resolver.nc = nc;
					    resolver.newCu = newCu;
						   
//				 	    resolver.resolve(groups, ranges, allChanges);		
					}
//					if (d.oldBindingMap.containsKey(tb)) {
//						ClientMethod oldMethod = d.oldMethod;
//						ClientMethod newMethod = d.newMethod;
//						CompilationUnit oldCu = (CompilationUnit)oldMethod.ast;
//						CompilationUnit newCu = (CompilationUnit)newMethod.ast;
//						System.out.println(oldMethod.toString());
//						
//						SDG lfg = resolver.findOrCreateOldSDG(oldMethod);						   
//						SDG rfg = resolver.findOrCreateNewSDG(newMethod);
//						resolver.oldMapper.add(oldMethod.ast);
//						   oc.init(leftEngine.getCurrentIR(), lfg,
//								   oldMethod,
//								   resolver.oldMapper.getLineMapper(oldCu), oldCu);   
//						   nc.init(rightEngine.getCurrentIR(), rfg, newMethod, 
//								   resolver.newMapper.getLineMapper(newCu), newCu);					  						   						 
//						   SourceCodeEntity mEntity = cf.getEntity(SourceCodeRange.convert(oldMethod.methodbody));					   			  
//						   FineChangesInMethod allChanges = (FineChangesInMethod)(cf.getChange(mEntity));
//						   List<DefUseChangeFactGroup> groups = cf.getDefUseChangeFactGroups(mEntity, oldCu, newCu, oc, nc, resolver);
//						   
//						   ranges = d.oldBindingMap.get(tb);
//						   resolver.oc = oc;
//						   resolver.oldCu = oldCu;
//						   resolver.nc = nc;
//						   resolver.newCu = newCu;
//						   
//						   resolver.resolve(groups, ranges, allChanges);
//					}
				}
			}
		}
	}
	
	public static Set<String> getPackageNames(String className) {
		Set<String> names = new HashSet<String>();
		String[] fragments = className.split("\\.");
		if (fragments.length == 1) {
			return names;
		}				
		if (fragments.length == 2) {
			names.add(fragments[0]);
			return names;
		}
		String tmp = fragments[0] + "." + fragments[1];
		names.add(tmp);
		for (int i = 2; i < fragments.length - 1; i++) {
			tmp = tmp + "." + fragments[i];
			names.add(tmp);
		}
		return names;	
	}
	
	/**
	 * Maybe duplicates of some existing implemented code
	 * @param binStr
	 * @return
	 */
	public static String convertToQName(String binStr) {
		if (binStr == null) return "";
		String tmp = binStr.substring(1);
		tmp = tmp.replaceAll("/", ".");
		return tmp;
	}
	
	public static boolean isInPackage(String tName, Set<String> pNames) {
		Set<String> pNames2 = getPackageNames(tName);
		pNames2.retainAll(pNames);
		return !pNames2.isEmpty();
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
}
