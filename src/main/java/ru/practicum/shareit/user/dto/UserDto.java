package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.shareit.validation.Create;
import ru.practicum.shareit.validation.Update;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;

    @NotBlank(groups = Create.class)
    @Size(max = 255, groups = {Create.class, Update.class})
    @Pattern(regexp = "(?s).*\\S.*", groups = Update.class)
    private String name;

    @NotBlank(groups = Create.class)
    @Pattern(regexp = "(?s).*\\S.*", groups = Update.class)
    @Email(groups = {Create.class, Update.class})
    @Size(max = 512, groups = {Create.class, Update.class})
    private String email;
}
