package com.wire.bots.cali;

import com.wire.bots.sdk.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Path("/user/auth/google_oauth2/callback")
public class AuthResource {
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(4);

    @GET
    public Response auth(@QueryParam("state") final String bot,
                         @QueryParam("code") final String code) throws Exception {

        Logger.info("AuthResource: Bot: %s, code: %s",
                bot,
                code);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    CalendarAPI.processAuthCode(bot, code);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        return Response.
                ok("Thank you! You can enjoy Cali now. Type: /list").
                status(200).
                build();
    }
}