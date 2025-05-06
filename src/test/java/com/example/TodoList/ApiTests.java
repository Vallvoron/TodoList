package com.example.TodoList;

import com.example.TodoList.models.Priority;
import com.example.TodoList.entities.Task;
import com.example.TodoList.models.Status;
import com.example.TodoList.models.TaskRequest;
import com.example.TodoList.repositories.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
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
	void create_TitleTooLong() throws Exception {//и длиннее чем должно быть
		String longTitle = "A".repeat(256);
		String requestBody = String.format("{\"title\":\"%s\",\"description\":\"New Description\",\"status\":\"ACTIVE\"}", longTitle);

		MvcResult result = mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andReturn();

		assertEquals(400, result.getResponse().getStatus());
		assertEquals(MediaType.APPLICATION_JSON_VALUE, result.getResponse().getContentType());
		String content = result.getResponse().getContentAsString();
		assertTrue(content.contains("Имя не может быть больше 255 символов"));
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

	//создание со статусом
	@ParameterizedTest
	@ValueSource(strings = {
			"ACTIVE,201",
			"COMPLETED,201",
			"INVALID_STATUS,400"
	})
	void create_StatusValidation(String testData) throws Exception {
		// Arrange
		String[] data = testData.split(",");
		String statusValue = data[0];
		int expectedStatus = Integer.parseInt(data[1]);

		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", "Valid Title",
						"description", "Valid Description",
						"status", statusValue)
		);

		// Act & Assert
		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().is(expectedStatus));
	}

	//проверка значений приоритета
	@ParameterizedTest
	@ValueSource(strings = {
			"CRITICAL,201",
			"HIGH,201",
			"MEDIUM,201",
			"LOW,201",
			"null,201",
			"INVALID_PRIORITY,400"
	})
	void create_PriorityValidation(String testData) throws Exception {
		String[] data = testData.split(",");
		String priorityValue = data[0];
		int expectedStatus = Integer.parseInt(data[1]);

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("title", "Valid Title");
		requestBody.put("description", "Valid Description");
		requestBody.put("status", "ACTIVE");

		if (!priorityValue.equals("null")) {
			requestBody.put("priority", priorityValue);
		} else {
			requestBody.put("priority", null);
		}

		String requestBodyJson = objectMapper.writeValueAsString(requestBody);
		mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBodyJson))
				.andExpect(status().is(expectedStatus));
	}

	//проверки правильной и неправильной установки макросов

	@ParameterizedTest
	@ValueSource(strings = {
			"Task with !1 priority,CRITICAL",
			"Task with !2 priority,HIGH",
			"Task with !3 priority,MEDIUM",
			"Task with !4 priority,LOW",
			"Task with !5 priority,MEDIUM"
	})
	void create_PriorityFromMacro(String testData) throws Exception {
		// Arrange
		String[] data = testData.split(",");
		String title = data[0];
		Priority expectedPriority = Priority.valueOf(data[1]);

		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", title,
						"description", "Valid Description",
						"status", "ACTIVE")
		);

		MvcResult result = mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(MockMvcResultMatchers.jsonPath("$.id").exists())
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		Task createdTask = objectMapper.readValue(responseBody, Task.class);

		assertNotNull(createdTask);
		assertEquals(expectedPriority, createdTask.getPriority());
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

		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		Task createdTask = objectMapper.readValue(responseBody, Task.class);
		assertNotNull(createdTask);
		assertEquals(Priority.HIGH, createdTask.getPriority());
	}

	//правильные и неправильные форматы дедлайна
	@ParameterizedTest
	@ValueSource(strings = {
			"Task !before 16.10.2026 deadline,2026-10-16",
			"Task !before 16-10-2026 deadline,2026-10-16",
			"Task !before 32.02.2027 deadline,null",
			"Task !before 29.02.2027 deadline,2027-02-28",
			"Task !before 16/10/2025 deadline,null"
	})
	void create_DeadlineFromMacro(String testData) throws Exception {
		// Arrange
		String[] data = testData.split(",");
		String title = data[0];
		String expectedDate = data[1];

		Map<String, Object> requestBody = Map.of(
				"title", title,
				"description", "Valid Description",
				"status", "ACTIVE"
		);

		MvcResult result = mockMvc.perform(post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(requestBody)))
				.andExpect(status().isCreated())
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		Task createdTask = objectMapper.readValue(responseBody, Task.class);

		assertNotNull(createdTask);

		if (expectedDate != null && !expectedDate.equals("null")) {
			assertEquals(LocalDate.parse(expectedDate), createdTask.getDeadline());
		} else {
			assertNull(createdTask.getDeadline());
		}
	}

	// и такая же проверка приоритета поля
	@Test
	void create_PrioritizeFormFieldDeadlineOverMacro() throws Exception {
		String title = "Task !before 16.10.2025 deadline";
		String requestBody = objectMapper.writeValueAsString(
				Map.of("title", title,
						"description", "Valid Description",
						"status", "ACTIVE",
						"deadline", "2026-10-16")
		);

		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/tasks")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		Task createdTask = objectMapper.readValue(responseBody, Task.class);
		assertNotNull(createdTask);
		assertEquals(LocalDate.of(2026, 10, 16), createdTask.getDeadline());
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

    //проверки сортировки, по имени и приоритету, что выдаст 1м
    @Test
    void getAll_DefaultSorting() throws Exception {
        Task task1 = new Task();
        task1.setTitle("Task B");
        task1.setDescription("Description");
        task1.setStatus(Status.ACTIVE);
        task1.setCreatedAt(LocalDateTime.now().minusDays(2));
        taskRepository.save(task1);

        Task task2 = new Task();
        task2.setTitle("Task A");
        task2.setDescription("Description");
        task2.setStatus(Status.ACTIVE);
        task2.setCreatedAt(LocalDateTime.now().minusDays(1));
        taskRepository.save(task2);

        MvcResult result = mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Task[] tasks = objectMapper.readValue(responseBody, Task[].class);

        assertEquals(2, tasks.length);
        assertEquals("Task B", tasks[0].getTitle());
        assertEquals("Task A", tasks[1].getTitle());
    }

    @Test
    void getAll_SortByTitleAsc() throws Exception {
        Task task1 = new Task();
        task1.setTitle("Task B");
        task1.setDescription("Description");
        task1.setStatus(Status.ACTIVE);
        taskRepository.save(task1);

        Task task2 = new Task();
        task2.setTitle("Task A");
        task2.setDescription("Description");
        task2.setStatus(Status.ACTIVE);
        taskRepository.save(task2);

        MvcResult result = mockMvc.perform(get("/api/tasks")
                        .param("sortBy", "title")
                        .param("sortDirection", "ASC"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Task[] tasks = objectMapper.readValue(responseBody, Task[].class);

        assertEquals(2, tasks.length);
        assertEquals("Task A", tasks[0].getTitle());
        assertEquals("Task B", tasks[1].getTitle());
    }

    @Test
    void getAll_SortByPriorityDesc() throws Exception {
        Task task1 = new Task();
        task1.setTitle("Task A");
        task1.setDescription("Description");
        task1.setStatus(Status.ACTIVE);
        task1.setPriority(Priority.HIGH);
        taskRepository.save(task1);

        Task task2 = new Task();
        task2.setTitle("Task B");
        task2.setDescription("Description");
        task2.setStatus(Status.ACTIVE);
        task2.setPriority(Priority.LOW);
        taskRepository.save(task2);

        MvcResult result = mockMvc.perform(get("/api/tasks")
                        .param("sortBy", "priority")
                        .param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Task[] tasks = objectMapper.readValue(responseBody, Task[].class);

        assertEquals(2, tasks.length);
        assertEquals(Priority.LOW, tasks[0].getPriority());
        assertEquals(Priority.HIGH, tasks[1].getPriority());
    }
}
