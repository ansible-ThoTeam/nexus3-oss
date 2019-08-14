import groovy.json.JsonSlurper
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInfo
import org.sonatype.nexus.scheduling.TaskScheduler
import org.sonatype.nexus.scheduling.schedule.Monthly
import org.sonatype.nexus.scheduling.schedule.Schedule
import org.sonatype.nexus.scheduling.schedule.Weekly

parsed_args = new JsonSlurper().parseText(args)

TaskScheduler taskScheduler = container.lookup(TaskScheduler.class.getName())

TaskInfo existingTask = taskScheduler.listsTasks().find { TaskInfo taskInfo ->
    taskInfo.name == parsed_args.name
}

if (existingTask && existingTask.getCurrentState().getRunState() != null) {
    log.info("Could not update currently running task : " + parsed_args.name)
    return
}

TaskConfiguration taskConfiguration = taskScheduler.createTaskConfigurationInstance(parsed_args.typeId)
if (existingTask) {
    taskConfiguration.setId(existingTask.getId())
}
taskConfiguration.setName(parsed_args.name)

parsed_args.taskProperties.each { key, value -> taskConfiguration.setString(key, value) }

if (parsed_args.task_alert_email) {
    taskConfiguration.setAlertEmail(parsed_args.task_alert_email)
}

parsed_args.booleanTaskProperties.each { key, value -> taskConfiguration.setBoolean(key, Boolean.valueOf(value)) }

if (parsed_args.schedule || parsed_args.cron) {

    def splitters = /\s*[\s|,.:;_-]+\s*/

    Schedule sch

    def start_date = parsed_args?.start_date ? Date.parse('dd/MM/yyyy HH:mm', parsed_args.start_date) : new Date()


    switch (parsed_args?.schedule) {
        case "manual":
            taskConfiguration.
                    sch = taskScheduler.scheduleFactory.manual()
            break
        case "hourly":
            sch = taskScheduler.scheduleFactory.hourly(start_date)
            break
        case "daily":
            sch = taskScheduler.scheduleFactory.daily(start_date)
            break
        case "weakly":
            Set<Weekly.Weekday> weekdays = [] as Set<Weekly.Weekday>
            (parsed_args?.weekly_days instanceof ArrayList ? parsed_args?.weekly_days : parsed_args?.weekly_days?.split(splitters)).each { day ->
                if (day) {
                    weekdays.add(Weekly.Weekday.valueOf(day))
                }
            }
            sch = taskScheduler.scheduleFactory.weekly(start_date, weekdays)
            break
        case "monthly":
            Set<Monthly.CalendarDay> calendarDays = [] as Set<Monthly.CalendarDay>
            (parsed_args?.monthly_days instanceof ArrayList ? parsed_args?.monthly_days : parsed_args?.monthly_days?.split(splitters)).each { monthlyDay ->
                if (monthlyDay && (monthlyDay instanceof Number || monthlyDay.isInteger())) {
                    calendarDays.add(Monthly.CalendarDay.day(monthlyDay as int))
                }
            }
            sch = taskScheduler.scheduleFactory.monthly(start_date, calendarDays)
            break
        default:
            sch = taskScheduler.scheduleFactory.cron(new Date(), parsed_args?.cron)
            break
    }

    taskScheduler.scheduleTask(taskConfiguration, sch)
} else {
    log.info("Could not update currently running task : " + parsed_args.name + ". You have to define schedule or cron.")
    return
}
