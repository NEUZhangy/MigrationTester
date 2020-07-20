package edu.vt.cs.changes.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InsnNode extends SNode{
	private int iType = 0;
	private int instIndex = 0;
	private List<Object> extraData;
	
	
	public InsnNode(String s, int id, int iType, int instIndex) {
		super(s, SNode.INST_NODE, id);
		this.iType = iType;
		this.instIndex = instIndex;
		this.extraData = Collections.EMPTY_LIST;
	}
	
	public void addExtraData(Object obj) {
		if (this.extraData.isEmpty()) {
			this.extraData = new ArrayList<Object>();			
		}
		extraData.add(obj);
	}
	
	public List<Object> getExtraData() {
		return extraData;
	}

	public int getInstIndex() {
		return instIndex;
	}
	
	public int getInstType() {
		return iType;
	}
	
	@Override
	public String toString() {
		return toTypeString(iType) + " " + label;
	}
	
	public String toTypeString(int iType) {
		return InstType.toTypeString(iType);
	}

}
