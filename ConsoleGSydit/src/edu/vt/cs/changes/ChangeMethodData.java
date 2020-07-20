package edu.vt.cs.changes;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import edu.vt.cs.diffparser.util.SourceCodeRange;
import partial.code.grapa.mapping.ClientMethod;

public class ChangeMethodData {

	public ClientMethod oldMethod;
	public ClientMethod newMethod;
	public List<SourceCodeRange> oldASTRanges;
	public List<SourceCodeRange> newASTRanges;
	
	public Map<IMethodBinding, Set<SourceCodeRange>> oldMethodBindingMap = null;
	public Map<IMethodBinding, Set<SourceCodeRange>> newMethodBindingMap = null;
	
	public Map<String, Set<SourceCodeRange>> oldTypeBindingMap = null;
	public Map<String, Set<SourceCodeRange>> newTypeBindingMap = null;
	
	public Map<String, ITypeBinding> oldClassNameToBinding = null;
	public Map<String, ITypeBinding> newClassNameToBinding = null;
	
	public Map<IVariableBinding, Set<SourceCodeRange>> oldFieldBindingMap = null;
	public Map<IVariableBinding, Set<SourceCodeRange>> newFieldBindingMap = null;
	
	public ChangeMethodData (ClientMethod oMethod, ClientMethod nMethod, List<SourceCodeRange> oRanges,
			List<SourceCodeRange> nRanges){
		oldMethod = oMethod;
		newMethod = nMethod;
		oldASTRanges = oRanges;
		newASTRanges = nRanges;
	}
	
	public String toString() {
		return oldMethod.getSignature() + "--" + newMethod.getSignature();
	}
}
