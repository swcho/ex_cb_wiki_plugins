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

import java.util.Collections;
import java.util.List;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

import com.intland.codebeamer.controller.support.CommaStringToIntegerListPropertyEditor;
import com.intland.codebeamer.controller.support.CommaStringToStringListPropertyEditor;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractMaxWikiPluginCommand;

/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 */
public class SimpleIssueQueryCommand extends AbstractMaxWikiPluginCommand implements PropertyEditorRegistrar {
	private List<String> sumitterEmailExp = Collections.EMPTY_LIST;
	private List<Integer> sumitterId = Collections.EMPTY_LIST;
	private List<Integer> notInProjectId = Collections.EMPTY_LIST;
	private List<Integer> notInTrackerId = Collections.EMPTY_LIST;

	/**
	 * Registering binding for my fields.
	 * @see org.springframework.beans.PropertyEditorRegistrar#registerCustomEditors(org.springframework.beans.PropertyEditorRegistry)
	 */
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		registry.registerCustomEditor(List.class, "sumitterId", new CommaStringToIntegerListPropertyEditor());
		registry.registerCustomEditor(List.class, "notInProjectId", new CommaStringToIntegerListPropertyEditor());
		registry.registerCustomEditor(List.class, "notInTrackerId", new CommaStringToIntegerListPropertyEditor());
		registry.registerCustomEditor(List.class, "sumitterEmailExp", new CommaStringToStringListPropertyEditor());
	}

	public List<String> getSumitterEmailExp() {
		return sumitterEmailExp;
	}

	public void setSumitterEmailExp(List<String> sumitterEmailExp) {
		this.sumitterEmailExp = sumitterEmailExp;
	}

	public List<Integer> getSumitterId() {
		return sumitterId;
	}

	public void setSumitterId(List<Integer> sumitterId) {
		this.sumitterId = sumitterId;
	}

	public List<Integer> getNotInProjectId() {
		return notInProjectId;
	}

	public void setNotInProjectId(List<Integer> notInProjectId) {
		this.notInProjectId = notInProjectId;
	}

	public List<Integer> getNotInTrackerId() {
		return notInTrackerId;
	}

	public void setNotInTrackerId(List<Integer> notInTrackerId) {
		this.notInTrackerId = notInTrackerId;
	}
}
