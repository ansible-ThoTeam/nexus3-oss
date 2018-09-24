import groovy.json.JsonSlurper

parsed_args = new JsonSlurper().parseText(args)

existingBlobStore = blobStore.getBlobStoreManager().get(parsed_args.name)
if (existingBlobStore == null) {
  if (parsed_args.type == "S3") {
      blobStore.createS3BlobStore(parsed_args.name, parsed_args.config)
      msg = "S3 blobstore {} created"
  } else {
      blobStore.createFileBlobStore(parsed_args.name, parsed_args.path)
      msg = "Created blobstore {} created"
  }
  log.info(msg, parsed_args.name)
} else {
    msg = "Blobstore {} already exists. Left untouched"
}

log.info(msg, parsed_args.name)
