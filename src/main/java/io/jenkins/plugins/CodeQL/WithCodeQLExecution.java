package io.jenkins.plugins.CodeQL;

import hudson.*;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;

import java.io.*;
import java.util.*;

import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Logger;
import java.util.logging.Level;

public class WithCodeQLExecution extends StepExecution {

    private static final Logger LOGGER = Logger.getLogger(WithCodeQLExecution.class.getName());
    private final transient WithCodeql step;
    private final transient Launcher launcher;
    private final transient TaskListener listener;
    private final transient EnvVars env;
    private transient Computer computer;
    private transient EnvVars envOverride;
    private transient String codeQLExecPath;
    private transient String codeQLRunnerHome;

    /**
     * Indicates if running on docker with <code>docker.image()</code> or <code>container()</code>
     */
    private boolean withContainer;

    private transient PrintStream console;

    public WithCodeQLExecution(StepContext context, WithCodeql step)  throws Exception {

        super(context);
        this.step = step;
        launcher = context.get(Launcher.class);
        listener = context.get(TaskListener.class);
        env = context.get(EnvVars.class);
    }

    @Override
    public boolean start() throws IOException, InterruptedException {
        envOverride = new EnvVars();

        console = listener.getLogger();
        codeQLRunnerHome = obtainCodeQLRunnerExec();

        HashMap<String,String> overrides = new HashMap<String,String>();
        overrides.put("PATH+CODEQL", codeQLRunnerHome);
        overrides.put("CODEQL_CLI_HOME", codeQLRunnerHome);

        EnvironmentExpander envEx = EnvironmentExpander.merge(getContext().get(EnvironmentExpander.class), new ExpanderImpl(overrides));

        getContext().newBodyInvoker()
                .withContext(envEx)
                .withCallback(new RunAnalyzeCallback(getContext())).start();
        
        return false;
    }

    private static class RunAnalyzeCallback extends BodyExecutionCallback {
        private final StepContext parentContext;

        public RunAnalyzeCallback(StepContext parentContext) {
            this.parentContext = parentContext;
        }

        @Override
        public void onSuccess(StepContext context, Object result) {
            parentContext.onSuccess("");
        }


        @Override
        public void onFailure(StepContext context, Throwable t) {
            parentContext.onFailure(t);
        }
    }

    /**
     * Find the "mvn" executable if exists, either specified by the "withMaven(){}" step or provided by the build agent.
     *
     * @return remote path to the Maven executable or {@code null} if none found
     * @throws IOException
     * @throws InterruptedException
     */
    @Nullable
    private String obtainCodeQLRunnerExec() throws IOException, InterruptedException {
        String codeQLInstallationName = step.getCodeql();
        LOGGER.log(Level.FINE, "Setting up codeql: {0}", codeQLInstallationName);

        StringBuilder consoleMessage = new StringBuilder("[withCodeQL]");
        String codeQLExecPath = "";

        if (StringUtils.isEmpty(codeQLInstallationName)) {
            // no maven installation name is passed, we will search for the CodeQL installation on the agent
            consoleMessage.append("No CodeQL Installation installation specified!");
        } else if (withContainer) {
            console.println(
                    "[withCodeQL] WARNING: Specified CodeQL '" + codeQLInstallationName + "' cannot be installed, will be ignored. " +
                            "Step running within a container, tool installations are not available see https://issues.jenkins-ci.org/browse/JENKINS-36159. ");
            LOGGER.log(Level.FINE, "Running in docker-pipeline, ignore CodeQL Installation parameter: {0}", codeQLInstallationName);
        } else {
            return obtainCodeQLRunnerExecutableFromCodeQLInstallation(codeQLInstallationName);
        }

        return codeQLExecPath;
    }


    private String obtainCodeQLRunnerExecutableFromCodeQLInstallation(String codeQLInstallationName) throws IOException, InterruptedException {

        CodeQLToolInstallation codeQLInstallation = null;
        for (CodeQLToolInstallation i : getCodeQLInstallations()) {
            if (codeQLInstallationName.equals(i.getName())) {
                codeQLInstallation = i;
                LOGGER.log(Level.FINE, "Found CodeQL installation {0} with installation home {1}", new Object[]{codeQLInstallation.getName(), codeQLInstallation.getHome()});
                break;
            }
        }
        if (codeQLInstallation == null) {
            throw new AbortException("Could not find specified CodeQL installation");
        }
        Node node = getComputer().getNode();
        if (node == null) {
            throw new AbortException("Could not obtain the Node for the computer: " + getComputer().getName());
        }
        codeQLInstallation = codeQLInstallation.forNode(node, listener).forEnvironment(env);
        console.println("[withCodeQL] using CodeQL installation '" + codeQLInstallation.getName() + "'");

        return codeQLInstallation.getHome();
    }

    private static CodeQLToolInstallation[] getCodeQLInstallations() {
        return Jenkins.get().getDescriptorByType(CodeQLToolInstallation.DescriptorImpl.class).getInstallations();
    }

    /**
     * Gets the computer for the current launcher.
     *
     * @return the computer
     * @throws AbortException in case of error.
     */
    @Nonnull
    private Computer getComputer() throws AbortException {
        if (computer != null) {
            return computer;
        }

        String node = null;
        Jenkins j = Jenkins.get();

        for (Computer c : j.getComputers()) {
            if (c.getChannel() == launcher.getChannel()) {
                node = c.getName();
                break;
            }
        }

        if (node == null) {
            throw new AbortException("Could not find computer for the job");
        }

        computer = j.getComputer(node);
        if (computer == null) {
            throw new AbortException("No such computer " + node);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Computer: {0}", computer.getName());
            try {
                LOGGER.log(Level.FINE, "Env: {0}", computer.getEnvironment());
            } catch (IOException | InterruptedException e) {// ignored
            }
        }
        return computer;
    }

    /**
     * Takes care of overriding the environment with our defined overrides
     */
    private static final class ExpanderImpl extends EnvironmentExpander {
        private static final long serialVersionUID = 1;
        private final Map<String, String> overrides;

        ExpanderImpl(HashMap<String, String> overrides) {
            this.overrides = overrides;

        }

        @Override
        public void expand(@Nonnull EnvVars env) {
            env.overrideAll(overrides);
        }
    }
}


