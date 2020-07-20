package edu.vt.cs.graph;

import com.ibm.wala.types.MemberReference;

public class ReferenceNode{
	public MemberReference ref;
	
	public ReferenceNode(MemberReference ref) {
		this.ref = ref;
	}
	
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ref == null) ? 0 : ref.hashCode());
		return result;
	}




	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReferenceNode other = (ReferenceNode) obj;
		if (ref == null) {
			if (other.ref != null)
				return false;
		} else if (!ref.equals(other.ref))
			return false;
		return true;
	}




	@Override
	public String toString() {
		return ref.getSignature();
	}
}
