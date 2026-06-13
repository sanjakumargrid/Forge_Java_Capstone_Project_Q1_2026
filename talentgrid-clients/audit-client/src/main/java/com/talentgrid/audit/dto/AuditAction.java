package com.talentgrid.audit.dto;

/**
 * Supported audit actions captured by the Audit Service.
 */
public enum AuditAction {

  CREATE,
  UPDATE,
  DELETE,

  APPROVE,
  REJECT,

  ASSIGN,
  UNASSIGN,

  LOGIN,
  LOGOUT,

  STATUS_CHANGE
}