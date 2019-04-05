import React from 'react';
import ReactDOM from 'react-dom';
import Filter from './filter';

window.pageRenderers = window.pageRenderers || {};
window.pageRenderers.contentManagement = window.pageRenderers.contentManagement || {};
window.pageRenderers.contentManagement.filter = window.pageRenderers.contentManagement.filter || {};
window.pageRenderers.contentManagement.filter.renderer = (id, {filter, wasFreshlyCreatedMessage} = {}) => {
  let filterJson = {};
  try{
    filterJson = JSON.parse(filter);
  }  catch(error) {}

  ReactDOM.render(
    <Filter
      filter={filterJson}
      { ...( wasFreshlyCreatedMessage && { wasFreshlyCreatedMessage } ) }
    />,
    document.getElementById(id),
  );
};
