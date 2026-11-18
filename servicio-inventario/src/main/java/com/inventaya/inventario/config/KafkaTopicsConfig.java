package com.inventaya.inventario.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declara los topicos con su numero de particiones definitivo. La creacion via KafkaAdmin
 * es idempotente, por lo que ambos servicios pueden declararlos sin importar quien arranque
 * primero; asi se evita que la creacion automatica de Kafka deje ventas-topic con 1 particion
 * y se pierda la garantia de orden por ventaId.
 */
@Configuration
@ConditionalOnProperty(name = "inventaya.kafka.crear-topicos", matchIfMissing = true)
public class KafkaTopicsConfig {

    @Bean
    public NewTopic ventasTopic(@Value("${inventaya.kafka.topics.ventas}") String nombre) {
        return TopicBuilder.name(nombre).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic alertasTopic(@Value("${inventaya.kafka.topics.alertas}") String nombre) {
        return TopicBuilder.name(nombre).partitions(1).replicas(1).build();
    }
}
