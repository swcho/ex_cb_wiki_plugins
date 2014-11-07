package com.intland.codebeamer.wiki.plugins.googlemaps;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for Regular Expression.
 *
 * @author Steffen Schramm
 *
 */
public class RegEx {

	/**
	 * Returns a <code>Matcher</code> for a given regular expression and input
	 *
	 * @param regex
	 *            The regular expression
	 * @param input
	 *            The input code
	 * @return The <code>Matcher</code> object.
	 */
	public static Matcher getMatcher(String regex, String input) {
		return getMatcher(regex, input, 0);
	}

	/**
	 * Returns a <code>Matcher</code> for a given regular expression and input
	 *
	 * @param regex
	 *            The regular expression
	 * @param input
	 *            The input code
	 * @param flags
	 *            Match flags for Pattern.compile()
	 * @return The <code>Matcher</code> object.
	 */
	public static Matcher getMatcher(String regex, String input, int flags) {
		Pattern pattern = Pattern.compile(regex, flags);
		Matcher matcher = pattern.matcher(input);
		return matcher;
	}

}
