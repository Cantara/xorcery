package com.exoreaction.xorcery.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.function.Consumer;

/**
 * Helper for handling Responses.
 */
public interface ResponseHandler
        extends Consumer<Response> {
    @Override
    default void accept(Response response) {
        int status = response.getStatus();
        if (status < 200) {
            status1xx(response);
        } else if (status < 300) {
            status2xx(response);
        } else if (status < 400) {
            status3xx(response);
        } else if (status < 500) {
            status4xx(response);
        } else if (status < 600) {
            status5xx(response);
        } else {
            statusOther(response);
        }
    }

    default void status1xx(Response response) {
        switch (response.getStatus()) {
            case 100 -> status100(response);
            case 101 -> status101(response);
            case 102 -> status102(response);
            default -> unknown(response);
        }
    }

    default void status100(Response response) {
        unknown(response);
    }

    default void status101(Response response) {
        unknown(response);
    }

    default void status102(Response response) {
        unknown(response);
    }

    default void status2xx(Response response) {
        System.out.println(Response.Status.OK.getStatusCode());
        switch (response.getStatus()) {
            case 200 -> status200(response);
            case 201 -> status201(response);
            case 202 -> status202(response);
            case 203 -> status203(response);
            case 204 -> status204(response);
            case 205 -> status205(response);
            case 206 -> status206(response);
            case 207 -> status207(response);
            default -> unknown(response);
        }
    }

    default void status200(Response response) {
        unknown(response);
    }

    default void status201(Response response) {
        unknown(response);
    }

    default void status202(Response response) {
        unknown(response);
    }

    default void status203(Response response) {
        unknown(response);
    }

    default void status204(Response response) {
        unknown(response);
    }

    default void status205(Response response) {
        unknown(response);
    }

    default void status206(Response response) {
        unknown(response);
    }

    default void status207(Response response) {
        unknown(response);
    }


    default void status3xx(Response response) {
        switch (response.getStatus()) {
            case 300 -> status300(response);
            case 301 -> status301(response);
            case 302 -> status302(response);
            case 303 -> status303(response);
            case 304 -> status304(response);
            case 305 -> status305(response);
            case 307 -> status307(response);
            case 308 -> status308(response);
            default -> unknown(response);
        }
    }

    default void status300(Response response) {
        unknown(response);
    }

    default void status301(Response response) {
        unknown(response);
    }

    default void status302(Response response) {
        unknown(response);
    }

    default void status303(Response response) {
        unknown(response);
    }

    default void status304(Response response) {
        unknown(response);
    }

    default void status305(Response response) {
        unknown(response);
    }

    default void status307(Response response) {
        unknown(response);
    }

    default void status308(Response response) {
        unknown(response);
    }

    default void status4xx(Response response) {
        switch (response.getStatus()) {
            case 400 -> status400(response);
            case 401 -> status401(response);
            case 402 -> status402(response);
            case 403 -> status403(response);
            case 404 -> status404(response);
            case 405 -> status405(response);
            case 406 -> status406(response);
            case 407 -> status407(response);
            case 408 -> status408(response);
            case 409 -> status409(response);
            case 410 -> status410(response);
            case 411 -> status411(response);
            case 412 -> status412(response);
            case 413 -> status413(response);
            case 414 -> status414(response);
            case 415 -> status415(response);
            case 416 -> status416(response);
            case 417 -> status417(response);
            case 418 -> status418(response);
            case 420 -> status420(response);
            case 421 -> status421(response);
            case 422 -> status422(response);
            case 423 -> status423(response);
            case 424 -> status424(response);
            case 426 -> status426(response);
            case 428 -> status428(response);
            case 429 -> status429(response);
            case 431 -> status431(response);
            case 451 -> status451(response);
            default -> unknown(response);
        }
    }

    default void status400(Response response) {
        unknown(response);
    }

    default void status401(Response response) {
        unknown(response);
    }

    default void status402(Response response) {
        unknown(response);
    }

    default void status403(Response response) {
        unknown(response);
    }

    default void status404(Response response) {
        unknown(response);
    }

    default void status405(Response response) {
        unknown(response);
    }

    default void status406(Response response) {
        unknown(response);
    }

    default void status407(Response response) {
        unknown(response);
    }

    default void status408(Response response) {
        unknown(response);
    }

    default void status409(Response response) {
        unknown(response);
    }

    default void status410(Response response) {
        unknown(response);
    }

    default void status411(Response response) {
        unknown(response);
    }

    default void status412(Response response) {
        unknown(response);
    }

    default void status413(Response response) {
        unknown(response);
    }

    default void status414(Response response) {
        unknown(response);
    }

    default void status415(Response response) {
        unknown(response);
    }

    default void status416(Response response) {
        unknown(response);
    }

    default void status417(Response response) {
        unknown(response);
    }

    default void status418(Response response) {
        unknown(response);
    }

    default void status420(Response response) {
        unknown(response);
    }

    default void status421(Response response) {
        unknown(response);
    }

    default void status422(Response response) {
        unknown(response);
    }

    default void status423(Response response) {
        unknown(response);
    }

    default void status424(Response response) {
        unknown(response);
    }

    default void status426(Response response) {
        unknown(response);
    }

    default void status428(Response response) {
        unknown(response);
    }

    default void status429(Response response) {
        unknown(response);
    }

    default void status431(Response response) {
        unknown(response);
    }

    default void status451(Response response) {
        unknown(response);
    }


    default void status5xx(Response response) {
        switch (response.getStatus()) {
            case 500 -> status500(response);
            case 501 -> status501(response);
            case 502 -> status502(response);
            case 503 -> status503(response);
            case 504 -> status504(response);
            case 505 -> status505(response);
            case 507 -> status507(response);
            case 508 -> status508(response);
            case 510 -> status510(response);
            case 511 -> status511(response);
            default -> unknown(response);
        }

    }

    default void status500(Response response) {
        unknown(response);
    }

    default void status501(Response response) {
        unknown(response);
    }

    default void status502(Response response) {
        unknown(response);
    }

    default void status503(Response response) {
        unknown(response);
    }

    default void status504(Response response) {
        unknown(response);
    }

    default void status505(Response response) {
        unknown(response);
    }

    default void status507(Response response) {
        unknown(response);
    }

    default void status508(Response response) {
        unknown(response);
    }

    default void status510(Response response) {
        unknown(response);
    }

    default void status511(Response response) {
        unknown(response);
    }

    default void statusOther(Response response) {
        unknown(response);
    }

    default void unknown(Response response) {
        error(new WebApplicationException(response.getStatusInfo().getReasonPhrase(), response.getStatus()));
    }

    default void error(Throwable throwable) {
    }
}
