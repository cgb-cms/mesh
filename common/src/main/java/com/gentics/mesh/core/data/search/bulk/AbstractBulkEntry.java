package com.gentics.mesh.core.data.search.bulk;

import com.gentics.mesh.etc.config.search.ComplianceMode;

public abstract class AbstractBulkEntry implements BulkEntry {

	private final String indexName;

	private final String documentId;

	private ComplianceMode mode;

	public AbstractBulkEntry(String indexName, String documentId, ComplianceMode mode) {
		this.indexName = indexName;
		this.documentId = documentId;
		this.mode = mode;
	}

	public String getIndexName() {
		return indexName;
	}

	public String getDocumentId() {
		return documentId;
	}

	public ComplianceMode getMode() {
		return mode;
	}

}
