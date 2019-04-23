/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.sample.apimevent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.databridge.agent.AgentHolder;
import org.wso2.carbon.databridge.agent.DataPublisher;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.utils.DataBridgeCommonsUtils;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * WSO2Event Client Publisher.
 */
public class Client {
    private static Log log = LogFactory.getLog(Client.class);
    private static final String REQUEST_STREAM = "org.wso2.apimgt.statistics.request";
    private static final String VERSION = "3.0.0";
    private static String agentConfigFileName = "data.agent.config.yaml";

    public static void main(String[] args) {

        DataPublisherUtil.setKeyStoreParams();
        DataPublisherUtil.setTrustStoreParams();

        log.info("These are the provided configurations: " + Arrays.deepToString(args));

        String protocol = args[0];
        String host = args[1];
        String port = args[2];
        int sslPort = Integer.parseInt(port) + 100;
        String username = args[3];
        String password = args[4];
        String numberOfEventsStr = args[5];
        String batchSizeStr = args[6];
        String batchTimeStr = args[7];

        long numberOfEvents = Long.parseLong(numberOfEventsStr);
        int batchSize = Integer.parseInt(batchSizeStr);
        int batchTime = Integer.parseInt(batchTimeStr);

        try {
            log.info("Starting APIM Event Client");
            AgentHolder.setConfigPath(DataPublisherUtil.getDataAgentConfigPath(agentConfigFileName));
            DataPublisher dataPublisher = new DataPublisher(protocol, "tcp://" + host + ":" + port,
                    "ssl://" + host + ":" + sslPort, username, password);
            Event requestEvent = new Event();
            requestEvent.setStreamId(DataBridgeCommonsUtils.generateStreamId(REQUEST_STREAM, VERSION));
            requestEvent.setCorrelationData(null);
            requestEvent.setMetaData(new Object[]{"mozilla"});

            long testStartTime = System.currentTimeMillis();
            long startTime, currentTime, totalCount = 0, batchCount;
            List<Event> arrayList = new ArrayList<Event>(batchSize);

            for (int i = 0; i < numberOfEvents; i += batchSize) {
                batchCount = 0;
                startTime = System.currentTimeMillis();
                for (int j = 0; j < batchSize; j++) {
                    requestEvent.setPayloadData(getRequestStream());
                    long eventCurrentTime = System.currentTimeMillis();
                    requestEvent.setTimeStamp(eventCurrentTime);
                    if (eventCurrentTime - startTime <= batchTime) {
                        batchCount++;
                        dataPublisher.publish(requestEvent);
                    }
                }
                totalCount += batchCount;
                System.out.println("Actual Batch Count : " + batchCount + " events");
                arrayList.clear();
                currentTime = System.currentTimeMillis();
                if (currentTime - startTime <= batchTime) {
                    try {
                        Thread.sleep(batchTime - (currentTime - startTime));
                    } catch (InterruptedException ex) {
                        log.error(ex);
                    }
                }
            }

            long testEndTime = System.currentTimeMillis();
            System.out.println("Total time to publish : " + (testEndTime - testStartTime) + " ms");
            System.out.println("Total events publish : " +  totalCount + " events");
            System.out.println("Total time to publish : " + ((totalCount * 1000 )/((testEndTime - testStartTime) * batchTime)) + " events/second");

            dataPublisher.shutdown();
            log.info("Events published successfully");

        } catch (Throwable e) {
            log.error(e);
        }
    }

    public static Object[] getRequestStream() {
        RequestDTO requestObj = new RequestDTO();
        String[] tenantDomain = {"carbon.super"};
        String[] apiName = {"ceylan"};
        String[] apiCreator = {"admin"};
        String[] apiMethod = {"GET"};
        String[] applicationId = {"1"};
        String[] applicationName = {"default"};
        String[] applicationCK = {"default"};
        String[] applicationOwner = {"Michael"};
        Boolean[] throttledOut = {false};
        long[] responseTime = {10, 60, 20};
        long[] backendTime = {10, 60, 20};


        int ip = ThreadLocalRandom.current().nextInt(0, 100);
        int index = ThreadLocalRandom.current().nextInt(0, 10);

        requestObj.applicationConsumerKey = applicationCK[0];
        requestObj.applicationName = applicationName[0];
        requestObj.applicationId = applicationId[0];
        requestObj.applicationOwner = applicationOwner[0];
        requestObj.apiContext = apiName[0] + "/" + "1.0";
        requestObj.apiName = apiName[0];
        requestObj.apiVersion = "1.0";
        requestObj.apiResourcePath = requestObj.apiName + "/" + apiMethod[0];
        requestObj.apiResourceTemplate = requestObj.apiName + "/" + apiMethod[0];
        requestObj.apiMethod = apiMethod[0];
        requestObj.apiCreator = apiCreator[0];
        requestObj.apiCreatorTenantDomain = tenantDomain[0];
        requestObj.apiTier = "unlimited";
        requestObj.apiHostname = "localhost";
        requestObj.username = String.valueOf(ip);
        requestObj.userTenantDomain = requestObj.apiCreatorTenantDomain;
        requestObj.userIp = String.valueOf(ip);
        requestObj.userAgent = "Mozilla";
        requestObj.requestTimestamp = new Timestamp(System.currentTimeMillis()).getTime();
        requestObj.throttledOut = throttledOut[0];
        requestObj.responseTime = responseTime[index % 3];
        requestObj.serviceTime = (long) 2;
        requestObj.backendTime = backendTime[index % 3];
        requestObj.responseCacheHit = false;
        requestObj.responseSize = (long) 2;
        requestObj.protocol = "Https";
        requestObj.responseCode = 200;
        requestObj.destination = "www.loc.com";
        requestObj.securityLatency = (long) 2;
        requestObj.throttlingLatency = (long) 2;
        requestObj.requestMedLat = (long) 2;
        requestObj.responseMedLat = (long) 2;
        requestObj.backendLatency = (long) 2;
        requestObj.otherLatency = (long) 2;
        requestObj.gatewayType = "SYNAPSE";
        requestObj.label = "SYNAPSE";

        return (new Object[]{
                requestObj.applicationConsumerKey,
                requestObj.applicationName,
                requestObj.applicationId,
                requestObj.applicationOwner,
                requestObj.apiContext,
                requestObj.apiName,
                requestObj.apiVersion,
                requestObj.apiResourcePath,
                requestObj.apiResourceTemplate,
                requestObj.apiMethod,
                requestObj.apiCreator,
                requestObj.apiCreatorTenantDomain,
                requestObj.apiTier,
                requestObj.apiHostname,
                requestObj.username,
                requestObj.userTenantDomain,
                requestObj.userIp,
                requestObj.userAgent,
                requestObj.requestTimestamp,
                requestObj.throttledOut,
                requestObj.responseTime,
                requestObj.serviceTime,
                requestObj.backendTime,
                requestObj.responseCacheHit,
                requestObj.responseSize,
                requestObj.protocol,
                requestObj.responseCode,
                requestObj.destination,
                requestObj.securityLatency,
                requestObj.throttlingLatency,
                requestObj.requestMedLat,
                requestObj.responseMedLat,
                requestObj.backendLatency,
                requestObj.otherLatency,
                requestObj.gatewayType,
                requestObj.label
        });
    }
}
