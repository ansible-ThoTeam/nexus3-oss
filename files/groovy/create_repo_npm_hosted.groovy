import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.config.Configuration

parsed_args = new JsonSlurper().parseText(args)

repositoryManager = repository.repositoryManager

existingRepository = repositoryManager.get(parsed_args.name)

if (existingRepository != null) {

    newConfig = existingRepository.configuration.copy()
    // We only update values we are allowed to change (cf. greyed out options in gui)
    newConfig.attributes['storage']['writePolicy'] = parsed_args.write_policy.toUpperCase()
    newConfig.attributes['storage']['strictContentTypeValidation'] = Boolean.valueOf(parsed_args.strict_content_validation)

    repositoryManager.update(newConfig)

} else {

    configuration = new Configuration(
            repositoryName: parsed_args.name,
            recipeName: 'npm-hosted',
            online: true,
            attributes: [
                    storage: [
                            writePolicy: parsed_args.write_policy.toUpperCase(),
                            blobStoreName: parsed_args.blob_store,
                            strictContentTypeValidation: Boolean.valueOf(parsed_args.strict_content_validation)
                    ]
            ]
    )

    repositoryManager.create(configuration)

}
