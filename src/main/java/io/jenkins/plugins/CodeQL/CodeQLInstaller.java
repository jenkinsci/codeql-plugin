package io.jenkins.plugins.CodeQL;

import hudson.Extension;
import hudson.FilePath;
import hudson.Functions;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.slaves.NodeSpecific;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import jenkins.MasterToSlaveFileCallable;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

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

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log) throws IOException, InterruptedException {
        FilePath dir = preferredLocation(tool, node);

        Installable inst = getInstallable();
        if(inst==null) {
            log.getLogger().println("Invalid tool ID "+id);
            return dir;
        }

        if (inst instanceof NodeSpecific) {
            inst = (Installable) ((NodeSpecific) inst).forNode(node, log);
        }

        if (dir.installIfNecessaryFrom(new URL(inst.url), log, "Unpacking " + inst.url + " to " + dir + " on " + node.getDisplayName())) {
            dir.act(new ChmodRecAPlusX());
        }

        return dir;
    }

    protected final class CodeQLInstallable extends NodeSpecificInstallable {

        public CodeQLInstallable(Installable inst) {
            super(inst);
        }

        @Override
        public NodeSpecificInstallable forNode(Node node, TaskListener log) throws IOException, InterruptedException {
            String codeqlCommand = "" ;
                Computer computer = node.toComputer();

                String arch = ((String) computer.getSystemProperties().get("os.name")).toLowerCase(Locale.ENGLISH);
                if (arch.contains("linux")) {
                    codeqlCommand = "codeql-linux64.zip";
                }
                if (arch.contains("windows")) {
                    codeqlCommand = "codeql-win64.zip";
                }
                if (arch.contains("mac")) {
                    codeqlCommand = "codeql-osx64.zip";
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

    static class ChmodRecAPlusX extends MasterToSlaveFileCallable<Void> {
        private static final long serialVersionUID = 1L;
        public Void invoke(File d, VirtualChannel channel) throws IOException {
            if(!Functions.isWindows())
                process(d);
            return null;
        }
        private void process(File f) {
            if (f.isFile()) {
                f.setExecutable(true, false);
            } else {
                File[] kids = f.listFiles();
                if (kids != null) {
                    for (File kid : kids) {
                        process(kid);
                    }
                }
            }
        }
    }
}