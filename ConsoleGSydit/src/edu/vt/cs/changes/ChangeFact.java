package edu.vt.cs.changes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.ibm.wala.util.collections.Pair;

import partial.code.grapa.mapping.ClientInitializer;
import partial.code.grapa.mapping.ClientMethod;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.ChangeType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.EntityType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SourceRange;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.Move;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;
import edu.vt.cs.append.CommonValue;
import edu.vt.cs.append.CompositeEntity;
import edu.vt.cs.append.DatabaseControl;
import edu.vt.cs.append.FineChangesInField;
import edu.vt.cs.append.FineChangesInMethod;
import edu.vt.cs.changes.api.APIResolver;
import edu.vt.cs.changes.api.CDGraphConvertor;
import edu.vt.cs.changes.api.DefUseChangeDetector;
import edu.vt.cs.changes.api.DefUseChangeFact;
import edu.vt.cs.changes.api.DefUseChangeFactGroup;
import edu.vt.cs.changes.api.GraphConvertor2;
import edu.vt.cs.changes.api.MethodAPIResolver;
import edu.vt.cs.changes.api.PatternInferer;
import edu.vt.cs.diffparser.util.SourceCodeRange;
import edu.vt.cs.grapa.extension.ClientEntityFinder;
import edu.vt.cs.grapa.extension.ClientFieldFinder;
import edu.vt.cs.grapa.extension.ClientMethodFinder;
import edu.vt.cs.graph.ClientClass;
import edu.vt.cs.graph.ClientField;

public class ChangeFact {

	File left = null;
	File right = null;
	Map<SourceCodeEntity, SourceCodeChange> entityChanges = null;
	
	List<Pair<ClientMethod, ClientMethod>> changedMethods = null;
	public List<ChangeMethodData> changedMethodData = null;
	List<Pair<ClientInitializer, ClientInitializer>> changedInitializers = null;
	List<ClientMethod> insertedMethods = null;
	List<ClientMethod> deletedMethods = null;
	List<Pair<ClientField, ClientField>> changedFields = null;
	public List<ChangeFieldData> changedFieldData = null;
	List<ClientField> insertedFields = null;
	List<ClientField> deletedFields = null;
	protected List<ClientClass> insertedClasses = null;
	protected List<ClientClass> deletedClasses = null;
	List<Pair<ClientClass, ClientClass>> changedClasses = null;
	Map<SourceCodeRange, SourceCodeEntity> rangeToEntity = null;
	Map<SourceCodeEntity, List<DefUseChangeFactGroup>> entityDefUseChanges = null;
	
	public ChangeFact(File l, File r, List<SourceCodeChange> changes) {
		left = l;
		right = r;		
		entityChanges = new HashMap<SourceCodeEntity, SourceCodeChange>();
		entityDefUseChanges = new HashMap<SourceCodeEntity, List<DefUseChangeFactGroup>>();
		rangeToEntity = new HashMap<SourceCodeRange, SourceCodeEntity>();
		EntityType et = null;
		SourceCodeEntity sc = null;
		SourceCodeChange c = null;		
		
		int i = 0;
		int size = changes.size();
		
			while (i < size) {
				c = changes.get(i);
				//added by shengzhe [
				if (c instanceof FineChangesInField) {
					List<SourceCodeChange> fc = ((FineChangesInField)c).getChanges();
					deal_with_fineChanges(fc);
				}
				// ]
				//added by shengzhe jun-22, 2017 [
				if (c.getChangeType() == ChangeType.STATEMENT_PARENT_CHANGE
					&& (c.getParentEntity().getType() == JavaEntityType.SUPER_INTERFACE_TYPES
					|| c.getParentEntity().getType() == JavaEntityType.SUPER_CLASS_TYPES)) {
					entityChanges.put(sc, c);
					sc = c.getChangedEntity();
					SourceCodeRange range = SourceCodeRange.convert(sc);
					rangeToEntity.put(range, sc);
					i++; continue;
				}
				// ]
				sc = c.getChangedEntity();
				if (sc == null) {
					// do not process deleted or added files
				} else {
					et = sc.getType();
					try{
						if (et.isClass() || et.isField() || et.isMethod()) {
							entityChanges.put(sc, c);
							SourceCodeRange range = null;
							if (et.isMethod()) {							
								SourceCodeEntity old = ((CompositeEntity)sc).getOldEntity();								
								range = SourceCodeRange.convert(old);
							} else{
//								System.out.println(sc.getStartPosition() + " " + sc.getEndPosition());
								range = SourceCodeRange.convert(sc);
							}							
							rangeToEntity.put(range, sc);
						} else {
							System.err.println("Need more process");				
						}	
					} catch(Exception e) {
						entityChanges.put(sc, c);
					}		
				}				
				i++;
			}		
	}
	
	//add by shengzhe to recurrently deal with fine changes
	public void deal_with_fineChanges(List<SourceCodeChange> changes) {
		EntityType et = null;
		SourceCodeEntity sc = null;
		SourceCodeChange c = null;		
		
		int i = 0;
		int size = changes.size();
		
		while (i < size) {
			c = changes.get(i);
			sc = c.getChangedEntity();
			if (sc == null) {
				// do not process deleted or added files
			} else {
				et = sc.getType();
				try{
					if (et.isQualifiedName()) {
						entityChanges.put(sc, c);
						SourceCodeRange range = null;
						if (et.isMethod()) {							
							SourceCodeEntity old = ((CompositeEntity)sc).getOldEntity();								
							range = SourceCodeRange.convert(old);
						} else{
							System.out.println(sc.getStartPosition() + " " + sc.getEndPosition());
							range = SourceCodeRange.convert(sc);
						}							
						rangeToEntity.put(range, sc);
					} else {
						System.err.println("Need more process");				
					}	
				} catch(Exception e) {
					entityChanges.put(sc, c);
				}		
			}				
			i++;
		}			
	}
	
	public int countChangedEntities() {
		return entityChanges.size();
	}
	
	public SourceCodeChange getChange(SourceCodeEntity e) {
		return entityChanges.get(e);
	}
	
	public static ClientClass getClass(Map<String, ClientClass> map, String name) {
		ClientClass c = map.get(name);
		return c;
	}
	
	public List<DefUseChangeFactGroup> getDefUseChangeFactGroupByEntity(SourceCodeEntity e) {
		return entityDefUseChanges.get(e);
	}
	
	public List<DefUseChangeFactGroup> getDefUseChangeFactGroups(SourceCodeEntity e, 
			CompilationUnit oldCu, CompilationUnit newCu, GraphConvertor2 oc, GraphConvertor2 nc, 
			APIResolver callback, ChangeMethodData d, CDGraphConvertor CD_oc, CDGraphConvertor CD_nc) {
		List<DefUseChangeFactGroup> result = entityDefUseChanges.get(e);
		if (result == null) {
			DefUseChangeDetector detector = new DefUseChangeDetector();
			SourceCodeChange c = entityChanges.get(e);			
			List<List<DefUseChangeFact>> factsList = detector.detect(oldCu, newCu, oc, nc, c, callback, CD_oc, CD_nc);
			PatternInferer inferer = new PatternInferer(this, entityChanges, oldCu, newCu, oc.getClassName() + ";", 
					nc.getClassName() + ";", oc.getSDG(), nc.getSDG());
			result = inferer.infer(factsList, ((FineChangesInMethod)c).getChanges(), detector.getRenamedVars());
			entityDefUseChanges.put(e, result);			
		}
		return result;
	}
	
	public SourceCodeEntity getEntity(SourceCodeRange r) {		
		return rangeToEntity.get(r);
	}
	
	public static ClientField getField(Map<String, ClientField> map, String name) {
		ClientField f = map.get(name);
		if (f == null) {
			name = name.substring(0, name.indexOf(":"));
			String key = null;
			ClientField value = null;
			String keySub = null;
			for (Entry<String, ClientField> entry : map.entrySet()) {
				key = entry.getKey();
				value = entry.getValue();
				keySub = key.substring(0, key.indexOf(":"));
				if (keySub.equals(name)) { 
					f = value;
					break;
				}
			}
		}	
		return f;
	}
	
	// added by shengzhe 4/5 18 for helping check the input-output semantics
	public static boolean checkGroupWithChange(int idx, DefUseChangeFactGroup group) {
		if (group.getFactIndexes().contains(idx)) {
			return true;		
		}
		return false;
	}
	
	public static DefUseChangeFactGroup getGroupWithChange(int idx, List<DefUseChangeFactGroup> groups) {
		for (DefUseChangeFactGroup g : groups) {
			if (g.getFactIndexes().contains(idx)) {
				return g;
			}
		}
		return null;
	}
	
	public List<ClientClass> getDeletedClasses() {
		return deletedClasses;
	}
	
	public List<ClientClass> getInsertedClasses() {
		return insertedClasses;
	}
	
	
	public static void refineChanges(List<ChangeFact> cfList, List<File> oldFiles, List<File> newFiles,
			List<ASTNode> leftTrees, List<ASTNode> rightTrees) {
		//String lib_str = "org.apache.lucene";
//		String lib_str = "org.bukkit";
		String lib_str = CommonValue.possible_lib_name1;
		
		Map<String, ASTNode> leftTreeMap = convertToMap(leftTrees);
		Map<String, ASTNode> rightTreeMap = convertToMap(rightTrees);
		SourceCodeEntity entity = null;
		SourceCodeChange sc = null;
		CompositeEntity cEntity = null;
		File oFile = null, nFile = null;
		SourceCodeEntity oEntity = null, nEntity = null;
		ASTNode lTree = null, rTree = null;		
		Set<ChangeFact> toRemove = new HashSet<ChangeFact>();
		System.out.println("In1"+cfList.size());
//		CommonValue.pureanalysis(oldFiles, newFiles, leftTreeMap, rightTreeMap);
		for (ChangeFact cf : cfList) {
			oFile = cf.left;
			nFile = cf.right;
			CommonValue.now_ofile = oFile.getName();
			CommonValue.now_nfile = nFile.getName();
			
//			ClientMethodFinder oFinder = new ClientMethodFinder();
//			ClientMethodFinder nFinder = new ClientMethodFinder();
//			ClientFieldFinder ofFinder = new ClientFieldFinder();
//			ClientFieldFinder nfFinder = new ClientFieldFinder();
			ClientEntityFinder oFinder = new ClientEntityFinder();
			ClientEntityFinder nFinder = new ClientEntityFinder();
//			System.out.print("");
			lTree = findTree(oFile, leftTreeMap);
			if (lTree == null) {//the file is a test file
				toRemove.add(cf);
				continue;
			}
			lTree.accept(oFinder);
			Map<String, ClientMethod> lmap = oFinder.methods;
			Map<String, ClientField> lfmap = oFinder.fields;
			Map<String, ClientClass> lcmap = oFinder.classes;
//			lTree.accept(ofFinder);
//			Map<String, ClientField> lfmap = ofFinder.fields;
			
			rTree = findTree(nFile, rightTreeMap);
			if (rTree == null) { // the file is a test file
				toRemove.add(cf);
				continue;
			}
			rTree.accept(nFinder);
			Map<String, ClientMethod> rmap = nFinder.methods;
			Map<String, ClientField> rfmap = nFinder.fields;
			Map<String, ClientClass> rcmap = nFinder.classes;
//			rTree.accept(nfFinder);
//			Map<String, ClientField> rfmap = nfFinder.fields;
			
			ClientMethod lMethod = null, rMethod = null;
			ClientField lField = null, rField = null;
			ClientClass lClass = null, rClass = null;
			ClientInitializer lInitializer = null, rInitializer = null;
			
			List<Pair<ClientMethod, ClientMethod>> changedMethods = new ArrayList<Pair<ClientMethod, ClientMethod>>();
			List<ChangeMethodData> changedMethodData = new ArrayList<ChangeMethodData>();
			List<ChangeFieldData> changedFieldData = new ArrayList<ChangeFieldData>();
			List<Pair<ClientInitializer, ClientInitializer>> changedInitializers = new ArrayList<Pair<ClientInitializer, ClientInitializer>>();
			
			List<ClientMethod> deletedMethods = new ArrayList<ClientMethod>();
			List<ClientMethod> addedMethods = new ArrayList<ClientMethod>();
			List<Pair<ClientField, ClientField>> changedFields = new ArrayList<Pair<ClientField, ClientField>>();
			List<ClientField> deletedFields = new ArrayList<ClientField>();
			List<ClientField> addedFields = new ArrayList<ClientField>();
			List<ClientClass> addedClasses = new ArrayList<ClientClass>();
			List<ClientClass> deletedClasses = new ArrayList<ClientClass>();
			List<Pair<ClientClass, ClientClass>> changedClasses = new ArrayList<Pair<ClientClass, ClientClass>>();
			System.out.println("Min"+cfList.size());
			for(Entry<SourceCodeEntity, SourceCodeChange> entry : cf.entityChanges.entrySet()) {
				if (entry.getValue().getChangeType() == ChangeType.STATEMENT_PARENT_CHANGE) {
					if (entry.getValue() instanceof Move) {
						Move inter_entry = (Move) entry.getValue();
						String stc = nFinder.SuperClassBinding;
						for (String sti : oFinder.SuperInterfaceBinding) {
							if (sti.contains(inter_entry.getChangedEntity().getUniqueName())
									&& stc.contains(inter_entry.getNewEntity().getUniqueName())) {
								if ((sti.contains(lib_str) && stc.contains(lib_str))
										|| (sti.contains(CommonValue.possible_lib_name2) && stc.contains(CommonValue.possible_lib_name2))) {
									System.out.println("implements -> extends " + inter_entry.getChangedEntity().getUniqueName());
									DatabaseControl data1 = new DatabaseControl();
									int label = data1.insertpattern(
											"implements " + inter_entry.getChangedEntity().getUniqueName(),
											"extends " + inter_entry.getChangedEntity().getUniqueName(),
											CommonValue.common_old_version, CommonValue.common_new_version, "Port Change", "1");
									data1.insertsnippet(inter_entry.getChangedEntity().toString(),
											inter_entry.getNewEntity().toString(), 
											CommonValue.common_project_name, CommonValue.common_commit_number, String.valueOf(label)
											, CommonValue.common_old_version+"-"+CommonValue.common_new_version);
								}
							}
							else if (stc.contains(inter_entry.getChangedEntity().getUniqueName())
									&& sti.contains(inter_entry.getNewEntity().getUniqueName())) {
								if ((sti.contains(lib_str) && stc.contains(lib_str))
										|| (sti.contains(CommonValue.possible_lib_name2) && stc.contains(CommonValue.possible_lib_name2))) {
									System.out.println("extends -> implements " + inter_entry.getChangedEntity().getUniqueName());
									DatabaseControl data1 = new DatabaseControl();
									int label = data1.insertpattern(
											"extends " + inter_entry.getChangedEntity().getUniqueName(),
											"implements " + inter_entry.getChangedEntity().getUniqueName(),
											CommonValue.common_old_version, CommonValue.common_new_version, "Port Change", "1");
									data1.insertsnippet(inter_entry.getChangedEntity().toString(),
											inter_entry.getNewEntity().toString(), 
											CommonValue.common_project_name, CommonValue.common_commit_number, String.valueOf(label)
											, CommonValue.common_old_version+"-"+CommonValue.common_new_version);
								}
							}
						}
//						System.out.println(lTree.(inter_entry.getRootEntity().getUniqueName()));
					}
				}
				entity = entry.getKey();
				if (entity == null) {
					continue;
				}
				sc = entry.getValue();
				EntityType et = entity.getType();
				System.out.print("entity:" + entity);
				if (entity instanceof CompositeEntity) {	
					cEntity = (CompositeEntity)entity;
					oEntity = cEntity.getOldEntity();
					nEntity = cEntity.getNewEntity();					
					String oName = oEntity.getUniqueName();
					String nName = nEntity.getUniqueName();
					System.out.println("fancy: "+ et);
					if (et.equals(JavaEntityType.METHOD)) {
						lMethod = lmap.get(oName);						
						if (lMethod != null) {
							lMethod.ast = lTree;				
						}
						if (lMethod == null) {
							lMethod = lmap.get(String.valueOf(oEntity.getStartPosition()));
							if (lMethod != null) {
								lMethod.ast = lTree;				
							}
						}
						rMethod = rmap.get(nName);
						if (rMethod != null) {
							rMethod.ast = rTree;
						}
						if (rMethod == null) {
							rMethod = rmap.get(String.valueOf(nEntity.getStartPosition()));
							if (rMethod != null) {
								rMethod.ast = rTree;				
							}
						}
						
						if (lMethod == null && rMethod != null) {
							addedMethods.add(rMethod);
						} else if (rMethod == null && lMethod != null) {						
							deletedMethods.add(lMethod);
						} else if (lMethod != null && rMethod != null){
							List<SourceCodeChange> changes = ((FineChangesInMethod)sc).getChanges();
							List<SourceCodeRange> oRanges = ClientMethodMarker.getChangeRanges(ClientMethodMarker.OLD, changes, lMethod);
							List<SourceCodeRange> nRanges = ClientMethodMarker.getChangeRanges(ClientMethodMarker.NEW, changes, rMethod);
							changedMethodData.add(new ChangeMethodData(lMethod, rMethod, oRanges, nRanges));
						}

					} else if (et.equals(JavaEntityType.FIELD)) {
						lField = lfmap.get(oName);
						lField.ast = lTree;
						rField = rfmap.get(nName);
						rField.ast = rTree;
						changedFields.add(new Pair<ClientField, ClientField>(lField, rField));
						//new added by shengzhe
						System.out.println(oEntity.getSourceRange() + " : " + oEntity.getStartPosition() + " " + oEntity.getEndPosition());
						
						List<SourceCodeRange> oRanges = new ArrayList<SourceCodeRange>();
						oRanges.add(new SourceCodeRange(oEntity.getStartPosition(), oEntity.getEndPosition() - oEntity.getStartPosition()));
						List<SourceCodeRange> nRanges = new ArrayList<SourceCodeRange>();
						nRanges.add(new SourceCodeRange(nEntity.getStartPosition(), nEntity.getEndPosition() - nEntity.getStartPosition()));
						changedFieldData.add(new ChangeFieldData(lField, rField, oRanges, nRanges));
					} else {
						System.err.println("Need more process"); //to be extended for Initializers
					}
				} else {	
					ChangeType ct = sc.getChangeType();
					if (et.isMethod()) {						
						switch(ct) {
						case ADDITIONAL_FUNCTIONALITY:			
							rMethod = rmap.get(entity.getUniqueName());	
							if (rMethod == null) break;
							rMethod.ast = rTree;
							lMethod = null;
							addedMethods.add(rMethod);
							break;
						case REMOVED_FUNCTIONALITY:
							lMethod = lmap.get(entity.getUniqueName());
							lMethod.ast = lTree;
							rMethod = null;
							deletedMethods.add(lMethod);
							break;
						default: 
							System.out.println("Need more process");
							break;
						}						
					} else if (et.isField()) {
						switch(ct) {		
						case ADDITIONAL_OBJECT_STATE:
//							System.out.print("");
							rField = getField(rfmap, entity.getUniqueName());
							rField.ast = rTree;
							addedFields.add(rField);
							break;
						case REMOVED_OBJECT_STATE:
							lField = getField(lfmap, entity.getUniqueName());
							lField.ast = lTree;
							deletedFields.add(lField);
							break;
						default: 
							System.out.println("Need more process");
							break;
						}
					} else if (et.isClass()) {						
						switch(ct) {
						case ADDITIONAL_CLASS:
							rClass = getClass(rcmap, entity.getUniqueName());
							rClass.ast = rTree;
							addedClasses.add(rClass);
							break;
						default: 
							System.out.println("need more process");	
						}						
					}
				}
			}
			cf.changedMethods = changedMethods;
			cf.changedMethodData = changedMethodData;
			cf.insertedMethods = addedMethods;
			cf.deletedMethods = deletedMethods;
			cf.changedInitializers = changedInitializers;
			cf.insertedFields = addedFields;
			cf.deletedFields = deletedFields;
			cf.changedFields = changedFields;
			cf.changedFieldData = changedFieldData;
			cf.insertedClasses = addedClasses;
			cf.deletedClasses = deletedClasses;
			cf.changedClasses = changedClasses;
			System.out.println();
		}
		if (!toRemove.isEmpty()) {
			cfList.removeAll(toRemove);
		}
	}
	
	private static Map<String, ASTNode> convertToMap(List<ASTNode> trees) {
		Map<String, ASTNode> map = new HashMap<String, ASTNode>();
		for (ASTNode t : trees) {
			CompilationUnit cu = (CompilationUnit)t;
			map.put(cu.getJavaElement().getElementName(), cu);
		}
		return map;
	}
	
	private static ASTNode findTree(File f, Map<String, ASTNode> treeMap) {
		if (f == null)
			return null;
		return treeMap.get(f.getName());
	}
	
	@Override
	public String toString() {
		return left.getName() + "--" + right.getName();
	}
}
