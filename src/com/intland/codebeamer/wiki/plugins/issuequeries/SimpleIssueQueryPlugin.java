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
package com.intland.codebeamer.wiki.plugins.issuequeries;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.controller.support.SimpleMessageResolver;
import com.intland.codebeamer.persistence.dao.TrackerItemDao;
import com.intland.codebeamer.persistence.dao.UserDao;
import com.intland.codebeamer.persistence.dao.impl.EntityCache;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto.Flag;
import com.intland.codebeamer.persistence.dto.TrackerLayoutLabelDto;
import com.intland.codebeamer.persistence.dto.TrackerTypeDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.util.Criteria;
import com.intland.codebeamer.persistence.util.Order;
import com.intland.codebeamer.persistence.util.Restrictions;
import com.intland.codebeamer.persistence.util.TrackerItemChoiceCriterion;
import com.intland.codebeamer.persistence.util.TrackerItemRestrictions;
import com.intland.codebeamer.ui.view.IssueStatusStyles;
import com.intland.codebeamer.ui.view.PriorityRenderer;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.DefaultWebBindingInitializer;

/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 */
public class SimpleIssueQueryPlugin extends AbstractCommandWikiPlugin<SimpleIssueQueryCommand> {
	private final static Logger logger = Logger.getLogger(SimpleIssueQueryPlugin.class);

	@Autowired
	private UserDao userDao;

	@Autowired
	private TrackerItemDao trackerItemDao;

	@Autowired
	private IssueStatusStyles issueStatusStyles;

	@Override
	public SimpleIssueQueryCommand createCommand() throws PluginException {
		SimpleIssueQueryCommand cmd = new SimpleIssueQueryCommand();
		setWebBindingInitializer(new DefaultWebBindingInitializer());
		cmd.setMax(Integer.valueOf(100));
		return cmd;
	}

	@Override
	protected Map populateModel(DataBinder binder, SimpleIssueQueryCommand command, Map params) throws PluginException {
		List<UserDto> submitters = getSubmitters(command.getSumitterEmailExp());
		// Add submitters by their ID.
		for (Integer userId : command.getSumitterId()) {
			UserDto u = new UserDto(userId);
			submitters.add(u);
		}

		TrackerLayoutLabelDto submittedBy = new TrackerLayoutLabelDto(TrackerLayoutLabelDto.SUBMITTED_BY_LABEL_ID);
		TrackerItemChoiceCriterion submittedByCriterion = new TrackerItemChoiceCriterion(getUser(), false);

		Criteria criteria = new Criteria();
		criteria.add(Restrictions.lt(TrackerItemRestrictions.TRACKER_TYPE_ID, TrackerTypeDto.MILESTONE.getId()));

		if (!submitters.isEmpty()) {
			for (UserDto submitter : submitters) {
			    submittedByCriterion.addLabelReferenceValue(submittedBy, EntityCache.USER_TYPE, submitter.getId());
			}

			criteria.add(submittedByCriterion);
		}

		if (!command.getNotInTrackerId().isEmpty()) {
			criteria.add(Restrictions.isNotIn(TrackerItemRestrictions.TRACKER_ID, command.getNotInTrackerId()));
		}

		if (!command.getNotInProjectId().isEmpty()) {
			criteria.add(Restrictions.isNotIn(TrackerItemRestrictions.PROJECT_ID, command.getNotInProjectId()));
		}

		if (!criteria.hasCriteria()) {
			throw new PluginException("No filter is defined");
		}

		criteria.getOrder().add(new Order(TrackerItemRestrictions.MODIFIED_AT, false));

		List<TrackerItemDto> issues = trackerItemDao.findByCriteria(getUser(), criteria, EnumSet.of(Flag.Deleted, Flag.Closed, Flag.Resolved));

		Map model = new HashMap();

		model.put("command", command);
		if (logger.isDebugEnabled()) {
			logger.debug("Found issues for <" + command +"> are: " + issues);
		}

		int numberofFoundIssues = issues.size();

		int max = command.getMax() != null ? command.getMax().intValue() : -1;
		if (max > 0 && numberofFoundIssues > max) {
			issues = issues.subList(0, max);
		}

		String displayedAndTotalIssues = getDisplayedAndTotalIssuesText(issues.size(), numberofFoundIssues, getSimpleMessageResolver());
		model.put("displayedAndTotalIssues", displayedAndTotalIssues);

		model.put("issues", issues);
		SimpleMessageResolver simpleMessageResolver = SimpleMessageResolver.getInstance(getWikiContext().getHttpRequest());
		model.put("priorityRenderer", new PriorityRenderer(getContextPath(), simpleMessageResolver));

		model.put("issueStatusStyles", issueStatusStyles);

		return model;
	}

	@Override
	protected String getTemplateFilename() {
		return "simpleIssueQuery-plugin.vm";
	}

	/**
	 * Collect all accounts with matching email address
	 * @param expressions
	 * @return
	 */
	protected List<UserDto> getSubmitters(List<String> expressions) {
		List<UserDto> submitters = new ArrayList<UserDto>();

		if (!CollectionUtils.isEmpty(expressions)) {
			List<Pattern> patterns = new ArrayList<Pattern>();
			for (String exp : expressions) {
				String e = StringUtils.trimToNull(exp);
				if (e != null) {
					patterns.add(Pattern.compile(e, Pattern.CASE_INSENSITIVE));
				}
			}

			if (!patterns.isEmpty()) {
				for (UserDto user : userDao.findAll()) {
					for (Pattern p : patterns) {
						List<String> emails = user.getEmails();
						if (!CollectionUtils.isEmpty(emails)) {
							for (String email : emails) {
								if (email != null && p.matcher(email).find()) {
									submitters.add(user);
								}
							}
						}
					}
				}
			}
		}
		return submitters;
	}

	public static String getDisplayedAndTotalIssuesText(int displayed, int total, SimpleMessageResolver simpleMessageResolver) {
		NumberFormat format = NumberFormat.getNumberInstance();

		String displayedAndTotalIssues;
		if (displayed < total) {
			displayedAndTotalIssues = simpleMessageResolver.getMessage("subset.x.of.y", format.format(displayed) , format.format(total));
		} else {
			displayedAndTotalIssues = format.format(displayed);
		}
		return displayedAndTotalIssues;
	}
}
