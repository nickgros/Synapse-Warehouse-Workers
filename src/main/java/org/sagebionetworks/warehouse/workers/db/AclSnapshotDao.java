package org.sagebionetworks.warehouse.workers.db;

import java.util.List;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.warehouse.workers.model.AclSnapshot;

public interface AclSnapshotDao {

	/**
	 * Insert a batch of AclSnapshot into ACL_SNAPSHOT table
	 * 
	 * @param batch
	 */
	public void insert(List<AclSnapshot> batch);

	/**
	 * 
	 * @return an AclSnapshot given the timestamp, ownerId, and ownerType
	 */
	public AclSnapshot get(Long timestamp, Long ownerId, ObjectType ownerType);

	/**
	 * Truncate all of the data.
	 */
	public void truncateAll();
}
