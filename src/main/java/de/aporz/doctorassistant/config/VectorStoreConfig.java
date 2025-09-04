package de.aporz.doctorassistant.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableConfigurationProperties({VectorStoreProperties.class, VectorTopKProperties.class})
public class VectorStoreConfig {

    @Bean
    @Qualifier("medicalVectorStore")
    public VectorStore medicalVectorStore(JdbcTemplate jdbcTemplate,
                                          EmbeddingModel embeddingModel,
                                          VectorStoreProperties props) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(props.getMedical().getTableName())
                .schemaName("public")
                .dimensions(props.getMedical().getDimension())
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .initializeSchema(true)
                .build();
    }

    @Bean
    @Qualifier("patientVectorStore")
    public VectorStore patientVectorStore(JdbcTemplate jdbcTemplate,
                                          EmbeddingModel embeddingModel,
                                          VectorStoreProperties props) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(props.getPatient().getTableName())
                .schemaName("public")
                .dimensions(props.getPatient().getDimension())
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .initializeSchema(true)
                .build();
    }
}
