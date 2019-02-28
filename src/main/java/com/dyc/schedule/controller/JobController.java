package com.dyc.schedule.controller;

import com.dyc.schedule.entity.JobEntity;
import com.dyc.schedule.service.DynamicJobService;
import com.dyc.schedule.util.JobStatus;
import com.dyc.schedule.util.Message;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class JobController {

    private static Logger logger = LoggerFactory.getLogger(JobController.class);

    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;

    @Autowired
    private DynamicJobService jobService;

    //初始化启动所有的Job
    @PostConstruct
    public void initialize() {
        try {
            reStartAllJobs();
            logger.info("INIT SUCCESS");
        } catch (SchedulerException e) {
            logger.info("INIT EXCEPTION : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     *获取所有的任务
     * @return
     */
    @GetMapping("/getAllJobs")
    @ResponseBody
    public List<JobEntity> getAllJobs(){
        logger.info("[JobController] the method:getAllJobs!");
        List<JobEntity> jobList = jobService.loadJobs();
        logger.info("[JobController] the method:getAllJobs is execution over ");
        return jobList;
    }

    /**
     * 获取正在执行的任务列表
     * @return
     * @throws SchedulerException
     */
    @GetMapping("/getRunJob")
    @ResponseBody
    public List<JobEntity> getAllRunningJob() throws SchedulerException{
        logger.info("[JobController] the method:getAllRunningJob!");
        return jobService.getAllRunningJob();
    }

    /**
     *更新或者添加一个任务
     * @param scheduleJob
     */
    @PostMapping("/saveOrUpdate")
    @ResponseBody
    public Object addOrUpdateJob(@ModelAttribute JobEntity scheduleJob){
        logger.info("[JobController] the method addOrUpdateJob, the param:{}", scheduleJob);
        Message message = Message.failure();
        try {
            jobService.saveOrUpdate(scheduleJob);
            message = Message.success();
        } catch (Exception e) {
            message.setMsg(e.getMessage());
            logger.error("[JobController] addOrUpdateJob is failure in method:addOrUpdateJob！");
        }
        return message;
    }

    /**
     *运行一个任务
     * @param id
     */
    @PostMapping("/runOneJob")
    @ResponseBody
    public Object runJob(Integer id){
        logger.info("----------------runOneJob----------------");
        Message message  = Message.failure();
        try {
            jobService.runOneJob(id);
            message = Message.success();
        } catch (SchedulerException e) {
            message.setMsg(e.getMessage());
            logger.error("[JobController] runOnejob is failure in method:runJob");
        }
        return message;
    }

    /**
     * 重启一个定时任务
     * @param id
     * @return
     */
    @ResponseBody
    @PostMapping("/resumeJob")
    public Object resumeJob(Integer id){
        logger.info("----------------resumeJob----------------");
        Message message = Message.failure();
        try {
            jobService.resumeJob(id);
            message = Message.success();
        } catch (SchedulerException e) {
            message.setMsg(e.getMessage());
            logger.error("[JobController] resumeJob is failre in method: resumeJob!");
        }
        return message;
    }

    /**
     *停止一个定时任务
     * @param id
     */
    @PostMapping("/pauseJob")
    @ResponseBody
    public Object pauseJob(Integer id){
        logger.info("------------closeJob----------------");
        Message message = Message.failure();
        try {
            jobService.pauseJob(id);
            message = Message.success();
        } catch (SchedulerException e) {
            message.setMsg(e.getMessage());
            logger.error("[JobController] pauseJob is failure in method:pauseJob");
        }
        return message;
    }

    /**
     * 删除一个定时任务
     * @param id
     * @return
     */
    @PostMapping("/deleteJob")
    @ResponseBody
    public Object deleteJob(Integer id){
        logger.info("------------deleteJob----------------");
        Message message = Message.failure();
        try {
            jobService.deleteJob(id);
            message = Message.success();
        } catch (SchedulerException e) {
            message.setMsg(e.getMessage());
            logger.error("[JobController] deleteJob is failre in method: deleteJob!");
        }
        return message;
    }


    //根据ID重启某个Job
    @GetMapping("/refresh/{id}")
    public String refresh(@PathVariable Integer id) throws SchedulerException {
        String result;
        JobEntity entity = jobService.getJobEntityById(id);
        if (entity == null) return "error: id is not exist ";
        TriggerKey triggerKey = new TriggerKey(entity.getJobName(), entity.getJobGroup());
        JobKey jobKey = jobService.getJobKey(entity);
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        try {
            scheduler.unscheduleJob(triggerKey);
            scheduler.deleteJob(jobKey);
            JobDataMap map = jobService.getJobDataMap(entity);
            JobDetail jobDetail = jobService.geJobDetail(jobKey, entity.getDescription(), map);
            if (entity.getStatus().equals(JobStatus.NORMAL.name())) {
                scheduler.scheduleJob(jobDetail, jobService.getTrigger(entity));
                result = "Refresh Job : " + entity.getJobName() + "\t api: " + entity.getApi() + " success !";
            } else {
                result = "Refresh Job : " + entity.getJobName() + "\t api: " + entity.getApi() + " failed ! , " +
                        "Because the Job status is " + entity.getStatus();
            }
        } catch (SchedulerException e) {
            result = "Error while Refresh " + e.getMessage();
        }
        return result;
    }


    //重启数据库中所有的Job
    @GetMapping("/refresh/all")
    public String refreshAll() {
        String result;
        try {
            reStartAllJobs();
            result = "SUCCESS";
        } catch (SchedulerException e) {
            result = "EXCEPTION : " + e.getMessage();
        }
        return "refresh all jobs : " + result;
    }

    /**
     * 重新启动所有的job
     */
    private void reStartAllJobs() throws SchedulerException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        Set<JobKey> set = scheduler.getJobKeys(GroupMatcher.anyGroup());
        for (JobKey jobKey : set) {
            scheduler.deleteJob(jobKey);
        }
        for (JobEntity job : jobService.loadJobs()) {
            logger.info("Job register name : {} , group : {} , cron : {}", job.getJobName(), job.getJobGroup(), job.getCron());
            JobDataMap map = jobService.getJobDataMap(job);
            JobKey jobKey = jobService.getJobKey(job);
            JobDetail jobDetail = jobService.geJobDetail(jobKey, job.getDescription(), map);
            if (job.getStatus().equals(JobStatus.NORMAL.name())) scheduler.scheduleJob(jobDetail, jobService.getTrigger(job));
            else
                logger.info("Job jump name : {} , Because {} status is {}", job.getJobName(), job.getJobName(), job.getStatus());
        }
    }
}
