import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

existingBlobStore = blobStore.getBlobStoreManager().get(parsed_args.name)
if (existingBlobStore != null) {
    blobStore.getBlobStoreManager().delete(parsed_args.name)
}
