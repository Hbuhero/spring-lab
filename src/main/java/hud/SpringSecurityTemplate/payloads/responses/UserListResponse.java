package hud.SpringSecurityTemplate.payloads.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserListResponse {
    private List<UserResponse> users;
    private Long totalCount;
    private Integer currentPage;
    private Integer totalPages;
    private Integer pageSize;
}
