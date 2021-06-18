package io.jenkins.plugins.codeql;

import hudson.Extension;
import hudson.Functions;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import jenkins.MasterToSlaveFileCallable;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class CodeQLInstaller extends DownloadFromUrlInstaller{


    @DataBoundConstructor
    public CodeQLInstaller(String id) {
        super(id);
    }

    @Override
    public Installable getInstallable() throws IOException {
        Installable installable = super.getInstallable();
        return installable != null ? new CodeQLInstallable(installable) : installable;
    }

    protected final class CodeQLInstallable extends NodeSpecificInstallable {

        public CodeQLInstallable(Installable inst) {
            super(inst);
        }

        @Override
        public NodeSpecificInstallable forNode(Node node, TaskListener log) throws IOException, InterruptedException {
            String codeqlCommand = "" ;
                Computer computer = node.toComputer();

                if(Objects.isNull(computer))  {
                    return this;
                }

                String arch = ((String) computer.getSystemProperties().get("os.name")).toLowerCase(Locale.ENGLISH);
                if (arch.contains("linux")) {
                    codeqlCommand = "codeql-bundle-linux64.tar.gz";
                }
                if (arch.contains("windows")) {
                    codeqlCommand = "codeql-bundle-win64.tar.gz";
                }
                if (arch.contains("mac")) {
                    codeqlCommand = "codeql-bundle-osx64.tar.gz";
                }

            url += codeqlCommand;
            return this;
        }
    }

    @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<CodeQLInstaller> {

        public String getDisplayName() {
            return "Install from GitHub.com";
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == CodeQLToolInstallation.class;
        }
    }
}