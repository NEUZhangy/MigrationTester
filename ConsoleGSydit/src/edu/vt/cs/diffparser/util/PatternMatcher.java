package edu.vt.cs.diffparser.util;

import java.util.regex.Pattern;

public class PatternMatcher {

	public static Pattern vPatternSSA = Pattern.compile("v[0-9]+");
	public static Pattern vPattern = Pattern.compile("V_[0-9]+");
	public static Pattern qPattern = Pattern.compile("QNAME_[0-9]+");
	public static Pattern tPattern = Pattern.compile("TYPE_[0-9]+");
	public static Pattern uPattern = Pattern.compile("U_[0-9]+");
	public static Pattern mPattern = Pattern.compile("m_[0-9]+");
}
