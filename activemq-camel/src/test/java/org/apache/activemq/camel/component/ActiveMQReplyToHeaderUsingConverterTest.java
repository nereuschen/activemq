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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Destination;

import static org.apache.activemq.camel.component.ActiveMQComponent.activeMQComponent;
import org.apache.activemq.camel.converter.ActiveMQConverter;
import org.apache.camel.CamelContext;
import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.AssertionClause;
import org.apache.camel.component.mock.MockEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class ActiveMQReplyToHeaderUsingConverterTest extends ContextTestSupport {
    private static final transient Logger LOG = LoggerFactory.getLogger(ActiveMQReplyToHeaderUsingConverterTest.class);
    protected Object expectedBody = "<time>" + new Date() + "</time>";
    protected String replyQueueName = "queue://test.my.reply.queue";
    protected String correlationID = "ABC-123";
    protected String groupID = "GROUP-XYZ";
    protected String messageType = getClass().getName();
    protected boolean useReplyToHeader = false;

    public void testSendingAMessageFromCamelSetsCustomJmsHeaders() throws Exception {
        MockEndpoint resultEndpoint = resolveMandatoryEndpoint("mock:result", MockEndpoint.class);

        resultEndpoint.expectedBodiesReceived(expectedBody);
        AssertionClause firstMessage = resultEndpoint.message(0);
        firstMessage.header("cheese").isEqualTo(123);
        firstMessage.header("JMSCorrelationID").isEqualTo(correlationID);
        if (useReplyToHeader) {
            firstMessage.header("JMSReplyTo").isEqualTo(ActiveMQConverter.toDestination(replyQueueName));
        }
        firstMessage.header("JMSType").isEqualTo(messageType);
        firstMessage.header("JMSXGroupID").isEqualTo(groupID);

        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("cheese", 123);
        if (useReplyToHeader) {
            headers.put("JMSReplyTo", replyQueueName);
        }
        headers.put("JMSCorrelationID", correlationID);
        headers.put("JMSType", messageType);
        headers.put("JMSXGroupID", groupID);
        template.sendBodyAndHeaders("activemq:test.a", expectedBody, headers);

        resultEndpoint.assertIsSatisfied();

        List<Exchange> list = resultEndpoint.getReceivedExchanges();
        Exchange exchange = list.get(0);
        Message in = exchange.getIn();
        Object replyTo = in.getHeader("JMSReplyTo");
        LOG.info("Reply to is: " + replyTo);
        if (useReplyToHeader) {
            Destination destination = assertIsInstanceOf(Destination.class, replyTo);
            assertEquals("ReplyTo", replyQueueName, destination.toString());
        }

        assertMessageHeader(in, "cheese", 123);
        assertMessageHeader(in, "JMSCorrelationID", correlationID);
        assertMessageHeader(in, "JMSType", messageType);
        assertMessageHeader(in, "JMSXGroupID", groupID);
    }

    protected CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = super.createCamelContext();

        // START SNIPPET: example
        camelContext.addComponent("activemq", activeMQComponent("vm://localhost?broker.persistent=false"));
        // END SNIPPET: example

        return camelContext;
    }

    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                from("activemq:test.a").to("activemq:test.b?preserveMessageQos=true");

                from("activemq:test.b").to("mock:result");
            }
        };
    }
}
