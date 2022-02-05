package com.wire.bots.cali;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.wire.xenon.WireClient;
import com.wire.xenon.assets.LinkPreview;
import com.wire.xenon.assets.MessageText;
import com.wire.xenon.assets.Picture;
import com.wire.xenon.backend.models.User;
import com.wire.xenon.models.AssetKey;
import com.wire.xenon.tools.Logger;
import org.jdbi.v3.core.Jdbi;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.wire.xenon.tools.Util.getResource;

class CommandManager {
    private static final String COMMAND_LIST = "/list";
    private static final String COMMAND_POLLY = "/polly";
    private static final String COMMAND_CALI = "/cali";
    private static final String NO_EVENTS_SCHEDULED_SO_FAR = "No events scheduled so far.";
    private static final String COMMAND_TODAY = "/today";
    private static final String COMMAND_TOMORROW = "/tomorrow";
    private static final String COMMAND_MUTE = "/mute";
    private static final String COMMAND_UNMUTE = "/unmute";
    private static final String COMMAND_HELP = "/help";
    public static final String REMINDME = "/remindme";

    private final CallScheduler callScheduler;

    CommandManager(Jdbi jdbi) {
        this.callScheduler = new CallScheduler(jdbi);
        try {
            callScheduler.loadSchedules();
        } catch (Exception e) {
            Logger.error("CallScheduler: %s", e);
        }
    }

    void processCommand(WireClient client, UUID sender, String command) throws Exception {
        command = command.toLowerCase().trim();

        if (command.startsWith(COMMAND_LIST)) {
            String args = command.replace(COMMAND_LIST, "").trim();
            int maxResults = parseInt(args, 5);
            Events events = CalendarAPI.listEvents(client.getId().toString(), maxResults);
            if (events.getItems().isEmpty()) {
                client.send(new MessageText(NO_EVENTS_SCHEDULED_SO_FAR), sender);
            } else {
                String msg = printEvents(events, "Here are your upcoming events:");
                client.send(new MessageText(msg), sender);
            }
        } else if (command.equals(COMMAND_TODAY)) {
            Events events = listEventsToday(client.getId().toString());
            if (events.getItems().isEmpty()) {
                client.send(new MessageText(NO_EVENTS_SCHEDULED_SO_FAR), sender);
            } else {
                String msg = printEvents(events, "Today’s events:");
                client.send(new MessageText(msg), sender);
            }
        } else if (command.equals(COMMAND_TOMORROW)) {
            Events events = listEventsTomorrow(client.getId().toString());
            if (events.getItems().isEmpty()) {
                client.send(new MessageText(NO_EVENTS_SCHEDULED_SO_FAR), sender);
            } else {
                String msg = printEvents(events, "Tomorrow’s events:");
                client.send(new MessageText(msg), sender);
            }
        } else if (command.startsWith(COMMAND_POLLY)) {
            String args = command.replace(COMMAND_POLLY, "").trim();
            scheduleCall(client, args);
        } else if (command.startsWith(REMINDME)) {
            String args = command.replace(REMINDME, "").trim();
            scheduleReminder(client, args, sender);
        } else if (command.startsWith(COMMAND_CALI)) {
            String args = command.replace(COMMAND_CALI, "").trim();
            scheduleNewEvent(client, args);
        } else if (command.equals(COMMAND_MUTE)) {
            setMute(client, true);
        } else if (command.equals(COMMAND_UNMUTE)) {
            setMute(client, false);
        } else if (command.equals(COMMAND_HELP)) {
            showHelp(client, sender);
        }
    }

    private void showHelp(WireClient client, UUID sender) throws Exception {
        String msg = "Here's the list of my controls:\n" +
                "\n" +
                "For a quick overview of your upcoming events, type: \n" +
                "`/list` or `/list 10`\n" +
                "—\n" +
                "For your daily schedule use: \n" +
                "`/today` or `/tomorrow`\n" +
                "—\n" +
                "To setup a reminder: \n" +
                "`/remindme Go to bed at 10pm`\n" +
                "—\n" +
                "To initiate a call at specific time: \n" +
                "`/polly tomorrow at 9`\n" +
                "—\n" +
                "To create a new Google event: \n" +
                "`/cali Board meeting with alan@wire.com morten@wire.com on Monday 2pm`\n" +
                "—\n" +
                "You can turn on/off my event notifications with: \n" +
                "`/mute` and `/unmute`";

        client.send(new MessageText(msg), sender);
    }

    private void setMute(WireClient client, boolean muted) throws Exception {
        boolean setMuted = callScheduler.setMuted(client.getId(), muted);
        if (setMuted) {
            if (muted) {
                String msg = "Notifications about your events are now **off**. \n" +
                        "If you want to turn them back on, type: `/unmute` .";
                client.send(new MessageText(msg));
            } else {
                String msg = "Notifications about your events are now **on**.";
                client.send(new MessageText(msg));
            }
        } else {
            Logger.warning("Failed to invoke setMute: %s", client.getId());
        }
    }

    void showAuthLink(WireClient client, User origin) throws Exception {
        try {
            String authUrl = CalendarAPI.getAuthUrl(client.getId().toString());
            LinkPreview linkPreview = new LinkPreview(authUrl, "Sign in - Google Accounts", uploadPreview(client));
            client.send(linkPreview);
        } catch (Exception e) {
            Logger.error("showAuthLink: bot: %s error: %s", client.getId(), e);
            client.send(new MessageText("Something went wrong :(."));
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
            Event event = CalendarAPI.addEvent(client.getId().toString(), args);
            if (event == null) {
                client.send(new MessageText("Sorry, I did not get that."));
                return;
            }

            DateFormat format = new SimpleDateFormat("EEEEE, dd MMMMM 'at' HH:mm");
            DateTime dateTime = event.getStart().getDateTime();
            long value = dateTime.getValue() + TimeUnit.MINUTES.toMillis(dateTime.getTimeZoneShift());
            String msg = String.format("Sure! I've created new [event](%s) for you:\n" +
                            "**%s** on %s",
                    event.getHtmlLink(),
                    event.getSummary(),
                    format.format(new Date(value)));
            client.send(new MessageText(msg));
        } catch (Exception e) {
            Logger.warning("scheduleNewEvent: %s", e.getMessage());
            client.send(new MessageText("Something went wrong :(."));
        }
    }

    private void scheduleCall(WireClient client, String text) throws Exception {
        UUID botId = client.getId();
        Date date = CallScheduler.parse(text);
        if (date != null) {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm', 'EEEE, MMMMM d, yyyy");
            format.setTimeZone(TimeZone.getTimeZone("CET"));

            boolean scheduled = callScheduler.scheduleCall(botId, date);
            if (scheduled) {
                String schedule = date.toString();
                callScheduler.saveSchedule(botId, schedule);
                client.send(new MessageText("OK, I will start the call here at: " + format.format(date)));
                Logger.info("Scheduled call for: `%s`, bot: %s", schedule, botId);
            } else {
                client.send(new MessageText("I am sorry, but I could not schedule the call for: " + format.format(date)));
            }
        } else {
            client.send(new MessageText("I am sorry, I could not parse that."));
        }
    }

    private void scheduleReminder(WireClient client, String text, UUID sender) throws Exception {
        UUID botId = client.getId();
        Date date = CallScheduler.parse(text);
        if (date != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm', 'EEEE, MMMMM d, yyyy");
            dateFormat.setTimeZone(TimeZone.getTimeZone("CET"));

            final String strDate = CallScheduler.extractDate(text);

            if (strDate != null)
                text = text.replace(strDate, "").trim();

            boolean scheduled = callScheduler.scheduleReminder(botId, date, text, sender);
            if (scheduled) {
                final String msg = String.format("OK, I will remind you here about: **%s**\nat: %s",
                        text,
                        dateFormat.format(date));

                client.send(new MessageText(msg), sender);

                callScheduler.saveSchedule(botId, date.toString());

                Logger.info("Scheduled reminder for: `%s`, bot: %s", date, botId);
            } else {
                client.send(new MessageText("I'm sorry, but I could not schedule a reminder for: " + dateFormat.format(date))
                        , sender);
            }
        } else {
            client.send(new MessageText("I am sorry, I could not parse that."), sender);
        }
    }

    private Picture uploadPreview(WireClient client) throws Exception {
        Picture preview = new Picture(getResource("icon.png"));
        preview.setPublic(true);

        AssetKey assetKey = client.uploadAsset(preview);
        preview.setAssetKey(assetKey.id);
        return preview;
    }

    private String printEvents(Events events, String title) {
        final DateFormat format = new SimpleDateFormat("EEEEE, dd MMMMM 'at' HH:mm");
        final StringBuilder sb = new StringBuilder(title);

        sb.append("\n\n");
        for (Event event : events.getItems()) {
            EventDateTime eventStart = event.getStart();
            DateTime start = eventStart.getDateTime();
            long value = start != null
                    ? start.getValue() + TimeUnit.MINUTES.toMillis(start.getTimeZoneShift())
                    : eventStart.getDate().getValue();
            sb.append(String.format("[%s](%s)\n%s\n—\n", event.getSummary(), event.getHtmlLink(), format.format(new Date(value))));
        }
        return sb.toString();
    }
}
