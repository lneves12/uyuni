/**
 * Copyright (c) 2015 SUSE LLC
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

package com.suse.manager.webui.controllers.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.rhn.domain.org.Org;
import com.redhat.rhn.domain.server.virtualhostmanager.VirtualHostManager;
import com.redhat.rhn.domain.server.virtualhostmanager.VirtualHostManagerConfig;
import com.redhat.rhn.domain.server.virtualhostmanager.VirtualHostManagerFactory;
import com.redhat.rhn.testing.BaseTestCaseWithUser;
import com.redhat.rhn.testing.RhnMockHttpServletResponse;
import com.redhat.rhn.testing.TestUtils;
import com.redhat.rhn.testing.UserTestUtils;
import com.suse.manager.gatherer.GathererJsonIO;
import com.suse.manager.gatherer.GathererRunner;
import com.suse.manager.model.gatherer.GathererModule;
import com.suse.manager.webui.controllers.VirtualHostManagerController;
import com.suse.manager.webui.utils.SparkTestUtils;
import spark.Request;
import spark.RequestResponseFactory;
import spark.Response;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.suse.manager.webui.utils.SparkTestUtils.createMockRequestWithParams;

/**
 * Test for basic scenarios in VirtualHostManagerController.
 */
public class VirtualHostManagerControllerTest extends BaseTestCaseWithUser {

    // common request that should be enough for controller functions without
    // dealing with parameters in the url
    private Response response;
    private VirtualHostManagerFactory factory;
    private final String baseUri = "http://localhost:8080/rhn/vhms/";
    private static final Gson GSON = new GsonBuilder().create();
    private static final String VIRT_HOST_GATHERER_MODULES = "{\n"+
        "    \"Kubernetes\": {\n"+
        "        \"module\": \"Kubernetes\",\n"+
        "        \"url\": \"\",\n"+
        "        \"username\": \"\",\n"+
        "        \"password\": \"\",\n"+
        "        \"client-cert\": \"\",\n"+
        "        \"client-key\": \"\",\n"+
        "        \"ca-cert\": \"\",\n"+
        "        \"kubeconfig\": \"\",\n"+
        "        \"context\": \"\"\n"+
        "    },\n"+
        "    \"File\": {\n"+
        "        \"module\": \"File\",\n"+
        "        \"url\": \"\"\n"+
        "    },\n"+
        "    \"VMware\": {\n"+
        "        \"module\": \"VMware\",\n"+
        "        \"hostname\": \"\",\n"+
        "        \"port\": 443,\n"+
        "        \"username\": \"\",\n"+
        "        \"password\": \"\"\n"+
        "    }\n"+
        "}\n";

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();

        response = RequestResponseFactory.create(new RhnMockHttpServletResponse());
        factory = VirtualHostManagerFactory.getInstance();
        VirtualHostManagerController.setGathererRunner(new GathererRunner() {
            @Override
            public Map<String, GathererModule> listModules() {
                return new HashMap<>();
            }
        });

        Map<String, GathererModule> modules = new GathererJsonIO()
                .readGathererModules(VIRT_HOST_GATHERER_MODULES);
        VirtualHostManagerController.setGathererModules(modules);
    }

    /**
     * Test the list endpoint.
     */
    @SuppressWarnings("unchecked")
    public void testGet() {
        VirtualHostManager vhm = createVirtualHostManagerWithLabel("myVHM", user.getOrg());
        String json = (String)VirtualHostManagerController
                .get(getRequestWithCsrf(""), response, user);
        List<Map<String, Object>> model = GSON.fromJson(json, List.class);
        assertEquals(1, model.size());
        assertEquals("myVHM", model.get(0).get("label"));
        assertEquals("File", model.get(0).get("gathererModule"));
        assertEquals(user.getOrg().getName(), model.get(0).get("orgName"));
        assertEquals((Object)vhm.getId(), ((Double)model.get(0).get("id")).longValue());
    }

    /**
     * Test the show endpoint from a wrong organization.
     */
    @SuppressWarnings("unchecked")
    public void testGetWrongOrg() {
        Org otherOrg = UserTestUtils.createNewOrgFull("foobar org");
        String label = "myVHM";
        createVirtualHostManagerWithLabel(label, otherOrg);

        Request request = getRequestWithCsrf("/manager/api/vhms");
        String result = (String)
                VirtualHostManagerController.get(request, response, user);
        List<Map<String, Object>> model = GSON.fromJson(result, List.class);
        assertTrue(model.isEmpty());
    }

    /**
     * Test create.
     */
    public void testCreate() {
        String label = "TestVHM_" + TestUtils.randomString(10);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("label", label);
        queryParams.put("module", "File");
        queryParams.put("module_url", "file:///some/file");
        Request request = createMockRequestWithParams("/manager/api/vhms/create",
                queryParams);

        VirtualHostManagerController.setMockFactory(new VirtualHostManagerFactory() {
            @Override
            public boolean isConfigurationValid(String moduleName,
                    Map<String, String> parameters, String... ignoreParams) {
                return true;
            }
        });
        VirtualHostManagerController.create(request, response, user);

        VirtualHostManager created = VirtualHostManagerFactory.getInstance()
                .lookupByLabel(label);
        assertEquals(label, created.getLabel());
        assertEquals("File", created.getGathererModule());
        assertEquals(1, created.getConfigs().size());
        VirtualHostManagerConfig config = created.getConfigs().iterator().next();
        assertEquals("url", config.getParameter());
        assertEquals("file:///some/file", config.getValue());
        config.getVirtualHostManager();
    }

    /**
     * Test create a VHM with a missing cfg param.
     * Should result in an error.
     */
    public void testCreate_noCfgParam() {
        String label = "TestVHM_" + TestUtils.randomString(10);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("module", "file");
        queryParams.put("label", label);

        Request request = createMockRequestWithParams("/manager/api/vhms/create", queryParams,
                new Object[] {});
        String result = VirtualHostManagerController.create(request, response, user);
        Map<String, Object> res = GSON.fromJson(result, Map.class);
        assertEquals(1, res.size());
        List<String> errs = (List<String>)res.get("errors");
        assertEquals(1, errs.size());
        assertEquals("All fields are mandatory.", errs.get(0));
    }

    /**
     * Test delete.
     */
    public void testDelete() throws UnsupportedEncodingException {
        String label = "myVHM";
        VirtualHostManager vhm = createVirtualHostManagerWithLabel(label, user.getOrg());
        String body = GSON.toJson(Arrays.asList(vhm.getId()));
        Request request = getPostRequestWithCsrfAndBody("/manager/api/vhms/delete", body);
        VirtualHostManagerController.delete(request, response, user);

        assertNull(factory.lookupByLabel(label));
    }

    /**
     * Test the delete endpoint from a wrong organization.
     */
    public void testGetDeleteWrongOrg() throws UnsupportedEncodingException {
        Org otherOrg = UserTestUtils.createNewOrgFull("foobar org");
        String label = "myVHM";
        VirtualHostManager vhm = createVirtualHostManagerWithLabel(label, otherOrg);
        String body = GSON.toJson(Arrays.asList(vhm.getId()));
        Request request = getPostRequestWithCsrfAndBody("/manager/api/vhms/delete", body);
        VirtualHostManagerController.delete(request, response, user);

        // the original VHM is not deleted
        assertNotNull(factory.lookupByLabel(label));
    }

    // Utils methods

    /**
     * Creates a request with csrf token.
     *
     * @param uri the uri
     * @param vars the vars
     * @return the request with csrf
     */
    private Request getRequestWithCsrf(String uri, Object... vars) {
        Request request = SparkTestUtils.createMockRequest(baseUri + uri, vars);
        request.session(true).attribute("csrf_token", "bleh");
        return request;
    }

    /**
     * Creates a request with csrf token.
     *
     * @param uri the uri
     * @param vars the vars
     * @return the request with csrf
     */
    private Request getPostRequestWithCsrfAndBody(String uri, String body, Object... vars) throws UnsupportedEncodingException {
        Request request = SparkTestUtils.createMockRequestWithBody(baseUri + uri, Collections.emptyMap(), body, vars);
        request.session(true).attribute("csrf_token", "bleh");
        return request;
    }


    /**
     * Creates and saves a virtual host manager with a certain label.
     *
     * @param label the label
     * @param org the org
     * @return the virtual host manager
     */
    private VirtualHostManager createVirtualHostManagerWithLabel(String label,
            Org org) {
        VirtualHostManager vhm =
            factory.createVirtualHostManager(label, org, "File",
                new HashMap<String, String>() { {
                        put("url", "notimportant");
                        put("username", "Bing Bong");
                        put("password", "imaginary friend");
                    } });
        factory.save(vhm);
        return vhm;
    }
}
