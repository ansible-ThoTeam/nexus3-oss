import groovy.json.JsonSlurper
import org.sonatype.nexus.selector.SelectorConfiguration
import org.sonatype.nexus.selector.SelectorManager

parsed_args = new JsonSlurper().parseText(args)

SelectorManager selectorManager = container.lookup(SelectorManager.class.name)

boolean update = true

SelectorConfiguration selectorConfig = selectorManager.browse().find { it -> it.name == parsed_args.name }

if (selectorConfig == null) {
    update = false
    try {
        selectorConfig = selectorManager.newSelectorConfiguration()
        selectorConfig.setName(parsed_args.name)
    } catch (MissingMethodException) {
        // Compatibility with nexus versions older than 3.20
        selectorConfig = new SelectorConfiguration('name': parsed_args.name)
    }
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
