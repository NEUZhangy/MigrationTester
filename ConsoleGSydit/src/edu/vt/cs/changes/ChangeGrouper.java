package edu.vt.cs.changes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import partial.code.grapa.commit.CommitComparator;
import partial.code.grapa.commit.DependenceGraph;
import partial.code.grapa.dependency.graph.DataFlowAnalysisEngine;
import partial.code.grapa.mapping.ClientMethod;

import com.ibm.wala.cast.java.ssa.AstJavaInvokeInstruction;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.vt.cs.graph.ChangeGraphUtil;
import edu.vt.cs.graph.ClientClass;
import edu.vt.cs.graph.ClientField;
import edu.vt.cs.graph.GraphUtil2;
import edu.vt.cs.graph.ReferenceEdge;
import edu.vt.cs.graph.ReferenceNode;

public class ChangeGrouper {

	
	public void groupBasedOnCallGraph(
			List<Pair<ClientMethod, ClientMethod>> mps,
			List<Pair<DependenceGraph, DependenceGraph>> list) {
		List<Set<ClientMethod>> lg = new ArrayList<Set<ClientMethod>>();
		List<Set<ClientMethod>> rg = new ArrayList<Set<ClientMethod>>();
		List<ClientMethod> lms = new ArrayList<ClientMethod>();
		List<ClientMethod> rms = new ArrayList<ClientMethod>();
		
		List<CallGraph> lcgs = new ArrayList<CallGraph>();
		List<CallGraph> rcgs = new ArrayList<CallGraph>();
		System.out.print("");
		for (int i = 0; i < list.size(); i++) {
			Pair<ClientMethod, ClientMethod> mp = mps.get(i);
			lms.add(mp.fst);
			rms.add(mp.snd);
			Pair<DependenceGraph, DependenceGraph> p = list.get(i);
			lcgs.add(p.fst.sdg.getCallGraph());
			rcgs.add(p.snd.sdg.getCallGraph());
		}
		
		lg = groupMethods(lms, lcgs);
		rg = groupMethods(rms, rcgs);
		
		
		List<Set<Pair<ClientMethod, ClientMethod>>> groups = new ArrayList<Set<Pair<ClientMethod, ClientMethod>>>();
		for (int i = 0; i < lg.size(); i++) {
			Set<ClientMethod> mSet1 = lg.get(i);
			Set<Pair<ClientMethod, ClientMethod>> pairSet = new HashSet<Pair<ClientMethod, ClientMethod>>();
			Set<Integer> indexSet = new HashSet<Integer>();
			for (ClientMethod cm : mSet1) {
				int idx = lms.indexOf(cm);
				pairSet.add(mps.get(idx));
				indexSet.add(idx);
			}
			Set<Integer> indexSet2 = new HashSet<Integer>();
			for (int j = 0; j < rg.size(); j++) {
				Set<ClientMethod> mSet2 = rg.get(j);
				for (ClientMethod cm : mSet2) {
					int idx = rms.indexOf(cm);
					indexSet2.add(idx);
				}
				Set<Integer> copy = new HashSet<Integer>(indexSet2);
				copy.retainAll(indexSet);
				if (!copy.isEmpty()) {
					for (Integer idx : indexSet2) {
						pairSet.add(mps.get(idx));
					}
				}
			}
			groups.add(pairSet);
		}
	}
	
	
	public void groupChanges(List<ChangeFact> cfList, CommitComparator comparator) {
		Set<MethodReference> oMRefs = new HashSet<MethodReference>();
		Set<ClientMethod> oldMethods = new HashSet<ClientMethod>();
		Set<MethodReference> nMRefs = new HashSet<MethodReference>();
		Set<ClientMethod> newMethods = new HashSet<ClientMethod>();
		
		Set<FieldReference> oFRefs = new HashSet<FieldReference>();
		Set<FieldReference> nFRefs = new HashSet<FieldReference>();
		
		Set<TypeReference> oCRefs = new HashSet<TypeReference>();
		Set<TypeReference> nCRefs = new HashSet<TypeReference>();
		
		DataFlowAnalysisEngine leftEngine = comparator.getLeftAnalysisEngine();
		DataFlowAnalysisEngine rightEngine = comparator.getRightAnalysisEngine();	
		
		ClientMethod m1 = null, m2 = null;
		for (ChangeFact cf : cfList) {
			for (Pair<ClientMethod, ClientMethod> p : cf.changedMethods) {
				m1 = p.fst;
				m2 = p.snd;
				leftEngine.getOrCreateMethodReference(m1);
				rightEngine.getOrCreateMethodReference(m2);
				oMRefs.add(m1.mRef);
				nMRefs.add(m2.mRef);
				oldMethods.add(m1);
				newMethods.add(m2);
			}
			for (ClientMethod cm : cf.insertedMethods) {
				rightEngine.getOrCreateMethodReference(cm);
				nMRefs.add(cm.mRef);				
			}
			newMethods.addAll(cf.insertedMethods);
			for (ClientMethod cm : cf.deletedMethods) {
				leftEngine.getOrCreateMethodReference(cm);
				oMRefs.add(cm.mRef);
			}
			oldMethods.addAll(cf.deletedMethods);
			ClientField f1 = null, f2 = null;
			for(Pair<ClientField, ClientField> p : cf.changedFields) {
				f1 = p.fst;
				f2 = p.snd;
				leftEngine.getOrCreateFieldReference(f1);
				rightEngine.getOrCreateFieldReference(f2);
				oFRefs.add(f1.fRef);
				nFRefs.add(f2.fRef);
			}
			for (ClientField f : cf.insertedFields) {
				rightEngine.getOrCreateFieldReference(f);
				nFRefs.add(f.fRef);
			}
			for (ClientField f : cf.deletedFields) {
				leftEngine.getOrCreateFieldReference(f);
				oFRefs.add(f.fRef);
			}
			
			ClientClass c1 = null, c2 = null;
			for (Pair<ClientClass, ClientClass> p : cf.changedClasses) {
				c1 = p.fst;
				c2 = p.snd;
				leftEngine.getOrCreateTypeReference(c1);
				rightEngine.getOrCreateTypeReference(c2);
				oCRefs.add(c1.tRef);
				nCRefs.add(c2.tRef);
			}
			for (ClientClass c : cf.insertedClasses) {
				rightEngine.getOrCreateTypeReference(c);
				nCRefs.add(c.tRef);
			}
			for (ClientClass c: cf.deletedClasses) {
				leftEngine.getOrCreateTypeReference(c);
				oCRefs.add(c.tRef);
			}
		}
		
		List<DirectedSparseGraph<ReferenceNode, ReferenceEdge>> lGraphs = new 
				ArrayList<DirectedSparseGraph<ReferenceNode, ReferenceEdge>>();
		
		relateChangesBasedOnIR(lGraphs, leftEngine, oldMethods, oMRefs, oFRefs, oCRefs);
		lGraphs = mergeChanges(lGraphs);			
		String dir = CommitComparator.resultDir + CommitComparator.bugName + "/";
		for (int i = 0; i < lGraphs.size(); i++) {
			ChangeGraphUtil.writeRelationGraph(lGraphs.get(i), dir + "left_" + i);
		}
		
		List<DirectedSparseGraph<ReferenceNode, ReferenceEdge>> rGraphs = new 
				ArrayList<DirectedSparseGraph<ReferenceNode, ReferenceEdge>>();
		relateChangesBasedOnIR(rGraphs, rightEngine, newMethods, nMRefs, nFRefs, nCRefs);
		rGraphs = mergeChanges(rGraphs);
		for (int i = 0; i < rGraphs.size(); i++) {
			ChangeGraphUtil.writeRelationGraph(rGraphs.get(i), dir + "right_" + i);
		}
		if(lGraphs.isEmpty() && rGraphs.isEmpty()) {
			System.out.println("No relationship");
		}		
//		combineChanges(lGraphs, rGraphs);
	}

	
	private List<DirectedSparseGraph<ReferenceNode, ReferenceEdge>> mergeChanges(List<DirectedSparseGraph<ReferenceNode, ReferenceEdge>> graphs) {
		List<DirectedSparseGraph<ReferenceNode, ReferenceEdge>> result = new ArrayList<DirectedSparseGraph<ReferenceNode, ReferenceEdge>>();
	
		DirectedSparseGraph<ReferenceNode, ReferenceEdge> g1 = null, g2 = null;
		boolean isChanged = true;
		Set<ReferenceNode> nSet1 = null, nSet2 = null;
		while(isChanged) {
			isChanged = false;
			for (int i = 0; i < graphs.size() - 1; i++) {
				g1 = graphs.get(i);
				if (g1.getVertexCount() == 0)
					continue;
				nSet1 = new HashSet<ReferenceNode>(g1.getVertices());
				for (int j = i + 1; j < graphs.size(); j++) {
					g2 = graphs.get(j);
					if (g2.getVertexCount() == 0)
						continue;
					for (ReferenceNode n : nSet1) {
						if (g2.containsVertex(n)) {
							isChanged = true;
							break;
						}
					}
					if (isChanged) {
						for (ReferenceNode n : g2.getVertices()) {
							if (!g1.containsVertex(n)) {								
								g1.addVertex(n);
							}
						}
						for (ReferenceEdge e : g2.getEdges()) {
							if (!g1.containsEdge(e)) {
								g1.addEdge(e, e.from, e.to);
							}
						}
						nSet2 = new HashSet<ReferenceNode>(g2.getVertices());
						for (ReferenceNode n : nSet2) {
							g2.removeVertex(n);
						}
						break;
					}
				}
			}
		}
		for (int i = 0; i < graphs.size(); i++) {
			g1 = graphs.get(i);
			if (g1.getVertexCount() != 0) {
				result.add(g1);
				for (ReferenceEdge e : g1.getEdges()) {
					System.out.println(e);
				}
			}
		}
		return result;
	}
	
	private void relateChangesBasedOnIR(List<DirectedSparseGraph<ReferenceNode, ReferenceEdge>> graphs,  
			DataFlowAnalysisEngine engine, Set<ClientMethod> methods, 
			Set<MethodReference> mRefs, Set<FieldReference> fRefs, 
			Set<TypeReference> cRefs) {
		for (ClientMethod m : methods) {
			DirectedSparseGraph<ReferenceNode, ReferenceEdge> graph = new DirectedSparseGraph<ReferenceNode, ReferenceEdge>();
			graphs.add(graph);
			ReferenceNode root = new ReferenceNode(m.mRef);
			engine.getCFGBuilder(m);
			IR ir = engine.getCurrentIR();
			if (ir == null)
				continue;
			for (SSAInstruction instr : ir.getInstructions()) {
				if (instr == null)
			        continue;
				if (!fRefs.isEmpty() && instr instanceof SSAFieldAccessInstruction) {
					SSAFieldAccessInstruction sInstr = (SSAFieldAccessInstruction)instr;
					FieldReference fRef = sInstr.getDeclaredField();
					if (fRefs.contains(fRef)) {
						ReferenceNode n = new ReferenceNode(fRef);
						ReferenceEdge edge = graph.findEdge(root, n);
						if (edge == null) {
							edge = new ReferenceEdge(root, n, ReferenceEdge.FIELD_ACCESS);
							graph.addEdge(edge, root, n);
						}								
						edge.increaseCount();						
					}
				} 
				if (!mRefs.isEmpty() && (instr instanceof SSAAbstractInvokeInstruction)) {
					MethodReference mRef = ((SSAAbstractInvokeInstruction)instr).getDeclaredTarget();
					if (mRefs.contains(mRef)) {
						ReferenceNode n = new ReferenceNode(mRef);						
						ReferenceEdge edge = graph.findEdge(root, n);
						if (edge == null) {
							edge = new ReferenceEdge(root, n, ReferenceEdge.METHOD_INVOKE);
							graph.addEdge(edge, root, n);
						}								
						edge.increaseCount();
					}
				}
			}
		}
	}
	

	
	
	private List<Set<ClientMethod>> groupMethods(List<ClientMethod> ms, List<CallGraph> cgs) {
		System.out.print("");
		List<Set<ClientMethod>> g = new ArrayList<Set<ClientMethod>>();
		for (int i = 0; i < ms.size(); i++) {
			CallGraph cg = cgs.get(i);
			ClientMethod m = ms.get(i);
			Set<CGNode> cgnodes = cg.getNodes(m.mRef);
			Set<ClientMethod> groupedMethods = new HashSet<ClientMethod>();
			for (int j = i + 1; j < ms.size(); j++) {
				ClientMethod m2 = ms.get(j);
				Set<CGNode> cgnodes2 = cg.getNodes(m2.mRef);
				for (CGNode n1 : cgnodes) {
					for (CGNode n2 : cgnodes2) {
						if (cg.hasEdge(n1, n2) || cg.hasEdge(n2, n1)) {
							groupedMethods.add(ms.get(i));
							groupedMethods.add(ms.get(j));
							break;
						}
					}
				}
			}
			g.add(groupedMethods);
		}
		boolean isChanged = true;
		while (isChanged) { // merge groups as much as we can 
			isChanged = false;
			for (int i = 0; i < g.size(); i++) {
				Set<ClientMethod> mSet1 = g.get(i);
				for (int j = i + 1; j < g.size(); j++) {
					Set<ClientMethod> mSet2 = g.get(j);
					Set<ClientMethod> copy = new HashSet<ClientMethod>(mSet2);
					copy.retainAll(mSet2);
					if (!copy.isEmpty()) {
						mSet1.addAll(mSet2);
						mSet2.clear();
						isChanged = true;
						break;
					}
				}
			}
		}				
		return g;
	}
}
