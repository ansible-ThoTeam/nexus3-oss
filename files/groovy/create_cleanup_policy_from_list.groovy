// Inspired from:
// https://github.com/idealista/nexus-role/blob/master/files/scripts/cleanup_policy.groovy
import com.google.common.collect.Maps
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import java.util.concurrent.TimeUnit

import org.sonatype.nexus.cleanup.storage.CleanupPolicy
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.IS_PRERELEASE_KEY
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_BLOB_UPDATED_KEY
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_DOWNLOADED_KEY
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.REGEX_KEY;


def cleanupPolicyStorage = container.lookup(CleanupPolicyStorage.class.getName())

parsed_args = new JsonSlurper().parseText(args)

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)

parsed_args.each { currentPolicy ->
    
    Map<String, String> currentResult = [name: currentPolicy.name, format: currentPolicy.format, mode: currentPolicy.mode]
    
    try {
    
        if (currentPolicy.name == null) {
            throw new Exception("Missing mandatory argument: name")
        }
    
        // create and update use this
        Map<String, String> criteriaMap = createCriteria(currentPolicy)
    
        // "update" operation
        if (cleanupPolicyStorage.exists(currentPolicy.name)) {
            existingPolicy = cleanupPolicyStorage.get(currentPolicy.name)
            if ( existingPolicy.getNotes() == currentPolicy.notes
                && existingPolicy.getFormat() == currentPolicy.format
                && existingPolicy.getCriteria()[LAST_BLOB_UPDATED_KEY] == asStringSeconds(currentPolicy.criteria.lastBlobUpdated)
                && existingPolicy.getCriteria()[LAST_DOWNLOADED_KEY] == asStringSeconds(currentPolicy.criteria.lastDownloaded)
                && existingPolicy.getCriteria()[REGEX_KEY] == currentPolicy.criteria.regexKey
            )
            {
                log.info("No change Cleanup Policy <name=${currentPolicy.name}>")
                return JsonOutput.toJson(scriptResults)
            } else {
                log.info("Update Cleanup Policy <name={}, format={}, lastBlob={}, lastDownload={}>", 
                            currentPolicy.name, 
                            currentPolicy.format, 
                            currentPolicy.criteria.lastBlobUpdated, 
                            currentPolicy.criteria.lastDownloaded)
                existingPolicy = cleanupPolicyStorage.get(currentPolicy.name)
                existingPolicy.setNotes(currentPolicy.notes)
                existingPolicy.setCriteria(criteriaMap)
                cleanupPolicyStorage.update(existingPolicy)
                
                currentResult.put('status', 'updated')
                scriptResults['changed'] = true
                return JsonOutput.toJson(scriptResults)
            }
        } else {
            // "create" operation
            format = currentPolicy.format == "all" ? "ALL_FORMATS" : currentPolicy.format
            log.info("Creating Cleanup Policy <name={}, format={}, lastBlob={}, lastDownload={}>", 
                            currentPolicy.name, 
                            currentPolicy.format, 
                            currentPolicy.criteria.lastBlobUpdated, 
                            currentPolicy.criteria.lastDownloaded)
            cleanupPolicy = new CleanupPolicy(
                    name: currentPolicy.name,
                    notes: currentPolicy.notes,
                    format: format,
                    mode: 'deletion',
                    criteria: criteriaMap
            )
            cleanupPolicyStorage.add(cleanupPolicy)
        
            currentResult.put('status', 'created')
            scriptResults['changed'] = true
        }
    } catch (Exception e) {
        currentResult.put('status', 'error')
        currentResult.put('error_msg', e.toString())
        scriptResults['error'] = true
        log.error('Configuration for repo {} could not be saved: {}', currentPolicy.name, e.toString())
    }
    scriptResults['action_details'].add(currentResult)
}
return JsonOutput.toJson(scriptResults)


def Map<String, String> createCriteria(currentPolicy) {
    Map<String, String> criteriaMap = Maps.newHashMap()
    if (currentPolicy.criteria.lastBlobUpdated == null) {
        criteriaMap.remove(LAST_BLOB_UPDATED_KEY)
    } else {
        criteriaMap.put(LAST_BLOB_UPDATED_KEY, asStringSeconds(currentPolicy.criteria.lastBlobUpdated))
    }
    if (currentPolicy.criteria.lastDownloaded == null) {
        criteriaMap.remove(LAST_DOWNLOADED_KEY)
    } else {
        criteriaMap.put(LAST_DOWNLOADED_KEY, asStringSeconds(currentPolicy.criteria.lastDownloaded))
    }
    if (currentPolicy.criteria.regexKey != "") {
       criteriaMap.put(REGEX_KEY, String.valueOf(currentPolicy.criteria.regexKey))
    }
    
    log.info("Using criteriaMap: ${criteriaMap}")

    return criteriaMap
}

def Integer asSeconds(days) {
    return days * TimeUnit.DAYS.toSeconds(1)
}

def String asStringSeconds(daysInt) {
    return String.valueOf(asSeconds(daysInt))
}
