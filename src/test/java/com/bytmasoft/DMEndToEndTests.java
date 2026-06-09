package com.bytmasoft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytmasoft.dm.dto.response.UploadEntityResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.DocumentType;

@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class DMEndToEndTests {

  @LocalServerPort
  private int port;

  @Autowired
  private WebClient.Builder webClientBuilder;

  private WebClient webClient;
  String jwtToken;

  private final Long documentId = 1L;
  @Autowired
  private WebTestClient webTestClient;


  @BeforeEach
  void setUp() {
    String baseUrl = "http://localhost:" + port + "/dss/api/v1";
    this.webClient = webClientBuilder
        .baseUrl(baseUrl)
        .build();
    jwtToken = JwtTokenUtil.generateToken();
  }


  @Test
  @Order(2)
  void testGetDocumentById() {
    webTestClient.get()
        .uri("/documents/{documentId}", documentId)
        .header("Authorization", "Bearer " + jwtToken)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBody(UploadEntityResponse.class)
        .value(document -> {
          assertNotNull(document);
          assertEquals(documentId, document.getId());
        });
  }

  @Test
  @Order(1)
  void testUploadDocument() throws IOException {
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("file", new ClassPathResource("test-files/IMG_6445.jpg"))
        .header("Content-Disposition", "form-data; name=file; filename=IMG_6445.jpg");
    builder.part("uploadType", DocumentType.PDF.getDescription());
    builder.part("ownerId", "1");

    webTestClient.post()
        .uri("/documents/upload")
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .bodyValue(builder.build())
        .exchange()
        .expectStatus().isOk()
        .expectBody(UploadEntityResponse.class)
        .value(document -> {
          assertNotNull(document);
          assertEquals("user_id_1_profile_picture_v4.jpg", document.getFileName());
        });
  }

  @Test
  @Order(3)
  void testGetAllDocuments() {
    webTestClient.get()
        .uri(uriBuilder -> uriBuilder.path("/documents")
            .queryParam("ownerId", "1")
            .queryParam("page", 0)
            .queryParam("size", 5)
            .build())
        .header("Authorization", "Bearer " + jwtToken)
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBody(new ParameterizedTypeReference<PagedModel<UploadEntityResponse>>() {
        })
        .value(page -> assertTrue(page.getContent().isEmpty()));
  }

  @Test
  @Order(4)
  void testDownloadDocument() {
    Long documentId = 2L; // Assume a document exists with this ID for testing

    webTestClient.get()
        .uri("/documents/{documentId}/download", documentId)
        .header("Authorization", "Bearer " + jwtToken)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().value(HttpHeaders.CONTENT_TYPE, contentType -> assertTrue(
            contentType.equals("application/octet-stream") || contentType.equals("image/jpeg")))
        .expectHeader().value(HttpHeaders.CONTENT_DISPOSITION, disposition ->
            assertTrue(
                disposition.contains("attachment; filename=\"user_id_4_birth_certificate_v1.jpg")))
        .expectBody(byte[].class)
        .value(body -> assertTrue(body.length > 0));
  }


  @Test
  @Order(5)
  void testUploadDocuments() throws Exception {
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    // Add multiple files
    builder.part("files", new ClassPathResource("test-files/logo_1.png"));
    builder.part("files", new ClassPathResource("test-files/logo_5.png"));

    // Add document types corresponding to each file
    //builder.part("documentTypes", DocumentType.BIRTH_CERTIFICATE.toString());
    //builder.part("documentTypes", DocumentType.PROFILE_PICTURE.toString());

    // Add the owner ID
    builder.part("ownerId", "1");

    webTestClient.post()
        .uri("/documents/uploads/files")
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .bodyValue(builder.build())
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(UploadEntityResponse.class)
        .value(documents -> {
          assertNotNull(documents);
          assertEquals(2, documents.size());
          assertEquals("user_id_1_birth_certificate_v5.png", documents.get(0).getFileName());
          assertEquals("user_id_1_profile_picture_v9.png", documents.get(1).getFileName());
        });
  }


  @Test
  @Order(6)
  void testDeleteDocument() {
    Long documentId = 1L; // Assume document with this ID exists

    webTestClient.delete()
        .uri("/documents/{documentId}", documentId)
        .header("Authorization", "Bearer " + jwtToken)
        .exchange()
        .expectStatus().isOk()
        .expectBody(Boolean.class)
        .value(deleted -> assertTrue(deleted));
  }


}
