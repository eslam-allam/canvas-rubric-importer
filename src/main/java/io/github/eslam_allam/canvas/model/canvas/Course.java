package io.github.eslam_allam.canvas.model.canvas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Course(
        // the unique identifier for the course
        Long id,

        // the SIS identifier for the course, if defined
        @JsonProperty("sis_course_id")
        String sisCourseId,

        // the UUID of the course
        String uuid,

        // the integration identifier for the course, if defined
        @JsonProperty("integration_id")
        String integrationId,

        // the unique identifier for the SIS import
        @JsonProperty("sis_import_id")
        Long sisImportId,

        // the full name of the course
        String name,

        // the course code
        @JsonProperty("course_code")
        String courseCode,

        // the actual course name
        @JsonProperty("original_name")
        String originalName,

        // the current state of the course
        @JsonProperty("workflow_state")
        String workflowState,

        // the account associated with the course
        @JsonProperty("account_id")
        Long accountId,

        // the root account associated with the course
        @JsonProperty("root_account_id")
        Long rootAccountId,

        // the enrollment term associated with the course
        @JsonProperty("enrollment_term_id")
        Long enrollmentTermId,

        // the grading standard associated with the course
        @JsonProperty("grading_standard_id")
        Long gradingStandardId,

        // the grade_passback_setting set on the course
        @JsonProperty("grade_passback_setting")
        String gradePassbackSetting,

        // the date the course was created.
        @JsonProperty("created_at")
        String createdAt,

        // the start date for the course, if applicable
        @JsonProperty("start_at")
        String startAt,

        // the end date for the course, if applicable
        @JsonProperty("end_at")
        String endAt,

        // the course-set locale, if applicable
        String locale,

        // optional: the total number of active and invited students in the course
        @JsonProperty("total_students")
        Integer totalStudents,

        // the type of page that users will see when they first visit the course
        @JsonProperty("default_view")
        String defaultView,

        // optional: user-generated HTML for the course syllabus
        @JsonProperty("syllabus_body")
        String syllabusBody,

        // optional: the number of submissions needing grading
        @JsonProperty("needs_grading_count")
        Integer needsGradingCount,

        // optional: the enrollment term object for the course
        Term term,

        // optional: information on progress through the course
        @JsonProperty("course_progress")
        CourseProgress courseProgress,

        // weight final grade based on assignment group percentages
        @JsonProperty("apply_assignment_group_weights")
        Boolean applyAssignmentGroupWeights,

        @JsonProperty("is_public")
        Boolean isPublic,

        @JsonProperty("is_public_to_auth_users")
        Boolean isPublicToAuthUsers,

        @JsonProperty("public_syllabus")
        Boolean publicSyllabus,

        @JsonProperty("public_syllabus_to_auth")
        Boolean publicSyllabusToAuth,

        // optional: the public description of the course
        @JsonProperty("public_description")
        String publicDescription,

        @JsonProperty("storage_quota_mb")
        Integer storageQuotaMb,

        @JsonProperty("storage_quota_used_mb")
        Integer storageQuotaUsedMb,

        @JsonProperty("hide_final_grades")
        Boolean hideFinalGrades,

        String license,

        @JsonProperty("allow_student_assignment_edits")
        Boolean allowStudentAssignmentEdits,

        @JsonProperty("allow_wiki_comments")
        Boolean allowWikiComments,

        @JsonProperty("allow_student_forum_attachments")
        Boolean allowStudentForumAttachments,

        @JsonProperty("open_enrollment")
        Boolean openEnrollment,

        @JsonProperty("self_enrollment")
        Boolean selfEnrollment,

        @JsonProperty("restrict_enrollments_to_course_dates")
        Boolean restrictEnrollmentsToCourseDates,

        @JsonProperty("course_format")
        String courseFormat,

        // optional: this will be true if this user is currently prevented from viewing
        // the course because of date restriction settings
        @JsonProperty("access_restricted_by_date")
        Boolean accessRestrictedByDate,

        // The course's IANA time zone name.
        @JsonProperty("time_zone")
        String timeZone,

        // optional: whether the course is set as a Blueprint Course
        Boolean blueprint,

        // optional: whether the course is set as a template
        Boolean template
) {
}



