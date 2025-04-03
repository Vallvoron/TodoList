package com.example.TodoList.controllers;

import com.example.TodoList.entities.Task;
import com.example.TodoList.models.*;
import com.example.TodoList.repositories.TaskRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/tasks")
public class TodoListController {

    private final TaskRepository taskRepository;

    @Autowired
    public TodoListController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAllTasks() {
        List<Task> taskList= taskRepository.findAll();
        return ResponseEntity.ok().body(taskList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(@RequestParam UUID id) {
        Optional<Task> optTask = taskRepository.findById(id);
        if(optTask.isEmpty()){
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "Задача не найдена"));
        }
        else {
            Task task = optTask.get();
            return ResponseEntity.ok().body(task);
        }
    }

    @PostMapping
    public ResponseEntity<?> createTask(@Valid @RequestBody TaskRequest request) {
        if(request.getTitle().length()<4) {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "Имя не может быть меньше 4 символов"));
        }
        Task task = new Task();
        task.setStatus(request.getStatus());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        processTitle(task);
        if(task.getDeadline()!=null && task.getDeadline().isBefore(LocalDate.now())){
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "Дэдлайн не может быть раньше настоящего времени"));
        }
        updateTaskStatus(task);
        Task savedTask = taskRepository.save(task);
        return new ResponseEntity<>(savedTask, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<?> updateTask(@RequestParam UUID id, @Valid @RequestBody TaskRequest taskDetails) {
        Optional<Task> optTask = taskRepository.findById(id);
        if(optTask.isEmpty()){
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "Задача не найдена"));
        }
        Task existingTask= optTask.get();
        existingTask.setTitle(taskDetails.getTitle());
        existingTask.setDescription(taskDetails.getDescription());
        existingTask.setStatus(taskDetails.getStatus());
        existingTask.setUpdatedAt(LocalDateTime.now());

        processTitle(existingTask);
        if(existingTask.getDeadline()!=null && existingTask.getDeadline().isBefore(LocalDate.now())){
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "Дэдлайн не может быть раньше настоящего времени"));
        }
        updateTaskStatus(existingTask);
        Task updatedTask = taskRepository.save(existingTask);

        return ResponseEntity.ok(updatedTask);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteTask(@RequestParam UUID id) {
        Optional<Task> optTask = taskRepository.findById(id);
        if(optTask.isEmpty()){
            return ResponseEntity.status(404).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "Задача не найдена"));
        }
        taskRepository.deleteById(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("message", "Задача удалена"));
    }

    private void processTitle(Task task) {
        if (task.getTitle().contains("!1")) {
            task.setPriority(Priority.CRITICAL);
        } else if (task.getTitle().contains("!2")) {
            task.setPriority(Priority.HIGH);
        } else if (task.getTitle().contains("!3")) {
            task.setPriority(Priority.MEDIUM);
        } else if (task.getTitle().contains("!4")) {
            task.setPriority(Priority.LOW);
        }

        task.setTitle(task.getTitle().replaceAll("!1|!2|!3|!4", "").trim());

        Pattern pattern = Pattern.compile("!before\\s+(\\d{2}[.-]\\d{2}[.-]\\d{4})");
        Matcher matcher = pattern.matcher(task.getTitle());
        if (matcher.find()) {
            String dateString = matcher.group(1);
            DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            try {
                LocalDate deadline = LocalDate.parse(dateString, formatter1);
                task.setDeadline(deadline);
            } catch (DateTimeParseException e1) {
                try {
                    LocalDate deadline = LocalDate.parse(dateString, formatter2);
                    task.setDeadline(deadline);
                } catch (DateTimeParseException e2) {
                    System.err.println("Неверный формат даты: " + dateString);
                }
            }
        }
        task.setTitle(task.getTitle().replaceAll(pattern.pattern(), "").trim());
    }

    private void updateTaskStatus(Task task) {
        if (task.getDeadline() != null) {
            LocalDate today = LocalDate.now();
            if (task.getStatus() == Status.COMPLETED) {
                if (task.getDeadline().isBefore(today)) {
                    task.setStatus(Status.LATE);
                }
            } else {
                if (task.getDeadline().isBefore(today)) {
                    task.setStatus(Status.OVERDUE);
                } else task.setStatus(Status.ACTIVE);
            }
        }
    }
}


