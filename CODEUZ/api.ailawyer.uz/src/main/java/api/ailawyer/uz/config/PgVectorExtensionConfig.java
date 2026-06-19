package api.ailawyer.uz.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * pgvector extension Hibernate schema generation dan OLDIN yaratiladi.
 * {@code schema.sql} bilan birga ishlaydi; xatolik bo'lsa ilova ishga tushishda to'xtamaydi.
 */
@Configuration
@AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
@Slf4j
public class PgVectorExtensionConfig {

    private static final String CREATE_VECTOR_EXTENSION = "CREATE EXTENSION IF NOT EXISTS vector";
    public static final String PGVECTOR_INITIALIZER_BEAN = "pgVectorExtensionInitializer";

    @Bean(name = PGVECTOR_INITIALIZER_BEAN)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public Object pgVectorExtensionInitializer(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(CREATE_VECTOR_EXTENSION);
            log.info("PostgreSQL pgvector extension tayyor");
        } catch (Exception e) {
            log.warn("pgvector extension yaratib bo'lmadi (PostgreSQL da pgvector o'rnatilmagan bo'lishi mumkin): {}",
                    e.getMessage());
        }
        return new Object();
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static BeanFactoryPostProcessor entityManagerFactoryDependsOnPgVector() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                    return;
                }
                BeanDefinition emf = beanFactory.getBeanDefinition("entityManagerFactory");
                List<String> dependsOn = new ArrayList<>();
                if (emf.getDependsOn() != null) {
                    dependsOn.addAll(Arrays.asList(emf.getDependsOn()));
                }
                if (!dependsOn.contains(PGVECTOR_INITIALIZER_BEAN)) {
                    dependsOn.add(PGVECTOR_INITIALIZER_BEAN);
                }
                emf.setDependsOn(dependsOn.toArray(String[]::new));
            }
        };
    }
}
