package org.sagebionetworks.warehouse.workers.db;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.warehouse.workers.model.TeamSnapshot;
import org.sagebionetworks.warehouse.workers.utils.ObjectSnapshotTestUtil;
import org.springframework.dao.EmptyResultDataAccessException;

public class TeamSnapshotDaoImplTest {
	TeamSnapshotDao dao = TestContext.singleton().getInstance(TeamSnapshotDao.class);

	@Before
	public void before(){
		dao.truncateAll();
	}

	@After
	public void after(){
		dao.truncateAll();
	}

	@Test
	public void test() {
		TeamSnapshot snapshot1 = ObjectSnapshotTestUtil.createValidTeamSnapshot();
		TeamSnapshot snapshot2 = ObjectSnapshotTestUtil.createValidTeamSnapshot();

		dao.insert(Arrays.asList(snapshot1, snapshot2));
		assertEquals(snapshot1, dao.get(snapshot1.getTimestamp(), Long.parseLong(snapshot1.getId())));
		assertEquals(snapshot2, dao.get(snapshot2.getTimestamp(), Long.parseLong(snapshot2.getId())));

		TeamSnapshot snapshot3 = ObjectSnapshotTestUtil.createValidTeamSnapshot();
		snapshot3.setTimestamp(snapshot2.getTimestamp());
		snapshot3.setId(snapshot2.getId());
		dao.insert(Arrays.asList(snapshot3));
		assertEquals(snapshot2, dao.get(snapshot3.getTimestamp(), Long.parseLong(snapshot3.getId())));
	}

	@Test (expected=EmptyResultDataAccessException.class)
	public void notFoundTest() {
		dao.get(System.currentTimeMillis(), new Random().nextLong());
	}
}
