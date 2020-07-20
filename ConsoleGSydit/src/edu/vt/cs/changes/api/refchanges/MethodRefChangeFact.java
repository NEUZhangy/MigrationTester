package edu.vt.cs.changes.api.refchanges;

import org.eclipse.jdt.core.dom.IMethodBinding;

public class MethodRefChangeFact extends SubChangeFact{

	public CHANGE_TYPE ct;
	public boolean isLib;
	public IMethodBinding binding;
	
	@Override
	public String toString() {
		return ct.toString() + binding.getKey() + ": isLib = " + isLib;
	}
}
