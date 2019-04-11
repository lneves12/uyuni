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
package com.suse.manager.webui.controllers.contentmanagement;

import com.google.gson.Gson;
import com.redhat.rhn.domain.contentmgmt.ContentEnvironment;
import com.redhat.rhn.domain.contentmgmt.ContentFilter;
import com.redhat.rhn.domain.contentmgmt.ContentProject;
import com.redhat.rhn.domain.user.User;
import com.redhat.rhn.manager.contentmgmt.ContentManager;
import com.suse.manager.webui.controllers.contentmanagement.mappers.ResponseMappers;
import com.suse.manager.webui.utils.FlashScopeHelper;
import com.suse.utils.Json;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.jade.JadeTemplateEngine;

import static com.suse.manager.webui.utils.SparkApplicationHelper.withCsrfToken;
import static com.suse.manager.webui.utils.SparkApplicationHelper.withUser;
import static spark.Spark.get;

/**
 * Spark controller class for content management pages.
 */
public class ContentManagementViewsController {

    private static Logger log = Logger.getLogger(ContentManagementViewsController.class);
    private static final Gson GSON = Json.GSON;

    private ContentManagementViewsController() {
    }


    /**
     * @param jade JadeTemplateEngine
     * Invoked from Router. Init routes for ContentManagement Views.
     */
    public static void initRoutes(JadeTemplateEngine jade) {
        // Projects
        get("/manager/contentmanagement/project",
                withCsrfToken(ContentManagementViewsController::createProjectView), jade);
        get("/manager/contentmanagement/project/:label",
                withCsrfToken(withUser(ContentManagementViewsController::editProjectView)), jade);
        get("/manager/contentmanagement/projects",
                withUser(ContentManagementViewsController::listProjectsView), jade);

        // Filters
        get("/manager/contentmanagement/filter/:label",
                withCsrfToken(withUser(ContentManagementViewsController::editFilterView)), jade);
        get("/manager/contentmanagement/filters",
                withUser(ContentManagementViewsController::listFiltersView), jade);
    }

    /**
     * Handler for the server create project page.
     *
     * @param req the request object
     * @param res the response object
     * @return the ModelAndView object to render the page
     */
    public static ModelAndView createProjectView(Request req, Response res) {
        Map<String, Object> data = new HashMap<>();
        return new ModelAndView(data, "controllers/contentmanagement/templates/create-project.jade");
    }

    /**
     * Handler for the server edit project page.
     *
     * @param req the request object
     * @param res the response object
     * @param user the current user
     * @return the ModelAndView object to render the page
     */
    public static ModelAndView editProjectView(Request req, Response res, User user) {

        String projectToEditLabel = req.params("label");

        Optional<ContentProject> projectToEdit = ContentManager.lookupProject(projectToEditLabel, user);

        Map<String, Object> data = new HashMap<>();
        projectToEdit.ifPresent(project -> {
            List<ContentEnvironment> contentEnvironments = ContentManager.listProjectEnvironments(
                    project.getLabel(), user
            );
            data.put("projectToEdit", GSON.toJson(ResponseMappers.mapProjectFromDB(project, contentEnvironments)));
        });
        data.put("wasFreshlyCreatedMessage", FlashScopeHelper.flash(req));

        return new ModelAndView(data, "controllers/contentmanagement/templates/project.jade");
    }

    /**
     * Handler for the server listing projects page.
     *
     * @param req the request object
     * @param res the response object
     * @param user the current user
     * @return the ModelAndView object to render the page
     */
    public static ModelAndView listProjectsView(Request req, Response res, User user) {
        Map<String, Object> data = new HashMap<>();

        List<ContentProject> projects = ContentManager.listProjects(user);

        Map<ContentProject, List<ContentEnvironment>> environmentsByProject = projects.stream()
                .collect(Collectors.toMap(p -> p, p -> ContentManager.listProjectEnvironments(p.getLabel(), user)));

        data.put("flashMessage", FlashScopeHelper.flash(req));
        data.put("contentProjects", GSON.toJson(ResponseMappers.mapProjectListingFromDB(environmentsByProject)));

        return new ModelAndView(data, "controllers/contentmanagement/templates/list-projects.jade");
    }

    /**
     *
     * @param req the request object
     * @param res the response object
     * @param user the current user
     * @return
     */
    public static ModelAndView listFiltersView(Request req, Response res, User user) {
        Map<String, Object> data = new HashMap<>();

        List<ContentFilter> filters = new LinkedList<>();

        data.put("flashMessage", FlashScopeHelper.flash(req));
        data.put("contentFilters", GSON.toJson(filters));


        return new ModelAndView(data, "controllers/contentmanagement/templates/list-filters.jade");
    }

    /**
     *
     * @param req the request object
     * @param res the response object
     * @param user the current user
     * @return
     */
    public static ModelAndView editFilterView(Request req, Response res, User user) {

        String projectToEditLabel = req.params("label");

        Optional<ContentFilter> projectToEdit = null; //ContentManager.lookupProject(projectToEditLabel, user);

        // TODO: Handle errors

        Map<String, Object> data = new HashMap<>();
        data.put("wasFreshlyCreatedMessage", FlashScopeHelper.flash(req));

        return new ModelAndView(data, "controllers/contentmanagement/templates/filter.jade");
    }
}
