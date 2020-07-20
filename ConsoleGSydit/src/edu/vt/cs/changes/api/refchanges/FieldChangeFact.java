package edu.vt.cs.changes.api.refchanges;

import com.ibm.wala.types.FieldReference;

public class FieldChangeFact extends SubChangeFact{

	public CHANGE_TYPE ct;	
	public FieldReference fRef;
	public boolean inSameClass;
	public String content;
	
	@Override
	public String toString() {
		return ct.toString();
	}
	
}
