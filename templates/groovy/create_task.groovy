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

if (existingTask && !existingTask.remove()) {
    throw new RuntimeException("Could not remove currently running task : " + parsed_args.name)
}

TaskConfiguration taskConfiguration = taskScheduler.createTaskConfigurationInstance(parsed_args.typeId)
taskConfiguration.setName(parsed_args.name)

parsed_args.taskProperties.each { key, value -> taskConfiguration.setString(key, value) }

Schedule schedule = taskScheduler.scheduleFactory.cron(new Date(), parsed_args.cron)

taskScheduler.scheduleTask(taskConfiguration, schedule)