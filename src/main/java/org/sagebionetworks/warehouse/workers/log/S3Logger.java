package org.sagebionetworks.warehouse.workers.log;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.warehouse.workers.model.LogRecord;

public interface S3Logger {

	/**
	 * Capture a log record in s3
	 * 
	 * @param progressCallback
	 * @param toLog
	 */
	public <T> void log(ProgressCallback<T> progressCallback, T toCallback, LogRecord toLog);
}
