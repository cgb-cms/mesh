package com.gentics.cailun.core.data.service.generic;

import java.util.List;

import org.springframework.data.neo4j.conversion.Result;

import com.gentics.cailun.core.data.model.generic.GenericNode;

public interface GenericNodeService<T extends GenericNode> {

	public T save(T node);

	public void save(List<T> nodes);

	public void delete(T node);

	public T findOne(Long id);

	public Result<T> findAll();

	public Result<T> findAll(String project);

	public T findByName(String project, String name);

	public T findByUUID(String project, String uuid);
	
	public T reload(T node);

}
