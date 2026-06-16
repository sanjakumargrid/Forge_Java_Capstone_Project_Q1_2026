package com.talentgrid.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "rbac")
public class RbacProperties {

    private List<String> publicPaths;
    private List<RbacRule> rules;

    public List<String> getPublicPaths() { return publicPaths; }
    public void setPublicPaths(List<String> publicPaths) { this.publicPaths = publicPaths; }

    public List<RbacRule> getRules() { return rules; }
    public void setRules(List<RbacRule> rules) { this.rules = rules; }

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
