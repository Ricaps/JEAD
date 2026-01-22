/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.antipatterns.oauth2.env;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Adds value for mockwebserver.url property.
 */
public class MockWebServerPropertySource extends PropertySource<MockWebServer> implements DisposableBean {

	private static final MockResponse JWKS_RESPONSE = response(
			"{ \"keys\": [ { \"kty\": \"RSA\", \"e\": \"AQAB\", \"n\": \"jvBtqsGCOmnYzwe_-HvgOqlKk6HPiLEzS6uCCcnVkFXrhnkPMZ-uQXTR0u-7ZklF0XC7-AMW8FQDOJS1T7IyJpCyeU4lS8RIf_Z8RX51gPGnQWkRvNw61RfiSuSA45LR5NrFTAAGoXUca_lZnbqnl0td-6hBDVeHYkkpAsSck1NPhlcsn-Pvc2Vleui_Iy1U2mzZCM1Vx6Dy7x9IeP_rTNtDhULDMFbB_JYs-Dg6Zd5Ounb3mP57tBGhLYN7zJkN1AAaBYkElsc4GUsGsUWKqgteQSXZorpf6HdSJsQMZBDd7xG8zDDJ28hGjJSgWBndRGSzQEYU09Xbtzk-8khPuw\" } ] }",
			200);

	private static final MockResponse NOT_FOUND_RESPONSE = response(
			"{ \"message\" : \"This mock authorization server responds to just one request: GET /.well-known/jwks.json.\" }",
			404);

	/**
	 * Name of the random {@link PropertySource}.
	 */
	public static final String MOCK_WEB_SERVER_PROPERTY_SOURCE_NAME = "mockwebserver";

	private boolean started;

	public MockWebServerPropertySource() {
		super(MOCK_WEB_SERVER_PROPERTY_SOURCE_NAME, new MockWebServer());
	}

	@Override
	public Object getProperty(String name) {
		if (!name.equals("mockwebserver.url")) {
			return null;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Looking up the url for '" + name + "'");
		}
		return getUrl();
	}

	@Override
	public void destroy() throws Exception {
		getSource().shutdown();
	}

	/**
	 * Get's the URL (i.e. "http://localhost:123456")
	 * @return the url with the dynamic port
	 */
	private String getUrl() {
		MockWebServer mockWebServer = getSource();
		if (!this.started) {
			initializeMockWebServer(mockWebServer);
		}
		String url = mockWebServer.url("").url().toExternalForm();
		return url.substring(0, url.length() - 1);
	}

	private void initializeMockWebServer(MockWebServer mockWebServer) {
		Dispatcher dispatcher = new Dispatcher() {
			@NotNull
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				if ("/.well-known/jwks.json".equals(request.getPath())) {
					return JWKS_RESPONSE;
				}

				return NOT_FOUND_RESPONSE;
			}
		};

		mockWebServer.setDispatcher(dispatcher);
		try {
			mockWebServer.start();
			this.started = true;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Could not start " + mockWebServer, ex);
		}
	}

	private static MockResponse response(String body, int status) {
		return new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.setResponseCode(status).setBody(body);
	}

}
