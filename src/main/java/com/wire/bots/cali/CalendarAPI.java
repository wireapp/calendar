package com.wire.bots.cali;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.DataStoreCredentialRefreshListener;
import com.google.api.client.googleapis.auth.oauth2.*;
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

class CalendarAPI {
    private static final String APPLICATION_NAME = "Wire Cali Bot";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static HttpTransport HTTP_TRANSPORT;
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);
    private static GoogleAuthorizationCodeFlow flow;
    private static File dataDir = new File(Service.CONFIG.getCryptoDir(), "/.credentials/cali");
    private static FileDataStoreFactory factory;

    static {
        try (InputStream in = new FileInputStream("/etc/cali/client_secret.json")) {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
            factory = new FileDataStoreFactory(dataDir);

            // Load client secrets.
            flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(factory)
                    .setAccessType("offline")
                    .build();
        } catch (Exception t) {
            t.printStackTrace();
        }
    }

    static String getAuthUrl(String botId) throws IOException {
        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
        return url
                .setRedirectUri("http://cali.35.187.84.91.xip.io/user/auth/google_oauth2/callback")
                .setState(botId)
                .build();
    }

    static void processAuthCode(String botId, String code) throws IOException {
        GoogleAuthorizationCodeTokenRequest req = flow.newTokenRequest(code);
        GoogleTokenResponse response = req
                .setRedirectUri("http://cali.35.187.84.91.xip.io/user/auth/google_oauth2/callback")
                .execute();
        flow.createAndStoreCredential(response, botId);

        renewToken(botId);
    }

    static Calendar getCalendarService(String botId) throws IOException {
        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredential(botId))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    static Credential getCredential(String botId) throws IOException {
        return flow.loadCredential(botId);
    }

    static void renewToken(String botId) throws IOException {
        GoogleCredential.Builder b = new GoogleCredential.Builder();
        DataStoreCredentialRefreshListener refreshListener = new DataStoreCredentialRefreshListener(botId, factory);
        b.addRefreshListener(refreshListener);
    }
}
