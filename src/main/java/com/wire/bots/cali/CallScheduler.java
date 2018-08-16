package com.wire.bots.cali;

import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.tools.Logger;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import org.ocpsoft.prettytime.nlp.parse.DateGroup;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class CallScheduler {
    private final ClientRepo repo;
    private final Timer timer = new Timer();
    private static final PrettyTimeParser prettyTimeParser = new PrettyTimeParser(TimeZone.getTimeZone("CET"));
    private final Database database;

    CallScheduler(Config.DB postgres, ClientRepo repo) {
        this.repo = repo;
        this.database = new Database(postgres);
    }

    void loadSchedules() throws Exception {
        ArrayList<String> subscribers = database.getSubscribers();
        for (String botId : subscribers) {
            String schedule = database.getSchedule(botId);
            if (schedule != null) {
                Date date = parse(schedule);
                if (date != null) {
                    boolean scheduled = schedule(botId, date);
                    if (scheduled) {
                        Logger.info("Loaded Scheduled call for: `%s`, bot: %s", date, botId);
                    }
                }
            }
        }
    }

    boolean schedule(String botId, Date date) {
        if (date.getTime() < new Date().getTime())
            return false;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    WireClient wireClient = repo.getWireClient(botId);
                    wireClient.call("{\"version\":\"3.0\",\"type\":\"GROUPSTART\",\"sessid\":\"\",\"resp\":false}");
                    deleteSchedule(wireClient.getId());
                } catch (Exception e) {
                    Logger.warning("schedule. Bot: %s, scheduled: `%s`, error: %s",
                            botId,
                            date,
                            e);
                }
            }
        }, date);

        return true;
    }

    private void deleteSchedule(String botId) throws Exception {
        boolean deleteSchedule = database.deleteSchedule(botId);
        Logger.info("Deleted schedule for bot: %s %s", botId, deleteSchedule);
    }

    public boolean scheduleRecurrent(String botId, Date firstRun, int days) {
        if (firstRun.getTime() < new Date().getTime())
            return false;

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    WireClient wireClient = repo.getWireClient(botId);
                    wireClient.call("{\"version\":\"3.0\",\"type\":\"GROUPSTART\",\"sessid\":\"\",\"resp\":false}");
                } catch (Exception e) {
                    Logger.warning("scheduleRecurrent. Bot: %s, scheduled: `%s`, error: %s",
                            botId,
                            firstRun,
                            e);
                }
            }
        }, firstRun, TimeUnit.DAYS.toMillis(days));

        return true;
    }

    static Date parse(String schedule) {
        List<DateGroup> dateGroups = prettyTimeParser.parseSyntax(schedule);
        for (DateGroup dateGroup : dateGroups) {
            for (Date date : dateGroup.getDates()) {
                return date;
            }
        }
        return null;
    }

    void saveSchedule(String botId, String text) throws Exception {
        boolean setSchedule = database.setSchedule(botId, text);
        Logger.info("Set schedule for bot: %s %s", botId, setSchedule);
    }

    void setMuted(String botId, boolean muted) throws Exception {
        database.setMuted(botId, muted);
        Logger.info("Set Muted to: %s for bot: %s", botId, muted);
    }
}
