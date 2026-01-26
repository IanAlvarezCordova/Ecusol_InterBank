package com.ecusol.ms_transacciones.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    public static final String EXCHANGE_ECUSOL = "ecusol.exchange";
    public static final String EXCHANGE_DLQ = "ecusol.exchange.dlq";
    public static final String ROUTING_KEY_RETURNS = "ecusol.returns.in";
    public static final String ROUTING_KEY_DLQ = "ecusol.returns.dlq";

    @Value("${banco.cola.entrada:queue-banco-ecusol}")
    private String colaEntrada;

    @Bean
    public Queue colaEcusol() {
        return QueueBuilder
            .durable(colaEntrada)
            .withArgument("x-dead-letter-exchange", EXCHANGE_DLQ)
            .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
            .withArgument("x-max-length", 10000) 
            .build();
    }

    @Bean
    public Queue dlqEcusol() {
        return QueueBuilder
            .durable(colaEntrada + ".dlq")
            .build();
    }

    @Bean
    public DirectExchange exchangeEcusol() {
        return new DirectExchange(EXCHANGE_ECUSOL, true, false);
    }

    @Bean
    public DirectExchange exchangeDlq() {
        return new DirectExchange(EXCHANGE_DLQ, true, false);
    }

    @Bean
    public Binding bindingEcusol(Queue colaEcusol, DirectExchange exchangeEcusol) {
        return BindingBuilder
            .bind(colaEcusol)
            .to(exchangeEcusol)
            .with(ROUTING_KEY_RETURNS);
    }

    @Bean
    public Binding bindingDlq(Queue dlqEcusol, DirectExchange exchangeDlq) {
        return BindingBuilder
            .bind(dlqEcusol)
            .to(exchangeDlq)
            .with(ROUTING_KEY_DLQ);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}