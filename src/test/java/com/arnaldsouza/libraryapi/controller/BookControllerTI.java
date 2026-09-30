package com.arnaldsouza.libraryapi.controller;

import com.arnaldsouza.libraryapi.AbstractIntegrationTest;
import com.arnaldsouza.libraryapi.entity.Book;
import com.arnaldsouza.libraryapi.entity.Role;
import com.arnaldsouza.libraryapi.entity.User;
import com.arnaldsouza.libraryapi.repository.BookRepository;
import com.arnaldsouza.libraryapi.repository.UserRepository;
import com.arnaldsouza.libraryapi.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();

        userToken = createUserAndToken("it-user", Role.USER);
        adminToken = createUserAndToken("it-admin", Role.ADMIN);

        bookRepository.save(buildBook(
                "Clean Code", "Robert C. Martin", "9780132350884",
                2008, "Software", 5));
        bookRepository.save(buildBook(
                "The Pragmatic Programmer", "Andrew Hunt", "9780201616224",
                1999, "Career", 7));
    }

    private String createUserAndToken(String username, Role role) {
        User user = userRepository.findByUsername(username).orElseGet(() -> {
            User created = new User();
            created.setUsername(username);
            created.setPassword(passwordEncoder.encode("test-password"));
            created.setRole(role);
            return userRepository.save(created);
        });
        return jwtService.generateToken(user.getUsername(), user.getRole().name());
    }

    private Book buildBook(String title, String author, String isbn,
                           Integer year, String genre, Integer copies) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setIsbn(isbn);
        book.setPublishedYear(year);
        book.setGenre(genre);
        book.setAvailableCopies(copies);
        return book;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    @DisplayName("GET /books without authentication should be rejected")
    void listWithoutAuthShouldBeRejected() throws Exception {
        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /books as USER should return the paginated list")
    void listAsUserShouldReturnBooks() throws Exception {
        mockMvc.perform(get("/api/v1/books")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /books with author filter should narrow the results")
    void listWithAuthorFilterShouldNarrowResults() throws Exception {
        mockMvc.perform(get("/api/v1/books")
                        .param("author", "martin")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"));
    }

    @Test
    @DisplayName("GET /books/{id} with unknown id should return 404")
    void getUnknownIdShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/books/999999")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /books as USER should be forbidden")
    void createAsUserShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", bearer(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /books as ADMIN should create the book")
    void createAsAdminShouldSucceed() throws Exception {
        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Refactoring"));
    }

    @Test
    @DisplayName("POST /books with invalid payload should return 400 with field errors")
    void createWithInvalidPayloadShouldReturnBadRequest() throws Exception {
        String payload = """
                {
                  "title": "",
                  "author": "Someone",
                  "isbn": "123",
                  "availableCopies": -5
                }
                """;

        mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists())
                .andExpect(jsonPath("$.fields.availableCopies").exists());
    }

    @Test
    @DisplayName("DELETE /books/{id} as USER should be forbidden")
    void deleteAsUserShouldBeForbidden() throws Exception {
        Long id = bookRepository.findAll().getFirst().getId();

        mockMvc.perform(delete("/api/v1/books/" + id)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /books/{id} as ADMIN should remove the book")
    void deleteAsAdminShouldSucceed() throws Exception {
        Long id = bookRepository.findAll().getFirst().getId();

        mockMvc.perform(delete("/api/v1/books/" + id)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    private String validPayload() {
        return """
                {
                  "title": "Refactoring",
                  "author": "Martin Fowler",
                  "isbn": "9780134757599",
                  "publishedYear": 2018,
                  "genre": "Software",
                  "availableCopies": 4
                }
                """;
    }
}