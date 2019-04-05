import React from 'react';
import ReactDOM from 'react-dom';
import ListFilters from './list-filters';

window.pageRenderers = window.pageRenderers || {};
window.pageRenderers.contentManagement = window.pageRenderers.contentManagement || {};
window.pageRenderers.contentManagement.listFilters = window.pageRenderers.contentManagement.listFilters || {};
window.pageRenderers.contentManagement.listFilters.renderer = (id, {filters, flashMessage}) => {

  let filtersJson = [];
  try{
    filtersJson = JSON.parse(filters);
  }  catch(error) {}

  ReactDOM.render(
      <ListFilters
        filters={filtersJson}
        flashMessage={flashMessage}
      />,
      document.getElementById(id),
    );
};
