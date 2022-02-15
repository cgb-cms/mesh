package com.gentics.mesh.core.data.node.field;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.parameter.LinkType;

public interface RestTransformers {

	FieldTransformer<StringField> STRING_TRANSFORMER = (field, transformParameters, parentNode) -> {
		if (field == null) {
			return null;
		}
		InternalActionContext ac = transformParameters.ac();
		List<String> languageTags = transformParameters.languageTags();
		CommonTx tx = CommonTx.get();
		WebRootLinkReplacer webRootLinkReplacer = tx.data().mesh().webRootLinkReplacer();
		// TODO validate found fields has same type as schema
		// StringGraphField graphStringField = new com.gentics.webRootLinkReplacer.core.data.node.field.impl.basic.StringGraphFieldImpl(
		// fieldKey, this);
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
	};

	FieldTransformer<HtmlField> HTML_TRANSFORMER = (field, transformParameters, parentNode) -> {
		if (field == null) {
			return null;
		}
		InternalActionContext ac = transformParameters.ac();
		List<String> languageTags = transformParameters.languageTags();
		CommonTx tx = CommonTx.get();
		WebRootLinkReplacer webRootLinkReplacer = tx.data().mesh().webRootLinkReplacer();

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
	};
}
