package edu.vt.cs.changes.api;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Queue;

import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;

public class NodeManipulator {
	/**
	 * The return tree contain nodes with id's
	 * @param n
	 * @return
	 */
	public static Node getCopy(Node n) {
		Queue<Node> queue = new LinkedList<Node>();
		Queue<Node> queue2 = new LinkedList<Node>();
		int index = 0;
		Node tmp = getSingleNodeCopy(n, index++); 	
		queue.add(n);
		queue2.add(tmp);
		Node original = null;
		Node copy = null;
		Node child = null;
		Node child2 = null;
		while(!queue.isEmpty()) {
			original = queue.remove();
			copy = queue2.remove();
			Enumeration<Node> cEnum = original.children();
			while (cEnum.hasMoreElements()) {
				child = cEnum.nextElement();
				child2 = getSingleNodeCopy(child, index++);
				copy.add(child2);
				queue.add(child);
				queue2.add(child2);
			}
		}
		return tmp;
	}
	
	
	public static Node getSingleNodeCopy(Node n, int id) {
		Node tmp = new Node(n.getLabel(), n.getValue());
		tmp.setUserObject(n.getUserObject());
		tmp.id = id;
		tmp.subStmtStarts = n.subStmtStarts;
		tmp.subStmtEnds = n.subStmtEnds;
		tmp.setEntity(n.getEntity());
		return tmp;
	}
}
