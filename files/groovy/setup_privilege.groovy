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
        'contentSelector': parsed_args.contentSelector,
        'repository': parsed_args.repository,
        'pattern': parsed_args.pattern,
        'domain': parsed_args.domain,
        'name': parsed_args.script_name,
        'actions': parsed_args.actions.join(',')
] as Map<String, String>)

if (update) {
    authManager.updatePrivilege(privilege)
    log.info("Privilege {} updated", parsed_args.name)
} else {
    authManager.addPrivilege(privilege)
    log.info("Privilege {} created", parsed_args.name)
}
