package se.diabol.pipefitter;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.ItemGroup;
import hudson.model.ViewDescriptor;
import hudson.model.ViewGroup;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import se.diabol.pipefitter.model.Pipeline;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author Per Huss <mr.per.huss@gmail.com>
 */
public class PipelineView extends AbstractPipelineView {
    private static final Logger LOGGER = Logger.getLogger(PipelineView.class.getName());

    private String title;
    private String firstJob;
    private int noOfPipelines = 1;

    @DataBoundConstructor
    public PipelineView(String name, String title, String firstJob,int noOfPipelines, ViewGroup owner) {
        super(name, owner);
        this.title = title;
        this.firstJob = firstJob;
        this.noOfPipelines = noOfPipelines;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFirstJob() {
        return firstJob;
    }

    public void setFirstJob(String firstJob) {
        this.firstJob = firstJob;
    }

    public int getNoOfPipelines() {
        return noOfPipelines;
    }

    public void setNoOfPipelines(int noOfPipelines) {
        this.noOfPipelines = noOfPipelines;
    }

    public List<Pipeline> getPipelines()
    {
        PipelineFactory pipelineFactory = new PipelineFactory();
        AbstractProject firstJob = Jenkins.getInstance().getItem(this.firstJob, Jenkins.getInstance(), AbstractProject.class);
        return pipelineFactory.createPipelineLatest(pipelineFactory.extractPipeline(getDisplayName(), firstJob), noOfPipelines);
    }




    @Extension
    public static class DescriptorImpl extends ViewDescriptor {
        public String getDisplayName() {
            return "Pipeline View";
        }

        /**
         * Display Job List Item in the Edit View Page
         *
         * @param context What to resolve relative job names against?
         * @return ListBoxModel
         */
        public ListBoxModel doFillFirstJobItems(@AncestorInPath ItemGroup<?> context) {
            ListBoxModel options = new ListBoxModel();
            for (AbstractProject<?, ?> p : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
                options.add(p.getFullDisplayName(), p.getRelativeNameFrom(context));
            }
            return options;
        }

        public ListBoxModel doFillNoOfPipelinesItems(@AncestorInPath ItemGroup<?> context) {
            ListBoxModel options = new ListBoxModel();
            options.add("1", "1");
            options.add("3", "3");
            options.add("5", "5");
            options.add("10", "10");
            return options;
        }

    }
}
