package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.rest.node.field.Field;

/**
 * A field can be transformed to a rest field
 */
public interface HibTransformableField<T extends Field> {

	/**
	 * Transform the field to a rest field given the provided transform parameters
	 * @param parameters
	 * @return
	 */
	T transformToRest(FieldTransformParameters parameters);
}
