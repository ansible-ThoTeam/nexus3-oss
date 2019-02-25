import groovy.json.JsonOutput
import org.sonatype.nexus.security.role.RoleIdentifier;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException
import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)

parsed_args.each { currentUserDef ->

	state = currentUserDef.get('state', 'present')
	Map<String, String> currentResult = [username: currentUserDef.username, state: state, roles: currentUserDef.roles.join(', ')]

	try {
		User user = security.securitySystem.getUser(userDefinition.username, "LDAP")
		authManager = security.getSecuritySystem().getAuthorizationManager(UserManager.DEFAULT_SOURCE)
		currentUserDef.roles.each { role ->
			log.info("RoleID : " + authManager.getRole(role).roleId.toString())
			RoleIdentifier newRole = new RoleIdentifier("default", authManager.getRole(role).roleId);
			if (state == 'absent') {
				user.removeRole(newRole)
				msg = "Role {} removed from user {}"
				currentResult.put('status', 'removed')
				scriptResults['changed'] = true
			} else {
				user.addRole(newRole)
				msg = "Role {} added to user {}"
				currentResult.put('status', 'created')
				scriptResults['changed'] = true
			}
			security.securitySystem.setUsersRoles(user.getUserId(), "LDAP", user.getRoles());
			log.info(msg, authManager.getRole(role).roleId.toString(), user.getUserId() )
		}
	} catch(UserNotFoundException e) {
		if (state == 'absent') {
			currentResult.put('status', 'absent')
		} else {
			currentResult.put('status', 'error')
			currentResult.put('error_msg', "User could not be found")
			scriptResults['error'] = true
		}
	} catch (Exception e) {
		currentResult.put('status', 'error')
		currentResult.put('error_msg', e.toString())
	}
	scriptResults['action_details'].add(currentResult)
}

return JsonOutput.toJson(scriptResults)
