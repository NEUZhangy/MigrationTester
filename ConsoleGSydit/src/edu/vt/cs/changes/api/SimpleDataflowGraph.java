package edu.vt.cs.changes.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.vt.cs.diffparser.util.PatternMatcher;

public class SimpleDataflowGraph extends DirectedSparseGraph<SNode, SEdge>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3626590504780187519L;
	private Map<String, DataNode> dataNodes = null;
	private Map<Integer, StmtNode> stmtNodes = null;
	private int index = 0;
	private List<SNode> allNodes = null;
	public SimpleDataflowGraph() {
		index = 0;
		dataNodes = new HashMap<String, DataNode>();
		stmtNodes = new HashMap<Integer, StmtNode>();
		allNodes = new ArrayList<SNode>();
	}
	
	public void addEdge(SNode n1, SNode n2) {
		SEdge e = new SEdge(n1.id, n2.id);
		this.addEdge(e, n1, n2);
	}
	
	
	public DataNode getDataNode(String s) {
		DataNode dNode = dataNodes.get(s);
		if (dNode == null) {
			dNode = new DataNode(s, index++);
			allNodes.add(dNode);
			
			this.addVertex(dNode);
			dataNodes.put(s, dNode);			
		}
		return dNode;
	}
	
	public StmtNode getStmtNode(String s, int sIndex) {
		StmtNode iNode = stmtNodes.get(sIndex);
		if (iNode == null) {
			iNode = new StmtNode(s, index++, sIndex);
			allNodes.add(iNode);	
			
			this.addVertex(iNode);
			stmtNodes.put(iNode.id, iNode);
		}
		return iNode;
	}
	
	public Map<Integer, StmtNode> getAllStmtNodes() {
		return stmtNodes;
	}
}
