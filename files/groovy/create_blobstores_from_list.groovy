import groovy.json.JsonOutput
import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

List<Map<String, String>> blobstoreDetails = []
Map blobstoreResults = [changed: false, error: false]
blobstoreResults.put('action_details', blobstoreDetails)

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
            blobstoreResults['changed'] = true
        } catch (Exception e) {
            log.error('Could not create blobstore {}: {}', blobstoreDef.name, e.toString())
            currentResult.put('status', 'error')
            blobstoreResults['error'] = true
            currentResult.put('error_msg', e.toString())
        }
    } else {
        msg = "Blobstore {} already exists. Left untouched"
        currentResult.put('status', 'exists')
    }

    log.info(msg, blobstoreDef.name)

    blobstoreResults['action_details'].add(currentResult)
}

return JsonOutput.toJson(blobstoreResults)