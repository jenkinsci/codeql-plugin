package io.jenkins.plugins.codeql;
import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;


import java.util.Set;

public class WithCodeQL extends Step {

    private String codeql;

    @DataBoundConstructor
    public WithCodeQL(String codeql) {
        this.codeql = codeql;
    }

    public String getCodeql() {
        return codeql;
    }

    @DataBoundSetter
    public void setCodeql(String codeql) {
        this.codeql = codeql;
    }


    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new WithCodeQLExecution(context, this);
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "withCodeQL";
        }

        @Override
        public String getDisplayName() {
            return "Provide codeql environment";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, FilePath.class, TaskListener.class, EnvVars.class);
        }

        private CodeQLToolInstallation.DescriptorImpl getCodeQLDescriptor() {
            return Jenkins.get().getDescriptorByType(CodeQLToolInstallation.DescriptorImpl.class);
        }

        @Restricted(NoExternalUse.class) // Only for UI calls
        public ListBoxModel doFillCodeqlItems(@AncestorInPath Item item) {
            ListBoxModel r = new ListBoxModel();
            if (item == null) {
                return r; // it's empty
            }
            item.checkPermission(Item.CONFIGURE);
            for (CodeQLToolInstallation installation : getCodeQLDescriptor().getInstallations()) {
                r.add(installation.getName());
            }
            return r;
        }
    }
}



