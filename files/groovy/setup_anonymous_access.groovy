import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

security.setAnonymousAccess(Boolean.valueOf(parsed_args.anonymous_access))


//User user = security.securitySystem.getUser('anonymous')
//
//Set<RoleIdentifier> existingRoles = user.getRoles()
//Set<RoleIdentifier> definedRoles = []
//userDef.roles.each { roleDef ->
//    RoleIdentifier role = new RoleIdentifier("default", authManager.getRole('anonymous').roleId);
//    definedRoles.add(role)
//}
//
//if (!existingRoles.equals(definedRoles)) {
//    security.securitySystem.setUsersRoles(user.getUserId(), "default", definedRoles)
//    currentResult.put('status', 'updated')
//    scriptResults['changed'] = true
//}
