package com.talentgrid.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

/**
 * Configuration properties class for Role-Based Access Control (RBAC).
 * 
 * <p>Binds the properties prefixed with {@code rbac} from the external configuration
 * (e.g., {@code rbac-rules.yml}). Defines the public paths that bypass authentication 
 * entirely and the granular rules for authorized paths.</p>
 */
@ConfigurationProperties(prefix = "rbac")
public class RbacProperties {

    /**
     * List of Ant-style path patterns that are public and bypass authentication.
     */
    private List<String> publicPaths;

    /**
     * List of granular access control rules.
     */
    private List<RbacRule> rules;

    public List<String> getPublicPaths() { return publicPaths; }
    public void setPublicPaths(List<String> publicPaths) { this.publicPaths = publicPaths; }

    public List<RbacRule> getRules() { return rules; }
    public void setRules(List<RbacRule> rules) { this.rules = rules; }

    /**
     * Represents a single access control rule mapping a path and HTTP methods 
     * to required roles or scopes.
     */
    public static class RbacRule {
        private String path;
        private List<String> methods;
        private List<String> allowedRoles;
        private List<String> allowedScopes;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public List<String> getMethods() { return methods; }
        public void setMethods(List<String> methods) { this.methods = methods; }

        public List<String> getAllowedRoles() { return allowedRoles; }
        public void setAllowedRoles(List<String> allowedRoles) { this.allowedRoles = allowedRoles; }

        public List<String> getAllowedScopes() { return allowedScopes; }
        public void setAllowedScopes(List<String> allowedScopes) { this.allowedScopes = allowedScopes; }
    }
}
