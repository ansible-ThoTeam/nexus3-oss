// Inspired from:
// https://github.com/idealista/nexus-role/blob/master/files/scripts/cleanup_policy.groovy
import com.google.common.collect.Maps
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.util.concurrent.TimeUnit

import org.sonatype.nexus.cleanup.storage.CleanupPolicy
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage


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
                            currentPolicy.criteria.isPrerelease,
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
                            currentPolicy.criteria.isPrerelease,
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
        criteriaMap.remove('lastBlobUpdated')
    } else {
        criteriaMap.put('lastBlobUpdated', asStringSeconds(currentPolicy.criteria.lastBlobUpdated))
    }
    if (currentPolicy.criteria.lastDownloaded == null) {
        criteriaMap.remove('lastDownloaded')
    } else {
        criteriaMap.put('lastDownloaded', asStringSeconds(currentPolicy.criteria.lastDownloaded))
    }
    if ((currentPolicy.criteria.isPrerelease == null) || (currentPolicy.criteria.isPrerelease == "")) {
        criteriaMap.remove('isPrerelease')
    } else {
        criteriaMap.put('isPrerelease', Boolean.toString(currentPolicy.criteria.isPrerelease == "PRERELEASES"))
    }
    if ((currentPolicy.criteria.regexKey == null) || (currentPolicy.criteria.regexKey == "")) {
        criteriaMap.remove('regex')
    } else {
       criteriaMap.put('regex', String.valueOf(currentPolicy.criteria.regexKey))
    }
    log.info("Using criteriaMap: ${criteriaMap}")

    return criteriaMap
}

def Boolean isPolicyEqual(existingPolicy, currentPolicy) {
    Boolean isequal = true

    def currentCriteria = createCriteria(currentPolicy)

    isequal &= existingPolicy.getNotes() == currentPolicy.notes
    isequal &= existingPolicy.getFormat() == currentPolicy.format

    isequal &= (((! existingPolicy.getCriteria().containsKey('lastBlobUpdated')) && (! currentCriteria.containsKey('lastBlobUpdated')))
    ||  (existingPolicy.getCriteria().containsKey('lastBlobUpdated')
        && currentCriteria.containsKey('lastBlobUpdated')
        && existingPolicy.getCriteria()['lastBlobUpdated'] == currentCriteria['lastBlobUpdated']))
    isequal &= ((! (existingPolicy.getCriteria().containsKey('lastDownloaded')) && (! currentCriteria.containsKey('lastDownloaded')))
    ||  (existingPolicy.getCriteria().containsKey('lastDownloaded')
        && currentCriteria.containsKey('lastDownloaded')
        && existingPolicy.getCriteria()['lastDownloaded'] == currentCriteria['lastDownloaded']))

    isequal &= (((! existingPolicy.getCriteria().containsKey('isPrerelease')) && (! currentCriteria.containsKey('isPrerelease')))
    ||  (existingPolicy.getCriteria().containsKey('isPrerelease')
        && currentCriteria.containsKey('isPrerelease')
        && existingPolicy.getCriteria()['isPrerelease'] == currentCriteria['isPrerelease']))

    isequal &= (((! existingPolicy.getCriteria().containsKey('regex')) && (! currentCriteria.containsKey('regex')))
    ||  (existingPolicy.getCriteria().containsKey('regex')
        && currentCriteria.containsKey('regex')
        && existingPolicy.getCriteria()['regex'] == currentCriteria['regex']))

    return isequal
}

def Integer asSeconds(days) {
    return days * TimeUnit.DAYS.toSeconds(1)
}

def String asStringSeconds(daysInt) {
    return String.valueOf(asSeconds(daysInt))
}
