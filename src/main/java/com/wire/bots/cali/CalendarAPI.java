package com.wire.bots.cali;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.DataStoreCredentialRefreshListener;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

class CalendarAPI {
    private static final String APPLICATION_NAME = "Wire Cali Bot";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static HttpTransport HTTP_TRANSPORT;
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);
    private static GoogleClientSecrets clientSecrets;
    private static ConcurrentHashMap<String, GoogleAuthorizationCodeFlow> flows = new ConcurrentHashMap<>();

    static {
        try (InputStream in = new FileInputStream(Service.CONFIG.getSecretPath())) {
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception t) {
            t.printStackTrace();
        }
    }

    static String getAuthUrl(String botId) throws IOException {
        GoogleAuthorizationCodeFlow flow = getFlow(botId);
        return flow.newAuthorizationUrl()
                .setRedirectUri("http://cali.35.187.84.91.xip.io/user/auth/google_oauth2/callback")
                .setState(botId)
                .build();
    }

    static Credential processAuthCode(String botId, String code) throws IOException {
        GoogleAuthorizationCodeFlow flow = getFlow(botId);
        GoogleTokenResponse response = flow.newTokenRequest(code)
                .setRedirectUri("http://cali.35.187.84.91.xip.io/user/auth/google_oauth2/callback")
                .execute();

        return flow.createAndStoreCredential(response, botId);
    }

    static Calendar getCalendarService(String botId) throws IOException {
        Credential credential = getFlow(botId).loadCredential(botId);
        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private static GoogleAuthorizationCodeFlow getFlow(String botId) throws IOException {
        GoogleAuthorizationCodeFlow flow = flows.get(botId);
        if (flow == null) {
            File dataDir = new File(Service.CONFIG.getCryptoDir(), "/.credentials/cali/" + botId);
            FileDataStoreFactory factory = new FileDataStoreFactory(dataDir);

            flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(factory)
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .addRefreshListener(new DataStoreCredentialRefreshListener(botId, factory))
                    .build();
            flows.put(botId, flow);
        }
        return flow;
    }
}
