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
 * Command bean for {@link FeedPlugin}
 * 
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class FeedPluginCommand {
	
	/**
	 * The feed url to show
	 */
	private String url = "http://www.intland.com/rss";
	
	/**
	 * Number of feed entries to show
	 */
	private int max = 10;
	
	/**
	 * Timeout in seconds
	 */
	private int timeout = 4;

	/**
	 * How detailed the feed is ?
	 */
	private boolean feedDetails = false;
	private boolean entryDetails = false;
	
	/**
	 * Cache time for feed in seconds. Use -1 for disabling caching
	 */
	private Integer cacheTime = Integer.valueOf(5*60);	// 5 minutes as default

	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}

	public int getMax() {
		return max;
	}
	public void setMax(int max) {
		if (max < 0) {
			max = 0;
		}
		this.max = max;
	}

	public int getTimeout() {
		return timeout;
	}
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public boolean isFeedDetails() {
		return feedDetails;
	}
	public void setFeedDetails(boolean feedDetails) {
		this.feedDetails = feedDetails;
	}

	public boolean isEntryDetails() {
		return entryDetails;
	}
	public void setEntryDetails(boolean entryDetails) {
		this.entryDetails = entryDetails;
	}
	
	public Integer getCacheTime() {
		return cacheTime;
	}
	public void setCacheTime(Integer cacheTime) {
		this.cacheTime = cacheTime;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}
	
}
