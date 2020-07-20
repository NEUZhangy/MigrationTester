package edu.vt.cs.changes.api.refchanges;

import com.ibm.wala.types.FieldReference;

public class FieldRefChangeFact extends SubChangeFact {

	public CHANGE_TYPE ct;
	public boolean isLib;
	public FieldReference fRef;
	
	@Override
	public String toString() {
		return ct.toString() + fRef.getSignature() + ": isLib = " + isLib;
	}
}
