package org.sagebionetworks.warehouse.workers.utils;

import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.sagebionetworks.warehouse.workers.model.LogRecord;

public class LogRecordUtils {

	public static Boolean isValidLogRecord(LogRecord record) {
		if (record 					== null) return false;
		if (record.getClassName() 	== null) return false;
		if (record.getThrowable() 	== null) return false;
		return true;
	}

	public static String getFormattedLog(LogRecord toLog) {
		if (!isValidLogRecord(toLog)) {
			throw new IllegalArgumentException("Invalid LogRecord: "+ toLog);
		}
		return String.format("%d::%s::%s", toLog.getTimestamp(), toLog.getClassName(), toLog.getThrowable().getClass().getSimpleName());
	}

	public static String getKey(LogRecord toLog) {
		if (!isValidLogRecord(toLog)) {
			throw new IllegalArgumentException("Invalid LogRecord: "+ toLog);
		}
		DateTime date = new DateTime(toLog.getTimestamp(), DateTimeZone.UTC);
		String dateString = DateTimeUtils.toDateString(date);
		String key = String.format("%02d-%02d-%02d-%03d-%s", date.getHourOfDay(), date.getMinuteOfHour(), date.getSecondOfMinute(), date.getMillisOfSecond(), UUID.randomUUID());
		return String.format("%s/%s/%s.log.gz", dateString, toLog.getClassName(), key);
	}
}
