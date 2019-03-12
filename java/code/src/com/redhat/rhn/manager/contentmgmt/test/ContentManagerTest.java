/**
 * Copyright (c) 2019 SUSE LLC
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */

package com.redhat.rhn.manager.contentmgmt.test;

import com.redhat.rhn.common.hibernate.HibernateFactory;
import com.redhat.rhn.common.security.PermissionException;
import com.redhat.rhn.domain.channel.Channel;
import com.redhat.rhn.domain.channel.ChannelFactory;
import com.redhat.rhn.domain.channel.test.ChannelFactoryTest;
import com.redhat.rhn.domain.contentmgmt.ContentEnvironment;
import com.redhat.rhn.domain.contentmgmt.ContentManagementException;
import com.redhat.rhn.domain.contentmgmt.ContentProject;
import com.redhat.rhn.domain.contentmgmt.ContentProjectFactory;
import com.redhat.rhn.domain.contentmgmt.ContentProjectHistoryEntry;
import com.redhat.rhn.domain.contentmgmt.EnvironmentTarget;
import com.redhat.rhn.domain.contentmgmt.ProjectSource;
import com.redhat.rhn.domain.errata.Errata;
import com.redhat.rhn.domain.errata.test.ErrataFactoryTest;
import com.redhat.rhn.domain.org.Org;
import com.redhat.rhn.domain.rhnpackage.Package;
import com.redhat.rhn.domain.rhnpackage.test.PackageTest;
import com.redhat.rhn.domain.user.User;
import com.redhat.rhn.frontend.xmlrpc.InvalidChannelLabelException;
import com.redhat.rhn.manager.EntityExistsException;
import com.redhat.rhn.manager.EntityNotExistsException;
import com.redhat.rhn.manager.contentmgmt.ContentManager;
import com.redhat.rhn.testing.BaseTestCaseWithUser;
import com.redhat.rhn.testing.ChannelTestUtils;
import com.redhat.rhn.testing.TestUtils;
import com.redhat.rhn.testing.UserTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.redhat.rhn.domain.contentmgmt.ProjectSource.State.ATTACHED;
import static com.redhat.rhn.domain.contentmgmt.ProjectSource.State.BUILT;
import static com.redhat.rhn.domain.contentmgmt.ProjectSource.State.DETACHED;
import static com.redhat.rhn.domain.contentmgmt.ProjectSource.Type.SW_CHANNEL;
import static com.redhat.rhn.domain.role.RoleFactory.ORG_ADMIN;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Tests for ContentManager
 */
public class ContentManagerTest extends BaseTestCaseWithUser {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        user.addPermanentRole(ORG_ADMIN);
    }

    /**
     * Test creating & looking up Content Project
     */
    public void testLookupContentProject() {
        ContentProject cp = ContentManager.createProject("cplabel", "cpname", "description", user);
        assertNotNull(cp.getId());

        Optional<ContentProject> fromDb = ContentManager.lookupProject("cplabel", user);
        assertEquals(cp, fromDb.get());
    }

    /**
     * Test creating & looking up nonexisting Content Project
     */
    public void testLookupNonexistingProject() {
        assertFalse(ContentManager.lookupProject("idontexist", user).isPresent());
    }

    /**
     * Test creating & listing Content Projects
     */
    public void testListContentProjects() {
        ContentProject cp1 = ContentManager.createProject("cplabel1", "cpname1", "description1", user);
        ContentProject cp2 = ContentManager.createProject("cplabel2", "cpname2", "description2", user);
        Org rangersOrg = UserTestUtils.createNewOrgFull("rangers");
        User anotherAdmin = UserTestUtils.createUser("Chuck", rangersOrg.getId());
        anotherAdmin.addPermanentRole(ORG_ADMIN);
        ContentProject cp3 = ContentManager.createProject("cplabel3", "cpname3", "description3", anotherAdmin);

        assertEquals(Arrays.asList(cp1, cp2), ContentManager.listProjects(user));
        assertEquals(singletonList(cp3), ContentManager.listProjects(anotherAdmin));
    }

    /**
     * Test multiple creating Content Project with same label
     */
    public void testMultipleCreateContentProject() {
        ContentManager.createProject("cplabel", "cpname", "description", user);
        try {
            ContentManager.createProject("cplabel", "differentname", null, user);
            fail("An exception should have been thrown");
        }
        catch (EntityExistsException e) {
            // expected
        }
    }

    /**
     * Test updating Content Project
     */
    public void testUpdateContentProject() {
        ContentProject cp = ContentManager.createProject("cplabel", "cpname", "description", user);
        ContentProject updated = ContentManager.updateProject(cp.getLabel(), of("new name"), of("new desc"), user);

        ContentProject fromDb = ContentManager.lookupProject("cplabel", user).get();
        assertEquals(fromDb, updated);
        assertEquals("new name", fromDb.getName());
        assertEquals("new desc", fromDb.getDescription());
    }

    /**
     * Test removing Content Project
     */
    public void testRemoveContentProject() {
        ContentProject cp = ContentManager.createProject("cplabel", "cpname", "description", user);
        int entitiesAffected = ContentManager.removeProject(cp.getLabel(), user);
        assertEquals(1, entitiesAffected);
        Optional<ContentProject> fromDb = ContentManager.lookupProject("cplabel", user);
        assertFalse(fromDb.isPresent());
    }

    /**
     * Tests various operations performed by a user from different organization
     */
    public void testContentProjectCrossOrg() {
        ContentProject cp = ContentManager.createProject("cplabel", "cpname", "description", user);

        Org rangersOrg = UserTestUtils.createNewOrgFull("rangers");
        User anotherUser = UserTestUtils.createUser("Chuck", rangersOrg.getId());
        anotherUser.addPermanentRole(ORG_ADMIN);

        assertFalse(ContentManager.lookupProject(cp.getLabel(), anotherUser).isPresent());

        try {
            ContentManager.updateProject(cp.getLabel(), of("new name"), of("new desc"), anotherUser);
            fail("An exception should have been thrown");
        }
        catch (EntityNotExistsException e) {
            // no-op
        }

        try {
            ContentManager.removeProject(cp.getLabel(), anotherUser);
            fail("An exception should have been thrown");
        }
        catch (EntityNotExistsException e) {
            // no-op
        }
    }

    /**
     * Tests permissions for Content Project CRUD
     */
    public void testContentProjectPermissions() {
        User guy = UserTestUtils.createUser("Regular user", user.getOrg().getId());

        try {
            ContentManager.createProject("cplabel", "cpname", "description", guy);
            fail("An exception should have been thrown");
        }
        catch (PermissionException e) {
            // expected
        }
        try {
            ContentManager.updateProject("cplabel", empty(), empty(), guy);
            fail("An exception should have been thrown");
        }
        catch (PermissionException e) {
            // expected
        }
        try {
            ContentManager.removeProject("cplabel", guy);
            fail("An exception should have been thrown");
        }
        catch (PermissionException e) {
            // expected
        }

        ContentProject project = ContentManager.createProject("cplabel", "cpname", "description", user);
        assertEquals(project, ContentManager.lookupProject("cplabel", guy).get());
    }

    /**
     * Tests creating, looking up and removing Environments in a Project
     */
    public void testContentEnvironmentLifecycle() {
        ContentProject cp = ContentManager.createProject("cplabel", "cpname", "description", user);

        ContentEnvironment fst = ContentManager.
                createEnvironment(cp.getLabel(), empty(), "fst", "first env", "desc", user);
        ContentEnvironment snd = ContentManager
                .createEnvironment(cp.getLabel(), of(fst.getLabel()), "snd", "second env", "desc2", user);
        assertEquals(asList(fst, snd), ContentProjectFactory.listProjectEnvironments(cp));

        ContentEnvironment mid = ContentManager.
                createEnvironment(cp.getLabel(), of(fst.getLabel()), "mid", "middle env", "desc", user);
        assertEquals(asList(fst, mid, snd), ContentProjectFactory.listProjectEnvironments(cp));

        int numRemoved = ContentManager.removeEnvironment(fst.getLabel(), cp.getLabel(), user);
        assertEquals(1, numRemoved);
        assertEquals(asList(mid, snd), ContentProjectFactory.listProjectEnvironments(cp));
        assertEquals(mid, cp.getFirstEnvironmentOpt().get());
    }

    /**
     * Test behavior when appending a Content Environment behind non-existing Content Environment
     */
    public void testAddingEnvironmentAfterMismatchedPredecessor() {
        ContentProject cp = ContentManager.createProject("cplabel", "cpname", "description", user);
        ContentManager.createEnvironment(cp.getLabel(), empty(), "fst", "first env", "desc", user);
        try {
            ContentManager.createEnvironment(cp.getLabel(), of("NONEXISTING"), "snd", "snd env", "desc",
                    user);
            fail("An exception should have been thrown");
        }
        catch (EntityNotExistsException e) {
            // expected
        }
    }

    /**
     * Test updating a Content Environment
     */
    public void testUpdateContentEnvironment() {
        ContentProject cp = ContentManager.createProject("cplabel", "cpname", "description", user);
        ContentManager
                .createEnvironment(cp.getLabel(), empty(), "fst", "first env", "desc", user);
        ContentManager.updateEnvironment("fst", "cplabel", of("new env name"),
                of("new description"), user);
        ContentEnvironment fromDb = ContentManager
                .lookupEnvironment("fst", "cplabel", user).get();

        assertEquals("new env name", fromDb.getName());
        assertEquals("new description", fromDb.getDescription());
    }

    /**
     * Tests permissions for Environment CRUD
     */
    public void testEnvironmentPermissions() {
        ContentProject project = ContentManager.createProject("cplabel", "cpname", "description", user);
        ContentEnvironment env =
                ContentManager.createEnvironment("cplabel", empty(), "dev", "dev env", "...", user);

        User guy = UserTestUtils.createUser("Regular user", user.getOrg().getId());
        assertEquals(env, ContentManager.lookupEnvironment("dev", "cplabel", guy).get());
        assertEquals(singletonList(env), ContentManager.listProjectEnvironments("cplabel", guy));

        try {
            ContentManager.createEnvironment("cplabel", empty(), "dev", "dev env", "...", guy);
            fail("An exception should have been thrown");
        }
        catch (PermissionException e) {
            // expected
        }
        try {
            ContentManager.updateEnvironment("dev", "cplabel", empty(), empty(), guy);
            fail("An exception should have been thrown");
        }
        catch (PermissionException e) {
            // expected
        }
        try {
            ContentManager.removeEnvironment("dev", "cplabel", guy);
            fail("An exception should have been thrown");
        }
        catch (PermissionException e) {
            // expected
        }
    }

    /**
     * Test attaching/detaching Project Sources, the happy path scenario
     *
     * @throws Exception - if anything goes wrong
     */
    public void testChangeProjectSource() throws Exception {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        Channel channel = ChannelTestUtils.createBaseChannel(user);

        ProjectSource source = ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);
        ProjectSource fromDb = ContentManager.lookupProjectSource("cplabel", SW_CHANNEL, channel.getLabel(), user).get();
        assertEquals(source, fromDb);
        assertEquals(channel, fromDb.asSoftwareSource().get().getChannel());
        assertEquals(singletonList(source), cp.getSources());

        source.setState(BUILT); // programmatically set to BUILT (normally this happens after building project)
        ContentManager.detachSource("cplabel", SW_CHANNEL, channel.getLabel(), user);
        ProjectSource projectSource = ContentProjectFactory.lookupProjectSource(cp, SW_CHANNEL, channel.getLabel(), user).get();
        assertEquals(DETACHED, projectSource.getState());
        assertEquals(singletonList(source), cp.getSources());
    }

    /**
     * Test attaching Source
     *
     * @throws Exception - if anything goes wrong
     */
    public void testAttachProjectSource() throws Exception {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        Channel channel = ChannelTestUtils.createBaseChannel(user);

        ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);

        assertEquals(1, cp.getSources().size());
        assertEquals(ATTACHED, cp.getSources().get(0).getState());
    }

    /**
     * Test attaching same Source multiple times
     *
     * @throws Exception - if anything goes wrong
     */
    public void testMultipleAttachProjectSource() throws Exception {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        Channel channel = ChannelTestUtils.createBaseChannel(user);

        ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);
        ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);

        assertEquals(1, cp.getSources().size());
        assertEquals(ATTACHED, cp.getSources().get(0).getState());
    }

    /**
     * Test attaching built Source multiple times
     *
     * @throws Exception - if anything goes wrong
     */
    public void testAttachBuiltProjectSource() throws Exception {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        Channel channel = ChannelTestUtils.createBaseChannel(user);

        ProjectSource src = ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);
        src.setState(BUILT); // programmatically set to BUILT (normally this happens after building project)

        // attach the same source
        ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);

        // it should stay in BUILT state
        assertEquals(1, cp.getSources().size());
        assertEquals(BUILT, cp.getSources().get(0).getState());
    }

    /**
     * Test attaching detached Source multiple times
     *
     * @throws Exception - if anything goes wrong
     */
    public void testAttachDetachedProjectSource() throws Exception {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        Channel channel = ChannelTestUtils.createBaseChannel(user);

        ProjectSource src = ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);
        src.setState(DETACHED); // programmatically set to DETACHED (normally this happens when detaching BUILT source))

        // attach the same source
        ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);

        // it should go back to BUILT state
        assertEquals(1, cp.getSources().size());
        assertEquals(BUILT, cp.getSources().get(0).getState());
    }

    /**
     * Test attaching source to with some missing data (e.g. missing channel)
     *
     * @throws Exception - if anything goes wrong
     */
    public void testAttachSourceMissingEntities() throws Exception {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        Channel channel = ChannelTestUtils.createBaseChannel(user);

        try {
            ContentManager.attachSource("cplabel", SW_CHANNEL, "notthere", empty(), user);
            fail("An exception should have been thrown");
        }
        catch (EntityNotExistsException e) {
            // expected
        }

        try {
            ContentManager.attachSource("idontexist", SW_CHANNEL, channel.getLabel(), empty(), user);
            fail("An exception should have been thrown");
        }
        catch (EntityNotExistsException e) {
            // expected
        }
    }

    /**
     * Test detaching a {@link ProjectSource}
     *
     * @throws Exception - if anything goes wrong
     */
    public void testDetachProjectSource() throws Exception {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        Channel channel = ChannelTestUtils.createBaseChannel(user);

        ProjectSource src = ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);
        src.setState(BUILT); // programmatically set to BUILT (normally this happens after building project)
        ContentManager.detachSource("cplabel", SW_CHANNEL, channel.getLabel(), user);

        assertEquals(1, cp.getSources().size());
        assertEquals(DETACHED, cp.getSources().get(0).getState());
    }

    /**
     * Test detaching a {@link ProjectSource}
     *
     * @throws Exception - if anything goes wrong
     */
    public void testDetachAttachedProjectSource() throws Exception {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        Channel channel = ChannelTestUtils.createBaseChannel(user);

        ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);
        ContentManager.detachSource("cplabel", SW_CHANNEL, channel.getLabel(), user);

        // freshly attached, non-built source should be removed after detaching
        assertEquals(0, cp.getSources().size());
    }

    /**
     * Test detaching same Source multiple times
     *
     * @throws Exception - if anything goes wrong
     */
    public void testMultipleDetachingProjectSource() throws Exception {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        Channel channel = ChannelTestUtils.createBaseChannel(user);

        ProjectSource src = ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);
        src.setState(BUILT); // programmatically set to BUILT (normally this happens after building project)
        ContentManager.detachSource("cplabel", SW_CHANNEL, channel.getLabel(), user);
        ContentManager.detachSource("cplabel", SW_CHANNEL, channel.getLabel(), user);

        assertEquals(1, cp.getSources().size());
        assertEquals(DETACHED, cp.getSources().get(0).getState());
    }

    /**
     * Test deleting underlying channel of a Source.
     * In this case, the Source is supposed to be deleted too.
     *
     * @throws Exception - if anything goes wrong
     */
    public void testDeleteSourceChannel() throws Exception {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        Channel channel = ChannelTestUtils.createBaseChannel(user);
        ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);

        assertTrue(ContentManager.lookupProjectSource("cplabel", SW_CHANNEL, channel.getLabel(), user).isPresent());
        ChannelFactory.remove(channel);
        assertFalse(ContentManager.lookupProjectSource("cplabel", SW_CHANNEL, channel.getLabel(), user).isPresent());
    }

    /**
     * Test deleting underlying Project of a Source.
     * In this case, the Source is supposed to be deleted too.
     *
     * @throws Exception - if anything goes wrong
     */
    public void testDeleteSourceProject() throws Exception {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        Channel channel = ChannelTestUtils.createBaseChannel(user);
        ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);

        assertTrue(ContentManager.lookupProjectSource("cplabel", SW_CHANNEL, channel.getLabel(), user).isPresent());
        ContentManager.removeProject("cplabel", user);
        // we can't use ContentManager.lookupProjectSource because the project does not exist
        assertTrue(HibernateFactory.getSession()
                .createQuery("SELECT 1 FROM SoftwareProjectSource s where s.contentProject = :cp")
                .setParameter("cp", cp)
                .list()
                .isEmpty());
    }

    /**
     * Test building project with no environments
     */
    public void testBuildProjectNoEnvs() {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        try {
            ContentManager.buildProject("cplabel", empty(), false, user);
            fail("An exception should have been thrown");
        }
        catch (ContentManagementException e) {
            // expected
        }
    }

    /**
     * Test building project with no sources assigned
     */
    public void testBuildProjectNoSources() {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        ContentEnvironment env = ContentManager.createEnvironment(cp.getLabel(), empty(), "fst", "first env", "desc", user);
        try {
            ContentManager.buildProject("cplabel", empty(), false, user);
            fail("An exception should have been thrown");
        }
        catch (ContentManagementException e) {
            // expected
        }
    }

    /**
     * Test building project - complex scenario with multiple builds and changing sources
     *
     * @throws Exception if anything goes wrong
     */
    public void testBuildProject() throws Exception {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        ContentEnvironment env = ContentManager.createEnvironment(cp.getLabel(), empty(), "fst", "first env", "desc", user);

        // 1. build a project with a source
        Channel channel = createPopulatedChannel();
        ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);
        assertEquals(channel, cp.lookupSwSourceLeader().get().getChannel());
        ContentManager.buildProject("cplabel", empty(), false, user);

        List<EnvironmentTarget> tgts = ContentProjectFactory.lookupEnvironmentTargets(env).collect(toList());
        assertEquals(1, tgts.size());
        Channel tgtChannel = tgts.get(0).asSoftwareTarget().get().getChannel();
        assertEquals("cplabel-fst-" + channel.getLabel(), tgtChannel.getLabel());
        assertTrue(channel.getClonedChannels().contains(tgtChannel));
        assertEquals(channel, tgtChannel.getOriginal());
        assertEquals(channel.getPackages(), tgtChannel.getPackages());
        assertEquals(channel.getErratas(), tgtChannel.getErratas());

        // 2. change a project source and rebuild
        Channel newChannel = createPopulatedChannel();
        ContentManager.attachSource("cplabel", SW_CHANNEL, newChannel.getLabel(), empty(), user);
        assertEquals(channel, cp.lookupSwSourceLeader().get().getChannel()); // leader is the same
        ContentManager.buildProject("cplabel", empty(), false, user);

        tgts = ContentProjectFactory.lookupEnvironmentTargets(env).collect(toList());
        assertEquals(2, tgts.size());
        Set<String> tgtLabels = tgts.stream().map(tgt -> tgt.asSoftwareTarget().get().getChannel().getLabel()).collect(toSet());
        assertContains(tgtLabels, "cplabel-fst-" + channel.getLabel());
        assertContains(tgtLabels, "cplabel-fst-" + newChannel.getLabel());
        Channel base = tgts.stream()
                .filter(t -> t.asSoftwareTarget().get().getChannel().getLabel().equals("cplabel-fst-" + channel.getLabel()))
                .findFirst().get().asSoftwareTarget().get().getChannel();
        Channel child = tgts.stream()
                .filter(t -> t.asSoftwareTarget().get().getChannel().getLabel().equals("cplabel-fst-" + newChannel.getLabel()))
                .findFirst().get().asSoftwareTarget().get().getChannel();
        assertNull(base.getParentChannel());
        assertEquals(base, child.getParentChannel());
        assertEquals(channel.getPackages(), tgtChannel.getPackages());
        assertEquals(channel.getErratas(), tgtChannel.getErratas());

        // 3. remove a source and rebuild
        ContentManager.detachSource("cplabel", SW_CHANNEL, channel.getLabel(), user);
        cp = (ContentProject) HibernateFactory.reload(cp);
        ContentManager.buildProject("cplabel", empty(), false, user);

        assertEquals(newChannel, cp.lookupSwSourceLeader().get().getChannel()); // leader is changed
        tgts = ContentProjectFactory.lookupEnvironmentTargets(env).collect(toList());
        assertEquals(1, tgts.size());
        assertEquals("cplabel-fst-" + newChannel.getLabel(), tgts.get(0).asSoftwareTarget().get().getChannel().getLabel());
        assertEquals(channel.getPackages(), tgtChannel.getPackages());
        assertEquals(channel.getErratas(), tgtChannel.getErratas());
    }

    /**
     * Test that if a matching target channel already exists, it is used when creating a new environment target.
     *
     * @throws Exception if anything goes wrong
     */
    public void testBuildProjectExistingChannel() throws Exception {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        ContentEnvironment env = ContentManager.createEnvironment(cp.getLabel(), empty(), "fst", "first env", "desc", user);

        Channel channel = createPopulatedChannel();
        ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);

        Channel alreadyExistingTgt = createPopulatedChannel();
        alreadyExistingTgt.setLabel("cplabel-fst-" + channel.getLabel());

        ContentManager.buildProject("cplabel", empty(), false, user);
        List<EnvironmentTarget> environmentTargets = ContentProjectFactory.lookupEnvironmentTargets(env).collect(toList());
        assertEquals(1, environmentTargets.size());
        assertEquals(alreadyExistingTgt, environmentTargets.get(0).asSoftwareTarget().get().getChannel());
    }

    /**
     * Test that if a matching target channel already exists in a different organization, an exception is thrown
     *
     * @throws Exception if anything goes wrong
     */
    public void testBuildProjectExistingChannelCrossOrg() throws Exception {
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        ContentManager.createEnvironment(cp.getLabel(), empty(), "fst", "first env", "desc", user);

        Channel channel = createPopulatedChannel();
        ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);

        Channel alreadyExistingTgt = createPopulatedChannel();

        Org otherOrg = UserTestUtils.createNewOrgFull("testOrg2");
        alreadyExistingTgt.setOrg(otherOrg);
        alreadyExistingTgt.setLabel("cplabel-fst-" + channel.getLabel());

        try {
            ContentManager.buildProject("cplabel", empty(), false, user);
            fail("An exception should have been thrown");
        }
        catch (InvalidChannelLabelException e) {
            // expected
        }
    }

    /**
     * Test multiple users interacting with a Project
     *
     * @throws Exception if anything goes wrong
     */
    public void testBuildProjectTwoUsers() throws Exception {
        User adminSameOrg = UserTestUtils.createUser("adminInSameOrg", user.getOrg().getId());
        adminSameOrg.addPermanentRole(ORG_ADMIN);
        User userSameOrg = UserTestUtils.createUser("userInSameOrg", user.getOrg().getId());
        Org otherOrg = UserTestUtils.createNewOrgFull("testOrg2");
        User userOtherOrg = UserTestUtils.createUser("userInOtherOrg", otherOrg.getId());

        // project is created by one user
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        ContentManager.createEnvironment(cp.getLabel(), empty(), "fst", "first env", "desc", user);

        // ... build by another user
        Channel channel = createPopulatedChannel(user);
        ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);
        ContentManager.buildProject("cplabel", empty(), false, adminSameOrg);

        ContentEnvironment envUser1 = ContentManager.lookupEnvironment("fst", "cplabel", userSameOrg).get();
        List<EnvironmentTarget> tgts = ContentProjectFactory.lookupEnvironmentTargets(envUser1).collect(toList());
        assertEquals(1, tgts.size());
        Channel tgtChannel = tgts.get(0).asSoftwareTarget().get().getChannel();
        // its assets should be accessible to a regular user in the org
        assertTrue(ChannelFactory.isAccessibleByUser(tgtChannel.getLabel(), userSameOrg.getId()));
        // but not for a user in another org
        assertFalse(ChannelFactory.isAccessibleByUser(tgtChannel.getLabel(), userOtherOrg.getId()));
    }

    /**
     * Tests that the project history entry is added when a project is built
     *
     * @throws Exception if anything goes wrong
     */
    public void testBuildProjectHistoryAdded() throws Exception {
        User adminSameOrg = UserTestUtils.createUser("adminInSameOrg", user.getOrg().getId());
        adminSameOrg.addPermanentRole(ORG_ADMIN);
        ContentProject cp = new ContentProject("cplabel", "cpname", "cpdesc", user.getOrg());
        ContentProjectFactory.save(cp);
        ContentManager.createEnvironment(cp.getLabel(), empty(), "fst", "first env", "desc", user);

        Channel channel = createPopulatedChannel(user);
        ContentManager.attachSource("cplabel", SW_CHANNEL, channel.getLabel(), empty(), user);
        ContentManager.buildProject("cplabel", empty(), false, user);

        List<ContentProjectHistoryEntry> history = ContentManager.lookupProject("cplabel", user).get().getHistoryEntries();
        assertEquals(1, history.size());
        assertEquals(Long.valueOf(1), history.get(0).getVersion());
        assertEquals(user, history.get(0).getUser());

        ContentManager.buildProject("cplabel", empty(), false, adminSameOrg);
        history = ContentManager.lookupProject("cplabel", user).get().getHistoryEntries();
        assertEquals(2, history.size());
        assertEquals(Long.valueOf(2), history.get(1).getVersion());
        assertEquals(adminSameOrg, history.get(1).getUser());
    }

    private Channel createPopulatedChannel() throws Exception {
        return createPopulatedChannel(user);
    }

    private Channel createPopulatedChannel(User user) throws Exception {
        Channel channel = TestUtils.reload(ChannelFactoryTest.createTestChannel(user, false));
        channel.setChecksumType(ChannelFactory.findChecksumTypeByLabel("sha1"));
        Package pack = PackageTest.createTestPackage(user.getOrg());
        Errata errata = ErrataFactoryTest.createTestPublishedErrata(
                user.getOrg().getId());
        channel.addPackage(pack);
        channel.addErrata(errata);
        ChannelFactory.save(channel);
        return channel;
    }
}
