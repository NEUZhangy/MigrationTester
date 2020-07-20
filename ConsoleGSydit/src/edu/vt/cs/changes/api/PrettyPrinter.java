package edu.vt.cs.changes.api;

import java.util.List;

import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import edu.vt.cs.append.JavaExpressionConverter;
import edu.vt.cs.diffparser.util.SourceCodeRange;

public class PrettyPrinter {
 
	public static String printTemplates(List<Node> templates) {
		StringBuffer buf = new StringBuffer();
		String temp = null;
		Node rangeNode = null;
		SourceCodeRange range = null;
		Node n = null;
		Node n2 = null;
		for (int i = 0; i < templates.size(); i++) {
			n = templates.get(i);
			if (n.subStmtStarts == null) {
				temp = printSingleNode(n);
				buf.append(temp).append("\n");
			} else {
				List<Node> children = JavaExpressionConverter.getChildren(n);
				rangeNode = children.get(n.subStmtStarts.get(0));
				range = SourceCodeRange.convert(rangeNode.getEntity());
				temp = DefUseChangeFact.renderWithTypeInfo(n, null);
				temp = temp + "{" ;
				buf.append(temp).append("\n");
				for (; i < templates.size(); i++) {
					n2 = templates.get(i);															
					if (SourceCodeRange.convert(n2.getEntity()).isInside(range)) {
						temp = printSingleNode(n2);
						buf.append(temp).append("\n");
					}
				}
				buf.append("}").append("\n");
			}			
		}	
		String result = buf.toString();
		System.out.print(result);
		return result;
	}
	
	private static String printSingleNode(Node n) {
		String temp = DefUseChangeFact.renderWithTypeInfo(n, null);
		if (!temp.endsWith(";")) {
			temp = temp + ";";
		}
		return temp;
	}
}
