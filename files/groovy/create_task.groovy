import groovy.json.JsonSlurper
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskInfo
import org.sonatype.nexus.scheduling.TaskScheduler
import org.sonatype.nexus.scheduling.schedule.Monthly
import org.sonatype.nexus.scheduling.schedule.Schedule
import org.sonatype.nexus.scheduling.schedule.Weekly
import java.text.SimpleDateFormat

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

taskConfiguration.setAlertEmail(parsed_args.get('task_alert_email', '') as String)

taskConfiguration.setEnabled(Boolean.valueOf(parsed_args.get('enabled', 'true') as String))

parsed_args.taskProperties.each { key, value ->
    taskConfiguration.setString(key, value)
}

parsed_args.booleanTaskProperties.each { key, value ->
    taskConfiguration.setBoolean(key, Boolean.valueOf(value))
}

// Init empty/default vars from script call

// Type of schedule. Defaults to cron
type = parsed_args.get('schedule_type', 'cron')

// Cron expression. Used only for cron schecule. Defaults to null
cron = parsed_args.get('cron', null)

// Start date time. Defaults to now. Unused for manual, now and cron schedules
// This is our expected date format
SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
date_time_string = parsed_args.get('start_date_time', null)
Date start_date
if (date_time_string)
    start_date = dateFormat.parse(date_time_string)
else
    start_date = new Date()

// List of weekdays to run task. Used for weekly schedule. Need at least one
// defaults to null which will error if this schedule is chosen
weekly_days = parsed_args.get('weekly_days', null)

// List of calendar days to run task. Used for monthly schedule. Need at leas one
// defaults to null which will error if this schedule is chosen
monthly_days = parsed_args.get('monthly_days', null)

Schedule schedule
switch(type) {
    case 'manual':
        schedule = taskScheduler.scheduleFactory.manual()
        break
    case 'now':
        schedule = taskScheduler.scheduleFactory.now()
        break
    case 'once':
        schedule = taskScheduler.scheduleFactory.once(start_date)
        break
    case 'hourly':
        schedule = taskScheduler.scheduleFactory.hourly(start_date)
        break
    case 'daily':
        schedule = taskScheduler.scheduleFactory.daily(start_date)
        break
    case 'weekly':
        if (!weekly_days)
            throw new Exception('Weekly scehedule requires a weekly_days list parameter')
        Set<Weekly.Weekday> weekdays = []
        weekly_days.each { day ->
            weekdays.add(Weekly.Weekday.valueOf(day))
        }
        schedule = taskScheduler.scheduleFactory.weekly(start_date, weekdays)
        break
    case 'monthly':
        if (!monthly_days)
            throw new Exception('Monthly scehedule requires a weekly_days list parameter')
        Set<Monthly.CalendarDay> calendardays = []
        monthly_days.each { day ->
            calendardays.add(Monthly.CalendarDay.day(day as Integer))
        }
        schedule = taskScheduler.scheduleFactory.monthly(start_date, calendardays)
        break
    case 'cron':
        schedule = taskScheduler.scheduleFactory.cron(new Date(), cron)
        break
    default:
        /** @todo: dont crash, return error in json response **/
        throw new Exception("Unknown schedule type: " + parsed_args.schedule_type.toString())
        break
}

taskScheduler.scheduleTask(taskConfiguration, schedule)
