package com.example.batch;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@SpringBootApplication
@ImportRuntimeHints(BatchApplication.Hints.class)
public class BatchApplication {


    record Dog(int id, String name, String description) {
    }

    static final Resource IN = new ClassPathResource("/dogs.csv");

    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
            hints.resources().registerResource(IN);
        }
    }

    @Bean
    FlatFileItemReader<@NonNull Dog> dogsCsvReader() {
        return new FlatFileItemReaderBuilder<@NonNull Dog>()
                .delimited(d -> d.names("id,name,description".split(",")))
                .linesToSkip(1)
                .resource(IN)
                .name("dogsCsvReader")
                .fieldSetMapper(fieldSet -> new Dog(fieldSet.readInt("id"),
                        fieldSet.readString("name"), fieldSet.readString("description")))
                .build();
    }

    @Bean
        // todo test this once ive gotten the MongoDB stuff working.
    JdbcBatchItemWriter<@NonNull Dog> dogsJdbcItemWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<@NonNull Dog>()
                .sql("INSERT INTO dog (id, name, description) VALUES (?, ?, ?)")
                .dataSource(dataSource)
                .itemPreparedStatementSetter((item, ps) -> {
                    ps.setInt(1, item.id());
                    ps.setString(2, item.name());
                    ps.setString(3, item.description());
                })
                .build();
    }

    @Bean
    Step one(JobRepository repository, ItemWriter<@NonNull Dog> dogsWriter,
             FlatFileItemReader<@NonNull Dog> flatFileItemReader,
             PlatformTransactionManager transactionManager) {
        return new StepBuilder(repository)
                .<Dog, Dog>chunk(10)
                .transactionManager(transactionManager)
                .reader(flatFileItemReader)
                .writer(dogsWriter)
                .build();
    }


    @Bean
    Job job(JobRepository repository, Step one) {
        return new JobBuilder(repository)
                .start(one)
                .incrementer(new RunIdIncrementer())
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
