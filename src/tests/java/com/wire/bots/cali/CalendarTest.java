package com.wire.bots.cali;

import org.junit.Test;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import org.ocpsoft.prettytime.nlp.parse.DateGroup;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CalendarTest {
    private static final String EMAIL_1 = "dejan1@wire.com";
    private static final String EMAIL_2 = "dejan2@wire.com";
    private static final String EMAIL_3 = "dejan3@wire.com";

    //@Test
    public void testEmailExctract() {
        ArrayList<String> emails = CalendarAPI.extractEmail(EMAIL_1);
        assert (!emails.isEmpty());
        assert (emails.get(0).equals(EMAIL_1));

        emails = CalendarAPI.extractEmail("This is some text with " + EMAIL_1 + " email addresses");
        assert (!emails.isEmpty());
        assert (emails.get(0).equals(EMAIL_1));

        emails = CalendarAPI.extractEmail("*** " + EMAIL_1 + " , @, . ,," + EMAIL_2 + " wsd.wew. " + EMAIL_3);
        assert (emails.size() == 3);
        assert (emails.get(0).equals(EMAIL_1));
        assert (emails.get(1).equals(EMAIL_2));
        assert (emails.get(2).equals(EMAIL_3));
    }

    @Test
    public void testDateParsing() {
        String line = "Tomorrow at 9:00";
        List<DateGroup> dateGroups = new PrettyTimeParser().parseSyntax(line);
        DateGroup dateGroup = dateGroups.get(0);
        Date s = dateGroup.getDates().get(0);
        System.out.println(s.toString());

        line = "Friday at 18:00";
        dateGroups = new PrettyTimeParser().parseSyntax(line);
        dateGroup = dateGroups.get(0);
        s = dateGroup.getDates().get(0);
        System.out.println(s.toString());

        line = "18:00";
        dateGroups = new PrettyTimeParser().parseSyntax(line);
        dateGroup = dateGroups.get(0);
        s = dateGroup.getDates().get(0);
        System.out.println(s.toString());

        line = "weekday 16:00";
        dateGroups = new PrettyTimeParser().parseSyntax(line);
        dateGroup = dateGroups.get(0);
        s = dateGroup.getDates().get(0);
        System.out.println(s.toString());
    }

    //@Test
    public void testScheduler() throws InterruptedException {
        String text = "every day at 9am";
        Date firstRun = CallScheduler.parse(text);
        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    System.out.println("Calling...");
                } catch (Exception e) {
                    System.err.printf("loadSchedules, schedule: `%s`, error: %s\n",
                            firstRun,
                            e);
                }
            }
        }, firstRun, TimeUnit.MINUTES.toMillis(1));

        Thread.sleep(1000000);
    }

    @Test
    public void dateFormat() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm', 'EEEE, MMMMM d, yyyy");
        format.setTimeZone(TimeZone.getTimeZone("CET"));
        Date now = new Date();
        System.out.println(format.format(now));
    }
}
