package com.gentics.mesh.util;

import static com.gentics.mesh.util.SortOrder.DESCENDING;
import static com.gentics.mesh.util.SortOrder.UNSORTED;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.generic.MeshEdge;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.paging.PagingInfo;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;

public final class TraversalHelper {

	public static <T> Page<? extends T> getPagedResult(VertexTraversal<?, ?, ?> traversal, VertexTraversal<?, ?, ?> countTraversal, String sortBy,
			SortOrder order, int page, int pageSize, Class<T> classOfT) throws InvalidArgumentException {

		if (page < 1) {
			throw new InvalidArgumentException("The page must always be positive");
		}
		if (pageSize < 1) {
			throw new InvalidArgumentException("The pageSize must always be positive");
		}

		// Internally we start with page 0
		page = page - 1;

		int low = page * pageSize;
		int upper = low + pageSize - 1;

		int count = (int) countTraversal.count();

		// Only add the filter to the pipeline when the needed parameters were correctly specified.
		if (order != UNSORTED && sortBy != null) {
			traversal = traversal.order((VertexFrame f1, VertexFrame f2) -> {
				if (order == DESCENDING) {
					VertexFrame tmp = f1;
					f1 = f2;
					f2 = tmp;
				}
				return f2.getProperty(sortBy).equals(f1.getProperty(sortBy)) ? 1 : 0;
			});
		}

		List<? extends T> list = traversal.range(low, upper).toListExplicit(classOfT);

		int totalPages = count / pageSize;

		// Internally the page size was reduced. We need to increment it now that we are finished.
		return new Page<T>(list, count, ++page, totalPages, list.size());

	}

	public static <T> Page<? extends T> getPagedResult(VertexTraversal<?, ?, ?> traversal, VertexTraversal<?, ?, ?> countTraversal,
			PagingInfo pagingInfo, Class<T> classOfT) throws InvalidArgumentException {
		return getPagedResult(traversal, countTraversal, pagingInfo.getSortBy(), pagingInfo.getOrder(), pagingInfo.getPage(),
				pagingInfo.getPerPage(), classOfT);
	}

	public static void debug(VertexTraversal<?, ?, ?> traversal) {
		for (MeshVertexImpl v : traversal.toListExplicit(MeshVertexImpl.class)) {
			System.out.println(v.getProperty("name") + " type: " + v.getFermaType() + " json: " + v.toJson());

		}
	}

	public static void debug(EdgeTraversal<?, ?, ?> traversal) {
		for (MeshEdge e : traversal.toListExplicit(MeshEdge.class)) {
			System.out.println(e.getLabel() + " type: " + e.getFermaType() + " json: " + e.toJson());
		}

	}

}
