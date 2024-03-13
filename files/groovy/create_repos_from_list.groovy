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
                    online: currentRepo.get('online', true),
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
                    deployPolicy : currentRepo.get('layout_policy','strict').toUpperCase()
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
                    blocked       : currentRepo.get('blocked', false),
                    autoBlock     : currentRepo.get('auto_block', true),
                    connection    : [
                            useTrustStore: false,
                            timeout: currentRepo.get('connection_timeout', null),
                            retries: currentRepo.get('connection_retries', null),
                            userAgentSuffix: currentRepo.get('user_agent_suffix', null),
                            enableCircularRedirects: currentRepo.get('enable_circular_redirects', false),
                            enableCookies: currentRepo.get('enable_cookies', false)
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

        // Configure content disposition for maven and raw proxy repos
        if (currentRepo.type == 'proxy' && currentRepo.format == 'maven2' || currentRepo.format == 'raw'){
                configuration.attributes['raw'] = [
                    contentDisposition: currentRepo.content_disposition ? currentRepo.content_disposition.toUpperCase() : "INLINE"
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

        // Configs for npm proxy repos usign bearer token
        if (currentRepo.bearerToken && currentRepo.type == 'proxy' && currentRepo.format == 'npm') {
            configuration.attributes['httpclient']['authentication'] = [
                    type: 'bearerToken',
                    bearerToken: currentRepo.bearerToken
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
                    layoutPolicy : currentRepo.get('layout_policy','strict').toUpperCase()
            ]
        }

        // Configs for all docker repos
        if (currentRepo.format == 'docker') {
            def dockerPort = currentRepo.get('http_port', '')
            dockerPort = !(dockerPort instanceof String) ? dockerPort as String : dockerPort
            configuration.attributes['docker'] = [
                    forceBasicAuth: currentRepo.force_basic_auth,
                    v1Enabled     : currentRepo.v1_enabled,
                    httpPort      : dockerPort?.isInteger() ? dockerPort.toInteger() : null,
                    subdomain     : currentRepo.sub_domain ? currentRepo.sub_domain : null
            ]
        }

        // Configs for all docker/npm group repos
        if (currentRepo.type == 'group' && currentRepo.format in ['docker', 'npm']) {
            configuration.attributes['group'] = [
                // when setting the groupWriteMember, the memberNames must be set as well, API expects both objects
                    groupWriteMember: currentRepo.writable_member_repo,
                    memberNames: currentRepo.member_repos
            ]
        }

        if (currentRepo.allow_redeploy_latest && currentRepo.type == 'hosted' && currentRepo.format == 'docker') {
            configuration.attributes['storage'] = [
                latestPolicy: currentRepo.allow_redeploy_latest ? currentRepo.allow_redeploy_latest : null,
                // When setting the allow_redeploy_latest, the writePolicy must be set to ALLOW_ONCE and API expects blobStoreName param
                writePolicy: currentRepo.allow_redeploy_latest ? "ALLOW_ONCE" : currentRepo.write_policy.toUpperCase(),
                blobStoreName: currentRepo.blob_store
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
