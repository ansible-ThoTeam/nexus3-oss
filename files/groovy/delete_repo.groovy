import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.Repository

def arg = new JsonSlurper().parseText(args)

Repository repo = repository.repositoryManager.get(arg.name)

if (repo != null) {
    repository.repositoryManager.delete(repo.name)
    repo.destroy()
}