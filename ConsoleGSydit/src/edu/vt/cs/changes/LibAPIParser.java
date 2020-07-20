package edu.vt.cs.changes;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;

import partial.code.grapa.dependency.graph.DataFlowAnalysisEngine;

public class LibAPIParser extends ASTVisitor {

	DataFlowAnalysisEngine engine = null;
	String libStr = null;
	List<IBinding> bindings = null;
	
	public LibAPIParser(DataFlowAnalysisEngine engine, String libStr) {
		this.engine = engine;
		this.libStr = libStr;		
	}
	
	public void init() {
		bindings = new ArrayList<IBinding>();
	}
	
	@Override
	public boolean visit(MethodInvocation node) {
		IMethodBinding mb = node.resolveMethodBinding();
		String typeName = mb.getDeclaringClass().getQualifiedName();
		if (isLib(typeName))
			bindings.add(mb);
		return super.visit(node);
	}
	
	boolean isLib(String typeName) {
		return typeName.contains(libStr);
	}
	
	@Override
	public boolean visit(ParameterizedType node) {
		ITypeBinding binding = node.resolveBinding();
		String typeName = binding.getQualifiedName();
		if (isLib(typeName))
			bindings.add(binding);
		return false;
	}
	
	@Override
	public boolean visit(QualifiedType node) {
		ITypeBinding binding = node.resolveBinding();
		String typeName = binding.getQualifiedName();
		if (isLib(typeName)) 
			bindings.add(binding);
		return false;
	}
	
	@Override
	public boolean visit(SimpleType node) {
		ITypeBinding binding = node.resolveBinding();
		if (binding != null) {
			String typeName = binding.getQualifiedName();
			if (isLib(typeName)) 
				bindings.add(binding);
		}
		return false;
	}
	
	@Override
	public boolean visit(SimpleName node) {
//		System.out.println(node.getFullyQualifiedName());
		IBinding tBinding = node.resolveBinding();
//		ITypeBinding tBinding = node.getQualifier().resolveTypeBinding();
		if (tBinding != null) {
			String typeName = tBinding.getKey();
//			String typeName = tBinding.getQualifiedName();
//			System.out.println(typeName);
			String new_typeName = typeName.replace('/', '.');
			if (isLib(new_typeName)) {
				bindings.add(node.resolveBinding());
			}
		}
		return false;
	}
	
	@Override
	public boolean visit(QualifiedName node) {
		ITypeBinding tBinding = node.getQualifier().resolveTypeBinding();
		if (tBinding != null) {
			String typeName = tBinding.getQualifiedName();
			if (isLib(typeName)) {
				bindings.add(node.resolveBinding());
			}
		}
		return false;
	}
	
	public List<IBinding> getBindings() {
		return bindings;
	}
}
