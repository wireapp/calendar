package com.wire.bots.cali;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.assets.Picture;
import com.wire.bots.sdk.models.AssetKey;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.tools.Logger;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

class CommandManager {
    private static final String PREVIEW_PIC_URL = "https://i.imgur.com/v9FQ8ba.png";
    private static final String COMMAND_LIST = "/list";
    private static final String COMMAND_POLLY = "/polly";
    private static final String COMMAND_AUTH = "/auth";
    private static final String COMMAND_CALI = "/cali";
    private static final String NO_EVENTS_SCHEDULED_SO_FAR = "No events scheduled so far.";

    private final CallScheduler callScheduler;

    CommandManager(ClientRepo repo) {
        this.callScheduler = new CallScheduler(Service.CONFIG.postgres, repo);
        try {
            callScheduler.loadSchedules();
        } catch (Exception e) {
            Logger.error("CallScheduler: %s", e);
        }
    }

    void processCommand(WireClient client, User owner, String command) throws Exception {
        command = command.toLowerCase().trim();

        if (command.equalsIgnoreCase(COMMAND_AUTH)) {
            showAuthLink(client, owner);
        } else if (command.startsWith(COMMAND_LIST)) {
            String args = command.replace(COMMAND_LIST, "").trim();
            int maxResults = parseInt(args, 5);
            Events events = CalendarAPI.listEvents(client.getId(), maxResults);
            if (events.getItems().isEmpty()) {
                client.sendText(NO_EVENTS_SCHEDULED_SO_FAR);
            } else {
                String msg = printEvents(events, "Here are your upcoming events:");
                client.sendText(msg);
            }
        } else if (command.equals("/today")) {
            Events events = listEventsToday(client.getId());
            if (events.getItems().isEmpty()) {
                client.sendText(NO_EVENTS_SCHEDULED_SO_FAR);
            } else {
                String msg = printEvents(events, "Today’s events:");
                client.sendText(msg);
            }
        } else if (command.equals("/tomorrow")) {
            Events events = listEventsTomorrow(client.getId());
            if (events.getItems().isEmpty()) {
                client.sendText(NO_EVENTS_SCHEDULED_SO_FAR);
            } else {
                String msg = printEvents(events,"Tomorrow’s events:");
                client.sendText(msg);
            }
        } else if (command.startsWith(COMMAND_POLLY)) {
            String args = command.replace(COMMAND_POLLY, "").trim();
            scheduleCall(client, args);
        } else if (command.startsWith(COMMAND_CALI)) {
            String args = command.replace(COMMAND_CALI, "").trim();
            scheduleNewEvent(client, args);
        } else if (command.equals("/mute")) {
            setMute(client, true);
        } else if (command.equals("/unmute")) {
            setMute(client, false);
        } else if (command.equals("/help")) {
            showHelp(client, owner);
        }
    }

    private void showHelp(WireClient client, User owner) throws Exception {
        String msg = "Here is how you can use me.\nFor a quick overview of your" +
                " forthcoming events, type: `/list`\n" +
                "If you want to see your day’s schedule\n" +
                "use: `/today` or `/tomorrow`\nThe command works only when it" +
                " starts a message and there is no space after the ”/“ character.";
        client.sendDirectText(msg, owner.id);
    }

    private void setMute(WireClient client, boolean muted) throws Exception {
        boolean setMuted = callScheduler.setMuted(client.getId(), muted);
        if (setMuted) {
            if (muted) {
                String msg = "Notifications about your events are now **off**.\n" +
                        "If you want them back, type: `/unmute`";
                client.sendText(msg);
            } else {
                String msg = "Notifications about your events are now **on**.";
                client.sendText(msg);
            }
        } else {
            Logger.warning("Failed to invoke setMute: %s", client.getId());
        }
    }

    void showAuthLink(WireClient client, User origin) throws Exception {
        try {
            String authUrl = CalendarAPI.getAuthUrl(client.getId());
            Picture preview = uploadPreview(client);
            client.sendDirectLinkPreview(authUrl, "Sign in - Google Accounts", preview, origin.id);
        } catch (Exception e) {
            Logger.error("showAuthLink: bot: %s error: %s", client.getId(), e);
            client.sendText("Something went wrong :(.");
        }
    }

    private Events listEventsToday(String botId) throws IOException {
        Date end = new Date(System.currentTimeMillis());
        end.setHours(23);
        end.setMinutes(59);
        end.setSeconds(59);
        return CalendarAPI.listEvents(botId, new DateTime(System.currentTimeMillis()), new DateTime(end));
    }

    private Events listEventsTomorrow(String botId) throws IOException {
        Date start = new Date(System.currentTimeMillis());
        start.setDate(start.getDate() + 1);
        start.setHours(0);
        start.setMinutes(0);
        start.setSeconds(0);

        Date end = new Date(System.currentTimeMillis());
        end.setDate(end.getDate() + 1);
        end.setHours(23);
        end.setMinutes(59);
        end.setSeconds(59);

        return CalendarAPI.listEvents(botId, new DateTime(start), new DateTime(end));
    }

    private int parseInt(String args, int defaultVal) {
        try {
            return Integer.parseInt(args);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private void scheduleNewEvent(WireClient client, String args) throws Exception {
        try {
            Event event = CalendarAPI.addEvent(client.getId(), args);
            if (event == null) {
                client.sendText("Sorry, I did not get that.");
                return;
            }

            DateFormat format = new SimpleDateFormat("EEEEE, dd MMMMM 'at' HH:mm");
            DateTime dateTime = event.getStart().getDateTime();
            long value = dateTime.getValue() + TimeUnit.MINUTES.toMillis(dateTime.getTimeZoneShift());
            String s = String.format("I've created new event for you:\n" +
                            "**%s** on %s\n%s",
                    event.getSummary(),
                    format.format(new Date(value)),
                    event.getHtmlLink());
            client.sendText(s);
        } catch (Exception e) {
            Logger.warning("scheduleNewEvent: %s", e.getMessage());
            client.sendText("Something went wrong :(.");
        }
    }

    private void scheduleCall(WireClient client, String text) throws Exception {
        String botId = client.getId();
        Date date = CallScheduler.parse(text);
        if (date != null) {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm', 'EEEE, MMMMM d, yyyy");
            format.setTimeZone(TimeZone.getTimeZone("CET"));

            boolean scheduled = callScheduler.schedule(botId, date);
            if (scheduled) {
                String schedule = date.toString();
                callScheduler.saveSchedule(botId, schedule);
                client.sendText("OK, I will start the call here at: " + format.format(date));
                Logger.info("Scheduled call for: `%s`, bot: %s", schedule, botId);
            } else {
                client.sendText("I am sorry, but I could not schedule the call for: " + format.format(date));
            }
        } else {
            client.sendText("I am sorry, I could not parse that.");
        }
    }

    private Picture uploadPreview(WireClient client) throws Exception {
        Picture preview = new Picture(PREVIEW_PIC_URL);
        preview.setPublic(true);

        AssetKey assetKey = client.uploadAsset(preview);
        preview.setAssetKey(assetKey.key);
        return preview;
    }

    private String printEvents(Events events, String title) {
        final DateFormat format = new SimpleDateFormat("EEEEE, dd MMMMM 'at' HH:mm");
        final StringBuilder sb = new StringBuilder(title);

        sb.append("\n");
        for (Event event : events.getItems()) {
            EventDateTime eventStart = event.getStart();
            DateTime start = eventStart.getDateTime();
            long value = start != null
                    ? start.getValue() + TimeUnit.MINUTES.toMillis(start.getTimeZoneShift())
                    : eventStart.getDate().getValue();
            sb.append(String.format("[%s](%s)\n%s\n", event.getSummary(), event.getHtmlLink(), format.format(new Date(value))));
        }
        return sb.toString();
    }
}
