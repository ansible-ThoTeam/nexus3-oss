import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.config.Configuration

parsed_args = new JsonSlurper().parseText(args)

repositoryManager = repository.repositoryManager

existingRepository = repositoryManager.get(parsed_args.name)

if (existingRepository != null) {

    newConfig = existingRepository.configuration.copy()
    // We only update values we are allowed to change (cf. greyed out options in gui)
    newConfig.attributes['docker']['forceBasicAuth'] = parsed_args.force_basic_auth
    newConfig.attributes['docker']['v1Enabled'] = parsed_args.v1_enabled
    newConfig.attributes['storage']['strictContentTypeValidation'] = Boolean.valueOf(parsed_args.strict_content_validation)
    newConfig.attributes['group']['memberNames'] = parsed_args.member_repos
    if (parsed_args.http_port) {
        newConfig.attributes['docker']['httpPort'] = parsed_args.http_port
    } else {
        newConfig.attributes['docker']['httpPort'] = ""
    }

    repositoryManager.update(newConfig)

} else {

    configuration = new Configuration(
            repositoryName: parsed_args.name,
            recipeName: 'docker-group',
            online: true,
            attributes: [
                    docker: [
                            forceBasicAuth: parsed_args.force_basic_auth,
                            v1Enabled : parsed_args.v1_enabled
                    ],
                    group: [
                            memberNames: parsed_args.member_repos
                    ],
                    storage: [
                            blobStoreName: parsed_args.blob_store,
                            strictContentTypeValidation: Boolean.valueOf(parsed_args.strict_content_validation)
                    ]
            ]
    )

    if (parsed_args.http_port) {
        configuration.attributes['docker']['httpPort'] = parsed_args.http_port
    }

    repositoryManager.create(configuration)
}
