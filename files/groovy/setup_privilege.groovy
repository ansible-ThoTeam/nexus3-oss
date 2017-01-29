import groovy.json.JsonSlurper
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.privilege.Privilege

parsed_args = new JsonSlurper().parseText(args)

authManager = security.getSecuritySystem().getAuthorizationManager(UserManager.DEFAULT_SOURCE)

def privilege
boolean update = true

try {
    privilege = authManager.getPrivilege(parsed_args.name)
} catch (NoSuchPrivilegeException ignored) {
    // could not find any existing  privilege
    update = false
    privilege = new Privilege(
            'id': parsed_args.name,
            'name': parsed_args.name
    )
}

privilege.setDescription(parsed_args.description)
privilege.setType(parsed_args.type)
privilege.setProperties([
        'format': parsed_args.format,
        'repository': parsed_args.repository,
        'actions': parsed_args.actions.join(',')
] as Map<String, String>)

if (update) {
    authManager.updatePrivilege(privilege)
} else {
    authManager.addPrivilege(privilege)
}
