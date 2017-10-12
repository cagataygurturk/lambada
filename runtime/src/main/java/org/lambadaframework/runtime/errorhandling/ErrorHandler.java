package org.lambadaframework.runtime.errorhandling;


import org.apache.log4j.Logger;
import org.lambadaframework.runtime.models.error.BadRequestResponse;
import org.lambadaframework.runtime.models.error.ErrorResponse;
import org.lambadaframework.runtime.models.error.NotFoundErrorResponse;
import org.lambadaframework.runtime.models.error.RuntimeErrorResponse;


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
            return new BadRequestResponse();
        } catch (NotFoundException ex) {
            return new NotFoundErrorResponse();
        } catch (RuntimeException ex) {
            return new RuntimeErrorResponse(ex.getMessage());
        } catch (Exception ex) {
            return new ErrorResponse();
        } finally {
            logger.debug("Exception occured:", e);
        }
    }
}
