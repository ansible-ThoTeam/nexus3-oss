import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

existingBlobStore = blobStore.getBlobStoreManager().get(parsed_args.name)
if (existingBlobStore == null) {
  if (parsed_args.type == "S3") {
      blobStore.createS3BlobStore(parsed_args.name, parsed_args.config)
  } else {
      blobStore.createFileBlobStore(parsed_args.name, parsed_args.path)
  }
}
