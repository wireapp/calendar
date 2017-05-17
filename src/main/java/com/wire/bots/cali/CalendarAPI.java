package com.wire.bots.cali;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class CalendarAPI {
    private static final String APPLICATION_NAME = "Wire Cali Bot";
    private static final File DATA_STORE_DIR = new File(Service.CONFIG.getCryptoDir(), "/.credentials/cali");
    private static FileDataStoreFactory DATA_STORE_FACTORY;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static HttpTransport HTTP_TRANSPORT;
    private static final List<String> SCOPES = Arrays.asList(CalendarScopes.CALENDAR_READONLY);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static String getAuthUrl(String botId) throws IOException {
        // Load client secrets.
        try (InputStream in = new FileInputStream("/etc/cali/client_secret.json")) {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(DATA_STORE_FACTORY)
                    .setAccessType("offline")
                    .build();

            GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
            return url
                    .setRedirectUri(String.format("http://cali.35.187.84.91.xip.io/user/auth/google_oauth2/callback"))
                    .build();
        }
    }

    public static void processAuthCode(String botId, String code) throws IOException {
        // Load client secrets.
        try (InputStream in = new FileInputStream("/etc/cali/client_secret.json")) {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(DATA_STORE_FACTORY)
                    .setAccessType("offline")
                    .build();

            GoogleAuthorizationCodeTokenRequest req = flow.newTokenRequest(code);
            GoogleTokenResponse response = req
                    .setRedirectUri(String.format("http://cali.35.187.84.91.xip.io/user/auth/google_oauth2/callback"))
                    .execute();

            flow.createAndStoreCredential(response, "user");
        }
    }

    public static com.google.api.services.calendar.Calendar getCalendarService(String botId) throws IOException {
        // Load client secrets.
        try (InputStream in = new FileInputStream("/etc/cali/client_secret.json")) {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

            // Build flow and trigger user authorization request.
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setDataStoreFactory(DATA_STORE_FACTORY)
                    .setAccessType("offline")
                    .build();

            Credential credential = flow.loadCredential(botId);
            return new com.google.api.services.calendar.Calendar.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
    }
}
