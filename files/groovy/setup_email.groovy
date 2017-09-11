import groovy.json.JsonSlurper
import org.sonatype.nexus.email.EmailConfiguration
import org.sonatype.nexus.email.EmailManager

parsed_args = new JsonSlurper().parseText(args)

def emailMgr = container.lookup(EmailManager.class.getName());

emailConfig = new EmailConfiguration(
        enabled: parsed_args.email_server_enabled,
        host: parsed_args.email_server_host,
        port: Integer.valueOf(parsed_args.email_server_port),
        username: parsed_args.email_server_username,
        password: parsed_args.email_server_password,
        fromAddress: parsed_args.email_from_address,
        subjectPrefix: parsed_args.email_subject_prefix,
        startTlsEnabled: parsed_args.email_tls_enabled,
        startTlsRequired: parsed_args.email_tls_required,
        sslOnConnectEnabled: parsed_args.email_ssl_on_connect_enabled,
        sslCheckServerIdentityEnabled: parsed_args.email_ssl_check_server_identity_enabled,
        nexusTrustStoreEnabled: parsed_args.email_trust_store_enabled
)

emailMgr.configuration = emailConfig
