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
package com.intland.codebeamer.wiki.plugins;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.manager.TrackerItemManager;
import com.intland.codebeamer.manager.TrackerManager;
import com.intland.codebeamer.manager.util.ActionData;
import com.intland.codebeamer.persistence.dto.TrackerChoiceOptionDto;
import com.intland.codebeamer.persistence.dto.TrackerDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerLayoutLabelDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.remoting.DescriptionFormat;
import com.intland.codebeamer.wiki.plugins.base.AbstractCodeBeamerWikiPlugin;

/**
 * Plugin to display a form to submit tracker items from a wiki page.
 * <p>
 * The example below creates a new tracker item in the tracker with ID=1.
 * <pre>
[{FormSet form='testForm' trackerId=1}]
[{FormOutput form='testForm' handler='SimpleTrackerFormPlugin' populate='handler'}]
[{FormOpen form='testform'}]

|Summary:|[{FormInput type='text' name='shortDescription' size=80}]\\
|Description:|[{FormTextarea name='longDescription' rows=5 cols=80}]
|Priority:|[{FormSelect name='priority' value='Low;High'}]
[{FormInput type='submit' name='OK' value='OK'}]
[{FormInput type='submit' name='CANCEL' value='Cancel'}]

[{FormClose}]
</pre>
 *
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class SimpleTrackerFormPlugin extends AbstractCodeBeamerWikiPlugin {
	private final static Logger log = Logger.getLogger(SimpleTrackerFormPlugin.class);

	private final static String SHORT_DESCRIPTION = "shortDescription";
	private final static String LONG_DESCRIPTION = "longDescription";
	private final static String PRIORITY = "priority";
	private final static String TRACKER_ID = "trackerId";
	private final static String CANCEL = "CANCEL";

	public String execute(WikiContext wcontext, Map params) throws PluginException {
		if (StringUtils.isNotBlank((String)params.get(CANCEL))) {
			// Cancel was hit.
			return null;
		}

		String shortDescriptionString = (String)params.get(SHORT_DESCRIPTION);
		if (shortDescriptionString == null) {
			return null;
		}

		// Validation
		String shortDescription = StringUtils.trimToNull(shortDescriptionString);
		String longDescription = StringUtils.trimToNull((String)params.get(LONG_DESCRIPTION));
		String priority = StringUtils.trimToNull((String)params.get(PRIORITY));

		if (shortDescription == null) {
			String msg = "Summary is mandatory";
			log.warn(msg);
			throw new PluginException(msg);
		}

		if (longDescription == null) {
			String msg = "Description is mandatory";
			log.warn(msg);
			throw new PluginException(msg);
		}

		String trackerIdStr = StringUtils.trimToNull((String)params.get(TRACKER_ID));
		if (trackerIdStr == null) {
			String msg = "Tracker id is unset";
			log.warn(msg);
			throw new PluginException(msg);
		}

		Integer trackerId = null;
		try {
			trackerId = new Integer(trackerIdStr);
		} catch (Throwable ex) {
			String msg = "Couldn't parse tracker id <" + trackerIdStr + ">";
			log.warn(msg);
			throw new PluginException(msg);
		}

		// Prepare and set related data
		UserDto user = getUserFromContext(wcontext);

		TrackerDto tracker = new TrackerDto();
		tracker.setId(trackerId);

		TrackerItemDto item = new TrackerItemDto();
		item.setTracker(tracker);
		item.setName(shortDescription);
		item.setDescription(longDescription);
		item.setDescriptionFormat(DescriptionFormat.WIKI);
		item.setNamedPriority(getPriority(tracker, priority));

		try {
			TrackerItemManager.getInstance().create(user, item, new ActionData(wcontext.getHttpRequest()));
		} catch (Throwable ex) {
			String msg = ex.getMessage();
			log.warn(msg);
			throw new PluginException(msg, ex);
		}

		return null;
	}

	protected TrackerChoiceOptionDto getPriority(TrackerDto tracker, String priority) {
		TrackerChoiceOptionDto option = null;
		if (StringUtils.isNotBlank(priority)) {
			Map<Object,TrackerChoiceOptionDto> priorities = TrackerManager.getInstance().getTrackerChoiceOptions(tracker).get(Integer.valueOf(TrackerLayoutLabelDto.PRIORITY_LABEL_ID));
			if ((option = priorities.get(priority.toLowerCase())) == null) {
				try {
					option = priorities.get(Integer.valueOf(priority));
				} catch(NumberFormatException ex) {
				}
			}
		}

		return option;
	}
}
