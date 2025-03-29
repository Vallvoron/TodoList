package com.example.TodoList.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TaskRequest {
    @Schema(description = "Название задачи", example = "name")
    @Size(min = 4, message = "Название не может быть меньше 4 символов")
    @NotBlank(message = "Название не должно быть пустым")
    public String title;

    @Schema(description = "Описание задачи", example = "description")
    public String description;

    @Schema(description = "Статус задачи", example = "ACTIVE")
    @Enumerated(EnumType.STRING)
    public Status status;

}
