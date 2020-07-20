package edu.vt.cs.changes.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;

import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.SourceRange;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.model.entities.Delete;
import ch.uzh.ifi.seal.changedistiller.model.entities.Insert;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeEntity;
import ch.uzh.ifi.seal.changedistiller.model.entities.Update;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.vt.cs.append.CommonValue;
import edu.vt.cs.append.FineChangesInMethod;
import edu.vt.cs.append.JavaExpressionConverter;
import edu.vt.cs.append.TopDownTreeMatcher;
import edu.vt.cs.append.terms.MethodNameTerm;
import edu.vt.cs.append.terms.Term;
import edu.vt.cs.append.terms.VariableTypeBindingTerm;
import edu.vt.cs.changes.ChangeFact;
import edu.vt.cs.changes.ChangeMethodData;
import edu.vt.cs.changes.api.DefUseChangeFact.DATA_CHANGE_TYPE;
import edu.vt.cs.diffparser.util.ASTNodeFinder;
import edu.vt.cs.diffparser.util.SourceCodeRange;
import edu.vt.cs.graph.SourceMapper;
import partial.code.grapa.dependency.graph.DataFlowAnalysisEngine;
import partial.code.grapa.dependency.graph.StatementEdge;
import partial.code.grapa.dependency.graph.StatementNode;
import partial.code.grapa.mapping.ClientMethod;

public class MethodAPIResolver {
	
	protected DataFlowAnalysisEngine leftEngine;
	protected DataFlowAnalysisEngine rightEngine;
	private APIResolver resolver;
	
//	private SourceMapper oldSourceMapper = null;
//	private SourceMapper newSourceMapper = null;
	
	public MethodAPIResolver(APIResolver resolver) {
		this.leftEngine = resolver.leftEngine;
		this.rightEngine = resolver.rightEngine;	
		this.resolver = resolver;
//		this.oldSourceMapper = new SourceMapper();
//		this.newSourceMapper = new SourceMapper();
	}
	
	public String getvarname(SourceCodeChange scc) {
		return "";
	}
	
	public String getfromname(SourceCodeChange scc) {
		return "";
	}
	
	public boolean isLib(String name) {
		return false;
	}
	
	public void resolve(Map<IMethodBinding, Set<ChangeFact>> missings, Map<IVariableBinding, Set<ChangeFact>> added_lib_version) {
		IMethodBinding mb = null;
		IVariableBinding fvb = null;
		Set<ChangeFact> value = null;
		Set<SourceCodeRange> ranges = null;
		GraphConvertor2 oc = new GraphConvertor2();
		GraphConvertor2 nc = new GraphConvertor2();
		//new added
		CDGraphConvertor CD_oc = new CDGraphConvertor();
		CDGraphConvertor CD_nc = new CDGraphConvertor();
		SourceMapper oldMapper = new SourceMapper();
		SourceMapper newMapper = new SourceMapper();
		Subgraph s1 = null, s2 = null;
		String mbKey = null;	
		
		// normal method resolver
		for (Entry<IMethodBinding, Set<ChangeFact>> entry : missings.entrySet()) {
			mb = entry.getKey();
			mbKey = mb.getKey();
			value = entry.getValue();
			for (ChangeFact cf : value) {
			   List<ChangeMethodData> data = cf.changedMethodData;			   			   
			   
			   for (ChangeMethodData d : data) {				   
				   if (d.oldMethodBindingMap.containsKey(mb)) {
					   ClientMethod oldMethod = d.oldMethod;
					   ClientMethod newMethod = d.newMethod;
					   CompilationUnit oldCu = (CompilationUnit)oldMethod.ast;
					   CompilationUnit newCu = (CompilationUnit)newMethod.ast;
					   System.out.println("Function Name: " + oldMethod.toString());
//					   if (!oldMethod.toString().contains("search"))
//						   continue;
//					   if (!mb.toString().contains("addDocument"))
//						   continue;
					   SDG lfg = resolver.findOrCreateOldSDG(oldMethod);						   
					   SDG rfg = resolver.findOrCreateNewSDG(newMethod);
					   oldMapper.add(oldMethod.ast);
//					   if (oldMethod.getSignature().contains("get")) continue;
					   oc.init(leftEngine.getCurrentIR(), lfg,
							   oldMethod,
							   oldMapper.getLineMapper(oldCu), oldCu);   
					   nc.init(rightEngine.getCurrentIR(), rfg, newMethod, 
							   newMapper.getLineMapper(newCu), newCu);					  
					   
					   //new added
					   lfg.reConstruct(DataDependenceOptions.NONE, ControlDependenceOptions.FULL);
					   rfg.reConstruct(DataDependenceOptions.NONE, ControlDependenceOptions.FULL);
					   CD_oc.init(leftEngine.getCurrentIR(), lfg,
							   oldMethod,
							   oldMapper.getLineMapper(oldCu), oldCu);   
					   CD_nc.init(rightEngine.getCurrentIR(), rfg, newMethod, 
							   newMapper.getLineMapper(newCu), newCu);
					   //end
					   
					   System.out.print("");
					   SourceCodeEntity mEntity = cf.getEntity(SourceCodeRange.convert(oldMethod.methodbody));					   			  
					   FineChangesInMethod allChanges = (FineChangesInMethod)(cf.getChange(mEntity));
					   
					   //added by shengzhe July, 2017
//					   List<SourceCodeChange> inter_changes = allChanges.getChanges();
//
//					   for(int i=0;i<inter_changes.size() -1;i++){  
//					       for(int j=0;j<inter_changes.size()-1-i;j++){  
//						        if(inter_changes.get(j).getChangedEntity().getStartPosition()
//						        		>inter_changes.get(j+1).getChangedEntity().getStartPosition()){  
//						        	Collections.swap(inter_changes, j, j+1);
//						        }  
//					        }  
//					    }  
//					   
//					   String left_top_bound, left_bot_bound, right_top_bound="", right_bot_bound="";
//					   boolean input_lib_label = false;
//					   int[] ingroup = new int[100];
//					   for (int i=0;i<inter_changes.size(); i++) ingroup[i] = 0;
//					   for (int pos = 0; pos< inter_changes.size(); pos++ ) {
//						   SourceCodeChange x = inter_changes.get(pos);
//						   if (x.getChangedEntity().getType() == JavaEntityType.IF_STATEMENT) {
//							   String focus = getvarname(x);
//							   ingroup[pos] = 1;
//							   if (isLib(focus)) {
//								   input_lib_label = true;
//							   }
//							   for (int j = pos-1; j>=0;j--) {
//								   if (getvarname(inter_changes.get(j)).equals(focus)) {
//									   focus = getfromname(inter_changes.get(j));
//									   ingroup[j] = 1;
//									   if (isLib(focus)) {
//										   input_lib_label = true;
//									   }
//								   }
//							   }
//							   left_top_bound = focus;
//							   focus = getvarname(x);
//							   for (int j = pos+1; j<inter_changes.size();j++) {
//								   if (getfromname(inter_changes.get(j)).equals(focus)) {
//									   ingroup[j] = 1;
//									   focus = getvarname(inter_changes.get(j));
//								   }
//							   }
//							   left_bot_bound = focus;
//							   
//							   //----
//							   if (left_top_bound.equals(right_top_bound) && left_bot_bound.equals(right_bot_bound)) {
//								   //groups.add(ingroup);
//							   }
//						   }
//						   pos++;
//					   }
					   
					   //added end
					   
					   // initialize the resolver
					   ranges = d.oldMethodBindingMap.get(mb);
					   resolver.oc = oc;
					   resolver.oldCu = oldCu;
					   resolver.nc = nc;
					   resolver.newCu = newCu;
					   CurrentResolver.set_current_resolver(resolver);
					   CurrentResolver.set_current_ranges(ranges);
					   CurrentResolver.set_current_allchanges(allChanges);
					   
					   List<DefUseChangeFactGroup> groups = cf.getDefUseChangeFactGroups(mEntity, oldCu, newCu, oc, nc, resolver, d, CD_oc, CD_nc);			   
					   
//					   resolver.resolve(groups, ranges, allChanges);					   
				   }
//					   ranges = d.oldBindingMap.get(mb);
//					  
//					   Set<SourceRange> newRangesInvolved = new HashSet<SourceRange>();
//					   
//					   for (SourceCodeRange r : ranges) {						 
//						  int idx = allChanges.getIndexOfChangeForOldRange(r.converToSourceRange());
//						  newRangesInvolved.add(allChanges.getNewRangeWithIndex(idx));
//						  group = ChangeFact.getGroupWithChange(idx, groups);
//						  group.reorganizeChanges();
//						  if(group.addedParameters.isEmpty() && group.deletedParameters.isEmpty()) {
//							  System.out.println("Information self-contained: \n" + group);
//						  } else {
//							  System.out.println("Informaion not self-contained: \n" + group);
//						  }						 
//					   }					   
					   
//					   for (Entry<IBinding, Set<SourceCodeRange>> entry2 : d.newBindingMap.entrySet()) {
//						   IBinding nb = entry2.getKey();
//						   Set<SourceCodeRange> newRanges = entry2.getValue();
//						   Set<SourceRange> convertedRanges = new HashSet<SourceRange>();
//						   for (SourceCodeRange scr : newRanges) {
//							   convertedRanges.add(scr.converToSourceRange());
//						   }					   
//					   }
					   
					   
					   //wait for use later ... by nmeng
//					   FineChangesInMethod allChanges = (FineChangesInMethod)(cf.getChange(mEntity));					  
//					   ranges = d.oldBindingMap.get(mb);
//					   Subgraph s1 = null, s2 = null;
//					   for (SourceCodeRange r : ranges) {							  						  
//						   SourceCodeChange c = allChanges.getChangeWithOldRange(r.converToSourceRange());
//						   if (c instanceof Update) {
//							   Update u = (Update)c;								  
//							   SourceCodeRange newR = SourceCodeRange.convert(u.getNewEntity());
//							   
//							   Node tree1 = getExpressionTree(oldCu, r, lConverter);
//							   Node tree2 = getExpressionTree(newCu, newR, rConverter);
//							   TopDownTreeMatcher matcher = new TopDownTreeMatcher();
//							   matcher.match(tree1, tree2);
//							   Map<Node, Node> unmatchedLeftToRight = matcher.getUnmatchedLeftToRight();	
//							   boolean isRelevant = true;
//							   for (Entry<Node, Node> entry2 : unmatchedLeftToRight.entrySet()) {
//								   Node lNode = entry2.getKey();
//								   Node rNode = entry2.getValue();
//								   Node apiNode = getAPI(lNode);
//								   SourceRange range = apiNode.getEntity().getSourceRange();
//								   ASTNodeFinder finder = new ASTNodeFinder();
//								   ASTNode miNode = finder.lookforASTNode(oldCu, new SourceCodeRange(range.getStart(), range.getEnd()));
//								   MethodInvocation mi = (MethodInvocation)miNode;
//								   IMethodBinding mBinding = mi.resolveMethodBinding();
//								   if (mBinding.getKey().equals(mbKey)) {
//									   Node replace = getAPI(rNode);		
//									   //1. infer API mapping template									  
//									   UpgradeFact f = parseAPITemplate(lNode, rNode);
//									   //2. infer insert def/use	
//									   Map<String, DATA_CHANGE_TYPE> addedParameters = f.addedParameters;
//										  Map<String, Integer> addedDefs = new HashMap<String, Integer>();
//										  Map<String, Integer> addedUses = new HashMap<String, Integer>();
//										  SourceRange sr = rNode.getEntity().getSourceRange();
//										  List<Integer> lines = this.getLines(newCu, new SourceCodeRange(sr.getStart(), sr.getEnd()));
//										  Set<Integer> knownNewInsns = new HashSet<Integer>();		
//										  if (!f.addedParameters.isEmpty()) {										   
//											   inferDefUse(addedParameters, addedDefs, addedUses, lines, nc, knownNewInsns);
//										   }  
//										  Map<String, DATA_CHANGE_TYPE> removedParameters = f.removedParameters;
//										  Map<String, Integer> removedDefs = new HashMap<String, Integer>();
//										  Map<String, Integer> removedUses = new HashMap<String, Integer>();
//										  sr = lNode.getEntity().getSourceRange();
//										  lines = this.getLines(oldCu, new SourceCodeRange(sr.getStart(), sr.getEnd()));
//										  Set<Integer> knownOldInsns = new HashSet<Integer>();										 									  
//									   if (!f.removedParameters.isEmpty()) {
//										   inferDefUse(removedParameters, removedDefs, removedUses, lines, oc, knownOldInsns);
//									   }
//									   //3. identify and group related changes, based on the following hypothesis:
//									   // (1) the new version always uses no less information than the old one
//									   // (2) any newly created parameter should be used to connect missing information
//									   if (!addedDefs.isEmpty()) {										   
//										   PDG pdg = nc.pdg;										   										  
//										   for (Entry<String, Integer> ad : addedDefs.entrySet()) {
//											   Integer idx = ad.getValue();
//											   Statement s = nc.getStatement(idx);									   
//											   Iterator<Statement> iter = pdg.getSuccNodes(s);
//											   
//											   Statement succ = null;
//											   Integer succIdx = null;
//											   List<Integer> succIdxes = new ArrayList<Integer>();
//											   while(iter.hasNext()) {
//												   succ = iter.next();
//												   System.out.println(succ);
//												   if (succ instanceof NormalStatement) {
//													   succIdx = ((NormalStatement)succ).getInstructionIndex();
//													   if (!knownNewInsns.contains(succIdx)) {
//														   succIdxes.add(succIdx);
//													   }
//												   }												  
//											   }
//											   List<Integer> relatedLines = nc.getLines(succIdxes);
//											   List<SourceRange> relatedRanges = new ArrayList<SourceRange>();
//											   for (Integer lineNum : relatedLines) {
//												   List<ASTNode> nodes = nc.lineMapper.findNodes(lineNum);
//												   for (ASTNode n : nodes) {
//													   relatedRanges.add(new SourceRange(n.getStartPosition(), n.getStartPosition() + n.getLength() - 1));													   
//												   }												   
//											   }
//											   Set<SourceRange> cRanges = allChanges.getAllNewRangesWithChange();
//											   relatedRanges.retainAll(cRanges);
//											   for (SourceRange rr : relatedRanges) {
//												   System.out.println(allChanges.getChangeWithNewRange(rr));
//											   }
//										   }
//									   }										  									 
//									   
//								   }
//							   }
////							   if (isRelevant) {
////								   s1 = getSubgraph(oldCu, r, oc);
////								   s2 = getSubgraph(newCu, newR, nc);
////								   comparator.compare(s1, s2, oc, nc, leftEngine, rightEngine);  
////							   }							   
//						   } else if (c instanceof Insert ) {
//							   Delete del = (Delete)c;
//							   SourceCodeRange oldR = SourceCodeRange.convert(del.getChangedEntity());							   
//						   } else if (c instanceof Delete) {
//							   Insert ins = (Insert)c;
//							   SourceCodeRange newR = SourceCodeRange.convert(ins.getChangedEntity());
//						   }
//					   }

				   //
				   
				   
			   }//end of for-loop of data
			}
		}		
		// added for insert lib version condition judgement
		for (Entry<IVariableBinding, Set<ChangeFact>> entry : added_lib_version.entrySet()) {
			fvb = entry.getKey();
		      mbKey = fvb.getKey();
		      value = entry.getValue();
		      for (ChangeFact cf : value) {
		         List<ChangeMethodData> data = cf.changedMethodData;                 
		         
		         for (ChangeMethodData d : data) {           
		           if (d.newFieldBindingMap.containsKey(fvb)) {
		             ClientMethod oldMethod = d.oldMethod;
		             ClientMethod newMethod = d.newMethod;
		             CompilationUnit oldCu = (CompilationUnit)oldMethod.ast;
		             CompilationUnit newCu = (CompilationUnit)newMethod.ast;
		             System.out.println("Function Name: " + oldMethod.toString());

		             SDG lfg = resolver.findOrCreateOldSDG(oldMethod);               
		             SDG rfg = resolver.findOrCreateNewSDG(newMethod);
		             oldMapper.add(oldMethod.ast);
//		             if (oldMethod.getSignature().contains("get")) continue;
		             oc.init(leftEngine.getCurrentIR(), lfg,
		                 oldMethod,
		                 oldMapper.getLineMapper(oldCu), oldCu);   
		             nc.init(rightEngine.getCurrentIR(), rfg, newMethod, 
		                 newMapper.getLineMapper(newCu), newCu);            
		             
		             //new added
		             lfg.reConstruct(DataDependenceOptions.NONE, ControlDependenceOptions.FULL);
		             rfg.reConstruct(DataDependenceOptions.NONE, ControlDependenceOptions.FULL);
		             CD_oc.init(leftEngine.getCurrentIR(), lfg,
		                 oldMethod,
		                 oldMapper.getLineMapper(oldCu), oldCu);   
		             CD_nc.init(rightEngine.getCurrentIR(), rfg, newMethod, 
		                 newMapper.getLineMapper(newCu), newCu);
		             //end
		             
		             System.out.print("");
		             SourceCodeEntity mEntity = cf.getEntity(SourceCodeRange.convert(oldMethod.methodbody));                    
		             FineChangesInMethod allChanges = (FineChangesInMethod)(cf.getChange(mEntity));
		             
		             // initialize the resolver
		             ranges = d.newFieldBindingMap.get(fvb);
		             resolver.oc = oc;
		             resolver.oldCu = oldCu;
		             resolver.nc = nc;
		             resolver.newCu = newCu;
		             CurrentResolver.set_current_resolver(resolver);
		             CurrentResolver.set_current_ranges(ranges);
		             CurrentResolver.set_current_allchanges(allChanges);
		             CurrentResolver.kaigua = 1;		             
		             List<DefUseChangeFactGroup> groups = cf.getDefUseChangeFactGroups(mEntity, oldCu, newCu, oc, nc, resolver, d, CD_oc, CD_nc);		         
		             
//		             resolver.resolve(groups, ranges, allChanges);
		             CurrentResolver.kaigua = 0;
		           }                                         
		         }//end of for-loop of data
		      }
		}
		
	}
}
