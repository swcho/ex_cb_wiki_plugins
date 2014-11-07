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
package com.intland.codebeamer.wiki.plugins.command.enums;

import com.intland.codebeamer.persistence.dto.TrackerLayoutLabelDto;

/**
 * Tracker item fields that can be specified as wiki plugin
 * parameters.
 *
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public enum TrackerItemField {
	ASSIGNEDTO(TrackerLayoutLabelDto.ASSIGNED_TO_LABEL_ID, "Assigned to"),
	CATEGORY(TrackerLayoutLabelDto.CATEGORY_LABEL_ID, "Category"),
	DETECTED(TrackerLayoutLabelDto.getChoiceFieldId(0), "Detected"),
	MILESTONE(TrackerLayoutLabelDto.MILESTONES_LABEL_ID, "Milestone"),
	/** @deprecated since CB-5.8 */
	@SuppressWarnings("deprecation")
	OS(TrackerLayoutLabelDto.OP_SYS_LABEL_ID, "OP-SYS"),
	PLATFORM(TrackerLayoutLabelDto.PLATFORM_LABEL_ID, "Platform"),
	PRIORITY(TrackerLayoutLabelDto.PRIORITY_LABEL_ID, "Priority"),
	RESOLUTION(TrackerLayoutLabelDto.RESOLUTION_LABEL_ID, "Resolution"),
	SEVERITY(TrackerLayoutLabelDto.SEVERITY_LABEL_ID, "Severity"),
	STATUS(TrackerLayoutLabelDto.STATUS_LABEL_ID, "Status"),
	SUBMITTER(TrackerLayoutLabelDto.SUBMITTED_BY_LABEL_ID, "Submitter"),
	TARGET(TrackerLayoutLabelDto.VERSION_LABEL_ID, "Target"),
	/** @since CB-5.8 */
	SUBJECT(TrackerLayoutLabelDto.SUBJECT_LABEL_ID, "Subject");

	private int id;
	/**
	 * It is primarily used to preserve correct capitalization in
	 * field names.
	 */
	private String displayName;

	private TrackerItemField(int id, String displayName) {
		this.id = id;
		this.displayName = displayName;
	}

	public int getId() {
		return id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isMembers() {
		return TrackerLayoutLabelDto.isUserReferenceField(id);
	}

}
