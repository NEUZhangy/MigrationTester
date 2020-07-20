package edu.vt.cs.changes;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;

import partial.code.grapa.commit.CommitComparator;
import partial.code.grapa.commit.DependenceGraph;
import partial.code.grapa.mapping.ClientMethod;
import partial.code.grapa.tool.FileUtils;
import partial.code.grapa.version.detect.CodeElementVisitor;
import partial.code.grapa.version.detect.VersionDetector;
import partial.code.grapa.version.detect.VersionPair;
import partial.code.grapa.version.detect.Versions;
import ch.uzh.ifi.seal.changedistiller.distilling.refactoring.AbstractRefactoringHelper;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;

import com.ibm.wala.util.collections.Pair;

import edu.vt.cs.append.CommonValue;
import edu.vt.cs.append.CompositeEntity;
import edu.vt.cs.grapa.extension.ClientMethodFinder;

public class CommitComparatorClient {

	public static boolean needExtraLibs = true; 
	
	CommitComparator comparator = null;

	//added by nameng
	public CommitComparatorClient(String projName, String lName) {
		comparator = new CommitComparator();
		comparator.detector = new VersionDetector();
		comparator.detector.setProject(projName);
		comparator.setProject(projName);
		comparator.setJ2seDir("/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/");
		comparator.setLibDirForMigration(CommonValue.dataspace+"lib/" + lName + "/");
		comparator.setResultDir(CommonValue.dataspace+"output_grapa_results/");
		comparator.setDotExe("/usr/bin/dot");
	}
		
    public void analyzeCommitForMigration(List<ChangeFact> cfList, String folderName,
    		String oldVer, String newVer) {
    	VersionPair pair = new VersionPair();
    	Set<File> oldSet = new HashSet<File>();
		Set<File> newSet = new HashSet<File>();
		for (ChangeFact c : cfList) {
			if (c.left != null) {
				oldSet.add(c.left);
			}
			if (c.right != null) {
				newSet.add(c.right);
			}
			System.out.println("this is:" + oldSet.toString() + "\n" + newSet.toString());
			
		}    	
    	pair.left = createVersions(oldSet, oldVer);
    	pair.right = createVersions(newSet, newVer);
    	
    	ArrayList<File> oldfiles = new ArrayList<File>();
		ArrayList<File> newfiles = new ArrayList<File>();
    	
    	for(File file : oldSet){
			if(!pair.left.testFiles.contains(file)){
				oldfiles.add(file);
			}
		}
		for (File file : newSet) {
			if(!pair.right.testFiles.contains(file)) {
				newfiles.add(file);
			}
		}
		boolean success = comparator.initializeAnalysis(pair, oldfiles, newfiles);
		System.out.println("pre" + cfList.size());
		if (success) {
			ChangeFact.refineChanges(cfList, oldfiles, newfiles, 
					new ArrayList<ASTNode>(comparator.getLeftTreeMap().values()), 
					new ArrayList<ASTNode>(comparator.getRightTreeMap().values()));

			
//			ChangeGrouper grouper = new ChangeGrouper();
//			grouper.groupChanges(cfList, comparator);
//			System.out.println("mid"+cfList.size());
			MigrationChangeDetector detector = new MigrationChangeDetector();
//			System.out.println("later"+cfList.size());
			detector.detect(cfList, comparator);
		}		
    }
    
    private Versions createVersions(Set<File> fSet,  String ver) {
    	Versions v = new Versions();
    	v.versions.add(ver);
    	for (File f : fSet) {
    		String sourcecode = FileUtils.getContent(f); //removed by nameng
    		ASTParser parser = ASTParser.newParser(AST.JLS8);
    		parser.setSource(sourcecode.toCharArray());
    		CompilationUnit tree = (CompilationUnit) parser.createAST(null);
    		CodeElementVisitor visitor = new CodeElementVisitor();
			tree.accept(visitor);
			if(visitor.bUnit){
				v.testFiles.add(f);
			}else{
				PackageDeclaration p = tree.getPackage();
				System.out.println(p);
				String paName = "";
				if(p!=null){
					System.out.println(p.getName());
					System.out.println(p.getName().getFullyQualifiedName());
					
					paName = p.getName().getFullyQualifiedName();
				}
				v.pTable.put(f, paName);
			}
    	}
    	return v;
    }
	
//	private List<Pair<ClientMethod, ClientMethod>> getChangedMethods(List<ChangeFact> cfList,
//			List<File> oldfiles, List<File> newfiles) {
//		List<Pair<ClientMethod, ClientMethod>> result = new ArrayList<Pair<ClientMethod, ClientMethod>>();
//		List<ASTNode> leftTrees = comparator.getLeftTrees();
//		Map<String, ASTNode> leftTreeMap = convertToMap(leftTrees);
//		List<ASTNode> rightTrees = comparator.getRightTrees();
//		Map<String, ASTNode> rightTreeMap = convertToMap(rightTrees);
//		SourceCodeEntity entity = null;
//		CompositeEntity cEntity = null;
//		File oFile = null, nFile = null;
//		SourceCodeEntity oEntity = null, nEntity = null;
//		ASTNode lTree = null, rTree = null;		
//		
//			for (ChangeFact cf : cfList) {			
//				oFile = cf.left;
//				nFile = cf.right;
//				ClientMethodFinder oFinder = new ClientMethodFinder();
//				ClientMethodFinder nFinder = new ClientMethodFinder();
//				lTree = findTree(oFile, leftTreeMap);
//				lTree.accept(oFinder);
//				Map<String, ClientMethod> lmap = oFinder.methods;
//				
//				rTree = findTree(nFile, rightTreeMap);
//				rTree.accept(nFinder);
//				Map<String, ClientMethod> rmap = nFinder.methods;
//				ClientMethod lMethod = null, rMethod = null;
//				for(Entry<SourceCodeEntity, SourceCodeChange> entry : cf.entityChanges.entrySet()) {
//					entity = entry.getKey();
//					if (entity instanceof CompositeEntity) {						
//						cEntity = (CompositeEntity)entity;
//						oEntity = cEntity.getOldEntity();
//						nEntity = cEntity.getNewEntity();
//						String oName = oEntity.getUniqueName();
//						String nName = nEntity.getUniqueName();	
//						lMethod = lmap.get(oName);
//						lMethod.ast = lTree;						
//						rMethod = rmap.get(nName);
//						rMethod.ast = rTree;
//						result.add(new Pair<ClientMethod, ClientMethod>(lMethod, rMethod));
//					}
//				}
//		}
//		return result;
//	}
	
//	private static Map<String, ASTNode> convertToMap(List<ASTNode> trees) {
//		Map<String, ASTNode> map = new HashMap<String, ASTNode>();
//		for (ASTNode t : trees) {
//			CompilationUnit cu = (CompilationUnit)t;
//			map.put(cu.getJavaElement().getElementName(), cu);
//		}
//		return map;
//	}
//	
//	private static ASTNode findTree(File f, Map<String, ASTNode> treeMap) {
//		return treeMap.get(f.getName());
//	}
}
