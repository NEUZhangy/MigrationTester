package edu.vt.cs.changes.api;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl.ConcreteJavaMethod;
import com.ibm.wala.cast.java.ssa.AstJavaInvokeInstruction;
import com.ibm.wala.cast.loader.AstMethod.DebuggingInformation;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ShrikeCTMethod;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.GetCaughtExceptionStatement;
import com.ibm.wala.ipa.slicer.PDG;
import com.ibm.wala.ipa.slicer.PhiStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.ssa.Value;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.vt.cs.diffparser.util.SourceCodeRange;
import edu.vt.cs.graph.ClassRangeKey;
import edu.vt.cs.graph.LineMapper;
import edu.vt.cs.graph.SourceMapper;
import partial.code.grapa.delta.graph.data.AbstractNode;
import partial.code.grapa.delta.graph.data.Edge;
import partial.code.grapa.dependency.graph.StatementEdge;
import partial.code.grapa.dependency.graph.StatementNode;
import partial.code.grapa.mapping.ClientMethod;

//added by nameng: Modified from GraphConvertor to instantiate the functionality (public static=>public)
public class CDGraphConvertor {
	SDG g = null;
	IR ir = null;	
	ClientMethod mClient = null;
	LineMapper lineMapper = null;
	CompilationUnit cu = null;
	private Map<Integer, List<Integer>> lineToIndexes = null;
	private ConcreteJavaMethod method = null;
	private ShrikeCTMethod method2 = null;
	private String className = null;
	private Map<Integer, Integer> indexToLine = null;
	private ValueConvertor vc = null;
	private SymbolTable symTab = null;
	private SSAInstruction[] instructions;
	private String[][] sourceNames;
	private Map<String, Set<Integer>> nameMap = null;
	private Map<Integer, Statement> insnToStmt = null;
	
	Map<Integer, DefUseFact> summary = null;
	PDG pdg = null;
	
	public void init(IR ir, SDG g, ClientMethod mClient, LineMapper lineMapper, CompilationUnit cu) {
		this.ir = ir;
		if (ir == null) return;
		instructions = ir.getInstructions();
		this.symTab = ir.getSymbolTable();
		this.g = g;
		System.out.print("");
		Set<CGNode> cgNodes = g.getCallGraph().getNodes(mClient.mRef);
		if (cgNodes.size() > 1 || cgNodes.size() == 0) {
			System.err.println("Need more process");
		}
		CGNode cgNode;
		try {
			cgNode = cgNodes.iterator().next();
		}
		catch (NoSuchElementException e) {
			return;
		}
		pdg = g.getPDG(cgNode);
		this.mClient = mClient;
		this.cu = cu;
		this.lineMapper = lineMapper;
		symTab = ir.getSymbolTable();
		
		int index = -1;
		List<ASTNode> coveredStatements = null;
	
		lineToIndexes = new HashMap<Integer, List<Integer>>();
		indexToLine = new HashMap<Integer, Integer>();
		method = null;
		className = null;
		summary = new HashMap<Integer, DefUseFact>();

		vc = new ValueConvertor(ir);
	
		Statement statement = null;
		Iterator<Statement> it = pdg.iterator();
		statement = it.next();
//		while(it.hasNext()) {
//			statement = it.next();
//			System.out.println(statement);
//		}		
		method = (ConcreteJavaMethod)statement.getNode().getMethod();
		className = method.getDeclaringClass().getName().toString();

//		className = method.getDeclaringClass().getName().toString();	
		
		SSAInstruction inst = null;
		it = pdg.iterator();
		insnToStmt = new HashMap<Integer, Statement>();
		while(it.hasNext()) {
			statement = it.next();			
			int src_line_number = 0;				
			// this includes ExceptionalReturnCaller, NormalReturnCaller, NormalStatement, and ParamCaller
			if (statement instanceof StatementWithInstructionIndex) {
				StatementWithInstructionIndex s = (StatementWithInstructionIndex)statement;				
				index = s.getInstructionIndex();
				inst = s.getInstruction();				
			} else if (statement instanceof GetCaughtExceptionStatement) {
				GetCaughtExceptionStatement s = (GetCaughtExceptionStatement)statement;
				index = s.getInstruction().iindex;
				inst = s.getInstruction();
			} else {//this includes METHOD_ENTRY/EXIT, PARAM_CALLEE, NORMAL_RET_CALLEE, PhiStatement
				continue;
			}	
			List<String> defStrs = new ArrayList<String>();
			for (int i = 0; i < inst.getNumberOfDefs(); i++) {
				defStrs.add(vc.convert(index, inst.getDef(i)));
			}
			List<String> useStrs = new ArrayList<String>();				
			for (int i = 0; i < inst.getNumberOfUses(); i++) {
				useStrs.add(vc.convert(index, inst.getUse(i)));
			}
			summary.put(index, new DefUseFact(defStrs, useStrs));
			insnToStmt.put(index, statement);
			src_line_number = method.getLineNumber(index);
			
			
			List<Integer> indexes = lineToIndexes.get(src_line_number);
			if (indexes == null) {
				indexes = new ArrayList<Integer>();
				lineToIndexes.put(src_line_number, indexes);
			}
			if (!indexes.contains(index))
				indexes.add(index);
			if (!indexToLine.containsKey(index)) {
				indexToLine.put(index, src_line_number);
			}
		} 
		
		JavaSourceLoaderImpl.ConcreteJavaMethod cjm = (JavaSourceLoaderImpl.ConcreteJavaMethod)ir.getMethod();
		DebuggingInformation debugInfo = cjm.debugInfo();
		sourceNames = debugInfo.getSourceNamesForValues();
		nameMap = new HashMap<String, Set<Integer>>();
		String[] names = null;
		String tmpName = null;
		Set<Integer> indexes = null;
		for (int i = 0; i < sourceNames.length; i++) {
			names = sourceNames[i];			
			for (int j = 0; j < names.length; j++) {
				tmpName = names[j];
				indexes = nameMap.get(tmpName);
				if (indexes == null) {
					indexes = new HashSet<Integer>();
					nameMap.put(tmpName, indexes);
				}
				indexes.add(j);
			}
		}
//		g.reConstruct(DataDependenceOptions.NONE, ControlDependenceOptions.FULL);		
//		it = g.iterator();
//		while (it.hasNext()) {
//			Statement s1 = it.next();		
//			Iterator<Statement> nodes = g.getSuccNodes(s1);			
//			while(nodes.hasNext()){
//				
//			}
//		}
		
//		g.reConstruct(DataDependenceOptions.NONE, ControlDependenceOptions.FULL);
//		it = g.iterator();
//		while(it.hasNext()){
//			Statement s1 = it.next();
//			Iterator<Statement> nodes = g.getSuccNodes(s1);
//			while(nodes.hasNext()){
//				Statement s2 = nodes.next();
//				
//			}
//		}
	}
	
	public String getClassName() {
		return className;
	}
	
	//shengzhe Aug
	public List<Integer> getControls(int target_index) {
		List<Integer> result = new ArrayList<Integer>();
		if (target_index >= instructions.length) return result;
		SSAInstruction insn = instructions[target_index];
		SSAInstruction inst = null;
		Iterator<Statement> it = pdg.iterator();
		insnToStmt = new HashMap<Integer, Statement>();
		Statement statement;
		int index = -1;
		while(it.hasNext()) {
			statement = it.next();			
			int src_line_number = 0;				
			// this includes ExceptionalReturnCaller, NormalReturnCaller, NormalStatement, and ParamCaller
			if (statement instanceof StatementWithInstructionIndex) {
				StatementWithInstructionIndex s = (StatementWithInstructionIndex)statement;				
				index = s.getInstructionIndex();
				inst = s.getInstruction();				
			} else if (statement instanceof GetCaughtExceptionStatement) {
				GetCaughtExceptionStatement s = (GetCaughtExceptionStatement)statement;
				index = s.getInstruction().iindex;
				inst = s.getInstruction();
			} else {//this includes METHOD_ENTRY/EXIT, PARAM_CALLEE, NORMAL_RET_CALLEE, PhiStatement
				continue;
			}	
			
			if (target_index == index) {
				Iterator<Statement> nodes = pdg.getControlSuccNodes(statement);
//				if (!nodes.hasNext()) {
//					break;
//				}
//				System.out.println("level 1:" + indexToLine.get(index));
				while(nodes.hasNext()){
					Statement s2 = nodes.next();
//					System.out.println("level 2:" + indexToLine.get(((StatementWithInstructionIndex)s2).getInstructionIndex()));
					
//					not sure which line should be used so far.
//					Integer one_sl_line = indexToLine.get(((StatementWithInstructionIndex)s2).getInstructionIndex());
					Integer one_sl_line = 0;
					if (s2 instanceof StatementWithInstructionIndex) {
						one_sl_line = ((StatementWithInstructionIndex)s2).getInstructionIndex();
					} else {//this includes METHOD_ENTRY/EXIT, PARAM_CALLEE, NORMAL_RET_CALLEE, PhiStatement
						continue;
					}
					if (!result.contains(one_sl_line)) {
						result.add(one_sl_line);
					}
				}
				break;
			}
		}
		return result;
	}
	
	public List<String> getDefs(int index) {
		List<String> result = new ArrayList<String>();
		SSAInstruction insn = instructions[index];
		int num = insn.getNumberOfDefs();
		int vNum = 0;
		for (int i = 0; i < num; i++) {
			vNum = insn.getDef(i);
			result.add(getName(index, vNum));
		}		
		return result;
	}
	
	public List<String> getUses(int index) {
		List<String> result = new ArrayList<String>();
		SSAInstruction insn = instructions[index];
		int num = insn.getNumberOfUses();
		int vNum = 0;
		for (int i = 0; i < num; i++) {
			vNum = insn.getUse(i);
			result.add(getName(index, vNum));
		}
		return result;
	}
	
	public Statement getStatement(int idx) {
		return insnToStmt.get(idx);
	}
	
	public List<Integer> getInstIndexes(int line) {
		
		// recovered by shengzhe 4/6 18
		if (lineToIndexes == null) {
			System.out.println("Execption in lineToIndexes when FieldAPI resolve");
			return Collections.EMPTY_LIST;
		}
		List<Integer> result = lineToIndexes.get(line);
		if (result == null) {
			return Collections.EMPTY_LIST;
		} else {
			return lineToIndexes.get(line);
		}		
	}
	
	public List<Integer> getLines(List<Integer> insnIndexes) {
		Set<Integer> lineNums = new HashSet<Integer>();
		Integer lineNum = null;
		for (Integer idx : insnIndexes) {
			lineNum = indexToLine.get(idx);
			if (lineNum != null)
				lineNums.add(indexToLine.get(idx));
		}
		List<Integer> result = new ArrayList<Integer>(lineNums);
		Collections.sort(result);
		return result;
	}
	
	public SSAInstruction[] getInstructions() {
		return instructions;
	}
	
	public String getName(int insnIndex, int vNum) {
		String[] name = null;
		try {
			name = sourceNames[vNum];		
		} catch(Exception e) {
			name = null;
		}
		String result = null;
		if (name != null && name.length != 0) {
			result = name[0];
		} else {				
			name = ir.getLocalNames(insnIndex, vNum);
			if (name.length != 0 ) {
				result = name[0];
			}else {
				result = "v" + vNum;
			}								
		}
		return result;
	}
	
	public boolean hasVariable(String name) {
		return nameMap.containsKey(name);
	}
	
	
	private InsnNode createInsnNode(SSAInstruction insn, Subgraph g) {
		int index = insn.iindex;
		String signature = null;
		InsnNode insnNode = null;
		InsnNode targetInsnNode = null;
		if (insn instanceof AstJavaInvokeInstruction) {
			AstJavaInvokeInstruction invoke = (AstJavaInvokeInstruction)insn;
			MethodReference mRef = invoke.getDeclaredTarget();
			signature = mRef.getSignature();				
			insnNode = g.getInsnNode(signature, InstType.INVOKE, index);
		} else if (insn instanceof SSAArrayLengthInstruction) {
			SSAArrayLengthInstruction arrayLength = (SSAArrayLengthInstruction)insn;			
			System.out.println("need more process");
		} else if (insn instanceof SSAArrayLoadInstruction) { 
			SSAArrayLoadInstruction arrayLoad = (SSAArrayLoadInstruction)insn;
			System.out.println("need more process");
		} else if (insn instanceof SSAArrayStoreInstruction) {
			SSAArrayStoreInstruction aStore = (SSAArrayStoreInstruction)insn;
			signature = aStore.getElementType().toString();
			insnNode = g.getInsnNode(signature, InstType.ASTORE, index);
		} else if (insn instanceof SSABinaryOpInstruction) {
			SSABinaryOpInstruction binOp = (SSABinaryOpInstruction)insn;
			signature = binOp.getOperator().toString();
			insnNode = g.getInsnNode(signature, InstType.BINOP, index);
		} else if (insn instanceof SSACheckCastInstruction) { 
			SSACheckCastInstruction cast = (SSACheckCastInstruction)insn;
			TypeReference[] tRefs = cast.getDeclaredResultTypes();
			StringBuffer buffer = new StringBuffer();
			for (TypeReference tRef : tRefs) {
				buffer.append(tRef.toString()).append(",");
			}
			signature = buffer.toString();
			insnNode = g.getInsnNode(signature, InstType.CAST, index);
		} else if (insn instanceof SSAComparisonInstruction) { 
			SSAComparisonInstruction comp = (SSAComparisonInstruction)insn;
			System.out.println("need more process");
		} else if (insn instanceof SSAConditionalBranchInstruction) {
			SSAConditionalBranchInstruction branch = (SSAConditionalBranchInstruction)insn;
			signature = branch.getOperator().toString();
			insnNode = g.getInsnNode(signature, InstType.BRANCH, index);
			if (branch.getTarget()>=0 && branch.getTarget()<instructions.length) { 
				targetInsnNode = createInsnNode(instructions[branch.getTarget()], g);
				g.addEdge(insnNode, targetInsnNode);
			}
		} else if (insn instanceof SSAConversionInstruction) { 
			SSAConversionInstruction convt = (SSAConversionInstruction)insn;
			System.out.println("need more process");
		} else if (insn instanceof SSAGetCaughtExceptionInstruction) {
			SSAGetCaughtExceptionInstruction caught = (SSAGetCaughtExceptionInstruction) insn;
			System.out.println("need more process");
		} else if (insn instanceof SSAGetInstruction) {
			SSAGetInstruction get = (SSAGetInstruction)insn;
			FieldReference fRef = get.getDeclaredField();
			signature = fRef.getSignature();
			if (get.isStatic()) {
				insnNode = g.getInsnNode(signature, InstType.GET_STATIC, index);
				insnNode.addExtraData(fRef);
			} else {
				insnNode = g.getInsnNode(signature, InstType.GET, index);
			}				
		} else if (insn instanceof SSAGotoInstruction) {
			SSAGotoInstruction gotoInst = (SSAGotoInstruction)insn;
			insnNode = g.getInsnNode("", InstType.GOTO, index);
			if (gotoInst.getTarget()>=0 && gotoInst.getTarget()<instructions.length) {
				targetInsnNode = createInsnNode(instructions[gotoInst.getTarget()], g);
				g.addEdge(insnNode, targetInsnNode);
			}
		} else if (insn instanceof SSAInstanceofInstruction) { 
			SSAInstanceofInstruction instanceInst = (SSAInstanceofInstruction)insn;
			System.out.println("need more process");
		} else if (insn instanceof SSALoadMetadataInstruction) {
			SSALoadMetadataInstruction l = (SSALoadMetadataInstruction)insn;
			signature = l.getType().getName().toString();
			insnNode = g.getInsnNode(signature, InstType.LOAD_META, index);
		} else if (insn instanceof SSAMonitorInstruction) {
			SSAMonitorInstruction monitor = (SSAMonitorInstruction)insn;
			if (monitor.isMonitorEnter()) {
				signature = "enter";
			} else {
				signature = "exit";
			}
			insnNode = g.getInsnNode(signature, InstType.MONITOR, index);
		} else if (insn instanceof SSANewInstruction) {
			SSANewInstruction newInst = (SSANewInstruction)insn;
			TypeReference tRef = newInst.getConcreteType();
			signature = tRef.toString();
			insnNode = g.getInsnNode(signature, InstType.NEW, index);
		} else if (insn instanceof SSAPhiInstruction){
			SSAPhiInstruction phi = (SSAPhiInstruction)insn;
			System.out.println("need more process");
		} else if (insn instanceof SSAPutInstruction) { 
			SSAPutInstruction put = (SSAPutInstruction)insn;
			System.out.println("need more process");
		} else if (insn instanceof SSAReturnInstruction) {			
			SSAReturnInstruction r = (SSAReturnInstruction)insn;
			insnNode = g.getInsnNode("", InstType.RETURN, index);
		} else if (insn instanceof SSASwitchInstruction) {
			SSASwitchInstruction s = (SSASwitchInstruction)insn;
			System.out.println("need more process");
		} else if (insn instanceof SSAThrowInstruction) {
			SSAThrowInstruction t = (SSAThrowInstruction)insn;
			insnNode = g.getInsnNode("", InstType.THROW, index);
		} else if (insn instanceof SSAUnaryOpInstruction) {
			SSAUnaryOpInstruction u = (SSAUnaryOpInstruction)insn;
			signature = u.getOpcode().toString();
			insnNode = g.getInsnNode(signature, InstType.UNARYOP, index);
		} else {
			System.out.println("need more process");
		}
		return insnNode;
	}
	
	public Subgraph getSubgraph(List<ISSABasicBlock> blocks) {
		int fIndex = -1, lIndex = -1;
		Subgraph sg = new Subgraph(symTab);
		DefUseFact f = null;
		InsnNode insnNode = null;
		
		SSAInstruction[] insns = ir.getInstructions();
		SSAInstruction insn = null;
		
		for (ISSABasicBlock b : blocks) {
			fIndex = b.getFirstInstructionIndex();
			lIndex = b.getLastInstructionIndex();
			for (int i = fIndex; i <= lIndex; i++) {
				f = summary.get(i);
				insn = insns[i];
				if (insn == null)
					continue;
				insnNode = createInsnNode(insn, sg);
//				System.out.println(insnNode +" "+  insn+" "+ f.defs+" "+ f.uses);
				sg.addEdges(insnNode, insn, f.defs, f.uses);			
			}			
		}
		return sg;
	}
	
	public class DefUseFact {
	    List<String> defs;
	    List<String> uses;
	    public DefUseFact(List<String> defs, List<String> uses) {
	    	this.defs = defs;
	    	this.uses = uses;
	    }
	}
	
	//added by shengzhe
	public SDG getSDG() {
		return this.g;
	}
}

   
