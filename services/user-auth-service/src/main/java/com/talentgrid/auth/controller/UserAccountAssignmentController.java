package com.talentgrid.auth.controller;

import com.talentgrid.auth.dto.request.CreateUserAccountAssignmentRequest;
import com.talentgrid.auth.dto.request.UpdateUserAccountAssignmentRequest;
import com.talentgrid.auth.dto.response.UserAccountAssignmentResponse;
import com.talentgrid.auth.service.interfaces.UserAccountAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for user-account assignment management.
 *
 * <p>All operations are restricted to administrators.
 */
@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
public class UserAccountAssignmentController {

    private final UserAccountAssignmentService assignmentService;

    /**
     * Creates a user-account assignment.
     *
     * POST /api/v1/assignments
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserAccountAssignmentResponse> createAssignment(
            @Valid @RequestBody CreateUserAccountAssignmentRequest request
    ) {
        return ResponseEntity.ok(assignmentService.createAssignment(request));
    }

    /**
     * Returns an assignment by id.
     *
     * GET /api/v1/assignments/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserAccountAssignmentResponse> getAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.getAssignmentById(id));
    }

    /**
     * Lists all assignments.
     *
     * GET /api/v1/assignments
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserAccountAssignmentResponse>> getAllAssignments() {
        return ResponseEntity.ok(assignmentService.getAllAssignments());
    }

    /**
     * Updates an assignment.
     *
     * PUT /api/v1/assignments/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserAccountAssignmentResponse> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserAccountAssignmentRequest request
    ) {
        return ResponseEntity.ok(assignmentService.updateAssignment(id, request));
    }

    /**
     * Deletes an assignment.
     *
     * DELETE /api/v1/assignments/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }
}
