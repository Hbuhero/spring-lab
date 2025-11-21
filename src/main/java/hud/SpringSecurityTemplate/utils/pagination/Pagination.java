package hud.SpringSecurityTemplate.utils.pagination;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;


public final class Pagination<T> {

    private final Page<T> t;

    private Pagination(Page<T> t) {
        this.t = t;
    }

    public static <T> Pagination of(Page<T> t) {
        return new Pagination(t);
    }

    public ResponseEntity<?> paginate() {

        PageResponse<T> pageResponse = new PageResponse<>();
        pageResponse.setPage(new hud.SpringSecurityTemplate.utils.pagination.Page(
                String.valueOf(t.getNumber()),
                String.valueOf(t.getNumberOfElements()),
                String.valueOf(t.getTotalElements()),
                String.valueOf(t.getTotalPages() - 1)));
        if (t.isEmpty()) {
            pageResponse.setRecords(new ArrayList<>());
        } else {
            pageResponse.setRecords(t.get().toList());
        }
        return new ResponseEntity<>(pageResponse, HttpStatus.OK);

    }
}