package com.wire.bots.cali.resources;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Channel;
import com.wire.bots.cali.CalendarAPI;
import com.wire.lithium.ClientRepo;
import com.wire.xenon.WireClient;
import com.wire.xenon.assets.MessageText;
import com.wire.xenon.tools.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Path("/user/auth/google_oauth2/callback")
public class AuthResource {
    private final ClientRepo repo;

    public AuthResource(ClientRepo repo) {
        this.repo = repo;
    }

    @GET
    public Response auth(@QueryParam("state") final UUID bot,
                         @QueryParam("code") final String code) {
        try {
            Credential credential = CalendarAPI.processAuthCode(bot.toString(), code);

            try {
                Channel channel = CalendarAPI.watch(bot.toString());
                Logger.info("New channel: %s", channel.getId());
            } catch (Exception e) {
                Logger.error("AuthResource: %s %s", bot, e);
            }

            Logger.info("New Credentials: Bot:%s token: %s refresh: %s Exp: %d minutes",
                    bot,
                    credential.getAccessToken() != null,
                    credential.getRefreshToken() != null,
                    TimeUnit.SECONDS.toMinutes(credential.getExpiresInSeconds())
            );

            WireClient wireClient = repo.getClient(bot);
            if (wireClient != null) {
                Calendar calendar = CalendarAPI.getCalendarService(bot.toString());
                String msg = String.format("Nice! I now have access to the **%s** calendar.\n" +
                                "I will be posting reminders and updates of your scheduled events here.\n",
                        calendar.calendars().get("primary").getCalendarId());

                wireClient.send(new MessageText(msg));
                wireClient.send(new MessageText("A couple of tips:\n" +
                        "For a quick overview of your\n" +
                        "forthcoming events, type: `/list`\n" +
                        "If you want to see your day’s schedule\n" +
                        "use: `/today` or `/tomorrow`\n" +
                        "Type `/help` for more details."));
            }

            return Response.
                    ok(AuthResource.class.getClassLoader().getResourceAsStream("service-connected.html"), MediaType.TEXT_HTML_TYPE).
                    status(200).
                    build();
        } catch (Exception e) {
            Logger.error("AuthResource: %s %s", bot, e);
            // e.printStackTrace();
            return Response.
                    status(500).
                    build();
        }
    }
}
