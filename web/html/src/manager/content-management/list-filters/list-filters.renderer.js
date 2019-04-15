import React from 'react';
import ReactDOM from 'react-dom';
import ListFilters from './list-filters';
import "./list-filters.css";

window.pageRenderers = window.pageRenderers || {};
window.pageRenderers.contentManagement = window.pageRenderers.contentManagement || {};
window.pageRenderers.contentManagement.listFilters = window.pageRenderers.contentManagement.listFilters || {};
window.pageRenderers.contentManagement.listFilters.renderer = (id, {filters, flashMessage}) => {

  let filtersJson = [];
  try{
    filtersJson = JSON.parse(filters);
    /* MOCK UP - DEVELOPMENT STUFF */
    const fake = '[' +
      '{"name":"temp1",' +
      '"target":"patch",' +
      '"criteria":"*java*",' +
      '"deny":true,' +
      '"projects":["temp","another-temp"]},{"name":"temp2","target":"package","criteria":"*py*","deny":false,"projects":["another-temp"]}]';
    filtersJson = JSON.parse(fake);
    /*****************/
  }  catch(error) {}

  ReactDOM.render(
      <ListFilters
        filters={filtersJson}
        flashMessage={flashMessage}
      />,
      document.getElementById(id),
    );
};
