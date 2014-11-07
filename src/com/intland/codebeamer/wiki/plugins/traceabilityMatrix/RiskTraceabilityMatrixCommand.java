/*
 * Copyright by Intland Software
 *
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Intland Software. ("Confidential Information"). You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Intland.
 */
package com.intland.codebeamer.wiki.plugins.traceabilityMatrix;

import com.intland.codebeamer.controller.support.TraceabilityBrowserSupport;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractWikiPluginCommand;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author <a href="mailto:gabor.nagy@intland.com">Gabor Nagy</a>
 */
public class RiskTraceabilityMatrixCommand extends AbstractWikiPluginCommand {

	private List<TraceabilityBrowserSupport.TrackerParameter> trackers;

	public List<TraceabilityBrowserSupport.TrackerParameter> getTrackersAsParameterList() {
		return trackers;
	}

	public String getTrackers() {
		return trackers.toString();
	}

	public void setTrackers(String trackers) {
		this.trackers = extractParameters(trackers);
	}

	/**
	 * Extracts IDs from a <code>trackers</code> parameter given to the plugin via wiki markup.
	 *
	 * Example: <code>trackers='1030(1123-1124-1125),1035,1036(1155-1156-1157),1040'</code>
	 *
	 * Where the numbers in the main list are tracker IDs, the optional numbers in parentheses after them are
	 * tracker item exclusions. Excluded tracker items will not be displayed in the result.
	 *
	 * @param parameterString String representation of <code>trackers</code> parameter (given in the wiki markup)
	 * @return List of parameters given
	 */
	private List<TraceabilityBrowserSupport.TrackerParameter> extractParameters(String parameterString) {
		List<TraceabilityBrowserSupport.TrackerParameter> parameters = new ArrayList<TraceabilityBrowserSupport.TrackerParameter>();

		final List<String> sections = Arrays.asList(StringUtils.split(parameterString, ","));
		for (String section : sections) {

			Integer trackerId;
			Set<Integer> excludedItemIds = new HashSet<Integer>();

			if (section.contains("(")) {
				final int start = section.indexOf("(");
				final int end = section.indexOf(")");
				if (end < start + 1) {
					throw new IllegalArgumentException("Malformed trackers parameter!");
				}
				final String itemList = section.substring(start + 1, end);
				final List<String> items = Arrays.asList(StringUtils.split(itemList, "-"));
				for (String item : items) {
					excludedItemIds.add(Integer.valueOf(Integer.parseInt(item)));
				}
				trackerId = Integer.valueOf(Integer.parseInt(section.substring(0, start)));
			} else {
				trackerId = Integer.valueOf(Integer.parseInt(section));
			}

			parameters.add(new TraceabilityBrowserSupport.TrackerParameter(trackerId, excludedItemIds));
		}

		return parameters;
	}
}
