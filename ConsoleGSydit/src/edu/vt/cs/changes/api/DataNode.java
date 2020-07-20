package edu.vt.cs.changes.api;

import com.ibm.wala.types.FieldReference;

public class DataNode extends SNode{

	FieldReference fRef = null;
	
	public DataNode(String label, int id) {
		super(label, SNode.DATA_NODE, id);	
	}
	
	private boolean isException = false;
	private boolean isConstant = false;
	
	public void enableConstant() {
		isConstant = true;
	}
	
	public void enableException() {
		isException = true;
	}
	
	public boolean isConstant() {
		return isConstant;
	}
	
	public boolean isException() {
		return isException;
	}
}
