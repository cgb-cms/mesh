package com.gentics.mesh.graphql.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MappedFilter;
import com.gentics.graphqlfilter.filter.StartMainFilter;
import com.gentics.graphqlfilter.filter.StringFilter;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.model.NodeReferenceIn;

/**
 * Filters incoming node references.
 */
public class NodeReferenceFilter extends StartMainFilter<NodeReferenceIn> {
	private final GraphQLContext context;
	private static final String NAME = "NodeReferenceFilter";

	public static NodeReferenceFilter nodeReferenceFilter(GraphQLContext context) {
		return context.getOrStore(NAME, () -> new NodeReferenceFilter(context));
	}

	private NodeReferenceFilter(GraphQLContext context) {
		super("NodeReferenceFilter", "Filters by incoming node references.", Optional.empty()); // TODO empty?
		this.context = context;
	}

	@Override
	protected List<FilterField<NodeReferenceIn, ?>> getFilters() {
		String owner = ElementType.NODE.name();
		return Arrays.asList(
			new MappedFilter<>(owner, "fieldName", "Filters by the field name that is used to reference this node.", StringFilter.filter(), NodeReferenceIn::getFieldName),
			new MappedFilter<>(owner, "micronodeFieldName", "Filters by the micronode field name that is used to reference this node.", StringFilter.filter(), NodeReferenceIn::getMicronodeFieldName),
			new MappedFilter<>(owner, "node", "Filters by the node that references this node", NodeFilter.filter(context), NodeReferenceIn::getNode)
		);
	}
}
