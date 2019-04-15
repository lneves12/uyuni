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
package com.suse.manager.webui.controllers.contentmanagement.handlers;

import static com.suse.manager.webui.utils.SparkApplicationHelper.json;
import static com.suse.manager.webui.utils.SparkApplicationHelper.withUser;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.delete;

import com.redhat.rhn.domain.contentmgmt.ContentFilter;
import com.redhat.rhn.domain.contentmgmt.ContentManagementException;
import com.redhat.rhn.domain.contentmgmt.FilterCriteria;
import com.redhat.rhn.domain.user.User;
import com.redhat.rhn.manager.contentmgmt.ContentManager;

import com.suse.manager.webui.controllers.contentmanagement.request.FilterRequest;
import com.suse.manager.webui.utils.gson.ResultJson;
import com.suse.utils.Json;

import com.google.gson.Gson;

import org.apache.http.HttpStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import spark.Request;
import spark.Response;

/**
 * Spark controller ContentManagement Filter Api.
 */
public class FilterApiController {

    private static final Gson GSON = Json.GSON;

    private FilterApiController() {
    }

    /** Init routes for ContentManagement Filter Api.*/
    public static void initRoutes() {
        post("/manager/contentmanagement/api/filters",
                withUser(FilterApiController::createContentFilter));

        put("/manager/contentmanagement/api/filters/:filterId",
                withUser(FilterApiController::updateContentFilter));

        delete("/manager/contentmanagement/api/filters/:filterId",
                withUser(FilterApiController::removeContentFilter));
    }

    /**
     * Return the JSON with the result of the creation of a filter.
     * @param req the http request
     * @param res the http response
     * @param user the current user
     * @return the JSON data
     */
    public static String createContentFilter(Request req, Response res, User user) {
        FilterRequest createFilterRequest = FilterHandler.getFilterRequest(req);

        HashMap<String, String> requestErrors = FilterHandler.validateFilterRequest(createFilterRequest);
        if (!requestErrors.isEmpty()) {
            return json(GSON, res, HttpStatus.SC_BAD_REQUEST, ResultJson.error(Arrays.asList(""), requestErrors));
        }

        FilterCriteria filterCriteria = new FilterCriteria(
                FilterCriteria.Matcher.CONTAINS,
                createFilterRequest.getField(),
                createFilterRequest.getValue());
        ContentManager.createFilter(
                createFilterRequest.getName(),
                ContentFilter.Rule.lookupByLabel(createFilterRequest.getRule()),
                ContentFilter.EntityType.lookupByLabel(createFilterRequest.getType()),
                filterCriteria,
                user
        );

        return json(GSON, res, ResultJson.success());
    }

    /**
     * Return the JSON with the result of updating a content project environemnt.
     * @param req the http request
     * @param res the http response
     * @param user the current user
     * @return the JSON data
     */
    public static String updateContentFilter(Request req, Response res, User user) {
        FilterRequest createFilterRequest = FilterHandler.getFilterRequest(req);

        HashMap<String, String> requestErrors = FilterHandler.validateFilterRequest(createFilterRequest);
        if (!requestErrors.isEmpty()) {
            return json(GSON, res, HttpStatus.SC_BAD_REQUEST, ResultJson.error(Arrays.asList(""), requestErrors));
        }

        FilterCriteria filterCriteria = new FilterCriteria(
                FilterCriteria.Matcher.CONTAINS,
                createFilterRequest.getField(),
                createFilterRequest.getValue());
        ContentManager.updateFilter(
                Long.parseLong(req.params("filterId")),
                Optional.ofNullable(createFilterRequest.getName()),
                Optional.of(ContentFilter.Rule.lookupByLabel(createFilterRequest.getRule())),
                Optional.of(filterCriteria),
                user
        );

        return json(GSON, res, ResultJson.success());
    }

    /**
     * Return the JSON with the result of removing a content project environemnt.
     * @param req the http request
     * @param res the http response
     * @param user the current user
     * @return the JSON data
     */
    public static String removeContentFilter(Request req, Response res, User user) {
        try {
            ContentManager.removeFilter(Long.parseLong(req.params("filterId")), user);
        }
        catch (ContentManagementException error) {
            return json(GSON, res, HttpStatus.SC_BAD_REQUEST, ResultJson.error(error.getMessage()));
        }

        return json(GSON, res, ResultJson.success());
    }

}
