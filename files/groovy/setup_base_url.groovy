import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

core.baseUrl(parsed_args.base_url)
