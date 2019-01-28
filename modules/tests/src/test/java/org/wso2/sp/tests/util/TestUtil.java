/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.sp.tests.util;

import io.netty.handler.codec.http.HttpMethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import static java.lang.System.currentTimeMillis;

/**
 * Util class for test cases.
 */
public class TestUtil {
    private static final String LINE_FEED = "\r\n";
    private static final String CHARSET = "UTF-8";
    private HttpURLConnection connection = null;
    private PrintWriter writer = null;
    private String boundary = null;
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(TestUtil.class);

    public TestUtil(URI baseURI, String path, Boolean auth, Boolean keepAlive, String methodType,
                    String contentType, String userName, String password) {
        try {
            URL url = baseURI.resolve(path).toURL();
            boundary = "---------------------------" + currentTimeMillis();

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept-Charset", CHARSET);
            connection.setRequestMethod(methodType);
            setHeader("HTTP_METHOD", methodType);
            if (keepAlive) {
                connection.setRequestProperty("Connection", "Keep-Alive");
            }
            if (contentType != null) {
                if (contentType.equals("multipart/form-data")) {
                    setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
                } else {
                    setHeader("Content-Type", contentType);
                }
            }
            connection.setUseCaches(false);
            connection.setDoInput(true);
            if (auth) {
                connection.setRequestProperty("Authorization",
                        "Basic " + java.util.Base64.getEncoder().
                                encodeToString((userName + ":" + password).getBytes()));
            }
            if (methodType.equals(HttpMethod.POST.name()) || methodType.equals(HttpMethod.PUT.name())
                    || methodType.equals(HttpMethod.DELETE.name())) {
                connection.setDoOutput(true);
                OutputStream outputStream = connection.getOutputStream();
                writer = new PrintWriter(new OutputStreamWriter(outputStream, CHARSET),
                        true);
            }
        } catch (IOException e) {
            handleException(e);
        }
    }

    public void addBodyContent(String body) {
        if (body != null && !body.isEmpty()) {
            writer.write(body);
            writer.close();
        }
    }

    public HTTPResponseMessage getResponse() {
        assert connection != null;
        String successContent = null;
        String errorContent = null;
        if (writer != null) {
            writer.append(LINE_FEED).flush();
            writer.append("--").append(boundary).append("--").append(LINE_FEED);
            writer.close();
        }
        try {
            if (connection.getResponseCode() >= 400) {
                errorContent = readErrorContent();
            } else {
                successContent = readSuccessContent();
            }
            return new HTTPResponseMessage(connection.getResponseCode(),
                    connection.getContentType(), connection.getResponseMessage(), successContent, errorContent);
        } catch (IOException e) {
            handleException(e);
        } finally {
            connection.disconnect();
        }
        return new HTTPResponseMessage();
    }

    private String readSuccessContent() throws IOException {
        StringBuilder sb = new StringBuilder("");
        String line;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                connection.getInputStream()))) {
            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private String readErrorContent() throws IOException {
        StringBuilder sb = new StringBuilder("");
        String line;
        InputStream errorStream = connection.getErrorStream();
        if (errorStream != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(errorStream))) {
                while ((line = in.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private void setHeader(String key, String value) {
        if (key != null && value != null) {
            connection.setRequestProperty(key, value);
        }
    }

    private void handleException(Exception ex) {
        logger.error("IOException occurred while running the Sample Test Case", ex);
    }

}
