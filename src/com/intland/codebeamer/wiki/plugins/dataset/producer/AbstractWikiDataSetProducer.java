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
package com.intland.codebeamer.wiki.plugins.dataset.producer;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.TrackerLayoutLabelDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.util.TrackerItemPropertyLabelIdTranslator;
import com.intland.codebeamer.wiki.plugins.dataset.WikiDataSetProducer;

/**
 * This mixin provides utility functionality for concrete producers.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 * @version $Id$
 */
public abstract class AbstractWikiDataSetProducer implements WikiDataSetProducer {
	private static final String PRODUCER_DATEFORMAT = "yyyy-MM-dd";

	private UserDto user;
	private ProjectDto project;
	private Map<String, String> params;

	private TrackerItemPropertyLabelIdTranslator trackerItemPropertyLabelIdTranslator;

	public UserDto getUser() {
		return user;
	}

	public void setUser(UserDto user) {
		this.user = user;
	}

	public ProjectDto getProject() {
		return project;
	}

	public void setProject(ProjectDto project) {
		this.project = project;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public void setTrackerItemPropertyLabelIdTranslator(TrackerItemPropertyLabelIdTranslator trackerItemPropertyLabelIdTranslator) {
		this.trackerItemPropertyLabelIdTranslator = trackerItemPropertyLabelIdTranslator;
	}

	// - common parameters -----------------------------------------------------

	private Date startDate;
	private Date endDate;

	/**
	 * Lazily calculates certain common parameters based on {@link #params}.
	 */
	protected void lazyCalculateCommonParameters() throws PluginException {
		if (params != null) {
			int since = NumberUtils.toInt(params.get("since"));
			String startDateParam = params.get("startDate");
			String endDateParam = params.get("endDate");
			if (since == 0) { // Will use start/end date
				// The date value is in the wiki-markup thus the date format must be user independent.
				DateFormat dateFormat = new SimpleDateFormat(PRODUCER_DATEFORMAT);

				try {
					if (startDateParam != null) {
						startDate = dateFormat.parse(startDateParam);
					} else {
						if (project != null) {
							startDate = project.getCreatedAt();
						}
					}
					endDate = endDateParam != null ? (Date) dateFormat.parse(endDateParam) : new Date();
				} catch (ParseException ex) {
					throw new PluginException("Dates must be specified in \"" + PRODUCER_DATEFORMAT + "\" format", ex);
				}
			} else { // evaluate 'since' recent days
				since = Math.abs(since);
				endDate = new Date();
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(endDate);
				calendar.add(Calendar.DAY_OF_YEAR, -since);
				startDate = calendar.getTime();
			}
		}
	}

	protected Date getStartDate() throws PluginException {
		if(startDate == null) {
			lazyCalculateCommonParameters();
		}

		return startDate;
	}

	protected Date getEndDate() throws PluginException {
		if(endDate == null) {
			lazyCalculateCommonParameters();
		}

		return endDate;
	}

	// - utilities ------------------------------------------------------------

	/**
	 * Converts a string like "1,2, 3" to a List&lt;Integer&gt; or
	 * returns an empty list if invalid or <code>null</code>.
	 */
	protected List<Integer> parseCommaSeparatedIds(String commaSeparatedIds) {
		List<Integer> ids = new ArrayList();

		String tokens[] = StringUtils.split(StringUtils.trim(commaSeparatedIds), ",");
		if(tokens != null) {
			for(int i = 0; i < tokens.length; i++) {
				try {
					ids.add(Integer.valueOf(StringUtils.trim(tokens[i])));
				} catch(NumberFormatException ex) {
					// do nothing
				}
			}
		}

		return ids;
	}

	/**
	 * Converts a label name like "categories" to ID constants defined in {@link TrackerLayoutLabelDto} or
	 * returns <code>CATEGORY_LABEL_ID</code> if it is not matching or <code>null</code>.
	 */
	protected Integer getLabelIdByLabelName(String label) {
		Integer labelId = trackerItemPropertyLabelIdTranslator.getLabelId(label);
		return (labelId != null) ? labelId : Integer.valueOf(TrackerLayoutLabelDto.CATEGORY_LABEL_ID);
	}
}
