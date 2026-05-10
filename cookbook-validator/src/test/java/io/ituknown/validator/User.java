package io.ituknown.validator;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
class User {

    @NotBlank(groups = Modify.class)
    private String username;

    @AgeRange(min = 0, max = 200)
    private Integer age;

    interface Modify {
    }
}
