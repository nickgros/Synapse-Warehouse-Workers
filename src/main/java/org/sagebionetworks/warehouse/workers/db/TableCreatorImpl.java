package org.sagebionetworks.warehouse.workers.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.sagebionetworks.warehouse.workers.config.Configuration;
import org.sagebionetworks.warehouse.workers.utils.ClasspathUtils;
import org.sagebionetworks.warehouse.workers.utils.PartitionUtil;
import org.sagebionetworks.warehouse.workers.utils.PartitionUtil.Period;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import com.google.inject.Inject;

public class TableCreatorImpl implements TableCreator {
	public static final String WAREHOUSE_WORKERS_SCHEMA_KEY = "org.sagebionetworks.warehouse.workers.schema";
	private JdbcTemplate template;
	private DateTime startDate;
	private DateTime endDate;
	private String schema;
	public static final String CHECK_PARTITION = "SELECT COUNT(*) "
			+ "FROM INFORMATION_SCHEMA.PARTITIONS "
			+ "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND PARTITION_NAME = ?";
	public static final String ALL_PARTITIONS = "SELECT PARTITION_NAME "
			+ "FROM INFORMATION_SCHEMA.PARTITIONS "
			+ "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
	public static final String ADD_PARTITION = "ALTER TABLE %1$S "
			+ "ADD PARTITION (PARTITION %2$S VALUES LESS THAN (%3$d))";
	public static final String DROP_PARTITION = "ALTER TABLE %1$S DROP PARTITION %2$S";

	@Inject
	TableCreatorImpl (JdbcTemplate template, Configuration config) {
		this.template = template;
		startDate = config.getPartitionStartDate();
		endDate = config.getEndDate();
		schema = config.getProperty(WAREHOUSE_WORKERS_SCHEMA_KEY);
	}

	@Override
	public void createTable(String fileName) {
		template.update(ClasspathUtils.loadStringFromClassPath(fileName));
	}

	@Override
	public void createTableWithPartitions(String fileName, String tableName, String fieldName, Period period) {
		if (fileName == null || tableName == null || fieldName == null || period == null)
			throw new IllegalArgumentException();
		String query = ClasspathUtils.loadStringFromClassPath(fileName);
		String partitionString = PartitionUtil.buildPartitions(tableName, fieldName, period, startDate, endDate);
		query += partitionString;
		template.update(query);
	}

	@Override
	public void createTableWithDefaultPartition(String fileName, String tableName, String fieldName, Period period) {
		if (fileName == null || tableName == null || fieldName == null || period == null)
			throw new IllegalArgumentException();
		String query = ClasspathUtils.loadStringFromClassPath(fileName);
		String partitionString = PartitionUtil.buildPartitions(tableName, fieldName, period, startDate, startDate);
		query += partitionString;
		template.update(query);
	}

	@Override
	public void createTable(TableConfiguration config) {
		if (config.isCreateWithPartitions()) {
			createTableWithPartitions(config.getSchemaFileName(),
					config.getTableName(), config.getPartitionFieldName(),
					config.getPartitionPeriod());
		} else {
			createTable(config.getSchemaFileName());
		}
	}

	@Override
	public void createTableWithoutPartitions(String fileName) {
		String query = ClasspathUtils.loadStringFromClassPath(fileName);
		if (query.contains(PartitionUtil.PARTITION)) {
			query = query.replace(PartitionUtil.PARTITION, "");
		}
		template.update(query);
	}

	@Override
	public boolean doesPartitionExist(String tableName, String partitionName) {
		return template.queryForLong(CHECK_PARTITION, schema, tableName, partitionName) == 1;
	}

	@Override
	public void addPartition(String tableName, String partitionName, long value) {
		template.execute(String.format(ADD_PARTITION, tableName, partitionName, value));
	}

	@Override
	public Set<String> getExistingPartitionsForTable(String tableName) {
		return new HashSet<String>(template.query(ALL_PARTITIONS,
				new RowMapper<String>() {

					@Override
					public String mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						return rs.getString(1);
					}
				}, schema, tableName));
	}

	@Override
	public void dropPartition(String tableName, String partitionName) {
		template.execute(String.format(DROP_PARTITION, tableName, partitionName));
	}

	@Override
	public boolean doesPartitionExist(String tableName, long timeMS, Period partitionPeriod) {
		if (tableName == null || partitionPeriod == null) {
			throw new IllegalArgumentException();
		}
		DateTime date = PartitionUtil.floorDateByPeriod(new DateTime(timeMS), partitionPeriod);
		DateTime nextDate = null;
		switch (partitionPeriod) {
			case DAY:
				nextDate = date.plusDays(1);
				break;
			case MONTH:
				nextDate = date.plusMonths(1);
		}
		String sameDatePartition = PartitionUtil.getPartitionName(tableName, date, partitionPeriod);
		String nextDatePartition = PartitionUtil.getPartitionName(tableName, nextDate, partitionPeriod);
		return doesPartitionExist(tableName, sameDatePartition) && doesPartitionExist(tableName, nextDatePartition);
	}
}
