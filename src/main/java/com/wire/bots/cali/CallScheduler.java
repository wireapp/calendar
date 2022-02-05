package com.wire.bots.cali;

import com.DAO.SubscribersDAO;
import com.wire.xenon.WireClient;
import com.wire.xenon.assets.Calling;
import com.wire.xenon.assets.MessageText;
import com.wire.xenon.tools.Logger;
import org.jdbi.v3.core.Jdbi;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import org.ocpsoft.prettytime.nlp.parse.DateGroup;

import java.util.*;

public class CallScheduler {
    private final Timer timer = new Timer();
    private static final PrettyTimeParser prettyTimeParser = new PrettyTimeParser(TimeZone.getTimeZone("CET"));
    private final SubscribersDAO subscribersDAO;

    CallScheduler(Jdbi jdbi) {
        subscribersDAO = jdbi.onDemand(SubscribersDAO.class);
    }

    static String extractDate(String schedule) {
        List<DateGroup> dateGroups = prettyTimeParser.parseSyntax(schedule);
        for (DateGroup dateGroup : dateGroups) {
            return dateGroup.getText();
        }
        return null;
    }

    void loadSchedules() {
        ArrayList<UUID> subscribers = subscribersDAO.getSubscribers();
        for (UUID botId : subscribers) {
            String schedule = subscribersDAO.getSchedule(botId);
            if (schedule != null) {
                Date date = parse(schedule);
                if (date != null) {
                    boolean scheduled = scheduleCall(botId, date);
                    if (scheduled) {
                        Logger.info("Loaded Scheduled call for: `%s`, bot: %s", date, botId);
                    }
                }
            }
        }
    }

    boolean scheduleCall(UUID botId, Date date) {
        if (date.getTime() < new Date().getTime())
            return false;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try (WireClient wireClient = Service.repo.getClient(botId)) {
                    wireClient.send(new Calling("{\"version\":\"3.0\",\"type\":\"GROUPSTART\",\"sessid\":\"\",\"resp\":false}"));
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

    private void deleteSchedule(UUID botId) {
        boolean deleteSchedule = subscribersDAO.setSchedule(botId, null) != 0;
        Logger.info("Deleted schedule for bot: %s %s", botId, deleteSchedule);
    }

//    public boolean scheduleRecurrent(String botId, Date firstRun, int days) {
//        if (firstRun.getTime() < new Date().getTime())
//            return false;
//
//        timer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                try (WireClient wireClient = repo.getClient(botId)) {
//                    wireClient.call("{\"version\":\"3.0\",\"type\":\"GROUPSTART\",\"sessid\":\"\",\"resp\":false}");
//                } catch (Exception e) {
//                    Logger.warning("scheduleRecurrent. Bot: %s, scheduled: `%s`, error: %s",
//                            botId,
//                            firstRun,
//                            e);
//                }
//            }
//        }, firstRun, TimeUnit.DAYS.toMillis(days));
//
//        return true;
//    }

    static Date parse(String schedule) {
        List<DateGroup> dateGroups = prettyTimeParser.parseSyntax(schedule);
        for (DateGroup dateGroup : dateGroups) {
            for (Date date : dateGroup.getDates()) {
                return date;
            }
        }
        return null;
    }

    boolean scheduleReminder(UUID botId, Date date, String text, UUID sender) {
        if (date.getTime() < new Date().getTime())
            return false;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try (WireClient wireClient = Service.repo.getClient(botId)) {
                    wireClient.send(new MessageText(text), sender);
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

    void saveSchedule(UUID botId, String text) {
        boolean setSchedule = subscribersDAO.setSchedule(botId, text) != 0;
        Logger.info("Set schedule for bot: %s %s", botId, setSchedule);
    }

    boolean setMuted(UUID botId, boolean muted) {
        Logger.info("Set Muted to: %s for bot: %s", botId, muted);
        return 0 != subscribersDAO.setMuted(botId, muted);
    }
}
