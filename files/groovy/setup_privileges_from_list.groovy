import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.privilege.Privilege

parsed_args = new JsonSlurper().parseText(args)

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)

authManager = security.getSecuritySystem().getAuthorizationManager(UserManager.DEFAULT_SOURCE)

parsed_args.each { privilegeDef ->

    Map<String, String> currentResult = [name: privilegeDef.name, status: 'no change']
    try {

        def privilege
        boolean update = true

        try {
            privilege = authManager.getPrivilege(privilegeDef.name)
        } catch (NoSuchPrivilegeException ignored) {
            // could not find any existing  privilege
            update = false
            privilege = new Privilege(
                    'id': privilegeDef.name,
                    'name': privilegeDef.name
            )
        }

        privilege.setDescription(privilegeDef.description)
        privilege.setType(privilegeDef.type)
        privilege.setProperties([
                'format'         : privilegeDef.format,
                'contentSelector': privilegeDef.contentSelector,
                'repository'     : privilegeDef.repository,
                'pattern'        : privilegeDef.pattern,
                'domain'         : privilegeDef.domain,
                'name'           : privilegeDef.script_name,
                'actions'        : privilegeDef.actions.join(',')
        ] as Map<String, String>)

        if (update && (privilege.getProperties().sort().toString() != authManager.getPrivilege(privilegeDef.name).getProperties().sort().toString())) {
            authManager.updatePrivilege(privilege)
            log.info("Privilege {} updated", privilegeDef.name)
            currentResult.status = 'updated'
            scriptResults.changed = true
        } else {
            authManager.addPrivilege(privilege)
            log.info("Privilege {} created", privilegeDef.name)
            currentResult.status = 'created'
            scriptResults.changed = true
        }
    } catch (Exception e) {
        currentResult.status = 'error'
        currentResult.put('error_msg', e.toString())
        scriptResults.error = true
    }

    scriptResults.action_details.add(currentResult)
}

return JsonOutput.toJson(scriptResults)
