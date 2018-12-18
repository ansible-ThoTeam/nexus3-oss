import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.Repository

def parsed_args = new JsonSlurper().parseText(args)

Repository repo = repository.repositoryManager.get(parsed_args.name)

if (repo != null) {
    repository.repositoryManager.delete(repo.name)
    repo.destroy()
}
