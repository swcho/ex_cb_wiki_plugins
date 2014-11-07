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

/**
 * Artifact types.
 * <p>
 * This is a public interface for the user, so the hidden artifact types
 * like e.g. "system calendar" are not listed here by design. 
 * 
 * @author <a href="mailto:aron.gombas@intland.com">Aron Gombas</a>
 */
public enum ArtifactType {
	DOCUMENT("Document"),
	WIKIPAGE("Wiki Page");
	
	/**
	 * It is primarily used to preserve correct capitalization in
	 * field names.  
	 */
	private String displayName;

	private ArtifactType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}	
}
