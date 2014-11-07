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
package com.intland.codebeamer.wiki.plugins.command;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import com.intland.codebeamer.controller.importexport.wiki.ExportWikiPageForm;
import com.intland.codebeamer.utils.URLCoder;
import com.intland.codebeamer.wiki.plugins.ExportWikiPlugin;

/**
 * Command bean for {@link ExportWikiPlugin}.
 * Uses nearly same parameters as the controller which exports the PDF {@link ExportWikiPageForm}
 * except few inconsistencies, which are solved by setters/getters delegating to the ExportWikiPageForm.
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 * $Id$
 */
public class ExportWikiPluginCommand extends ExportWikiPageForm implements Serializable{

	/**
	 * The text shown on the link for the wiki-plugin.
	 */
	private String linkText = null;

	/**
	 * Parsed page-ids as integers
	 */
	private List<Integer> pageIds = Collections.EMPTY_LIST;

	/**
	 * The id property receives the plugin parameter, same as wikiPageId
	 * @return the id
	 */
	public String getId() {
		return getWikiPageId();
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		setWikiPageId(id);
	}

	/**
	 * @return the headerText
	 */
	public String getHeaderText() {
		return getHeader();
	}

	/**
	 * @param headerText the headerText to set
	 */
	public void setHeaderText(String headerText) {
		setHeader(headerText);
	}

	/**
	 * @return the footerText
	 */
	public String getFooterText() {
		return getFooter();
	}

	/**
	 * @param footerText the footerText to set
	 */
	public void setFooterText(String footerText) {
		setFooter(footerText);
	}

	/**
	 * @return the linkText
	 */
	public String getLinkText() {
		if (StringUtils.isEmpty(linkText)) {
			return (getPath() == null ? "Export as " : "Archive as ") + StringUtils.upperCase(getFormat().name());
		}
		return linkText;
	}

	/**
	 * @param linkText the linkText to set
	 */
	public void setLinkText(String linkText) {
		this.linkText = linkText;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}

	/**
	 * @return the pageIds
	 */
	public List<Integer> getPageIds() {
		return pageIds;
	}

	/**
	 * @param pageIds the pageIds to set
	 */
	public void setPageIds(List<Integer> pageIds) {
		this.pageIds = pageIds;
	}

	/**
	 * Generate markup used by the ExportWiki-plugin form the settings stored in this bean.
	 */
	public String getExportWikiMarkup() {
		// create a new instance of this bean to get the default values.
		ExportWikiPluginCommand defaultValues = new ExportWikiPluginCommand();

		StringBuilder buf = new StringBuilder();
		buf.append("[{ExportWiki ");
		buf.append("id='").append(getId()).append("'");
		if (getRevision() != null) {
			buf.append(" revision='").append(getRevision()).append("'");
		}
		addParam (buf, "linkText", this.getLinkText(), defaultValues.getLinkText(), false);
		addParam (buf, "format" , this.getFormat().name(), defaultValues.getFormat().name() , false);
		addParam (buf, "path", this.getPath(), defaultValues.getPath(), false);
		addParam (buf, "headerText", this.getHeaderText(), defaultValues.getHeaderText(), false);
		addParam (buf, "footerText", this.getFooterText(), defaultValues.getFooterText(), false);
		addParam (buf, "dpi", this.getDpi() == null ? null : String.valueOf(this.getDpi()), String.valueOf(defaultValues.getDpi()), false);
		buf.append("}]");
		return buf.toString();
	}

	/**
	 * Add a parameter to the wiki markup, unless it is the default value
	 * @param buf
	 * @param paramName
	 * @param value
	 * @param defaultValue
	 * @param encode
	 */
	private void addParam(StringBuilder buf, String paramName, String value, String defaultValue, boolean encode) {
		if (!StringUtils.isEmpty(value)) {
			if (defaultValue != null && defaultValue.equals(value)) {
				return;	// don't add default value
			}

			if (encode) {
				value = URLCoder.encode(value);
			}
			buf.append("\n ").append(paramName).append("='").append(value).append("'");
		}
	}

}
