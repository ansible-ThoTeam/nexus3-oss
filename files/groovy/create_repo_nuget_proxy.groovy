import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.config.Configuration

parsed_args = new JsonSlurper().parseText(args)

repositoryManager = repository.repositoryManager

authentication = parsed_args.remote_username == null ? null : [
        type: 'username',
        username: parsed_args.remote_username,
        password: parsed_args.remote_password
]

existingRepository = repositoryManager.get(parsed_args.name)

if (existingRepository != null) {

    newConfig = existingRepository.configuration.copy()
    // We only update values we are allowed to change (cf. greyed out options in gui)
    newConfig.attributes['proxy']['remoteUrl'] = parsed_args.remote_url
    newConfig.attributes['storage']['writePolicy'] = parsed_args.write_policy.toUpperCase()
    newConfig.attributes['httpclient']['authentication'] = authentication
    newConfig.attributes['storage']['strictContentTypeValidation'] = Boolean.valueOf(parsed_args.strict_content_validation)

    repositoryManager.update(newConfig)

} else {

    configuration = new Configuration(
            repositoryName: parsed_args.name,
            recipeName: 'nuget-proxy',
            online: true,
            attributes: [
                    proxy  : [
                            remoteUrl: parsed_args.remote_url,
                            contentMaxAge: 1440.0,
                            metadataMaxAge: 1440.0
                    ],
                    httpclient: [
                            blocked: false,
                            autoBlock: true,
                            authentication: authentication,
                            connection: [
                                    useTrustStore: false
                            ]
                    ],
                    storage: [
                            blobStoreName: parsed_args.blob_store,
                            strictContentTypeValidation: Boolean.valueOf(parsed_args.strict_content_validation)
                    ],
                    negativeCache: [
                            enabled: parsed_args.get("negativeCacheEnabled", true),
                            timeToLive: parsed_args.get("negativeCacheTTL", 1440.0)
                    ]
            ]
    )

    repositoryManager.create(configuration)

}
