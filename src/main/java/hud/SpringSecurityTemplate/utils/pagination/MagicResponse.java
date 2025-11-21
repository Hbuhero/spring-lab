package hud.SpringSecurityTemplate.utils.pagination;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import hud.SpringSecurityTemplate.payloads.responses.Message;

import java.util.Optional;

public final class MagicResponse<T> {

    private final Optional<T> t;
    private MagicResponse(T t) {
        this.t = Optional.ofNullable(t);
    }


    public static <T> MagicResponse of(T t) {
        return new MagicResponse(t);
    }

    public ResponseEntity<?> ifPresentOrElse(String errorMessage) {
        if (t.isEmpty()) {
            return new ResponseEntity<>(new Message(errorMessage),HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(t.get(), HttpStatus.OK);

    }
}

//MagicResponse.of(this.depositWeightRepository.findById(id)).ifPresentOrElse("Deposit weight with id " + id + " not found");