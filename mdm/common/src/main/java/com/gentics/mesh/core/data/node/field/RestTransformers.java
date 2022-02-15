package com.gentics.mesh.core.data.node.field;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.list.HibBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.HibDateFieldList;
import com.gentics.mesh.core.data.node.field.list.HibHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.S3BinaryField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.parameter.LinkType;

public interface RestTransformers {

	FieldTransformer<StringField, HibStringField> STRING_TRANSFORMER = (supplier, transformParameters, fieldKey, parentNode) -> {
		InternalActionContext ac = transformParameters.ac();
		List<String> languageTags = transformParameters.languageTags();
		CommonTx tx = CommonTx.get();
		WebRootLinkReplacer webRootLinkReplacer = tx.data().mesh().webRootLinkReplacer();
		// TODO validate found fields has same type as schema
		// StringGraphField graphStringField = new com.gentics.webRootLinkReplacer.core.data.node.field.impl.basic.StringGraphFieldImpl(
		// fieldKey, this);
		HibStringField graphStringField = supplier.get();
		if (graphStringField == null) {
			return null;
		} else {
			StringField field = graphStringField.transformToRest(transformParameters);
			if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
				HibProject project = tx.getProject(ac);
				if (project == null) {
					project = parentNode.get().getProject();
				}
				field.setString(webRootLinkReplacer.replace(ac, tx.getBranch(ac).getUuid(),
						ContainerType.forVersion(ac.getVersioningParameters().getVersion()), field.getString(),
						ac.getNodeParameters().getResolveLinks(), project.getName(), languageTags));
			}
			return field;

		}
	};

	FieldTransformer<HtmlField, HibHtmlField> HTML_TRANSFORMER = (supplier, transformParameters, fieldKey, parentNode) -> {
		InternalActionContext ac = transformParameters.ac();
		List<String> languageTags = transformParameters.languageTags();
		CommonTx tx = CommonTx.get();
		WebRootLinkReplacer webRootLinkReplacer = tx.data().mesh().webRootLinkReplacer();
		HibHtmlField graphHtmlField = supplier.get();
		if (graphHtmlField == null) {
			return null;
		} else {
			HtmlField field = graphHtmlField.transformToRest(transformParameters);
			// If needed resolve links within the html
			if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
				HibProject project = tx.getProject(ac);
				if (project == null) {
					project = parentNode.get().getProject();
				}
				field.setHTML(webRootLinkReplacer.replace(ac, tx.getBranch(ac).getUuid(),
						ContainerType.forVersion(ac.getVersioningParameters().getVersion()), field.getHTML(),
						ac.getNodeParameters().getResolveLinks(), project.getName(), languageTags));
			}
			return field;
		}
	};

	FieldTransformer<NodeField, HibNodeField> NODE_TRANSFORMER = (supplier, transformParameters, fieldKey, parentNode) -> {
		HibNodeField graphNodeField = supplier.get();
		if (graphNodeField == null) {
			return null;
		} else {
			// TODO check permissions
			return graphNodeField.transformToRest(transformParameters);
		}
	};
}
