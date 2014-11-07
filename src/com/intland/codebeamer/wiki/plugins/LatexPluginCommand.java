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

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

/**
 * Command bean for {@link LatexPlugin}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class LatexPluginCommand {

	/**
	 * The document/artifact's id which contains the latex/expression to render
	 */
	private Integer id;

	/**
	 * The data URL or classpath reference or local file-system reference for the text being rendered
	 */
	private String src;

	/**
	 * The encoding for the input data defined in the src. Defaults to UTF-8
	 */
	private String encoding = "UTF-8";

	/**
	 * The latex text to render, optional
	 */
	private String text;

	/**
	 * Plugin's title
	 */
	private String title;

	/**
	 * If PNG image is rendered (true), or false for GIF. Defaults to PNG
	 */
	private boolean usePng = true;

	/**
	 * Timeout in seconds to render the content.
	 */
	private int timeout = 30;

	/**
	 * The horizontal/vertical density: DPI of the generated image.
	 * see: {@link "http://www.imagemagick.org/script/command-line-options.php?ImageMagick=864f3jpkk4vtoblm5t94r7noe3#density"}
	 */
	private String density = "120";

	/**
	 * If caching for this latex rendering is enabled
	 */
	private boolean cache = true;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public boolean isUsePng() {
		return usePng;
	}

	public void setUsePng(boolean usePng) {
		this.usePng = usePng;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String getDensity() {
		return density;
	}

	public void setDensity(String density) {
		this.density = density;
	}

	public void setCache(boolean cache) {
		this.cache = cache;
	}

	public boolean isCache() {
		return cache;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}

}
