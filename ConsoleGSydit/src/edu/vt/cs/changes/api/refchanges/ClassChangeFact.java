package edu.vt.cs.changes.api.refchanges;

import org.eclipse.jdt.core.dom.ITypeBinding;

import com.ibm.wala.types.TypeReference;

import edu.vt.cs.graph.ClientClass;

public class ClassChangeFact extends SubChangeFact {

	public CHANGE_TYPE ct;
	public ITypeBinding tBinding;
	public boolean inSameClass;
	public ClientClass linkedEntity;
	
	@Override
	public String toString() {
		return ct.toString();
	}
}
