package edu.vt.cs.changes.api.refchanges;

import org.eclipse.jdt.core.dom.ITypeBinding;

public class ClassRefChangeFact extends SubChangeFact{
	
	public CHANGE_TYPE ct;
	public boolean isLib;
	public ITypeBinding binding;
	
	
	@Override
	public String toString() {
		return ct.toString() + " " + binding.getQualifiedName() + ": isLib = " + isLib;
	}
}
