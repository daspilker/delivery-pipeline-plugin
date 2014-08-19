/*
This file is part of Delivery Pipeline Plugin.

Delivery Pipeline Plugin is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Delivery Pipeline Plugin is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Delivery Pipeline Plugin.
If not, see <http://www.gnu.org/licenses/>.
*/
package se.diabol.jenkins.pipeline;

import au.com.centrumsystems.hudson.plugin.buildpipeline.trigger.BuildPipelineTrigger;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.*;
import hudson.plugins.parameterizedtrigger.AbstractBuildParameterFactory;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.ResultCondition;
import hudson.security.ACL;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.security.Permission;
import hudson.tasks.BuildTrigger;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.json.JSONObject;

import static org.junit.Assert.*;

import org.acegisecurity.AuthenticationException;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;
import org.jvnet.hudson.test.WithoutJenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import se.diabol.jenkins.pipeline.domain.*;
import se.diabol.jenkins.pipeline.sort.NameComparator;
import se.diabol.jenkins.pipeline.trigger.TriggerException;

@RunWith(MockitoJUnitRunner.class)
public class DeliveryPipelineViewTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    @WithoutJenkins
    public void testOnJobRenamed() {
        List<DeliveryPipelineView.ComponentSpec> componentSpecs = new ArrayList<DeliveryPipelineView.ComponentSpec>();
        componentSpecs.add(new DeliveryPipelineView.ComponentSpec("comp1", "build1"));
        componentSpecs.add(new DeliveryPipelineView.ComponentSpec("comp2", "build2"));

        DeliveryPipelineView view = new DeliveryPipelineView("Test");
        view.setComponentSpecs(componentSpecs);
        view.onJobRenamed(null, "build1", "newbuild");
        assertEquals("newbuild", view.getComponentSpecs().get(0).getFirstJob());
    }

    @Test
    @WithoutJenkins
    @Bug(23373)
    public void testOnJobRenamedNoComponentSpecs() {
        DeliveryPipelineView view = new DeliveryPipelineView("Test");
        //Rename
        view.onJobRenamed(null, "build1", "newbuild");
        //Delete
        view.onJobRenamed(null, "build1", null);
    }


    @Test
    @WithoutJenkins
    public void testOnJobRenamedDelete() {
        List<DeliveryPipelineView.ComponentSpec> componentSpecs = new ArrayList<DeliveryPipelineView.ComponentSpec>();
        componentSpecs.add(new DeliveryPipelineView.ComponentSpec("comp1", "build1"));
        componentSpecs.add(new DeliveryPipelineView.ComponentSpec("comp2", "build2"));


        DeliveryPipelineView view = new DeliveryPipelineView("Test");
        view.setComponentSpecs(componentSpecs);
        assertEquals(2, view.getComponentSpecs().size());
        view.onJobRenamed(null, "build1", null);

        assertEquals(1, view.getComponentSpecs().size());

    }

    @Test
    @WithoutJenkins
    public void testSubmit() throws Exception {
        DeliveryPipelineView view = new DeliveryPipelineView("name");
        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        when(request.getSubmittedForm()).thenReturn(new JSONObject());
        view.submit(request);
        verify(request, times(1)).bindJSON(view, new JSONObject());
        verify(request, times(1)).bindJSONToList(DeliveryPipelineView.ComponentSpec.class, null);
        verify(request, times(1)).bindJSONToList(DeliveryPipelineView.RegExpSpec.class, null);
    }


    @Test
    @WithoutJenkins
    public void testDoCheckUpdateInterval() {
        DeliveryPipelineView.DescriptorImpl d = new DeliveryPipelineView.DescriptorImpl();
        assertEquals(FormValidation.Kind.ERROR, d.doCheckUpdateInterval("").kind);
        assertEquals(FormValidation.Kind.ERROR, d.doCheckUpdateInterval(null).kind);
        assertEquals(FormValidation.Kind.OK, d.doCheckUpdateInterval("3").kind);
        assertEquals(FormValidation.Kind.ERROR, d.doCheckUpdateInterval("3a").kind);
        assertEquals(FormValidation.Kind.ERROR, d.doCheckUpdateInterval("0").kind);
        assertEquals(FormValidation.Kind.OK, d.doCheckUpdateInterval("1").kind);
    }

    @Test
    @WithoutJenkins
    public void testDefaults() {
        DeliveryPipelineView view = new DeliveryPipelineView("name");
        assertEquals(3, view.getNoOfPipelines());
        assertEquals(1, view.getNoOfColumns());
        assertEquals(2, view.getUpdateInterval());
        assertEquals("none", view.getSorting());
        assertNull(view.getEmbeddedCss());
        assertNull(view.getFullScreenCss());
        assertNull(view.getComponentSpecs());
        assertFalse(view.isShowAggregatedPipeline());
        assertFalse(view.getShowAvatars());
        assertFalse(view.isShowChanges());
        assertFalse(view.isAllowManualTriggers());
    }

    @Test
    @WithoutJenkins
    public void testSettersAndGetters() {
        DeliveryPipelineView view = new DeliveryPipelineView("name");
        view.setNoOfPipelines(17);
        assertEquals(17, view.getNoOfPipelines());
        view.setShowChanges(true);
        assertTrue(view.isShowChanges());
        view.setShowChanges(false);
        assertFalse(view.isShowChanges());
        view.setNoOfColumns(2);
        assertEquals(2, view.getNoOfColumns());
        view.setShowAvatars(true);
        assertTrue(view.getShowAvatars());
        view.setShowAvatars(false);
        assertFalse(view.getShowAvatars());
        assertNotNull(view.getLastUpdated());
        view.setAllowManualTriggers(true);
        assertTrue(view.isAllowManualTriggers());

    }

    @Test
    @WithoutJenkins
    public void testCssUrl() {
        DeliveryPipelineView view = new DeliveryPipelineView("name");
        view.setEmbeddedCss("");
        view.setFullScreenCss("");
        assertNull(view.getEmbeddedCss());
        assertNull(view.getFullScreenCss());
    }

    @Test
    @WithoutJenkins
    public void testOldSorter() throws Exception {
        DeliveryPipelineView view = new DeliveryPipelineView("name");
        Field field = view.getClass().getDeclaredField("sorting");
        field.setAccessible(true);
        field.set(view, "se.diabol.jenkins.pipeline.sort.NoOpComparator");
        assertEquals("none", view.getSorting());

        view.setSorting("se.diabol.jenkins.pipeline.sort.NoOpComparator");
        assertEquals("none", view.getSorting());
    }

    @Test
    @WithoutJenkins
    public void testSetSorting() {
        DeliveryPipelineView view = new DeliveryPipelineView("name");
        view.setSorting("se.diabol.jenkins.pipeline.sort.NameComparator");
        assertEquals("se.diabol.jenkins.pipeline.sort.NameComparator", view.getSorting());
    }


    @Test
    public void testGetItemsAndContains() throws Exception {
        FreeStyleProject build = jenkins.createFreeStyleProject("build");
        FreeStyleProject sonar = jenkins.createFreeStyleProject("sonar");
        FreeStyleProject packaging = jenkins.createFreeStyleProject("packaging");
        build.getPublishersList().add(new BuildTrigger("sonar", false));
        build.getPublishersList().add(new BuildTrigger("packaging", false));

        jenkins.getInstance().rebuildDependencyGraph();


        List<DeliveryPipelineView.ComponentSpec> specs = new ArrayList<DeliveryPipelineView.ComponentSpec>();
        specs.add(new DeliveryPipelineView.ComponentSpec("Comp", "build"));
        DeliveryPipelineView view = new DeliveryPipelineView("name");
        view.setComponentSpecs(specs);
        jenkins.getInstance().addView(view);

        assertTrue(view.contains(build));
        assertTrue(view.contains(sonar));
        assertTrue(view.contains(packaging));

        Collection<TopLevelItem> items =  view.getItems();
        assertEquals(3, items.size());

    }

    @Test
    public void testGetItemsAndContainsWithFolders() throws Exception {
        MockFolder folder = jenkins.createFolder("folder");
        FreeStyleProject build = folder.createProject(FreeStyleProject.class, "build");
        FreeStyleProject sonar = folder.createProject(FreeStyleProject.class, "sonar");
        FreeStyleProject packaging = folder.createProject(FreeStyleProject.class,"packaging");


        build.getPublishersList().add(new BuildTrigger("sonar", false));
        build.getPublishersList().add(new BuildTrigger("packaging", false));

        jenkins.getInstance().rebuildDependencyGraph();


        List<DeliveryPipelineView.ComponentSpec> specs = new ArrayList<DeliveryPipelineView.ComponentSpec>();
        specs.add(new DeliveryPipelineView.ComponentSpec("Comp", "build"));
        DeliveryPipelineView view = new DeliveryPipelineView("name");
        view.setComponentSpecs(specs);
        folder.addView(view);

        assertTrue(view.contains(build));
        assertTrue(view.contains(sonar));
        assertTrue(view.contains(packaging));

        Collection<TopLevelItem> items =  view.getItems();
        assertEquals(3, items.size());

    }


    @Test
    public void testGetPipelines() throws Exception {
        FreeStyleProject build = jenkins.createFreeStyleProject("build");
        build.addProperty(new PipelineProperty("Build", "BuildStage"));
        List<DeliveryPipelineView.ComponentSpec> specs = new ArrayList<DeliveryPipelineView.ComponentSpec>();
        specs.add(new DeliveryPipelineView.ComponentSpec("Comp", "build"));
        DeliveryPipelineView view = new DeliveryPipelineView("Pipeline");
        view.setComponentSpecs(specs);
        view.setSorting(NameComparator.class.getName());
        jenkins.getInstance().addView(view);
        List<Component> components = view.getPipelines();
        assertNull(view.getError());
        assertEquals(1, components.size());
        Component component = components.get(0);
        assertEquals(0, component.getPipelines().size());
        assertEquals("Comp", component.getName());

        jenkins.setQuietPeriod(0);
        jenkins.buildAndAssertSuccess(build);

        components = view.getPipelines();
        assertNull(view.getError());
        assertEquals(1, components.size());
        component = components.get(0);
        assertEquals(1, component.getPipelines().size());
        assertEquals("Comp", component.getName());
        Pipeline pipeline = component.getPipelines().get(0);
        assertEquals("#1", pipeline.getVersion());
        assertNotNull(pipeline.getTimestamp());
        assertFalse(pipeline.isAggregated());
        assertEquals(1, pipeline.getTriggeredBy().size());
        assertEquals(0, pipeline.getContributors().size());
        assertEquals(1, pipeline.getStages().size());
        assertEquals(0, pipeline.getChanges().size());

        Stage stage = pipeline.getStages().get(0);
        assertEquals("BuildStage", stage.getName());
        assertEquals(1, stage.getTasks().size());
        Task task = stage.getTasks().get(0);
        assertEquals("Build", task.getName());
        assertEquals("build", task.getId());
        assertEquals("1", task.getBuildId());
        assertNull(task.getTestResult());


        view.setShowAggregatedPipeline(true);
        components = view.getPipelines();
        assertNull(view.getError());
        assertEquals(1, components.size());
        component = components.get(0);
        assertEquals(2, component.getPipelines().size());
        assertEquals("Comp", component.getName());



        pipeline = component.getPipelines().get(0);
        assertNull(pipeline.getVersion());
        assertNull(pipeline.getTimestamp());
        assertTrue(pipeline.isAggregated());
        assertNull(pipeline.getTriggeredBy());
        assertNull(pipeline.getContributors());
        assertEquals(1, pipeline.getStages().size());
        assertNull(pipeline.getChanges());



        pipeline = component.getPipelines().get(1);
        assertEquals("#1", pipeline.getVersion());
        assertNotNull(pipeline.getTimestamp());
        assertFalse(pipeline.isAggregated());
        assertEquals(1, pipeline.getTriggeredBy().size());
        assertEquals(0, pipeline.getContributors().size());
        assertEquals(1, pipeline.getStages().size());
        assertEquals(0, pipeline.getChanges().size());



    }

    @Test
    @WithoutJenkins
    public void testDoCheckName() {
        DeliveryPipelineView.ComponentSpec.DescriptorImpl d = new DeliveryPipelineView.ComponentSpec.DescriptorImpl();
        assertEquals(FormValidation.Kind.ERROR,  d.doCheckName(null).kind);
        assertEquals(FormValidation.Kind.ERROR,  d.doCheckName("").kind);
        assertEquals(FormValidation.Kind.ERROR,  d.doCheckName(" ").kind);
        assertEquals(FormValidation.Kind.OK,  d.doCheckName("Component").kind);


    }

    @Test
    @WithoutJenkins
    public void testDoCheckRegexpFirstJob() {
        DeliveryPipelineView.RegExpSpec.DescriptorImpl d = new DeliveryPipelineView.RegExpSpec.DescriptorImpl();
        assertEquals(FormValidation.Kind.OK, d.doCheckRegexp(null).kind);
        assertEquals(FormValidation.Kind.ERROR, d.doCheckRegexp("*").kind);
        assertEquals(FormValidation.Kind.ERROR, d.doCheckRegexp("^build-.+?-project").kind);
        assertEquals(FormValidation.Kind.OK, d.doCheckRegexp("^build-(.+?)-project").kind);
    }

    @Test
    @WithoutJenkins
    public void testUpdateInterval() {
        DeliveryPipelineView view = new DeliveryPipelineView("Pipeline");
        view.setUpdateInterval(0);
        assertEquals(2, view.getUpdateInterval());
    }

    @Test
    @WithoutJenkins
    public void testFullScreenCss() {
        DeliveryPipelineView view = new DeliveryPipelineView("Pipeline");
        view.setFullScreenCss(null);
        assertNull(view.getFullScreenCss());
        view.setFullScreenCss(" ");
        assertNull(view.getFullScreenCss());
        view.setFullScreenCss("http://somewhere.com");
        assertEquals("http://somewhere.com", view.getFullScreenCss());

    }

    @Test
    @WithoutJenkins
    public void testEmbeddedCss() {
        DeliveryPipelineView view = new DeliveryPipelineView("Pipeline");
        view.setEmbeddedCss(null);
        assertNull(view.getEmbeddedCss());
        view.setEmbeddedCss(" ");
        assertNull(view.getEmbeddedCss());
        view.setEmbeddedCss("http://somewhere.com");
        assertEquals("http://somewhere.com", view.getEmbeddedCss());

    }

    @Test
    public void testDoFillSortingItems() {
        ListBoxModel model = new DeliveryPipelineView.DescriptorImpl().doFillSortingItems();
        assertNotNull(model);
        assertTrue(model.size() >= 1);
    }

    @Test
    public void testDoFillNoOfPipelinesItems() {
        ListBoxModel model = new DeliveryPipelineView.DescriptorImpl().doFillNoOfPipelinesItems(jenkins.getInstance());
        assertNotNull(model);
        assertTrue(model.size() != 0);
    }

    @Test
    public void testDoFillNoOfColumnsItems() {
        ListBoxModel model = new DeliveryPipelineView.DescriptorImpl().doFillNoOfColumnsItems(jenkins.getInstance());
        assertNotNull(model);
        assertTrue(model.size() != 0);
    }

    @Test
    public void testGetPipelinesRegExp() throws Exception {
        jenkins.createFreeStyleProject("compile-Project1");
        jenkins.createFreeStyleProject("compile-Project2");
        jenkins.createFreeStyleProject("compile-Project3");
        jenkins.createFreeStyleProject("compile");

        DeliveryPipelineView.RegExpSpec regExpSpec = new DeliveryPipelineView.RegExpSpec("^compile-(.*)");
        List<DeliveryPipelineView.RegExpSpec> regExpSpecs = new ArrayList<DeliveryPipelineView.RegExpSpec>();
        regExpSpecs.add(regExpSpec);

        DeliveryPipelineView view = new DeliveryPipelineView("Pipeline");
        view.setRegexpFirstJobs(regExpSpecs);
        assertEquals(regExpSpecs, view.getRegexpFirstJobs());

        jenkins.getInstance().addView(view);

        List<Component> components = view.getPipelines();
        assertNull(view.getError());
        assertEquals(3, components.size());

        List<String> names = new ArrayList<String>();

        for (Component component : components) {
            names.add(component.getName());
        }

        assertTrue(names.contains("Project1"));
        assertTrue(names.contains("Project2"));
        assertTrue(names.contains("Project3"));

        assertEquals(3, view.getItems().size());
    }

    @Test
    @WithoutJenkins
    public void testGetApi() {
        DeliveryPipelineView view = new DeliveryPipelineView("Pipeline");
        Api api = view.getApi();
        assertTrue(api instanceof PipelineApi);
    }

    @Test
    @WithoutJenkins
    public void withoutFolderPrefixShouldRemoveFolderPrefixIfPresentInProjectName() {
        final String projectName = "Job1";
        final String projectNameWithFolderPrefix = "Folder1/" + projectName;
        assertEquals(projectName, DeliveryPipelineView.withoutFolderPrefix(projectNameWithFolderPrefix));
    }

    @Test
    @WithoutJenkins
    public void withoutFolderPrefixShouldReturnProjectNameIfNoFolderPrefixIsPresent() {
        final String projectNameWithoutFolderPrefix = "Job2";
        assertEquals(projectNameWithoutFolderPrefix, DeliveryPipelineView.withoutFolderPrefix(projectNameWithoutFolderPrefix));
    }

    @Test // JENKINS-23532
    @WithoutJenkins
    public void triggerExceptionMessageShouldSuggestRemovingFolderPrefixIfPresent() {
        final String projectName = "Job3";
        final String projectNameWithFolderPrefix = "Folder2/" + projectName;
        final String exceptionMessage = DeliveryPipelineView.triggerExceptionMessage(projectNameWithFolderPrefix, "upstream", "1");
        assertTrue(exceptionMessage.contains(projectNameWithFolderPrefix));
        assertTrue(exceptionMessage.contains("Did you mean to specify " + projectName + "?"));
    }

    @Ignore("Ignored due to JenkinsRule 404 for the URL /plugin/jquery-ui/js/jquery-ui-1.8.9.custom.min.js")
    @Test
    public void testDoCreateItem() throws Exception {
        testDoCreateItem("testDoCreateItem", "");

        DeliveryPipelineView view = new DeliveryPipelineView("Delivery Pipeline");
        jenkins.hudson.addView(view);
        
        testDoCreateItem("testDoCreateItemAsTheDefaultViewFromTheViewUrl", "view/Delivery%20Pipeline/");

        jenkins.hudson.setPrimaryView(view);
        testDoCreateItem("testDoCreateItemAsTheDefaultView", "");
    }

    private void testDoCreateItem(String projectName, String baseUrl) throws Exception {
        HtmlPage page = jenkins.createWebClient().goTo(baseUrl + "newJob");
        HtmlForm form = page.getFormByName("createItem");
        form.getInputByName("name").setValueAttribute(projectName);
        form.getRadioButtonsByName("mode").get(0).setChecked(true);
        jenkins.submit(form);
        
        assertTrue(jenkins.jenkins.getJobNames().contains(projectName));
    }

    @Test
    public void testTriggerManualNoTriggerFound() throws Exception {
        jenkins.createFreeStyleProject("A");
        jenkins.createFreeStyleProject("B");
        DeliveryPipelineView view = new DeliveryPipelineView("View");
        try {
            view.triggerManual("B", "A", "#1");
            fail();
        } catch (TriggerException e) {
            //Should throw this
        } catch (AuthenticationException e) {
            fail();
        }
    }

    @Test
    public void testTriggerManualNoBuildFound() throws Exception {
        FreeStyleProject a = jenkins.createFreeStyleProject("A");
        jenkins.createFreeStyleProject("B");
        a.getPublishersList().add(new BuildPipelineTrigger("B", null));

        jenkins.getInstance().rebuildDependencyGraph();
        DeliveryPipelineView view = new DeliveryPipelineView("View");
        jenkins.getInstance().addView(view);
        try {
            view.triggerManual("B", "A", "#1");
            fail();
        } catch (TriggerException e) {
            //Should throw this
        } catch (AuthenticationException e) {
            fail();
        }
    }


    @Test
    public void testTriggerManualNotAuthorized() throws Exception {
        FreeStyleProject a = jenkins.createFreeStyleProject("A");
        jenkins.createFreeStyleProject("B");
        a.getPublishersList().add(new BuildPipelineTrigger("B", null));

        jenkins.getInstance().rebuildDependencyGraph();
        DeliveryPipelineView view = new DeliveryPipelineView("View");
        jenkins.getInstance().addView(view);

        jenkins.getInstance().setSecurityRealm(jenkins.createDummySecurityRealm());
        GlobalMatrixAuthorizationStrategy gmas = new GlobalMatrixAuthorizationStrategy();
        gmas.add(Permission.READ, "devel");
        jenkins.getInstance().setAuthorizationStrategy(gmas);

        SecurityContext oldContext = ACL.impersonate(User.get("devel").impersonate());
        try {
            view.triggerManual("B", "A", "#1");
            fail();
        } catch (TriggerException e) {
            fail();
        } catch (AuthenticationException e) {
            //Should throw this
        }
        SecurityContextHolder.setContext(oldContext);
    }

    @Test
    @Bug(22658)
    public void testRecursiveStages() throws Exception {

        FreeStyleProject a = jenkins.createFreeStyleProject("A");
        a.addProperty(new PipelineProperty("A", "A"));
        FreeStyleProject b = jenkins.createFreeStyleProject("B");
        b.addProperty(new PipelineProperty("B", "B"));
        FreeStyleProject c = jenkins.createFreeStyleProject("C");
        c.addProperty(new PipelineProperty("C", "C"));
        FreeStyleProject d = jenkins.createFreeStyleProject("D");
        d.addProperty(new PipelineProperty("D", "B"));

        a.getPublishersList().add(new hudson.plugins.parameterizedtrigger.BuildTrigger(new BuildTriggerConfig("B", ResultCondition.SUCCESS, new ArrayList<AbstractBuildParameterFactory>())));
        b.getPublishersList().add(new hudson.plugins.parameterizedtrigger.BuildTrigger(new BuildTriggerConfig("C", ResultCondition.SUCCESS, new ArrayList<AbstractBuildParameterFactory>())));
        c.getPublishersList().add(new hudson.plugins.parameterizedtrigger.BuildTrigger(new BuildTriggerConfig("D", ResultCondition.SUCCESS, new ArrayList<AbstractBuildParameterFactory>())));

        jenkins.getInstance().rebuildDependencyGraph();

        DeliveryPipelineView view = new DeliveryPipelineView("Pipeline");
        List<DeliveryPipelineView.ComponentSpec> componentSpecs = new ArrayList<DeliveryPipelineView.ComponentSpec>();
        componentSpecs.add(new DeliveryPipelineView.ComponentSpec("Comp", "A"));
        view.setComponentSpecs(componentSpecs);

        jenkins.getInstance().addView(view);

        List<Component> components = view.getPipelines();
        assertEquals(0, components.size());
        assertNotNull(view.getError());
        assertTrue(view.getError().startsWith("Circular dependencies between stages: "));
        assertTrue(view.getError().contains("B"));
        assertTrue(view.getError().contains("C"));

    }

    @Test
    public void testInvalidSorter() throws Exception {
        jenkins.createFreeStyleProject("A");
        jenkins.createFreeStyleProject("B");

        DeliveryPipelineView view = new DeliveryPipelineView("Pipeline");
        List<DeliveryPipelineView.ComponentSpec> componentSpecs = new ArrayList<DeliveryPipelineView.ComponentSpec>();
        componentSpecs.add(new DeliveryPipelineView.ComponentSpec("Comp2", "A"));
        componentSpecs.add(new DeliveryPipelineView.ComponentSpec("Comp1", "B"));
        view.setComponentSpecs(componentSpecs);
        view.setShowAggregatedPipeline(true);
        view.setSorting("this will not be found");
        jenkins.getInstance().addView(view);

        List<Component> components = view.getPipelines();
        assertEquals(2, components.size());
        assertEquals("Comp2", components.get(0).getName());
        assertEquals("Comp1", components.get(1).getName());

        view.setSorting(null);

        components = view.getPipelines();
        assertEquals(2, components.size());
        assertEquals("Comp2", components.get(0).getName());
        assertEquals("Comp1", components.get(1).getName());


    }

    @Test
    public void testNoneSorter() throws Exception {
        jenkins.createFreeStyleProject("A");
        jenkins.createFreeStyleProject("B");

        DeliveryPipelineView view = new DeliveryPipelineView("Pipeline");
        List<DeliveryPipelineView.ComponentSpec> componentSpecs = new ArrayList<DeliveryPipelineView.ComponentSpec>();
        componentSpecs.add(new DeliveryPipelineView.ComponentSpec("Comp2", "A"));
        componentSpecs.add(new DeliveryPipelineView.ComponentSpec("Comp1", "B"));
        view.setComponentSpecs(componentSpecs);
        view.setShowAggregatedPipeline(true);
        view.setSorting("none");
        jenkins.getInstance().addView(view);

        List<Component> components = view.getPipelines();
        assertEquals(2, components.size());
        assertEquals("Comp2", components.get(0).getName());
        assertEquals("Comp1", components.get(1).getName());
        assertEquals("A", components.get(0).getFirstJob());
        assertEquals("B", components.get(1).getFirstJob());
    }

    /**
     * This testcase just validates the bug in BPP since BPP don´t handle folders very well
     */
    @Test
    @Bug(23532)
    public void testFolderInManualTrigger() throws Exception {
        MockFolder folder = jenkins.createFolder("folder");
        FreeStyleProject a = folder.createProject(FreeStyleProject.class, "A");
        folder.createProject(FreeStyleProject.class, "B");

        a.getPublishersList().add(new BuildPipelineTrigger("folder/B", null));

        jenkins.getInstance().rebuildDependencyGraph();

        Pipeline pipeline = Pipeline.extractPipeline("Pipe", a);
        assertNotNull(pipeline);
        assertEquals(2, pipeline.getStages().size());

        DeliveryPipelineView view = new DeliveryPipelineView("Pipe");

        jenkins.getInstance().addView(view);

        jenkins.buildAndAssertSuccess(a);
        try {
            view.triggerManual("Folder/B", "Folder/A", "1");
            fail();
        } catch (TriggerException e) {
            //Should happen
        } catch (AuthenticationException e) {
            fail();
        }

    }




}
