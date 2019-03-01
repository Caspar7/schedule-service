package com.dyc.schedule.job;

import com.dyc.schedule.util.StringUtils;
import com.google.gson.Gson;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * :@DisallowConcurrentExecution : 此标记用在实现Job的类上面,意思是不允许并发执行.
 * :注意org.quartz.threadPool.threadCount线程池中线程的数量至少要多个,否则@DisallowConcurrentExecution不生效
 * :假如Job的设置时间间隔为3秒,但Job执行时间是5秒,设置@DisallowConcurrentExecution以后程序会等任务执行完毕以后再去执行,否则会在3秒时再启用新的线程执行
 */
@DisallowConcurrentExecution
@Component
public class DynamicJob implements Job {

    private Logger logger = LoggerFactory.getLogger(DynamicJob.class);

    @Autowired
    RestTemplate restTemplate;

    /**
     * 核心方法,Quartz Job真正的执行逻辑.
     * @param executorContext executorContext JobExecutionContext中封装有Quartz运行所需要的所有信息
     * @throws JobExecutionException execute()方法只允许抛出JobExecutionException异常
     */
    @Override
    public void execute(JobExecutionContext executorContext) throws JobExecutionException {
        //JobDetail中的JobDataMap是共用的,从getMergedJobDataMap获取的JobDataMap是全新的对象
        JobDataMap map = executorContext.getMergedJobDataMap();
        String api = map.getString("api");
        String parameter = map.getString("parameter");
        String method = map.getString("method");
        logger.info("Running Job jobName : {} ", map.getString("jobName"));
        logger.info("Running Job description : " + map.getString("JobDescription"));
        logger.info("Running Job jobGroup: {} ", map.getString("jobGroup"));
        logger.info("Running Job cron : " + map.getString("cron"));
        logger.info("Running Job api : {} ", api);
        logger.info("Running Job parameter : {} ", parameter);
        logger.info("Running Job method : {} ", method);
        long startTime = System.currentTimeMillis();

        //todo
        if(org.apache.commons.lang.StringUtils.isBlank(api)){
            return;
        }

        if("GET".equals(method)){
            String url = api.concat(parameter);
            String result = restTemplate.getForObject(url, String.class);
            logger.info("execute GET result: {}",result);
        }

        if("POST".equals(method)){
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String jsonStr = parameter;
            HttpEntity<String> entity = new HttpEntity<>(jsonStr, headers);
            ResponseEntity<String> response = restTemplate.exchange(api, HttpMethod.POST, entity, String.class);
            logger.info("execute POST result: {}",response.toString());
        }

        long endTime = System.currentTimeMillis();
        logger.info(">>>>>>>>>>>>> Running Job has been completed , cost time :  " + (endTime - startTime) + "ms\n");
    }
}
