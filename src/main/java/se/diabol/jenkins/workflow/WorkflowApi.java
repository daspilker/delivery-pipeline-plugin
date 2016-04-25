/*
This file is part of Delivery Pipeline Plugin.

Delivery Pipeline Plugin is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Delivery Pipeline Plugin is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Delivery Pipeline Plugin.
If not, see <http://www.gnu.org/licenses/>.
*/
package se.diabol.jenkins.workflow;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.logging.Logger;

public class WorkflowApi {

    private static final Logger LOG = Logger.getLogger(WorkflowApi.class.getName());
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    private Jenkins jenkins;

    public WorkflowApi(final Jenkins instance) {
        this.jenkins = instance;
    }

    public void lastRunFor(String job) {
        try {
            HttpRequest request = requestFor(workflowApiUrl(job) + "runs");
            LOG.info("Getting workflow runs for " + job + " from Workflow API: " + request.getUrl());
            HttpResponse response = request.execute();
            LOG.info("Received workflow runs for " + job + ": " + response.parseAsString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static HttpRequest requestFor(String url) throws IOException {
        HttpRequest request = requestFactory().buildGetRequest(new GenericUrl(url));
        request.setConnectTimeout(WorkflowPipelineView.DEFAULT_INTERVAL - 250);
        request.setReadTimeout(WorkflowPipelineView.DEFAULT_INTERVAL - 250);
        return request;
    }

    public static HttpRequestFactory requestFactory() {
        return HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) {
                request.setParser(new JsonObjectParser(JSON_FACTORY));
            }
        });
    }

    private String workflowApiUrl(String jobName) {
        return jenkinsUrl() + "job/" + jobName + "/wfapi/";
    }

    private String jenkinsUrl() {
        return jenkins.getRootUrl();
    }
}
