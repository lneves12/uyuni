package com.redhat.rhn.manager.channel.test;

import com.redhat.rhn.common.hibernate.HibernateFactory;
import com.redhat.rhn.domain.channel.Channel;
import com.redhat.rhn.domain.channel.ChannelFactory;
import com.redhat.rhn.domain.channel.test.ChannelFactoryTest;
import com.redhat.rhn.domain.contentmgmt.ContentEnvironment;
import com.redhat.rhn.domain.contentmgmt.ContentProject;
import com.redhat.rhn.domain.contentmgmt.SoftwareEnvironmentTarget;
import com.redhat.rhn.domain.contentmgmt.SoftwareProjectSource;
import com.redhat.rhn.domain.errata.Errata;
import com.redhat.rhn.domain.errata.test.ErrataFactoryTest;
import com.redhat.rhn.domain.rhnpackage.Package;
import com.redhat.rhn.domain.rhnpackage.PackageEvr;
import com.redhat.rhn.domain.rhnpackage.test.PackageEvrFactoryTest;
import com.redhat.rhn.domain.rhnpackage.test.PackageTest;
import com.redhat.rhn.domain.server.InstalledPackage;
import com.redhat.rhn.domain.server.Server;
import com.redhat.rhn.domain.server.test.ServerFactoryTest;
import com.redhat.rhn.manager.channel.ChannelManager;
import com.redhat.rhn.manager.contentmgmt.ContentManager;
import com.redhat.rhn.manager.system.SystemManager;
import com.redhat.rhn.testing.BaseTestCaseWithUser;
import com.redhat.rhn.testing.ChannelTestUtils;
import com.redhat.rhn.testing.ServerTestUtils;

import static com.redhat.rhn.domain.contentmgmt.ProjectSource.Type.SW_CHANNEL;
import static com.redhat.rhn.domain.role.RoleFactory.ORG_ADMIN;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toSet;

/**
 * Test for the content-management related methods in {@link ChannelManager}
 */
public class ChannelManagerContentAlignmentTest extends BaseTestCaseWithUser {

    private ContentProject project;
    private Channel srcChannel;
    private SoftwareProjectSource src;
    private Channel tgtChan;
    private ContentEnvironment devEnv;
    private SoftwareEnvironmentTarget tgt;
    private Errata errata;
    private Package pkg;

    /**
     * In test we prefabricate a content project with single environment and source (sw channel).
     * The source channel has single package and errata.
     * In the tests we'll aligning EnvironmentTarget to this source
     *
     * @throws Exception if anything goes wrong
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        user.addPermanentRole(ORG_ADMIN);

        // create objects needed in all tests:
        // we need at least a package, errata associated with it and a channel
        project = ContentManager.createProject("cplabel", "cpname1", "description1", user);
        pkg = PackageTest.createTestPackage(user.getOrg());
        errata = ErrataFactoryTest.createTestPublishedErrata(user.getOrg().getId());
        errata.addPackage(pkg);
        srcChannel = ChannelFactoryTest.createTestChannel(user, false);
        srcChannel.addPackage(pkg);
        srcChannel.addErrata(errata);

        src = ContentManager.attachSource("cplabel", SW_CHANNEL, srcChannel.getLabel(), empty(), user).asSoftwareSource().get();
        tgtChan = ChannelTestUtils.createBaseChannel(user);
        devEnv = ContentManager.createEnvironment(project.getLabel(), empty(), "fst", "first env", "desc", user);
        tgt = new SoftwareEnvironmentTarget(devEnv, tgtChan);
    }

    /**
     * Test that the packages and errata of the target are aligned to the source.
     *
     * @throws Exception if anything goes wrong
     */
    public void testAlignEntities() throws Exception {
        // let's add a package to the target. it should be removed after aligning
        tgtChan.getPackages().add(PackageTest.createTestPackage(user.getOrg()));
        ChannelManager.alignChannelsSync(src, tgt, user);

        // check that newest packages cache has been updated
        assertEquals(
                srcChannel.getPackages().stream().map(p -> p.getId()).collect(toSet()),
                ChannelManager.latestPackagesInChannel(tgtChan).stream()
                        .map(m -> m.get("id"))
                        .collect(toSet()));

        // check that packages and errata have been aligned
        tgtChan = (Channel) HibernateFactory.reload(tgtChan);
        assertEquals(srcChannel.getPackages(), tgtChan.getPackages());
        assertEquals(srcChannel.getErratas(), tgtChan.getErratas());
    }

    /**
     * Test that the cache of newest packages in the channel is refreshed
     *
     * @throws Exception if anything goes wrong
     */
    public void testNewestPackagesCacheRefreshed() throws Exception {
        Package pack2 = PackageTest.createTestPackage(user.getOrg());

        tgtChan.getPackages().add(pack2);
        ChannelFactory.refreshNewestPackageCache(tgtChan, "java::alignPackages");
        assertEquals(pack2.getId(), ChannelManager.getLatestPackageEqual(tgtChan.getId(), pack2.getPackageName().getName()));
        assertNull(ChannelManager.getLatestPackageEqual(tgtChan.getId(), pkg.getPackageName().getName()));

        ChannelManager.alignChannelsSync(src, tgt, user);
        assertEquals(pkg.getId(), ChannelManager.getLatestPackageEqual(tgtChan.getId(), pkg.getPackageName().getName()));
        assertNull(ChannelManager.getLatestPackageEqual(tgtChan.getId(), pack2.getPackageName().getName()));
    }

    /**
     * Test that the cache of needed packages by server is refreshed.
     *
     * @throws Exception if anything goes wrong
     */
    public void testServerNeededCacheRefreshed() throws Exception {
        Server server = ServerFactoryTest.createTestServer(user);
        // server will have an older package installed -> after the alignment, the rhnServerNeededCache
        // must contain the entry corresponding to the the newer package
        InstalledPackage olderPack = new InstalledPackage();
        PackageEvr packageEvr = pkg.getPackageEvr();
        olderPack.setEvr(PackageEvrFactoryTest.createTestPackageEvr(
                packageEvr.getEpoch(),
                "0.9.9",
                packageEvr.getRelease()
        ));
        olderPack.setArch(pkg.getPackageArch());
        olderPack.setName(pkg.getPackageName());
        olderPack.setServer(server);
        server.getPackages().add(olderPack);

        SystemManager.subscribeServerToChannel(user, server, tgtChan);
        ServerTestUtils.createTestSystem(user);

        ChannelManager.alignChannelsSync(src, tgt, user);
        assertEquals(errata.getId(), SystemManager.relevantErrata(user, server.getId()).get(0).getId());
    }
}