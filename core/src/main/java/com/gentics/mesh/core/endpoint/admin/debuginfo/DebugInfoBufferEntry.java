package com.gentics.mesh.core.endpoint.admin.debuginfo;

import java.util.zip.ZipEntry;

import com.gentics.mesh.json.JsonUtil;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * Entry which references the buffer and filename to be read and added to the zip file data stream for the final debug info zip data.
 */
public class DebugInfoBufferEntry implements DebugInfoEntry {

	private final String fileName;
	private final Buffer data;

	private DebugInfoBufferEntry(String fileName, Buffer data) {
		this.fileName = fileName;
		this.data = data;
	}

	public static DebugInfoEntry fromBuffer(String filename, Buffer data) {
		return new DebugInfoBufferEntry(filename, data);
	}

	public static DebugInfoEntry fromString(String filename, String data) {
		return new DebugInfoBufferEntry(
			filename,
			Buffer.buffer(data));
	}

	public static DebugInfoEntry asJson(String fileName, Object data) {
		return fromString(fileName, JsonUtil.toJson(data));
	}

	@Override
	public ZipEntry createZipEntry() {
		return new ZipEntry(fileName);
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	@Override
	public Flowable<Buffer> getData() {
		return Flowable.just(data);
	}
}
