import groovy.json.JsonSlurper
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.role.NoSuchRoleException

parsed_args = new JsonSlurper().parseText(args)

authManager = security.getSecuritySystem().getAuthorizationManager(UserManager.DEFAULT_SOURCE)

privileges = (parsed_args.privileges == null ? new HashSet() : parsed_args.privileges.toSet())
roles = (parsed_args.roles == null ? new HashSet() : parsed_args.roles.toSet())

try {
    existingRole = authManager.getRole(parsed_args.id)
    existingRole.setName(parsed_args.name)
    existingRole.setDescription(parsed_args.description)
    existingRole.setPrivileges(privileges)
    existingRole.setRoles(roles)
    authManager.updateRole(existingRole)
    log.info("Role {} updated", parsed_args.name)
} catch (NoSuchRoleException ignored) {
    security.addRole(parsed_args.id, parsed_args.name, parsed_args.description, privileges.toList(), roles.toList())
    log.info("Role {} created", parsed_args.name)
}
