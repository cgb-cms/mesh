package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.nesting.HibListableField;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import org.apache.commons.lang3.StringUtils;

public interface HibHtmlField extends HibListableField, HibTransformableField<HtmlField> {

	/**
	 * Set the HTML field value for the field.
	 * 
	 * @param html
	 */
	void setHtml(String html);

	/**
	 * Return the HTML field value for the field.
	 * 
	 * @return
	 */
	String getHTML();

	@Override
	default HibHtmlField cloneTo(HibFieldContainer container) {
		HibHtmlField clone = container.createHTML(getFieldKey());
		clone.setHtml(getHTML());
		return clone;
	}

	@Override
	default HtmlField transformToRest(FieldTransformParameters parameters) {
		HtmlFieldImpl htmlField = new HtmlFieldImpl();
		String html = getHTML();
		// TODO really empty string for unset field value?!
		htmlField.setHTML(html == null ? StringUtils.EMPTY : html);
		return htmlField;
	}
}
