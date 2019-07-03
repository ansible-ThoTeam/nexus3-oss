import groovy.json.JsonSlurper
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInfo
import org.sonatype.nexus.scheduling.TaskScheduler
import org.sonatype.nexus.scheduling.schedule.Schedule

parsed_args = new JsonSlurper().parseText(args)

TaskScheduler taskScheduler = container.lookup(TaskScheduler.class.getName())

TaskInfo existingTask = taskScheduler.listsTasks().find { TaskInfo taskInfo ->
    taskInfo.name == parsed_args.name
}

if (existingTask && existingTask.getCurrentState().getRunState() != null) {
    log.info('Could not update currently running task : {}', parsed_args.name)
    return
}

TaskConfiguration taskConfiguration = taskScheduler.createTaskConfigurationInstance(parsed_args.typeId)
if (existingTask) { taskConfiguration.setId(existingTask.getId()) }
taskConfiguration.setName(parsed_args.name)

parsed_args.taskProperties.each { key, value -> taskConfiguration.setString(key, value) }

if (parsed_args.task_alert_email) {
    taskConfiguration.setAlertEmail(parsed_args.task_alert_email)
}

parsed_args.booleanTaskProperties.each { key, value -> taskConfiguration.setBoolean(key, Boolean.valueOf(value)) }

String frequency = parsed_args.frequency ?: 'cron'

Schedule schedule
switch (frequency) {
    case 'cron':
        schedule = taskScheduler.scheduleFactory.cron(new Date(), parsed_args.cron)
        break
    case 'manual':
        schedule = taskScheduler.scheduleFactory.manual()
        break
    default:
        log.info('Unhandled `frequency` parameter value : {}', frequency)
        return
}

taskScheduler.scheduleTask(taskConfiguration, schedule)
