/* global onDocumentReadyInitOldJS, Loggerhead */
import App, {HtmlScreen} from "senna";
import Surface from "senna/lib/surface/Surface";
import "senna/build/senna.css"
import "./spa-engine.css"
import SpaRenderer from "core/spa/spa-renderer";
import CancellablePromise from 'metal-promise';
import { exitDocument } from 'metal-dom';
import _isEmpty from 'lodash/isEmpty';

function isLoginPage (pathName) {
  const allLoginPossiblePaths = ["/", "/rhn/Login.do", "/rhn/Login2.do", "/manager/login", "/rhn/ReLogin.do"];
  return allLoginPossiblePaths.some(loginPath => loginPath === pathName);
}

window.pageRenderers = window.pageRenderers || {};
window.pageRenderers.spa = window.pageRenderers.spa || {};

window.pageRenderers.spa.init = function init() {
  // We need this until the login page refactor using a different layout template is completed
  if(!isLoginPage(window.location.pathname)) {
    const appInstance = new App();
    // appInstance.setLinkSelector("a.js-spa");
    appInstance.setFormSelector("form.js-spa");

    const pageBodySurface = new Surface("page-body");
    pageBodySurface.show = function (screenId) {
        let from = pageBodySurface.activeChild;
        let to = pageBodySurface.getChild(screenId);
        if (!to) {
          to = pageBodySurface.defaultChild;
        }
        this.activeChild = to;

        const onResolve = function() {
          window.pageRenderers.resolvers = () => {
            if (from) {
              from.style.display = 'none';
              from.classList.remove('flipped');
            }
            if (to) {
              to.style.display = 'block';
              to.classList.add('flipped');
            }
            if (from && from !== to) {
              exitDocument(from);
            }
            SpaRenderer.cleanOldReactTrees();
          }
        };

        return CancellablePromise.resolve(onResolve())
    }
    pageBodySurface.remove = function () {};

    appInstance.addSurfaces(["left-menu-data", "ssm-box", pageBodySurface])
    appInstance.addRoutes([{
      path: /.*/,
      handler: function (route, a, b) {
        const screen = new HtmlScreen();
        screen.setCacheable(false);
        //TODO: remove after https://github.com/liferay/senna.js/pull/311/files
        screen.setHttpHeaders({
          ...screen.getHttpHeaders(),
          ...{"Content-type": "application/x-www-form-urlencoded"}
        });
        screen.getFormData = function(form, submitedButton) {
          let body = $(form).serialize();
          if (submitedButton && submitedButton.name) {
            body += '&' + encodeURI(submitedButton.name) + '=' + encodeURI(submitedButton.value)
          }
          return body;
        }
        screen.beforeActivate = function() {
          window.pageRenderers.spa.globalRenderersToRemove = window.pageRenderers.spa.navigationRenderersToClean;
          window.pageRenderers.spa.navigationRenderersToClean = [];
        };

        return screen;
      }
    }]);
    window.pageRenderers.spa.appInstance = appInstance;

    appInstance.on('beforeNavigate', function(navigation) {
      // Integration with bootstrap 3. We need to make sure all the existing modals get fully removed
      $('.modal').remove();
      $('.modal-backdrop').remove();
      $('body').removeClass( "modal-open" );
    })

    appInstance.on('endNavigate', function(navigation) {
      // workaround to redirect to the login page when there is no session:
      // More info: https://github.com/liferay/senna.js/issues/302
      const urlPath = appInstance.browserPathBeforeNavigate;
      let urlParser = document.createElement('a');
      urlParser.href = urlPath;
      if(isLoginPage(urlParser.pathname)) {
        document.getElementsByClassName("spacewalk-main-column-layout")[0].innerHTML = `
        <div class="container-fluid">
            <div class="alert alert-danger">
                No session. Redirecting to login page...          if(_isEmpty(window.pageRenderers.spa.navigationRenderersToClean)) {
            window.pageRenderers.resolvers  && window.pageRenderers.resolvers();
          }
            </div>
        </div>
      `;
        window.location = urlPath;
      }

      // If an error happens we make a full refresh to make sure the original request is shown instead of a SPA replacement
      if (
        navigation.error && (navigation.error.invalidStatus || navigation.error.timeout || navigation.error.requestError)
      ) {
        window.location = navigation.path;
      }

      Loggerhead.info('[' + new Date().toUTCString() + '] - Loading `' + window.location + '`');
      SpaRenderer.onSpaEndNavigation();
      onDocumentReadyInitOldJS();
      // if(_isEmpty(window.pageRenderers.spa.navigationRenderersToClean)) {
      //   window.pageRenderers.resolvers  && window.pageRenderers.resolvers();
      // }
      // endNavigate not called in the end.............
      // clean now
    });

    return appInstance;
  }
}

window.pageRenderers.spa.navigate = function navigate(url) {
  if(window.pageRenderers.spa.appInstance) {
    window.pageRenderers.spa.appInstance.navigate(url);
  } else {
    window.location = url;
  }
}
