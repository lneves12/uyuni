/* global module */
const { renderWithHotReload } = require('components/hot-reload/render-with-hot-reload');
const { GuestsEdit } = require('./guests-edit');

window.pageRenderers = window.pageRenderers || {};
window.pageRenderers.guests = window.pageRenderers.guests || {};
window.pageRenderers.guests.edit = window.pageRenderers.guests.edit || {};
window.pageRenderers.guests.edit.guestsEditRenderer = (id, { host, guestUuid }) => {
  const guestEditProps = {
    host,
    guestUuid,
  };

  renderWithHotReload(GuestsEdit, guestEditProps, id);

  if (module.hot) {
    module.hot.accept('./guests-edit.js', () => {
      // eslint-disable-next-line
      const { GuestsEdit } = require('./guests-edit');
      renderWithHotReload(GuestsEdit, guestEditProps, id);
    });
  }
};
