package com.gentics.cailun.core.data.model.generic;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.data.model.I18NProperties;
import com.gentics.cailun.core.data.model.Language;

@NodeEntity
public class GenericContent extends GenericFile {

	private static final long serialVersionUID = 1100206059138098335L;

	public static final String CONTENT_KEYWORD = "content";
	public static final String FILENAME_KEYWORD = "filename";
	public static final String NAME_KEYWORD = "name";

	public static final String TEASER_KEYWORD = null;

	// @RelatedToVia(type = BasicRelationships.LINKED, direction = Direction.OUTGOING, elementClass = Linked.class)
	// private Collection<Linked> links = new HashSet<>();

	@Fetch
	protected Set<I18NProperties> filenames;

	@Indexed
	@Fetch
	protected Set<I18NProperties> contents;

	public GenericContent() {

	}

	public String getContent(Language language) {
		return getProperty(language, CONTENT_KEYWORD);
	}

}
