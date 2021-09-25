package br.com.merkandi.rest;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.vavr.control.Try;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@Path("a")
@WebListener
public class AService implements ServletContextListener
{
    private static final String RATE_LIMITER_NAME = "a-get-rate-limiter";
    private static RateLimiter rateLimiter = null;
    private static int LIMIT_FOR_PERIOD = 10;
    private static int LIMIT_REFRESH_PERIOD_IN_MILLISECONDS = 1000;
    private static int TIMEOUT_DURATION_IN_MILLISECONDS = 0;

    private static Logger aServiceLogger = Logger.getLogger("aServiceLogger");
    private static FileHandler aServiceLoggerFilehandler = null;
    private static String FILE_HANDLER_PATTERN = null;


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getA()
    {
        Supplier<Response> supplier = RateLimiter.decorateSupplier(rateLimiter, this::getAExecute);

        return Try.ofSupplier(supplier)
                .recover(throwable -> getAlternativeResponse() )
                .get();
    }

    private Response getAlternativeResponse()
    {
        return Response.status(Response.Status.TOO_MANY_REQUESTS).entity(RATE_LIMITER_NAME).build();
    }

    public Response getAExecute()
    {
        return Response.status(Response.Status.OK).entity("A").build();
    }


    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        FILE_HANDLER_PATTERN = sce.getServletContext().getInitParameter("LOG_PATH");
        try
        {
            SimpleFormatter formatter = new SimpleFormatter();
            aServiceLoggerFilehandler = new FileHandler(FILE_HANDLER_PATTERN);
            aServiceLoggerFilehandler.setFormatter(formatter);
            aServiceLogger.addHandler(aServiceLoggerFilehandler);
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if( rateLimiter == null )
        {
            RateLimiterConfig config = RateLimiterConfig
                    .custom()
                    .limitRefreshPeriod(Duration.ofMillis(LIMIT_REFRESH_PERIOD_IN_MILLISECONDS))
                    .limitForPeriod(LIMIT_FOR_PERIOD)
                    .timeoutDuration(Duration.ofMillis(TIMEOUT_DURATION_IN_MILLISECONDS))
                    .build();

            RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.of(config);
            rateLimiter = rateLimiterRegistry.rateLimiter(RATE_LIMITER_NAME, config);
        }

        aServiceLogger.info("A Service running...");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }
}
