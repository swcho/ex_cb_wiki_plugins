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
package com.intland.codebeamer.wiki.plugins.traceabilityMatrix;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.AssertFalse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Splitter;
import com.intland.codebeamer.controller.TraceabilityMatrixController;
import com.intland.codebeamer.manager.TrackerViewManager;
import com.intland.codebeamer.wiki.plugins.command.base.AbstractMaxWikiPluginCommand;

/**
 * Command bean for {@link TraceabilityMatrixPlugin}
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class TraceabilityMatrixCommand extends AbstractMaxWikiPluginCommand {
	
	/**
	 * The list of tracker ids and view ids on the horizontal axis
	 * This is in form of "<tracker-id>/<view-id>,<tracker-id>/<view-id>..." format where there is a "/" character between tracker-ids and view ids and "," 
	 * between the next tracker/view id.
	 * 
	 * The view-id is optional
	 */
	private String horizontalTrackersAndViews;
	private String verticalTrackersAndViews;
	
	/**
	 * If folders are included in the matrix ?
	 */
	private boolean includeFolders = false;
	
	/**
	 * Contains the default view's id to use When the view-id is missing as parameter on the horizontal/vertical axis.
	 * Defaults to "All items" , see: {@link TrackerViewManager}
	 */
	private Integer defaultView = TrackerViewManager.ATV_ALL_ITEMS;
	
	/**
	 * If the traceability matrix can be edited, so clicking will show the matrix
	 */
	private boolean allowEditing = false;
	
	public TraceabilityMatrixCommand() {
		setTitle("Traceability Matrix");
		setMax(Integer.valueOf(TraceabilityMatrixController.TRACEABILITY_MATRIX_MAX_SIZE));	// set the default max number of issues on each axis
	}
	
	public boolean isIncludeFolders() {
		return includeFolders;
	}
	public TraceabilityMatrixCommand setIncludeFolders(boolean includeFolders) {
		this.includeFolders = includeFolders;
		return this;
	}
	
	public String getHorizontalTrackersAndViews() {
		return horizontalTrackersAndViews;
	}
	public TraceabilityMatrixCommand setHorizontalTrackersAndViews(String horizontalTrackersAndViews) {
		this.horizontalTrackersAndViews = horizontalTrackersAndViews;
		return this;
	}
	public TraceabilityMatrixCommand setVerticalTrackersAndViews(String verticalTrackersAndViews) {
		this.verticalTrackersAndViews = verticalTrackersAndViews;
		return this;
	}
	public String getVerticalTrackersAndViews() {
		return verticalTrackersAndViews;
	}
	
	private List<Pair<Integer,Integer>> parseTrackerAndViewIds(String idparams) {
		List<Pair<Integer,Integer>> parsed = new ArrayList<Pair<Integer,Integer>>();
		if (idparams == null) {
			return parsed;
		}
		for (String trackerIdAndViewId : Splitter.on(",").split(idparams)) {
			String[] parts = StringUtils.split(trackerIdAndViewId, "/");
			Integer trackerId = null;
			Integer viewId = null;
			if (parts.length > 0) {
				trackerId = Integer.valueOf(StringUtils.trimToNull((parts[0])));
			}
			if (parts.length > 1) {
				viewId = Integer.valueOf(StringUtils.trimToNull((parts[1])));
			}
			if (trackerId != null) {
				if (viewId == null) {
					viewId = defaultView;
				}
				parsed.add(Pair.of(trackerId, viewId));
			}			
		}
		return parsed;
	}
	
	public List<Pair<Integer,Integer>> parseHorizontalTrackersAndViews() {
		return parseTrackerAndViewIds(horizontalTrackersAndViews);
	}
	
	public List<Pair<Integer,Integer>> parseVerticalTrackersAndViews() {
		return parseTrackerAndViewIds(verticalTrackersAndViews);
	}
	
	/**
	 * Validator method for empty horizontal axis
	 */
	@AssertFalse(message = "{traceabilityMatrix.invalid.horizontal.axis.empty}")
	public boolean isEmptyHorizontalAxis() {
		return parseHorizontalTrackersAndViews().isEmpty();
	}
	
	@AssertFalse(message = "{traceabilityMatrix.invalid.vertical.axis.empty}")
	public boolean isEmptyVerticalAxis() {
		return parseVerticalTrackersAndViews().isEmpty();
	}
	
	/**
	 * Get/set the default view
	 * @return the defaultView
	 */
	public Integer getDefaultView() {
		return defaultView;
	}
	public void setDefaultView(Integer defaultView) {
		this.defaultView = defaultView;
	}
	
	public boolean isAllowEditing() {
		return allowEditing;
	}
	public void setAllowEditing(boolean allowEditing) {
		this.allowEditing = allowEditing;
	}
	
	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}	

}
