package hud.SpringSecurityTemplate.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageableParam {
    private String sortBy;
    private String sortDirection;
    private Integer size;
    private Integer page;


    public PageableParam(Integer size, Integer page) {
        this.size = size;
        this.page = page;
    }

}

