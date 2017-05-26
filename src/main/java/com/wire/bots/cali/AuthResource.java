package com.wire.bots.cali;

import com.google.api.client.auth.oauth2.Credential;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.WireClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

@Path("/user/auth/google_oauth2/callback")
public class AuthResource {
    private final ClientRepo repo;

    AuthResource(ClientRepo repo) {
        this.repo = repo;
    }

    @GET
    public Response auth(@QueryParam("state") final String bot,
                         @QueryParam("code") final String code) throws Exception {

        Credential credential = CalendarAPI.processAuthCode(bot, code);

        Logger.info("New Credentials: Bot:%s token: %s refresh: %s Exp: %d minutes",
                bot,
                credential.getAccessToken() != null,
                credential.getRefreshToken() != null,
                TimeUnit.SECONDS.toMinutes(credential.getExpiresInSeconds())
        );

        try {
            WireClient wireClient = repo.getWireClient(bot);
            if (wireClient != null) {
                String msg = "Cool! You can schedule a meeting now by simply just writing:\n" +
                        "`Dinner tomorrow at 2pm`\n" +
                        "or\n" +
                        "`Fishing every Tuesday at 4am till noon`\n" +
                        "or\n" +
                        "`June 13 at 14:45 Dentist :(`\n" +
                        "I will remind you on time too ;)\n" +
                        "You can list upcoming events by typing: `/list`";

                wireClient.sendText(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Response.
                ok("Thank you! You can enjoy Cali now.").
                status(200).
                build();
    }
}