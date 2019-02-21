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
    // Only set attributes that can be changed
    newConfig.attributes['docker']['forceBasicAuth'] = parsed_args.force_basic_auth
    newConfig.attributes['docker']['v1Enabled'] = parsed_args.v1_enabled
    newConfig.attributes['proxy']['remoteUrl'] = parsed_args.proxy_url
    newConfig.attributes['dockerProxy']['indexType'] = parsed_args.index_type
    newConfig.attributes['dockerProxy']['useTrustStoreForIndexAccess'] = parsed_args.use_nexus_certificates_to_access_index
    newConfig.attributes['storage']['strictContentTypeValidation'] = Boolean.valueOf(parsed_args.strict_content_validation)
    newConfig.attributes['httpclient']['authentication'] = authentication

    if (parsed_args.http_port) {
        newConfig.attributes['docker']['httpPort'] = parsed_args.http_port
    } else {
        newConfig.attributes['docker']['httpPort'] = ""
    }

    repositoryManager.update(newConfig)

} else {

    configuration = new Configuration(
            repositoryName: parsed_args.name,
            recipeName: 'docker-proxy',
            online: true,
            attributes: [
                    docker: [
                            forceBasicAuth: parsed_args.force_basic_auth,
                            v1Enabled : parsed_args.v1_enabled
                    ],
                    proxy: [
                            remoteUrl: parsed_args.proxy_url,
                            contentMaxAge: parsed_args.get('maximum_component_age', 1440.0),
                            metadataMaxAge: parsed_args.get('maximum_metadata_age', 1440.0)
                    ],
                    dockerProxy: [
                            indexType: parsed_args.index_type,
                            useTrustStoreForIndexAccess: parsed_args.use_nexus_certificates_to_access_index
                    ],
                    httpclient: [
                            blocked: false,
                            autoBlock: true,
                            connection: [
                              useTrustStore: false
                            ],
                            authentication: authentication
                    ],
                    storage: [
                            writePolicy: parsed_args.write_policy.toUpperCase(),
                            blobStoreName: parsed_args.blob_store,
                            strictContentTypeValidation: Boolean.valueOf(parsed_args.strict_content_validation)
                    ],
                    negativeCache: [
                        enabled: parsed_args.get("negative_cache_enabled", true),
                        timeToLive: parsed_args.get("negative_cache_ttl", 1440.0)
                    ]
            ]
    )

    if (parsed_args.http_port) {
        configuration.attributes['docker']['httpPort'] = parsed_args.http_port
    }

    repositoryManager.create(configuration)
}
