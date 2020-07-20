package edu.vt.cs.changes.api;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.SimpleName;

public class FieldAccessVisitor extends ASTVisitor{

	Map<String, IVariableBinding> map = null;
	
	public Map<String, IVariableBinding> lookforFieldAccess(ASTNode node) {
		map = new HashMap<String, IVariableBinding>();
		node.accept(this);
		return map;
	}
	
	
	@Override
	public boolean visit(FieldAccess node) {
		IVariableBinding binding = node.resolveFieldBinding();
		map.put(node.toString(), binding);
		return false;
	}
	
	@Override
	public boolean visit(SimpleName node) {
		IBinding binding = node.resolveBinding();
		if (binding.getKind() == IBinding.VARIABLE) {
			map.put(node.toString(), (IVariableBinding) binding);
		}
		return false;
	}
}
