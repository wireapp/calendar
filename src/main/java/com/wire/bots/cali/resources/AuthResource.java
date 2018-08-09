package com.wire.bots.cali.resources;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.calendar.model.Channel;
import com.wire.bots.cali.CalendarAPI;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.tools.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/user/auth/google_oauth2/callback")
public class AuthResource {
    private final ClientRepo repo;

    public AuthResource(ClientRepo repo) {
        this.repo = repo;
    }

    @GET
    public Response auth(@QueryParam("state") final String bot,
                         @QueryParam("code") final String code) {
        try {
            Credential credential = CalendarAPI.processAuthCode(bot, code);

            try {
                Channel channel = CalendarAPI.watch(bot);
                Logger.info("New channel: %s", channel.getId());
            } catch (Exception e) {
                Logger.error("AuthResource: %s %s", bot, e);
            }

//            Logger.info("New Credentials: Bot:%s token: %s refresh: %s Exp: %d minutes",
//                    bot,
//                    credential.getAccessToken() != null,
//                    credential.getRefreshToken() != null,
//                    TimeUnit.SECONDS.toMinutes(credential.getExpiresInSeconds())
//            );

            WireClient wireClient = repo.getWireClient(bot);
            if (wireClient != null) {
                String msg = "Cool! You can list upcoming events by typing: `/list`\n";
                wireClient.sendText(msg);
            }

            return Response.
                    ok("Thank you!").
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