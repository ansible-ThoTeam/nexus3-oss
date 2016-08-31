import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

security.setAnonymousAccess(Boolean.valueOf(parsed_args.anonymous_access))
