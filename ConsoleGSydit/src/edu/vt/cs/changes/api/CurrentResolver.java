package edu.vt.cs.changes.api;

import java.util.Set;

import edu.vt.cs.append.FineChangesInMethod;
import edu.vt.cs.diffparser.util.SourceCodeRange;

public class CurrentResolver {
	public static APIResolver current_resolver;
	public static Set<SourceCodeRange> current_ranges;
	public static FineChangesInMethod current_allchanges;
	public static int kaigua = 0;
	
	public static boolean isinsertversion() {
		if (kaigua == 1) return true;
		else return false;
	}
	
	public static void set_current_resolver(APIResolver inre) {
		current_resolver = inre;
	}
	
	public static void set_current_ranges(Set<SourceCodeRange> inra) {
		current_ranges = inra;
	}
	
	public static void set_current_allchanges(FineChangesInMethod inac) {
		current_allchanges = inac;
	}
}
