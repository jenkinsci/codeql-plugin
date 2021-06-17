package io.jenkins.plugins.CodeQL;

import hudson.*;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CodeQLToolInstallation extends ToolInstallation
        implements EnvironmentSpecific<CodeQLToolInstallation>, NodeSpecific<CodeQLToolInstallation>, Serializable {

    private final String codeqlHome;


    @DataBoundConstructor
    public CodeQLToolInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, launderHome(home), properties);
        this.codeqlHome = super.getHome();
    }

    private static String launderHome(String home) {
        if (home != null && (home.endsWith("/") || home.endsWith("\\"))) {
            // see https://issues.apache.org/bugzilla/show_bug.cgi?id=26947
            // Ant doesn't like the trailing slash, especially on Windows
            return home.substring(0, home.length() - 1);
        } else {
            return home;
        }
    }

    @Override
    public String getHome() {
        if (codeqlHome != null) {
            return codeqlHome;
        }
        return super.getHome();
    }

    @Override
    public void buildEnvVars(EnvVars env) {
        env.put("PATH+CODEQL", getHome());
        env.put("CODEL_CLI_HOME", getHome());
    }

    public CodeQLToolInstallation forEnvironment(EnvVars environment) {
        return new CodeQLToolInstallation(getName(), environment.expand(codeqlHome), getProperties().toList());
    }

    public CodeQLToolInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new CodeQLToolInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Extension @Symbol("codeql")
    public static class DescriptorImpl extends ToolDescriptor<CodeQLToolInstallation> {

        @CopyOnWrite
        private volatile CodeQLToolInstallation[] installations = new CodeQLToolInstallation[0];

        public DescriptorImpl() {
        }

        @Override
        public String getDisplayName() {
            return Messages.installer_displayName();
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {

            return Collections.singletonList(new CodeQLInstaller(null));
        }

        @Override
        public CodeQLToolInstallation[] getInstallations() {
            load();
            return Arrays.copyOf(installations, installations.length);
        }

        @Override
        public void setInstallations(CodeQLToolInstallation... installations) {
            this.installations = installations;
            save();
        }

    }
}

