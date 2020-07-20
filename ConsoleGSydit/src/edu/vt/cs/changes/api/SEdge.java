package edu.vt.cs.changes.api;

public class SEdge {

	public int srcIdx;
	public int dstIdx;
	public boolean mark;
	
	public SEdge(int srcIdx, int dstIdx) {
		this.srcIdx = srcIdx;
		this.dstIdx = dstIdx;
	}
	
	public void enableMark() {
		this.mark = true;
	}
}
