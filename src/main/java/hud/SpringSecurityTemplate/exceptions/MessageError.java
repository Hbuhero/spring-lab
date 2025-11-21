package hud.SpringSecurityTemplate.exceptions;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageError implements Serializable {
    private String error;
    private String errorDescription;
}
