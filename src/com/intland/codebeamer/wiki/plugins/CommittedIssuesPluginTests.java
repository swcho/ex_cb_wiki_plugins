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
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.intland.codebeamer.controller.ControllerUtils;
import com.intland.codebeamer.persistence.dto.ScmChangeSetDto;
import com.intland.codebeamer.persistence.dto.ScmRepositoryDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;
import com.intland.codebeamer.persistence.dto.UserDto;
import com.intland.codebeamer.persistence.test.AbstractProjectSCMPersistenceTests;
import com.intland.codebeamer.utils.MultiValue;

/**
 * Unit test for {@link CommittedIssuesPlugin}
 *
 * @author <a href="mailto:zoltan.luspai@intland.com">Zoltan Luspai</a>
 */
public class CommittedIssuesPluginTests extends AbstractProjectSCMPersistenceTests {

	private ScmRepositoryDto repository;
	private CommittedIssuesPlugin plugin;
	private UserDto user;

	private List<ScmChangeSetDto> testData;

	@SuppressWarnings("deprecation")
	@Override
	protected void onSetUp() throws Exception {
		super.onSetUp();
		repository = createRepository(getProject(), "svn");
		garbage.add(repository);

		plugin = new CommittedIssuesPlugin();
		ControllerUtils.autoWire(plugin, getApplicationContext());
		user = getProjectAdmin();

		testData = createTestData();
	}

	// the start/reference date is the current hour
	private Calendar createReferenceDate() {
		return createReferenceDate(0);
	}
	private Calendar createReferenceDate(int minute) {
		Calendar today = Calendar.getInstance();
		today.set(Calendar.YEAR, 2014);
		today.set(Calendar.MONTH, Calendar.MARCH);
		today.set(Calendar.DAY_OF_MONTH, 17);
		today.set(Calendar.HOUR_OF_DAY, 9);
		today.set(Calendar.MINUTE, minute);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MILLISECOND, 0);
		return today;
	}

	// creates one changeset for each minute of 11:00
	private List<ScmChangeSetDto> createTestData() {
		List<ScmChangeSetDto> changes = new ArrayList<ScmChangeSetDto>();
		Calendar cal = createReferenceDate();
		for (int i=0; i< 30; i++) {
			cal.set(Calendar.MINUTE, i);

			ScmChangeSetDto change = createChangeSet(user, repository);
			change.setRevision("minute_" + i);
			change.setSubmittedAt(cal.getTime());
			scmChangeSetDao.create(change);
			garbage.add(change);
			changes.add(change);
		}
		return changes;
	}

	private CommittedIssuesPluginCommand initCommand() {
		CommittedIssuesPluginCommand command = new CommittedIssuesPluginCommand();
		command.setRepositoryId(repository.getId());
		command.setMax(Integer.valueOf(1000));
		return command;
	}

	/**
	 * Test how the plugin collects/populates the data filtering by date
	 */
	public void testCollectDataByDate() {
		CommittedIssuesPluginCommand command = initCommand();

		// test with a date range in yesterday
		Calendar yesterday = createReferenceDate();
		yesterday.add(Calendar.DAY_OF_YEAR, -1);
		command.setStartDate(yesterday.getTime());
		yesterday.add(Calendar.HOUR_OF_DAY, +1);
		command.setEndDate(yesterday.getTime());
		MultiValue<List<TrackerItemDto>, List<ScmChangeSetDto>> found = plugin.findIssuesAndChangeSetsAffected(user, command);
		assertFindIssuesAndChangeSetsAffectedInvariants(found);
		assertTrue("There is no date overlap with the data-set, must not find anything", CollectionUtils.intersection(found.getRight(), testData).isEmpty());

		// test with a 10 minute range: 11:10->11:20
		command.setStartDate(createReferenceDate(10).getTime());
		command.setEndDate(createReferenceDate(20).getTime());
		found = plugin.findIssuesAndChangeSetsAffected(user, command);
		assertFindIssuesAndChangeSetsAffectedInvariants(found);

		Collection<ScmChangeSetDto> foundChangesInRange = found.getRight();//CollectionUtils.intersection(found.getRight(), testData);
		assertFalse("The changes in 11:10->11:20 should be found", foundChangesInRange.isEmpty());
		assertEquals("As many changes as many minutes (11)", 11, foundChangesInRange.size());
		assertChangesAreInTimeRange(foundChangesInRange, 10, 20);
	}

	@SuppressWarnings("deprecation")
	private void assertChangesAreInTimeRange(Collection<ScmChangeSetDto> changes, int startMinute, int endMinute) {
		for (ScmChangeSetDto change:changes) {
			assertTrue(change.getSubmittedAt().getMinutes() >= startMinute);
			assertTrue(change.getSubmittedAt().getMinutes() <= endMinute);
		}
	}

	/**
	 * Check invariant asserts between the multi-value of issues and changes collection
	 */
	private void assertFindIssuesAndChangeSetsAffectedInvariants(MultiValue<List<TrackerItemDto>, List<ScmChangeSetDto>> found) {
		List issues = new ArrayList(found.getLeft());
		for (ScmChangeSetDto change:found.getRight()) {
			assertTrue(CollectionUtils.isSubCollection(change.getTrackerItems(), issues));
		}
	}

	/**
	 * Test filtering by revision range.
	 */
	public void testCollectDataByRevision() {
		// here we use that the revision now contains the minute when the change is created, so this very similar to the date range test above
		CommittedIssuesPluginCommand command = initCommand();

		// test with a 10 minute range: 11:10->11:20
		command.setStartRevision("minute_10");
		command.setEndRevision("minute_20");
		MultiValue<List<TrackerItemDto>, List<ScmChangeSetDto>> found = plugin.findIssuesAndChangeSetsAffected(user, command);
		assertFindIssuesAndChangeSetsAffectedInvariants(found);

		Collection<ScmChangeSetDto> foundChangesInRange = found.getRight(); //CollectionUtils.intersection(found.getRight(), testData);	// drop data may have left by the previous test
		assertFalse("The changes in 11:10->11:20 should be found", foundChangesInRange.isEmpty());
		assertEquals("As many changes as many revisions", 11, foundChangesInRange.size());
		assertChangesAreInTimeRange(foundChangesInRange, 10, 20);
	}

	/**
	 * Test filtering by both revision range and date range
	 */
	public void testCollectDataByRevisionAndDate() {
		// different combinations of revision and date numbers
		testCollectDataByRevisionAndDate("minute_10", "minute_20", 12, 33, 12, 20);
		testCollectDataByRevisionAndDate("minute_10", null, 12, 33, 12, 33);
		testCollectDataByRevisionAndDate("minute_10", "minute_20", 8, 19, 10, 19);
	}

	private void testCollectDataByRevisionAndDate(String startRev, String endRev, int startMinutes, int endMinutes, int expectedStart, int expectedEnd) {
		CommittedIssuesPluginCommand command = initCommand();

		command.setStartRevision(startRev);
		command.setEndRevision(endRev);
		command.setStartDate(createReferenceDate(startMinutes).getTime());
		command.setEndDate(createReferenceDate(endMinutes).getTime());

		MultiValue<List<TrackerItemDto>, List<ScmChangeSetDto>> found;
		found = plugin.findIssuesAndChangeSetsAffected(user, command);
		assertFindIssuesAndChangeSetsAffectedInvariants(found);

		Collection<ScmChangeSetDto> foundChangesInRange = CollectionUtils.intersection(found.getRight(), testData); 	// drop data may have left by previous tests
		assertChangesAreInTimeRange(foundChangesInRange, expectedStart, expectedEnd);
	}

}
