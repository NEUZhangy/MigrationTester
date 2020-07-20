package edu.vt.cs.changes.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import com.ibm.wala.cast.java.ssa.AstJavaInvokeInstruction;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;

import partial.code.grapa.dependency.graph.StatementEdge;
import partial.code.grapa.dependency.graph.StatementNode;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.vt.cs.diffparser.util.PatternMatcher;
import edu.vt.cs.graph.GraphConvertor;

public class Subgraph extends DirectedSparseGraph<SNode, SEdge>{
		
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Map<String, DataNode> dataNodes = null;
	private Map<Integer, InsnNode> insnNodes = null;
	private List<SNode> allNodes = null;
	
	private Set<SNode> markedSources = null;
	private Set<SNode> markedDests = null;
	
	private int index = 0;
	private SymbolTable symTab = null;
	
	public Subgraph(SymbolTable symTab) {
		this.symTab = symTab;
		index = 0;
		dataNodes = new HashMap<String, DataNode>();
		insnNodes = new HashMap<Integer, InsnNode>();
		allNodes = new ArrayList<SNode>();
	}
	
	public void addEdge(SNode n1, SNode n2) {
		if (n1 == null || n2 == null) {
//			System.out.println("cannot addEdge:" + n1 +"->"+ n2);
			return;
		}
		SEdge e = new SEdge(n1.id, n2.id);
		this.addEdge(e, n1, n2);
	}
	
	public void addEdges(InsnNode insnNode, SSAInstruction insn, List<String> defs, List<String> uses) {
		DataNode n = null;
		int exception = -1;
		if (insn instanceof AstJavaInvokeInstruction) {
			exception = ((AstJavaInvokeInstruction)insn).getException();
		}
		if (insn instanceof SSAGetInstruction) {
			SSAGetInstruction get = ((SSAGetInstruction)insn);
			FieldReference fRef = get.getDeclaredField();
			String f = fRef.getName().toString();
			n = getDataNode(f, exception);
			n.fRef = fRef;
			this.addEdge(n, insnNode);
		} else if (insn instanceof SSAPutInstruction) {
			SSAPutInstruction put = ((SSAPutInstruction)insn);
			FieldReference fRef = put.getDeclaredField();
			String f = fRef.getName().toString();
			n = getDataNode(f, exception);
			n.fRef = fRef;
			System.out.println(insnNode + " " + n);
			this.addEdge(insnNode, n);
		} 
		for (String s : defs) {
			n = getDataNode(s, exception);
			this.addEdge(insnNode, n);
		}
		for (String s : uses) {			
			n = getDataNode(s, exception);
			this.addEdge(n, insnNode);
		}
	}
	
	public DataNode getDataNode(String s, int exception) {
		DataNode dNode = dataNodes.get(s);
		if (dNode == null) {
			dNode = new DataNode(s, index++);
			allNodes.add(dNode);
			
			this.addVertex(dNode);
			dataNodes.put(s, dNode);
			// added by shengzhe July, 2017
			if (s!=null && PatternMatcher.vPatternSSA.matcher(s).matches()) {
				int vNum = Integer.parseInt(s.substring(1));
				if (symTab.isConstant(vNum)) {
					dNode.enableConstant();
				} else if (vNum == exception) {
					dNode.enableException();
				}	
			}
		}
		return dNode;
	}
	
	public InsnNode getInsnNode(String s, int it, int insnIndex) {
		InsnNode iNode = insnNodes.get(insnIndex);
		if (iNode == null) {
			iNode = new InsnNode(s, index++, it, insnIndex);
			allNodes.add(iNode);
			
			this.addVertex(iNode);
			insnNodes.put(insnIndex, iNode);
		}
		return iNode;
	}
	
	public Set<SNode> getMarkedSources() {
		return markedSources;
	}
	
	public Set<SNode> getMarkedDests() {
		return markedDests;
	}
	
	public void mark(Set<Integer> instIndexes) {		
		SNode s = null;
		markedSources = new HashSet<SNode>();
		markedDests = new HashSet<SNode>();
		Set<SNode> markedNodes = new HashSet<SNode>();
		for (SNode n : allNodes) {
			if (n.type == SNode.INST_NODE ) {
				InsnNode iNode = (InsnNode)n;
				if (instIndexes.contains(iNode.getInstIndex())) {
					n.enableMark();
					markedNodes.add(n);
					for (SEdge e : this.getInEdges(n)) {
						e.enableMark();
						s = allNodes.get(e.srcIdx);
						if (s.getType() == SNode.DATA_NODE) {
							s.enableMark();
							markedNodes.add(s);
						}						
					}
					for (SEdge e : this.getOutEdges(n)) {
						e.enableMark();
						s = allNodes.get(e.dstIdx);
						if (s.getType() == SNode.DATA_NODE) {
							s.enableMark();
							markedNodes.add(s);
						}
					}
				}				
			}
		}
		boolean containMarked = false;
		for (SNode n : markedNodes) {
//			System.out.println("here n:"+ n);
//			if (n == null) {
//				System.out.println("Subgraph MarkedNodes n: is null!!!");
//				continue;
//			}
			System.out.print("");
			containMarked = false;
			for (SEdge e : this.getInEdges(n)) {
				if (e.mark) {
					containMarked = true;
					break;
				}
			}
			if (!containMarked) {
				
				if (n.getType() == SNode.INST_NODE && ((InsnNode)n).getInstType() == InstType.NEW) {
					s = allNodes.get(this.getOutEdges(n).iterator().next().dstIdx);// the temporary instance
					s = allNodes.get(this.getOutEdges(s).iterator().next().dstIdx);// the initializer
					if (this.getInEdges(s).size() == 1) {
						markedSources.add(n);
					}					
				} else {
					markedSources.add(n);
				}
			}
			containMarked = false;
//			System.out.println("this n:" + n);
			for (SEdge e : this.getOutEdges(n)) {
				if (e.mark) {
					containMarked = true;
					break;
				}
			}
			
			if (n.label == null)
				continue;
			if (!containMarked) {
				Matcher matcher = PatternMatcher.vPatternSSA.matcher(n.label);
				if (!matcher.matches()) {
					markedDests.add(n);
				}				
			} else {
				if (n.getType() == SNode.DATA_NODE) {
					Matcher matcher = PatternMatcher.vPatternSSA.matcher(n.label);
					if (!matcher.matches()) {
						Collection<SEdge> edges = this.getInEdges(n);
						if (edges.size() == 1 && this.getOutEdges(n).size() == 1) {
							s = allNodes.get(edges.iterator().next().srcIdx);
							if (s.getType() == SNode.INST_NODE && ((InsnNode)s).getInstType() == InstType.NEW) {
								markedDests.add(n);
							}
						}
					}
				}
			}
		}		
	}	
}
