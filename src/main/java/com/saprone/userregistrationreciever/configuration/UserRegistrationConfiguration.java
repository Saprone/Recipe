package com.saprone.userregistrationreciever.configuration;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.spring.cloud.service.servicebus.consumer.ServiceBusErrorHandler;
import com.azure.spring.cloud.service.servicebus.consumer.ServiceBusRecordMessageListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UserRegistrationConfiguration {
    @Bean
    public ServiceBusRecordMessageListener processMessage() {
        return context -> {
            ServiceBusReceivedMessage message = context.getMessage();
            String messageBody = message.getBody().toString();
            System.out.printf("Processing registration message. Id: %s; Contents: %s%n", message.getMessageId(), messageBody);
        };
    }

    @Bean
    public ServiceBusErrorHandler processError() {
        return context -> {
            System.out.printf("Error receiving messages: %s%n", context.getFullyQualifiedNamespace());
        };
    }
}
