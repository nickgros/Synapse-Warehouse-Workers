package org.sagebionetworks.warehouse.workers.snapshot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.aws.utils.s3.ObjectCSVReader;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.warehouse.workers.collate.StreamResourceProvider;
import org.sagebionetworks.warehouse.workers.db.AclSnapshotDao;
import org.sagebionetworks.warehouse.workers.model.AclSnapshot;
import org.sagebionetworks.warehouse.workers.model.SnapshotHeader;
import org.sagebionetworks.warehouse.workers.utils.ObjectSnapshotTestUtil;
import org.sagebionetworks.warehouse.workers.utils.ObjectSnapshotUtils;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.sqs.model.Message;

public class AclSnapshotWorkerTest {

	AmazonS3Client mockS3Client;
	AclSnapshotDao mockDao;
	AclSnapshotWorker worker;
	ProgressCallback<Message> mockCallback;
	Message message;
	String messageBody;
	StreamResourceProvider mockStreamResourceProvider;
	File mockFile;
	ObjectCSVReader<ObjectRecord> mockObjectCSVReader;
	List<ObjectRecord> batch;

	@SuppressWarnings("unchecked")
	@Before
	public void before() throws JSONObjectAdapterException {
		mockS3Client = Mockito.mock(AmazonS3Client.class);
		mockDao = Mockito.mock(AclSnapshotDao.class);
		mockStreamResourceProvider = Mockito.mock(StreamResourceProvider.class);
		worker = new AclSnapshotWorker(mockS3Client, mockDao, mockStreamResourceProvider);
		mockCallback = Mockito.mock(ProgressCallback.class);
		mockFile = Mockito.mock(File.class);
		mockObjectCSVReader = Mockito.mock(ObjectCSVReader.class);

		messageBody = "<Message>\n"
				+"  <bucket>bucket</bucket>\n"
				+"  <key>key</key>\n"
				+"</Message>";
		message = new Message();
		message.setBody(messageBody);

		Mockito.when(mockStreamResourceProvider.createTempFile(Mockito.eq(AclSnapshotWorker.TEMP_FILE_NAME_PREFIX), Mockito.eq(AclSnapshotWorker.TEMP_FILE_NAME_SUFFIX))).thenReturn(mockFile);
		Mockito.when(mockStreamResourceProvider.createObjectCSVReader(mockFile, ObjectRecord.class, SnapshotHeader.OBJECT_RECORD_HEADERS)).thenReturn(mockObjectCSVReader);

		batch = ObjectSnapshotTestUtil.createValidAclSnapshotBatch(5);
	}

	@Test
	public void runTest() throws RecoverableMessageException, IOException {
		worker.run(mockCallback, message);
		Mockito.verify(mockStreamResourceProvider).createTempFile(Mockito.eq(AclSnapshotWorker.TEMP_FILE_NAME_PREFIX), Mockito.eq(AclSnapshotWorker.TEMP_FILE_NAME_SUFFIX));
		Mockito.verify(mockS3Client).getObject((GetObjectRequest) Mockito.any(), Mockito.eq(mockFile));
		Mockito.verify(mockStreamResourceProvider).createObjectCSVReader(mockFile, ObjectRecord.class, SnapshotHeader.OBJECT_RECORD_HEADERS);
		Mockito.verify(mockFile).delete();
		Mockito.verify(mockObjectCSVReader).close();
	}

	@Test
	public void deleteFileTest() throws RecoverableMessageException, IOException {
		Mockito.when(mockS3Client.getObject((GetObjectRequest) Mockito.any(), Mockito.eq(mockFile))).thenThrow(new AmazonClientException(""));
		try {
			worker.run(mockCallback, message);
		} catch (AmazonClientException e) {
			// expected
		}
		Mockito.verify(mockStreamResourceProvider).createTempFile(Mockito.eq(AclSnapshotWorker.TEMP_FILE_NAME_PREFIX), Mockito.eq(AclSnapshotWorker.TEMP_FILE_NAME_SUFFIX));
		Mockito.verify(mockS3Client).getObject((GetObjectRequest) Mockito.any(), Mockito.eq(mockFile));
		Mockito.verify(mockStreamResourceProvider, Mockito.never()).createObjectCSVReader(mockFile, ObjectRecord.class, SnapshotHeader.OBJECT_RECORD_HEADERS);
		Mockito.verify(mockFile).delete();
		Mockito.verify(mockObjectCSVReader, Mockito.never()).close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void writeEmptyListTest() throws IOException {
		Mockito.when(mockObjectCSVReader.next()).thenReturn(null);
		AclSnapshotWorker.writeAclSnapshot(mockObjectCSVReader, mockDao, 2, mockCallback, message);
		Mockito.verify(mockDao, Mockito.never()).insert((List<AclSnapshot>) Mockito.any());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void writeInvalidRecordTest() throws IOException, JSONObjectAdapterException {
		ObjectRecord invalidRecord = ObjectSnapshotTestUtil.createValidAclObjectRecord();
		invalidRecord.setTimestamp(null);
		Mockito.when(mockObjectCSVReader.next()).thenReturn(invalidRecord, invalidRecord, null);
		AclSnapshotWorker.writeAclSnapshot(mockObjectCSVReader, mockDao, 2, mockCallback, message);
		Mockito.verify(mockDao, Mockito.never()).insert((List<AclSnapshot>) Mockito.any());
	}

	@Test
	public void writeLessThanBatchSizeTest() throws IOException {
		Mockito.when(mockObjectCSVReader.next()).thenReturn(batch.get(0), batch.get(1), batch.get(2), batch.get(3), null);
		AclSnapshotWorker.writeAclSnapshot(mockObjectCSVReader, mockDao, 5, mockCallback, message);
		List<AclSnapshot> expected = new ArrayList<AclSnapshot>(Arrays.asList(
				ObjectSnapshotUtils.getAclSnapshot(batch.get(0)),
				ObjectSnapshotUtils.getAclSnapshot(batch.get(1)),
				ObjectSnapshotUtils.getAclSnapshot(batch.get(2)),
				ObjectSnapshotUtils.getAclSnapshot(batch.get(3))));
		Mockito.verify(mockDao, Mockito.times(1)).insert(expected);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void writeBatchSizeTest() throws IOException {
		Mockito.when(mockObjectCSVReader.next()).thenReturn(batch.get(0), batch.get(1), batch.get(2), batch.get(3), batch.get(4), null);
		AclSnapshotWorker.writeAclSnapshot(mockObjectCSVReader, mockDao, 5, mockCallback, message);
		Mockito.verify(mockDao, Mockito.times(1)).insert((List<AclSnapshot>) Mockito.any());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void writeOverBatchSizeTest() throws IOException {
		Mockito.when(mockObjectCSVReader.next()).thenReturn(batch.get(0), batch.get(1), batch.get(2), batch.get(3), batch.get(4), null);
		AclSnapshotWorker.writeAclSnapshot(mockObjectCSVReader, mockDao, 3, mockCallback, message);
		Mockito.verify(mockDao, Mockito.times(2)).insert((List<AclSnapshot>) Mockito.any());
	}
}