package com.example.TodoList;

import com.example.TodoList.models.Priority;
import com.example.TodoList.entities.Task;
import com.example.TodoList.models.Status;
import com.example.TodoList.models.TaskRequest;
import com.example.TodoList.repositories.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class ApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private TaskRepository taskRepository;

	@BeforeEach
	void setup() {
		taskRepository.deleteAll();
	}

	@Test
	void getAll_Ok() throws Exception { //просто тест запроса
		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	void create_TitleTooShort() throws Exception {//проверки имен задач, короче чем нужно
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
	void create_TitleFourCharacters() throws Exception {//ровно нужная длина
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
	void create_TitleTooLong() throws Exception {//и длиннее чем должно быть(из за SQL запроса тест не проводится)
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
	void create_TitleEmpty() throws Exception {//пустой (очевидно меньше 4 символов)
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
	void create_TitleOnlyWhitespace() throws Exception {//чисто из пробелов(не должен работать)
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", "        ",
						"description", "Valid Description",
						"status", "ACTIVE")
		);

		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest());
	}

	@Test
	void create_DeadlineInPast() throws Exception {//теперь проверки дедлайнов, в прошлом настоящем(сегодня) и будущем
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
	void create_DeadlineToday() throws Exception {
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
	void create_DeadlineInFuture() throws Exception {
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
	void create_StatusInvalid() throws Exception {//проверки возможных значений для статусов(неправильная не пройдет)
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
	void create_ValidStatus() throws Exception {
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
	void create_PriorityInvalid() throws Exception {//и то же самое для приоритета
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
	void create_ValidPriority() throws Exception {
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", "Valid Title",
						"description", "Valid Description",
						"status", "ACTIVE",
						"priority", "HIGH")
		);

		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andReturn();
	}

	@Test
	void create_NullPriority() throws Exception {
		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("title", "Valid Title");
		requestBody.put("description", "Valid Description");
		requestBody.put("status", "ACTIVE");
		requestBody.put("priority", null);

		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody.toString()))
				.andExpect(status().isBadRequest());
	}

	//проверки правильной установки макросов(пока возможные значения)
	@Test
	void create_CriticalPriorityFromMacro() throws Exception {
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
	void create_HighPriorityFromMacro() throws Exception {
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
	void create_MediumPriorityFromMacro() throws Exception {
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
	void create_LowPriorityFromMacro() throws Exception {
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

	//проверка приоритета поля над макросом
	@Test
	void create_PrioritizeFormFieldOverMacro() throws Exception {
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

	//правильные и неправильные форматы дедлайна
	@Test
	void create_DeadlineFromMacroWithDots() throws Exception {
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
	void create_DeadlineFromMacroWithDashes() throws Exception {
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

	// и такая же проверка приоритета поля
	@Test
	void create_PrioritizeFormFieldDeadlineOverMacro() throws Exception {
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

	//неправильные значения приоритета
	@Test
	void create_PriorityFromMacroInvalid() throws Exception {
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
	void create_DeadlineFromMacroInvalid() throws Exception {
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

	//и оставшиеся функции
	@Test
	void getById_ExistingTask() throws Exception {
		Task task = new Task();
		task.setTitle("Test Task");
		task.setDescription("Description");
		task.setStatus(Status.ACTIVE);
		Task savedTask = taskRepository.save(task);
		UUID taskId = savedTask.getId();

		mockMvc.perform(get("/api/tasks/" + taskId))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.title").value("Test Task"));
	}

	@Test
	void getById_NonExistingTask() throws Exception {
		UUID nonExistingId = UUID.randomUUID();
		mockMvc.perform(get("/api/tasks/" + nonExistingId))
				.andExpect(status().isNotFound());
	}

	@Test
	void update_ExistingTask() throws Exception {
		Task task = new Task();
		task.setTitle("Original Title");
		task.setDescription("Original Description");
		task.setStatus(Status.ACTIVE);
		task.setPriority(Priority.HIGH);
		task.setDeadline(LocalDate.now());
		Task savedTask = taskRepository.save(task);
		UUID taskId = savedTask.getId();

		TaskRequest taskDetails = new TaskRequest();
		taskDetails.setTitle("Updated Title");
		taskDetails.setDescription("Updated Description");
		taskDetails.setStatus(Status.COMPLETED);

		String requestBody = objectMapper.writeValueAsString(taskDetails);

		mockMvc.perform(put("/api/tasks")
						.param("id", taskId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.title").value("Updated Title"))
				.andExpect(jsonPath("$.description").value("Updated Description"))
				.andExpect(jsonPath("$.status").value("COMPLETED"));

		Task updatedTask = taskRepository.findById(taskId).orElse(null);
		assertNotNull(updatedTask);
		assertEquals("Updated Title", updatedTask.getTitle());
		assertEquals("Updated Description", updatedTask.getDescription());
		assertEquals(Status.COMPLETED, updatedTask.getStatus());

	}

	@Test
	void update_NonExistingTask() throws Exception {

		TaskRequest taskDetails = new TaskRequest();
		taskDetails.setTitle("Updated Title");
		taskDetails.setDescription("Updated Description");
		taskDetails.setStatus(Status.ACTIVE);

		String requestBody = objectMapper.writeValueAsString(taskDetails);

		UUID nonExistentId = UUID.randomUUID();
		mockMvc.perform(put("/api/tasks")
						.param("id", nonExistentId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Задача не найдена"));

	}
	@Test
	void update_TaskWithShortTitle() throws Exception {
		// Arrange: Create a task and save it
		Task task = new Task();
		task.setTitle("Initial Title");
		task.setDescription("Initial Description");
		task.setStatus(Status.ACTIVE);
		Task savedTask = taskRepository.save(task);
		UUID taskId = savedTask.getId();

		TaskRequest taskDetails = new TaskRequest();
		taskDetails.setTitle("abc");
		taskDetails.setDescription("Description");
		taskDetails.setStatus(Status.ACTIVE);
		String requestBody = objectMapper.writeValueAsString(taskDetails);

		mockMvc.perform(put("/api/tasks")
						.param("id", taskId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Имя не может быть меньше 4 символов"));
	}

	@Test
	void delete_ExistingTask() throws Exception {

		Task task = new Task();
		task.setTitle("Task to Delete");
		task.setDescription("Description");
		task.setStatus(Status.ACTIVE);
		Task savedTask = taskRepository.save(task);
		UUID taskId = savedTask.getId();

		mockMvc.perform(MockMvcRequestBuilders.delete("/api/tasks")
						.param("id", taskId.toString()))
				.andExpect(status().isOk());

		assertFalse(taskRepository.existsById(taskId));
	}

	@Test
	void delete_NonExistingTask() throws Exception {
		UUID nonExistingId = UUID.randomUUID();
		mockMvc.perform(MockMvcRequestBuilders.delete("/api/tasks")
						.param("id", nonExistingId.toString()))
				.andExpect(status().isNotFound());
	}
}
