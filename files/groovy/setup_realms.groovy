import groovy.json.JsonSlurper
import org.sonatype.nexus.security.realm.RealmManager

parsed_args = new JsonSlurper().parseText(args)

realmManager = container.lookup(RealmManager.class.getName())

// enable/disable the NuGet API-Key Realm
realmManager.enableRealm("NuGetApiKey", parsed_args.nuget_api_key_realm)

// enable/disable the npm Bearer Token Realm
realmManager.enableRealm("NpmToken", parsed_args.npm_bearer_token_realm)

// enable/disable the Rut Auth Realm
realmManager.enableRealm("rutauth-realm", parsed_args.rut_auth_realm)

// enable/disable the LDAP Realm
realmManager.enableRealm("LdapRealm", parsed_args.ldap_realm)

// enable/disable the Docker Bearer Token Realm
realmManager.enableRealm("DockerToken", parsed_args.docker_bearer_token_realm)
