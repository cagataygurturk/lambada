package org.lambadaframework.runtime.errorhandling;


import org.apache.log4j.Logger;
import org.lambadaframework.runtime.models.error.BadRequestResponse;
import org.lambadaframework.runtime.models.error.ErrorResponse;
import org.lambadaframework.runtime.models.error.NotFoundErrorResponse;


import javax.ws.rs.NotFoundException;
import java.lang.reflect.InvocationTargetException;

public class ErrorHandler {

    static final Logger logger = Logger.getLogger(ErrorHandler.class);

    private ErrorHandler() {
    }

    public static ErrorResponse getErrorResponse(Exception e) {
        try {
            throw e;
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(new BadRequestResponse().getErrorMessage());
//            return new BadRequestResponse();
        } catch (NotFoundException ex) {
            throw new RuntimeException(new NotFoundErrorResponse().getErrorMessage());
//            return new NotFoundErrorResponse("Page not found");
        } catch (Exception ex) {
            throw new RuntimeException(new ErrorResponse().getErrorMessage());
//            return new ErrorResponse();
        } finally {
            logger.debug("Exception occured:", e);
        }
    }
}
