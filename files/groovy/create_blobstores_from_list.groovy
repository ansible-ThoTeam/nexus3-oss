import groovy.json.JsonOutput
import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)

parsed_args.each { blobstoreDef ->

    Map<String, String> currentResult = [name: blobstoreDef.name, type: blobstoreDef.get('type', 'file')]

    existingBlobStore = blobStore.getBlobStoreManager().get(blobstoreDef.name)
    if (existingBlobStore == null) {
        try {
            if (blobstoreDef.type == "S3") {
                blobStore.createS3BlobStore(blobstoreDef.name, blobstoreDef.config)
                msg = "S3 blobstore {} created"
            } else {
                blobStore.createFileBlobStore(blobstoreDef.name, blobstoreDef.path)
                msg = "File blobstore {} created"
            }
            log.info(msg, blobstoreDef.name)
            currentResult.put('status', 'created')
            scriptResults['changed'] = true
        } catch (Exception e) {
            log.error('Could not create blobstore {}: {}', blobstoreDef.name, e.toString())
            currentResult.put('status', 'error')
            scriptResults['error'] = true
            currentResult.put('error_msg', e.toString())
        }
    } else {
        log.info("Blobstore {} already exists. Left untouched", blobstoreDef.name)
        currentResult.put('status', 'exists')
    }

    scriptResults['action_details'].add(currentResult)
}

return JsonOutput.toJson(scriptResults)
