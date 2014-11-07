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
package com.intland.codebeamer.wiki.plugins.command.base;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Base POJO command bean for reuse in subclasses of AbstractCommandWikiPlugin.
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public abstract class AbstractWikiPluginCommand implements Cloneable {
	/**
	 * Optional title.
	 */
	private String title = null;

	/**
	 * Extra CSS class to add to the outermost element of the markup (typically a table).
	 */
	private String cssClass = null;
	/**
	 * Extra CSS style to add to the outermost element of the markup (typically a table).
	 */
	private String cssStyle = null;
	
	/**
	 * The body of the Wiki plugin is found here
	 */
	private String pluginBody;

	public final String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public final String getCssClass() {
		return cssClass;
	}

	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}

	public final String getCssStyle() {
		return cssStyle;
	}

	public void setCssStyle(String cssStyle) {
		this.cssStyle = cssStyle;
	}
	
	public String getPluginBody() {
		return pluginBody;
	}
	public void setPluginBody(String pluginBody) {
		this.pluginBody = pluginBody;
	}

	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	} 
		
}
