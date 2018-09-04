import groovy.json.JsonSlurper
import org.sonatype.nexus.selector.SelectorManager
import org.sonatype.nexus.selector.SelectorConfiguration

parsed_args = new JsonSlurper().parseText(args)

selectorManager = container.lookup(SelectorManager.class.name)

def selectorConfig
boolean update = true

selectorConfig = selectorManager.browse().find { it -> it.name == parsed_args.name } 

if (selectorConfig == null) {
    update = false
    selectorConfig = new SelectorConfiguration(
        'name': parsed_args.name
    )
}

selectorConfig.setDescription(parsed_args.description)
selectorConfig.setType('csel')
selectorConfig.setAttributes([
    'expression': parsed_args.search_expression
] as Map<String, Object>)

if (update) {
    selectorManager.update(selectorConfig)
} else {
    selectorManager.create(selectorConfig)
}
