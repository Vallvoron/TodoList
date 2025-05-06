package com.example.TodoList;

import com.example.TodoList.controllers.TodoListController;
import com.example.TodoList.entities.Task;
import com.example.TodoList.models.Priority;
import com.example.TodoList.models.Status;
import com.example.TodoList.models.TaskRequest;
import com.example.TodoList.repositories.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


class UnitTests {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TodoListController TaskProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void title_criticalPriority() {
        Task task = new Task();
        task.setTitle("  Important task !1  ");
        TaskProcessor.processTitle(task);
        assertEquals(Priority.CRITICAL, task.getPriority());
        assertEquals("Important task", task.getTitle());
    }

    @Test
    void title_highPriority() {
        Task task = new Task();
        task.setTitle("Task !2");
        TaskProcessor.processTitle(task);
        assertEquals(Priority.HIGH, task.getPriority());
        assertEquals("Task", task.getTitle());
    }

    @Test
    void title_mediumPriority() {
        Task task = new Task();
        task.setTitle("Another Task !3");
        TaskProcessor.processTitle(task);
        assertEquals(Priority.MEDIUM, task.getPriority());
        assertEquals("Another Task", task.getTitle());
    }

    @Test
    void title_lowPriority() {
        Task task = new Task();
        task.setTitle("Some Task !4");
        TaskProcessor.processTitle(task);
        assertEquals(Priority.LOW, task.getPriority());
        assertEquals("Some Task", task.getTitle());
    }

    @Test
    void title_noPriority() {
        Task task = new Task();
        task.setTitle("Regular task");
        TaskProcessor.processTitle(task);
        assertEquals(Priority.MEDIUM, task.getPriority());
        assertEquals("Regular task", task.getTitle());
    }

    @Test
    void title_multiplePriority() {
        Task task = new Task();
        task.setTitle("Task !1 !2 !3 !4");
        TaskProcessor.processTitle(task);
        assertEquals(Priority.CRITICAL, task.getPriority());
        assertEquals("Task", task.getTitle());
    }

    @Test
    void title_beforeDateCorrect() {
        Task task = new Task();
        task.setTitle("Grocery shopping !before 25.12.2024");
        TaskProcessor.processTitle(task);
        assertEquals(LocalDate.of(2024, 12, 25), task.getDeadline());
        assertEquals("Grocery shopping", task.getTitle());
    }

    @Test
    void title_beforeDateCorrectDashes() {
        Task task = new Task();
        task.setTitle("Grocery shopping !before 25-12-2024");
        TaskProcessor.processTitle(task);
        assertEquals(LocalDate.of(2024, 12, 25), task.getDeadline());
        assertEquals("Grocery shopping", task.getTitle());
    }

    @Test
    void title_invalidDate() {
        Task task = new Task();
        task.setTitle("Grocery shopping !before 25/12/2024");
        TaskProcessor.processTitle(task);
        assertNull(task.getDeadline());
        assertEquals("Grocery shopping !before 25/12/2024", task.getTitle());
    }

    @Test
    void title_beforeDateAndPriorityMarkers() {
        Task task = new Task();
        task.setTitle("Important task !2 !before 10.01.2025");
        TaskProcessor.processTitle(task);
        assertEquals(Priority.HIGH, task.getPriority());
        assertEquals(LocalDate.of(2025, 01, 10), task.getDeadline());
        assertEquals("Important task", task.getTitle());
    }

    @Test
    void updateTaskStatusCompletedPast() {
        Task task = new Task();
        task.setStatus(Status.COMPLETED);
        task.setDeadline(LocalDate.now().minusDays(1));
        TaskProcessor.updateTaskStatus(task);
        assertEquals(Status.LATE, task.getStatus());
    }

    @Test
    void updateTaskStatusCompletedFuture() {
        Task task = new Task();
        task.setStatus(Status.COMPLETED);
        task.setDeadline(LocalDate.now().plusDays(1));
        TaskProcessor.updateTaskStatus(task);
        assertEquals(Status.COMPLETED, task.getStatus());
    }

    @Test
    void updateTaskStatusActivePast() {
        Task task = new Task();
        task.setStatus(Status.ACTIVE);
        task.setDeadline(LocalDate.now().minusDays(1));
        TaskProcessor.updateTaskStatus(task);
        assertEquals(Status.OVERDUE, task.getStatus());
    }

    @Test
    void updateTaskStatusActiveFuture() {
        Task task = new Task();
        task.setStatus(Status.ACTIVE);
        task.setDeadline(LocalDate.now().plusDays(1));
        TaskProcessor.updateTaskStatus(task);
        assertEquals(Status.ACTIVE, task.getStatus());
    }
    @Test
    void updateTaskStatusNoDeadline() {
        Task task = new Task();
        task.setStatus(Status.ACTIVE);
        task.setDeadline(null);
        TaskProcessor.updateTaskStatus(task);
        assertEquals(Status.ACTIVE, task.getStatus());
    }

    @Test
    void title_onlyData() {
        Task task = new Task();
        task.setTitle("!before 15.03.2024");
        TaskProcessor.processTitle(task);
        assertEquals(LocalDate.of(2024, 3, 15), task.getDeadline());
        assertEquals("", task.getTitle());
    }
    @Test
    void title_onlyDataDashes() {
        Task task = new Task();
        task.setTitle("!before 15-03-2024");
        TaskProcessor.processTitle(task);
        assertEquals(LocalDate.of(2024, 3, 15), task.getDeadline());
        assertEquals("", task.getTitle());
    }

    @Test
    void task_priorityOverride() {
        TaskRequest request = new TaskRequest();
        request.setTitle("Task !1");
        request.setPriority(Priority.LOW);
        request.setStatus(Status.ACTIVE);

        Task expectedTask = new Task();
        expectedTask.setTitle("Task");
        expectedTask.setPriority(Priority.LOW);

        when(taskRepository.save(any(Task.class))).thenReturn(expectedTask);

        ResponseEntity<?> response = TaskProcessor.createTask(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(Priority.LOW, ((Task) response.getBody()).getPriority());
    }
    @Test
    void task_DeadlineOverride() {

        TaskRequest request = new TaskRequest();
        request.setTitle("Task !before 25.12.2024");
        request.setPriority(Priority.LOW);
        request.setStatus(Status.ACTIVE);
        request.setDeadline(LocalDate.of(2025, 1, 1));
        Task expectedTask = new Task();
        expectedTask.setTitle("Task");
        expectedTask.setPriority(Priority.LOW);
        expectedTask.setDeadline(LocalDate.of(2025, 1, 1));

        when(taskRepository.save(any(Task.class))).thenReturn(expectedTask);

        ResponseEntity<?> response = TaskProcessor.createTask(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createTask_withShortTitle_shouldReturnBadRequest() {

        TaskRequest request = new TaskRequest();
        request.setTitle("abc"); // Short title
        request.setStatus(Status.ACTIVE);
        request.setPriority(Priority.HIGH);

        ResponseEntity<?> response = TaskProcessor.createTask(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    void createTask_withInvalidDeadline_shouldReturnBadRequest() {

        TaskRequest request = new TaskRequest();
        request.setTitle("Valid Title");
        request.setDeadline(LocalDate.now().minusDays(1));
        request.setStatus(Status.ACTIVE);

        ResponseEntity<?> response = TaskProcessor.createTask(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }
}
