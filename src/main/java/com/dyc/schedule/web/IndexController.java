package com.dyc.schedule.web;

import com.dyc.schedule.entity.JobEntity;
import com.dyc.schedule.service.DynamicJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class IndexController {

    private static Logger logger = LoggerFactory.getLogger(IndexController.class);

    @Autowired
    private DynamicJobService jobService;

    @RequestMapping("/")
    public String index(HttpServletRequest request){
        logger.info("[JobController] the url path:------------/index----------------");
        logger.info("[JobController] the method index is start......");
        //List<ScheduleJob> jobList = schedulerJobService.getAllScheduleJob();

        List<JobEntity> jobList = jobService.loadJobs();
        request.setAttribute("jobs",jobList);
        System.out.println(jobList);
        logger.info("[JobController] the method index is end......");
        return "index";
    }
}
