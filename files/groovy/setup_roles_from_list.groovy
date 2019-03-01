import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.sonatype.nexus.security.authz.AuthorizationManager
import org.sonatype.nexus.security.role.Role
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.role.NoSuchRoleException

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)

parsed_args = new JsonSlurper().parseText(args)

AuthorizationManager authManager = security.securitySystem.getAuthorizationManager(UserManager.DEFAULT_SOURCE)

parsed_args.each { roleDef ->

    Map<String, String> currentResult = [id: roleDef.id, name: roleDef.name, status: 'no change']

    privileges = (roleDef.privileges == null ? new HashSet() : roleDef.privileges.toSet())
    roles = (roleDef.roles == null ? new HashSet() : roleDef.roles.toSet())

    try {
        Role newRole = authManager.getRole(roleDef.id)
        newRole.setName(roleDef.name)
        newRole.setDescription(roleDef.description)
        newRole.setPrivileges(privileges)
        newRole.setRoles(roles)
        Role currentRole = authManager.getRole(roleDef.id)
        if (
                // Role comparison does not include roles and priviliges
                newRole != currentRole
                || newRole.getRoles() != currentRole.getRoles()
                || newRole.getPrivileges() != currentRole.getPrivileges()
        ) {
            try {
                authManager.updateRole(newRole)
                log.info("Role {} updated", roleDef.name)
                currentResult.status = 'updated'
                scriptResults.changed = true
            } catch (Exception e) {
                log.error("Role {} could not be updated", )
                currentResult.status = 'error'
                currentResult.put('error_msg', e.toString())
                scriptResults.error = true
            }
        }
    } catch (NoSuchRoleException ignored) {
        try {
            security.addRole(roleDef.id, roleDef.name, roleDef.description, privileges.toList(), roles.toList())
            log.info("Role {} created", roleDef.name)
            currentResult.status = 'created'
            scriptResults.changed = true
        } catch (Exception e) {
            log.error("Role {} could not be created", roleDef.name)
            currentResult.status = 'error'
            currentResult.put('error_msg', e.toString())
            scriptResults.error = true
        }
    } catch (Exception e) {
        log.error(e.stackTrace.toString())
        currentResult.status = 'error'
        currentResult.put('error_msg', e.toString())
        scriptResults.error = true
    }

    scriptResults['action_details'].add(currentResult)
}

return JsonOutput.toJson(scriptResults)