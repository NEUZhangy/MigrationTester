package edu.vt.cs.graph;

public class ReferenceEdge {

	public static final int FIELD_ACCESS = 1;
	public static final int METHOD_INVOKE = 2;
	public int type;
	public ReferenceNode from;
	public ReferenceNode to;
	public int count = 0;
	
	public ReferenceEdge(ReferenceNode from, ReferenceNode to, int type) {
		this.from = from;
		this.to = to;
		this.type = type;
	}
	
	public void increaseCount() {
		count++;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		result = prime * result + ((to == null) ? 0 : to.hashCode());
		result = prime * result + type;
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
		ReferenceEdge other = (ReferenceEdge) obj;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return from.toString() + "->" + to.toString() + "[" + count + "]";
	}
}
