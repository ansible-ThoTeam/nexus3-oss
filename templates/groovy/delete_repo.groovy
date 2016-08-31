import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

repository.getRepositoryManager().delete(parsed_args.name)
