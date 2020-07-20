package partial.code.grapa.commit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import partial.code.grapa.commit.DependenceGraph;
import partial.code.grapa.delta.graph.AbstractEdit;
import partial.code.grapa.delta.graph.ChangeGraphBuilder;
import partial.code.grapa.delta.graph.DeleteNode;
import partial.code.grapa.delta.graph.GraphEditScript;
import partial.code.grapa.delta.graph.InsertNode;
import partial.code.grapa.delta.graph.UpdateNode;
import partial.code.grapa.delta.graph.data.AbstractNode;
import partial.code.grapa.delta.graph.data.Edge;
import partial.code.grapa.dependency.graph.DataFlowAnalysisEngine;
import partial.code.grapa.dependency.graph.StatementEdge;
import partial.code.grapa.dependency.graph.StatementNode;
import partial.code.grapa.mapping.AstTreeComparator;
import partial.code.grapa.mapping.ClientMethod;
import partial.code.grapa.tool.GraphUtil;
import partial.code.grapa.version.detect.VersionDetector;
import partial.code.grapa.version.detect.VersionPair;
import ca.mcgill.cs.swevo.ppa.ui.PPAUtil;

import com.ibm.wala.cast.java.translator.jdt.JDTSourceModuleTranslator;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.collections.Pair;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.vt.cs.graph.GraphDotUtil;
import edu.vt.cs.graph.GraphUtil2;
import edu.vt.cs.graph.SourceMapper;

public class CommitComparator {

	private String pName;
	private String elementListDir;
	private String commitDir;
	private String libDir;
	private String j2seDir;
	//modified by Na Meng to public
	public VersionDetector detector;
	public static String resultDir;
	
	private DataFlowAnalysisEngine leftEngine;
	private DataFlowAnalysisEngine rightEngine;
	private String otherLibDir;
	private String exclusionsFile;
	//modified by Na meng to public
	public static String bugName;
	private Map<IFile, CompilationUnit> leftMap = null;
	private Map<IFile, CompilationUnit> rightMap = null;

	private boolean bVisited = false;		
	
	public void run() {
		// TODO Auto-generated method stub
		detector = new VersionDetector();
		detector.setProject(pName);
		detector.readElementList(elementListDir);
		File d = new File(commitDir); 
		for(File c:d.listFiles()){
			if(c.isDirectory()){
				bugName = c.getName();
				System.out.println(bugName);
				if(bVisited){
//					analyzeCommit(c);
//					break;
				}
				if(bugName.compareTo("2f5f0c2_CASSANDRA-1804")==0){
					bVisited = true;
				}
			}
		}
		System.out.println("Done!");
	}

	public void clear() {
		try {
			leftEngine.clearAnalysisScope();
			rightEngine.clearAnalysisScope();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	private void writeToLog(String version, String name) {
		// TODO Auto-generated method stub
		File file = new File(this.resultDir+"/bugNames.txt");
		FileWriter fw = null;
		BufferedWriter writer = null;
		try{
	         fw = new FileWriter(file, true);
	         writer = new BufferedWriter(fw);
	         writer.write(version+"\t"+name);
	         writer.newLine();
	         writer.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
                writer.close();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}


	public void setDotExe(String dotExe) {
		GraphUtil.dotExe = dotExe;
	}
	
	//added by nameng[
	public boolean initializeAnalysis(ArrayList<File> oldfiles, ArrayList<File> newfiles) {
		boolean bLeftSuccess = false;
		try {
			leftEngine = new DataFlowAnalysisEngine();
			leftEngine.setExclusionsFile(this.exclusionsFile);
			leftEngine.addtoScope("left", null, j2seDir, libDir, otherLibDir, null, oldfiles);
			leftEngine.initClassHierarchy();
			leftMap = new HashMap<IFile, CompilationUnit>(JDTSourceModuleTranslator.fileToCu);
			if (!leftMap.isEmpty()) {
				bLeftSuccess = true;
			}			
		} catch (Exception e) {
			bLeftSuccess = false;
		}
		if (!bLeftSuccess) {
			System.out.println("Fail to parse the left side project");
			return bLeftSuccess;
		}
		boolean bRightSuccess = false;
		try{
			rightEngine = new DataFlowAnalysisEngine();
			rightEngine.setExclusionsFile(this.exclusionsFile);	
			rightEngine.addtoScope("right", null, j2seDir, libDir, otherLibDir, null, newfiles);
			rightEngine.initClassHierarchy();
			rightMap = new HashMap<IFile, CompilationUnit>(JDTSourceModuleTranslator.fileToCu);
			if (!rightMap.isEmpty()) {
				bRightSuccess = true;
			}
		}catch(Exception e){
			e.printStackTrace();
			bRightSuccess = false;
		}
		if (!bRightSuccess) {
			System.out.println("Fail to parse the right side project");
			return bRightSuccess;
		}
		String dirName = resultDir + bugName + "/";
		File dir = new File(dirName);
		if (!dir.exists()) {
			  dir.mkdirs();
		}
		return true;
	}//]
	
	// added by nameng[
	public boolean initializeAnalysis(VersionPair pair, ArrayList<File> oldfiles, ArrayList<File> newfiles) {
		boolean bLeftSuccess = false;
		for(String oldVersion:pair.left.versions){
			System.out.print(oldVersion+",");
			try{
				leftEngine = new DataFlowAnalysisEngine();
				leftEngine.setExclusionsFile(this.exclusionsFile);
				leftEngine.addtoScope("left", pair.left.pTable, j2seDir, libDir, otherLibDir, oldVersion, oldfiles);
				leftEngine.initClassHierarchy();
				leftMap = new HashMap<IFile, CompilationUnit>(JDTSourceModuleTranslator.fileToCu);
				bLeftSuccess = true;
			}catch(Exception e){
				e.printStackTrace();
				bLeftSuccess = false;
			}
			if(bLeftSuccess){
				break;
			}
		}
		
		if(!bLeftSuccess){
			System.out.println("Fail to parse the left side project");
		}
		
		boolean bRightSuccess = false;
		System.out.print("->");
		
		for(String newVersion:pair.right.versions){
			System.out.print(newVersion+",");
			try{
				rightEngine = new DataFlowAnalysisEngine();
				rightEngine.setExclusionsFile(this.exclusionsFile);	
				rightEngine.addtoScope("right", pair.right.pTable, j2seDir, libDir, otherLibDir,  newVersion, newfiles);
				rightEngine.initClassHierarchy();
				rightMap = new HashMap<IFile, CompilationUnit>(JDTSourceModuleTranslator.fileToCu);
				bRightSuccess = true;
			}catch(Exception e){
				e.printStackTrace();
				bRightSuccess = false;
			}
			if(bRightSuccess){
				break;
			}
		}
		
		if(!bRightSuccess){
			System.out.println("Fail to parse the right side project");
		}
		boolean success = bLeftSuccess & bRightSuccess;
		if (success) {
			String dirName = resultDir + bugName + "/";
			File dir = new File(dirName);
			if (!dir.exists()) {
				  dir.mkdirs();
			}
		}
		return success;
	}	
	
	public DataFlowAnalysisEngine getLeftAnalysisEngine() {
		return leftEngine;
	}
	
	public Map<IFile, CompilationUnit> getLeftTreeMap() {
		return leftMap;
	}

	public DataFlowAnalysisEngine getRightAnalysisEngine() {
		return rightEngine;
	}
	
	public Map<IFile, CompilationUnit> getRightTreeMap() {
		return rightMap;
	}
	//]end added by nameng

	
	private void compareGraph2(DirectedSparseGraph<StatementNode, StatementEdge> leftGraph,
			IR lir, DirectedSparseGraph<StatementNode, StatementEdge> rightGraph, IR rir) {		
		Collection<StatementNode> vertices = leftGraph.getVertices();
		for (StatementNode sn : vertices) {
			Statement st = sn.statement;
			Collection<StatementEdge> edges = leftGraph.getEdges();
			for (StatementEdge se : edges) {
				System.out.print(se.from);
				System.out.print("->");
				System.out.println(se.to);
			}
		}
	}	
	
	private void  writeDependencyGraph(
			DirectedSparseGraph<StatementNode, StatementEdge> graph, IR lir,
			IR rir, String filename) {
		// TODO Auto-generated method stub
		GraphUtil.writeDeltaGraphXMLFile(graph, rir, rir, filename);
		GraphUtil.writePdfDeltaGraph(graph, lir, rir, filename);
	}

	private ArrayList<DirectedSparseGraph<StatementNode, StatementEdge>> compareGraphs(
			DirectedSparseGraph<StatementNode, StatementEdge> leftGraph,
			IR lir, DirectedSparseGraph<StatementNode, StatementEdge> rightGraph, IR rir) {
		// TODO Auto-generated method stub
		GraphEditScript script = new GraphEditScript(leftGraph, lir, rightGraph, rir);

		ArrayList<AbstractEdit> edits = script.extractChanges();
		for(AbstractEdit edit:edits){
			if(edit instanceof UpdateNode||edit instanceof DeleteNode||edit instanceof InsertNode)
			System.out.println(edit);
		}
		System.out.println("---------------------------------------------");
		
		ChangeGraphBuilder builder = new ChangeGraphBuilder(leftGraph, lir, rightGraph, rir);
		ArrayList<DirectedSparseGraph<StatementNode, StatementEdge>> graphs = builder.extractChangeGraph();
		return graphs;
	}


	public void setProject(String name) {
		// TODO Auto-generated method stub
		pName = name;	
	}

	public void setElementListDir(String eld) {
		// TODO Auto-generated method stub
		elementListDir = eld+pName+"/";
	}

	public void setCommitDir(String cd) {
		// TODO Auto-generated method stub
		commitDir = cd+pName+"/";
	}

	public void setLibDir(String dir) {
		// TODO Auto-generated method stub
		libDir = dir+pName+"/";
	}

	public void setLibDirForMigration(String dir) {
		libDir = dir;
	}
	
	public void setJ2seDir(String dir) {
		// TODO Auto-generated method stub
		j2seDir = dir;
	}

	public void setResultDir(String dir) {
		// TODO Auto-generated method stub
		resultDir = dir+pName+"/";
	}

	public void setOtherLibDir(String dir) {
		// TODO Auto-generated method stub
		otherLibDir = dir+pName+"/";
	}

	public void setExclusionFile(String file) {
		// TODO Auto-generated method stub
		exclusionsFile = file;
	}

}
