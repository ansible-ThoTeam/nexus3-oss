import groovy.json.JsonSlurper
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.role.NoSuchRoleException

parsed_args = new JsonSlurper().parseText(args)

authManager = security.getSecuritySystem().getAuthorizationManager(UserManager.DEFAULT_SOURCE)

def existingRole = null

try {
    existingRole = authManager.getRole(parsed_args.id)
} catch (NoSuchRoleException ignored) {
    // could not find role
}

privileges = (parsed_args.privileges == null ? new HashSet() : parsed_args.privileges.toSet())
roles = (parsed_args.roles == null ? new HashSet() : parsed_args.roles.toSet())

if (existingRole != null) {
    existingRole.setName(parsed_args.name)
    existingRole.setDescription(parsed_args.description)
    existingRole.setPrivileges(privileges)
    existingRole.setRoles(roles)
    authManager.updateRole(existingRole)
} else {
    security.addRole(parsed_args.id, parsed_args.name, parsed_args.description, privileges.toList(), roles.toList())
}
