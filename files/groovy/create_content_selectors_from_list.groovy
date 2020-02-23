import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.sonatype.nexus.selector.SelectorConfiguration
import org.sonatype.nexus.selector.SelectorManager

parsed_args = new JsonSlurper().parseText(args)

List<Map<String, String>> actionDetails = []
Map scriptResults = [changed: false, error: false]
scriptResults.put('action_details', actionDetails)

SelectorManager selectorManager = container.lookup(SelectorManager.class.name)

parsed_args.each { currentSelector ->

    Map<String, String> currentResult = [name: currentSelector.name]

    try {
        boolean update = true

        SelectorConfiguration selectorConfig = selectorManager.browse().find { it -> it.name == currentSelector.name }

        if (selectorConfig == null) {
            update = false
            try {
                selectorConfig = selectorManager.newSelectorConfiguration()
            } catch (MissingMethodException) {
                selectorConfig = SelectorConfiguration.newInstance()
            }
            selectorConfig.setName(currentSelector.name)
        } else {
            existingConfigDump = selectorConfig.dump()
        }

        selectorConfig.setDescription(currentSelector.description)
        selectorConfig.setType('csel')
        selectorConfig.setAttributes([
                'expression': currentSelector.search_expression
        ] as Map<String, Object>)

        if (update) {
            if (existingConfigDump != selectorConfig.dump()) {
                selectorManager.update(selectorConfig)
                currentResult.put('status', 'updated')
                scriptResults['changed'] = true
            } else {
                currentResult.put('status', 'no change')
            }
        } else {
            selectorManager.create(selectorConfig)
            currentResult.put('status', 'created')
            scriptResults['changed'] = true
        }
    } catch (Exception e) {
        currentResult.put('status', 'error')
        currentResult.put('error_msg', e.toString())
        scriptResults['error'] = true
    }

    scriptResults['action_details'].add(currentResult)
}

return JsonOutput.toJson(scriptResults)
