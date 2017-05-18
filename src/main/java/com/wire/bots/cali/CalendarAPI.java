package com.wire.bots.cali;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class CalendarAPI {
    private static final String APPLICATION_NAME = "Wire Cali Bot";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static HttpTransport HTTP_TRANSPORT;
    private static final List<String> SCOPES = Arrays.asList(CalendarScopes.CALENDAR_READONLY);
    private static GoogleAuthorizationCodeFlow flow;

    static {
        try (InputStream in = new FileInputStream("/etc/cali/client_secret.json")) {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            File dataDir = new File(Service.CONFIG.getCryptoDir(), "/.credentials/cali");
            FileDataStoreFactory factory = new FileDataStoreFactory(dataDir);
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

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

    public static String getAuthUrl(String botId) throws IOException {
        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl();
        return url
                .setRedirectUri(String.format("http://cali.35.187.84.91.xip.io/user/auth/google_oauth2/callback"))
                .setState(botId)
                .build();
    }

    public static void processAuthCode(String botId, String code) throws IOException {
        GoogleAuthorizationCodeTokenRequest req = flow.newTokenRequest(code);
        GoogleTokenResponse response = req
                .setRedirectUri(String.format("http://cali.35.187.84.91.xip.io/user/auth/google_oauth2/callback"))
                .execute();
        flow.createAndStoreCredential(response, botId);
    }

    public static Calendar getCalendarService(String botId) throws IOException {
        Credential credential = flow.loadCredential(botId);
        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
