/**
 * Copyright 2016 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.motorcorp.messagehub.consumer;

import com.motorcorp.messagehub.consumer.config.EnvLoader;
import com.motorcorp.messagehub.consumer.config.KafkaConfig;
import com.motorcorp.messagehub.consumer.config.MessageHubProperties;
import com.motorcorp.messagehub.consumer.receiver.CustomerMessageReceiver;
import com.motorcorp.messagehub.consumer.receiver.CustomerVisitMessageReceiver;
import com.motorcorp.messagehub.consumer.receiver.MessageReceiver;
import com.motorcorp.messagehub.consumer.util.MessageHubConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Schedule;
import javax.ejb.Singleton;


@Singleton
public class ConsumerInstance {

    static Logger logger = Logger.getLogger(ConsumerInstance.class.getName());

    private KafkaConsumer<byte[], byte[]> consumer;

    private Map<String, MessageReceiver> receivers = new HashMap<>();

    private ArrayList<String> topics = new ArrayList<>();

    public ConsumerInstance() {
        topics.add("new-customer");
        topics.add("new-visit");

        try {
            initConsumer(topics);
        } catch (EnvLoader.MissingConfigurationException e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
    }

    protected void initConsumer(List<String> topics) throws EnvLoader.MissingConfigurationException {
        Properties properties = EnvLoader.load();

        String crmEndpoint = properties.getProperty("crm-endpoint");

        receivers.put("new-customer", new CustomerMessageReceiver(crmEndpoint));
        receivers.put("new-visit", new CustomerVisitMessageReceiver(crmEndpoint));


        KafkaConfig kafkaConfig = new KafkaConfig();
        kafkaConfig.loadConfig(properties);

        javax.security.auth.login.Configuration.setConfiguration(kafkaConfig);

        MessageHubProperties messageHubProperties = MessageHubProperties.getInstance(properties);

        consumer = MessageHubConsumer.getInstance(messageHubProperties);
        consumer.subscribe(topics);
    }

    @Schedule(hour = "*", minute = "*", second = "*/1", persistent = false)
    public void checkForIncomingMessagesEverySecond() {
        if (consumer == null) {
            try {
                initConsumer(topics);
            } catch (EnvLoader.MissingConfigurationException e) {
                // consumer could not initialize due to missing `APP_CONFIG` env variable.
                // we are not repeating the error message displayed in the constructor
            }
            return;
        }

        for (ConsumerRecord<byte[], byte[]> message : consumer.poll(1000)) {
            handleMessage(message.topic(), message.value());
        }
    }


    public void handleMessage(String topic, byte[] payload) {
        MessageReceiver receiver = receivers.get(topic);

        if (receiver == null) {
            logger.log(Level.WARNING, "No receiver found for topic=" + topic);
            return;
        }

        receiver.receiveMessage(payload);
    }

}