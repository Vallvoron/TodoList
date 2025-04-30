package com.example.TodoList;

import com.example.TodoList.controllers.TodoListController;
import com.example.TodoList.models.Priority;
import com.example.TodoList.entities.Task;
import com.example.TodoList.repositories.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class TodoListApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private WebApplicationContext webApplicationContext;

	private TodoListController todoListController;

	@Autowired
	private TaskRepository taskRepository;

	@BeforeEach
	void setup() {
		taskRepository.deleteAll();
	}

	@Test
	void contextLoads() {
		System.out.println("taskRepository is mock: " + Mockito.mockingDetails(taskRepository).isMock());
	}

	@Test
	void getAllTasks_shouldReturnOk() throws Exception {
		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	void createTask_whenTitleIsTooShort() throws Exception {
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", "abc",
						"description", "New Description",
						"status", "ACTIVE")
		);

		MvcResult result = mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andReturn();

		assertEquals(400, result.getResponse().getStatus());
		assertEquals(MediaType.APPLICATION_JSON_VALUE, result.getResponse().getContentType());
		String content = result.getResponse().getContentAsString();
		assertTrue(content.contains("Имя не может быть меньше 4 символов"));
	}

	@Test
	void createTask_whenTitleIsFourCharacters() throws Exception {
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", "abcd",
						"description", "New Description",
						"status", "ACTIVE")
		);

		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated());
	}

	@Test
	void createTask_whenTitleIsTooLong() throws Exception {
		String longTitle = "A".repeat(256);
		String requestBody = String.format("{\"title\":\"%s\",\"description\":\"New Description\",\"status\":\"ACTIVE\"}", longTitle);

		MvcResult result = mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andReturn();

		int status = result.getResponse().getStatus();
		assertTrue(status == 500 || status == 400, "Expected status code 500 or 400, but got " + status);

		String content = result.getResponse().getContentAsString();
		assertTrue(content.contains("TITLE CHARACTER VARYING(255)") || content.contains("Data truncation") || content.contains("constraint violation") || content.contains("Имя не может быть больше 255 символов"), "Content was: " + content);
	}

	@Test
	void createTask_whenTitleIsEmpty() throws Exception {
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", "",
						"description", "Valid Description",
						"status", "ACTIVE")
		);

		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest());

	}

	@Test
	void createTask_whenTitleIsOnlyWhitespace() throws Exception {
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", "   ",
						"description", "Valid Description",
						"status", "ACTIVE")
		);

		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createTask_whenDeadlineIsInPast() throws Exception {
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", "Past Deadline Task",
						"description", "Task with deadline in the past",
						"status", "ACTIVE",
						"deadline", LocalDate.now().minusDays(1).toString())
		);

		MvcResult result = mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andReturn();

		assertEquals(400, result.getResponse().getStatus());

		assertEquals(MediaType.APPLICATION_JSON_VALUE, result.getResponse().getContentType());

		String content = result.getResponse().getContentAsString();
		assertTrue(content.contains("Дэдлайн не может быть раньше настоящего времени"));
	}

	@Test
	void createTask_whenDeadlineIsToday() throws Exception {
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", "Today Deadline Task",
						"description", "Task with deadline today",
						"status", "ACTIVE",
						"deadline", LocalDate.now().toString())
		);

		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated());
	}

	@Test
	void createTask_whenDeadlineIsInFuture() throws Exception {
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", "Future Deadline Task",
						"description", "Task with deadline in the future",
						"status", "ACTIVE",
						"deadline", LocalDate.now().plusDays(1).toString())
		);

		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated());
	}

	@Test
	void createTask_whenStatusIsInvalid() throws Exception {
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", "Valid Title",
						"description", "Valid Description",
						"status", "INVALID_STATUS")
		);

		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createTask_shouldAcceptValidStatus() throws Exception {
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", "Valid Title",
						"description", "Valid Description",
						"status", "ACTIVE")
		);

		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated());
	}

	@Test
	void createTask_whenPriorityIsInvalid() throws Exception {
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", "Valid Title",
						"description", "Valid Description",
						"status", "ACTIVE",
						"priority", "INVALID_PRIORITY")
		);

		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createTask_shouldAcceptValidPriority() throws Exception {
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", "Valid Title",
						"description", "Valid Description",
						"status", "ACTIVE",
						"priority", "HIGH")
		);

		MvcResult result = mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andReturn();

		assertEquals(201, result.getResponse().getStatus());
	}

	@Test
	void createTask_shouldAcceptNullPriority() throws Exception {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("title", "Valid Title");
		requestBody.put("description", "Valid Description");
		requestBody.put("status", "ACTIVE");
		requestBody.put("priority", null);

		MvcResult result = mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody.toString()))
				.andExpect(status().isCreated())
				.andReturn();

		assertEquals(201, result.getResponse().getStatus());
	}

	@Test
	void createTask_shouldSetCriticalPriorityFromMacro() throws Exception {
		String title = "Task with !1 priority";
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", title,
						"description", "Та самая",
						"status", "ACTIVE")
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(MockMvcResultMatchers.status().isCreated());

		List<Task> tasks = taskRepository.findAll();
		System.out.println("Tasks in database: ");
		for (Task task : tasks) {
			System.out.println("  Title: " + task.getTitle() + ", Priority: " + task.getPriority()  + ", Status: " + task.getStatus()  + ", Deadline: " + task.getDeadline()  + ", Description: " + task.getDescription());
		}
		Task createdTask = tasks.stream()
				.filter(task -> task.getTitle().equals("Task with  priority"))
				.findFirst()
				.orElse(null);
		assertNotNull(createdTask);
		assertEquals(Priority.CRITICAL, createdTask.getPriority());
	}


	@Test
	void createTask_shouldSetHighPriorityFromMacro() throws Exception {
		String title = "Task with !2 priority";
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", title,
						"description", "Valid Description",
						"status", "ACTIVE")
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(MockMvcResultMatchers.status().isCreated());

		List<Task> tasks = taskRepository.findAll();
		System.out.println("Tasks in database: ");
		for (Task task : tasks) {
			System.out.println("  Title: " + task.getTitle() + ", Priority: " + task.getPriority()  + ", Status: " + task.getStatus()  + ", Deadline: " + task.getDeadline()  + ", Description: " + task.getDescription());
		}
		Task createdTask = tasks.stream()
				.filter(task -> task.getTitle().equals("Task with  priority"))
				.findFirst()
				.orElse(null);
		assertNotNull(createdTask);
		assertEquals(Priority.HIGH, createdTask.getPriority());
	}

	@Test
	void createTask_shouldSetMediumPriorityFromMacro() throws Exception {
		String title = "Task with !3 priority";
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", title,
						"description", "Valid Description",
						"status", "ACTIVE")
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(MockMvcResultMatchers.status().isCreated());

		List<Task> tasks = taskRepository.findAll();
		System.out.println("Tasks in database: ");
		for (Task task : tasks) {
			System.out.println("  Title: " + task.getTitle() + ", Priority: " + task.getPriority()  + ", Status: " + task.getStatus()  + ", Deadline: " + task.getDeadline()  + ", Description: " + task.getDescription());
		}
		Task createdTask = tasks.stream()
				.filter(task -> task.getTitle().equals("Task with  priority"))
				.findFirst()
				.orElse(null);
		assertNotNull(createdTask);
		assertEquals(Priority.MEDIUM, createdTask.getPriority());
	}

	@Test
	void createTask_shouldSetLowPriorityFromMacro() throws Exception {
		String title = "Task with !4 priority";
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", title,
						"description", "Valid Description",
						"status", "ACTIVE")
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(MockMvcResultMatchers.status().isCreated());

		List<Task> tasks = taskRepository.findAll();
		System.out.println("Tasks in database: ");
		for (Task task : tasks) {
			System.out.println("  Title: " + task.getTitle() + ", Priority: " + task.getPriority()  + ", Status: " + task.getStatus()  + ", Deadline: " + task.getDeadline()  + ", Description: " + task.getDescription());
		}
		Task createdTask = tasks.stream()
				.filter(task -> task.getTitle().equals("Task with  priority"))
				.findFirst()
				.orElse(null);
		assertNotNull(createdTask);
		assertEquals(Priority.LOW, createdTask.getPriority());
	}

	@Test
	void createTask_shouldPrioritizeFormFieldOverMacro() throws Exception {
		String title = "Task with !1 priority";
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", title,
						"description", "Valid Description",
						"status", "ACTIVE",
						"priority", "HIGH")
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(MockMvcResultMatchers.status().isCreated());

		List<Task> tasks = taskRepository.findAll();
		System.out.println("Tasks in database: ");
		for (Task task : tasks) {
			System.out.println("  Title: " + task.getTitle() + ", Priority: " + task.getPriority()  + ", Status: " + task.getStatus()  + ", Deadline: " + task.getDeadline()  + ", Description: " + task.getDescription());
		}
		Task createdTask = tasks.stream()
				.filter(task -> task.getTitle().equals("Task with  priority"))
				.findFirst()
				.orElse(null);
		assertNotNull(createdTask);
		assertEquals(Priority.HIGH, createdTask.getPriority());
	}

	@Test
	void createTask_shouldSetDeadlineFromMacroWithDots() throws Exception {
		String title = "Task !before 16.10.2026 deadline";
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", title,
						"description", "Valid Description",
						"status", "ACTIVE")
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(MockMvcResultMatchers.status().isCreated());

		List<Task> tasks = taskRepository.findAll();
		System.out.println("Tasks in database: ");
		for (Task task : tasks) {
			System.out.println("  Title: " + task.getTitle() + ", Priority: " + task.getPriority()  + ", Status: " + task.getStatus()  + ", Deadline: " + task.getDeadline()  + ", Description: " + task.getDescription());
		}
		Task createdTask = tasks.stream()
				.filter(task -> task.getTitle().equals("Task  deadline"))
				.findFirst()
				.orElse(null);
		assertNotNull(createdTask);
		assertEquals(LocalDate.of(2026, 10, 16), createdTask.getDeadline());
	}

	@Test
	void createTask_shouldSetDeadlineFromMacroWithDashes() throws Exception {
		String title = "Task !before 16-10-2026 deadline";
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", title,
						"description", "Valid Description",
						"status", "ACTIVE")
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(MockMvcResultMatchers.status().isCreated());

		List<Task> tasks = taskRepository.findAll();
		System.out.println("Tasks in database: ");
		for (Task task : tasks) {
			System.out.println("  Title: " + task.getTitle() + ", Priority: " + task.getPriority()  + ", Status: " + task.getStatus()  + ", Deadline: " + task.getDeadline()  + ", Description: " + task.getDescription());
		}
		Task createdTask = tasks.stream()
				.filter(task -> task.getTitle().equals("Task  deadline"))
				.findFirst()
				.orElse(null);
		assertNotNull(createdTask);
		assertEquals(LocalDate.of(2026, 10, 16), createdTask.getDeadline());
	}

	@Test
	void createTask_shouldPrioritizeFormFieldDeadlineOverMacro() throws Exception {
		String title = "Task !before 15.02.2024 deadline";
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", title,
						"description", "Valid Description",
						"status", "ACTIVE",
						"deadline", "2026-10-16")
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(MockMvcResultMatchers.status().isCreated());

		List<Task> tasks = taskRepository.findAll();
		System.out.println("Tasks in database: ");
		for (Task task : tasks) {
			System.out.println("  Title: " + task.getTitle() + ", Priority: " + task.getPriority()  + ", Status: " + task.getStatus()  + ", Deadline: " + task.getDeadline()  + ", Description: " + task.getDescription());
		}
		Task createdTask = tasks.stream()
				.filter(task -> task.getTitle().equals("Task  deadline"))
				.findFirst()
				.orElse(null);
		assertNotNull(createdTask);
		assertEquals(LocalDate.of(2026, 10, 16), createdTask.getDeadline());
	}

	@Test
	void createTask_shouldNotSetPriorityFromMacroIfInvalid() throws Exception {
		String title = "Task with !5 priority";
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", title,
						"description", "Valid Description",
						"status", "ACTIVE")
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(MockMvcResultMatchers.status().isCreated());

		List<Task> tasks = taskRepository.findAll();
		System.out.println("Tasks in database: ");
		for (Task task : tasks) {
			System.out.println("  Title: " + task.getTitle() + ", Priority: " + task.getPriority()  + ", Status: " + task.getStatus()  + ", Deadline: " + task.getDeadline()  + ", Description: " + task.getDescription());
		}
		Task createdTask = tasks.stream()
				.filter(task -> task.getTitle().equals("Task with !5 priority"))
				.findFirst()
				.orElse(null);
		assertNotNull(createdTask);
	}

	@Test
	void createTask_shouldNotSetDeadlineFromMacroIfInvalidFormat() throws Exception {
		String title = "Task !before 15/02/2024 deadline";
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", title,
						"description", "Valid Description",
						"status", "ACTIVE")
		);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(MockMvcResultMatchers.status().isCreated());

		List<Task> tasks = taskRepository.findAll();
		System.out.println("Tasks in database: ");
		for (Task task : tasks) {
			System.out.println("  Title: " + task.getTitle() + ", Priority: " + task.getPriority()  + ", Status: " + task.getStatus()  + ", Deadline: " + task.getDeadline()  + ", Description: " + task.getDescription());
		}
		Task createdTask = tasks.stream()
				.filter(task -> task.getTitle().equals("Task !before 15/02/2024 deadline"))
				.findFirst()
				.orElse(null);
		assertNotNull(createdTask);
	}

}
