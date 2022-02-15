package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;

public interface HibNumberField extends HibListableField, HibTransformableField<NumberField> {

	/**
	 * Set the number in the graph field.
	 * 
	 * @param number
	 */
	public void setNumber(Number number);

	/**
	 * Return the number that is stored in the graph field.
	 * 
	 * @return
	 */
	public Number getNumber();

	@Override
	default HibField cloneTo(HibFieldContainer container) {
		HibNumberField clone = container.createNumber(getFieldKey());
		clone.setNumber(getNumber());
		return clone;
	}

	@Override
	default NumberField transformToRest(FieldTransformParameters fieldTransformParameters) {
		NumberField restModel = new NumberFieldImpl();
		restModel.setNumber(getNumber());
		return restModel;
	}
}
