import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException
import org.sonatype.nexus.security.internal.SecurityApiImpl
import groovy.json.JsonSlurper
import java.util.List

parsed_args = new JsonSlurper().parseText(args)
state = parsed_args.state == null ? 'present' : parsed_args.state
List<String> roleIds = new ArrayList<String>(parsed_args.roles)


if ( state == 'absent' ) {
	try {
		removeRoles(parsed_args, roleIds) 
	} catch (UserNotFoundException ignored) {
		log.info("User not found, cannot remove roles")
	}
} else {
	try {
		addRoles(parsed_args, roleIds)
	} catch (UserNotFoundException ignored) {
		log.info("Cannot update user")
	}
}

def addRoles(parsed_args,List<String> roleIds) {
	try {
		User user = security.securitySystem.getUser(parsed_args.username, "LDAP")
		authManager = security.getSecuritySystem().getAuthorizationManager(UserManager.DEFAULT_SOURCE)
		for (String role : roleIds ) {
			//def existingRole = authManager.getRole(role)
			log.info("RoleID : " + authManager.getRole(role).roleId.toString())
			if(user != null) {
				RoleIdentifier newRole = new RoleIdentifier("default", authManager.getRole(role).roleId);
				user.addRole(newRole)
				security.securitySystem.setUsersRoles(user.getUserId(), "LDAP", user.getRoles());
				log.info("Role {} added to user {}", authManager.getRole(role).roleId.toString(), user.getUserId() )
			} else {
				log.warn("No user with ID of $parsed_args.username found.")
			}
		}
	}
	catch(e) {
		log.warn(e.printStackTrace())
	}
}

def removeRoles(parsed_args, List<String> roleIds) {
	try {
		User user = security.securitySystem.getUser(parsed_args.username, "LDAP")
		authManager = security.getSecuritySystem().getAuthorizationManager(UserManager.DEFAULT_SOURCE)
		for (String role : roleIds ) {
			//def existingRole = authManager.getRole(role)
			log.info("RoleID : " + authManager.getRole(role).roleId.toString())
			if(user != null) {
				RoleIdentifier newRole = new RoleIdentifier("default", authManager.getRole(role).roleId);
				user.removeRole(newRole)
				security.securitySystem.setUsersRoles(user.getUserId(), "LDAP", user.getRoles());
				log.info("Role {} removed from user {}", authManager.getRole(role).roleId.toString(), user.getUserId() )
			} else {
				log.warn("No user with ID of $parsed_args.username found.")
			}
		}
	}
	catch(e) {
		log.warn(e.printStackTrace())
	}
}