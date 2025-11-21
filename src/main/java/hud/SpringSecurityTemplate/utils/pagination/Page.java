package hud.SpringSecurityTemplate.utils.pagination;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Page {
    private String pageNo;
    private String pageSize;
    private String totalSize;
    private String pageCount;
}

