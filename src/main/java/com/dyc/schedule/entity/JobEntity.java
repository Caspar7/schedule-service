package com.dyc.schedule.entity;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 示例,可自定义相关属性
 */
@Entity
@Table(name = "JOB_ENTITY")
public class JobEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String jobName;          //job名称
    private String jobGroup;         //job组名
    private String cron;          //执行的cron
    private String parameter;     //job的参数
    private String description;   //job描述信息
    private String method;       //vm参数
    private String api;       //job的jar路径
    private String status;        //job的执行状态

    public JobEntity() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }



    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "JobEntity{" +
                "id=" + id +
                ", jobName='" + jobName + '\'' +
                ", jobGroup='" + jobGroup + '\'' +
                ", cron='" + cron + '\'' +
                ", parameter='" + parameter + '\'' +
                ", description='" + description + '\'' +
                ", method='" + method + '\'' +
                ", api='" + api + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    //新增Builder模式,可选,选择设置任意属性初始化对象
    public JobEntity(Builder builder) {
        id = builder.id;
        jobName = builder.jobName;
        jobGroup = builder.jobGroup;
        cron = builder.cron;
        parameter = builder.parameter;
        description = builder.description;
        method = builder.method;
        api = builder.api;
        status = builder.status;
    }

    public static class Builder {
        private Integer id;
        private String jobName = "";          //job名称
        private String jobGroup = "";         //job组名
        private String cron = "";          //执行的cron
        private String parameter = "";     //job的参数 或 postbody
        private String description = "";   //job描述信息
        private String method = "";       //api方法 GET POST
        private String api = "";       //api url
        private String status = "";        //job的执行状态,只有该值为OPEN才会执行该Job

        public Builder withId(Integer i) {
            id = i;
            return this;
        }

        public Builder withName(String n) {
            jobName = n;
            return this;
        }

        public Builder withGroup(String g) {
            jobGroup = g;
            return this;
        }

        public Builder withCron(String c) {
            cron = c;
            return this;
        }

        public Builder withParameter(String p) {
            parameter = p;
            return this;
        }

        public Builder withDescription(String d) {
            description = d;
            return this;
        }

        public Builder withMethod(String method) {
            method = method;
            return this;
        }

        public Builder withApi(String api) {
            api = api;
            return this;
        }

        public Builder withStatus(String s) {
            status = s;
            return this;
        }

        public JobEntity newJobEntity() {
            return new JobEntity(this);
        }
    }

}
