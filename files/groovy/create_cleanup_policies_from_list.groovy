// Inspired from:
// https://github.com/idealista/nexus-role/blob/master/files/scripts/cleanup_policy.groovy
import com.google.common.collect.Maps
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.util.concurrent.TimeUnit

import org.sonatype.nexus.cleanup.storage.CleanupPolicy
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.IS_PRERELEASE_KEY
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_BLOB_UPDATED_KEY
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_DOWNLOADED_KEY
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.REGEX_KEY;


CleanupPolicyStorage cleanupPolicyStorage = container.lookup(CleanupPolicyStorage.class.getName())

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
            CleanupPolicy existingPolicy = cleanupPolicyStorage.get(currentPolicy.name)
            if ( isPolicyEqual(existingPolicy, currentPolicy) )
            {
                log.info("No change Cleanup Policy <name=${currentPolicy.name}>")
            } else {
                log.info("Update Cleanup Policy <name={}, format={}, lastBlob={}, lastDownload={}, prerelease={}, regex={}> ",
                            currentPolicy.name,
                            currentPolicy.format,
                            currentPolicy.criteria.lastBlobUpdated,
                            currentPolicy.criteria.lastDownloaded,
                            currentPolicy.criteria.preRelease,
                            currentPolicy.criteria.regexKey)
                existingPolicy.setNotes(currentPolicy.notes)
                existingPolicy.setCriteria(criteriaMap)
                cleanupPolicyStorage.update(existingPolicy)

                currentResult.put('status', 'updated')
                scriptResults['changed'] = true
            }
        } else {
            // "create" operation
            log.info("Creating Cleanup Policy <name={}, format={}, lastBlob={}, lastDownload={}, preRelease={}, regex={}>",
                            currentPolicy.name,
                            currentPolicy.format,
                            currentPolicy.criteria.lastBlobUpdated,
                            currentPolicy.criteria.lastDownloaded,
                            currentPolicy.criteria.preRelease,
                            currentPolicy.criteria.regexKey)

            CleanupPolicy cleanupPolicy = cleanupPolicyStorage.newCleanupPolicy()
            cleanupPolicy.with {
                setName(currentPolicy.name)
                setNotes(currentPolicy.notes)
                setFormat(currentPolicy.format == "all" ? "ALL_FORMATS" : currentPolicy.format)
                setMode('deletion')
                setCriteria(criteriaMap)
            }
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
    if ((currentPolicy.criteria.preRelease == null) || (currentPolicy.criteria.preRelease == "")) {
        criteriaMap.remove(IS_PRERELEASE_KEY)
    } else {
        criteriaMap.put(IS_PRERELEASE_KEY, Boolean.toString(currentPolicy.criteria.preRelease == "PRERELEASES"))
    }
    if ((currentPolicy.criteria.regexKey == null) || (currentPolicy.criteria.regexKey == "")) {
        criteriaMap.remove(REGEX_KEY)
    } else {
       criteriaMap.put(REGEX_KEY, String.valueOf(currentPolicy.criteria.regexKey))
    }
    log.info("Using criteriaMap: ${criteriaMap}")

    return criteriaMap
}

def Boolean isPolicyEqual(existingPolicy, currentPolicy) {
    Boolean isequal = true

    def currentCriteria = createCriteria(currentPolicy)

    isequal &= existingPolicy.getNotes() == currentPolicy.notes
    isequal &= existingPolicy.getFormat() == currentPolicy.format

    isequal &= (((! existingPolicy.getCriteria().containsKey(LAST_BLOB_UPDATED_KEY)) && (! currentCriteria.containsKey(LAST_BLOB_UPDATED_KEY)))
    ||  (existingPolicy.getCriteria().containsKey(LAST_BLOB_UPDATED_KEY)
        && currentCriteria.containsKey(LAST_BLOB_UPDATED_KEY)
        && existingPolicy.getCriteria()[LAST_BLOB_UPDATED_KEY] == currentCriteria[LAST_BLOB_UPDATED_KEY]))
    isequal &= ((! (existingPolicy.getCriteria().containsKey(LAST_DOWNLOADED_KEY)) && (! currentCriteria.containsKey(LAST_DOWNLOADED_KEY)))
    ||  (existingPolicy.getCriteria().containsKey(LAST_DOWNLOADED_KEY)
        && currentCriteria.containsKey(LAST_DOWNLOADED_KEY)
        && existingPolicy.getCriteria()[LAST_DOWNLOADED_KEY] == currentCriteria[LAST_DOWNLOADED_KEY]))

    isequal &= (((! existingPolicy.getCriteria().containsKey(IS_PRERELEASE_KEY)) && (! currentCriteria.containsKey(IS_PRERELEASE_KEY)))
    ||  (existingPolicy.getCriteria().containsKey(IS_PRERELEASE_KEY)
        && currentCriteria.containsKey(IS_PRERELEASE_KEY)
        && existingPolicy.getCriteria()[IS_PRERELEASE_KEY] == currentCriteria[IS_PRERELEASE_KEY]))

    isequal &= (((! existingPolicy.getCriteria().containsKey(REGEX_KEY)) && (! currentCriteria.containsKey(REGEX_KEY)))
    ||  (existingPolicy.getCriteria().containsKey(REGEX_KEY)
        && currentCriteria.containsKey(REGEX_KEY)
        && existingPolicy.getCriteria()[REGEX_KEY] == currentCriteria[REGEX_KEY]))

    return isequal
}

def Integer asSeconds(days) {
    return days * TimeUnit.DAYS.toSeconds(1)
}

def String asStringSeconds(daysInt) {
    return String.valueOf(asSeconds(daysInt))
}
