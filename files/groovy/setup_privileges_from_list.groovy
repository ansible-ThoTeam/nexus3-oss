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
            currentResult.put('debug', "test")
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

        // @todo: Fix update when only description is changed.
        //  For time being if you only change description, the privilege
        //  will not be updated
        if (update) {
            definedProperties = privilege.getProperties().sort().toString()
            currentProperties = authManager.getPrivilege(privilegeDef.name).getProperties().sort().toString()
            if (definedProperties != currentProperties) {
                authManager.updatePrivilege(privilege)
                currentResult.status = 'updated'
                scriptResults.changed = true
            }
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
