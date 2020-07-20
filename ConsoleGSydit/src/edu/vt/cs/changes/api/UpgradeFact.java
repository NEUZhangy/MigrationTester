package edu.vt.cs.changes.api;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.IBinding;
import org.stringtemplate.v4.ST;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import edu.vt.cs.append.terms.Term;
import edu.vt.cs.append.terms.VariableTypeBindingTerm;

public class UpgradeFact extends DefUseChangeFact{
	
	private String oldAPICall;
	private String newAPICall;	
	public Map<String, DATA_CHANGE_TYPE> removedParameters = null;
	public Map<String, IBinding> removedVarBindings = null;
	
	public Map<String, DATA_CHANGE_TYPE> addedParameters = null;
	public Map<String, IBinding> addedVarBindings = null;
	
	public Map<String, Integer> addedDefs = null;
	public Map<String, Integer> addedUses = null;
	public Map<String, Integer> removedDefs = null;
	public Map<String, Integer> removedUses = null;
	public Map<String,Integer> old_stpostoid = null;
	public Map<String,Integer> new_stpostoid = null;
	public Set<String> reusedParameters = null;	
	public Set<Integer> knownOldInsns;
	public Set<Integer> knownNewInsns;
	private Node originalLeft;
	private Node originalRight;
	Node templateLeft;
	Node templateRight;
	
	public UpgradeFact(Node l, Node r) {			
		super();
		this.originalLeft = l;
		this.originalRight = r;
		this.old_stpostoid = l.getpostid();
		this.new_stpostoid = r.getpostid();
		removedParameters = new HashMap<String, DATA_CHANGE_TYPE>();
		removedVarBindings = new HashMap<String, IBinding>();
		addedParameters = new HashMap<String, DATA_CHANGE_TYPE>();
		addedVarBindings = new HashMap<String, IBinding>();
		reusedParameters = new HashSet<String>();		
		knownOldInsns = new HashSet<Integer>();
		knownNewInsns = new HashSet<Integer>();
		addedDefs = new HashMap<String, Integer>();
		addedUses = new HashMap<String, Integer>();
		removedDefs = new HashMap<String, Integer>();
		removedUses = new HashMap<String, Integer>();
		
		templateLeft = getTemplate(l, removedParameters, removedVarBindings);
//		oldAPICall = render(templateLeft);
//		System.out.println(oldAPICall);
		System.out.print("");
		templateRight = getTemplate(r, addedParameters, addedVarBindings);
//		newAPICall = render(templateRight);
//		System.out.println(newAPICall);
		reusedParameters.addAll(removedParameters.keySet());
		reusedParameters.retainAll(addedParameters.keySet()); //common parameters
		for (String p : reusedParameters) {
			System.out.println(getexceptions());
			if (getexceptions().contains(p)) {
				continue;
			}
			addedParameters.remove(p);
			removedParameters.remove(p);
		}		
	}
	
	public void setTemplateLeftwithlines(Node l, List<Integer> lines, GraphConvertor2 oc) {
		templateLeft = getTemplatewithRange(l, removedParameters, removedVarBindings, lines, oc);
	}
	
	public void setTemplateRightwithlines(Node r, List<Integer> lines, GraphConvertor2 nc) {
		templateLeft = getTemplatewithRange(r, addedParameters, addedVarBindings, lines, nc);
	}
	
	
	
	public String getNewAPICall() {
		if (newAPICall == null) {
			newAPICall = render(templateRight);
		}
		return newAPICall;
	}
	
	public String getOldAPICall() {
		if (oldAPICall == null) {
			oldAPICall = render(templateLeft);			
		}		
		return oldAPICall;
	}
	
	@Override
	public void removeParameter(String key) {
		removedParameters.remove(key);
		addedParameters.remove(key);		
		super.removeParameter(key);				
	}
	
	@Override
	public String render() {
		return render(templateLeft) + "\n" + render(templateRight);
	}
	
	@Override
	public void replaceIdentifier(String str1, String str2) {
		replaceIdentifier(templateLeft, str1, str2);
		replaceIdentifier(templateRight, str1, str2);
		oldAPICall = null;		
		newAPICall = null;	
	}
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer(super.toString());
		buf.append("\nRemoved parameters:");
		for (Entry<String, DATA_CHANGE_TYPE> entry : removedParameters.entrySet()) {
			buf.append("<").append(entry.getKey()).append(",").append(entry.getValue()).append(">");
		}		
		buf.append("\nAdded parameters:");
		for (Entry<String, DATA_CHANGE_TYPE> entry : addedParameters.entrySet()) {
			buf.append("<").append(entry.getKey()).append(",").append(entry.getValue()).append(">");
		}
		buf.append("\n").append(oldAPICall).append("->\n").append(newAPICall);
		return buf.toString();
	}
}
