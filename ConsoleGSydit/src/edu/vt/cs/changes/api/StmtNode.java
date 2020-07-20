package edu.vt.cs.changes.api;

public class StmtNode extends SNode {
	
	int sIndex;
	
	public StmtNode(String label, int id, int sIndex) {
		super(label, SNode.STMT_NODE, id);
		this.sIndex = sIndex;
	}

}
