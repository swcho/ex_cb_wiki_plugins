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
package com.intland.codebeamer.wiki.plugins.dataset;

import java.util.Map;

import org.jfree.data.general.Dataset;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.persistence.dto.ProjectDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.wiki.plugins.dataset.producer.AbstractWikiDataSetProducer;

/**
 * Implementation classes gather (statistical) data into a {@link WikiDataSet}.
 * There is only one convention to follow when implementing producers:
 * <strong>data sets should grow "vertically", not "horizontally"</strong>.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public interface WikiDataSetProducer  {
	void setUser(UserDto user);
	void setProject(ProjectDto project);
	void setParams(Map<String, String> params); // FIXME this seems redundant in the interface as produce() receives it

	/**
	 * @param params is the parameters passed to {@link DataSetPlugin} without any change.
	 * 		Common parameters are passed to the producer constructor in case it is inherited
	 * 		from {@link AbstractWikiDataSetProducer}.
	 */
	Dataset produce(Map<String, String> params) throws PluginException;
}
