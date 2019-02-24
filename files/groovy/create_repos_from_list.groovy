import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.config.Configuration

parsed_args = new JsonSlurper().parseText(args)

repositoryManager = repository.repositoryManager

parsed_args.each { currentRepo ->

    recipeName = currentRepo.format + '-' + currentRepo.type

    existingRepository = repositoryManager.get(currentRepo.name)


    if (existingRepository == null) {
        log.info('Creating configuration for new repo `' + currentRepo.name + ' (Format: ' + currentRepo.format + ', Type: ' + currentRepo.type + ')')
        // Default and/or immutable values
        configuration = new Configuration(
                repositoryName: currentRepo.name,
                recipeName: recipeName,
                online: true,
                attributes: [
                    storage: [
                        blobStoreName: currentRepo.blob_store
                    ]
                ]
        )
    } else {
        log.info('Loading configuration for existing repo `' + currentRepo.name + ' (Format: ' + currentRepo.format + ', Type: ' + currentRepo.type)
        // load existing repository configuration
        configuration = existingRepository.configuration.copy()
    }

    // Configs common to all repos
    configuration.attributes['storage']['strictContentTypeValidation'] = Boolean.valueOf(currentRepo.strict_content_validation)

    // Configs for all group repos
    if (currentRepo.type == 'group') {
        configuration.attributes['group'] = [
                memberNames: currentRepo.member_repos
        ]
    }

    // Configs for all hosted repos
    if (currentRepo.type == 'hosted') {
        configuration.attributes['storage']['writePolicy'] = currentRepo.write_policy.toUpperCase()
    }

    // Configs for yum hosted repos
    if (currentRepo.type == 'hosted' && currentRepo.format == 'yum') {
        configuration.attributes['yum'] = [
                repodataDepth: currentRepo.repodata_depth.toInteger(),
                layoutPolicy : currentRepo.layout_policy.toUpperCase()
        ]
    }

    // Configs for all proxy repos
    if (currentRepo.type == 'proxy') {
        authentication = currentRepo.remote_username == null ? null : [
                type    : 'username',
                username: currentRepo.remote_username,
                password: currentRepo.remote_password
        ]

        configuration.attributes['httpclient'] = [
                authentication: authentication,
                blocked       : false,
                autoBlock     : true,
                connection    : [
                        useTrustStore: false
                ]
        ]

        configuration.attributes['proxy'] = [
                remoteUrl     : currentRepo.remote_url,
                contentMaxAge : currentRepo.get('maximum_component_age', 1440.0),
                metadataMaxAge: currentRepo.get('maximum_metadata_age', 1440.0)
        ]
    }

    // Configs for docker proxy repos
    if (currentRepo.type == 'proxy' && currentRepo.format == 'docker') {
        configuration.attributes['dockerProxy'] = [
                indexType                  : currentRepo.index_type,
                useTrustStoreForIndexAccess: currentRepo.use_nexus_certificates_to_access_index
        ]
    }

    // Configs for maven hosted/proxy repos
    if (currentRepo.type in ['hosted', 'proxy'] && currentRepo.format == 'maven2') {
        configuration.attributes['maven'] = [
                versionPolicy: currentRepo.version_policy.toUpperCase(),
                layoutPolicy : currentRepo.layout_policy.toUpperCase()
        ]
    }

    // Configs for all docker repos
    if (currentRepo.format == 'docker') {
        configuration.attributes['docker'] = [
                forceBasicAuth: currentRepo.force_basic_auth,
                v1Enabled     : currentRepo.v1_enabled,
                httpPort      : currentRepo.get('http_port', '')
        ]
    }

    if (existingRepository == null) {
        repositoryManager.create(configuration)
    } else {
        repositoryManager.update(configuration)
    }
    log.info('Configuration for repo `' + currentRepo.name + ' saved')
}
