package dev.sentinel.batch;

import dev.sentinel.config.SentinelProperties;
import dev.sentinel.detector.AnomalyStore;
import dev.sentinel.detector.Detector;
import dev.sentinel.detector.LockContentionDetector;
import dev.sentinel.model.Anomaly;
import dev.sentinel.model.SqlSample;
import dev.sentinel.repository.AwrRepository;
import dev.sentinel.repository.VSqlRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SentinelBatchConfig {

    @Bean
    public Tasklet sqlSamplingTasklet(SentinelProperties props,
                                      VSqlRepository vSql,
                                      AwrRepository awr,
                                      List<Detector> detectors,
                                      LockContentionDetector lockDetector,
                                      AnomalyStore store) {
        return (contribution, chunkContext) -> {
            List<SqlSample> samples = new ArrayList<>(vSql.recentSamples(props.schemas()));
            samples.addAll(awr.latestSnapshot());
            List<Anomaly> all = new ArrayList<>();
            for (Detector d : detectors) all.addAll(d.detect(samples));
            all.addAll(lockDetector.detect());
            store.addAll(all);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step sentinelStep(JobRepository jobRepository,
                             PlatformTransactionManager tx,
                             Tasklet sqlSamplingTasklet) {
        return new StepBuilder("sentinelStep", jobRepository)
                .tasklet(sqlSamplingTasklet, tx)
                .build();
    }

    @Bean
    public Job sentinelJob(JobRepository jobRepository, Step sentinelStep) {
        return new JobBuilder("sentinelJob", jobRepository)
                .start(sentinelStep)
                .build();
    }
}
