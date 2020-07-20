package edu.vt.cs.changes.api;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SourceRange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;

import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.util.collections.Pair;

import partial.code.grapa.dependency.graph.DataFlowAnalysisEngine;
import partial.code.grapa.mapping.ClientMethod;
import edu.vt.cs.append.CommonValue;
import edu.vt.cs.append.DatabaseControl;
import edu.vt.cs.append.FineChangesInField;
import edu.vt.cs.append.FineChangesInMethod;
import edu.vt.cs.append.JavaExpressionConverter;
import edu.vt.cs.append.TopDownTreeMatcher;
import edu.vt.cs.changes.ChangeFact;
import edu.vt.cs.changes.ChangeFieldData;
import edu.vt.cs.changes.ChangeMethodData;
import edu.vt.cs.diffparser.util.ASTNodeFinder;
import edu.vt.cs.diffparser.util.SourceCodeRange;
import edu.vt.cs.graph.ClientField;
import edu.vt.cs.graph.SourceMapper;

public class FieldAPIResolver {

	protected DataFlowAnalysisEngine leftEngine;
	protected DataFlowAnalysisEngine rightEngine;
	private APIResolver resolver;

	public static Set<String> getPossibleFieldNames(String fieldName,
			String className) {
		Set<String> names = new HashSet<String>();
		String[] fragments = className.split("\\.");
		names.add(fieldName);
		String tmp = fieldName;
		for (int i = fragments.length - 1; i >= 0; i--) {
			tmp = fragments[i] + "." + tmp;
			names.add(tmp);
		}
		return names;
	}

	public FieldAPIResolver(APIResolver resolver) {
		this.leftEngine = resolver.leftEngine;
		this.rightEngine = resolver.rightEngine;
		this.resolver = resolver;
	}

	// add by shengzhe
	public boolean check_valid(char x) {
		if ('a' < x && x < 'z')
			return true;
		if ('A' < x && x < 'Z')
			return true;
		if ('0' < x && x < '9')
			return true;
		if (x == '_')
			return true;
		return false;
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

	// add by shengzhe
	public void resolve(Map<IVariableBinding, Set<ChangeFact>> missings) {
		IVariableBinding vb = null;
		Set<ChangeFact> value = null;
		Set<SourceCodeRange> ranges = null;
		GraphConvertor2 oc = new GraphConvertor2();
		GraphConvertor2 nc = new GraphConvertor2();
		SourceMapper oldMapper = new SourceMapper();
		SourceMapper newMapper = new SourceMapper();
		Subgraph s1 = null, s2 = null;
		String vbKey = null;

		for (Entry<IVariableBinding, Set<ChangeFact>> entry : missings
				.entrySet()) {
			vb = entry.getKey();
			vbKey = vb.getKey();
			value = entry.getValue();
//			System.out.println("DeclaringMethod->" + vb.getDeclaringMethod());
//			System.out.println("Old Code:");
			//added by shengzhe for Field API double check
			if (!vbKey.contains(CommonValue.possible_lib_name1)
					&& !vbKey.contains(CommonValue.possible_lib_name2))
				continue;
			for (ChangeFact cf : value) {
//				System.out.println(cf.changedMethodData);
				List<ChangeMethodData> data = cf.changedMethodData;

				for (ChangeMethodData d : data) {
					if (d.oldFieldBindingMap.containsKey(vb)) {
//						System.out.println(d.oldFieldBindingMap);
//						System.out.println(d.newFieldBindingMap);

						ClientMethod oldMethod = d.oldMethod;
						ClientMethod newMethod = d.newMethod;
						CompilationUnit oldCu = (CompilationUnit) oldMethod.ast;
						CompilationUnit newCu = (CompilationUnit) newMethod.ast;
						System.out.println("Function Name: "
								+ oldMethod.toString());
						// if (!oldMethod.toString().contains("search"))
						// continue;
						// if (!mb.toString().contains("addDocument"))
						// continue;
						
//						SDG lfg = resolver.findOrCreateOldSDG(oldMethod);
//						SDG rfg = resolver.findOrCreateNewSDG(newMethod);
						oldMapper.add(oldMethod.ast);
//						oc.init(leftEngine.getCurrentIR(), lfg, oldMethod,
//								oldMapper.getLineMapper(oldCu), oldCu);
//						nc.init(rightEngine.getCurrentIR(), rfg, newMethod,
//								newMapper.getLineMapper(newCu), newCu);

						SourceCodeEntity mEntity;
						if (oldMethod.methodbody!=null) {
							mEntity = cf.getEntity(SourceCodeRange
									.convert(oldMethod.methodbody));
						}
						else {
//							mEntity = cf.getEntity(new SourceCodeRange(11,490));
							mEntity = cf.getEntity(SourceCodeRange
									.convert(oldMethod.initializerbody));
						}

						FineChangesInMethod allChanges = (FineChangesInMethod) (cf
								.getChange(mEntity));

						Node tree1 = null, tree2 = null;
						SourceCodeRange oldR = null, newR = null;
						JavaExpressionConverter lConverter = new JavaExpressionConverter();
						JavaExpressionConverter rConverter = new JavaExpressionConverter();
						if (allChanges == null) continue;
						for (SourceCodeChange oneChange : allChanges
								.getChanges()) {
							if (oneChange instanceof Update) {
								Update u = (Update) oneChange;
								oldR = SourceCodeRange.convert(u
										.getChangedEntity());
								newR = SourceCodeRange
										.convert(u.getNewEntity());
								tree1 = getExpressionTree(oldCu, oldR,
										lConverter);
								tree2 = getExpressionTree(newCu, newR,
										rConverter);
								TopDownTreeMatcher matcher = new TopDownTreeMatcher();
								CommonValue.Exp_Sniffer = 0;
								matcher.match(tree1, tree2);
								if (CommonValue.Exp_Sniffer == 1) continue;
								Map<Node, Node> unmatchedLeftToRight = matcher
										.getUnmatchedLeftToRight();
								if (unmatchedLeftToRight.isEmpty()) { // this is
																		// rename
																		// change
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
										System.out.println(lx
												+ " -> "
												+ ry);
										System.out
												.println("this pattern#######");
										if (lx.length() <= 5 || ry.length()<=5) {
											continue;
										}
										if (!lx.contains(".")) {
											continue;
										}
										if (lx.startsWith("\"") && ry.startsWith("\"")) {
											continue;
										}
										DatabaseControl data1 = new DatabaseControl();
										int label = data1.insertpattern(lx, ry, CommonValue.common_old_version, CommonValue.common_new_version, "Qualified Parameter Change", "1");
										data1.insertsnippet(u.getChangedEntity().toString(), u.getNewEntity().toString(), CommonValue.common_project_name, CommonValue.common_commit_number, String.valueOf(label)
												, CommonValue.common_old_version+"-"+CommonValue.common_new_version);
									}
								}
								List<DefUseChangeFact> temp = null;
								for (Entry<Node, Node> entry2 : unmatchedLeftToRight.entrySet()) {
									Node lNode = entry2.getKey();
									Node rNode = entry2.getValue();
									System.out.print("");
									if (lNode == null || rNode == null) continue;
									UpgradeFact f = new UpgradeFact(lNode, rNode);
									Set<String> added = f.addedParameters
											.keySet();
									Set<String> removed = f.removedParameters
											.keySet();
									if (added.size() == 1
											&& removed.size() == 1) {
										String lx = removed.iterator().next();
										String ry = added.iterator().next();
										System.out.println("this pattern*******");
										System.out.println(lx+ " -> " + ry);
										System.out.println("this pattern#######");
										if (lx.equals(ry)) {
											continue;
										}
										if (lx.startsWith("\"") && ry.startsWith("\"")) {
											continue;
										}
										if (lx.contains(".")) {
											DatabaseControl data1 = new DatabaseControl();
											int label = data1.insertpattern(lx, ry, CommonValue.common_old_version, CommonValue.common_new_version, "Qualified Parameter Change", "1");
											data1.insertsnippet(u.getChangedEntity().toString(), u.getNewEntity().toString(), CommonValue.common_project_name, CommonValue.common_commit_number, String.valueOf(label)
													, CommonValue.common_old_version+"-"+CommonValue.common_new_version);
										}
									}
									else {
										for (String remove_ed : removed) {
											System.out
													.println("this pattern*******");
											System.out.println(u.getChangedEntity().toString() + " ->\n" + u.getNewEntity().toString());
											System.out.println("para["+ remove_ed+"] removed in " + lNode.toString());
											System.out
													.println("this pattern#######");
											String lx = u.getChangedEntity().getUniqueName();
											String ry = u.getNewEntity().getUniqueName();
											if (u.getChangeType() == ChangeType.CONDITION_EXPRESSION_CHANGE) {
												continue;
											}
//											DatabaseControl data1 = new DatabaseControl();
//											int label = data1.insertpattern(lx, ry, CommonValue.common_old_version, CommonValue.common_new_version, "para["+ remove_ed+"] removed", "1");
//											data1.insertsnippet(u.getChangedEntity().toString(), u.getNewEntity().toString(), CommonValue.common_project_name, CommonValue.common_commit_number, String.valueOf(label)
//													, CommonValue.common_old_version+"-"+CommonValue.common_new_version);											
										}
									}
								}				
							}
//							if (oneChange.toString().contains(vb.getName())) {
//								System.out.println("Old Code:");
//								System.out.println(vb.getName());
//								System.out.println("in here:    "
//										+ oneChange.toString());
//								int idx = oneChange.getUniqueName()
//										.lastIndexOf(vb.getName());
//								int edx = vb.getName().length();
//								// System.out.println(idx + " " + len);
//								
//								SourceCodeEntity newstring = ((Update) oneChange)
//										.getNewEntity();
//								for (edx = idx; edx < newstring.getUniqueName()
//										.length(); edx++) {
//									char element = newstring.getUniqueName()
//											.charAt(edx);
//									if (check_valid(element) == false) {
//										break;
//									}
//								}
//								System.out.println("New Code:\n"
//										+ newstring.getUniqueName().substring(
//												idx, edx));
//								System.out.println("in here:   "
//										+ newstring.getUniqueName());
//								System.out.println("API replacement:\n"
//										+ vb.getKey().split(";")[0]
//										+ "."
//										+ vb.getName()
//										+ " is replaced by "
//										+ vb.getKey().split(";")[0]
//										+ "."
//										+ newstring.getUniqueName().substring(
//												idx, edx));
//
//							}
						}
//						List<DefUseChangeFactGroup> groups = cf
//								.getDefUseChangeFactGroups(mEntity, oldCu,
//										newCu, oc, nc, resolver);

						ranges = d.oldFieldBindingMap.get(vb);
						resolver.oc = oc;
						resolver.oldCu = oldCu;
						resolver.nc = nc;
						resolver.newCu = newCu;

//						System.out.println("groups: " + groups);
//						System.out.println("ranges: " + ranges);
//						System.out.println("allchanges: " + allChanges);
						
//						resolver.resolve(groups, ranges, allChanges);
					}
				}
				// Add by March-9, 2017 Shengzhe
				List<ChangeFieldData> data2 = cf.changedFieldData;

				for (ChangeFieldData d : data2) {
					if (d.oldFieldBindingMap.containsKey(vb)) {
						System.out.println(d.oldFieldBindingMap);
						System.out.println(d.newFieldBindingMap);

						ClientField oldField = d.oldField;
						ClientField newField = d.newField;
						CompilationUnit oldCu = (CompilationUnit) oldField.ast;
						CompilationUnit newCu = (CompilationUnit) newField.ast;
						System.out.println("Function Name: "
								+ oldField.toString());
						// if (!oldMethod.toString().contains("search"))
						// continue;
						// if (!mb.toString().contains("addDocument"))
						// continue;
						oldMapper.add(oldField.ast);

//						System.out.print("CountChangeEntity: " + cf.countChangedEntities());
						//added by shengzhe July, 2017
						if (oldField.field.getInitializer()==null) {
							continue;
						}
						SourceCodeEntity mEntity = cf.getEntity(SourceCodeRange
								.convert(oldField.field.getInitializer()));
//						SourceCodeEntity mEntity = cf.getEntity(d.oldASTRanges.get(0));
						SourceCodeChange oneChange = cf.getChange(mEntity);

						Node tree1 = null, tree2 = null;
						SourceCodeRange oldR = null, newR = null;
						JavaExpressionConverter lConverter = new JavaExpressionConverter();
						JavaExpressionConverter rConverter = new JavaExpressionConverter();
						
							if (oneChange instanceof Update) {
								Update u = (Update) oneChange;
								oldR = SourceCodeRange.convert(u
										.getChangedEntity());
								newR = SourceCodeRange.convert(u
										.getNewEntity());
								tree1 = getExpressionTree(oldCu, oldR,
										lConverter);
								tree2 = getExpressionTree(newCu, newR,
										rConverter);
								TopDownTreeMatcher matcher = new TopDownTreeMatcher();
								matcher.match(tree1, tree2);
								Map<Node, Node> unmatchedLeftToRight = matcher
										.getUnmatchedLeftToRight();
								if (unmatchedLeftToRight.isEmpty()) { // this is
																		// rename
																		// change
									UpgradeFact tmpFact = new UpgradeFact(
											tree1, tree2);
									Set<String> added = tmpFact.addedParameters
											.keySet();
									Set<String> removed = tmpFact.removedParameters
											.keySet();
									if (added.size() == 1
											&& removed.size() == 1) {
										System.out
												.println("this pattern*******");
										System.out.println(removed.iterator()
												.next()
												+ " -> "
												+ added.iterator().next());
										System.out
												.println("this pattern#######");
									}
								}
							}
					}
				}
			}
		}
	}
}
