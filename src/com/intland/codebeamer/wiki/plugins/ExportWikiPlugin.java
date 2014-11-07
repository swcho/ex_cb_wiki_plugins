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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;

import com.ecyrd.jspwiki.plugin.PluginException;
import com.intland.codebeamer.controller.importexport.wiki.ExportWikiPageController.ExportFormat;
import com.intland.codebeamer.utils.URLCoder;
import com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin;
import com.intland.codebeamer.wiki.plugins.base.NamedPluginException;
import com.intland.codebeamer.wiki.plugins.command.ExportWikiPluginCommand;

/**
 * Plugin that enables embedding download links into the wiki markup.
 *
 * @author <a href="mailto:kao@intland.com">Alexey Kolesnikov</a>
 * @version $Id$
 */
public class ExportWikiPlugin extends AbstractCommandWikiPlugin<ExportWikiPluginCommand> {
	private static final String RENDER_TEMPLATE = "exportwiki-plugin.vm";

	/* Error messages. */
	private static final String MISSED_PAGE_IDS = "id parameter must be specified";
	private static final String INCORRECT_PAGE_ID =  "Error parsing id parameter";
	private static final String NO_PAGE_ID_AVAILABLE =  "No available pages found";

	@Override
	public ExportWikiPluginCommand createCommand() throws PluginException {
		return new ExportWikiPluginCommand();
	}

	/* (non-Javadoc)
	 * @see com.intland.codebeamer.wiki.plugins.base.AutoWiringCodeBeamerPlugin#getTemplateFilename()
	 */
	@Override
	public String getTemplateFilename() {
		return RENDER_TEMPLATE;
	}

	/* (non-Javadoc)
	 * @see com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin#validate(org.springframework.validation.DataBinder, java.lang.Object, java.util.Map)
	 */
	@Override
	protected void validate(DataBinder binder, ExportWikiPluginCommand command, Map params) throws NamedPluginException {
		super.validate(binder, command, params);
		BindingResult result = binder.getBindingResult();

		if (command.getFormat() == null) {
			result.rejectValue("format", "missing format value!");
		} else {
			if (!command.getFormat().isEnabled()) {
				result.rejectValue("format", "invalid value, only " + ExportFormat.getEnabledFormats() + " is allowed!");
			}
		}

		List<Integer> pageIds = new ArrayList<Integer>();

		if (StringUtils.isEmpty(command.getId())) {
			result.rejectValue("id", MISSED_PAGE_IDS);
		} else {
			String[] ids = command.getId().split(",");
			for (int i = 0; i < ids.length; i++) {
				try {
					int id = Integer.parseInt(ids[i].trim());
					pageIds.add(Integer.valueOf(id));
				} catch (Exception e) {
					result.rejectValue("id", INCORRECT_PAGE_ID, new Object[] { ids[i]} , "Invalid id value: " +ids[i]);
				}
			}

			if (pageIds.isEmpty()) {
				result.rejectValue("id", NO_PAGE_ID_AVAILABLE);
			}
		}
		command.setPageIds(pageIds);
	}

	/* (non-Javadoc)
	 * @see com.intland.codebeamer.wiki.plugins.base.AbstractCommandWikiPlugin#populateModel(org.springframework.validation.DataBinder, java.lang.Object, java.util.Map)
	 */
	@Override
	protected Map populateModel(DataBinder binder, ExportWikiPluginCommand command, Map params) throws PluginException {
		Map model = new HashMap();

		model.put("contextPath", getApplicationContextPath(getWikiContext()));
		model.put("linkText", command.getLinkText());
		model.put("outputFormat", command.getFormat());
		model.put("outputPath", StringUtils.isNotBlank(command.getPath()) ? URLCoder.encode(command.getPath()) : null);

		String wikiPageIds = command.getPageIds().toString();
		wikiPageIds = wikiPageIds.replaceAll("\\[|\\]|\\s", ""); // remove the [ and ] from the "[id1, id2..]"
		model.put("wikiPageIds", wikiPageIds);
		model.put("revision", command.getRevision());
		model.put("headerText", StringUtils.isNotBlank(command.getHeaderText()) ? URLCoder.encode(command.getHeaderText()) : null);
		model.put("footerText", StringUtils.isNotBlank(command.getFooterText()) ? URLCoder.encode(command.getFooterText()) : null);
		model.put("resolution", command.getDpi());

		return model;
	}

}
