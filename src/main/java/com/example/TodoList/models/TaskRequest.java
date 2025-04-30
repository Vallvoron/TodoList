package com.example.TodoList.models;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequest {
    @Schema(description = "Название задачи", example = "name")
    @NotBlank(message = "Название не должно быть пустым")
    public String title;

    @Schema(description = "Описание задачи", example = "description")
    public String description;

    @Schema(description = "Статус задачи", example = "ACTIVE")
    @Enumerated(EnumType.STRING)
    public Status status;

    @Schema(description = "Приоритет", example = "LOW")
    @Enumerated(EnumType.STRING)
    public Priority priority;

    @Schema(description = "Дэдлайн", example = "11.11.2111")
    public LocalDate deadline;

}
