package edu.vt.cs.diffparser.util;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class ASTNodeFinder extends ASTVisitor{

	private ASTNode node = null;
	private int start = 0;
	private int length = 0;
	
	private long squareDiff;
	private boolean isMatched = false;
	
	protected static Map<String, SoftReference<Map<SourceCodeRange, ASTNode>>> pathToASTs = 
			new HashMap<String, SoftReference<Map<SourceCodeRange, ASTNode>>>();
	
	public ASTNode lookforASTNode(CompilationUnit unit, SourceCodeRange r) {
		String path = unit.getJavaElement().getPath().toOSString();
		ASTNode result = null;
		Map<SourceCodeRange, ASTNode> map = null;
		
		if (pathToASTs.containsKey(path) && pathToASTs.get(path).get() != null) {
			map = pathToASTs.get(path).get();
//			if (r.length == 17) {
//				System.out.println();
//			}
			if (map.containsKey(r)) {
				return map.get(r);
			}
		} else {
			map = new HashMap<SourceCodeRange, ASTNode>();
			pathToASTs.put(path, new SoftReference<Map<SourceCodeRange, ASTNode>>(map));
		}
		setRange(r);		
		unit.accept(this);
		result = node;
		map.put(r, result);
		return result;
	}
	
	void setRange(SourceCodeRange r) {
		this.start = r.startPosition;
		this.length = r.length;
		this.squareDiff = Long.MAX_VALUE;
		this.isMatched = false;		
	}
	
	@Override
	public void preVisit(ASTNode node) {
		if (!isMatched) {
			if (node.getStartPosition() == this.start
					&& node.getLength() == this.length) {
				this.squareDiff = 0;
				this.node = node;
				this.isMatched = true;
			} else {
				long diffOffset = this.start - node.getStartPosition();
				long diffLength = this.length - node.getLength();
				// the new method node is closer to the given range
				if (diffOffset * diffOffset + diffLength * diffLength < this.squareDiff) {
					this.squareDiff = diffOffset * diffOffset + diffLength
							* diffLength;
					this.node = node;
				}
			}
		}
	}
	
}
