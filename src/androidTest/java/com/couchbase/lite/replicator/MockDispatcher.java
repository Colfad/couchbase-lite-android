package com.couchbase.lite.replicator;

import com.squareup.okhttp.mockwebserver.Dispatcher;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.RecordedRequest;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class MockDispatcher extends Dispatcher {

    // Map where the key is a path regex, (eg, "/_changes/*), and
    // the value is a Queue of MockResponse objects
    private Map<String, BlockingQueue<MockResponse>> queueMap;

    public MockDispatcher() {
        super();
        queueMap = new HashMap<String, BlockingQueue<MockResponse>>();
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        System.out.println(String.format("Request: %s", request));
        for(String pathRegex: queueMap.keySet()){
            if (regexMatches(pathRegex, request.getPath())) {
                BlockingQueue<MockResponse> responseQueue = queueMap.get(pathRegex);
                if (responseQueue == null) {
                    String msg = String.format("No queue found for pathRegex: %s", pathRegex);
                    throw new RuntimeException(msg);
                }
                MockResponse take = responseQueue.take();
                return take;
            }
        }
        return new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND); // fail fast
    }

    public void enqueueResponse(String pathRegex, MockResponse response) {
        BlockingQueue<MockResponse> responseQueue = queueMap.get(pathRegex);
        if (responseQueue == null) {
            responseQueue = new LinkedBlockingDeque<MockResponse>();
            queueMap.put(pathRegex, responseQueue);
        }
        responseQueue.add(response);
    }

    private boolean regexMatches(String pathRegex, String actualPath) {
        try {
            return actualPath.matches(pathRegex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


}
