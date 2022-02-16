package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.util.VersionNumber;

/**
 * Holds rest fields of a HibNodeFieldContainer. Used for read only purposes
 */
public interface ReadOnlyNodeFieldContainer {

	/**
	 * the id of the field container
	 * @return
	 */
	Object getId();

	/**
	 * The language tag of the field container
	 * @return
	 */
	String getLanguageTag();

	/**
	 * The version number of the field container
	 * @return
	 */
	VersionNumber getVersion();

	/**
	 * The schemaVersion of the field container
	 * @return
	 */
	HibSchemaVersion getSchemaContainerVersion();

	/**
	 * The editor of the field container
	 * @return
	 */
	HibUser getEditor();

	/**
	 * The last edited timestamp of the field container
	 * @return
	 */
	Long getLastEditedTimestamp();

	/**
	 * The last edited date of the field container
	 * @return
	 */
	String getLastEditedDate();

	/**
	 * Get a rest field using the provided parameters
	 * @param ac
	 * @param name
	 * @param fieldEntry
	 * @param containerLanguageTags
	 * @param level
	 * @return
	 */
	Field getRestField(InternalActionContext ac, String name, FieldSchema fieldEntry, List<String> containerLanguageTags, int level);
}
