package edu.vt.cs.changes.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.IBinding;
import org.stringtemplate.v4.ST;

import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;

public class InsertFact extends DefUseChangeFact{

	public Map<String, DATA_CHANGE_TYPE> addedParameters = null;
	public Map<String, IBinding> addedVarBindings = null;
	public Map<String, Integer> addedDefs = null;
	public Map<String, Integer> addedUses = null;
	public Map<String, Integer> stpostoid = null;
	private String template;
	Node templateTree;
	public Set<Integer> knownNewInsns = null;
	
	public InsertFact(Node r) {
		super();
		this.addedParameters = new HashMap<String, DATA_CHANGE_TYPE>();
		this.addedVarBindings = new HashMap<String, IBinding>();
		this.knownNewInsns = new HashSet<Integer>();
		this.addedDefs = new HashMap<String, Integer>();
		this.addedUses = new HashMap<String, Integer>();
		this.stpostoid = r.getpostid();
		templateTree = getTemplate(r, addedParameters, addedVarBindings);
//		template = render(templateTree);
//		System.out.println(template);
	}
	
	public void setTemplatewithlines(Node r, List<Integer> lines, GraphConvertor2 nc) {
	  templateTree = getTemplatewithRange(r, addedParameters, addedVarBindings, lines, nc);
	}
	
	public String getTemplate() {
		if (template == null) {
			template = render(templateTree);
		}		
		return template;
	}
	
	@Override
	public void removeParameter(String key) {
		addedParameters.remove(key);		
		super.removeParameter(key);
	}
	
	@Override
	public String render() {
		return render(templateTree);
	}
	
	@Override
	public void replaceIdentifier(String str1, String str2) {		
		replaceIdentifier(templateTree, str1, str2);		
		template = null;
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer(super.toString());
		buf.append("\nAdded parameters: ");
		for (Entry<String, DATA_CHANGE_TYPE> entry : addedParameters.entrySet()) {
			buf.append("<").append(entry.getKey()).append(",").append(entry.getValue()).append(">");
		}		
		buf.append("\n").append(getTemplate()).append("\n");
		return buf.toString();
	}
}
