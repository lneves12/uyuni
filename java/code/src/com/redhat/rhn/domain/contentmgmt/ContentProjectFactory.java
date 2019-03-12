/**
 * Copyright (c) 2018 SUSE LLC
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

package com.redhat.rhn.domain.contentmgmt;

import com.redhat.rhn.common.hibernate.HibernateFactory;
import com.redhat.rhn.domain.channel.Channel;
import com.redhat.rhn.domain.channel.ChannelFactory;
import com.redhat.rhn.domain.contentmgmt.ProjectSource.Type;
import com.redhat.rhn.domain.org.Org;
import com.redhat.rhn.domain.user.User;
import org.apache.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static com.suse.utils.Opt.consume;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;

/**
 *  HibernateFactory for the {@link com.redhat.rhn.domain.contentmgmt.ContentProject} class and related classes.
 */
public class ContentProjectFactory extends HibernateFactory {

    private static final ContentProjectFactory INSTANCE = new ContentProjectFactory();
    private static Logger log = Logger.getLogger(ContentProjectFactory.class);

    // forbid  instantiation
    private ContentProjectFactory() {
        super();
    }

    /**
     * Save the ContentFilter
     *
     * @param contentFilter the content filter
     */
    public static void save(ContentFilter contentFilter) {
        INSTANCE.saveObject(contentFilter);
    }

    /**
     * Save the ContentProject
     *
     * @param contentProject - the ContentProject
     */
    public static void save(ContentProject contentProject) {
        INSTANCE.saveObject(contentProject);
    }

    /**
     * Remove a Content Project
     *
     * @param contentProject - the Content Project to remove
     * @return the number of object affected
     */
    public static int remove(ContentProject contentProject) {
        return INSTANCE.removeObject(contentProject);
    }

    /**
     * Looks up a ContentProject by label and organization
     *
     * @param label - the label
     * @param org - the org
     * @return Optional with ContentProject with given label
     */
    public static Optional<ContentProject> lookupProjectByLabelAndOrg(String label, Org org) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<ContentProject> criteria = builder.createQuery(ContentProject.class);
        Root<ContentProject> root = criteria.from(ContentProject.class);
        criteria.where(builder.and(
                builder.equal(root.get("label"), label),
                builder.equal(root.get("org"), org)));
        return getSession().createQuery(criteria).uniqueResultOptional();
    }

    /**
     * List all ContentProjects with given organization
     * @param org - the organization
     * @return the ContentProjects in given organization
     */
    public static List<ContentProject> listProjects(Org org) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<ContentProject> query = builder.createQuery(ContentProject.class);
        Root<ContentProject> root = query.from(ContentProject.class);
        query.where(builder.equal(root.get("org"), org));
        return getSession().createQuery(query).list();
    }

    /**
     * Save a Content Environment in DB
     * @param contentEnvironment the content environment
     */
    public static void save(ContentEnvironment contentEnvironment) {
        INSTANCE.saveObject(contentEnvironment);
    }

    /**
     * Delete a {@link ContentEnvironment} from the database.
     * @param contentEnvironment Environment to be deleted.
     */
    private static void remove(ContentEnvironment contentEnvironment) {
        INSTANCE.removeObject(contentEnvironment);
    }

    /**
     * Lists all Environments of a Content Project with the respect to their ordering.
     *
     * @param project the Content Project
     * @return Environments of the Content Project
     */
    public static List<ContentEnvironment> listProjectEnvironments(ContentProject project) {
        List<ContentEnvironment> result = new LinkedList<>();
        Optional<ContentEnvironment> env = project.getFirstEnvironmentOpt();
        while (env.isPresent()) {
            result.add(env.get());
            env = env.get().getNextEnvironmentOpt();
        }
        return unmodifiableList(result);
    }

    /**
     * Look up Content Environment by label and Content Project label
     *
     * @param label the Environment label
     * @param project the Content Project
     * @return matching Environment
     */
    public static Optional<ContentEnvironment> lookupEnvironmentByLabelAndProject(String label,
            ContentProject project) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<ContentEnvironment> criteria = builder.createQuery(ContentEnvironment.class);
        Root<ContentEnvironment> root = criteria.from(ContentEnvironment.class);
        criteria.where(builder.and(
                builder.equal(root.get("label"), label),
                builder.equal(root.get("contentProject"), project)));
        return getSession().createQuery(criteria).uniqueResultOptional();
    }

    /**
     * Insert the new environment in the path after the given environment.
     * @param newEnv new environment to be inserted
     * @param predecessor optional predecessor:
     *                    if defined -> insert after predecessor
     *                    if empty -> insert as 1st environment in the project
     * @throws ContentManagementException if projects of given environments mismatch
     */
    public static void insertEnvironment(ContentEnvironment newEnv, Optional<ContentEnvironment> predecessor)
            throws ContentManagementException {
        consume(
                predecessor,
                () -> insertFirstEnvironment(newEnv),
                (pred) -> appendEnvironment(newEnv, pred));
    }

    private static void insertFirstEnvironment(ContentEnvironment newEnv) {
        ContentProject contentProject = newEnv.getContentProject();

        ContentEnvironment oldFirstEnvironment = contentProject.getFirstEnvironment();
        if (oldFirstEnvironment != null) {
            newEnv.setNextEnvironment(oldFirstEnvironment);
            oldFirstEnvironment.setPrevEnvironment(newEnv);
            save(oldFirstEnvironment);
        }

        contentProject.setFirstEnvironment(newEnv);
        save(contentProject);
        save(newEnv);
    }

    private static void appendEnvironment(ContentEnvironment newEnv, ContentEnvironment predecessor) {
        if (!newEnv.getContentProject().equals(predecessor.getContentProject())) {
            throw new ContentManagementException("Environments from different Projects");
        }

        predecessor.getNextEnvironmentOpt().ifPresent(successor -> {
            predecessor.setNextEnvironment(newEnv);
            newEnv.setPrevEnvironment(predecessor);
            newEnv.setNextEnvironment(successor);
            successor.setPrevEnvironment(newEnv);
            save(successor);
        });
        if (!predecessor.getNextEnvironmentOpt().isPresent()) {
            predecessor.setNextEnvironment(newEnv);
            newEnv.setPrevEnvironment(predecessor);
        }
        save(newEnv);
        save(predecessor);
    }

    /**
     * Remove an environment from the path. It take care that chain stay intact
     * @param toRemove environment to remove.
     */
    public static void removeEnvironment(ContentEnvironment toRemove) {
        if (toRemove.getNextEnvironmentOpt().isPresent()) {
            ContentEnvironment next = toRemove.getNextEnvironmentOpt().get();
            if (toRemove.getPrevEnvironmentOpt().isPresent()) {
                ContentEnvironment prev = toRemove.getPrevEnvironmentOpt().get();
                prev.setNextEnvironment(next);
                next.setPrevEnvironment(prev);
                save(prev);
            }
            else {
                // First Env - change the project
                ContentProject project = toRemove.getContentProject();
                project.setFirstEnvironment(next);
                next.setPrevEnvironment(null);
                save(project);
            }
            save(next);
        }
        else if (toRemove.getPrevEnvironmentOpt().isPresent()) {
            ContentEnvironment prev = toRemove.getPrevEnvironmentOpt().get();
            prev.setNextEnvironment(null);
            save(prev);
        }
        else {
            // Only Env - change the project
            ContentProject project = toRemove.getContentProject();
            project.setFirstEnvironment(null);
            save(project);
        }
        toRemove.setPrevEnvironment(null);
        toRemove.setNextEnvironment(null);
        remove(toRemove);
    }

    /**
     * Save an Environment Target
     * @param target the Environment Target
     */
    public static void save(EnvironmentTarget target) {
        INSTANCE.saveObject(target);
    }

    /**
     * Purge the Environment Target - delete it and delete its underlying resource (e.g. {@link Channel}) too
     *
     * @param target the Environment Target
     */
    public static void purgeTarget(EnvironmentTarget target) {
        INSTANCE.removeObject(target);
        target.asSoftwareTarget().ifPresent(swTgt -> ChannelFactory.remove(swTgt.getChannel()));
    }

    /**
     * Looks up SoftwareEnvironmentTarget with a given channel
     *
     * @param label the {@link Channel} label
     * @param user the user
     * @return Optional of {@link com.redhat.rhn.domain.contentmgmt.SoftwareEnvironmentTarget}
     */
    public static Optional<SoftwareEnvironmentTarget> lookupEnvironmentTargetByChannelLabel(String label, User user) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<SoftwareEnvironmentTarget> query = builder.createQuery(SoftwareEnvironmentTarget.class);
        Root<SoftwareEnvironmentTarget> from = query.from(SoftwareEnvironmentTarget.class);
        query.where(builder.equal(from.get("channel").get("label"), label));
        return getSession().createQuery(query)
                .uniqueResultOptional()
                .filter(tgt -> ChannelFactory.isAccessibleByUser(tgt.getChannel().getLabel(), user.getId()));
    }

    /**
     * Looks up {@link EnvironmentTarget}s of given {@link ContentEnvironment}
     *
     * @param env the Environment
     * @return the Stream of {@link EnvironmentTarget}s
     */
    public static Stream<EnvironmentTarget> lookupEnvironmentTargets(ContentEnvironment env) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<EnvironmentTarget> query = builder.createQuery(EnvironmentTarget.class);
        Root<EnvironmentTarget> root = query.from(EnvironmentTarget.class);
        query.where(builder.equal(root.get("contentEnvironment"), env));
        return getSession().createQuery(query).stream();
    }

    /**
     * Save a Project source
     *
     * @param source the project source
     */
    public static void save(ProjectSource source) {
        INSTANCE.saveObject(source);
    }

    /**
     * List Project Sources in a Content Project
     *
     * @param cp the content project
     * @return the sources in the project
     */
    public static List<ProjectSource> listProjectSourcesByProject(ContentProject cp) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<ProjectSource> query = builder.createQuery(ProjectSource.class);
        Root<ProjectSource> root = query.from(ProjectSource.class);
        query.where(builder.equal(root.get("contentProject"), cp));
        return getSession().createQuery(query).list();
    }

    /**
     * Save the Content Project history entry
     *
     * @param entry  - the Content Project history entry
     */
    private static void save(ContentProjectHistoryEntry entry) {
        INSTANCE.saveObject(entry);
    }

    /**
     * Add a history entry to a Content Project
     *
     * @param project - Content Project
     * @param entry - the history entry
     */
    public static void addHistoryEntryToProject(ContentProject project, ContentProjectHistoryEntry entry) {
        List<ContentProjectHistoryEntry> entries = project.getHistoryEntries();
        entry.setVersion(entries.size() == 0 ? 1 : entries.get(entries.size() - 1).getVersion() + 1);
        entry.setContentProject(project);
        save(entry);
        entries.add(entry);
        save(project);
    }

    /**
     * Remove a Project Source
     *
     * @param source the Source
     * @return the number of object affected
     */
    public static int remove(ProjectSource source) {
        return INSTANCE.removeObject(source);
    }

    /**
     * Look up Project Source based with given Project and Channel
     *
     * @param project the Project
     * @param sourceType the Source type
     * @param sourceLabel the Source label
     * @param user the User
     * @return Optional with matching Source
     */
    public static Optional<ProjectSource> lookupProjectSource(ContentProject project, Type sourceType,
            String sourceLabel, User user) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<ProjectSource> query = builder.createQuery(sourceType.getSourceClass());
        Root<ProjectSource> root = query.from(sourceType.getSourceClass());

        Predicate sourcePredicate;
        switch (sourceType) {
            case SW_CHANNEL:
                if (!ChannelFactory.isAccessibleByUser(sourceLabel, user.getId())) {
                    return empty();
                }
                sourcePredicate = builder.equal(root.get("channel").get("label"), sourceLabel);
                break;
            default:
                throw new IllegalArgumentException("Unsupported Source type " + sourceType);
        }

        query.where(
                builder.and(
                        builder.equal(root.get("contentProject"), project),
                        sourcePredicate));
        return getSession().createQuery(query).uniqueResultOptional();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Logger getLogger() {
        return log;
    }
}
