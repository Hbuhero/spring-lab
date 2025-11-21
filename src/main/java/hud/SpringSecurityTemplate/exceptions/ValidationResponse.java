package hud.SpringSecurityTemplate.exceptions;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public class ValidationResponse {
    private String error;
    private Integer status;
    private List<ValidationError> errors;
    private Instant timestamp;

    public ValidationResponse(List<ValidationError> errors, String error, Integer status) {
        setErrors(errors);
        this.error = error;
        this.status = status;
        this.timestamp = Instant.now();
    }

    public List<ValidationError> getErrors() {

        return errors == null ? null : new ArrayList<ValidationError>(errors);
    }

    public final void setErrors(List<ValidationError> errors)  {

        if (errors == null) {
            this.errors = null;
        } else {
            this.errors = Collections.unmodifiableList(errors);
        }
    }

}
