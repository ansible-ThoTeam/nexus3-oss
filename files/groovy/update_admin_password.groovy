import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

security.securitySystem.changePassword('admin', parsed_args.new_password)
