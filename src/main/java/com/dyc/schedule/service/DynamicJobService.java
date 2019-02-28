package com.dyc.schedule.service;

import com.dyc.schedule.dao.JobEntityRepository;
import com.dyc.schedule.entity.JobEntity;
import com.dyc.schedule.job.DynamicJob;
import com.dyc.schedule.util.JobStatus;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DynamicJobService {

    private static Logger logger = LoggerFactory.getLogger(DynamicJobService.class);

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    @Autowired
    private JobEntityRepository repository;

    //通过Id获取Job
    public JobEntity getJobEntityById(Integer id) {
        Optional<JobEntity> op = repository.findById(id);
        return op.isPresent() ? op.get() : null;
    }

    //从数据库中加载获取到所有Job
    public List<JobEntity> loadJobs() {
        List<JobEntity> list = new ArrayList<>();
        repository.findAll().forEach(list::add);
        return list;
    }

    //获取JobDataMap.(Job参数对象)
    public JobDataMap getJobDataMap(JobEntity job) {
        JobDataMap map = new JobDataMap();
        map.put("jobName", job.getJobName());
        map.put("jobGroup", job.getJobGroup());
        map.put("cronExpression", job.getCron());
        map.put("parameter", job.getParameter());
        map.put("JobDescription", job.getDescription());
        map.put("method", job.getMethod());
        map.put("api", job.getApi());
        map.put("status", job.getStatus());
        return map;
    }

    //获取JobDetail,JobDetail是任务的定义,而Job是任务的执行逻辑,JobDetail里会引用一个Job Class来定义
    public JobDetail geJobDetail(JobKey jobKey, String description, JobDataMap map) {
        return JobBuilder.newJob(DynamicJob.class)
                .withIdentity(jobKey)
                .withDescription(description)
                .setJobData(map)
                .storeDurably()
                .build();
    }

    //获取Trigger (Job的触发器,执行规则)
    public Trigger getTrigger(JobEntity job) {
        return TriggerBuilder.newTrigger()
                .withIdentity(job.getJobName(), job.getJobGroup())
                .withSchedule(CronScheduleBuilder.cronSchedule(job.getCron()))
                .build();
    }

    //获取JobKey,包含Name和Group
    public JobKey getJobKey(JobEntity job) {
        return JobKey.jobKey(job.getJobName(), job.getJobGroup());
    }

    /**
     * 获取所有运行中的任务
     *
     * @return
     * @throws SchedulerException
     */
    public List<JobEntity> getAllRunningJob() throws SchedulerException {

        List<JobExecutionContext> executionJobList = schedulerFactoryBean.getScheduler().getCurrentlyExecutingJobs();
        List<JobEntity> jobList = new ArrayList<>();
        for (JobExecutionContext jobExecutionContext : executionJobList) {
            JobDetail jobDetail = jobExecutionContext.getJobDetail();
            JobKey jobKey = jobDetail.getKey();
            Trigger trigger = jobExecutionContext.getTrigger();
            JobEntity scheduleJob = getScheduleJob(schedulerFactoryBean.getScheduler(), jobKey, trigger);
            jobList.add(scheduleJob);
        }
        return jobList;
    }

    /**
     * 更新新的任务或者添加一个新的任务
     *
     * @param scheduleJob
     * @throws Exception
     */
    public void saveOrUpdate(JobEntity scheduleJob) throws Exception {
        TriggerKey triggerKey = TriggerKey.triggerKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
        CronTrigger cronTrigger = (CronTrigger) schedulerFactoryBean.getScheduler().getTrigger(triggerKey);
        if (cronTrigger == null) {
            logger.info("save a new job:" + scheduleJob.toString());
            addJob(scheduleJob);
        } else {
            logger.info("update a job:" + scheduleJob.toString());
            updateJobCronSchedule(scheduleJob);
        }
    }

    private JobEntity getScheduleJob(Scheduler scheduler, JobKey jobKey, Trigger trigger) {
        JobEntity scheduleJob = new JobEntity();
        try {
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            scheduleJob = (JobEntity) jobDetail.getJobDataMap().get("scheduleJob");
            Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
            scheduleJob.setStatus(triggerState.name());
            scheduleJob.setJobName(jobKey.getName());
            scheduleJob.setJobGroup(jobKey.getGroup());
            if (trigger instanceof CronTrigger) {
                CronTrigger cronTrigger = (CronTrigger) trigger;
                scheduleJob.setCron(cronTrigger.getCronExpression());
            }

        } catch (Exception e) {
            logger.error("[SchedulerJobServiceImpl] method getScheduleJob get JobDetail error:{}", e);
        }
        return scheduleJob;
    }

    /**
     * 添加任务
     *
     * @param scheduleJob
     * @throws Exception
     */
    private void addJob(JobEntity scheduleJob) throws Exception {
        checkNotNull(scheduleJob);
        if (StringUtils.isBlank(scheduleJob.getCron())) {
            throw new Exception("[SchedulerJobServiceImpl] CronExpression is null");
        }
        scheduleJob.setStatus(JobStatus.NORMAL.name());
        repository.save(scheduleJob);
        logger.info("job add to db success." + scheduleJob.toString());
        JobDetail jobDetail = JobBuilder.newJob(DynamicJob.class).withIdentity(scheduleJob.getJobName(), scheduleJob.getJobGroup())
                .build();
        jobDetail.getJobDataMap().put("scheduleJob", scheduleJob);
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(scheduleJob.getCron());
        CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity(scheduleJob.getJobName(), scheduleJob.getJobGroup())
                .withSchedule(cronScheduleBuilder).build();
        schedulerFactoryBean.getScheduler().scheduleJob(jobDetail, cronTrigger);

    }

    /**
     * 运行一个任务
     *
     * @param id
     * @throws SchedulerException
     */
    public void runOneJob(Integer id) throws SchedulerException {
        Optional<JobEntity> scheduleJob = repository.findById(id);
        if(!scheduleJob.isPresent()){
            throw new SchedulerException("job not found.");
        }
        if(JobStatus.PAUSED.name().equals(scheduleJob.get().getStatus())){
            throw new SchedulerException("job status is PAUSED!");
        }
        JobKey jobKey = JobKey.jobKey(scheduleJob.get().getJobName(), scheduleJob.get().getJobGroup());
        schedulerFactoryBean.getScheduler().triggerJob(jobKey);
    }

    /**
     * 重启一个任务
     * @param id
     * @throws SchedulerException
     */
    public void resumeJob(Integer id) throws SchedulerException{
        Optional<JobEntity> op = repository.findById(id);
        if(!op.isPresent()){
            throw new SchedulerException("job not found.");
        }
        JobEntity scheduleJob = op.get();
        JobKey jobKey = JobKey.jobKey(scheduleJob.getJobName(),scheduleJob.getJobGroup());
        scheduleJob.setStatus(JobStatus.NORMAL.name());
        repository.save(scheduleJob);
        schedulerFactoryBean.getScheduler().resumeJob(jobKey);
        JobDataMap map = getJobDataMap(scheduleJob);
        JobDetail jobDetail = geJobDetail(jobKey, scheduleJob.getDescription(), map);
        schedulerFactoryBean.getScheduler().scheduleJob(jobDetail, getTrigger(scheduleJob));
    }

    /**
     * 停止运行任务
     * @param id
     * @throws SchedulerException
     */
    public void pauseJob(Integer id) throws SchedulerException{
        Optional<JobEntity> op = repository.findById(id);
        if(!op.isPresent()){
            throw new SchedulerException("job not found.");
        }
        JobEntity scheduleJob = op.get();
        JobKey jobKey = JobKey.jobKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
        scheduleJob.setStatus(JobStatus.PAUSED.name());
        repository.save(scheduleJob);
        schedulerFactoryBean.getScheduler().pauseJob(jobKey);
        TriggerKey triggerKey = new TriggerKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
        schedulerFactoryBean.getScheduler().unscheduleJob(triggerKey);
        schedulerFactoryBean.getScheduler().deleteJob(jobKey);
    }

    /**
     * 删除一个任务
     * @param id
     * @throws SchedulerException
     */
    public void deleteJob(Integer id) throws SchedulerException{
        Optional<JobEntity> op = repository.findById(id);
        if(!op.isPresent()){
            throw new SchedulerException("job not found.");
        }
        JobEntity scheduleJob = op.get();
        JobKey jobKey = JobKey.jobKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
        repository.deleteById(id);
        schedulerFactoryBean.getScheduler().deleteJob(jobKey);
    }


    /**
     * 更新一个任务
     *
     * @param scheduleJob
     * @throws Exception
     */
    private void updateJobCronSchedule(JobEntity scheduleJob) throws Exception {
        checkNotNull(scheduleJob);
        if (StringUtils.isBlank(scheduleJob.getCron())) {
            throw new Exception("[SchedulerJobServiceImpl] CronExpression is null.");
        }
        TriggerKey triggerKey = TriggerKey.triggerKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
        CronTrigger cronTrigger = (CronTrigger) schedulerFactoryBean.getScheduler().getTrigger(triggerKey);
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(scheduleJob.getCron());
        cronTrigger = cronTrigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(cronScheduleBuilder).build();
        JobKey jobKey = JobKey.jobKey(scheduleJob.getJobName(), scheduleJob.getJobGroup());
        JobDetail jobDetail = schedulerFactoryBean.getScheduler().getJobDetail(jobKey);
        jobDetail.getJobDataMap().put("scheduleJob", scheduleJob);
        schedulerFactoryBean.getScheduler().rescheduleJob(triggerKey, cronTrigger);
        //scheduleJobInService.updateByPrimaryKey(scheduleJob);
        repository.save(scheduleJob);
    }

    /**
     * 判断一个任务是否为空
     *
     * @param scheduleJob
     */
    public void checkNotNull(JobEntity scheduleJob) {
        if (scheduleJob == null) {
            throw new IllegalStateException("scheduleJob is null,Please check it");
        }
        if (StringUtils.isBlank(scheduleJob.getJobName())) {
            throw new IllegalStateException("the jobName of scheduleJob is null,Please check it");
        }
        if (StringUtils.isBlank(scheduleJob.getJobGroup())) {
            throw new IllegalStateException("the jobGroup of scheduleJob is null,Please check it");
        }
    }
}
