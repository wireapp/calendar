package com.wire.bots.cali;

import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.storage.Storage;
import com.wire.bots.sdk.tools.Logger;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import org.ocpsoft.prettytime.nlp.parse.DateGroup;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class CallScheduler {
    private static final String CALL_SCHEDULE_FILENAME = "call.schedule";
    private final ClientRepo repo;
    private final Timer timer = new Timer();
    private static final PrettyTimeParser prettyTimeParser = new PrettyTimeParser(TimeZone.getTimeZone("CET"));

    public CallScheduler(ClientRepo repo) {
        this.repo = repo;
    }

    public void loadSchedules() throws Exception {
        StorageFactory storageFactory = repo.getStorageFactory();
        for (WireClient client : repo.listClients()) {
            final String botId = client.getId();
            Storage storage = storageFactory.create(botId);
            String schedule = storage.readFile(CALL_SCHEDULE_FILENAME);
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

    public boolean schedule(String botId, Date date) {
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
        Storage storage = repo.getStorageFactory().create(botId);
        storage.deleteFile(CALL_SCHEDULE_FILENAME);
        Logger.info("Deleted schedule for bot: %s", botId);
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


    public static Date parse(String schedule) {
        List<DateGroup> dateGroups = prettyTimeParser.parseSyntax(schedule);
        for (DateGroup dateGroup : dateGroups) {
            for (Date date : dateGroup.getDates()) {
                return date;
            }
        }
        return null;
    }

    public void saveSchedule(String botId, String text) throws Exception {
        StorageFactory storageFactory = repo.getStorageFactory();
        Storage storage = storageFactory.create(botId);
        storage.saveFile(CALL_SCHEDULE_FILENAME, text);
    }
}
