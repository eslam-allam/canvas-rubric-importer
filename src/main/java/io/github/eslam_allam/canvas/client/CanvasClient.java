package io.github.eslam_allam.canvas.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.eslam_allam.canvas.domain.DynamicURIBuilder;
import io.github.eslam_allam.canvas.model.canvas.Assignment;
import io.github.eslam_allam.canvas.model.canvas.Assignment.GradingType;
import io.github.eslam_allam.canvas.model.canvas.Course;
import io.github.eslam_allam.canvas.model.canvas.RubricModels;
import io.github.eslam_allam.canvas.request.AssignmentPointsUpdateRequest;
import io.github.eslam_allam.canvas.request.RequestWrapper;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;

public final class CanvasClient {

    private enum ResourceType {
        COURSES("courses"),
        ASSIGNMENTS("assignments");
        private String type;

        private ResourceType(String type) {
            this.type = type;
        }

        public String type() {
            return this.type;
        }
    }

    private final DynamicURIBuilder baseApi;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public CanvasClient(CanvasCredentialProvider provider) {
        this.baseApi = DynamicURIBuilder.of(provider::loadBaseUrl).appendPath("/api/v1");
        this.httpClient = HttpClientBuilder.create()
                .addRequestInterceptorFirst((request, entity, context) ->
                        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + provider.loadToken()))
                .setDefaultHeaders(
                        List.of(new BasicHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public void updateAssignmentPoints(String courseId, String assignmentId, double points)
            throws IOException, URISyntaxException {
        URI url = this.baseApi
                .newInstance()
                .appendPath(ResourceType.COURSES.type())
                .appendPath(courseId)
                .appendPath(ResourceType.ASSIGNMENTS.type())
                .appendPath(assignmentId)
                .build();
        String body = objectMapper.writeValueAsString(
                RequestWrapper.wrap("assignment", new AssignmentPointsUpdateRequest(points, GradingType.POINTS)));

        HttpPut request = new HttpPut(url);
        request.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
        this.httpClient.execute(request, response -> {
            if (response.getCode() >= 400) {
                throw new IOException("Failed to update assignment points: HTTP "
                        + response.getCode()
                        + " "
                        + EntityUtils.toString(response.getEntity()));
            }
            return null;
        });
    }

    public RubricModels.Created createRubric(String courseId, Map<String, String> formFields)
            throws IOException, URISyntaxException {
        URI url = this.baseApi
                .newInstance()
                .appendPath(ResourceType.COURSES.type())
                .appendPath(courseId)
                .appendPath("rubrics")
                .build();
        String formBody = toFormBody(formFields);

        HttpPost request = new HttpPost(url);
        request.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        request.setEntity(new StringEntity(formBody, ContentType.APPLICATION_FORM_URLENCODED));
        return this.httpClient.execute(request, response -> {
            if (response.getCode() >= 400) {
                throw new IOException("Rubric create failed: HTTP "
                        + response.getCode()
                        + "\n"
                        + EntityUtils.toString(response.getEntity()));
            }
            return objectMapper.readValue(response.getEntity().getContent(), RubricModels.Created.class);
        });
    }

    public List<Course> listCourses() throws IOException, URISyntaxException {
        return getPaginated(
                this.baseApi
                        .newInstance()
                        .appendPath(ResourceType.COURSES.type())
                        .addParameter("enrollment_state", "active")
                        .build(),
                Course.class);
    }

    public List<Assignment> listAssignments(String courseId) throws IOException, URISyntaxException {
        return getPaginated(
                this.baseApi
                        .newInstance()
                        .appendPath(ResourceType.COURSES.type())
                        .appendPath(courseId)
                        .appendPath("assignments")
                        .build(),
                Assignment.class);
    }

    public Assignment getAssignmentWithRubric(String courseId, String assignmentId)
            throws IOException, URISyntaxException {
        URI url = this.baseApi
                .newInstance()
                .appendPath(ResourceType.COURSES.type())
                .appendPath(courseId)
                .appendPath(ResourceType.ASSIGNMENTS.type())
                .appendPath(assignmentId)
                .addParameter("include", "rubric,assignment_visibility,overrides,ab_guid")
                .build();
        return this.httpClient.execute(new HttpGet(url), response -> {
            if (response.getCode() >= 400) {
                throw new IOException("Failed to fetch assignment: HTTP "
                        + response.getCode()
                        + " "
                        + EntityUtils.toString(response.getEntity()));
            }
            return objectMapper.readValue(response.getEntity().getContent(), Assignment.class);
        });
    }

    private <T> List<T> getPaginated(URI url, Class<T> clazz) throws IOException {
        var result = new java.util.ArrayList<JsonNode>();
        URI nextUrl = url;
        while (nextUrl != null) {
            final URI targetUrl = nextUrl;
            Pair<JsonNode, URI> bodyAndNextUrl = this.httpClient.execute(new HttpGet(targetUrl), response -> {
                if (response.getCode() >= 400) {
                    throw new IOException("HTTP "
                            + response.getCode()
                            + " while calling "
                            + targetUrl
                            + ": "
                            + EntityUtils.toString(response.getEntity()));
                }
                return Pair.of(
                        objectMapper.readTree(response.getEntity().getContent()),
                        parseNextLink(
                                response.containsHeader("Link")
                                        ? response.getFirstHeader("Link").getValue()
                                        : ""));
            });

            JsonNode body = bodyAndNextUrl.getLeft();
            nextUrl = bodyAndNextUrl.getRight();
            if (body.isArray()) {
                body.forEach(result::add);
            } else {
                result.add(body);
            }
        }
        return objectMapper.convertValue(
                result, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
    }

    private static URI parseNextLink(String linkHeader) {
        if (linkHeader == null || linkHeader.isEmpty()) {
            return null;
        }
        String[] parts = linkHeader.split(",");
        for (String part : parts) {
            if (part.contains("rel=\"next\"")) {
                int start = part.indexOf('<');
                int end = part.indexOf('>');
                if (start >= 0 && end > start) {
                    return URI.create(part.substring(start + 1, end));
                }
            }
        }
        return null;
    }

    public Map<String, String> buildFormFieldsForRubricCreate(
            String title,
            boolean freeFormComments,
            List<RubricModels.Criterion> criteria,
            int associationId,
            boolean useForGrading,
            boolean hideScoreTotal,
            String purpose) {
        Map<String, String> data = new HashMap<>();
        data.put("rubric[title]", title);
        data.put("rubric[free_form_criterion_comments]", Boolean.toString(freeFormComments));

        data.put("rubric_association[association_id]", Integer.toString(associationId));
        data.put("rubric_association[association_type]", "Assignment");
        data.put("rubric_association[use_for_grading]", Boolean.toString(useForGrading));
        data.put("rubric_association[hide_score_total]", Boolean.toString(hideScoreTotal));
        data.put("rubric_association[purpose]", purpose);

        for (int i = 0; i < criteria.size(); i++) {
            RubricModels.Criterion c = criteria.get(i);
            String critId = "_" + i;
            String base = "rubric[criteria][" + i + "]";
            data.put(base + "[id]", critId);
            data.put(base + "[description]", c.name());
            data.put(base + "[long_description]", c.description());
            data.put(base + "[points]", Double.toString(c.points()));
            data.put(base + "[criterion_use_range]", "false");

            List<RubricModels.Rating> ratings = c.ratings();
            for (int j = 0; j < ratings.size(); j++) {
                RubricModels.Rating r = ratings.get(j);
                String rbase = base + "[ratings][" + j + "]";
                data.put(rbase + "[id]", "r" + i + "_" + j);
                data.put(rbase + "[criterion_id]", critId);
                data.put(rbase + "[description]", r.description());
                data.put(rbase + "[long_description]", r.longDescription());
                data.put(rbase + "[points]", Double.toString(r.points()));
            }
        }

        return data;
    }

    private static String toFormBody(Map<String, String> fields) {

        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> e : fields.entrySet()) {
            String key = urlEncode(e.getKey());
            String value = urlEncode(e.getValue());
            joiner.add(key + "=" + value);
        }
        return joiner.toString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
