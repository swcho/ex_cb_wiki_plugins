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
package com.intland.codebeamer.wiki.plugins.recentactivities;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import com.intland.codebeamer.persistence.dao.impl.EntityCache;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.base.IdentifiableDto;
import com.intland.codebeamer.remoting.ArtifactType;
import com.intland.codebeamer.remoting.GroupType;
import com.intland.codebeamer.remoting.GroupTypeClassUtils;

/**
 * Enum lists the possible entities for the RecentActivities plugin
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public enum EntitiesFilter {

	@SuppressWarnings("deprecation")
	@Deprecated
	COMMIT (Integer.valueOf(GroupType.SCM_CHANGE_SET)),
	ISSUE (EntityCache.TRACKER_ITEM_TYPE),
	WIKIPAGE (EntityCache.ARTIFACT_TYPE, Integer.valueOf(ArtifactType.PROJECT_WIKIPAGE)),
	DOCUMENT (EntityCache.ARTIFACT_TYPE, Integer.valueOf(ArtifactType.FILE), Integer.valueOf(ArtifactType.BASELINE_TAG)),
	@SuppressWarnings("deprecation")
	@Deprecated
	FORUM (Integer.valueOf(GroupType.FORUM));

	// expected group-types
	private Set<Integer> groupTypes;
	// expected artifact-types
	private Set<Integer> artifactTypes = Collections.emptySet();

	private EntitiesFilter(Integer entityType, Integer... artifactType) {
		this.groupTypes = entityType != null ? Collections.singleton(entityType) : Collections.EMPTY_SET;
		if (artifactType != null && artifactType.length > 0) {
			this.artifactTypes = Collections.unmodifiableSet(new TreeSet<Integer>(Arrays.asList(artifactType)));
		}
	}

	public Set<Integer> getGroupTypes() {
		return groupTypes;
	}

	public Set<Integer> getArtifactTypes() {
		return artifactTypes;
	}

	/**
	 * Get which filter matches with the entity object
	 */
	public static EntitiesFilter getFilter(IdentifiableDto entity) {
		final Integer groupType = GroupTypeClassUtils.objectToGroupType(entity);
		Integer artifactType = null;
		if (entity instanceof ArtifactDto) {
			artifactType = ((ArtifactDto) entity).getTypeId();
		}

		for (EntitiesFilter filter:values()) {
			if (filter.groupTypes.contains(groupType)) {
				// is it the expected artifact type?
				if (artifactType == null || filter.getArtifactTypes().isEmpty() || filter.getArtifactTypes().contains(artifactType)) {
					return filter;
				}
			}
		}
		return null;
	}

}
