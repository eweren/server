package io.polygloat.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.polygloat.dtos.response.KeyResponseDTO;
import io.polygloat.dtos.response.ViewDataResponse;
import io.polygloat.dtos.response.translations_view.ResponseParams;
import io.polygloat.helpers.JsonHelper;
import io.polygloat.model.Repository;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testng.annotations.Test;

import java.util.LinkedHashSet;
import java.util.Map;

import static io.polygloat.controllers.LoggedRequestFactory.loggedGet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TranslationControllerTest extends SignedInControllerTest {

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void getViewDataSearch() throws Exception {
        Repository app = dbPopulator.populate(generateUniqueString());

        String searchString = "This";

        ViewDataResponse<LinkedHashSet<KeyResponseDTO>, ResponseParams> response = performValidViewRequest(app, "?search=" + searchString);
        assertThat(response.getData().size()).isGreaterThan(0);
        assertSearch(response, searchString);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void getViewDataQueryLanguages() throws Exception {
        Repository repository = dbPopulator.populate(generateUniqueString());

        ViewDataResponse<LinkedHashSet<KeyResponseDTO>, ResponseParams> response = performValidViewRequest(repository, "?languages=en");

        assertThat(response.getData().size()).isGreaterThan(8);

        for (KeyResponseDTO item : response.getData()) {
            assertThat(item.getTranslations()).doesNotContainKeys("de");
        }

        performGetDataForView(repository.getId(), "?languages=langNotExists").andExpect(status().isNotFound());

        //with same language multiple times
        ViewDataResponse<LinkedHashSet<KeyResponseDTO>, ResponseParams> response4 = performValidViewRequest(repository, "?languages=en,en");
    }

    private ViewDataResponse<LinkedHashSet<KeyResponseDTO>, ResponseParams> performValidViewRequest(Repository repository, String queryString) throws Exception {
        MvcResult mvcResult = performGetDataForView(repository.getId(), queryString).andExpect(status().isOk()).andReturn();

        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    @Test
    void getViewDataQueryPagination() throws Exception {
        Repository repository = dbPopulator.populate(generateUniqueString());

        int limit = 5;

        ViewDataResponse<LinkedHashSet<KeyResponseDTO>, ResponseParams> response = performValidViewRequest(repository, String.format("?limit=%d", limit));

        assertThat(response.getData().size()).isEqualTo(limit);
        assertThat(response.getPaginationMeta().getAllCount()).isEqualTo(12);
        assertThat(response.getPaginationMeta().getOffset()).isEqualTo(0);


        int offset = 3;

        ViewDataResponse<LinkedHashSet<KeyResponseDTO>, ResponseParams> responseOffset = performValidViewRequest(repository, String.format("?limit=%d&offset=%d", limit, offset));

        assertThat(responseOffset.getData().size()).isEqualTo(limit);
        assertThat(responseOffset.getPaginationMeta().getOffset()).isEqualTo(offset);

        response.getData().stream().limit(offset).forEach(i -> assertThat(responseOffset.getData()).doesNotContain(i));

        response.getData().stream().skip(offset).forEach(i -> {
            assertThat(responseOffset.getData()).contains(i);
        });
    }

    @Test
    void getViewDataMetadata() throws Exception {
        Repository repository = dbPopulator.populate(generateUniqueString());
        int limit = 5;
        ViewDataResponse<LinkedHashSet<KeyResponseDTO>, ResponseParams> response = performValidViewRequest(repository, String.format("?limit=%d", limit));

        assertThat(response.getParams().getLanguages()).contains("en", "de");
    }

    private static void assertSearch(ViewDataResponse<LinkedHashSet<KeyResponseDTO>, ResponseParams> response, String searchString) {
        for (KeyResponseDTO item : response.getData()) {
            assertThat(JsonHelper.asJsonString(item)).contains(searchString);
        }
    }

    @Test
    void getTranslations() throws Exception {
        Repository repository = dbPopulator.populate(generateUniqueString());

        MvcResult mvcResult = mvc.perform(
                loggedGet("/api/repository/" + repository.getId() + "/translations/en,de")
                        .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();

        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> result = mapper.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<>() {
        });

        assertThat(result).containsKeys("en", "de");
    }

    private ResultActions performGetDataForView(Long repositoryId, String queryString) throws Exception {
        return mvc.perform(
                loggedGet("/api/repository/" + repositoryId + "/translations/view" + queryString)
                        .contentType(MediaType.APPLICATION_JSON));
    }

}
