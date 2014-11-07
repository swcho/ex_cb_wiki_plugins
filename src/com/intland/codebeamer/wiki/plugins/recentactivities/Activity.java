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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import com.intland.codebeamer.persistence.dto.ActivityLogEntryDto.Type;
import com.intland.codebeamer.persistence.dto.TrackerItemHistoryEntryDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.dto.base.IdentifiableDto;
import com.intland.codebeamer.remoting.DescriptionFormat;

/**
 * Activity-record contains information about an user/system activity.
 * see {@link ActivityStreamManager}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class Activity<T extends IdentifiableDto> implements Comparable<Activity>{

	/**
	 * When did the activity happen?
	 */
	private Date date;
	/**
	 * Who performed this activity
	 */
	private UserDto madeBy;

	/** Type of this activity */
	private Type type;

	/**
	 * The target NamedDto which has been changed/was affected by the action.
	 *
	 * IMPORTANT: this entity should contain the historical data/properties which the entity had AFTER the change.
	 * For example if the user changed the 'name' property to 'apple' in this change, but the current value of the 'name' property is now 'moon', then this entity
	 * should contain the 'apple' as value.
	 */
	private T target;

	private boolean prerendered = false;

	/**
	 * Inner class representing detailed information about the change.
	 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
	 */
	public static class Change {

		public Change(String subject) {
			this(subject, null);
		}

		public Change(String subject, Object detail) {
			this.type = Type.Modify;
			this.subject = subject;
			this.detail = detail;
		}

		public Change(Type type, String subject, Object detail) {
			this.type = type != null ? type : Type.Modify;
			this.subject = subject;
			this.detail = detail;
		}

		/** The type of the change */
		private Type type;

		/**
		 * Any short text describing the change,
		 */
		private String subject;

		/**
		 * Format of the subject. Either wiki or html or plain-text.
		 */
		private String subjectFormat = DescriptionFormat.PLAIN_TEXT;

		/**
		 * Detail object about this change. Can be a {@link TrackerItemHistoryEntryDto}, for example.
		 */
		private Object detail;

		public Type getType() {
			return type;
		}

		public String getSubject() {
			return subject;
		}

		public Change setSubject(String subject) {
			this.subject = subject;
			return this;
		}

		public String getSubjectFormat() {
			return subjectFormat;
		}

		public Change setSubjectFormat(String subjectFormat) {
			this.subjectFormat = subjectFormat;
			return this;
		}

		public Object getDetail() {
			return detail;
		}

		public Change setDetail(Object detail) {
			this.detail = detail;
			return this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + type.hashCode();
			result = prime * result + ((detail == null) ? 0 : detail.hashCode());
			result = prime * result + ((subject == null) ? 0 : subject.hashCode());
			result = prime * result + ((subjectFormat == null) ? 0 : subjectFormat.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Change other = (Change) obj;
			if (!type.equals(other.getType())) {
				return false;
			}
			if (detail == null) {
				if (other.detail != null)
					return false;
			} else if (!detail.equals(other.detail))
				return false;
			if (subject == null) {
				if (other.subject != null)
					return false;
			} else if (!subject.equals(other.subject))
				return false;
			if (subjectFormat == null) {
				if (other.subjectFormat != null)
					return false;
			} else if (!subjectFormat.equals(other.subjectFormat))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return ReflectionToStringBuilder.toString(this);
		}

	}

	/**
	 * Multiple smaller changes may be part of the activity.
	 * For example when the user changes 5 properties of an issue, then there should be as many Change objects here
	 */
	private List<Change> changes = new ArrayList<Change>();

	public Activity(Date happened, UserDto madeBy, Type type, T target) {
		this.date = happened;
		this.madeBy = madeBy;
		this.type = type;
		this.target = target;
	}


	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public UserDto getMadeBy() {
		return madeBy;
	}

	public void setMadeBy(UserDto madeBy) {
		this.madeBy = madeBy;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public T getTarget() {
		return target;
	}

	public void setTarget(T target) {
		this.target = target;
	}

	public int compareTo(Activity o) {
		// descending by the date
		int c = - this.date.compareTo(o.date);
		if (c != 0) {
			return c;
		}
		// then sort by target objects
		return this.target.compareTo(o.target);
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this);
	}

	public List<Change> getChanges() {
		return changes;
	}

	public void setChanges(List<Change> changes) {
		this.changes = changes;
	}

	// generated by eclipse
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((madeBy == null) ? 0 : madeBy.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		return equals(obj, 0);
	}

	/**
	 * Compare two activities
	 *
	 * @param obj
	 * @param timeDifference The time difference in milliseconds that's accepted when comparing the two dates
	 */
	// Note:  Generated by eclipse, but modified to add timeDifference
	public boolean equals(Object obj, int timeDifference) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Activity other = (Activity) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!isDateNearlyEquals(date,other.date, timeDifference))
			return false;
		if (type != other.getType()) {
			return false;
		}
		if (madeBy == null) {
			if (other.madeBy != null)
				return false;
		} else if (!madeBy.equals(other.madeBy))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}

	/**
	 * Compares two dates with some time difference accepted
	 * @param timeDifference The time difference in milliseconds that's accepted when comparing the two dates
	 */
	protected boolean isDateNearlyEquals(Date date1, Date date2, int timeDifference) {
		if (date1 == null || date2 == null) {
			return false;
		}

		long delta = date1.getTime() - date2.getTime();
		return Math.abs(delta) <= timeDifference;
	}

	/**
	 * If the activity contains a pre-rendered content
	 */
	public boolean isPrerendered() {
		return prerendered;
	}

	public void setPrerendered(boolean prerendered) {
		this.prerendered = prerendered;
	}

}
