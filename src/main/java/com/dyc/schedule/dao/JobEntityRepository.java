package com.dyc.schedule.dao;

import com.dyc.schedule.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import javax.transaction.Transactional;


public interface JobEntityRepository extends JpaRepository<JobEntity, Integer>, CrudRepository<JobEntity, Integer>,
        JpaSpecificationExecutor {


    @Transactional
    @Modifying
    @Query("delete from JobEntity j where j.jobGroup = :jobGroup and j.jobName= :jobName")
    void deleteByJobNameAndJobGroup(@Param("jobGroup") String jobGroup, @Param("jobName") String jobName);

    @Query("Select j from JobEntity j where j.jobGroup = :jobGroup and j.jobName= :jobName")
    JobEntity findByJobNameAndJobGroup(@Param("jobGroup") String jobGroup, @Param("jobName") String jobName);
}
