import React from 'react';
import ReactDOM from 'react-dom';
import CreateProject from './create-filter';

window.pageRenderers = window.pageRenderers || {};
window.pageRenderers.contentManagement = window.pageRenderers.contentManagement || {};
window.pageRenderers.contentManagement.createProject = window.pageRenderers.contentManagement.createProject || {};
window.pageRenderers.contentManagement.createProject.renderer = (id) => {
  ReactDOM.render(
    <CreateProject />,
    document.getElementById(id),
  );
};
