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
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.wire.bots.sdk.Logger;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import org.ocpsoft.prettytime.nlp.parse.DateGroup;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CalendarAPI {
    private static final String APPLICATION_NAME = "Wire Cali Bot";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static HttpTransport HTTP_TRANSPORT;
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static GoogleClientSecrets clientSecrets;
    private static ConcurrentHashMap<String, GoogleAuthorizationCodeFlow> flows = new ConcurrentHashMap<>();
    private static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+", Pattern.CASE_INSENSITIVE);

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

    static Event addEvent(String botId, String line) throws IOException {
        List<DateGroup> dateGroups = new PrettyTimeParser().parseSyntax(line);
        if (dateGroups.isEmpty())
            return null;

        int tzShift = getTimeZoneShift(botId);

        DateGroup dateGroup = dateGroups.get(0);
        Date s = dateGroup.getDates().get(0);

        DateTime startDateTime = new DateTime(s.getTime() - TimeUnit.MINUTES.toMillis(tzShift));
        EventDateTime startEvent = new EventDateTime()
                .setDateTime(startDateTime);

        Date e = new Date(startDateTime.getValue() + TimeUnit.HOURS.toMillis(1));
        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(e));

        List<EventAttendee> attendees = extractAttendees(line);

        String summary = extractSummary(line, dateGroup.getText(), attendees);

        Event event = new Event()
                .setSummary(summary.trim())
                .setStart(startEvent)
                .setEnd(end)
                .setAttendees(attendees);

        event = getCalendarService(botId)
                .events()
                .insert("primary", event)
                .execute();

        Logger.info("`%s` at `%s` recurrent: %s, event: %s",
                dateGroup.getText(),
                s.toString(),
                dateGroup.isRecurring(),
                event.getStart().getDateTime().toString());

        return event;
    }

    private static String extractSummary(String line, String dates, List<EventAttendee> attendees) {
        String ret = line.replace(dates, "");
        for (EventAttendee attendee : attendees) {
            ret = ret.replace(attendee.getEmail(), "");
        }
        ret = ret.replace("with", "");
        return ret.trim();
    }

    private static List<EventAttendee> extractAttendees(String summary) {
        ArrayList<EventAttendee> ret = new ArrayList<>();

        ArrayList<String> emails = extractEmail(summary);
        for (String email : emails) {
            EventAttendee eventAttendee = new EventAttendee()
                    .setEmail(email);
            ret.add(eventAttendee);
        }
        return ret;
    }

    static Event getEvent(String botId, String eventId) throws IOException {
        Calendar service = CalendarAPI.getCalendarService(botId);
        return service
                .events()
                .get("primary", eventId)
                .execute();
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

    private static int getTimeZoneShift(String botId) {
        try {
            Calendar service = getCalendarService(botId);
            Events events = service.events().list("primary")
                    .setMaxResults(1)
                    .setTimeMin(new DateTime(System.currentTimeMillis()))
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            if (events.isEmpty())
                return 0;

            return events.getItems().get(0).getStart().getDateTime().getTimeZoneShift();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    static ArrayList<String> extractEmail(String emailStr) {
        ArrayList<String> ret = new ArrayList<>();
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(emailStr);
        while (matcher.find()) {
            ret.add(matcher.group());
        }
        return ret;
    }
}
