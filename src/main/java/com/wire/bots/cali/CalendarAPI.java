package com.wire.bots.cali;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.DataStoreCredentialRefreshListener;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import com.google.api.services.people.v1.PeopleService;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import org.ocpsoft.prettytime.nlp.parse.DateGroup;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalendarAPI {
    private static final String APPLICATION_NAME = "Wire Cali Bot";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String CALENDAR_ID = "primary";
    private static HttpTransport HTTP_TRANSPORT;
    private static final DataStoreFactory factory = new PostgresDataStoreFactory(Service.service.getJdbi());
    private static GoogleAuthorizationCodeFlow.Builder builder;

    private static final Pattern VALID_EMAIL_ADDRESS_REGEX =
            Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+", Pattern.CASE_INSENSITIVE);

    static {
        try (InputStream in = new FileInputStream(Service.CONFIG.secretPath)) {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            builder = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, CalendarScopes.all())
                    .setDataStoreFactory(factory)
                    .setAccessType("offline")
                    .setApprovalPrompt("force");
        } catch (Exception t) {
            t.printStackTrace();
        }
    }

    private static GoogleAuthorizationCodeFlow flow(String botId) throws IOException {
        return builder
                .addRefreshListener(new DataStoreCredentialRefreshListener(botId, factory))
                .build();
    }

    static String getAuthUrl(String botId) throws IOException {
        return flow(botId).newAuthorizationUrl()
                .setRedirectUri(Service.CONFIG.authRedirect)
                .setState(botId)
                .build();
    }

    public static Credential processAuthCode(String botId, String code) throws IOException {
        GoogleTokenResponse response = flow(botId)
                .newTokenRequest(code)
                .setRedirectUri(Service.CONFIG.authRedirect)
                .execute();

        return createAndStoreCredential(botId, response);
    }

    public static Credential createAndStoreCredential(String botId, GoogleTokenResponse response) throws IOException {
        return flow(botId).createAndStoreCredential(response, botId);
    }

    public static Credential loadCredential(String botId) throws IOException {
        return flow(botId).loadCredential(botId);
    }

    public static Calendar getCalendarService(String botId) throws IOException {
        Credential credential = loadCredential(botId);
        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static PeopleService getPeopleService(String botId) throws IOException {
        Credential credential = loadCredential(botId);
        return new PeopleService.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
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
                .insert(CALENDAR_ID, event)
                .setSendNotifications(true)
                .execute();

//        Logger.info("`%s` at `%s` recurrent: %s, event: %s",
//                dateGroup.getText(),
//                s.toString(),
//                dateGroup.isRecurring(),
//                event.getStart().getDateTime().toString());

        return event;
    }

    public static Channel watch(String botId) throws IOException {
        Channel channel = new Channel();
        channel.setId(botId);
        channel.setKind("api#channel");
        channel.setType("web_hook");
        channel.setAddress(Service.CONFIG.notificationsUrl);

        Calendar.Events.Watch watch = getCalendarService(botId)
                .events()
                .watch(CALENDAR_ID, channel);

        return watch.execute();
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
        Calendar service = getCalendarService(botId);
        return service
                .events()
                .get(CALENDAR_ID, eventId)
                .execute();
    }

    static Events listEvents(String botId, int maxResults) throws IOException {
        Calendar service = getCalendarService(botId);
        DateTime now = new DateTime(System.currentTimeMillis());
        return service.events().list("primary")
                .setMaxResults(maxResults)
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .setShowDeleted(false)
                .execute();
    }

    static Events listEvents(String botId, DateTime min, DateTime max) throws IOException {
        Calendar service = getCalendarService(botId);
        return service.events().list("primary")
                .setTimeMin(min)
                .setTimeMax(max)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .setShowDeleted(false)
                .execute();
    }

    private static int getTimeZoneShift(String botId) {
        try {
            Calendar service = getCalendarService(botId);
            Events events = service.events().list(CALENDAR_ID)
                    .setMaxResults(1)
                    .setTimeMin(new DateTime(System.currentTimeMillis()))
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            if (events.isEmpty())
                return 0;

            return events.getItems().get(0).getStart().getDateTime().getTimeZoneShift();
        } catch (Exception e) {
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

    public static Events getChanges(String botId, String syncToken) throws IOException {
        return getCalendarService(botId).events().list(CALENDAR_ID)
                .setSyncToken(syncToken).execute();
    }

    public static void stop(String botId, String channelId) throws IOException {
        Channel channel = new Channel();
        channel.setResourceId(channelId);
        getCalendarService(botId).channels().stop(channel).execute();
    }
}
