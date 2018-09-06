import Login from "./login";
const ReactDOM = require("react-dom");
const React = require("react");

window.pageRenderers = window.pageRenderers || {};
window.pageRenderers.login = window.pageRenderers.login || {};

window.pageRenderers.login.loginRenderer = (id, {urlBounce}) => ReactDOM.render(
  <Login
    bounce={urlBounce}
  />,
  document.getElementById(id)
);
