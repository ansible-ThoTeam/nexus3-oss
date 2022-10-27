import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.config.Configuration

parsed_args = new JsonSlurper().parseText(args)

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)

repositoryManager = repository.repositoryManager

private Configuration newConfiguration(Map map) {
    Configuration config
    try {
        config = repositoryManager.newConfiguration()
    } catch (MissingMethodException) {
        // Compatibility with nexus versions older than 3.21
        config = Configuration.newInstance()
    }
    config.with {
        repositoryName = map.repositoryName
        recipeName = map.recipeName
        online = map.online
        attributes = map.attributes as Map
    }
    return config
}

private boolean configurationChanged(Configuration oldConfig, Configuration newConfig) {
    if (oldConfig.attributes.httpclient) {
        if (oldConfig.attributes.httpclient.authentication == [:]) {
            oldConfig.attributes.httpclient.authentication = null
        }
    }
    return oldConfig.properties != newConfig.properties
}


parsed_args.each { currentRepo ->

    Map<String, String> currentResult = [name: currentRepo.name, format: currentRepo.format, type: currentRepo.type]

    recipeName = currentRepo.format + '-' + currentRepo.type

    existingRepository = repositoryManager.get(currentRepo.name)

    try {
        if (existingRepository == null) {
            log.info('Creating configuration for new repo {} (Format: {},  Type: {})', currentRepo.name, currentRepo.format, currentRepo.type)
            // Default and/or immutable values
            configuration = newConfiguration(
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
            log.info('Loading configuration for existing repo {} (Format: {},  Type: {})', currentRepo.name, currentRepo.format, currentRepo.type)
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

        // Configs for apt repos
        if (currentRepo.type == 'proxy' && currentRepo.format == 'apt') {
            configuration.attributes['apt'] = [
                    distribution: currentRepo.distribution,
                    flat        : Boolean.valueOf(currentRepo.flat)
            ]
        }
        if (currentRepo.type == 'hosted' && currentRepo.format == 'apt') {
            configuration.attributes['apt'] = [
                    distribution: currentRepo.distribution
            ]
            configuration.attributes['aptSigning'] = [
                    keypair   : currentRepo.keypair,
                    passphrase: currentRepo.passphrase
            ]
        }

        // Configs for all proxy repos
        if (currentRepo.type == 'proxy') {
            configuration.attributes['httpclient'] = [
                    blocked       : false,
                    autoBlock     : true,
                    connection    : [
                            useTrustStore: false
                    ]
            ]

            if (currentRepo.remote_username) {
                configuration.attributes['httpclient']['authentication'] = [
                    type    : 'username',
                    username: currentRepo.remote_username,
                    password: currentRepo.remote_password
                ]
            }

            configuration.attributes['proxy'] = [
                    remoteUrl     : currentRepo.remote_url,
                    contentMaxAge : currentRepo.get('maximum_component_age', 1440.0),
                    metadataMaxAge: currentRepo.get('maximum_metadata_age', 1440.0)
            ]

            configuration.attributes['negativeCache'] = [
                    enabled: currentRepo.get('negative_cache_enabled', true),
                    timeToLive: currentRepo.get('negative_cache_ttl', 1440.0)
            ]
        }

        // Configure cleanup policy
        if (currentRepo.type == 'proxy' || currentRepo.type == 'hosted') {
            def cleanupPolicies = currentRepo.cleanup_policies as Set
            if (cleanupPolicies != null)
            {
                configuration.attributes['cleanup'] = [
                    policyName: cleanupPolicies
                ]
            }
        }

        // Configs for nuget proxy repos
        if (currentRepo.type == 'proxy' && currentRepo.format == 'nuget') {
            configuration.attributes['nugetProxy'] = [
                    nugetVersion: currentRepo.nuget_version.toUpperCase()
            ]
        }

        // Configs for docker proxy repos
        if (currentRepo.type == 'proxy' && currentRepo.format == 'docker') {
            configuration.attributes['dockerProxy'] = [
                    indexType                  : currentRepo.index_type,
                    useTrustStoreForIndexAccess: currentRepo.use_nexus_certificates_to_access_index,
                    foreignLayerUrlWhitelist   : currentRepo.foreign_layer_url_whitelist,
                    cacheForeignLayers         : currentRepo.cache_foreign_layers
            ]
        }

        // Configs for all maven repos
        if (currentRepo.format == 'maven2') {
            configuration.attributes['maven'] = [
                    versionPolicy: currentRepo.version_policy.toUpperCase(),
                    layoutPolicy : currentRepo.layout_policy.toUpperCase()
            ]
        }

        // Configs for all docker repos
        if (currentRepo.format == 'docker') {
            def dockerPort = currentRepo.get('http_port', '')
            dockerPort = !(dockerPort instanceof String) ? dockerPort as String : dockerPort
            configuration.attributes['docker'] = [
                    forceBasicAuth: currentRepo.force_basic_auth,
                    v1Enabled     : currentRepo.v1_enabled,
                    httpPort      : dockerPort?.isInteger() ? dockerPort.toInteger() : null
            ]
        }

        if (existingRepository == null) {
            repositoryManager.create(configuration)
            currentResult.put('status', 'created')
            scriptResults['changed'] = true
            log.info('Configuration for repo {} created', currentRepo.name)
        } else {
            if (configurationChanged(existingRepository.configuration, configuration)) {
                repositoryManager.update(configuration)
                currentResult.put('status', 'updated')
                log.info('Configuration for repo {} saved', currentRepo.name)
                scriptResults['changed'] = true
            } else {
                currentResult.put('status', 'no change')
            }
        }

    } catch (Exception e) {
        currentResult.put('status', 'error')
        currentResult.put('error_msg', e.toString())
        scriptResults['error'] = true
        log.error('Configuration for repo {} could not be saved: {}', currentRepo.name, e.toString())
    }
    scriptResults['action_details'].add(currentResult)
}

return JsonOutput.toJson(scriptResults)
