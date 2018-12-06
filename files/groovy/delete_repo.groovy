/**
 * Delete the repsotiroy of the name given in input if exist
 *
 * Input is json:
 *   name: Repository name
 * Output is json:
 *   result: "true" if ok
 *           "error message" if not ok
 *           "null" if repository doesn't exist
 *
 **/

import groovy.json.JsonSlurper
import org.sonatype.nexus.repository.Repository

def arg = new JsonSlurper().parseText(args)

if(arg.name == null) {
    return "you need to provid a repository 'name'"
}
Repository repo = repository.repositoryManager.get(arg.name)

if (repo != null) {
    try {
        repository.repositoryManager.delete(repo.name)
        repo.destroy()
    } catch(Exception e) {
        return e.toString()
    }
    return true
}