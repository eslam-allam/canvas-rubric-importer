package io.github.eslam_allam.canvas.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.eslam_allam.canvas.model.canvas.Assignment;
import io.github.eslam_allam.canvas.model.canvas.Course;
import io.github.eslam_allam.canvas.model.canvas.RubricModels;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class CanvasClient {

    private final String baseUrl;
    private final String token;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CanvasClient(String baseUrl, String token) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.token = token;
        this.httpClient = HttpClient.newBuilder().build();
        this.objectMapper = new ObjectMapper();
    }

    public void updateAssignmentPoints(String courseId, String assignmentId, double points)
            throws IOException, InterruptedException {
        String url =
                String.format(
                        "%s/api/v1/courses/%s/assignments/%s", baseUrl, courseId, assignmentId);
        Map<String, Object> payload =
                Map.of("assignment", Map.of("points_possible", points, "grading_type", "points"));
        String body = objectMapper.writeValueAsString(payload);

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException(
                    "Failed to update assignment points: HTTP "
                            + response.statusCode()
                            + " "
                            + response.body());
        }
    }

    public RubricModels.Created createRubric(String courseId, Map<String, String> formFields)
            throws IOException, InterruptedException {
        String url = String.format("%s/api/v1/courses/%s/rubrics", baseUrl, courseId);
        String formBody = toFormBody(formFields);

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formBody))
                        .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException(
                    "Rubric create failed: HTTP " + response.statusCode() + "\n" + response.body());
        }
        return objectMapper.readValue(response.body(), RubricModels.Created.class);
    }

    public List<Course> listCourses() throws IOException, InterruptedException {
        String url = baseUrl + "/api/v1/courses?enrollment_state=active";
        return getPaginated(url, Course.class);
    }

    public List<Assignment> listAssignments(String courseId)
            throws IOException, InterruptedException {
        String url = String.format("%s/api/v1/courses/%s/assignments", baseUrl, courseId);
        return getPaginated(url, Assignment.class);
    }

    public Assignment getAssignmentWithRubric(String courseId, String assignmentId)
            throws IOException, InterruptedException {
        String url =
                String.format(
                        "%s/api/v1/courses/%s/assignments/%s?include=rubric,assignment_visibility,overrides,ab_guid",
                        baseUrl, courseId, assignmentId);
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException(
                    "Failed to fetch assignment: HTTP "
                            + response.statusCode()
                            + " "
                            + response.body());
        }
        return objectMapper.readValue(response.body(), Assignment.class);
    }

    private <T> List<T> getPaginated(String url, Class<T> clazz) throws IOException, InterruptedException {
        var result = new java.util.ArrayList<JsonNode>();
        String nextUrl = url;
        while (nextUrl != null) {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(nextUrl))
                            .header("Authorization", "Bearer " + token)
                            .GET()
                            .build();

            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IOException(
                        "HTTP "
                                + response.statusCode()
                                + " while calling "
                                + nextUrl
                                + ": "
                                + response.body());
            }

            JsonNode body = objectMapper.readTree(response.body());
            if (body.isArray()) {
                body.forEach(result::add);
            } else {
                result.add(body);
            }

            nextUrl = parseNextLink(response.headers().firstValue("Link").orElse(""));
        }
        return objectMapper.convertValue(result, 
            objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
    }

    private static String parseNextLink(String linkHeader) {
        if (linkHeader == null || linkHeader.isEmpty()) {
            return null;
        }
        String[] parts = linkHeader.split(",");
        for (String part : parts) {
            if (part.contains("rel=\"next\"")) {
                int start = part.indexOf('<');
                int end = part.indexOf('>');
                if (start >= 0 && end > start) {
                    return part.substring(start + 1, end);
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
