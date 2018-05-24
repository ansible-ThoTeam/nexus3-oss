import groovy.json.JsonSlurper
import org.sonatype.nexus.security.user.UserManager
import org.sonatype.nexus.security.user.UserNotFoundException
import org.sonatype.nexus.security.user.User

parsed_args = new JsonSlurper().parseText(args)
state = parsed_args.state == null ? 'present' : parsed_args.state

if ( state == 'absent' ) {
    deleteUser(parsed_args)
} else {
    try {
        updateUser(parsed_args)
    } catch (UserNotFoundException ignored) {
        addUser(parsed_args)
    }
}

def updateUser(parsed_args) {
    User user = security.securitySystem.getUser(parsed_args.username)
    user.setFirstName(parsed_args.first_name)
    user.setLastName(parsed_args.last_name)
    user.setEmailAddress(parsed_args.email)
    security.securitySystem.updateUser(user)
    security.setUserRoles(parsed_args.username, parsed_args.roles)
    security.securitySystem.changePassword(parsed_args.username, parsed_args.password)
}

def addUser(parsed_args) {
    security.addUser(parsed_args.username, parsed_args.first_name, parsed_args.last_name, parsed_args.email, true, parsed_args.password, parsed_args.roles)
}

def deleteUser(parsed_args) {
    try {
        security.securitySystem.deleteUser(parsed_args.username, UserManager.DEFAULT_SOURCE)
    } catch (UserNotFoundException ignored) {
        // No user, so nothing to do
    }
}
