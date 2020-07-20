package edu.vt.cs.changes;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import edu.vt.cs.diffparser.util.SourceCodeRange;
import edu.vt.cs.graph.ClientField;
import partial.code.grapa.mapping.ClientMethod;

public class ChangeFieldData {

	public ClientField oldField;
	public ClientField newField;
	public List<SourceCodeRange> oldASTRanges;
	public List<SourceCodeRange> newASTRanges;
	
	public Map<IVariableBinding, Set<SourceCodeRange>> oldFieldBindingMap = null;
	public Map<IVariableBinding, Set<SourceCodeRange>> newFieldBindingMap = null;
	
	public ChangeFieldData (ClientField oField, ClientField nField, List<SourceCodeRange> oRanges,
			List<SourceCodeRange> nRanges){
		oldField = oField;
		newField = nField;
		oldASTRanges = oRanges;
		newASTRanges = nRanges;
	}
}
