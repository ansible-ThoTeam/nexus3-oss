import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

existingBlobStore = blobStore.getBlobStoreManager().get(parsed_args.name)
if (existingBlobStore == null) {
    def config = [bucket:parsed_args.bucket, accessKeyId:parsed_args.accessKeyId, secretAccessKey:parsed_args.secretAccessKey, region:parsed_args.region]
    blobStore.createS3BlobStore(parsed_args.name, config)
}
