package com.wire.bots.cali;

import java.util.ArrayList;

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
}
