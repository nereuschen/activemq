/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.camel.component;

import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.ContextTestSupport;
import org.apache.camel.Endpoint;
import org.apache.camel.component.jms.JmsConsumer;
import org.apache.camel.component.jms.JmsEndpoint;
import org.apache.camel.component.jms.JmsProducer;
import org.apache.camel.processor.CamelLogger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.connection.SingleConnectionFactory;

/**
 * 
 */
public class ActiveMQConfigureTest extends ContextTestSupport {
    
    public void testJmsTemplateUsesPoolingConnectionFactory() throws Exception {
        JmsEndpoint endpoint = resolveMandatoryEndpoint("activemq:test.foo");
        JmsProducer producer = (JmsProducer) endpoint.createProducer();

        JmsTemplate template = assertIsInstanceOf(JmsTemplate.class, producer.getInOutTemplate());
        assertEquals("pubSubDomain", false, template.isPubSubDomain());
        assertIsInstanceOf(PooledConnectionFactory.class, template.getConnectionFactory());
    }

    public void testJmsTemplateUsesSingleConnectionFactory() throws Exception {
        JmsEndpoint endpoint = resolveMandatoryEndpoint("activemq:test.foo?useSingleConnection=true");
        JmsProducer producer = (JmsProducer) endpoint.createProducer();

        JmsTemplate template = assertIsInstanceOf(JmsTemplate.class, producer.getInOutTemplate());
        assertEquals("pubSubDomain", false, template.isPubSubDomain());
        SingleConnectionFactory connectionFactory = assertIsInstanceOf(SingleConnectionFactory.class, template.getConnectionFactory());
        assertIsInstanceOf(ActiveMQConnectionFactory.class, connectionFactory.getTargetConnectionFactory());
    }

    public void testJmsTemplateDoesNotUsePoolingConnectionFactory() throws Exception {
        JmsEndpoint endpoint = resolveMandatoryEndpoint("activemq:test.foo?usePooledConnection=false");
        JmsProducer producer = (JmsProducer) endpoint.createProducer();

        JmsTemplate template = assertIsInstanceOf(JmsTemplate.class, producer.getInOutTemplate());
        assertEquals("pubSubDomain", false, template.isPubSubDomain());
        assertIsInstanceOf(ActiveMQConnectionFactory.class, template.getConnectionFactory());
    }

    public void testListenerContainerUsesSpringConnectionFactory() throws Exception {
        JmsEndpoint endpoint = resolveMandatoryEndpoint("activemq:topic:test.foo");
        JmsConsumer consumer = endpoint.createConsumer(new CamelLogger());

        AbstractMessageListenerContainer listenerContainer = consumer.getListenerContainer();
        assertEquals("pubSubDomain", true, listenerContainer.isPubSubDomain());
        assertIsInstanceOf(PooledConnectionFactory.class, listenerContainer.getConnectionFactory());
    }

    @Override
    protected JmsEndpoint resolveMandatoryEndpoint(String uri) {
        Endpoint endpoint = super.resolveMandatoryEndpoint(uri);
        return assertIsInstanceOf(JmsEndpoint.class, endpoint);
    }
}