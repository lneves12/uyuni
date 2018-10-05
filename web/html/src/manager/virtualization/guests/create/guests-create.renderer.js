/* global module */
const { renderWithHotReload } = require('components/hot-reload/render-with-hot-reload');
const { GuestsCreate } = require('./guests-create');

window.pageRenderers = window.pageRenderers || {};
window.pageRenderers.guests = window.pageRenderers.guests || {};
window.pageRenderers.guests.create = window.pageRenderers.guests.create || {};
window.pageRenderers.guests.create.guestsCreateRenderer = (id, { host }) => {
  const guestCreateProps = { host };

  renderWithHotReload(GuestsCreate, guestCreateProps, id);

  if (module.hot) {
    module.hot.accept('./guests-create.js', () => {
      // eslint-disable-next-line
      const { GuestsCreate } = require('./guests-create');
      renderWithHotReload(GuestsCreate, guestCreateProps, id);
    });
  }
};
