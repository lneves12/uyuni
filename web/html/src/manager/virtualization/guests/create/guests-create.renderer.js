/* global module */
const { GuestsCreate } = require('./guests-create');

window.pageRenderers = window.pageRenderers || {};
window.pageRenderers.guests = window.pageRenderers.guests || {};
window.pageRenderers.guests.create = window.pageRenderers.guests.create || {};
window.pageRenderers.guests.create.guestsCreateRenderer = (id, { host }) => {
  ReactDOM.render(
    <GuestsCreate
      host={host}
    />,
    document.getElementById(id),
  );
};
