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
import edu.vt.cs.changes.api.DefUseChangeFact.DATA_CHANGE_TYPE;

public class DeleteFact extends DefUseChangeFact{

	public Map<String, DATA_CHANGE_TYPE> removedParameters = null;
	public Map<String, IBinding> removedVarBindings = null;
	
	public Map<String, Integer> removedDefs = null;
	public Map<String, Integer> removedUses = null;
	public Map<String,Integer> stpostoid = null;
	private String template;
	public Set<Integer> knownOldInsns;
	Node templateTree = null;
	
	public DeleteFact(Node l) {
		super();
		knownOldInsns = new HashSet<Integer>();
		removedParameters = new HashMap<String, DATA_CHANGE_TYPE>();
		removedVarBindings = new HashMap<String, IBinding>();
		this.removedDefs = new HashMap<String, Integer>();
		this.removedUses = new HashMap<String, Integer>();
		this.stpostoid = l.getpostid();
		templateTree = getTemplate(l, removedParameters, removedVarBindings);
//		template = render(templateTree);
//		System.out.println(template);
	}
	
	public void setTemplatewithlines(Node l, List<Integer> lines, GraphConvertor2 oc) {
		  templateTree = getTemplatewithRange(l, removedParameters, removedVarBindings, lines, oc);
		}
	
	public String getTemplate() {
		if (template == null) {
			template = render(templateTree);
		}		
		return template;
	}
	
	@Override
	public void removeParameter(String key) {
		removedParameters.remove(key);		
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
		buf.append("\nRemoved parameters: ");
		for (Entry<String, DATA_CHANGE_TYPE> entry : removedParameters.entrySet()) {
			buf.append("<").append(entry.getKey()).append(",").append(entry.getValue()).append(">");
		}
		buf.append("\n").append(template).append("\n");
		return buf.toString();
	}
}
