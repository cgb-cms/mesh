package com.gentics.mesh.search;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;

public class TagFamilySearchVerticleTest extends AbstractSearchVerticleTest {

	@Autowired
	private TagFamilyVerticle tagFamilyVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(tagFamilyVerticle);
		return list;
	}

}
