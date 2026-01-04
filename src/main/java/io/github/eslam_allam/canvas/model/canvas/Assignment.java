package io.github.eslam_allam.canvas.model.canvas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Assignment(
        // the ID of the assignment
        Long id,
        // the name of the assignment
        String name,
        // the assignment description, in an HTML fragment
        String description,
        // The time at which this assignment was originally created
        @JsonProperty("created_at") String createdAt,
        // The time at which this assignment was last modified in any way
        @JsonProperty("updated_at") String updatedAt,
        // the due date for the assignment
        @JsonProperty("due_at") String dueAt,
        // the lock date
        @JsonProperty("lock_at") String lockAt,
        // the unlock date
        @JsonProperty("unlock_at") String unlockAt,
        // whether this assignment has overrides
        @JsonProperty("has_overrides") Boolean hasOverrides,
        // (Optional) all dates associated with the assignment, if applicable
        @JsonProperty("all_dates") List<AssignmentDate> allDates,
        // the ID of the course the assignment belongs to
        @JsonProperty("course_id") Long courseId,
        // the URL to the assignment's web page
        @JsonProperty("html_url") String htmlUrl,
        // the URL to download all submissions as a zip
        @JsonProperty("submissions_download_url") String submissionsDownloadUrl,
        // the ID of the assignment's group
        @JsonProperty("assignment_group_id") Long assignmentGroupId,
        // Boolean flag indicating whether the assignment requires a due date
        @JsonProperty("due_date_required") Boolean dueDateRequired,
        // Allowed file extensions
        @JsonProperty("allowed_extensions") List<String> allowedExtensions,
        // maximum length an assignment's name may be
        @JsonProperty("max_name_length") Integer maxNameLength,
        // Turnitin enabled
        @JsonProperty("turnitin_enabled") Boolean turnitinEnabled,
        // VeriCite enabled
        @JsonProperty("vericite_enabled") Boolean vericiteEnabled,
        // Turnitin settings
        @JsonProperty("turnitin_settings") TurnitinSettings turnitinSettings,
        // group assignment grading individually
        @JsonProperty("grade_group_students_individually") Boolean gradeGroupStudentsIndividually,
        // external tool attributes (simplified / ignored structure)
        // peer reviews required
        @JsonProperty("peer_reviews") Boolean peerReviews,
        // automatic peer reviews
        @JsonProperty("automatic_peer_reviews") Boolean automaticPeerReviews,
        // amount of reviews each user is assigned
        @JsonProperty("peer_review_count") Integer peerReviewCount,
        // date the reviews are due by
        @JsonProperty("peer_reviews_assign_at") String peerReviewsAssignAt,
        // whether intra-group peer reviews are allowed
        @JsonProperty("intra_group_peer_reviews") Boolean intraGroupPeerReviews,
        // assignment's group set id
        @JsonProperty("group_category_id") Long groupCategoryId,
        // number of submissions that need grading
        @JsonProperty("needs_grading_count") Integer needsGradingCount,
        // needs grading count by section
        @JsonProperty("needs_grading_count_by_section") List<NeedsGradingCount> needsGradingCountBySection,
        // sorting order of the assignment in the group
        Integer position,
        // post to SIS
        @JsonProperty("post_to_sis") Boolean postToSis,
        // Third Party unique identifier for Assignment
        @JsonProperty("integration_id") String integrationId,
        // maximum points possible for the assignment
        @JsonProperty("points_possible") Double pointsPossible,
        // types of submissions allowed
        @JsonProperty("submission_types") List<String> submissionTypes,
        // If true, the assignment has been submitted to by at least one student
        @JsonProperty("has_submitted_submissions") Boolean hasSubmittedSubmissions,
        // type of grading the assignment receives
        @JsonProperty("grading_type") String gradingType,
        // id of the grading standard being applied to this assignment
        @JsonProperty("grading_standard_id") Long gradingStandardId,
        // Whether the assignment is published
        Boolean published,
        // Whether the assignment's 'published' state can be changed to false
        Boolean unpublishable,
        // Whether the assignment is only visible to overrides
        @JsonProperty("only_visible_to_overrides") Boolean onlyVisibleToOverrides,
        // Whether or not this is locked for the user
        @JsonProperty("locked_for_user") Boolean lockedForUser,
        // An explanation of why this is locked for the user
        @JsonProperty("lock_explanation") String lockExplanation,
        // id of the associated quiz
        @JsonProperty("quiz_id") Long quizId,
        // whether anonymous submissions are accepted
        @JsonProperty("anonymous_submissions") Boolean anonymousSubmissions,
        // Boolean indicating if assignment will be frozen when it is copied
        @JsonProperty("freeze_on_copy") Boolean freezeOnCopy,
        // Boolean indicating if assignment is frozen for the calling user
        Boolean frozen,
        // Array of frozen attributes for the assignment
        @JsonProperty("frozen_attributes") List<String> frozenAttributes,

        // If true, the rubric is directly tied to grading the assignment
        @JsonProperty("use_rubric_for_grading") Boolean useRubricForGrading,
        // basic attributes of the rubric
        @JsonProperty("rubric_settings") RubricModels.Settings rubricSettings,
        // list of scoring criteria and ratings for each rubric criterion
        List<RubricModels.Criteria> rubric,
        // array of student IDs who can see this assignment
        @JsonProperty("assignment_visibility") List<Long> assignmentVisibility,

        // If true, the assignment will be omitted from the student's final grade
        @JsonProperty("omit_from_final_grade") Boolean omitFromFinalGrade,
        // If true, the assignment will not be shown in any gradebooks
        @JsonProperty("hide_in_gradebook") Boolean hideInGradebook,
        // Boolean indicating if the assignment is moderated
        @JsonProperty("moderated_grading") Boolean moderatedGrading,
        // maximum number of provisional graders who may issue grades
        @JsonProperty("grader_count") Integer graderCount,
        // user ID of the grader responsible for choosing final grades
        @JsonProperty("final_grader_id") Long finalGraderId,
        // Boolean indicating if provisional graders' comments are visible
        @JsonProperty("grader_comments_visible_to_graders") Boolean graderCommentsVisibleToGraders,
        // Boolean indicating if provisional graders' identities are hidden
        @JsonProperty("graders_anonymous_to_graders") Boolean gradersAnonymousToGraders,
        // Boolean indicating if provisional grader identities are visible
        @JsonProperty("grader_names_visible_to_final_grader")
        Boolean graderNamesVisibleToFinalGrader,
        // Boolean indicating if the assignment is graded anonymously
        @JsonProperty("anonymous_grading") Boolean anonymousGrading,
        // number of submission attempts a student can make
        @JsonProperty("allowed_attempts") Integer allowedAttempts,
        // Whether the assignment has manual posting enabled
        @JsonProperty("post_manually") Boolean postManually,
        // score statistics
        @JsonProperty("score_statistics") ScoreStatistic scoreStatistics,
        // flags whether user has the right to submit the assignment
        @JsonProperty("can_submit") Boolean canSubmit,
        // academic benchmarks associated with the assignment
        @JsonProperty("ab_guid") List<String> abGuid,
        // id of the attachment to be annotated by students
        @JsonProperty("annotatable_attachment_id") Long annotatableAttachmentId,
        // Boolean indicating whether student names are anonymized
        @JsonProperty("anonymize_students") Boolean anonymizeStudents,
        // Boolean indicating whether the Respondus LockDown Browser is required
        @JsonProperty("require_lockdown_browser") Boolean requireLockdownBrowser,
        // Boolean indicating whether this assignment has important dates
        @JsonProperty("important_dates") Boolean importantDates,
        // Boolean indicating whether notifications are muted for this assignment
        Boolean muted,
        // Boolean indicating whether peer reviews are anonymous
        @JsonProperty("anonymous_peer_reviews") Boolean anonymousPeerReviews,
        // Boolean indicating whether instructor annotations are anonymous
        @JsonProperty("anonymous_instructor_annotations") Boolean anonymousInstructorAnnotations,
        // Boolean indicating whether this assignment has graded submissions
        @JsonProperty("graded_submissions_exist") Boolean gradedSubmissionsExist,
        // Boolean indicating whether this is a quiz lti assignment
        @JsonProperty("is_quiz_assignment") Boolean isQuizAssignment,
        // Boolean indicating whether this assignment is in a closed grading period
        @JsonProperty("in_closed_grading_period") Boolean inClosedGradingPeriod,
        // Boolean indicating whether this assignment can be duplicated
        @JsonProperty("can_duplicate") Boolean canDuplicate,
        // original assignment's course_id
        @JsonProperty("original_course_id") Long originalCourseId,
        // original assignment's id
        @JsonProperty("original_assignment_id") Long originalAssignmentId,
        // original assignment's lti_resource_link_id
        @JsonProperty("original_lti_resource_link_id") Long originalLtiResourceLinkId,
        // original assignment's name
        @JsonProperty("original_assignment_name") String originalAssignmentName,
        // original assignment's quiz_id
        @JsonProperty("original_quiz_id") Long originalQuizId,
        // String indicating what state this assignment is in
        @JsonProperty("workflow_state") String workflowState) {

    public enum GradingType {
        PASS_FAIL,
        PERCENT,
        LETTER_GRADE,
        GPA_SCALE,
        POINTS;

        public String type() {
            return this.name().toLowerCase();
        }

        @JsonValue
        public String toValue() {
            return this.type();
        }
    }
}
