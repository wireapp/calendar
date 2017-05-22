package com.wire.bots.cali;

import com.google.api.client.auth.oauth2.Credential;
import com.wire.bots.sdk.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Path("/user/auth/google_oauth2/callback")
public class AuthResource {
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(4);

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
        return Response.
                ok("Thank you! You can enjoy Cali now. Type: /list").
                status(200).
                build();
    }
}