package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Vertex;

@Component
public class RoleService extends AbstractMeshGraphService<Role> {

	@Autowired
	private MeshUserService userService;

	public static RoleService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static RoleService getRoleService() {
		return instance;
	}

	public Role findByName(String name) {
		return findByName(name, RoleImpl	.class);
	}

	public List<? extends Role> findAll() {

		// public Page<Role> findAll(String userUuid, Pageable pageable) {
		// // @Query(value = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + "return role ORDER BY role.name",
		//
		// // countQuery = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + " return count(role)")
		// return null;
		// }
		// TODO filter for permissions?
		return fg.v().has(RoleImpl.class).toListExplicit(RoleImpl.class);
	}

	// public void addCRUDPermissionOnRole(MeshAuthUser requestUser, GroupRoot groupRoot, Permission createPerm, Group group) {

	public void addCRUDPermissionOnRole(MeshAuthUser requestUser, MeshVertex node, Permission permission, GenericNode targetNode) {

		// 1. Determine all roles that grant given permission
		// Node userNode = neo4jTemplate.getPersistentState(user);
		Vertex userNode = requestUser.getVertex();
		Set<Role> roles = new HashSet<>();

		// TODO use core blueprint api or gremlin traversal?
		// for (Edge rel : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.TYPES.MEMBER_OF, Direction.OUT)
		// .relationships(AuthRelationships.TYPES.HAS_ROLE, Direction.IN)
		// .relationships(AuthRelationships.TYPES.HAS_PERMISSION, Direction.OUT).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
		// .traverse(userNode).relationships()) {
		//
		// if (AuthRelationships.HAS_PERMISSION.equalsIgnoreCase(rel.getLabel())) {
		// // Check whether this relation in fact targets our object we want to check
		// boolean matchesTargetNode = rel.getVertex(com.tinkerpop.blueprints.Direction.OUT).getId() == meshPermission.getTargetNode().getId();
		// if (matchesTargetNode) {
		// // Convert the api relationship to a framed edge
		// GraphPermission perm = framedGraph.frame(rel, GraphPermission.class);
		// if (meshPermission.implies(perm) == true) {
		// // This permission is permitting. Add it to the list of roles
		// roles.add(perm.getRole());
		// }
		// }
		// }
		// }

		// 2. Add CRUD permission to identified roles and target node
		for (Role role : roles) {
			role.addPermissions(targetNode, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM);
		}
	}

	public Page<? extends Role> findAll(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// TODO filter for permissions
		VertexTraversal traversal = fg.v().has(RoleImpl.class);
		VertexTraversal countTraversal = fg.v().has(RoleImpl.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, RoleImpl.class);
	}

	@Override
	public Role findByUUID(String uuid) {
		return findByUUID(uuid, RoleImpl.class);
	}

}
