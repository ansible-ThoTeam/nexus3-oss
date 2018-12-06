import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.Repository

def arg = new JsonSlurper().parseText(args)

if(arg.name == null) {
    return "you need to provid a repository 'name'"
}
Repository repo = repository.repositoryManager.get(arg.name)

if (repo != null) {
    repository.repositoryManager.delete(repo.name)
    repo.destroy()
}