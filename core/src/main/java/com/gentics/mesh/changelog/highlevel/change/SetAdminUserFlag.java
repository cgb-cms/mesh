package com.gentics.mesh.changelog.highlevel.change;

import javax.inject.Inject;

import com.gentics.mesh.changelog.highlevel.AbstractHighLevelChange;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.MeshOptions;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Changelog entry which migrates from admin role to admin user flag.
 */
public class SetAdminUserFlag extends AbstractHighLevelChange {

	private static final Logger log = LoggerFactory.getLogger(SetAdminUserFlag.class);

	@Inject
	public SetAdminUserFlag(Lazy<BootstrapInitializer> boot) {
	}

	@Override
	public String getUuid() {
		return "21093BF213A244B484F3369A14AE7076";
	}

	@Override
	public String getName() {
		return "AdminUserFlagUpdater";
	}

	@Override
	public String getDescription() {
		return "This change assigns the admin user flag to all users which have access to the admin role";
	}

	@Override
	public void apply() {
		log.info("Applying change: " + getName());
		GroupDao groupDao = Tx.get().groupDao();
		for (HibRole role : Tx.get().roleDao().findAll()) {
			if (!"admin".equals(role.getName())) {
				continue;
			}
			for (HibGroup group : role.getGroups()) {
				for (HibUser user : groupDao.getUsers(group)) {
					log.info("Setting admin flag for user " + user.getUsername());
					user.setAdmin(true);
				}
			}
		}
	}

	@Override
	public boolean isAllowedInCluster(MeshOptions options) {
		// this change is allowed in cluster environments, since it only sets new properties (which will be ignored in old Mesh versions anyway)
		return true;
	}
}
